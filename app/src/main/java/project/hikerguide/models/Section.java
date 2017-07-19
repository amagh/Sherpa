package project.hikerguide.models;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.models.abstractmodels.BaseModelWithImage;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Section extends BaseModelWithImage {
    // ** Constants ** //
    private static final String GUIDE_ID = "guideId";
    private static final String SECTION = "section";
    private static final String CONTENT = "content";

    // ** Member Variables ** //
    public String guideId;
    public int section;
    public String content;

    public Section() {}

    public Section(long id, String guideId, int section, String content) {
        this.id = id;
        this.guideId = guideId;
        this.section = section;
        this.content = content;
    }

    public Section(long id, int section, String content) {
        this.id = id;
        this.section = section;
        this.content = content;
    }

    /**
     * Creates a Section using the values described in a Cursor
     *
     * @param cursor    Cursor describing a Section of a Guide
     * @return Section with the values contained in the Cursor
     */
    public Section createSectionFromCursor(Cursor cursor) {
        // Index the columns of the Cursor
        int idxId = cursor.getColumnIndex(GuideContract.SectionEntry._ID);
        int idxGuideId = cursor.getColumnIndex(GuideContract.SectionEntry.GUIDE_ID);
        int idxSection = cursor.getColumnIndex(GuideContract.SectionEntry.SECTION);
        int idxContent = cursor.getColumnIndex(GuideContract.SectionEntry.CONTENT);

        // Retrieve the values from the Cursor
        long id = cursor.getLong(idxId);
        String guideId = cursor.getString(idxGuideId);
        int section = cursor.getInt(idxSection);
        String content = cursor.getString(idxContent);

        // Instantiate a new Section with the values
        return new Section(id, guideId, section, content);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(ID, id);
        map.put(GUIDE_ID, guideId);
        map.put(SECTION, section);
        map.put(CONTENT, content);

        return map;
    }
}
