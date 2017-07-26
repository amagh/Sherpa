package project.hikerguide.utilities;

import android.content.Context;
import android.graphics.Color;

import project.hikerguide.R;

/**
 * Created by Alvin on 7/26/2017.
 */

public class ColorGenerator {

    /**
     * Retrieves a color from the color array resource
     *
     * @param context          Interface to global Context
     * @param colorPosition    The position of the color in the array to be retrieved
     * @return The color as an int
     */
    public static int getColor(Context context, int colorPosition) {
        return context.getResources().getIntArray(R.array.track_color_array)[colorPosition];
    }

}
