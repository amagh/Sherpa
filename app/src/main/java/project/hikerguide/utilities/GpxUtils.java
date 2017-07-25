package project.hikerguide.utilities;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticMeasurement;
import org.gavaghan.geodesy.GlobalPosition;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import project.hikerguide.utilities.objects.MapboxOptions;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GpxUtils {

    /**
     * Uses Vincenty's formulate to calculate the distance traveled from a GPX file. Also
     * calculates the max difference in altitude for the track. This means that if the trail starts
     * and ends at the same point, it will still calculate the difference in altitude from the
     * highest and lowest points on the trail.
     *
     * @param gpxFile    File corresponding to a GPX file that contains coordinates for a guide
     * @return A GpxStats Object containing the calculates distance and elevation
     */
    public static GpxStats getGpxStats(File gpxFile) {
        try {
            // Create a FileInputStream from the Gpx File
            InputStream inStream = new FileInputStream(gpxFile);

            // Parse to a Gpx
            Gpx parsedGpx = new GPXParser().parse(inStream);

            // Close the FileInputStream
            inStream.close();

            if (parsedGpx != null) {
                // Get the TrackPoints from the Gpx
                List<TrackPoint> points = parsedGpx.getTracks().get(0).getTrackSegments().get(0).getTrackPoints();

                // Initialize the variables that will be used to calculate the distance and elevation
                double totalDistance = 0.0;
                double low = 0.0;
                double high = 0.0;

                // Init the Geodetic Calculater (Uses Vincenty's formula for higher accuracy)
                GeodeticCalculator calculator = new GeodeticCalculator();

                // Iterate through each pair of points and calculate the distance between them
                for (int i = 0; i < points.size() - 1; i++) {
                    // Ellipsoid.WGS84 is the commonly accepted model for Earth
                    GeodeticMeasurement measurement = calculator.calculateGeodeticMeasurement(Ellipsoid.WGS84,
                            // Note: Distance is calculated ignoring elevation
                            // First point
                            new GlobalPosition(points.get(i).getLatitude(), points.get(i).getLongitude(), 0),
                            // Second point
                            new GlobalPosition(points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude(), 0));

                    // Add the distance between the two points to the total distance
                    totalDistance += measurement.getPointToPointDistance();

                    // Find the high and low elevation
                    if (i == 0) {
                        // For the first point, set the respective elevations for the low and high
                        boolean firstHigher = points.get(i).getElevation() > points.get(i + 1).getElevation();
                        if (firstHigher) {
                            low = points.get(i + 1).getElevation();
                            high = points.get(i).getElevation();
                        } else {
                            low = points.get(i).getElevation();
                            high = points.get(i + 1).getElevation();
                        }
                    } else {
                        // For all other iterations, set the high elevation if higher or low
                        // elevation if lower
                        double elevation = points.get(i + 1).getElevation();

                        if (elevation < low) {
                            low = elevation;
                        } else if (elevation > high) {
                            high = elevation;
                        }
                    }
                }

                // Create a GpxStats Object from the stats
                GpxStats gpxStats = new GpxStats();
                gpxStats.distance = totalDistance;
                gpxStats.elevation = high - low;

                return gpxStats;
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates a PolylineOptions that can be used to visualize the GPX's coordinates on a MapboxMap.
     * Also calculates the center of the trail so that the map can be centered as the correct
     * coordinates.
     *
     * @param gpxFile    A File corresponding to a GPX file that contains coordinates for a guide
     * @return A PolylineOptions that can be used to plot the trail of a guide
     */
    public static void getMapboxOptions(@NonNull final File gpxFile, final MapboxOptions.MapboxListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Initialize the List that will be used to generate the Polyline
                List<LatLng> trailPoints = new ArrayList<>();

                // Initialize the coordinates that will be used to calculate the center of the trail
                double north = 0;
                double south = 0;
                double east = 0;
                double west = 0;

                try {
                    // Create an InputStream from gpxFile
                    FileInputStream inStream = new FileInputStream(gpxFile);

                    // Parse a Gpx from the FileInputStream
                    Gpx parsedGpx = new GPXParser().parse(inStream);

                    // Close the InputStream
                    inStream.close();

                    if (parsedGpx != null) {
                        // Get the individual points from the Gpx
                        List<TrackPoint> points = parsedGpx.getTracks().get(0).getTrackSegments().get(0).getTrackPoints();

                        // Iterate through and convert the point coordinates to a LatLng
                        for (TrackPoint point : points) {
                            LatLng trailPoint = new LatLng(point.getLatitude(), point.getLongitude(), point.getElevation());

                            // Add the LatLng to the List
                            trailPoints.add(trailPoint);

                            double latitude = trailPoint.getLatitude();
                            double longitude = trailPoint.getLongitude();

                            if (north == 0) {
                                // For the first point, set all coordinates to the points coordinates
                                north = longitude;
                                south = longitude;
                                east = latitude;
                                west = latitude;
                            } else {
                                // If the new coordinates are more extreme than the values set, then set the
                                // values to the new coordinates
                                if (north < longitude) {
                                    north = longitude;
                                } else if (south > longitude) {
                                    south = longitude;
                                }

                                if (east < latitude) {
                                    east = latitude;
                                } else if (west > latitude) {
                                    west = latitude;
                                }
                            }
                        }
                    }
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }

                // Calculate the center coordinates as the average of the east-most and west-most
                // coordinates for the Latitude and the average of the north-most and south-most
                // coordinates for the Longitude
                LatLng center = new LatLng((west + east) / 2, (north + south) / 2);

                // Create a PolylineOptions from the List
                PolylineOptions polylineOptions = new PolylineOptions().addAll(trailPoints);

                listener.onOptionReady(new MapboxOptions(polylineOptions, center));
            }
        }).run();

    }
}
