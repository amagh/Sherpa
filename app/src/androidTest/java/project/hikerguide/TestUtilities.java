package project.hikerguide;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.Set;

import project.hikerguide.data.GuideContract;
import project.hikerguide.models.Area;
import project.hikerguide.models.Author;
import project.hikerguide.models.BaseModel;
import project.hikerguide.models.Guide;
import project.hikerguide.models.Section;
import project.hikerguide.models.Trail;

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

    /**
     * Checks that the values of the data model returned has the same values as the data model that
     * was inserted in the Firebase Database
     *
     * @param expected    The data model inserted into the Firebase Database
     * @param returned    The data model that was returned from the Firebase Database
     */
    public static void validateModelValues(BaseModel expected, BaseModel returned) {
        // Get all the Fields of the data model
        Field[] fields = expected.getClass().getFields();

        // Iterate through and ensure each Field value is equal
        for (Field field : fields) {
            String errorValueDifferent = "Values in the returned data model (" + returned.firebaseId + ") do not match the values inserted (" + expected.firebaseId + "). " + field.toString();
            try {
                System.out.println("Field: " + field.toString() + " | Expected: " + field.get(expected) + " | Actual: " + field.get(returned));
                assertEquals(errorValueDifferent, field.get(expected), field.get(returned));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
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

    public static Guide getGuide() {
        return new Guide(1, 1, 1, System.currentTimeMillis(), "Firebase GPX URI", 37.734, -119.602, "Firebase Image URI");
    }

    public static Guide[] getGuides() {
        Guide guide1 = new Guide(1, 1, 1, System.currentTimeMillis(), "Firebase1 GPX URI", 37.734, -119.602, "Firebase Image URI");
        Guide guide2 = new Guide(2, 1, 1, System.currentTimeMillis(), "Firebase2 GPX URI", 37.735, -119.603, "Firebase Image URI");
        Guide guide3 = new Guide(3, 1, 1, System.currentTimeMillis(), "Firebase3 GPX URI", 37.736, -119.604, "Firebase Image URI");
        Guide guide4 = new Guide(4, 1, 1, System.currentTimeMillis(), "Firebase4 GPX URI", 37.737, -119.605, "Firebase Image URI");
        Guide guide5 = new Guide(5, 1, 1, System.currentTimeMillis(), "Firebase5 GPX URI", 37.738, -119.606, "Firebase Image URI");
        Guide guide6 = new Guide(6, 1, 1, System.currentTimeMillis(), "Firebase6 GPX URI", 37.739, -119.607, "Firebase Image URI");
        Guide guide7 = new Guide(7, 1, 1, System.currentTimeMillis(), "Firebase7 GPX URI", 37.730, -119.608, "Firebase Image URI");
        Guide guide8 = new Guide(8, 1, 1, System.currentTimeMillis(), "Firebase8 GPX URI", 37.731, -119.609, "Firebase Image URI");
        Guide guide9 = new Guide(9, 1, 1, System.currentTimeMillis(), "Firebase9 GPX URI", 37.732, -119.600, "Firebase Image URI");

        Guide[] guides = {guide1, guide2, guide3, guide4, guide5, guide6, guide7, guide8, guide9};
        return guides;
    }

    public static Trail getTrail() {
        return new Trail(1, 1, "Four Mile Trail", "Temporarily closed");
    }

    public static Author getAuthor() {
        return new Author(1, "John Muir", "Firebase Profile URI", 0);
    }

    public static Section getSection() {
        return new Section(1, 1, 1, "Description of hike");
    }

    public static Area getArea() {
        return new Area(1, "Yosemite");
    }
}
