package project.hikerguide;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import project.hikerguide.data.GuideDatabase;
import project.hikerguide.data.GuideProvider;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Alvin on 7/17/2017.
 */

@RunWith(AndroidJUnit4.class)
public class ContentProviderTest {
    // ** Member Variables ** //
    Context context;

    /**
     * Gets a references to the Context used to access the ContentProvider and ensures that the
     * database is created before each test
     */
    @Before
    public void setUp() {
        // Get reference to Context
        context = InstrumentationRegistry.getContext();

        // Delete the database if it exists
        context.deleteDatabase(GuideDatabase.DATABASE_NAME);
    }

    /**
     * Tests insertions and queries using the ContentProvider
     */
    @Test
    public void testDatabaseInsertion() {
        // Area Table
        // Get the ContentValues and URI for insertion
        ContentValues areaValues = TestUtilities.getAreaValues();
        Uri areaUri = GuideProvider.Areas.CONTENT_URI;

        // Test the insertion to ensure that it returns a valid URI
        insertContentValues(areaValues, areaUri);

        // Test the query to ensure the database contains the same values that were inserted
        validateInsertion(areaValues, areaUri);

        // Author Table
        ContentValues authorValues = TestUtilities.getAuthorValues();
        Uri authorUri = GuideProvider.Authors.CONTENT_URI;

        insertContentValues(authorValues, authorUri);
        validateInsertion(authorValues, authorUri);

        // Trail Table
        ContentValues trailValues = TestUtilities.getTrailValues();
        Uri trailUri = GuideProvider.Trails.CONTENT_URI;

        insertContentValues(trailValues, trailUri);
        validateInsertion(trailValues, trailUri);

        // Guide Table
        ContentValues guideValues = TestUtilities.getGuideValues();
        Uri guideUri = GuideProvider.Guides.CONTENT_URI;

        insertContentValues(guideValues, guideUri);
        validateInsertion(guideValues, guideUri);

        // Section Table
        ContentValues sectionValues = TestUtilities.getSectionValues();
        Uri sectionUri = GuideProvider.Sections.CONTENT_URI;

        insertContentValues(sectionValues, sectionUri);
        validateInsertion(sectionValues, sectionUri);
    }

    /**
     * Tests that the joined table query is valid
     */
    @Test
    public void testQueryJoinedTable() {
        // Insert values into the database
        insertContentValues(TestUtilities.getAreaValues(), GuideProvider.Areas.CONTENT_URI);
        insertContentValues(TestUtilities.getAuthorValues(), GuideProvider.Authors.CONTENT_URI);
        insertContentValues(TestUtilities.getTrailValues(), GuideProvider.Trails.CONTENT_URI);
        insertContentValues(TestUtilities.getGuideValues(), GuideProvider.Guides.CONTENT_URI);
        insertContentValues(TestUtilities.getSectionValues(), GuideProvider.Sections.CONTENT_URI);

        // Query the database to see if the joined table works correctly
        Uri joinedUri = GuideProvider.Guides.withId(1);
        Cursor cursor = context.getContentResolver().query(
                joinedUri,
                null,
                null,
                null,
                null);

        // Assert the the Cursor is not null
        String errorNullCursor = "Cursor is null. Check the query is valid.";
        assertNotNull(errorNullCursor, cursor);

        try {
            // Assert that the Cursor has valid entries
            String emptyCursor = "Cursor does not contain any entries.";
            assertTrue(emptyCursor, cursor.moveToFirst());

            // Assert the Cursor contains as many columns as all the joined tables combined
            assertEquals(26, cursor.getColumnCount());
        } finally {
            // Close the Cursor
            cursor.close();
        }
    }

    /**
     * Inserts a ContentValues into the database and checks to ensure it returns valid URI
     * @param values          ContentValues to insert using the ContentProvider
     * @param insertionUri    URI target for the insertion
     */
    public void insertContentValues(ContentValues values, Uri insertionUri) {
        // Insert the ContentValues using the ContentProvider
        Uri returnUri = context.getContentResolver().insert(
                insertionUri,
                values);

        // Assert that the operation returned a valid URI
        String errorInserting = "No URI was returned with the insertion. Values were not inserted into database.";
        assertNotNull(errorInserting, returnUri);
    }

    /**
     * Checks that the values read from the database are the same as the ContentValues that were
     * inserted
     * @param values          ContentValues to check the database values against
     * @param insertionUri    URI to query the database
     */
    public void validateInsertion(ContentValues values, Uri insertionUri) {
        // Query the database
        Cursor cursor = context.getContentResolver().query(
                insertionUri,
                null,
                null,
                null,
                null);

        // Assert the the Cursor is not null
        String errorNullCursor = "Cursor is null. Check the query is valid.";
        assertNotNull(errorNullCursor, cursor);

        try {
            // Assert that the Cursor has valid entries
            String emptyCursor = "Cursor does not contain any entries.";
            assertTrue(emptyCursor, cursor.moveToFirst());

            // Check the values of the database against the ContentValues used to insert those values
            TestUtilities.validateCursorValues(cursor, values);
        } finally {
            // Close the Cursor
            cursor.close();
        }
    }

    /**
     * Deletes the database
     */
    @After
    public void cleanUp() {
        context.deleteDatabase(GuideDatabase.DATABASE_NAME);
    }
}
