package project.sherpa.models.datamodels;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.models.AreaAdapterSortable;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 7/18/2017.
 */

public class Area extends BaseModel implements Parcelable, AreaAdapterSortable {
    // ** Constants ** //
    private static final String NAME            = "name";
    public static final String LOWER_CASE_NAME  = "lowerCaseName";
    private static final String LATITUDE        = "latitude";
    private static final String LONGITUDE       = "longitude";
    private static final String LOCATION        = "location";

    // ** Member Variables ** //
    public String name;
    public String location;
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
    public static Area createAreaFromCursor(Cursor cursor) {

        // Index the columns of the Cursor
        int idxFirebaseId   = cursor.getColumnIndex(GuideContract.AreaEntry.FIREBASE_ID);
        int idxName         = cursor.getColumnIndex(GuideContract.AreaEntry.NAME);
        int idxLatitude     = cursor.getColumnIndex(GuideContract.AreaEntry.LATITUDE);
        int idxLongitude    = cursor.getColumnIndex(GuideContract.AreaEntry.LONGITUDE);
        int idxLocation     = cursor.getColumnIndex(GuideContract.AreaEntry.LOCATION);
        int idxDraft        = cursor.getColumnIndex(GuideContract.AreaEntry.DRAFT);

        // Retrieve the values from the Cursor
        String firebaseId   = cursor.getString(idxFirebaseId);
        String name         = cursor.getString(idxName);
        double latitude     = cursor.getDouble(idxLatitude);
        double longitude    = cursor.getDouble(idxLongitude);
        String location     = cursor.getString(idxLocation);
        boolean draft       = cursor.getInt(idxDraft) == 1;

        // Create a new Area with the values
        Area area           = new Area();
        area.firebaseId     = firebaseId;
        area.name           = name;
        area.latitude       = latitude;
        area.longitude      = longitude;
        area.location       = location;
        area.setDraft(draft);

        return area;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(NAME, name);
        map.put(LOWER_CASE_NAME, name.toLowerCase());
        map.put(LATITUDE, latitude);
        map.put(LONGITUDE, longitude);
        map.put(LOCATION, location);

        return map;
    }

    @Override
    public String getName() {
        return name;
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
        parcel.writeString(location);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);

        if (isDraft()) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }
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
        firebaseId  = parcel.readString();
        name        = parcel.readString();
        location    = parcel.readString();
        latitude    = parcel.readDouble();
        longitude   = parcel.readDouble();

        if (parcel.readInt() == 1) {
            setDraft(true);
        }
    }
}
