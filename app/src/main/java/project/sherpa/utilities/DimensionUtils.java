package project.sherpa.utilities;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Created by Alvin on 9/14/2017.
 */

public class DimensionUtils {

    /**
     * Converts dp unit to equivalent pixels, depending on device density.
     *
     * @param context    Interface to global Context
     * @param dp         A value in dp (density independent pixels) unit to be converted into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(Context context, float dp){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * Converts device specific pixels to density independent pixels.
     *
     * @param context    Interface to global Context
     * @param px         A value in px (pixels) unit to be convereted into dp.
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }
}
