package project.hikerguide.firebasedatabase;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.models.Area;
import project.hikerguide.models.Author;
import project.hikerguide.models.BaseModel;
import project.hikerguide.models.Guide;
import project.hikerguide.models.Section;
import project.hikerguide.models.Trail;

import static junit.framework.Assert.assertNotNull;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.AREA;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.AUTHOR;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.GUIDE;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.SECTION;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.TRAIL;

/**
 * Created by Alvin on 7/17/2017.
 */

public class FirebaseProvider {
    // ** Constants ** //
    private static final String TAG = FirebaseProvider.class.getSimpleName();
    private static final String GEOFIRE_PATH = "geofire";
    private static final int GUIDE_LIMIT = 20;

    @IntDef({GUIDE, TRAIL, AUTHOR, SECTION, AREA})
    public @interface FirebaseType {
        int GUIDE = 0;
        int TRAIL = 1;
        int AUTHOR = 2;
        int SECTION = 3;
        int AREA = 4;
    }

    // ** Member Variables ** //
    private static FirebaseProvider sProvider;
    private DatabaseReference mDatabase;

    /**
     * Private Constructor to enforce Singleton pattern
     */
    private FirebaseProvider() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }
    }

    /**
     * Instantiates a FirebaseProvider if not already instantiated and returns it
     *
     * @return The Singleton FirebaseProvider instance
     */
    public static synchronized FirebaseProvider getInstance() {
        if (sProvider == null) {
            sProvider = new FirebaseProvider();
        }

        return sProvider;
    }

    private String getFirebasePath(String directory, String key) {
        return "/" + directory + "/" + key;
    }

    /**
     * Inserts a record into the Firebase Database
     *
     * @param models    The data model Object describing the information to add to the database
     */
    public BaseModel[] insertRecord(BaseModel... models) {

        // Init the Map that will be used to insert values into the Firebase Database
        Map<String, Object> childUpdates = new HashMap<>();

        // Init the directory variable that will be used to generate the path for insertion
        String directory;

        // Iterate, get the key for each record, and add the values to childUpdates
        for (BaseModel model : models) {

            if (model instanceof Guide) {
                directory = GuideDatabase.GUIDES;
            } else if (model instanceof Trail) {
                directory = GuideDatabase.TRAILS;
            } else if (model instanceof Author) {
                directory = GuideDatabase.AUTHORS;
            } else if (model instanceof Section) {
                directory = GuideDatabase.SECTIONS;
            } else if (model instanceof Area) {
                directory = GuideDatabase.AREAS;
            } else {
                throw new UnsupportedOperationException("Unknown model:" + model.getClass());
            }

            // Push the path to get the key
            String key = mDatabase.child(directory).push().getKey();

            // Add the model's values to childUpdates
            childUpdates.put(getFirebasePath(directory, key), model.toMap());

            // Modify the model so it includes the key
            model.firebaseId = key;

            if (model instanceof Guide) {
                // When inserting a Guide object, there needs to be accompanying coordinate data
                // loaded into the Database for GeoFire queries
                DatabaseReference geoFireReference = mDatabase.child(GEOFIRE_PATH);
                GeoFire geoFire = new GeoFire(geoFireReference);

                GeoLocation location = new GeoLocation(((Guide) model).latitude, ((Guide) model).longitude);

                geoFire.setLocation(key, location);
            }
        }

        mDatabase.updateChildren(childUpdates);
        return models;
    }

    /**
     * Retrieves a data model Object from the Firebase Database
     *
     * @param type        The Firebase type corresponding to the data model to be retrieved
     * @param firebaseId  ID of the Guide to retrieve
     * @param listener    Listener to pass the instance of the Guide that was retrieved
     */
    public void getRecord(@FirebaseType final int type, final String firebaseId, @NonNull final FirebaseSingleListener listener) {

        // Initialize the child variable that will be used as the directory to retrieve the data
        // from the Firebase Database
        String child = null;

        // Set the variable according to the FirebaseType
        switch (type) {
            case GUIDE:
                child = GuideDatabase.GUIDES;
                break;

            case TRAIL:
                child = GuideDatabase.TRAILS;
                break;

            case AUTHOR:
                child = GuideDatabase.AUTHORS;
                break;

            case SECTION:
                child = GuideDatabase.SECTIONS;
                break;

            case AREA:
                child = GuideDatabase.AREAS;
                break;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }

        // Retrieve the record from the database
        mDatabase.child(child).child(firebaseId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Init the variable that will hold the values from the database
                BaseModel model;

                // Cast the data based on the input FirebaseType
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

                    default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
                }

                System.out.println("Snapshot: " + dataSnapshot);

                // Add the id to the data model
                assertNotNull(model);
                model.firebaseId = firebaseId;

                // Inform the observer that the model is ready
                listener.onDataReady(model);

                mDatabase.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Retrieves a List of Guides that were most recently added to the database
     *
     * @param listener    The listener used to pass the List of Guides returned from the database
     */
    public void getRecentGuides(final FirebaseListListener listener) {
        // Query for the most recent guides
        mDatabase.child(GuideDatabase.GUIDES)
                .orderByChild(GuideContract.GuideEntry.DATE_ADDED)
                .limitToLast(GUIDE_LIMIT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Guide> guideList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Cast the data to a Guide Object
                    Guide guide = snapshot.getValue(Guide.class);

                    // Set the Guide's firebaseId
                    assertNotNull(guide);
                    guide.firebaseId = snapshot.getKey();

                    // Add it to the List to be returned by the listener
                    guideList.add(guide);
                }

                listener.onDataReady(guideList);
                mDatabase.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mDatabase.removeEventListener(this);
            }
        });
    }

    /**
     * Removes records from the Firebase Database
     *
     * @param type    The Firebase Type of record to remove
     * @param firebaseIds     The ID of the record to remove
     */
    public void deleteRecords(@FirebaseType int type, String... firebaseIds) {
        // Init the variable to hold the path of the type to be deleted
        String child;

        switch (type) {
            case GUIDE:
                child = GuideDatabase.GUIDES;
                break;

            case TRAIL:
                child = GuideDatabase.TRAILS;
                break;

            case AUTHOR:
                child = GuideDatabase.AUTHORS;
                break;

            case SECTION:
                child = GuideDatabase.SECTIONS;
                break;

            case AREA:
                child = GuideDatabase.AREAS;
                break;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }

        // Iterate and delete each child that matches one of the ids within the path
        for (String firebaseId : firebaseIds) {
            mDatabase.child(child).child(firebaseId).removeValue();
        }
    }

    /**
     * Queries the Firebase Database for all Guides that exist in the search area
     *
     * @param location    GeoLocation Object containing the coordinates to center the search around
     * @param radius      How far from the center the search should take place
     * @param listener    GeofireListener to inform the observer which guides are in the area
     */
    public void geoQuery(GeoLocation location, double radius, final GeofireListener listener) {
        // Get a reference to the child for Geofire
        DatabaseReference firebaseRef = mDatabase.child(GEOFIRE_PATH);
        GeoFire geofire = new GeoFire(firebaseRef);

        // Use Geofire to query for all guides in the parameter area
        GeoQuery query = geofire.queryAtLocation(location, radius);
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Inform the observer that a guide is in the search radius
                listener.onKeyEntered(key);
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @VisibleForTesting
    public void deleteAllRecords() {
        mDatabase.child(GuideDatabase.GUIDES).removeValue();
        mDatabase.child(GuideDatabase.TRAILS).removeValue();
        mDatabase.child(GuideDatabase.AUTHORS).removeValue();
        mDatabase.child(GuideDatabase.SECTIONS).removeValue();
        mDatabase.child(GuideDatabase.AREAS).removeValue();
        mDatabase.child(GEOFIRE_PATH).removeValue();
    }

    public interface FirebaseSingleListener {
        void onDataReady(BaseModel model);
    }

    public interface FirebaseListListener {
        void onDataReady(List<Guide> guideList);
    }

    public interface GeofireListener {
        void onKeyEntered(String guideId);
    }
}
