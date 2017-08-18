package project.hikerguide.models.datamodels;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alvin on 8/18/2017.
 */

public class Rating {
    // ** Constants ** //
    private static final String AUTHOR_ID   = "authorId";
    private static final String GUIDE_ID    = "guideId";
    private static final String COMMENT     = "comment";
    private static final String RATING      = "rating";

    // ** Member Variables ** //
    private String authorId;
    private String guideId;
    private String comment;
    private int rating;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(COMMENT, comment);
        map.put(RATING, rating);

        return map;
    }

    //********************************************************************************************//
    //*********************************** Getters and Setters ************************************//
    //********************************************************************************************//

    @Exclude
    public String getAuthorId() {
        return authorId;
    }

    @Exclude
    public String getGuideId() {
        return guideId;
    }

    public String getComment() {
        return comment;
    }

    public int getRating() {
        return rating;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setGuideId(String guideId) {
        this.guideId = guideId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
