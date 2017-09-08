package project.sherpa.models.datamodels;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.files.GpxFile;
import project.sherpa.models.datamodels.abstractmodels.BaseModelWithImage;
import project.sherpa.utilities.objects.GpxStats;
import project.sherpa.utilities.GpxUtils;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Guide extends BaseModelWithImage implements Parcelable {
    // ** Constants ** //
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
    private static final String RATERS          = "raters";

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
    private boolean favorite;
    public Map<String, Rating> raters;

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
        int idxFavorite         = cursor.getColumnIndex(GuideContract.GuideEntry.FAVORITE);

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
        boolean favorite        = cursor.getInt(idxFavorite) == 1;

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
        map.put(RATERS, raters);

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

    @Exclude
    public boolean isFavorite() {
        return favorite;
    }

    @Exclude
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /**
     * Converts the Map of raters into an Array of Ratings to be used to populate the Guide's
     * ratings
     *
     * @return An Array describing the people who have rated this guide, their rating, and their
     * comments
     */
    @Exclude
    public Rating[] getRatings() {

        // Validate raters
        if (raters != null) {

            // Create a List to hold the Ratings to be returned
            List<Rating> ratingList = new ArrayList<>();

            // Iterate through the Map and convert each entry to a Rating
            for (String authorId : raters.keySet()) {

                Rating rating = raters.get(authorId);

                // Add the authorId to the Rating
                rating.setAuthorId(authorId);

                // Add the Rating to the List
                ratingList.add(rating);
            }

            Collections.sort(ratingList, new Comparator<Rating>() {
                @Override
                public int compare(Rating rating, Rating t1) {
                    return rating.getDate() < t1.getDate()
                            ? -1
                            : rating.getDate() > t1.getDate()
                            ? 1
                            : 0;
                }
            });

            return ratingList.toArray(new Rating[ratingList.size()]);
        } else {
            return null;
        }
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

        if (raters != null) {
            parcel.writeInt(raters.size());

            for (String authorId : raters.keySet()) {
                parcel.writeString(authorId);
                parcel.writeString(raters.get(authorId).getAuthorName());
                parcel.writeInt(raters.get(authorId).getRating());
                parcel.writeString(raters.get(authorId).getComment());
                parcel.writeLong(raters.get(authorId).getDate());
            }
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

        int ratings;
        if ((ratings = parcel.readInt()) != 0) {
            raters = new HashMap<>();

            for (int i = 0; i < ratings; i++) {
                String authorId = parcel.readString();

                Rating rating = new Rating();

                rating.setAuthorName(parcel.readString());
                rating.setRating(parcel.readInt());
                rating.setComment(parcel.readString());
                rating.setDateAdded(parcel.readLong());

                raters.put(authorId, rating);
            }
        }
    }
}
