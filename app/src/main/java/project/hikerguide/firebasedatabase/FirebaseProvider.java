package project.hikerguide.firebasedatabase;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    /**
     * Inserts a record into the Firebase Database
     *
     * @param models    The data model Object describing the information to add to the database
     */
    public void insertRecord(BaseModel... models) {

        // Iterate and insert each record into the database
        for (BaseModel model : models) {
            if (model instanceof Guide) {
                Guide guide = (Guide) model;
                mDatabase.child(GuideDatabase.GUIDES).child(Long.toString(guide.id)).setValue(guide);

                // When inserting a Guide object, there needs to be accompanying coordinate data
                // loaded into the Database for GeoFire queries
                DatabaseReference geoFireReference = mDatabase.child(GEOFIRE_PATH);
                GeoFire geoFire = new GeoFire(geoFireReference);

                GeoLocation location = new GeoLocation(((Guide) model).latitude, ((Guide) model).longitude);

                geoFire.setLocation(Long.toString(model.id), location);
            } else if (model instanceof Trail) {
                Trail trail = (Trail) model;
                mDatabase.child(GuideDatabase.TRAILS).child(Long.toString(trail.id)).setValue(trail);
            } else if (model instanceof Author) {
                Author author = (Author) model;
                mDatabase.child(GuideDatabase.AUTHORS).child(Long.toString(author.id)).setValue(author);
            } else if (model instanceof Section) {
                Section section = (Section) model;
                mDatabase.child(GuideDatabase.SECTIONS).child(Long.toString(section.id)).setValue(section);
            } else if (model instanceof Area) {
                Area area = (Area) model;
                mDatabase.child(GuideDatabase.AREAS).child(Long.toString(area.id)).setValue(area);
            }
        }
    }

    /**
     * Retrieves a data model Object from the Firebase Database
     *
     * @param type        The Firebase type corresponding to the data model to be retrieved
     * @param id          ID of the Guide to retrieve
     * @param listener    Listener to pass the instance of the Guide that was retrieved
     */
    public void getRecord(@FirebaseType final int type, final long id, @NonNull final FirebaseListener listener) {

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
        mDatabase.child(child).child(Long.toString(id)).addListenerForSingleValueEvent(new ValueEventListener() {
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

                // Add the id to the data model
                assertNotNull(model);
                model.id = id;

                // Inform the observer that the model is ready
                listener.onDataReady(model);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Deletes a Guide from the Firebase Database
     *
     * @param id    ID of the Guide to remove from the Firebase Database
     */
    public void deleteGuide(long id) {
        mDatabase.child(GuideDatabase.GUIDES).child(Long.toString(id)).removeValue();
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
                listener.onKeyEntered(Long.parseLong(key));
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

    public interface FirebaseListener {
        void onDataReady(BaseModel model);
    }

    public interface GeofireListener {
        void onKeyEntered(long guideId);
    }
}
