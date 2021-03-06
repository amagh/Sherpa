package project.sherpa.models.datamodels;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.files.GpxFile;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.datamodels.abstractmodels.BaseModelWithImage;
import project.sherpa.utilities.objects.GpxStats;
import project.sherpa.utilities.GpxUtils;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Guide extends BaseModelWithImage implements Parcelable {
    // ** Constants ** //
    public static final String TITLE            = "title";
    private static final String TRAIL_ID        = "trailId";
    private static final String TRAIL_NAME      = "trailName";
    private static final String AUTHOR_ID       = "authorId";
    private static final String AUTHOR_NAME     = "authorName";
    private static final String DATE_ADDED      = "dateAdded";
    private static final String RATING          = "rating";
    private static final String REVIEWS         = "reviews";
    private static final String LATITUDE        = "latitude";
    private static final String LONGITUDE       = "longitude";
    private static final String ELEVATION       = "elevation";
    private static final String HAS_IMAGE       = "hasImage";
    private static final String DISTANCE        = "distance";
    private static final String DIFFICULTY      = "difficulty";
    private static final String AREA            = "area";

    // ** Member Variables ** //
    private String title;
    public String trailId;
    public String trailName;
    public String authorId;
    public String authorName;
    private long dateAdded;
    public double rating;
    public int reviews;
    public double latitude;
    public double longitude;
    public double distance;
    public double elevation;
    public int difficulty;
    public String area;
    private boolean favorite;
    private boolean addDate;

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
        int idxGuideTitle       = cursor.getColumnIndex(GuideContract.GuideEntry.TITLE);
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
        int idxFavorite         = cursor.getColumnIndex(GuideContract.GuideEntry.FAVORITE);

        // Get the values from the Cursor
        String firebaseId       = cursor.getString(idxFirebaseId);
        String title            = cursor.getString(idxGuideTitle);
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
        boolean favorite        = cursor.getInt(idxFavorite) == 1;

        // Create a new Guide using the values from the Cursor
        Guide guide             = new Guide();
        guide.firebaseId        = firebaseId;
        guide.title             = title;
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
        guide.favorite          = favorite;
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

        map.put(TITLE, title);
        map.put(TRAIL_ID, trailId);
        map.put(TRAIL_NAME, trailName);
        map.put(AUTHOR_ID, authorId);
        map.put(AUTHOR_NAME, authorName);
        map.put(DATE_ADDED, getDateAdded());
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
    @Exclude
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

    /**
     * Updates the field values for the Guide with new ones from a Guide with the same FirebaseId
     *
     * @param newModelValues    BaseModel containing the new Values
     */
    @Override
    public void updateValues(BaseModel newModelValues) {

        // Cast newModelValues to a Guide
        if (!(newModelValues instanceof Guide)) return;
        Guide newGuideValues = (Guide) newModelValues;

        // Ensure that the newGuideValues has the same FirebaseId
        if (!newGuideValues.firebaseId.equals(firebaseId)) return;

        title       = newGuideValues.title;
        trailId     = newGuideValues.trailId;
        trailName   = newGuideValues.trailName;
        authorId    = newGuideValues.authorId;
        authorName  = newGuideValues.authorName;
        dateAdded   = newGuideValues.dateAdded;
        rating      = newGuideValues.rating;
        reviews     = newGuideValues.reviews;
        latitude    = newGuideValues.latitude;
        longitude   = newGuideValues.longitude;
        distance    = newGuideValues.distance;
        elevation   = newGuideValues.elevation;
        difficulty  = newGuideValues.difficulty;
        area        = newGuideValues.area;
        favorite    = newGuideValues.favorite;
        addDate     = newGuideValues.addDate;
        gpxUri      = newGuideValues.gpxUri;
    }

    //********************************************************************************************//
    //*********************************** Getters & Setters **************************************//
    //********************************************************************************************//

    @Exclude
    public boolean isFavorite() {
        return favorite;
    }

    @Exclude
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public Object getDateAdded() {
        if (addDate) {
            return ServerValue.TIMESTAMP;
        } else {
            return dateAdded;
        }
    }

    @Exclude
    public long getDate() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public void addDate() {
        this.addDate = true;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
        } else {
            parcel.writeString(null);
        }

        if (imageUri != null) {
            parcel.writeString(imageUri.toString());
        } else {
            parcel.writeString(null);
        }

        if (isDraft()) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }

        if (isFavorite()) {
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

        if (parcel.readInt() == 1) {
            setFavorite(true);
        }
    }
}
