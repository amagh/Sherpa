package project.hikerguide.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import project.hikerguide.R;

/**
 * Created by Alvin on 7/27/2017.
 */

public class ConversionUtils {
    // ** Constants ** //
    private static final double METERS_PER_MILE = 1609.34;
    private static final double METERS_PER_FEET = 0.3048;
    private static final double METERS_PER_KILOMETER = 1000;

    public static double convertDistance(Context context, double distance) {

        return distance / METERS_PER_MILE;
    }

    public static double convertElevation(Context context, double elevation) {

        return elevation / METERS_PER_FEET;
    }
}
