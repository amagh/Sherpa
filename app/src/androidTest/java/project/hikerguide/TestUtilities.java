package project.hikerguide;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Set;

import project.hikerguide.data.GuideContract;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by Alvin on 7/17/2017.
 */

public class TestUtilities {

    /**
     * Checks values from a Cursor to match the values in the ContentValues
     *
     * @param cursor    Cursor containing database values
     * @param values    ContentValues from which the database values are to be matched against
     */
    public static void validateCursorValues(Cursor cursor, ContentValues values) {
        String nullCursorError = "Cursor is null. Is ContentProvider registered in " +
                "AndroidManifest.xml?";
        assertNotNull(nullCursorError, cursor);

        Set<String> keySet = values.keySet();

        for (String key : keySet) {
            int columnIndex = cursor.getColumnIndex(key);

            String columnNotFoundError = key + " column not found";
            assertFalse(columnNotFoundError, columnIndex == -1);

            String expectedValue = values.getAsString(key);
            String cursorValue = cursor.getString(columnIndex);

            String matchError = "Expected value: " + expectedValue +
                    " does not match actual value: " + cursorValue;

            assertEquals(matchError, expectedValue, cursorValue);
        }
    }

    // Getters for dummy values to insert into the database

    public static ContentValues getAreaValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.AreaEntry.NAME, "Yosemite");

        return values;
    }

    public static ContentValues getAuthorValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.AuthorEntry.NAME, "John Muir");
        values.put(GuideContract.AuthorEntry.PROFILE_PICTURE, "Firebase Profile URI");
        values.put(GuideContract.AuthorEntry.SCORE, 100);

        return values;
    }

    public static ContentValues getTrailValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.TrailEntry.AREA_ID, 1);
        values.put(GuideContract.TrailEntry.NAME, "Four Mile Trail");
        values.put(GuideContract.TrailEntry.NOTES, "Temporarily Closed");

        return values;
    }

    public static ContentValues getGuideValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.GuideEntry.AUTHOR_ID, 1);
        values.put(GuideContract.GuideEntry.TRAIL_ID, 1);
        values.put(GuideContract.GuideEntry.DATE_ADDED, System.currentTimeMillis());
        values.put(GuideContract.GuideEntry.GPX, "Firebase GPX URI");
        values.put(GuideContract.GuideEntry.IMAGE, "Firebase Image URI");
        values.put(GuideContract.GuideEntry.LATITUDE, 37.734);
        values.put(GuideContract.GuideEntry.LONGITUDE, -119.602);
        values.put(GuideContract.GuideEntry.RATING, 5);
        values.put(GuideContract.GuideEntry.REVIEWS, 1);

        return values;
    }

    public static ContentValues getSectionValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.SectionEntry.GUIDE_ID, 1);
        values.put(GuideContract.SectionEntry.SECTION, 1);
        values.put(GuideContract.SectionEntry.CONTENT, "Description of the hike");

        return values;
    }
}
