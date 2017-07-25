package project.hikerguide.mpandroidchart;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;


/**
 * Created by Alvin on 7/25/2017.
 */

public class ElevationAxisFormatter implements IAxisValueFormatter {
    // ** Constants ** //
    private static final String FEET_LABEL = " ft";
    private static final String METERS_LABELS = " m";

    // ** Member Variables ** //
    Context mContext;
    DecimalFormat mFormat;

    public ElevationAxisFormatter(Context context) {
        // Init the memvars
        mContext = context;
        mFormat = new DecimalFormat("###,###");
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        // Return the formatted elevation
        return mFormat.format(value) + FEET_LABEL;
    }
}
