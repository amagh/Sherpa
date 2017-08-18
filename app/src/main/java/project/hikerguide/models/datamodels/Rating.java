package project.hikerguide.models.datamodels;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alvin on 8/18/2017.
 */

public class Rating {
    // ** Constants ** //
    private static final String COMMENT     = "comment";
    private static final String RATING      = "rating";

    // ** Member Variables ** //
    private String comment;
    private int rating;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(COMMENT, comment);
        map.put(RATING, rating);

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

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
