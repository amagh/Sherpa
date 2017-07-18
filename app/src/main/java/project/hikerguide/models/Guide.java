package project.hikerguide.models;

import android.database.Cursor;

import project.hikerguide.data.GuideContract;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Guide {
    public long id;
    public long trailId;
    public long authorId;
    public long dateAdded;
    public double rating;
    public int reviews;
    public String gpx;
    public double latitude;
    public double longitude;
    public String image;

    /**
     * Default constructor required for Firebase Database
     */
    public Guide() {}

    public Guide(long id, long trailId, long authorId, long dateAdded, String gpx, double latitude,
                 double longitude, String image) {

        this.id = id;
        this.trailId = trailId;
        this.authorId = authorId;
        this.dateAdded = dateAdded;
        this.gpx = gpx;
        this.latitude = latitude;
        this.longitude = longitude;
        this.image = image;
    }

    /**
     * Setter for the Guide's rating
     * @param rating    The Guide's rating
     */
    public void setRating(double rating) {
        this.rating = rating;
    }

    /**
     * Setter for the Guide's number of reviews
     * @param reviews    The number of reviews for the Guide
     */
    public void setReviews(int reviews) {
        this.reviews = reviews;
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
        int idxGpx = cursor.getColumnIndex(GuideContract.GuideEntry.GPX);
        int idxLatitude = cursor.getColumnIndex(GuideContract.GuideEntry.LATITUDE);
        int idxLongitude = cursor.getColumnIndex(GuideContract.GuideEntry.LONGITUDE);
        int idxImage = cursor.getColumnIndex(GuideContract.GuideEntry.IMAGE);

        // Get the values from the Cursor
        long id = cursor.getLong(idxId);
        long trailId = cursor.getLong(idxTrailId);
        long authorId = cursor.getLong(idxAuthorId);
        long dateAdded = cursor.getLong(idxDateAdded);
        double rating = cursor.getDouble(idxRating);
        int reviews = cursor.getInt(idxReviews);
        String gpx = cursor.getString(idxGpx);
        double latitude = cursor.getDouble(idxLatitude);
        double longitude = cursor.getDouble(idxLongitude);
        String image = cursor.getString(idxImage);

        // Create a new Guide using the values from the Cursor
        Guide guide = new Guide(id, trailId, authorId, dateAdded, gpx, latitude, longitude, image);
        guide.setRating(rating);
        guide.setReviews(reviews);

        return guide;
    }
}
