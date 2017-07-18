package project.hikerguide.models;

import android.database.Cursor;

import project.hikerguide.data.GuideContract;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Author extends BaseModel {
    public String name;
    public String profilePicture;
    public int score;

    public Author() {}

    public Author(long id, String name, String profilePicture, int score) {
        this.id = id;
        this.name = name;
        this.profilePicture = profilePicture;
        this.score = score;
    }

    public Author(long id, String name, String profilePicture) {
        this.id = id;
        this.name = name;
        this.profilePicture = profilePicture;
    }

    /**
     * Creates an Author Object using values described in a Cursor
     *
     * @param cursor    Cursor describing an Author
     * @return An Author Object with values described in the Cursor
     */
    public Author createAuthorFromCursor(Cursor cursor) {
        // Index the columns of the Cursor
        int idxId = cursor.getColumnIndex(GuideContract.AuthorEntry._ID);
        int idxName = cursor.getColumnIndex(GuideContract.AuthorEntry.NAME);
        int idxProfilePicture = cursor.getColumnIndex(GuideContract.AuthorEntry.PROFILE_PICTURE);
        int idxScore = cursor.getColumnIndex(GuideContract.AuthorEntry.SCORE);

        // Retrieve the values from the Cursor
        long id = cursor.getLong(idxId);
        String name = cursor.getString(idxName);
        String profilePicture = cursor.getString(idxProfilePicture);
        int score = cursor.getInt(idxScore);

        // Instantiate a new Author with the values
        return new Author(id, name, profilePicture, score);
    }
}
