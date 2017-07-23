package project.hikerguide.firebasedatabase;

import android.net.Uri;
import android.support.annotation.IntDef;
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
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.utilities.FirebaseProviderUtils;

import static junit.framework.Assert.assertNotNull;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.AREA;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.AUTHOR;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.GUIDE;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.SECTION;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.TRAIL;

/**
 * The Provider that will be used to interface with Firebase Database.
 *
 * Always access these methods on a separate thread. The thread accessing these methods will be
 * blocked until results are returned.
 *
 * * * * * * * * * * * * * * * * DO NOT USE ON UI THREAD * * * * * * * * * * * * * * * * * * * * * *
 */

public class DatabaseProvider {
    // ** Constants ** //
    private static final String TAG = DatabaseProvider.class.getSimpleName();
    private static final String GEOFIRE_PATH = "geofire";
    private static final String GUIDE_ID = "guideId";
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
    private static DatabaseProvider sProvider;
    private DatabaseReference mDatabase;

    /**
     * Private Constructor to enforce Singleton pattern
     */
    private DatabaseProvider() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }
    }

    /**
     * Instantiates a DatabaseProvider if not already instantiated and returns it
     *
     * @return The Singleton DatabaseProvider instance
     */
    public static synchronized DatabaseProvider getInstance() {
        if (sProvider == null) {
            sProvider = new DatabaseProvider();
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
    public void insertRecord(BaseModel... models) {

        // Init the Map that will be used to insert values into the Firebase Database
        Map<String, Object> childUpdates = new HashMap<>();

        // Init the directory variable that will be used to generate the path for insertion
        String directory;

        // Iterate, get the key for each record, and add the values to childUpdates
        for (BaseModel model : models) {
            // Get the directory to insert the records into from the BaseModel's class
            directory = FirebaseProviderUtils.getDirectoryFromModel(model);

            // Generate the DatabaseReference from the directory
            DatabaseReference ref = mDatabase.child(directory);

            if (directory == GuideDatabase.SECTIONS) {
                // If inserting Sections into the database, create a subdirectory using the Guide's
                // ID
                ref = ref.child(((Section) model).guideId);
            }

            // Push the path to get the key
            String key = ref.push().getKey();

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
    }

    /**
     * Retrieves a data model Object from the Firebase Database
     *
     * @param type        The Firebase type corresponding to the data model to be retrieved
     * @param firebaseId  ID of the Guide to retrieve
     * @return The model matching the firebaseId in the signature
     */
    public BaseModel getRecord(@FirebaseType final int type, final String firebaseId) {

        final DatabaseListener listener = new DatabaseListener();

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
                listener.onSuccess(model);

                mDatabase.child(directory).child(firebaseId).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure(databaseError);
                mDatabase.child(directory).child(firebaseId).removeEventListener(this);
            }
        });

        listener.pauseUntilComplete();
        return listener.getModel();
    }

    /**
     * Retrieves a List of Guides that were most recently added to the database
     *
     * @return The most recently added Guides in the Firebase Database
     */
    public Guide[] getRecentGuides() {

        final DatabaseListener listener = new DatabaseListener();

        // Query for the most recent guides
        mDatabase.child(GuideDatabase.GUIDES)
                .orderByChild(GuideContract.GuideEntry.DATE_ADDED)
                .limitToLast(GUIDE_LIMIT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Guide[] guides = (Guide[]) FirebaseProviderUtils.getModelsFromSnapshot(GUIDE, dataSnapshot);

                listener.onSuccess(guides);
                mDatabase.child(GuideDatabase.GUIDES).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure(databaseError);
                mDatabase.child(GuideDatabase.GUIDES).removeEventListener(this);
            }
        });

        listener.pauseUntilComplete();

        return (Guide[]) listener.getModels();
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
     *
     * @return The BaseModels that match the query
     */
    public BaseModel[] searchForRecords(@FirebaseType final int type, String query, int limit) {
        // Convert the query to lowercase because Firebase's sorting function is case-sensitive
        query = query.toLowerCase();

        // Make the end limit of the sorting function only return items that start with the query
        // Because "z" is highest-value character, it is added to the query so that it will return
        // all items that begin with the query.
        //
        // e.g. Query: "yose" -> End: "yosez" -> This will return all results from "yosea" to
        // "yosez" including "yosemite"
        String endString = query + "z";

        final DatabaseListener listener = new DatabaseListener();

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
                listener.onSuccess(FirebaseProviderUtils.getModelsFromSnapshot(type, dataSnapshot));
                mDatabase.child(FirebaseProviderUtils.getDirectoryFromType(type)).removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailure(databaseError);
                mDatabase.child(FirebaseProviderUtils.getDirectoryFromType(type)).removeEventListener(this);
            }
        });

        listener.pauseUntilComplete();

        return listener.getModels();
    }

    /**
     * Returns all Sections that correspond to a Guide
     *
     * @param guide       Guide for which the returned Sections will correspond to
     * @return The Sections that belong to the Guide in the signature
     */
    public Section[] getSectionsForGuide(Guide guide) {
        final DatabaseListener listener2 = new DatabaseListener();

        // Search using the guide's ID
        mDatabase.child(GuideDatabase.SECTIONS)
                .orderByChild(GUIDE_ID)
                .equalTo(guide.firebaseId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        listener2.onSuccess(FirebaseProviderUtils.getModelsFromSnapshot(SECTION, dataSnapshot));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        listener2.onFailure(databaseError);
                    }
                });

        listener2.pauseUntilComplete();

        return (Section[]) listener2.getModels();
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
        final GeoQuery query = geofire.queryAtLocation(location, radius);
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
                query.removeGeoQueryEventListener(this);
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                listener.onFailure(error);
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

//    public interface FirebaseSingleListener {
//        void onDataReady(BaseModel model);
//        void onFailure(DatabaseError databaseError);
//    }
//
//    public interface DatabaseListener {
//        void onDataReady(BaseModel[] models);
//        void onFailure(DatabaseError databaseError);
//    }

    public interface GeofireListener {
        void onKeyEntered(String guideId);
        void onFailure(DatabaseError databaseError);
    }

    public class DatabaseListener {
        // ** Member Variables ** //
        private boolean status = false;
        private boolean complete = false;
        private BaseModel model;
        private BaseModel[] models;
        private List<BaseModel> modelList;
        private Uri uri;

        /**
         * To be called when an operation is successful.
         */
        public synchronized void onSuccess() {
            // Set the member variables to reflect the status of the operation
            status = true;
            complete = true;

            models = new BaseModel[modelList.size()];
            modelList.toArray(models);

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        /**
         * To be called when an operation is successful.
         */
        public synchronized void onSuccess(BaseModel model) {
            // Set the member variables to reflect the status of the operation
            status = true;
            complete = true;
            this.model = model;

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        /**
         * To be called when an operation is successful.
         */
        public synchronized void onSuccess(BaseModel[] models) {
            // Set the member variables to reflect the status of the operation
            status = true;
            complete = true;
            this.models = models;

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        public void addModel(BaseModel model) {
            if (modelList == null) {
                modelList = new ArrayList<>();
            }

            modelList.add(model);
        }

        public synchronized void onFailure(DatabaseError e) {
            // Set the member variables to reflect the status of the operation
            status = false;
            complete = true;

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        /**
         * Pauses the calling thread until the operation is complete
         */
        public synchronized void pauseUntilComplete() {
            // Do not continue until the operation is complete
            while (!complete) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Gives the boolean status for whether the operation was successful
         *
         * @return True if successful. False if unsuccessful.
         */
        public boolean getStatus() {
            return status;
        }

        /**
         * Returns the Uri corresponding to the download link for a File
         *
         * @return Uri corresponding to the download link for a File
         */
        public Uri getDownloadUri() {
            return uri;
        }

        public BaseModel getModel(){
            return this.model;
        }

        public BaseModel[] getModels() {
            return this.models;
        }
    }
}
