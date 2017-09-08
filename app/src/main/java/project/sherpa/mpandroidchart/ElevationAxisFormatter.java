package project.sherpa.mpandroidchart;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import project.sherpa.R;


/**
 * Created by Alvin on 7/25/2017.
 */

public class ElevationAxisFormatter implements IAxisValueFormatter {
    // ** Member Variables ** //
    Context mContext;

    public ElevationAxisFormatter(Context context) {
        // Init the memvars
        mContext = context;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        // Return the formatted elevation
        return mContext.getString(R.string.list_guide_format_elevation_imperial, value);
    }
}
