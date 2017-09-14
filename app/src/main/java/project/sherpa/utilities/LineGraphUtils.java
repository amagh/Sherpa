package project.sherpa.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.util.List;

import project.sherpa.R;
import project.sherpa.mpandroidchart.DistanceAxisFormatter;
import project.sherpa.mpandroidchart.ElevationAxisFormatter;

/**
 * Created by Alvin on 7/27/2017.
 */

public class LineGraphUtils {
    // ** Constants ** //
    private static final double METERS_PER_MILE         = 1609.34;
    private static final double METERS_PER_FEET         = 0.3048;
    private static final double METERS_PER_KILOMETER    = 1000;

    public static void addElevationDataToLineChart(File gpxFile, final LineChart lineChart, final Context context) {

        // Calculate the Entries for the LineChart from the .gpx data
        GpxUtils.getElevationChartData(gpxFile, new GpxUtils.ElevationDataListener() {
            @Override
            public void onElevationDataReady(List<Entry> elevationData) {
                if (elevationData == null) {
                    return;
                }

                double distanceConversion   = GeneralUtils.isUnitPreferenceMetric(context)
                        ? METERS_PER_KILOMETER
                        : METERS_PER_MILE;
                double heightConversion     = GeneralUtils.isUnitPreferenceMetric(context)
                        ? 1
                        : METERS_PER_FEET;

                for (Entry entry : elevationData) {
                    // Convert to imperial
                    entry.setX((float) (entry.getX() / distanceConversion));
                    entry.setY((float) (entry.getY() / heightConversion));
                }

                float totalDistance = elevationData.get(elevationData.size() - 1).getX();

                // Set the number of labels to display on the chart based on the total distance of
                // the trail
                float interval = GeneralUtils.isUnitPreferenceMetric(context)
                        ? 5.0f      // 5.0 km interval as minimum
                        : 2.5f;     // 2.5 mi interval as minimum

                // Calculate how many labels there would be if the interval were 2.5 miles. Use
                // floor instead of round() because fractions of a label can't be shown
                int numLabels;

                while ((numLabels = (int) Math.floor(totalDistance / interval)) > 5) {
                    // Double the interval until there are less than 5 labels in the graph
                    // 2.5 mi > 5.0 mi > 10 mi > 20 mi etc
                    interval *= 2;
                }

                // Init the Array of labels to show
                float[] labels = new float[numLabels + 1];

                // Set the first and last items as 0 (beginning) and the total distance of the
                // trail respectively
                labels[0] = 0;

                // Init the distance variable that will be used to calculate the labels
                float distance = interval;

                for (int i = 1; i < numLabels + 1; i++) {
                    // Set the label
                    labels[i] = distance;

                    // Increment the label by the interval amount
                    distance += interval;
                }

                // Convert the data to a LineDataSet that can be applied to the LineChart
                LineDataSet dataSet = new LineDataSet(elevationData, null);

                // Remove the indicators for individual points
                dataSet.setDrawCircles(false);

                // Set the color of the line
                dataSet.setColor(ContextCompat.getColor(context, R.color.green_700));

                // Set width of line
                dataSet.setLineWidth(2);

                // Disable the legend
                lineChart.getLegend().setEnabled(false);

                // Set up the X-Axis
                lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                lineChart.getXAxis().setValueFormatter(new DistanceAxisFormatter(context));
                lineChart.getXAxis().setShowSpecificLabelPositions(true);
                lineChart.getXAxis().setSpecificLabelPositions(labels);

                // Set up the Y-Axes
                float granularity = GeneralUtils.isUnitPreferenceMetric(context)
                        ? 250f
                        : 500f;

                lineChart.getAxisRight().setValueFormatter(new ElevationAxisFormatter(context));
                lineChart.getAxisRight().setGranularity(granularity);
                lineChart.getAxisLeft().setValueFormatter(new ElevationAxisFormatter(context));
                lineChart.getAxisLeft().setGranularity(granularity);

                // Remove the description label from the chart
                Description description = new Description();
                description.setText("");
                lineChart.setDescription(description);

                // Disable zooming on the chart
                lineChart.setDoubleTapToZoomEnabled(false);
                lineChart.setPinchZoom(false);

                // Set the data to the chart and invalidate to refresh it.
                lineChart.setData(new LineData(dataSet));
                lineChart.invalidate();
            }
        });
    }
}