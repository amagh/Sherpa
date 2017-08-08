package project.hikerguide.models.datamodels;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Trail extends BaseModel implements Parcelable {
    // ** Constants ** //
    private static final String AREA_ID = "areaId";
    private static final String NAME = "name";
    private static final String LOWER_CASE_NAME = "lowerCaseName";
    private static final String NOTES = "notes";

    // ** Member Variables ** //
    public String areaId;
    public String name;
    public String notes;

    public Trail() {}

    public Trail(String areaId, String name, String notes) {
        this.areaId = areaId;
        this.name = name;
        this.notes = notes;
    }

    public Trail(String name, String notes) {
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
        int idxFirebaseId   = cursor.getColumnIndex(GuideContract.TrailEntry.FIREBASE_ID);
        int idxAreaId       = cursor.getColumnIndex(GuideContract.TrailEntry.AREA_ID);
        int idxName         = cursor.getColumnIndex(GuideContract.TrailEntry.NAME);
        int idxNotes        = cursor.getColumnIndex(GuideContract.TrailEntry.NOTES);

        // Retrieve the values from the Cursor
        String firebaseId   = cursor.getString(idxFirebaseId);
        String areaId       = cursor.getString(idxAreaId);
        String name         = cursor.getString(idxName);
        String notes        = cursor.getString(idxNotes);

        // Create a new Trail from the values
        Trail trail         = new Trail();
        trail.firebaseId    = firebaseId;
        trail.areaId        = areaId;
        trail.name          = name;
        trail.notes         = notes;

        return trail;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(AREA_ID, areaId);
        map.put(NAME, name);
        map.put(LOWER_CASE_NAME, name.toLowerCase());
        map.put(NOTES, notes);

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
        parcel.writeString(name);
        parcel.writeString(notes);
    }

    public static final Parcelable.Creator<Trail> CREATOR = new Parcelable.Creator<Trail>() {
        @Override
        public Trail createFromParcel(Parcel parcel) {
            return new Trail(parcel);
        }

        @Override
        public Trail[] newArray(int i) {
            return new Trail[i];
        }
    };

    private Trail(Parcel parcel) {
        firebaseId = parcel.readString();
        name = parcel.readString();
        notes = parcel.readString();
    }
}
