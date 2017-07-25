package project.hikerguide.models.datamodels;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.models.datamodels.abstractmodels.BaseModelWithImage;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Author extends BaseModelWithImage {
    // ** Constants ** //
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String LOWER_CASE_NAME = "lowerCaseName";
    private static final String HAS_IMAGE = "hasImage";
    private static final String SCORE = "score";

    // ** Member Variables ** //
    public String name;
    public String description;
    public int score;

    public Author() {}

    public Author(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public Author(String name) {
        this.name = name;
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
        int idxScore = cursor.getColumnIndex(GuideContract.AuthorEntry.SCORE);

        // Retrieve the values from the Cursor
        long id = cursor.getLong(idxId);
        String name = cursor.getString(idxName);
        int score = cursor.getInt(idxScore);

        // Instantiate a new Author with the values
        return new Author(name, score);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(NAME, name);
        map.put(DESCRIPTION, description);
        map.put(LOWER_CASE_NAME, name.toLowerCase());
        map.put(HAS_IMAGE, hasImage);
        map.put(SCORE, score);

        return map;
    }
}
