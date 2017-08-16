package project.hikerguide.models.datamodels;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.files.GpxFile;
import project.hikerguide.models.datamodels.abstractmodels.BaseModelWithImage;
import project.hikerguide.utilities.objects.GpxStats;
import project.hikerguide.utilities.GpxUtils;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Guide extends BaseModelWithImage implements Parcelable {
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
    private static final String ELEVATION = "elevation";
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
    public double elevation;
    public int difficulty;
    public String area;

    private Uri gpxUri;

    /**
     * Default constructor required for Firebase Database
     */
    public Guide() {}

    public Guide(String trailId, String authorId, long dateAdded, double latitude, double longitude) {
        this.trailId = trailId;
        this.authorId = authorId;
        this.dateAdded = dateAdded;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Guide(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    /**
     * Takes the values from a Cursor and uses it to create a new Guide
     *
     * @param cursor    Cursor describing a Guide
     * @return Guide with the values described in the input Cursor
     */
    public static Guide createGuideFromCursor(Cursor cursor) {

        // Get the index of every column from the Cursor
        int idxFirebaseId       = cursor.getColumnIndex(GuideContract.GuideEntry.FIREBASE_ID);
        int idxTrailId          = cursor.getColumnIndex(GuideContract.GuideEntry.TRAIL_ID);
        int idxTrailName        = cursor.getColumnIndex(GuideContract.GuideEntry.TRAIL_NAME);
        int idxAuthorId         = cursor.getColumnIndex(GuideContract.GuideEntry.AUTHOR_ID);
        int idxAuthorName       = cursor.getColumnIndex(GuideContract.GuideEntry.AUTHOR_NAME);
        int idxDateAdded        = cursor.getColumnIndex(GuideContract.GuideEntry.DATE_ADDED);
        int idxRating           = cursor.getColumnIndex(GuideContract.GuideEntry.RATING);
        int idxReviews          = cursor.getColumnIndex(GuideContract.GuideEntry.REVIEWS);
        int idxLatitude         = cursor.getColumnIndex(GuideContract.GuideEntry.LATITUDE);
        int idxLongitude        = cursor.getColumnIndex(GuideContract.GuideEntry.LONGITUDE);
        int idxDistance         = cursor.getColumnIndex(GuideContract.GuideEntry.DISTANCE);
        int idxElevation        = cursor.getColumnIndex(GuideContract.GuideEntry.ELEVATION);
        int idxDifficulty       = cursor.getColumnIndex(GuideContract.GuideEntry.DIFFICULTY);
        int idxArea             = cursor.getColumnIndex(GuideContract.GuideEntry.AREA);
        int idxImageUri         = cursor.getColumnIndex(GuideContract.GuideEntry.IMAGE_URI);
        int idxGpxUri           = cursor.getColumnIndex(GuideContract.GuideEntry.GPX_URI);
        int idxDraft            = cursor.getColumnIndex(GuideContract.GuideEntry.DRAFT);

        // Get the values from the Cursor
        String firebaseId       = cursor.getString(idxFirebaseId);
        String trailId          = cursor.getString(idxTrailId);
        String trailName        = cursor.getString(idxTrailName);
        String authorId         = cursor.getString(idxAuthorId);
        String authorName       = cursor.getString(idxAuthorName);
        long dateAdded          = cursor.getLong(idxDateAdded);
        double rating           = cursor.getDouble(idxRating);
        int reviews             = cursor.getInt(idxReviews);
        double latitude         = cursor.getDouble(idxLatitude);
        double longitude        = cursor.getDouble(idxLongitude);
        double distance         = cursor.getDouble(idxDistance);
        double elevation        = cursor.getDouble(idxElevation);
        int difficulty          = cursor.getInt(idxDifficulty);
        String area             = cursor.getString(idxArea);
        String imageUriString   = cursor.getString(idxImageUri);
        String gpxUriString     = cursor.getString(idxGpxUri);
        boolean draft           = cursor.getInt(idxDraft) == 1;

        // Create a new Guide using the values from the Cursor
        Guide guide             = new Guide();
        guide.firebaseId        = firebaseId;
        guide.trailId           = trailId;
        guide.trailName         = trailName;
        guide.authorId          = authorId;
        guide.authorName        = authorName;
        guide.dateAdded         = dateAdded;
        guide.rating            = rating;
        guide.reviews           = reviews;
        guide.latitude          = latitude;
        guide.longitude         = longitude;
        guide.distance          = distance;
        guide.elevation         = elevation;
        guide.difficulty        = difficulty;
        guide.area              = area;
        guide.setDraft(draft);

        if (imageUriString != null) {
            File imageFile = new File(Uri.parse(imageUriString).getPath());
            guide.setImageUri(imageFile);
        }

        if (gpxUriString != null) {
            File gpxFile = new File(Uri.parse(gpxUriString).getPath());
            guide.setGpxUri(gpxFile);
        }

        return guide;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(TRAIL_ID, trailId);
        map.put(TRAIL_NAME, trailName);
        map.put(AUTHOR_ID, authorId);
        map.put(AUTHOR_NAME, authorName);
        map.put(DATE_ADDED, dateAdded);
        map.put(RATING, rating);
        map.put(REVIEWS, reviews);
        map.put(LATITUDE, latitude);
        map.put(LONGITUDE, longitude);
        map.put(DISTANCE, distance);
        map.put(ELEVATION, elevation);
        map.put(DISTANCE, distance);
        map.put(DIFFICULTY, difficulty);
        map.put(AREA, area);
        map.put(HAS_IMAGE, hasImage);

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
        GpxStats stats = GpxUtils.getGpxStats(gpxFile);

        if (stats == null) {
            throw new RuntimeException("Selected file does not contain proper GPS coordinates");
        }

        this.distance = stats.distance;
        this.latitude = stats.latitude;
        this.longitude = stats.longitude;
        this.elevation = stats.elevation;
        this.gpxUri = Uri.fromFile(gpxFile);
    }

    public Uri getGpxUri() {
        return gpxUri;
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
        return gpxFile;
    }

    //********************************************************************************************//
    //***************************** Parcelable Related Methods ***********************************//
    //********************************************************************************************//


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(firebaseId);
        parcel.writeString(trailId);
        parcel.writeString(trailName);
        parcel.writeString(authorId);
        parcel.writeString(authorName);
        parcel.writeLong(dateAdded);
        parcel.writeDouble(rating);
        parcel.writeInt(reviews);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeDouble(elevation);
        parcel.writeDouble(distance);
        parcel.writeInt(difficulty);
        parcel.writeString(area);

        if (gpxUri != null) {
            parcel.writeString(gpxUri.toString());
        }

        if (imageUri != null) {
            parcel.writeString(imageUri.toString());
        }

        if (isDraft()) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }
    }

    public static final Parcelable.Creator<Guide> CREATOR = new Parcelable.Creator<Guide>() {
        @Override
        public Guide createFromParcel(Parcel parcel) {
            return new Guide(parcel);
        }

        @Override
        public Guide[] newArray(int i) {
            return new Guide[i];
        }
    };

    private Guide(Parcel parcel) {
        firebaseId = parcel.readString();
        trailId = parcel.readString();
        trailName = parcel.readString();
        authorId = parcel.readString();
        authorName = parcel.readString();
        dateAdded = parcel.readLong();
        rating = parcel.readDouble();
        reviews = parcel.readInt();
        latitude = parcel.readDouble();
        longitude = parcel.readDouble();
        elevation = parcel.readDouble();
        distance = parcel.readDouble();
        difficulty = parcel.readInt();
        area = parcel.readString();

        String gpxUriString = parcel.readString();
        if (gpxUriString != null) {
            gpxUri = Uri.parse(gpxUriString);
        }

        String imageUriString = parcel.readString();
        if (imageUriString != null) {
            hasImage = true;
            imageUri = Uri.parse(imageUriString);
        }

        if (parcel.readInt() == 1) {
            setDraft(true);
        }
    }
}
