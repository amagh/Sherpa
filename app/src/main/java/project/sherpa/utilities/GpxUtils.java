package project.sherpa.utilities;

import android.support.annotation.NonNull;

import com.github.mikephil.charting.data.Entry;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
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
import project.sherpa.utilities.objects.GpxStats;

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

                // Initialize the coordinates that will be used to calculate the center of the trail
                double north = 0;
                double south = 0;
                double east = 0;
                double west = 0;

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

                        // For the first point, set all coordinates to the points coordinates
                        if (points.get(i).getLongitude() > points.get(i + 1).getLongitude()) {
                            north = points.get(i).getLongitude();
                            south = points.get(i + 1).getLongitude();
                        } else {
                            north = points.get(i + 1).getLongitude();
                            south = points.get(i).getLongitude();
                        }

                        if (points.get(i).getLatitude() > points.get(i + 1).getLatitude()) {
                            east = points.get(i).getLatitude();
                            west = points.get(i + 1).getLatitude();
                        } else {
                            east = points.get(i + 1).getLatitude();
                            west = points.get(i).getLatitude();
                        }

                    } else {
                        // For all other iterations, set the high elevation if higher or low
                        // elevation if lower
                        double elevation = points.get(i + 1).getElevation();

                        // Set the coordinates if they are more extreme than the previous coordinate
                        double longitude = points.get(i + 1).getLongitude();
                        double latitude = points.get(i + 1).getLatitude();

                        if (elevation < low) {
                            low = elevation;
                        } else if (elevation > high) {
                            high = elevation;
                        }

                        if (north < longitude) {
                            north = longitude;
                        } else if (south > longitude) {
                            south = longitude;
                        }

                        if (east  < latitude) {
                            east = latitude;
                        } else if (west > latitude) {
                            west = latitude;
                        }
                    }
                }

                // Create a GpxStats Object from the stats
                GpxStats gpxStats = new GpxStats();
                gpxStats.distance = totalDistance;
                gpxStats.elevation = high - low;
                gpxStats.latitude = (west + east) / 2;
                gpxStats.longitude = (north + south) / 2;

                return gpxStats;
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Creates a PolylineOptions that can be used to visualize the GPX's coordinates on a
     * MapboxMap. Also creates a Marker to be used to indicate the start of a trail.
     *
     * @param gpxFile    A File corresponding to a GPX file that contains coordinates for a guide
     * @return A PolylineOptions that can be used to plot the trail of a guide
     */
    public static void getMapboxOptions(@NonNull final File gpxFile, final MapboxOptionsListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Initialize the List that will be used to generate the Polyline
                List<LatLng> trailPoints = new ArrayList<>();

                // Get a reference of the LatLng that will be used to mark the start of the trail
                LatLng start = null;

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

                            if (start == null) {
                                // Set the LatLng to be used for the starting coordinate
                                start = new LatLng(point.getLatitude(), point.getLongitude());
                            }
                        }
                    }
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }

                // Create a MarkerOptions for marking the beginning of the trail
                MarkerOptions markerOptions = new MarkerOptions().position(start);

                // Create a PolylineOptions from the List
                PolylineOptions polylineOptions = new PolylineOptions().addAll(trailPoints);

                listener.onOptionReady(markerOptions, polylineOptions);
            }
        }).run();

    }

    /**
     * Calculates the Entries that will be used to plot the elevation data for the LineGraph if the
     * .gpx file includes elevation data. When the data has been calculated, it alerts the calling
     * thread and passes the completed data through.
     *
     * @param gpxFile    .gpx file that wil be used to calculate the elevation chart data
     * @param listener   Listener to alert the calling thread that calculations are complete
     */
    public static void getElevationChartData(final File gpxFile, final ElevationDataListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Init List of Entries to return
                List<Entry> elevationData = new ArrayList<>();

                try {
                    // Create InputStream from File
                    InputStream inStream = new FileInputStream(gpxFile);

                    // Parse the Gpx from the InputStream
                    Gpx parsedGpx = new GPXParser().parse(inStream);

                    if (parsedGpx == null) {
                        // Unable to parse. Nothing to return
                        listener.onElevationDataReady(null);
                    }

                    // Get the TrackPoints from the parsed Gpx
                    List<TrackPoint> trackPoints = parsedGpx.getTracks().get(0).getTrackSegments().get(0).getTrackPoints();

                    // Init the calculator that will be used to get the distance between each point
                    GeodeticCalculator calculator = new GeodeticCalculator();

                    // Keep track of total distance for X-coordinate
                    double totalDistance = 0.0;

                    // Iterate and get the distance traveled between each point and its elevation
                    for (int i = 0; i < trackPoints.size() - 1; i++) {
                        // X-Coordinate = distance traveled
                        // Y-Coordinate = elevation at the end point
                        // Setup
                        double lat1 = trackPoints.get(i).getLatitude();
                        double lon1 = trackPoints.get(i).getLongitude();
                        double ele1 = trackPoints.get(i).getElevation();
                        double lat2 = trackPoints.get(i + 1).getLatitude();
                        double lon2 = trackPoints.get(i + 1).getLongitude();
                        double ele2 = trackPoints.get(i + 1).getElevation();

                        if (i == 0) {
                            // First Entry: The X-coord is 0 for the start, and the starting elevation will
                            // be used as the Y-coord
                            elevationData.add(new Entry(0, (float) ele1));
                        }

                        // Calculate the distance between the two points
                        GeodeticMeasurement measurement = calculator.calculateGeodeticMeasurement(
                                Ellipsoid.WGS84,
                                new GlobalPosition(lat1, lon1, 0),
                                new GlobalPosition(lat2, lon2, 0));

                        // Add the traveled distance to the total distance to keep track of the X-coord
                        totalDistance += measurement.getPointToPointDistance();

                        // Add the Entry to the List
                        elevationData.add(new Entry((float) totalDistance, (float) ele2));
                    }

                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }

                // Check to ensure the Gpx file has elevation data to plot
                float elevationCheck = 0;
                for (Entry entry : elevationData) {
                    // Add the elevation of each Entry to the elevationCheck
                    elevationCheck += entry.getY();

                    // If there is any elevation, then the chart will be valid
                    if (elevationCheck > 0) {
                        break;
                    }
                }

                if (elevationCheck == 0) {
                    // If total elevation is zero, then there is no data to plot
                    listener.onElevationDataReady(null);
                } else {
                    listener.onElevationDataReady(elevationData);
                }
            }
        }).run();
    }

    /**
     * Retrieves the coordinates of the mid-point of a Gpx File
     *
     * @param gpxFile    Gpx File to find the mid-point for.
     * @return LatLngs coordinates for the mid-point of the Gpx File
     */
    public static LatLng getMidPoint(File gpxFile) {
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

                // Get the TrackPoint at the middle of the List
                TrackPoint point = points.get(points.size() / 2);

                return new LatLng(point.getLatitude(), point.getLongitude());
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public interface MapboxOptionsListener {
        void onOptionReady(MarkerOptions markerOptions, PolylineOptions polylineOptions);
    }

    public interface ElevationDataListener {
        void onElevationDataReady(List<Entry> elevationData);
    }
}
