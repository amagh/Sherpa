package project.hikerguide.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;

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
     * Creates a ContentValues for a Guide data model
     *
     * @param guide    Guide to create ContentValues for
     * @return ContentValues describing a Guide
     */
    public static ContentValues getValuesForGuide(Guide guide) {
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

        // Add image Uri if the Guide has an image
        if (guide.hasImage) {
            values.put(GuideContract.GuideEntry.IMAGE_URI,  guide.getImageUri().toString());
        }

        // Add GPX Uri if the Guide has a GPX
        if (guide.getGpxUri() != null) {
            values.put(GuideContract.GuideEntry.GPX_URI,    guide.getGpxUri().toString());
        }

        return values;
    }

    /**
     * Creates a ContentValues describing a Trail data model
     *
     * @param trail    Trail to be converted to ContentValues
     * @return ContentValues describing the Trail in the signature
     */
    public static ContentValues getValuesForTrail(Trail trail) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.TrailEntry.FIREBASE_ID,        trail.firebaseId);
        values.put(GuideContract.TrailEntry.AREA_ID,            trail.areaId);
        values.put(GuideContract.TrailEntry.NAME,               trail.name);
        values.put(GuideContract.TrailEntry.LOWER_CASE_NAME,    trail.name.toLowerCase());
        values.put(GuideContract.TrailEntry.NOTES,              trail.notes);

        return values;
    }

    /**
     * Creates a ContentValues describing an Author data model
     *
     * @param author    Author to be converted to ContentValues
     * @return ContentValues describing the Author in the signature
     */
    public static ContentValues getValuesForAuthor(Author author) {
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

        return values;
    }

    /**
     * Creates a ContentValues for a Section data model
     *
     * @param section    Section to be converted to ContentValues
     * @return ContentValues describing the Section in the signature
     */
    public static ContentValues getValuesForSection(Section section) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.SectionEntry.FIREBASE_ID,      section.firebaseId);
        values.put(GuideContract.SectionEntry.GUIDE_ID,         section.guideId);
        values.put(GuideContract.SectionEntry.SECTION,          section.section);
        values.put(GuideContract.SectionEntry.CONTENT,          section.content);

        // Add image Uri if the Guide has an image
        if (section.hasImage) {
            values.put(GuideContract.SectionEntry.IMAGE_URI,    section.getImageUri().toString());
        }

        return values;
    }

    /**
     * Creates a ContentValues for an Area data model
     *
     * @param area    Area to be converted to ContentValues
     * @return ContentValues describing the Area in the signature
     */
    public static ContentValues getValuesForArea(Area area) {
        ContentValues values = new ContentValues();

        values.put(GuideContract.AreaEntry.FIREBASE_ID,         area.firebaseId);
        values.put(GuideContract.AreaEntry.NAME,                area.name);
        values.put(GuideContract.AreaEntry.LOWER_CASE_NAME,     area.name.toLowerCase());
        values.put(GuideContract.AreaEntry.LATITUDE,            area.latitude);
        values.put(GuideContract.AreaEntry.LONGITUDE,           area.longitude);
        values.put(GuideContract.AreaEntry.STATE,               area.state);

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
