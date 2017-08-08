package project.hikerguide;

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.utilities.ContentProviderUtils;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Alvin on 8/8/2017.
 */

@RunWith(AndroidJUnit4.class)
public class ContentProviderUtilTest {
    // ** Member Variables ** //
    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
        mContext.deleteDatabase(GuideDatabase.DATABASE_NAME);
    }

    @Test
    public void testInsertGuide() {

        // Init the Guide to be inserted
        Guide guide = TestUtilities.getGuide1(mContext);

        // Insert the Guide
        ContentProviderUtils.insertModel(mContext, guide);

        // Query the database for the Guide
        Cursor cursor = mContext.getContentResolver().query(
                GuideProvider.Guides.CONTENT_URI,
                null,
                GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                new String[] {guide.firebaseId},
                null);

        // Check to ensure that the Cursor is not null
        String nullCursor = "Error querying the database for the inserted Guide.";
        assertNotNull(nullCursor, cursor);

        // Check that the Cursor contains an entry
        String errorEmptyCursor = "Returned Cursor is empty. Guide not properly inserted into " +
                "database.";
        assertTrue(errorEmptyCursor, cursor.moveToFirst());

        // Convert the Cursor to a Guide
        Guide dbGuide = Guide.createGuideFromCursor(cursor);

        // Validate the Guide against the inserted Guide
        TestUtilities.validateModelValues(guide, dbGuide);

        // Close the Cursor
        cursor.close();
    }

    @Test
    public void testInsertTrail() {

        // Init the Trail to be inserted
        Trail trail = TestUtilities.getTrail1();

        // Insert the Trail into the database
        ContentProviderUtils.insertModel(mContext, trail);

        // Query the database for the inserted Trail
        Cursor cursor = mContext.getContentResolver().query(
                GuideProvider.Trails.CONTENT_URI,
                null,
                GuideContract.TrailEntry.FIREBASE_ID + " = ?",
                new String[] {trail.firebaseId},
                null);

        // Check to ensure that the Cursor is not null
        String nullCursor = "Error querying the database for the inserted Guide.";
        assertNotNull(nullCursor, cursor);

        // Check that the Cursor contains an entry
        String errorEmptyCursor = "Returned Cursor is empty. Guide not properly inserted into " +
                "database.";
        assertTrue(errorEmptyCursor, cursor.moveToFirst());

        // Convert the Cursor to a Trail
        Trail dbTrail = Trail.createTrailFromCursor(cursor);

        // Validate the Trail against the inserted Trail
        TestUtilities.validateModelValues(trail, dbTrail);

        // Close the Cursor
        cursor.close();
    }

    @Test
    public void testInsertAuthor() {

        // Init the Author to be inserted
        Author author = TestUtilities.getAuthor1(mContext);

        // Insert the Author
        ContentProviderUtils.insertModel(mContext, author);

        // Query the database for the inserted Author
        Cursor cursor = mContext.getContentResolver().query(
                GuideProvider.Authors.CONTENT_URI,
                null,
                GuideContract.AuthorEntry.FIREBASE_ID + " = ?",
                new String[] {author.firebaseId},
                null);

        // Check to ensure that the Cursor is not null
        String nullCursor = "Error querying the database for the inserted Guide.";
        assertNotNull(nullCursor, cursor);

        // Check that the Cursor contains an entry
        String errorEmptyCursor = "Returned Cursor is empty. Guide not properly inserted into " +
                "database.";
        assertTrue(errorEmptyCursor, cursor.moveToFirst());

        // Convert the Cursor to a Author
        Author dbAuthor = Author.createAuthorFromCursor(cursor);

        // Validate the Author against the inserted Author
        TestUtilities.validateModelValues(author, dbAuthor);

        // Close the Cursor
        cursor.close();
    }

    @Test
    public void testInsertArea() {

        // Init the Area to be inserted
        Area area = TestUtilities.getArea1();

        // Insert the Area
        ContentProviderUtils.insertModel(mContext, area);

        // Query the database for the inserted Area
        Cursor cursor = mContext.getContentResolver().query(
                GuideProvider.Areas.CONTENT_URI,
                null,
                GuideContract.AreaEntry.FIREBASE_ID + " = ?",
                new String[] {area.firebaseId},
                null);

        // Check to ensure that the Cursor is not null
        String nullCursor = "Error querying the database for the inserted Guide.";
        assertNotNull(nullCursor, cursor);

        // Check that the Cursor contains an entry
        String errorEmptyCursor = "Returned Cursor is empty. Guide not properly inserted into " +
                "database.";
        assertTrue(errorEmptyCursor, cursor.moveToFirst());

        // Convert the Cursor to a Area
        Area dbArea = Area.createAreaFromCursor(cursor);

        // Validate the Area against the inserted Area
        TestUtilities.validateModelValues(area, dbArea);

        // Close the Cursor
        cursor.close();
    }
}
