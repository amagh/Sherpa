package project.hikerguide.models.datamodels;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alvin on 8/18/2017.
 */

public class Rating {
    // ** Constants ** //
    private static final String COMMENT     = "comment";
    private static final String RATING      = "rating";
    private static final String AUTHOR_NAME = "authorName";

    // ** Member Variables ** //
    private String comment;
    private int rating;
    private String authorId;
    private String authorName;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(COMMENT, comment);
        map.put(RATING, rating);

        if (authorName != null) {
            map.put(AUTHOR_NAME, authorName);
        }

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
}
