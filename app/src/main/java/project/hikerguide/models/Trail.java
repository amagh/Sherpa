package project.hikerguide.models;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Trail extends BaseModel {
    // ** Constants ** //
    private static final String AREA_ID = "areaId";
    private static final String NAME = "name";
    private static final String NOTES = "notes";

    // ** Member Variables ** //
    public String areaId;
    public String name;
    public String notes;

    public Trail() {}

    public Trail(long id, String areaId, String name, String notes) {
        this.id = id;
        this.areaId = areaId;
        this.name = name;
        this.notes = notes;
    }

    public Trail(long id, String name, String notes) {
        this.id = id;
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
        String areaId = cursor.getString(idxAreaId);
        String name = cursor.getString(idxName);
        String notes = cursor.getString(idxNotes);

        // Create a new Trail from the values
        return new Trail(id, areaId, name, notes);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(ID, id);
        map.put(AREA_ID, areaId);
        map.put(NAME, name);
        map.put(NOTES, notes);

        return map;
    }
}
