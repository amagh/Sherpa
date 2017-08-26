package project.hikerguide.utilities;

import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.firebase.geofire.GeoFire;
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

import project.hikerguide.BR;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.files.GpxFile;
import project.hikerguide.files.ImageFile;
import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Rating;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import timber.log.Timber;

import static junit.framework.Assert.assertNotNull;
import static project.hikerguide.utilities.FirebaseProviderUtils.FirebaseType.AREA;
import static project.hikerguide.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;
import static project.hikerguide.utilities.FirebaseProviderUtils.FirebaseType.GUIDE;
import static project.hikerguide.utilities.FirebaseProviderUtils.FirebaseType.RATING;
import static project.hikerguide.utilities.FirebaseProviderUtils.FirebaseType.SECTION;
import static project.hikerguide.utilities.FirebaseProviderUtils.FirebaseType.TRAIL;

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

    @IntDef({GUIDE, TRAIL, AUTHOR, SECTION, AREA, RATING})
    public @interface FirebaseType {
        int GUIDE = 0;
        int TRAIL = 1;
        int AUTHOR = 2;
        int SECTION = 3;
        int AREA = 4;
        int RATING = 5;
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

    /**
     * Adds/removes a Guide from an User's list of favorite Guides
     *
     * @param author    Data model representing the Firebase Database entry for the User
     * @param guide     Guide to be added/removed to the Author's favorite list
     */
    public static void toggleFirebaseFavorite(Author author, Guide guide) {

        // Check whether the Guide is currently a favorite and switch it in the data model
        if (guide.isFavorite()) {
            guide.setFavorite(false);
        } else {
            guide.setFavorite(true);
        }

        // Ensure that the List of Guides has been initialized
        if (author.favorites == null) {
            author.favorites = new HashMap<>();
        }

        // Modify the Firebase Database entry
        if (guide.isFavorite()) {
            author.favorites.put(guide.firebaseId, guide.trailName);
        } else {
            author.favorites.remove(guide.firebaseId);
        }

        // Push the update to FirebaseDatabase
        String directory = GuideDatabase.AUTHORS + "/" + author.firebaseId;

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(directory, author);

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(childUpdates);
    }

    /**
     * Updates a User's Firebase Database entry to match the local changes made
     *
     * @param user      User to be updated
     */
    public static void updateUser(Author user) {

        // Run an update on the values
        Map<String, Object> childUpdates = new HashMap<>();

        String directory = GuideDatabase.AUTHORS + "/" + user.firebaseId;

        childUpdates.put(directory, user.toMap());

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(childUpdates);
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

        // Update the Guide and the Author
        updateGuideScore(rating, previousRating);
        updateAuthorScore(rating, previousRating);

        // Push the Rating to the Firebase Database
        String directory = RATING_DIRECTORY + "/" + rating.getGuideId() + "/" + rating.firebaseId;

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(directory, rating.toMap());

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(childUpdates);
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
    public static void getGuideRatingForFirebaseUser(String guideId, final FirebaseListener listener) {

        // Check to ensure the Firebase User is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            listener.onModelReady(null);
            return;
        }

        // Generate the Query for the Rating
        final Query ratingQuery = FirebaseDatabase.getInstance().getReference()
                .child(RATING_DIRECTORY)
                .child(guideId)
                .orderByChild(Rating.AUTHOR_ID)
                .equalTo(user.getUid());

        ratingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    // Retrieve the Rating from the DataSnapshot
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        listener.onModelReady(getModelFromSnapshot(RATING, snapshot));
                        break;
                    }

                } else {

                    // Nothing found, return null
                    listener.onModelReady(null);
                }

                // Remove the Listener
                ratingQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove the Listener
                ratingQuery.removeEventListener(this);
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
