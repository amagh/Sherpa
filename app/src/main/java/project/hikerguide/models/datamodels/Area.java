package project.hikerguide.models.datamodels;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import project.hikerguide.data.GuideContract;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 7/18/2017.
 */

public class Area extends BaseModel implements Parcelable {
    // ** Constants ** //
    private static final String NAME            = "name";
    private static final String LOWER_CASE_NAME = "lowerCaseName";
    private static final String LATITUDE        = "latitude";
    private static final String LONGITUDE       = "longitude";
    private static final String STATE           = "state";

    // ** Member Variables ** //
    public String name;
    public String state;
    public double latitude;
    public double longitude;

    public Area() {}

    public Area(String name) {
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
        return new Area(name);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(NAME, name);
        map.put(LOWER_CASE_NAME, name.toLowerCase());
        map.put(LATITUDE, latitude);
        map.put(LONGITUDE, longitude);
        map.put(STATE, state);

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
        parcel.writeString(state);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
    }

    public static final Parcelable.Creator<Area> CREATOR = new Parcelable.Creator<Area>() {
        @Override
        public Area createFromParcel(Parcel parcel) {
            return new Area(parcel);
        }

        @Override
        public Area[] newArray(int i) {
            return new Area[i];
        }
    };

    private Area(Parcel parcel) {
        firebaseId = parcel.readString();
        name = parcel.readString();
        state = parcel.readString();
        latitude = parcel.readDouble();
        longitude = parcel.readDouble();
    }
}
