package project.hikerguide.utilities.objects;

import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Created by Alvin on 7/24/2017.
 */

public class MapboxOptions {
    private PolylineOptions options;
    private LatLng center;

    public MapboxOptions(PolylineOptions options, LatLng center) {
        this.options = options;
        this.center = center;
    }

    public void setPolylineOptions(PolylineOptions options) {
        this.options = options;
    }

    public void setCenter(LatLng center) {
        this.center = center;
    }

    public PolylineOptions getPolylineOptions() {
        return options;
    }

    public LatLng getCenter() {
        return center;
    }
}
