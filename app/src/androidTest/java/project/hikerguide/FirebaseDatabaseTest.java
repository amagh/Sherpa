package project.hikerguide;

import android.support.test.runner.AndroidJUnit4;

import com.firebase.geofire.GeoLocation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import project.hikerguide.firebasedatabase.FirebaseProvider;
import project.hikerguide.models.Area;
import project.hikerguide.models.Author;
import project.hikerguide.models.BaseModel;
import project.hikerguide.models.Guide;
import project.hikerguide.models.Section;
import project.hikerguide.models.Trail;

import static junit.framework.Assert.assertEquals;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.AREA;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.AUTHOR;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.GUIDE;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.SECTION;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.TRAIL;

/**
 * Created by Alvin on 7/17/2017.
 */

@RunWith(AndroidJUnit4.class)
public class FirebaseDatabaseTest {
    // ** Member Variables ** //
    FirebaseProvider mWriter;

    @Before
    public void setup() {
        // Store the reference to the FirebaseProvider as a mem var
        mWriter = FirebaseProvider.getInstance();
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

        for (int type : types) {
            // Retrieve the Guide from the FirebaseDatabase using the guide's id
            mWriter.getRecord(type, 1, new FirebaseProvider.FirebaseSingleListener() {
                @Override
                public void onDataReady(BaseModel model) {
                    // Assert that all values from the returned Guide are equal to the inserted Guide's
                    // values
                    if (model instanceof Guide) {
                        TestUtilities.validateModelValues(guide, model);
                    } else if (model instanceof Trail) {
                        TestUtilities.validateModelValues(trail, model);
                    } else if (model instanceof Author) {
                        TestUtilities.validateModelValues(author, model);
                    } else if (model instanceof Section) {
                        TestUtilities.validateModelValues(section, model);
                    } else if (model instanceof Area) {
                        TestUtilities.validateModelValues(area, model);
                    }
                }
            });
        }
    }

    @Test
    public void testGeoQuery() {
        // Insert the Guide into the Database - also inserts the guide's coordinates to the GeoFire
        // child of the Database
        final Guide guide = insertGuide();

        // Run a GeoQuery for the lat/long of the inserted Guide
        mWriter.geoQuery(new GeoLocation(guide.latitude, guide.longitude), 1, new FirebaseProvider.GeofireListener() {
            @Override
            public void onKeyEntered(long guideId) {
                // Ensure that the query returns the inserted Guide's ID
                String errorIncorrectGuideId = "The GeoQuery did not return the correct guide id";
                assertEquals(errorIncorrectGuideId, guide.id, guideId);
            }
        });
    }

    @Test
    public void testGetRecordList() {
        // Generate an Array of Guides to insert in the database
        final Guide[] guides = TestUtilities.getGuides();

        // Insert
        mWriter.insertRecord(guides);

        // Query for the latest guides
        mWriter.getRecentGuides(new FirebaseProvider.FirebaseListListener() {
            @Override
            public void onDataReady(List<Guide> guideList) {
                // Validate each returned Guide against the guides inserted
                for (int i = 0; i < guideList.size(); i++) {
                    TestUtilities.validateModelValues(guides[i], guideList.get(i));
                }
            }
        });
    }

    @Test
    public void testDeleteRecords() {
        // Get an Array of guides
        Guide[] guides = TestUtilities.getGuides();

        // Get an array of the ids of each of the guides
        long[] ids = new long[guides.length];
        for (int i = 0; i < guides.length; i++) {
            ids[i] = guides[i].id;
        }

        // Insert the guides into the database
        mWriter.insertRecord(guides);

        // Delete all the records from the database
        mWriter.deleteRecords(GUIDE, ids);

        // Check to ensure that there are no guides left in the database
        mWriter.getRecentGuides(new FirebaseProvider.FirebaseListListener() {
            @Override
            public void onDataReady(List<Guide> guideList) {
                // Verify that there are no guides returned
                String errorNotEmpty = "The size of the returned guide list is greater than 0";
                assertEquals(errorNotEmpty, 0, guideList.size());
            }
        });
    }

    public Guide insertGuide() {
        Guide guide = TestUtilities.getGuide();
        mWriter.insertRecord(guide);

        return guide;
    }

    public Trail insertTrail() {
        Trail trail = TestUtilities.getTrail();
        mWriter.insertRecord(trail);

        return trail;
    }

    public Author insertAuthor() {
        Author author = TestUtilities.getAuthor();
        mWriter.insertRecord(author);

        return author;
    }

    public Section insertSection() {
        Section section = TestUtilities.getSection();
        mWriter.insertRecord(section);

        return section;
    }

    public Area insertArea() {
        Area area = TestUtilities.getArea();
        mWriter.insertRecord(area);

        return area;
    }
    @After
    public void cleanup() {
        // Delete the test guide that was previously uploaded
    }
}
