package project.hikerguide.utilities;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.io.File;

/**
 * Created by Alvin on 7/27/2017.
 */

public class MapUtils {

    /**
     * Adds a PolylineOptions representing the track from a .gpx file to a MapboxMap
     *
     * @param gpxFile      .gpx file containing a track to be plotted on the MapboxMap
     * @param mapboxMap    MapboxMap where the PolylineOptions will be drawn on
     */
    public static void addMapOptionsToMap(File gpxFile, final MapboxMap mapboxMap) {

        // Parse the GPX File to get the Mapbox PolyLine and Marker
        GpxUtils.getMapboxOptions(gpxFile, new GpxUtils.MapboxOptionsListener() {
            @Override
            public void onOptionReady(MarkerOptions markerOptions, PolylineOptions polylineOptions) {
                // Set the Marker for the start of the trail
                mapboxMap.addMarker(markerOptions);

                // Set the Polyline representing the trail
                mapboxMap.addPolyline(polylineOptions
                        .width(3));
            }
        });
    }
}
