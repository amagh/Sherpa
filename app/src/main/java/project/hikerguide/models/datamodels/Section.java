package project.hikerguide.models.datamodels;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.models.datamodels.abstractmodels.BaseModelWithImage;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Section extends BaseModelWithImage implements Parcelable {
    // ** Constants ** //
    private static final String GUIDE_ID = "guideId";
    private static final String SECTION = "section";
    private static final String CONTENT = "content";
    private static final String HAS_IMAGE = "hasImage";

    // ** Member Variables ** //
    public String guideId;
    public int section;
    public String content;

    public Section() {}

    public Section(String guideId, int section, String content) {
        this.guideId = guideId;
        this.section = section;
        this.content = content;
    }

    public Section(int section, String content) {
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
        return new Section(guideId, section, content);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(GUIDE_ID, guideId);
        map.put(SECTION, section);
        map.put(CONTENT, content);
        map.put(HAS_IMAGE, hasImage);

        return map;
    }

    //********************************************************************************************//
    //***************************** Parcelable Related Methods ***********************************//
    //********************************************************************************************//


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(firebaseId);
        parcel.writeInt(section);
        parcel.writeString(content);

        if (hasImage) {
            parcel.writeString(getImageUri().toString());
        }
    }

    public static final Parcelable.Creator<Section> CREATOR = new Parcelable.Creator<Section>() {
        @Override
        public Section createFromParcel(Parcel parcel) {
            return new Section(parcel);
        }

        @Override
        public Section[] newArray(int i) {
            return new Section[i];
        }
    };

    private Section(Parcel parcel) {
        firebaseId = parcel.readString();
        section = parcel.readInt();
        content = parcel.readString();

        String imageUriString = parcel.readString();
        if (imageUri != null) {
            imageUri = Uri.parse(imageUriString);
        }
    }
}
