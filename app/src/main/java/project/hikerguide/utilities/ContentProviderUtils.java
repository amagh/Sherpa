package project.hikerguide.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import timber.log.Timber;

/**
 * Created by Alvin on 8/7/2017.
 */

public class ContentProviderUtils {

    /**
     * Inserts a model into the database
     *
     * @param context    Interface to global Context
     * @param model      Model to be inserted into the database
     */
    public static void insertModel(Context context, BaseModel model) {

        // Insert the values into the database
        context.getContentResolver().insert(

                // Retrieve the Uri for the model
                getUriForModel(model),

                // Build the ContentValues for the model
                getContentValuesForModel(model));
    }

    /**
     * Bulk inserts an Array of Sections into the database
     *
     * @param context     Interface to global Context
     * @param sections    Array of Sections to be inserted into the database
     */
    public static void bulkInsertSections(Context context, Section... sections) {

        // Init the Array of ContentValues to be bulk inserted
        ContentValues[] sectionValues = new ContentValues[sections.length];

        // Iterate through the Sections and create ContentValues from them
        for (int i = 0; i < sections.length; i++) {
            Section section = sections[i];
            sectionValues[i] = getValuesForSection(section);
        }

        // Bulk insert the values into the database
        context.getContentResolver().bulkInsert(
                GuideProvider.Sections.CONTENT_URI,
                sectionValues);
    }

    /**
     * Removes a model from the database, using its associated FirebaseId
     *
     * @param context    Interface to global Context
     * @param model      Model to be removed from the database
     */
    public static void deleteModel(Context context, BaseModel model) {
        context.getContentResolver().delete(
                getUriForModel(model),
                GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                new String[] {model.firebaseId});
    }

    /**
     * Removes all Sections for a given Guide from the database
     *
     * @param context    Interface to global Context
     * @param guide      Guide whose associated Sections are to be removed
     */
    public static void deleteSectionsForGuide(Context context, Guide guide) {
        context.getContentResolver().delete(
                GuideProvider.Sections.CONTENT_URI,
                GuideContract.SectionEntry.GUIDE_ID + " = ?",
                new String[] {guide.firebaseId});
    }

    /**
     * Counts the number of Guides in the local database authored by the Author
     *
     * @param context    Interface to global Context
     * @param author     Author to count the number of Guides for
     * @return The number of Guides authored by the Author that exist in the local database
     */
    public static int getGuideCountForAuthor(Context context, Author author) {

        // Query the database for Guides authored by the Author
        Cursor cursor = context.getContentResolver().query(
                GuideProvider.Guides.CONTENT_URI,
                null,
                GuideContract.GuideEntry.AUTHOR_ID + " = ?",
                new String[] {author.firebaseId},
                null);

        // Return the Cursor count
        try {
            if (cursor != null) {
                return cursor.getCount();
            } else {
                return 0;
            }
        } finally {

            // Close the Cursor
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Counts the number of Trails in the database using an Area
     *
     * @param context    Interface to global Context
     * @param area       Area to count the number of associated Trails for
     * @return The number of Trails in the database associated with an Area
     */
    public static int getTrailCountForArea(Context context, Area area) {

        // Query the database for Trails associated with the Area
        Cursor cursor = context.getContentResolver().query(
                GuideProvider.Trails.CONTENT_URI,
                null,
                GuideContract.TrailEntry.AREA_ID + " = ?",
                new String[] {area.firebaseId},
                null);

        // Return the Cursor count
        try {
            if (cursor != null) {
                return cursor.getCount();
            } else {
                return 0;
            }
        } finally {

            // Close the Cursor
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Checks whether a data model already exists in the database
     *
     * @param context    Interface to global Context
     * @param model      Model to be checked against database records to see if it exists
     * @return True if the Model's FirebaseId already exists in the database. False otherwise
     */
    public static boolean isModelInDatabase(Context context, BaseModel model) {

        // Query the database for the model's FirebaseId
        Cursor cursor = context.getContentResolver().query(
                getUriForModel(model),
                null,
                GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                new String[] {model.firebaseId},
                null
        );

        // Check if Cursor is valid
        if (cursor != null) {
            if (cursor.moveToFirst()) {

                // Entry exists in database
                return true;
            }

            // Close the Cursor
            cursor.close();
        }

        return false;
    }

    /**
     * Toggles the favorite status of a Guide in the local database
     *
     * @param context    Interface to global Context
     * @param guide      Guide whose favorite status is to be toggled
     */
    public static void toggleFavorite(Context context, Guide guide) {

        // Toggle the favorite status of the Guide
        if (guide.isFavorite()) {
            guide.setFavorite(false);
        } else {
            guide.setFavorite(true);
        }

        // Either insert the Guide or update the value in the database
        if (isModelInDatabase(context, guide)) {
            context.getContentResolver().update(
                    GuideProvider.Guides.CONTENT_URI,
                    getContentValuesForModel(guide),
                    GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                    new String[] {guide.firebaseId});
        } else {
            insertModel(context, guide);
        }
    }

    /**
     * Checks whether a Guide is favorite'd in the local database
     *
     * @param context    Interface to global Context
     * @param guide      Guide to be checked
     * @return True if the Guide is favorite'd in the database. False otherwise.
     */
    public static boolean isGuideFavorite(Context context, Guide guide) {

        // Check whether the Guide exists in the database
        if (!isModelInDatabase(context, guide)) {

            // Does not exist in the database. Cannot be a favorite
            return false;
        } else {

            // Query the database for the Guide
            Cursor cursor = context.getContentResolver().query(
                    GuideProvider.Guides.CONTENT_URI,
                    null,
                    GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                    new String[] {guide.firebaseId},
                    null);

            if (cursor != null) {

                // Create a Guide from the Cursor
                cursor.moveToFirst();
                guide = Guide.createGuideFromCursor(cursor);

                try {

                    // Return the favorite status of the Guide
                    return guide.isFavorite();
                } finally {

                    // Close the Cursor
                    cursor.close();
                }

            } else {
                return false;
            }
        }
    }

    /**
     * Creates a ContentValues for a Guide data model
     *
     * @param guide    Guide to create ContentValues for
     * @return ContentValues describing a Guide
     */
    private static ContentValues getValuesForGuide(Guide guide) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.GuideEntry.FIREBASE_ID,    guide.firebaseId);
        values.put(GuideContract.GuideEntry.TRAIL_ID,       guide.trailId);
        values.put(GuideContract.GuideEntry.TRAIL_NAME,     guide.trailName);
        values.put(GuideContract.GuideEntry.AUTHOR_ID,      guide.authorId);
        values.put(GuideContract.GuideEntry.AUTHOR_NAME,    guide.authorName);
        values.put(GuideContract.GuideEntry.DATE_ADDED,     guide.dateAdded);
        values.put(GuideContract.GuideEntry.RATING,         guide.rating);
        values.put(GuideContract.GuideEntry.REVIEWS,        guide.reviews);
        values.put(GuideContract.GuideEntry.LATITUDE,       guide.latitude);
        values.put(GuideContract.GuideEntry.LONGITUDE,      guide.longitude);
        values.put(GuideContract.GuideEntry.DISTANCE,       guide.distance);
        values.put(GuideContract.GuideEntry.ELEVATION,      guide.elevation);
        values.put(GuideContract.GuideEntry.DIFFICULTY,     guide.difficulty);
        values.put(GuideContract.GuideEntry.AREA,           guide.area);

        // Add image Uri if the Guide has an image
        if (guide.getImageUri() != null) {
            values.put(GuideContract.GuideEntry.IMAGE_URI,  guide.getImageUri().toString());
        }

        // Add GPX Uri if the Guide has a GPX
        if (guide.getGpxUri() != null) {
            values.put(GuideContract.GuideEntry.GPX_URI,    guide.getGpxUri().toString());
        }

        // Add column value for whether the Guide is a draft
        if (guide.isDraft()) {
            values.put(GuideContract.GuideEntry.DRAFT,      1);
        }

        if (guide.isFavorite()) {
            values.put(GuideContract.GuideEntry.FAVORITE,   1);
        } else {
            values.put(GuideContract.GuideEntry.FAVORITE,   0);
        }

        return values;
    }

    /**
     * Creates a ContentValues describing a Trail data model
     *
     * @param trail    Trail to be converted to ContentValues
     * @return ContentValues describing the Trail in the signature
     */
    private static ContentValues getValuesForTrail(Trail trail) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.TrailEntry.FIREBASE_ID,        trail.firebaseId);
        values.put(GuideContract.TrailEntry.AREA_ID,            trail.areaId);
        values.put(GuideContract.TrailEntry.NAME,               trail.name);
        values.put(GuideContract.TrailEntry.LOWER_CASE_NAME,    trail.name.toLowerCase());
        values.put(GuideContract.TrailEntry.NOTES,              trail.notes);

        if (trail.isDraft()) {
            values.put(GuideContract.TrailEntry.DRAFT,          1);
        }
        return values;
    }

    /**
     * Creates a ContentValues describing an Author data model
     *
     * @param author    Author to be converted to ContentValues
     * @return ContentValues describing the Author in the signature
     */
    private static ContentValues getValuesForAuthor(Author author) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.AuthorEntry.FIREBASE_ID,       author.firebaseId);
        values.put(GuideContract.AuthorEntry.NAME,              author.name);
        values.put(GuideContract.AuthorEntry.LOWER_CASE_NAME,   author.name.toLowerCase());
        values.put(GuideContract.AuthorEntry.DESCRIPTION,       author.description);
        values.put(GuideContract.AuthorEntry.SCORE,             author.score);

        // Add image Uri if the Guide has an image
        if (author.hasImage) {
            values.put(GuideContract.AuthorEntry.IMAGE_URI,     author.getImageUri().toString());
        }

        if (author.isDraft()) {
            values.put(GuideContract.AuthorEntry.DRAFT,         1);
        }

        return values;
    }

    /**
     * Creates a ContentValues for a Section data model
     *
     * @param section    Section to be converted to ContentValues
     * @return ContentValues describing the Section in the signature
     */
    private static ContentValues getValuesForSection(Section section) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.SectionEntry.FIREBASE_ID,      section.firebaseId);
        values.put(GuideContract.SectionEntry.GUIDE_ID,         section.guideId);
        values.put(GuideContract.SectionEntry.SECTION,          section.section);
        values.put(GuideContract.SectionEntry.CONTENT,          section.content);

        // Add image Uri if the Guide has an image
        if (section.hasImage) {
            values.put(GuideContract.SectionEntry.IMAGE_URI,    section.getImageUri().toString());
        }

        if (section.isDraft()) {
            values.put(GuideContract.SectionEntry.DRAFT,        1);
        }

        return values;
    }

    /**
     * Creates a ContentValues for an Area data model
     *
     * @param area    Area to be converted to ContentValues
     * @return ContentValues describing the Area in the signature
     */
    private static ContentValues getValuesForArea(Area area) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.AreaEntry.FIREBASE_ID,         area.firebaseId);
        values.put(GuideContract.AreaEntry.NAME,                area.name);
        values.put(GuideContract.AreaEntry.LOWER_CASE_NAME,     area.name.toLowerCase());
        values.put(GuideContract.AreaEntry.LATITUDE,            area.latitude);
        values.put(GuideContract.AreaEntry.LONGITUDE,           area.longitude);
        values.put(GuideContract.AreaEntry.STATE,               area.state);

        if (area.isDraft()) {
            values.put(GuideContract.AreaEntry.DRAFT,           1);
        }


        return values;
    }

    /**
     * Creates a ContentValues from a data model Object by checking the type of data object it is
     * and calling the corresponding getValuesFor(Model) method.
     *
     * @param model    BaseModel data model to be converted to ContentValues
     * @return ContentValues describing the data model in the signature
     */
    public static ContentValues getContentValuesForModel(BaseModel model) {

        // Convert to ContentValues based on the class of the data model
        if (model instanceof Guide) {
            return getValuesForGuide((Guide) model);
        } else if (model instanceof Trail) {
            return getValuesForTrail((Trail) model);
        } else if (model instanceof Author) {
            return getValuesForAuthor((Author) model);
        } else if (model instanceof Section) {
            return getValuesForSection((Section) model);
        } else if (model instanceof Area) {
            return getValuesForArea((Area) model);
        }

        return null;
    }

    /**
     * Retrieves the Content Uri required to insert values for a given model
     *
     * @param model    The data model to be inserted into the database
     * @return The Uri corresponding to the data model for insertion into the database
     */
    public static Uri getUriForModel(BaseModel model) {

        // Retrieve the Content Uri for insertion based on the class of the BaseModel
        if (model instanceof Guide) {
            return GuideProvider.Guides.CONTENT_URI;
        } else if (model instanceof Trail) {
            return GuideProvider.Trails.CONTENT_URI;
        } else if (model instanceof Author) {
            return GuideProvider.Authors.CONTENT_URI;
        } else if (model instanceof Section) {
            return GuideProvider.Sections.CONTENT_URI;
        } else if (model instanceof Area) {
            return GuideProvider.Areas.CONTENT_URI;
        }

        return null;
    }
}
