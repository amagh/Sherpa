package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.mpandroidchart.DistanceAxisFormatter;
import project.hikerguide.mpandroidchart.ElevationAxisFormatter;
import project.hikerguide.ui.MapboxActivity;
import project.hikerguide.utilities.GpxUtils;
import project.hikerguide.utilities.objects.LineGraphOptions;
import project.hikerguide.utilities.objects.MapboxOptions;

import static project.hikerguide.utilities.StorageProviderUtils.GPX_EXT;
import static project.hikerguide.utilities.StorageProviderUtils.GPX_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideViewModel extends BaseObservable {
    // ** Constants ** //
    private static final double METERS_PER_MILE = 1609.34;
    private static final double METERS_PER_FEET = 0.3048;
    private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir", "."));
    private static final float TWENTY_MI_IN_KM = 32186.9f;

    // ** Member Variables ** //
    private Context mContext;
    private Guide mGuide;
    private WeakReference<MapboxActivity> mActivity;

    public GuideViewModel(Context context, Guide guide) {
        mContext = context;

        // If the passesd Context is a MapboxActivity, then set the mem var to reference it
        if (mContext instanceof MapboxActivity) {
            mActivity = new WeakReference<>((MapboxActivity) mContext);
        }

        mGuide = guide;
    }

    @Bindable
    public String getTrailName() {
        return mGuide.trailName;
    }

    @Bindable
    public String getAreaName() {
        return mGuide.area;
    }

    @Bindable
    public String getDistance() {
        return mContext.getString(
                R.string.list_guide_format_distance_imperial,
                mGuide.distance / METERS_PER_MILE);
    }

    @Bindable
    public String getElevation() {
        return mContext.getString(
                R.string.list_guide_format_elevation_imperial,
                mGuide.elevation / METERS_PER_FEET);
    }

    @Bindable
    public String getRating() {
        if (mGuide.reviews != 0) {
            return mContext.getString(R.string.list_guide_format_rating, mGuide.rating / mGuide.reviews);
        } else {
            return mContext.getString(R.string.list_guide_format_rating_zero);
        }
    }

    @Bindable
    public String getReviews() {
        return mContext.getString(R.string.list_guide_format_reviews, mGuide.reviews);
    }

    @Bindable
    public String getDifficulty() {
        String difficultyString;

        switch (mGuide.difficulty) {
            case 1:
                difficultyString = mContext.getString(R.string.difficulty_easy);
                break;

            case 2:
                difficultyString = mContext.getString(R.string.difficulty_moderate);
                break;

            case 3:
                difficultyString = mContext.getString(R.string.difficulty_hard);
                break;

            case 4:
                difficultyString = mContext.getString(R.string.difficulty_expert);
                break;

            case 5:
                difficultyString = mContext.getString(R.string.difficulty_extreme);
                break;

            default: difficultyString = "Unknown";
        }

        return difficultyString;
    }

    @Bindable
    public StorageReference getImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mGuide.firebaseId + JPEG_EXT);
    }

    @BindingAdapter("bind:image")
    public static void loadImage(ImageView imageView, StorageReference image) {
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                .load(image)
                .into(imageView);
    }

    @Bindable
    public StorageReference getAuthorImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mGuide.authorId + JPEG_EXT);
    }

    @BindingAdapter("bind:authorImage")
    public static void loadAuthorImage(ImageView imageView, StorageReference authorImage) {
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                // Use default profile image if the author does not have one
                .load(authorImage)
                .error(R.drawable.ic_account_circle)
                .into(imageView);
    }

    @Bindable
    public String getAuthor() {
        return mGuide.authorName;
    }

    @Bindable
    public String getFirebaseId() {
        return mGuide.firebaseId;
    }

    @Bindable
    public MapboxActivity getActivity() {
        return mActivity.get();
    }

    @Bindable
    public double getLatitude() {
        return mGuide.latitude;
    }

    @Bindable
    public double getLongitude() {
        return mGuide.longitude;
    }

    @Bindable
    public Context getContext() {
        return mContext;
    }

    @Bindable
    public int getElevationVisibility() {
        return mGuide.elevation != 0 ? View.VISIBLE : View.GONE;
    }

    @BindingAdapter({"bind:firebaseId", "bind:activity", "bind:latitude", "bind:longitude"})
    public static void loadGpxToMap(SmartMapView mapView, final String firebaseId,
                                    MapboxActivity activity, final double latitude, final double longitude) {

        // The MapView will retain it's internal LifeCycle regardless of how many times it's
        // rendered
        mapView.startMapView(activity);

        // Attach the MapView to the Activity so it can follow the rest of the lifecycle
//        activity.attachMapView(mapView);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                // Set the Map Style to the outdoor view with elevation
                mapboxMap.setStyleUrl(Style.OUTDOORS);

                // Create a temporary File where the GPX will be downloaded
                final File tempGpx = new File(TEMP_DIRECTORY, firebaseId + GPX_EXT);

                if (tempGpx.length() == 0) {
                    // Download the GPX File
                    FirebaseStorage.getInstance().getReference()
                            .child(GPX_PATH)
                            .child(firebaseId + GPX_EXT)
                            .getFile(tempGpx)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // Parse the GPX File to get the MapboxOptions
                                    addPolylineOptionsToMapView(tempGpx, mapboxMap, latitude, longitude);
                                }
                            });
                } else {
                    // Parse the GPX File to get the MapboxOptions
                    addPolylineOptionsToMapView(tempGpx, mapboxMap, latitude, longitude);
                }


            }
        });
    }

    @BindingAdapter({"bind:firebaseId", "bind:context"})
    public static void loadElevationData(final LineChart lineChart, final String firebaseId, final Context context) {

        // Create a temporary File where the GPX will be downloaded
        final File tempGpx = new File(TEMP_DIRECTORY, firebaseId + GPX_EXT);

        if (tempGpx.length() == 0) {
            // Download the GPX File
            FirebaseStorage.getInstance().getReference()
                    .child(GPX_PATH)
                    .child(firebaseId + GPX_EXT)
                    .getFile(tempGpx)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            addElevationDataToLineChart(tempGpx, lineChart, context);
                        }
                    });
        } else {
            addElevationDataToLineChart(tempGpx, lineChart, context);
        }

    }

    /**
     * Adds a PolylineOptions representing the track from a .gpx file to a MapboxMap
     *
     * @param gpxFile      .gpx file containing a track to be plotted on the MapboxMap
     * @param mapboxMap    MapboxMap where the PolylineOptions will be drawn on
     * @param latitude     Latitude coordinate where the map should be centered
     * @param longitude    Longitude coordinate where the map should centered
     */
    private static void addPolylineOptionsToMapView(File gpxFile, final MapboxMap mapboxMap,
                                                   final double latitude, final double longitude) {

        // Parse the GPX File to get the MapboxOptions
        GpxUtils.getPolylineOptions(gpxFile, new MapboxOptions.MapboxListener() {
            @Override
            public void onOptionReady(PolylineOptions options) {
                // Set the Polyline representing the trail
                mapboxMap.addPolyline(options
                        .width(3));

                // Set the camera to the correct position
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(latitude, longitude))
                        .zoom(11)
                        .build());
            }
        });
    }

    private static void addElevationDataToLineChart(File gpxFile, final LineChart lineChart, final Context context) {

        // Calculate the Entries for the LineChart from the .gpx data
        GpxUtils.getElevationChartData(gpxFile, new LineGraphOptions.ElevationDataListener() {
            @Override
            public void onElevationDataReady(List<Entry> elevationData) {
                if (elevationData == null) {
                    return;
                }

                for (Entry entry : elevationData) {
                    // Convert to imperial
                    entry.setX((float) (entry.getX() / METERS_PER_MILE));
                    entry.setY((float) (entry.getY() / METERS_PER_FEET));
                }

                float totalDistance = elevationData.get(elevationData.size() - 1).getX();

                // Set the number of labels to display on the chart based on the total distance of
                // the trail
                float interval = 2.5f;      // 2.5 mi interval as minimum

                // Calculate how many labels there would be if the interval were 2.5 miles. Use
                // floor instead of round() because fractions of a label can't be shown
                int numLabels = (int) Math.floor(totalDistance / interval);

                while (numLabels > 6) {
                    // Double the interval until there are less than 6 labels in the graph
                    // 2.5 mi > 5.0 mi > 10 mi > 20 mi etc
                    interval *= 2;
                    numLabels = (int) Math.floor(totalDistance / interval);
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
                dataSet.setColor(context.getResources().getColor(R.color.green_700));

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
                lineChart.getAxisRight().setValueFormatter(new ElevationAxisFormatter(context));
                lineChart.getAxisRight().setGranularity(500f);
                lineChart.getAxisLeft().setValueFormatter(new ElevationAxisFormatter(context));
                lineChart.getAxisLeft().setGranularity(500f);

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
