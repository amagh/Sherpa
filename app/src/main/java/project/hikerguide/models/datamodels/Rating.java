package project.hikerguide.models.datamodels;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import timber.log.Timber;

/**
 * Created by Alvin on 8/18/2017.
 */

public class Rating extends BaseModel {
    // ** Constants ** //
    public static final String GUIDE_ID    = "guideId";
    private static final String COMMENT     = "comment";
    private static final String RATING      = "rating";
    public static final String AUTHOR_ID   = "authorId";
    private static final String AUTHOR_NAME = "authorName";
    private static final String DATE_ADDED  = "dateAdded";

    // ** Member Variables ** //
    private String guideId;
    private String guideAuthorId;
    private String comment;
    private int rating;
    private String authorId;
    private String authorName;
    private long dateAdded;
    private boolean addDate;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(GUIDE_ID, guideId);
        map.put(COMMENT, comment);
        map.put(RATING, rating);
        map.put(AUTHOR_ID, authorId);
        map.put(AUTHOR_NAME, authorName);
        map.put(DATE_ADDED, getDateAdded());

        return map;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Rating) {
            if (((Rating) obj).getComment().equals(this.comment) &&
                    ((Rating) obj).rating == this.rating) {
                return true;
            }
        }

        return false;
    }

    //********************************************************************************************//
    //*********************************** Getters and Setters ************************************//
    //********************************************************************************************//

    public String getGuideId() {
        return guideId;
    }

    @Exclude
    public String getGuideAuthorId() {
        return guideAuthorId;
    }

    public String getComment() {
        return comment;
    }

    public int getRating() {
        return rating;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
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

    public void setGuideId(String guideId) {
        this.guideId = guideId;
    }

    public void setGuideAuthorId(String guideAuthorId) {
        this.guideAuthorId = guideAuthorId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public void addDate() {
        this.addDate = true;
    }
}
