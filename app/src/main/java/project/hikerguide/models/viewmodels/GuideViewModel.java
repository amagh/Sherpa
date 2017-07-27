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
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.mpandroidchart.DistanceAxisFormatter;
import project.hikerguide.mpandroidchart.ElevationAxisFormatter;
import project.hikerguide.ui.MapboxActivity;
import project.hikerguide.utilities.ColorGenerator;
import project.hikerguide.utilities.ConversionUtils;
import project.hikerguide.utilities.GpxUtils;
import project.hikerguide.utilities.SaveUtils;

import static project.hikerguide.utilities.LineGraphUtils.addElevationDataToLineChart;
import static project.hikerguide.utilities.MapUtils.addMapOptionsToMap;
import static project.hikerguide.utilities.StorageProviderUtils.GPX_EXT;
import static project.hikerguide.utilities.StorageProviderUtils.GPX_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideViewModel extends BaseObservable {


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
                ConversionUtils.convertDistance(mContext, mGuide.distance));
    }

    @Bindable
    public String getElevation() {
        return mContext.getString(
                R.string.list_guide_format_elevation_imperial,
                ConversionUtils.convertElevation(mContext, mGuide.elevation));
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
    public File getGpx() {
        if (mGuide.firebaseId == null) {
            return new File(mGuide.getGpxUri().getPath());
        } else {

            // Create a temporary File where the GPX will be downloaded
            final File tempGpx = SaveUtils.createTempFile(StorageProvider.FirebaseFileType.GPX_FILE, mGuide.firebaseId);

            if (tempGpx.length() == 0) {

                // Download the GPX File
                FirebaseStorage.getInstance().getReference()
                        .child(GPX_PATH)
                        .child(mGuide.firebaseId + GPX_EXT)
                        .getFile(tempGpx)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                // Parse the GPX File to get the MapboxOptions
                                notifyPropertyChanged(BR.gpx);
                            }
                        });

                return null;
            } else {
                return tempGpx;
            }
        }
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
    public static void loadGpxToMap(final SmartMapView mapView, final File gpx,
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

                // In the case of creating a new Guide without a GPX loaded, show the world map
                if (latitude == 0 && longitude == 0) {
                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .zoom(0)
                            .build());
                }

                // Set the camera to the correct position
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(latitude, longitude))
                        .build());

                // Parse the GPX File to get the Mapbox PolyLine and Marker
                addMapOptionsToMap(gpx, mapboxMap);
            }
        });
    }

    @BindingAdapter({"bind:gpx", "bind:context"})
    public static void loadElevationData(final LineChart lineChart, final File gpx, final Context context) {

        // Parse the GPX data to get the elevation profile and add it the LineChart
        addElevationDataToLineChart(gpx, lineChart, context);
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

}
