package project.hikerguide.utilities.objects;

import com.github.mikephil.charting.data.Entry;

import java.util.List;

/**
 * Created by Alvin on 7/25/2017.
 */

public class LineGraphOptions {
    private List<Entry> elevationData;

    public LineGraphOptions(List<Entry> elevationData) {
        this.elevationData = elevationData;
    }

    public interface ElevationDataListener {
        void onElevationDataReady(List<Entry> elevationData);
    }
}
