package project.sherpa.utilities;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import project.sherpa.R;

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

        // Check to see if the requested color is the highlight color
        if (colorPosition == HIGHLIGHT_POSITION) {
            return ContextCompat.getColor(context, R.color.colorHighlight);
        }

        // Retrieve the Array of Colors
        int[] colorArray = context.getResources().getIntArray(R.array.track_color_array);

        // If selecting a color greater than the length of the Array of colors, cycle through the
        // colors
        while (colorPosition >= colorArray.length) {
            colorPosition -= colorArray.length;
        }

        return colorArray[colorPosition];
    }

}
