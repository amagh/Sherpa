package project.sherpa.models.datamodels;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;

import java.util.HashMap;
import java.util.Map;

import project.sherpa.data.GuideDatabase;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 8/18/2017.
 */

public class Rating extends BaseModel {
    // ** Constants ** //
    public static final String DIRECTORY        = "ratings";
    public static final String GUIDE_ID         = "guideId";
    public static final String GUIDE_AUTHOR_ID  = "guideAuthorId";
    public static final String COMMENT          = "comment";
    public static final String RATING           = "rating";
    public static final String AUTHOR_ID        = "authorId";
    public static final String AUTHOR_NAME      = "authorName";
    public static final String DATE_ADDED       = "dateAdded";

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

        map.put(GUIDE_ID,           guideId);
        map.put(GUIDE_AUTHOR_ID,    guideAuthorId);
        map.put(COMMENT,            comment);
        map.put(RATING,             rating);
        map.put(AUTHOR_ID,          authorId);
        map.put(AUTHOR_NAME,        authorName);
        map.put(DATE_ADDED,         getDateAdded());

        return map;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Rating) {
            if (((Rating) obj).getComment() == null && this.comment != null ||
                    ((Rating) obj).getComment() != null && this.comment == null) {
                return false;
            } else if ((((Rating) obj).getComment() == (this.comment) ||
                    ((Rating) obj).getComment().equals(this.comment)) &&
                    ((Rating) obj).rating == this.rating) {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates a Rating with new values from an update Rating of the same FirebaseId
     *
     * @param newModelValues    BaseModel containing the new Values
     */
    @Override
    public void updateValues(BaseModel newModelValues) {

        // Cast newModelValues to a Rating
        if (!(newModelValues instanceof Rating)) return;
        Rating newRatingValues = (Rating) newModelValues;

        // Check to ensure newRatingValues has the same FirebaseId
        if (!newRatingValues.firebaseId.equals(firebaseId)) return;

        guideId         = newRatingValues.guideId;
        guideAuthorId   = newRatingValues.guideAuthorId;
        comment         = newRatingValues.comment;
        rating          = newRatingValues.rating;
        authorId        = newRatingValues.authorId;
        authorName      = newRatingValues.authorName;
        dateAdded       = newRatingValues.dateAdded;
    }

    /**
     * Updates the Rating on Firebase Database
     *
     * @param previousRating    The previous numerical rating. 0 if it hasn't been rated before
     */
    public void updateFirebase(final int previousRating) {

        // Set the FirebaseId of the Rating if it is new
        if (firebaseId == null) {
            firebaseId = FirebaseDatabase.getInstance().getReference()
                    .child(DIRECTORY).child(guideId).push().getKey();
        }

        // Set the Rating to add the Server timestamp as the date
        addDate();

        // Get the directory for where to push the update
        String directory = FirebaseProviderUtils.generateDirectory(DIRECTORY, guideId, firebaseId);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(directory, toMap());

        // Update the Firebase entry
        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // Update guide and author scores
                        updateGuideScore(previousRating);
                        updateAuthorScore(previousRating);
                    }
                });
    }

    /**
     * Updates the score of the Guide for which the rating is for
     *
     * @param previousRating    The previous numerical rating this Rating was changed from
     */
    private void updateGuideScore(final int previousRating) {

        Transaction.Handler guideUpdateHandler = new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Guide guide = mutableData.getValue(Guide.class);

                if (guide == null) {
                    // Glitch -- recall the function
                    updateGuideScore(previousRating);
                    return Transaction.abort();
                }

                // Update guide's values
                guide.rating = guide.rating - previousRating + rating;
                if (previousRating == 0) {
                    guide.reviews++;
                }

                mutableData.setValue(guide.toMap());
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Timber.e("Error updating guide score\n" + databaseError.getMessage());
                }
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .child(guideId)
                .runTransaction(guideUpdateHandler);
    }

    /**
     * Updates the score of the Author for which the Rating corresponds to
     * @param previousRating    The previous numerical Rating given to the Author
     */
    private void updateAuthorScore(final int previousRating) {

        // Ratings less than three do not affect the Author's score. This is to prevent many low
        // ratings from giving bad authors a benefit.
        if (rating < 3 && previousRating < 3) return;

        Transaction.Handler authorUpdateHandler = new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Author author = mutableData.getValue(Author.class);

                if (author == null) {
                    // Glitch -- recall the function
                    updateAuthorScore(previousRating);
                    return Transaction.abort();
                }

                // Update author values
                if (rating >= 3) author.score += rating;
                if (previousRating >= 3) author.score -= previousRating;

                mutableData.setValue(author.toMap());
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Timber.e("Error updating author's score\n" + databaseError.getMessage());
                }
            }
        };

        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(guideAuthorId)
                .runTransaction(authorUpdateHandler);
    }

    //********************************************************************************************//
    //*********************************** Getters and Setters ************************************//
    //********************************************************************************************//

    public String getGuideId() {
        return guideId;
    }

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
