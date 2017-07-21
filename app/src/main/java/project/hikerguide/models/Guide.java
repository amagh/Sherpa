package project.hikerguide.models;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.files.GpxFile;
import project.hikerguide.models.abstractmodels.BaseModelWithImage;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Guide extends BaseModelWithImage {
    // ** Constants ** //
    private static final String TRAIL_ID = "trailId";
    private static final String TRAIL_NAME = "trailName";
    private static final String AUTHOR_ID = "authorId";
    private static final String AUTHOR_NAME = "authorName";
    private static final String DATE_ADDED = "dateAdded";
    private static final String RATING = "rating";
    private static final String REVIEWS = "reviews";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String HAS_IMAGE = "hasImage";
    private static final String DISTANCE = "distance";
    private static final String DIFFICULTY = "difficulty";
    private static final String AREA = "area";

    // ** Member Variables ** //
    public String trailId;
    public String trailName;
    public String authorId;
    public String authorName;
    public long dateAdded;
    public double rating;
    public int reviews;
    public double latitude;
    public double longitude;
    public double distance;
    public String difficulty;
    public String area;

    private Uri gpxUri;

    /**
     * Default constructor required for Firebase Database
     */
    public Guide() {}

    public Guide(long id, String trailId, String authorId, long dateAdded, double latitude, double longitude) {
        this.id = id;
        this.trailId = trailId;
        this.authorId = authorId;
        this.dateAdded = dateAdded;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Guide(long id, long dateAdded, double latitude, double longitude) {
        this.id = id;
        this.dateAdded = dateAdded;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Takes the values from a Cursor and uses it to create a new Guide
     *
     * @param cursor    Cursor describing a Guide
     * @return Guide with the values described in the input Cursor
     */
    public Guide createGuideFromCursor(Cursor cursor) {
        // Get the index of every column from the Cursor
        int idxId = cursor.getColumnIndex(GuideContract.GuideEntry._ID);
        int idxTrailId = cursor.getColumnIndex(GuideContract.GuideEntry.TRAIL_ID);
        int idxAuthorId = cursor.getColumnIndex(GuideContract.GuideEntry.AUTHOR_ID);
        int idxDateAdded = cursor.getColumnIndex(GuideContract.GuideEntry.DATE_ADDED);
        int idxRating = cursor.getColumnIndex(GuideContract.GuideEntry.RATING);
        int idxReviews = cursor.getColumnIndex(GuideContract.GuideEntry.REVIEWS);
        int idxLatitude = cursor.getColumnIndex(GuideContract.GuideEntry.LATITUDE);
        int idxLongitude = cursor.getColumnIndex(GuideContract.GuideEntry.LONGITUDE);

        // Get the values from the Cursor
        long id = cursor.getLong(idxId);
        String trailId = cursor.getString(idxTrailId);
        String authorId = cursor.getString(idxAuthorId);
        long dateAdded = cursor.getLong(idxDateAdded);
        double rating = cursor.getDouble(idxRating);
        int reviews = cursor.getInt(idxReviews);
        double latitude = cursor.getDouble(idxLatitude);
        double longitude = cursor.getDouble(idxLongitude);

        // Create a new Guide using the values from the Cursor
        Guide guide = new Guide(id, trailId, authorId, dateAdded, latitude, longitude);
        guide.rating = rating;
        guide.reviews = reviews;

        return guide;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(ID, id);
        map.put(TRAIL_ID, trailId);
        map.put(TRAIL_NAME, trailName);
        map.put(AUTHOR_ID, authorId);
        map.put(AUTHOR_NAME, authorName);
        map.put(DATE_ADDED, dateAdded);
        map.put(RATING, rating);
        map.put(REVIEWS, reviews);
        map.put(LATITUDE, latitude);
        map.put(LONGITUDE, longitude);
        map.put(HAS_IMAGE, hasImage);
        map.put(DISTANCE, distance);
        map.put(DIFFICULTY, difficulty);
        map.put(AREA, area);

        return map;
    }

    /**
     * Sets the Uri describing a GPX file
     *
     * @param gpxUri    Uri corresponding to the location of a GPX file.
     */
//    public void setGpxUri(Uri gpxUri) {
//        this.gpxUri = gpxUri;
//    }

    /**
     * Converts a File to a Uri to be saved as a reference to the actual GPX File so that it may be
     * accessed later
     *
     * @param gpxFile    File describing the location of a GPX file
     */
    public void setGpxUri(File gpxFile) {
        this.gpxUri = Uri.fromFile(gpxFile);
    }

    /**
     * Converts the Objects gpxUri to a GpxFile so that it may be uploaded to Firebase Storage
     *
     * @return GpxFile corresponding to the original GPX file that was set to the Guide
     */
    public GpxFile getGpxFile() {
        return new GpxFile(this.firebaseId, this.gpxUri.getPath());
    }

    /**
     * Generates a new GpxFile for downloading a GpxFile from Firebase Storage
     *
     * @param context    Interface to global Context
     * @return GpxFile corresponding to where the File will be downloaded to Internal Storage
     */
    public GpxFile generateGpxFileForDownload(Context context) {
        // Create the GpxFile
        GpxFile gpxFile = GpxFile.getDestinationFile(context, this.firebaseId);

        // Set the gpxUri to the File's path
        setGpxUri(gpxFile);
        return gpxFile;
    }
}
