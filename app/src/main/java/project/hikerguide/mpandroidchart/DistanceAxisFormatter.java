package project.hikerguide.mpandroidchart;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;

/**
 * Created by Alvin on 7/25/2017.
 */

public class DistanceAxisFormatter implements IAxisValueFormatter {
    // ** Constants ** //
    private static String MILES_LABEL = " mi";
    private static String KILOMETERS_LABEL = " km";

    // ** Member Variables ** //
    Context mContext;
    DecimalFormat mFormat;

    private static final double MILES_TO_METERS = 1609.34;

    public DistanceAxisFormatter(Context context) {
        // Initialize the mem vars
        mContext = context;
        mFormat = new DecimalFormat("###,###.##");
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        // Return the formatted String to be used as the axis labels
        return mFormat.format(value) + MILES_LABEL;
    }
}
