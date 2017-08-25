package project.hikerguide;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Rating;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static android.test.MoreAsserts.assertEmpty;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.AREA;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.AUTHOR;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.GUIDE;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.SECTION;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.TRAIL;
import static project.hikerguide.utilities.FirebaseProviderUtils.RATING_DIRECTORY;

/**
 * Created by Alvin on 7/17/2017.
 */

@RunWith(AndroidJUnit4.class)
public class FirebaseDatabaseTest {
    // ** Member Variables ** //
    Context mContext;
    DatabaseProvider mDatabase;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();

        // Store the reference to the DatabaseProvider as a mem var
        mDatabase = DatabaseProvider.getInstance();
        mDatabase.deleteAllRecords();
    }

    @Test
    public void testInsertRecords() {
        // Insert each data model into the Firebase Database
        final Guide guide = insertGuide();
        final Trail trail = insertTrail();
        final Author author = insertAuthor();
        final Section section = insertSection();
        final Area area = insertArea();

        int[] types = {GUIDE, TRAIL, AUTHOR, SECTION, AREA};
        BaseModel[] models = {guide, trail, author, section, area};

        for (int i = 0; i < types.length; i++) {

            BaseModel model = null;

            // Retrieve the Model from the FirebaseDatabase using the model's firebaseId
            if (models[i] instanceof Section) {
                FirebaseDatabase.getInstance().getReference()
                        .child(GuideDatabase.SECTIONS)
                        .child(((Section) models[i]).guideId)
                        .child(models[i].firebaseId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Section model = (Section) FirebaseProviderUtils.getModelFromSnapshot(SECTION, dataSnapshot);
                                TestUtilities.validateModelValues(section, model);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                // Ensure that the validation has time to process
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                model = mDatabase.getRecord(types[i], models[i].firebaseId);
            }


            // Assert that all values from the returned Guide are equal to the inserted Guide's
            // values
            if (model instanceof Guide) {
                TestUtilities.validateModelValues(guide, model);
            } else if (model instanceof Trail) {
                TestUtilities.validateModelValues(trail, model);
            } else if (model instanceof Author) {
                TestUtilities.validateModelValues(author, model);
            } else if (model instanceof Area) {
                TestUtilities.validateModelValues(area, model);
            }
        }
    }

    @Test
    public void testGeoQuery() {
        // Insert the Guide into the Database - also inserts the guide's coordinates to the GeoFire
        // child of the Database
        final Guide guide = insertGuide();

        // Run a GeoQuery for the lat/long of the inserted Guide
        mDatabase.geoQuery(new GeoLocation(guide.latitude, guide.longitude), 1, new DatabaseProvider.GeofireListener() {
            @Override
            public void onKeyEntered(String guideId) {
                // Ensure that the query returns the inserted Guide's ID
                String errorIncorrectGuideId = "The GeoQuery did not return any ids";
                assertThat(errorIncorrectGuideId, guideId, not(isEmptyOrNullString()));
            }

            @Override
            public void onFailure(DatabaseError databaseError) {
                assertNull(databaseError.getMessage(), databaseError);
            }
        });
    }

    @Test
    public void testGetRecordList() {
        // Generate an Array of Guides to insert in the database
        final Guide[] guides = TestUtilities.getGuides();

        // Insert
        mDatabase.insertRecord(guides);

        // Query for the latest guides
        Guide[] returnedGuides = mDatabase.getRecentGuides();

        for (Guide returnedGuide : returnedGuides) {
            for (Guide guide : returnedGuides) {
                if (guide.firebaseId.equals(returnedGuide.firebaseId)) {
                    TestUtilities.validateModelValues(guide, returnedGuide);
                }
            }
        }
    }

    @Test
    public void testDeleteRecords() {
        // Get an Array of guides
        Guide[] guides = TestUtilities.getGuides();

        // Insert the guides into the database
        mDatabase.insertRecord(guides);

        // Get an array of the ids of each of the guides
        String[] firebaseIds = new String[guides.length];
        for (int i = 0; i < guides.length; i++) {
            firebaseIds[i] = guides[i].firebaseId;
        }

        // Delete all the records from the database
        mDatabase.deleteRecords(GUIDE, firebaseIds);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check to ensure that there are no guides left in the database
        BaseModel[] models = mDatabase.getRecentGuides();

        // Verify that there are no guides returned
        String errorNotEmpty = "The size of the returned guide list is greater than 0";
        assertEquals(errorNotEmpty, 0, models.length);
    }

    @Test
    public void testSearch() {
        Area[] areas = TestUtilities.getAreas();
        mDatabase.insertRecord(areas);

        BaseModel[] models = mDatabase.searchForRecords(AREA, "Grand", 5);

        Set<String> expectedNames = new HashSet();
        expectedNames.add("Grand Canyon");
        expectedNames.add("Grand Teton");

        for (BaseModel model : models) {
            expectedNames.remove(((Area) model).name);
        }

        String errorWrongNames = "The search query did not return all the names expected.";
        assertEmpty(errorWrongNames, expectedNames);
    }

    @Test
    public void testRating() {

        // Generate the Guide and Author
        final Guide guide = TestUtilities.getGuide1(mContext);
        final Author author = TestUtilities.getAuthor1(mContext);

        // Generate the Rating
        final Rating rating = new Rating();
        rating.setGuideId(guide.firebaseId);
        rating.setComment("Test Comment");
        rating.setRating(5);
        rating.setAuthorName(author.name);
        rating.setAuthorId(author.firebaseId);
        rating.addDate();

        // Object lock to ensure the test runs to completion
        final AtomicBoolean check = new AtomicBoolean(false);

        // Insert the rating into Firebase
        FirebaseProviderUtils.updateRating(rating, 0);

        // Query for the Rating by GuideId
        final Query queryByGuide = FirebaseDatabase.getInstance().getReference()
                .child(RATING_DIRECTORY)
                .orderByChild(GuideContract.SectionEntry.GUIDE_ID)
                .equalTo(rating.getGuideId());

        queryByGuide.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            // Verify the returned Rating matches the inserted Rating
                            Rating returnedRating = dataSnapshot.getValue(Rating.class);
                            returnedRating.firebaseId = snapshot.getKey();
                            TestUtilities.validateModelValues(rating, returnedRating);
                        }

                        queryByGuide.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        queryByGuide.removeEventListener(this);
                    }
                });

        // Query for the Rating by the AuthorId
        final Query queryByAuthor = FirebaseDatabase.getInstance().getReference()
                .child(GuideContract.GuideEntry.AUTHOR_ID)
                .orderByChild(rating.getAuthorId())
                .equalTo(rating.getAuthorId());

        queryByAuthor.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    // Verify the returned Rating matches the inserted Rating
                    Rating returnedRating = dataSnapshot.getValue(Rating.class);
                    returnedRating.firebaseId = snapshot.getKey();
                    TestUtilities.validateModelValues(rating, returnedRating);
                }

                // Allow the test to proceed
                synchronized (check) {
                    check.set(true);
                    check.notifyAll();
                }

                queryByAuthor.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                queryByAuthor.removeEventListener(this);
            }
        });

        // Wait until the Ratings are verified before proceeding
        synchronized (check) {
            while (!check.get()) {
                try {
                    check.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Guide insertGuide() {
        Guide guide = TestUtilities.getGuide1(mContext);
        mDatabase.insertRecord(guide);

        return guide;
    }

    public Trail insertTrail() {
        Trail trail = TestUtilities.getTrail1();
        mDatabase.insertRecord(trail);

        return trail;
    }

    public Author insertAuthor() {
        Author author = TestUtilities.getAuthor1(mContext);
        mDatabase.insertRecord(author);

        return author;
    }

    public Section insertSection() {
        Section section = TestUtilities.getSection();
        mDatabase.insertRecord(section);

        return section;
    }

    public Area insertArea() {
        Area area = TestUtilities.getArea1();
        mDatabase.insertRecord(area);

        return area;
    }

    @After
    public void cleanup() {
        // Delete the test guide that was previously uploaded
        mDatabase.deleteAllRecords();
    }
}
