package project.hikerguide.utilities;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import project.hikerguide.R;

/**
 * Created by Alvin on 7/26/2017.
 */

public class ColorGenerator {
    // ** Constants ** //
    public static final int HIGHLIGHT_POSITION = -99;

    /**
     * Retrieves a color from the color array resource
     *
     * @param context          Interface to global Context
     * @param colorPosition    The position of the color in the array to be retrieved
     * @return The color as an int
     */
    public static int getColor(Context context, int colorPosition) {
        if (colorPosition == HIGHLIGHT_POSITION) {
            return ContextCompat.getColor(context, R.color.yellow_a200);
        }

        return context.getResources().getIntArray(R.array.track_color_array)[colorPosition];
    }

}
