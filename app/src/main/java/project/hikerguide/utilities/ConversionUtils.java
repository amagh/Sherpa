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

    /**
     * Converts the distance to the correct units based on the user's preference
     *
     * @param context     Interface to global Context
     * @param distance    Distance in meters
     * @return Distance in either miles or kilometers based on user preference
     */
    public static double convertDistance(Context context, double distance) {

        // Get the user's unit preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unitPref = prefs.getString(
                context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_default));

        // Convert units
        if (unitPref.equals(context.getString(R.string.pref_units_imperial))) {
            return distance / METERS_PER_MILE;
        } else {
            return distance / METERS_PER_KILOMETER;
        }
    }

    /**
     * Converts the elevation to the correct units based on the user's preference
     *
     * @param context      Interface to global Context
     * @param elevation    Elevation in meters
     * @return Elevation in either feet or meters based on user preference
     */
    public static double convertElevation(Context context, double elevation) {

        // Get the user's unit prefernce
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unitPref = prefs.getString(
                context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_default));

        // Convert units
        if (unitPref.equals(context.getString(R.string.pref_units_imperial))) {
            return elevation / METERS_PER_FEET;
        } else {
            return elevation;
        }
    }
}
