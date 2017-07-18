package project.hikerguide.models;

import android.database.Cursor;

import project.hikerguide.data.GuideContract;

/**
 * Created by Alvin on 7/18/2017.
 */

public class Area extends BaseModel {
    public String name;

    public Area() {}

    public Area(long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Creates an Area Object using values descirbed in a Cursor
     *
     * @param cursor    Cursor describing an Area
     * @return Area with values described in the Cursor
     */
    public Area createAreaFromCursor(Cursor cursor) {
        // Index the columns of the Cursor
        int idxId = cursor.getColumnIndex(GuideContract.AreaEntry._ID);
        int idxName = cursor.getColumnIndex(GuideContract.AreaEntry.NAME);

        // Retrieve the values from the Cursor
        long id = cursor.getLong(idxId);
        String name = cursor.getString(idxName);

        // Create a new Area with the values
        return new Area(id, name);
    }
}
