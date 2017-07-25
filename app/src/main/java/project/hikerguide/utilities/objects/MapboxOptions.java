package project.hikerguide.utilities.objects;

import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Created by Alvin on 7/24/2017.
 */

public class MapboxOptions {
    private PolylineOptions options;

    public MapboxOptions(PolylineOptions options) {
        this.options = options;
    }

    public PolylineOptions getPolylineOptions() {
        return options;
    }

    public interface MapboxListener {
        void onOptionReady(PolylineOptions options);
    }
}
