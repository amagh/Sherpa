package project.sherpa.utilities;

import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.geofire.GeoFire;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.files.GpxFile;
import project.sherpa.files.ImageFile;
import project.sherpa.files.abstractfiles.BaseFile;
import project.sherpa.models.datamodels.Area;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.Message;
import project.sherpa.models.datamodels.Rating;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Section;
import project.sherpa.models.datamodels.Trail;
import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AREA;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.CHAT;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.GUIDE;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.MESSAGE;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.RATING;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.SECTION;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.TRAIL;

/**
 * Utility class for commonly used functions for the DatabaseProvider
 */

public class FirebaseProviderUtils {
    // ** Constants ** //
    public static final String IMAGE_PATH = "images";
    public static final String GPX_PATH = "gpx";
    public static final String JPEG_EXT = ".jpg";
    public static final String GPX_EXT = ".gpx";
    public static final String BACKDROP_SUFFIX = "_bd";
    public static final String GEOFIRE_PATH = "geofire";

    @IntDef({GUIDE, TRAIL, AUTHOR, SECTION, AREA, RATING, CHAT, MESSAGE})
    public @interface FirebaseType {
        int GUIDE       = 0;
        int TRAIL       = 1;
        int AUTHOR      = 2;
        int SECTION     = 3;
        int AREA        = 4;
        int RATING      = 5;
        int CHAT        = 6;
        int MESSAGE     = 7;
    }

    public static final String RATING_DIRECTORY = "ratings";

    /**
     * Helper method for getting the directory of a type
     *
     * @param type    FirebaseType
     * @return The directory that corresponds to the type
     */
    public static String getDirectoryFromType(@FirebaseType int type) {
        switch (type) {
            case GUIDE:
                return GuideDatabase.GUIDES;

            case TRAIL:
                return GuideDatabase.TRAILS;

            case AUTHOR:
                return GuideDatabase.AUTHORS;

            case SECTION:
                return GuideDatabase.SECTIONS;

            case AREA:
                return GuideDatabase.AREAS;

            case CHAT:
                return GuideDatabase.CHATS;

            case MESSAGE:
                return GuideDatabase.MESSAGES;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }
    }

    /**
     * Helper method for getting the directory corresponding to the class of a BaseModel
     *
     * @param model    The BaseModel extended Class
     * @return The directory corresponding to the Class of the BaseModel parameter
     */
    public static String getDirectoryFromModel(BaseModel model) {

        // Resolve the directory based on the Class
        if (model instanceof Guide) {
            return GuideDatabase.GUIDES;
        } else if (model instanceof Trail) {
            return GuideDatabase.TRAILS;
        } else if (model instanceof Author) {
            return GuideDatabase.AUTHORS;
        } else if (model instanceof Section) {
            return GuideDatabase.SECTIONS;
        } else if (model instanceof Area) {
            return GuideDatabase.AREAS;
        } else if (model instanceof Chat) {
            return GuideDatabase.CHATS;
        } else if (model instanceof Message) {
            return GuideDatabase.MESSAGES;
        } else {
            throw new UnsupportedOperationException("Unknown model:" + model.getClass());
        }
    }

    /**
     * Retrieves an Array of BaseModels from a DataSnapShot containing children of those types
     *
     * @param type            The FirebaseType. This will correspond to the type of BaseModel that
     *                        will be returned
     * @param dataSnapshot    The DataSnapShot containing children of the FirebaseType
     * @return An Array of BaseModels corresponding to the FirebaseType parameter
     */
    public static BaseModel[] getModelsFromSnapshot(@FirebaseType int type, DataSnapshot dataSnapshot) {
        // Initialize the List that will store all the BaseModels created from the DataSnapshots
        List<BaseModel> modelList = new ArrayList<>();

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            // Cast the data to the correct BaseModel based on the type
            BaseModel model = getModelFromSnapshot(type, snapshot);

            // Add it to the List to be returned by the listener
            modelList.add(model);
        }

        // Create a new Array to return
        BaseModel[] models;

        switch (type) {
            case GUIDE:
                models = new Guide[modelList.size()];
                break;

            case TRAIL:
                models = new Trail[modelList.size()];
                break;

            case AUTHOR:
                models = new Author[modelList.size()];
                break;

            case SECTION:
                models = new Section[modelList.size()];
                break;

            case AREA:
                models = new Area[modelList.size()];
                break;

            case RATING:
                models = new Rating[modelList.size()];
                break;

            case CHAT:
                models = new Chat[modelList.size()];
                break;

            case MESSAGE:
                models = new Message[modelList.size()];
                break;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }

        // Copy the data to the created Array
        return modelList.toArray(models);
    }

    /**
     * Converts a DataSnapshot to a single BaseModel
     *
     * @param type            The FirebaseType corresponding to the BaseModel to be returned
     * @param dataSnapshot    The DataSnapshot describing a BaseModel
     * @return A BaseModel with the information contained within the DataSnapshot
     */
    public static BaseModel getModelFromSnapshot(@FirebaseType int type, DataSnapshot dataSnapshot) {
        BaseModel model;

        switch (type) {
            case GUIDE:
                model = dataSnapshot.getValue(Guide.class);
                break;

            case TRAIL:
                model = dataSnapshot.getValue(Trail.class);
                break;

            case AUTHOR:
                model = dataSnapshot.getValue(Author.class);
                break;

            case SECTION:
                model = dataSnapshot.getValue(Section.class);
                break;

            case AREA:
                model = dataSnapshot.getValue(Area.class);
                break;

            case RATING:
                model = dataSnapshot.getValue(Rating.class);
                break;

            case CHAT:
                model = dataSnapshot.getValue(Chat.class);
                break;

            case MESSAGE:
                model = dataSnapshot.getValue(Message.class);
                break;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }

        // Set the model's firebaseId
        assertNotNull(model);
        model.firebaseId = dataSnapshot.getKey();

        return model;
    }

    /**
     * Retrieves a Model corresponding to the FirebaseId and type parameters
     *
     * @param type          FirebaseType pertaining to the type of BaseModel to be retrieved
     * @param firebaseId    FirebaseId of entry to be retrieved
     * @param listener      FirebaseListener that will be used to pass the retrieved object to the
     *                      calling Object
     */
    public static void getModel(@FirebaseType final int type,
                                @NonNull String firebaseId,
                                @NonNull final FirebaseListener listener) {

        // Get the directory to insert the data to based on the type
        String directory = getDirectoryFromType(type);

        // Get a Database Reference for retrieving the data
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child(directory)
                .child(firebaseId);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // Check that the DataSnapshot exists
                        if (dataSnapshot.exists()) {

                            // Convert the DataSnapshot to a BaseModel
                            BaseModel model = getModelFromSnapshot(type, dataSnapshot);

                            // Return the model to the requesting Object
                            listener.onModelReady(model);
                        } else {
                            listener.onModelReady(null);
                        }

                        // Remove the Listener
                        reference.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        // Return a null result
                        listener.onModelReady(null);

                        // Remove the Listener
                        reference.removeEventListener(this);
                    }
                });
    }

    /**
     * Retrieves an Array of Sections from Firebase Database corresponding to the FirebaseId of
     * a Guide
     *
     * @param firebaseId    FirebaseId of the Guide to retrieve the Sections for
     * @param listener      Listener to pass the retrieved Sections to the calling Object
     */
    public static void getSectionsForGuide(String firebaseId, final FirebaseArrayListener listener) {

        // Query the database for Sections that match the FirebaseId of the Guide in the signature
        final Query query = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.SECTIONS)
                .orderByKey()
                .equalTo(firebaseId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Check that the DataSnapshot is valid
                if (dataSnapshot.exists()) {


                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // Convert the DataSnapshot to an Array of BaseModels
                        Section[] models = (Section[]) getModelsFromSnapshot(SECTION, snapshot);

                        // Return the Sections to the calling Object
                        listener.onModelsReady(models);

                        return;
                    }
                }

                // Remove the Listener
                query.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove the Listener
                query.removeEventListener(this);
            }
        });
    }

    /**
     * Retrieves the Author data model representing the FirebaseUser currently logged in
     *
     * @param listener The Listener that will be used to pass the retrieved Author to the calling
     *                 Object
     */
    public static void getAuthorForFirebaseUser(final FirebaseListener listener) {

        // Get the FirebaseUser currently logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {

            // Retrieve the Author corresponding to the User's UID
            getModel(AUTHOR, user.getUid(), listener);
        }
    }

    public static void updateUser(Author user) {
        updateUser(user, null);
    }

    /**
     * Updates a User's Firebase Database entry to match the local changes made
     *
     * @param user      User to be updated
     * @param listener  Listener to pass result to observer
     */
    public static void updateUser(Author user, @Nullable OnSuccessListener<Void> listener) {

        // Run an update on the values
        Map<String, Object> childUpdates = new HashMap<>();

        String directory = GuideDatabase.AUTHORS + "/" + user.firebaseId;

        childUpdates.put(directory, user.toMap());

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(childUpdates)
                .addOnSuccessListener(listener);
    }

    /**
     * Adds/removes a Guide from an User's list of favorite Guides
     *
     * @param guide     Guide to be added/removed to the Author's favorite list
     */
    public static void toggleFirebaseFavorite(final Guide guide) {

        // Check to see if the user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // If the user is not logged in, do nothing
        if (user == null) return;

        // Check to see if the user's Author model is cached
        final Author author = (Author) DataCache.getInstance().get(user.getUid());

        if (author != null) {

            // Toggle the favorite status for the User
            toggleFavoriteForUser(author, guide);
        } else {

            // User Object is not cached. Retrieve it from Firebase
            getAuthorForFirebaseUser(new FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {
                    if (model != null) {

                        // Cache the data
                        Author author = (Author) model;
                        DataCache.getInstance().store(author);

                        // Toggle the favorite status for the User
                        toggleFavoriteForUser(author, guide);
                    }
                }
            });
        }
    }

    /**
     * Updates the user's favorite Map to either add or remove a guide from it
     *
     * @param user     The Author to be modified
     * @param guide    The Guide that is to be added/removed from favorites
     */
    private static void toggleFavoriteForUser(Author user, Guide guide) {

        // Ensure that the List of Guides has been initialized
        if (user.favorites == null) {
            user.favorites = new HashMap<>();
        }

        // Modify the Firebase Database entry
        if (guide.isFavorite()) {
            user.favorites.put(guide.firebaseId, guide.trailName);
        } else {
            user.favorites.remove(guide.firebaseId);
        }

        updateUser(user);
    }

    /**
     * Adds/updates the Rating of a Guide, the Guide's ratings, and the Author's score
     *
     * @param rating            Rating to be inserted/updated in the Firebase Database
     * @param previousRating    The value of the rating of the previous Rating for the Guide by
     *                          the user
     */
    public static void updateRating(final Rating rating, final int previousRating) {

        // Add a FirebaseId to the Rating if it doesn't already have one
        if (rating.firebaseId == null) {
            rating.firebaseId = FirebaseDatabase.getInstance().getReference()
                    .child(RATING_DIRECTORY)
                    .push()
                    .getKey();
        }

        // Push the Rating to the Firebase Database
        String directory = RATING_DIRECTORY + "/" + rating.firebaseId;

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(directory, rating.toMap());

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(childUpdates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // Update the Guide and the Author
                        updateGuideScore(rating, previousRating);
                        updateAuthorScore(rating, previousRating);
                    }
                });
    }

    /**
     * Updates a Guide's score after their rating has been altered
     *
     * @param rating            The Rating to be added to be added the Guide's rating/reviews
     * @param previousRating    The value of the rating of the previous Rating for the Guide by
     *                          the user
     */
    private static void updateGuideScore(final Rating rating, final int previousRating) {

        // Update the Guide's rating/reviews
        DatabaseReference guideRef = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .child(rating.getGuideId());

        guideRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {

                        // Retrieve the corresponding Guide for the Rating
                        Guide guide = mutableData.getValue(Guide.class);

                        // Re-do the procedure if the Guide is null
                        if (guide == null) {
                            updateGuideScore(rating, previousRating);
                            return Transaction.abort();
                        }

                        guide.rating += rating.getRating() - previousRating;

                        if (previousRating == 0) {

                            // Increment the Guide reviews if it has not previously been rated
                            guide.reviews++;
                        }

                        // Update the Firebase Database value
                        mutableData.setValue(guide);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        Timber.e("Error updating guide's score: " + databaseError);
                    }
                });
    }

    /**
     * Updates an Author's score after a rating for one of their Guides has been altered
     *
     * @param rating            The Rating to be added to the Author's Map of rated Guides
     * @param previousRating    The value of the rating of the previous Rating for the Guide by
     *                          the user
     */
    private static void updateAuthorScore(final Rating rating, final int previousRating) {

        // Build the Transaction
        DatabaseReference authorRef = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(rating.getGuideAuthorId());

        authorRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {

                        // Get the Author from the data
                        Author author = mutableData.getValue(Author.class);

                        // Re-do the procedure if the Author is null
                        if (author == null) {
                            updateAuthorScore(rating, previousRating);
                            return Transaction.abort();
                        }

                        // Change the Author's score
                        author.score += rating.getRating() - previousRating;

                        // Set the data to be updated to Firebase Database
                        mutableData.setValue(author);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        Timber.e("Error updating author's score: " + databaseError);
                    }
                });
    }

    /**
     * Retrieves ratings for a specific Guide
     *
     * @param guide       Guide to retrieve Ratings for
     * @param page        Correlates to the number of Ratings to be returned
     * @param listener    Listener that will be used to pass the Ratings to the calling Object
     */
    public static void getRatingsForGuide(Guide guide, int page, final FirebaseArrayListener listener) {

        // Setup the query
        Query ratingQuery = FirebaseDatabase.getInstance().getReference()
                .child(RATING_DIRECTORY)
                .orderByKey()
                .equalTo(guide.firebaseId);

        if (page == 0) {

            // For page 0, only return 5 reviews as a preview of the reviews
            ratingQuery = ratingQuery.limitToLast(5);
        } else {

            // For subsequent pages, return an additional 20 for each page
            ratingQuery = ratingQuery.limitToLast(5 + 20 * page);
        }

        final Query finalRatingQuery = ratingQuery;

        ratingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // Retrieve the Ratings and pass them to the Listener
                        Rating[] ratings = (Rating[]) getModelsFromSnapshot(RATING, snapshot);

                        listener.onModelsReady(ratings);
                    }

                } else {

                    // No data found, return null
                    listener.onModelsReady(null);
                }
                // Remove the Listener
                finalRatingQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove the Listener
                finalRatingQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Retrieves a Rating specific to a Guide for the logged in Firebase User
     *
     * @param guideId     The FirebaseId of the Guide to retrieve the Rating for
     * @param listener    The Listener that will be used to pass the Rating to the calling Object
     */
    public static void getGuideRatingForFirebaseUser(final String guideId, final FirebaseListener listener) {

        // Check to ensure the Firebase User is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            listener.onModelReady(null);
            return;
        }

        // Get all the Ratings authored by the user
        getAllRatingsForFirebaseUser(new FirebaseArrayListener() {
            @Override
            public void onModelsReady(BaseModel[] models) {

                // Check to see that the array is valid
                if (models == null || models.length == 0) {
                    listener.onModelReady(null);
                    return;
                }

                // Iterate through each Rating and return the one that matches the guideId
                Rating[] ratings = (Rating[]) models;

                for (Rating rating : ratings) {
                    Timber.d("Rating: " + rating + ": " + rating.getGuideId());
                    Timber.d("GuideId: " + guideId);
                    if (rating.getGuideId().equals(guideId)) {
                        listener.onModelReady(rating);
                        return;
                    }
                }

                // No matches
                listener.onModelReady(null);
            }
        });
    }

    /**
     * Queries FirebaseDatabase for any Ratings that are authored by the logged in Firebase User
     *
     * @param listener    Listener to return the Ratings to the observer
     */
    public static void getAllRatingsForFirebaseUser(final FirebaseArrayListener listener) {

        // Check to ensure the Firebase User is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {

            // User not logged in, return null
            listener.onModelsReady(null);
            return;
        }

        // Query database for all Ratings authored by the user
        final Query ratingsQuery = FirebaseDatabase.getInstance().getReference()
                .child(RATING_DIRECTORY)
                .orderByChild(GuideContract.GuideEntry.AUTHOR_ID)
                .equalTo(user.getUid());

        ratingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Convert the data to an array of Ratings
                Rating[] ratings = (Rating[]) getModelsFromSnapshot(RATING, dataSnapshot);

                // Pass it to the observer
                listener.onModelsReady(ratings);

                // Remove the Listener
                ratingsQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove the Listener
                ratingsQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Retrieves the Chats that the user is involved in from Firebase Database
     *
     * @param listener    Listener to return results to the observer
     */
    public static void getChatsForFirebaseUser(final FirebaseListener listener) {

        // Get the logged in FirebaseUser
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        // Get the Author associated with the FirebaseUser
        Author author = (Author) DataCache.getInstance().get(user.getUid());

        if (author == null) {
            getAuthorForFirebaseUser(new FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {

                    // Cache the Author and re-run this function
                    DataCache.getInstance().store(model);

                    getChatsForFirebaseUser(listener);
                }
            });
        } else {

            // Get each Chat that the User is a part of
            if (author.getChats() == null) {
                listener.onModelReady(null);

                return;
            }

            for (String chatId : author.getChats()) {
                final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference()
                        .child(GuideDatabase.CHATS)
                        .child(chatId);

                chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Chat chat = (Chat) getModelFromSnapshot(CHAT, dataSnapshot);

                        if (chat != null) listener.onModelReady(chat);

                        chatRef.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        chatRef.removeEventListener(this);
                    }
                });
            }
        }
    }

    /**
     * Queries Firebase for Areas that match the query
     *
     * @param query       The query to filter the Firebase Database for
     * @param listener    The Listener to pass the results to the calling Object
     */
    public static void queryFirebaseForAreas(String query, final FirebaseArrayListener listener) {

        // Build a Query for the Firebase Database
        final Query firebaseQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AREAS)
                .orderByChild(Area.LOWER_CASE_NAME)
                .startAt(query.toLowerCase())
                .endAt(query.toLowerCase() + "z");

        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Check that the DataSnapshot is valid
                if (dataSnapshot.exists()) {

                    // Pass the Array to listener
                    Area[] areas = (Area[]) FirebaseProviderUtils.getModelsFromSnapshot(
                            FirebaseProviderUtils.FirebaseType.AREA,
                            dataSnapshot);

                    listener.onModelsReady(areas);
                } else {

                    // Return an empty Array
                    listener.onModelsReady(new Area[0]);
                }

                // Remove Listener
                firebaseQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                firebaseQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Filters the Firebase Data for Trails that match an Area's FirebaseId
     *
     * @param area        Area to search for matching Trails
     * @param listener    The Listener to pass the results to the receiver
     */
    public static void queryFirebaseForTrails(Area area, final FirebaseArrayListener listener) {

        // Query the Firebase Database
        final Query trailQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.TRAILS)
                .orderByChild(GuideContract.TrailEntry.AREA_ID)
                .equalTo(area.firebaseId);

        trailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Check that the result is valid
                if (dataSnapshot.exists()) {

                    // Retrieve the Trails from the DataSnapshot
                    Trail[] trails = (Trail[]) FirebaseProviderUtils.getModelsFromSnapshot(FirebaseProviderUtils.FirebaseType.TRAIL, dataSnapshot);

                    // Pass the Trails to the Listener
                    listener.onModelsReady(trails);
                } else {
                    listener.onModelsReady(new Trail[0]);
                }

                // Remove Listener
                trailQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                trailQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Queries the Firebase Database for an Auther that has a matching username
     *
     * @param username    Username to query the Firebase Database for
     * @param listener    Listener to return the results to the observer
     */
    public static void queryForUsername(String username, final FirebaseListener listener) {
        final Query usernameQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .orderByChild(GuideContract.AuthorEntry.LOWER_CASE_USERNAME)
                .equalTo(username.toLowerCase());

        usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Author author = (Author) getModelFromSnapshot(AUTHOR, snapshot);

                        listener.onModelReady(author);
                    }
                } else {
                    listener.onModelReady(null);
                }

                usernameQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                usernameQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Returns an instance of GeoFire that is already set to the correct Firebase Database Reference
     *
     * @return a new GeoFire instance
     */
    public static GeoFire getGeoFireInstance() {

        // Get the Database Reference for GeoFire's database path
        DatabaseReference geoFireRef = FirebaseDatabase.getInstance().getReference()
                .child(GEOFIRE_PATH);

        return new GeoFire(geoFireRef);
    }

    //********************************************************************************************//
    //************************* Firebase Storage Related Methods *********************************//
    //********************************************************************************************//

    public static String getDirectoryFromFile(BaseFile file) {
        // Return the directory based on the class of the File
        if (file instanceof ImageFile) {
            return IMAGE_PATH;
        } else if (file instanceof GpxFile){
            return GPX_PATH;
        } else {
            throw new UnsupportedOperationException("Unknown BaseFile: " + file.getClass());
        }
    }

    public static String getFileExtensionFromFile(BaseFile file) {
        // Return the file extension based on the class of the File
        if (file instanceof ImageFile) {
            return JPEG_EXT;
        } else if (file instanceof GpxFile){
            return GPX_EXT;
        } else {
            throw new UnsupportedOperationException("Unknown BaseFile: " + file.getClass());
        }
    }

    /**
     * Generates the StorageReference for where a File is stored on Firebase Storage
     *
     * @param file    File to get the StorageReference for
     * @return The StorageReference for a File
     */
    public static StorageReference getReferenceForFile(StorageReference storageReference, BaseFile file) {
        // Get the directory and file extension based on the File's type
        String directory = getDirectoryFromFile(file);
        String fileExtension = getFileExtensionFromFile(file);

        // Generate the StorageReference using the directory and file extension
        return storageReference.child(directory).child(file.firebaseId + fileExtension);
    }

    /**
     * Parses a Uri pointing to a FirebaseStorage Location to a StorageReference
     *
     * @param firebaseUri    Uri to be parsed
     * @return StorageReference pointing to the location indicated by the Uri
     */
    public static StorageReference getReferenceFromUri(Uri firebaseUri) {

        // Check to ensure the schema matches that for Firebase Storage
        if (!firebaseUri.getScheme().matches("gs")) {
            return null;
        } else {

            // Build the Storage using the segments of the Uri
            List<String> segments = firebaseUri.getPathSegments();
            return FirebaseStorage.getInstance().getReference()
                    .child(segments.get(0))
                    .child(segments.get(1));
        }
    }

    /**
     * Listener used to return a BaseModel once it has been successfully loaded
     */
    public interface FirebaseListener {
        void onModelReady(BaseModel model);
    }

    public interface FirebaseArrayListener {
        void onModelsReady(BaseModel[] models);
    }
}
