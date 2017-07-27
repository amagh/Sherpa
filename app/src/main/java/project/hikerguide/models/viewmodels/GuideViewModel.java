package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.graphics.drawable.ColorDrawable;
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
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import project.hikerguide.R;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.mpandroidchart.DistanceAxisFormatter;
import project.hikerguide.mpandroidchart.ElevationAxisFormatter;
import project.hikerguide.ui.MapboxActivity;
import project.hikerguide.utilities.ColorGenerator;
import project.hikerguide.utilities.GpxUtils;
import project.hikerguide.utilities.SaveUtils;

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

    // ** Member Variables ** //
    private Context mContext;
    private Guide mGuide;
    private WeakReference<MapboxActivity> mActivity;
    private int mColorPosition;

    public GuideViewModel(Context context, Guide guide) {
        mContext = context;

        // If the passesd Context is a MapboxActivity, then set the mem var to reference it
        if (mContext instanceof MapboxActivity) {
            mActivity = new WeakReference<>((MapboxActivity) mContext);
        }

        mGuide = guide;
    }

    public GuideViewModel(Context context, Guide guide, int colorPosition) {
        mContext = context;

        // If the passesd Context is a MapboxActivity, then set the mem var to reference it
        if (mContext instanceof MapboxActivity) {
            mActivity = new WeakReference<>((MapboxActivity) mContext);
        }

        mGuide = guide;
        mColorPosition = colorPosition;
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
        String difficultyString = "Unknown";

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
    public StorageReference getGpx() {
        return FirebaseStorage.getInstance().getReference()
                .child(GPX_PATH)
                .child(mGuide.firebaseId + GPX_EXT);
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

    @BindingAdapter({"bind:gpx", "bind:activity", "bind:latitude", "bind:longitude"})
    public static void loadGpxToMap(SmartMapView mapView, final StorageReference gpx,
                                    MapboxActivity activity, final double latitude, final double longitude) {

        // The MapView will retain it's internal LifeCycle regardless of how many times it's
        // rendered
        mapView.startMapView(activity);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {

                // Check to make sure the Polyline hasn't already been added the MapboxMap
                // e.g. when scrolling the RecyclerView, the View will be reloaded from memory, so
                // it does not need to re-position the camera or add the Polyline again.
                if (mapboxMap.getPolylines().size() > 0) {
                    return;
                }

                // Set the camera to the correct position
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(latitude, longitude))
                        .build());

                // Parse the FirebaseId from the file name on Firebase Storage
                String firebaseId = gpx.getName().replace(GPX_EXT, "");

                // Create a temporary File where the GPX will be downloaded
                final File tempGpx = SaveUtils.createTempFile(StorageProvider.FirebaseFileType.GPX_FILE, firebaseId);

                if (tempGpx.length() == 0) {

                    // Download the GPX File
                    gpx.getFile(tempGpx)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // Parse the GPX File to get the MapboxOptions
                                    addMapOptionsToMap(tempGpx, mapboxMap);
                                }
                            });
                } else {
                    // Parse the GPX File to get the Mapbox PolyLine and Marker
                    addMapOptionsToMap(tempGpx, mapboxMap);
                }
            }
        });
    }

    @BindingAdapter({"bind:gpx", "bind:context"})
    public static void loadElevationData(final LineChart lineChart, final StorageReference gpx, final Context context) {

        // Parse the FirebaseId from the file name on Firebase Storage
        String firebaseId = gpx.getName().replace(GPX_EXT, "");

        // Create a temporary File where the GPX will be downloaded
        final File tempGpx = SaveUtils.createTempFile(StorageProvider.FirebaseFileType.GPX_FILE, firebaseId);

        if (tempGpx.length() == 0) {

            // Download the GPX File
            gpx.getFile(tempGpx)
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

    @Bindable
    public int getColor() {
        return ColorGenerator.getColor(mContext, mColorPosition);
    }

    @BindingAdapter("bind:color")
    public static void setTrackColor(CircleImageView imageView, int color) {
        // Set the color swatch to match the Guide's track's color
        imageView.setImageDrawable(new ColorDrawable(color));
    }

    /**
     * Adds a PolylineOptions representing the track from a .gpx file to a MapboxMap
     *
     * @param gpxFile      .gpx file containing a track to be plotted on the MapboxMap
     * @param mapboxMap    MapboxMap where the PolylineOptions will be drawn on
     */
    private static void addMapOptionsToMap(File gpxFile, final MapboxMap mapboxMap) {

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

    private static void addElevationDataToLineChart(File gpxFile, final LineChart lineChart, final Context context) {

        // Calculate the Entries for the LineChart from the .gpx data
        GpxUtils.getElevationChartData(gpxFile, new GpxUtils.ElevationDataListener() {
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
