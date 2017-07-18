package project.hikerguide.models;

import android.database.Cursor;

import project.hikerguide.data.GuideContract;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Trail {
    public long id;
    public long areaId;
    public String name;
    public String notes;

    public Trail() {}

    public Trail(long id, long areaId, String name, String notes) {
        this.id = id;
        this.areaId = areaId;
        this.name = name;
        this.notes = notes;
    }

    /**
     * Creates a Trail Object from the values of a Cursor
     *
     * @param cursor    Cursor describing a Trail
     * @return Trail with the values described from the Cursor
     */
    public Trail createTrailFromCursor(Cursor cursor) {
        // Index all the columns in the Cursor
        int idxId = cursor.getColumnIndex(GuideContract.TrailEntry._ID);
        int idxAreaId = cursor.getColumnIndex(GuideContract.TrailEntry.AREA_ID);
        int idxName = cursor.getColumnIndex(GuideContract.TrailEntry.NAME);
        int idxNotes = cursor.getColumnIndex(GuideContract.TrailEntry.NOTES);

        // Retrieve the values from the Cursor
        long id = cursor.getLong(idxId);
        long areaId = cursor.getLong(idxAreaId);
        String name = cursor.getString(idxName);
        String notes = cursor.getString(idxNotes);

        // Create a new Trail from the values
        return new Trail(id, areaId, name, notes);
    }
}
