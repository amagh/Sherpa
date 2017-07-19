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

import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.models.BaseModel;
import project.hikerguide.models.Guide;
import project.hikerguide.utilities.FirebaseProviderUtils;

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

    private static String getFirebasePath(String directory, String key) {
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
            // Get the directory to insert the records into from the BaseModel's class
            directory = FirebaseProviderUtils.getDirectoryFromModel(model);

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
        final String directory = FirebaseProviderUtils.getDirectoryFromType(type);

        // Retrieve the record from the database
        mDatabase.child(directory).child(firebaseId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Init the variable that will hold the values from the database
                BaseModel model = FirebaseProviderUtils.getModelFromSnapshot(type, dataSnapshot);

                // Inform the observer that the model is ready
                listener.onDataReady(model);

                mDatabase.child(directory).child(firebaseId).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mDatabase.child(directory).child(firebaseId).removeEventListener(this);
            }
        });
    }

    /**
     * Retrieves a List of Guides that were most recently added to the database
     *
     * @param listener    The listener used to pass the List of Guides returned from the database
     */
    public void getRecentGuides(final FirebaseListener listener) {
        // Query for the most recent guides
        mDatabase.child(GuideDatabase.GUIDES)
                .orderByChild(GuideContract.GuideEntry.DATE_ADDED)
                .limitToLast(GUIDE_LIMIT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Guide[] guides = (Guide[]) FirebaseProviderUtils.getModelsFromSnapshot(GUIDE, dataSnapshot);

                listener.onDataReady(guides);
                mDatabase.child(GuideDatabase.GUIDES).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mDatabase.child(GuideDatabase.GUIDES).removeEventListener(this);
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
        String directory = FirebaseProviderUtils.getDirectoryFromType(type);

        // Iterate and delete each child that matches one of the ids within the path
        for (String firebaseId : firebaseIds) {
            mDatabase.child(directory).child(firebaseId).removeValue();
        }
    }

    /**
     * Searches for a FirebaseType for the name given the query.
     *
     * @param type        The FirebaseType to query for
     * @param query       The input String to search for
     * @param limit       The max number of items to return
     * @param listener    Notifies the observer of the results
     */
    public void searchForRecords(@FirebaseType final int type, String query, int limit, final FirebaseListener listener) {
        // Convert the query to lowercase because Firebase's sorting fucntion is case-sensitive
        query = query.toLowerCase();

        // Make the end limit of the sorting function only return items that start with the query
        // Because "z" is highest-value character, it is added to the query so that it will return
        // all items that begin with the query.
        //
        // e.g. Query: "yose" -> End: "yosez" -> This will return all results from "yosea" to
        // "yosez" including "yosemite"
        String endString = query + "z";

        // Query the database, ordering by the lowerCaseName key
        mDatabase.child(FirebaseProviderUtils.getDirectoryFromType(type))
                .orderByChild("lowerCaseName")
                .startAt(query)
                .endAt(endString)
                .limitToFirst(limit)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Notify the observer of the results
                listener.onDataReady(FirebaseProviderUtils.getModelsFromSnapshot(type, dataSnapshot));
                mDatabase.child(FirebaseProviderUtils.getDirectoryFromType(type)).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mDatabase.child(FirebaseProviderUtils.getDirectoryFromType(type)).removeEventListener(this);
            }
        });
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

    public interface FirebaseListener {
        void onDataReady(BaseModel[] models);
    }

    public interface GeofireListener {
        void onKeyEntered(String guideId);
    }
}
