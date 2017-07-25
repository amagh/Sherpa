package project.hikerguide.mpandroidchart;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;

import project.hikerguide.R;

/**
 * Created by Alvin on 7/25/2017.
 */

public class DistanceAxisFormatter implements IAxisValueFormatter {
    // ** Member Variables ** //
    Context mContext;

    private static final double MILES_TO_METERS = 1609.34;

    public DistanceAxisFormatter(Context context) {
        // Initialize the mem vars
        mContext = context;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        // Return the formatted String to be used as the axis labels
        return mContext.getString(R.string.list_guide_format_distance_imperial, value);
    }
}
