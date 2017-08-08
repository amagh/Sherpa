package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.activities.CreateGuideActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.utilities.ColorGenerator;
import project.hikerguide.utilities.ConversionUtils;
import project.hikerguide.utilities.SaveUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.LineGraphUtils.addElevationDataToLineChart;
import static project.hikerguide.utilities.MapUtils.addMapOptionsToMap;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_EXT;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideViewModel extends BaseObservable {

    // ** Member Variables ** //
    private Context mContext;
    private Guide mGuide;
    private WeakReference<MapboxActivity> mActivity;
    private int mColorPosition;
    private boolean mShowImageError;
    private boolean mShowGpxError;
    private MapboxMap mMapboxMap;
    private boolean mTrackUserPosition;

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

        // If Guide does not have FirebaseId, return null
        if (mGuide.firebaseId == null) return null;

        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mGuide.firebaseId + JPEG_EXT);
    }

    @Bindable
    public Uri getImageUri() {
        return mGuide.getImageUri();
    }


    @BindingAdapter({"image", "imageUri"})
    public static void loadImage(ImageView imageView, StorageReference image, Uri imageUri) {

        // Check whether to load image from File or from Firebase Storage
        if (image == null) {

            // No StorageReference, load local file using the File's Uri
            Glide.with(imageView.getContext())
                    .load(imageUri)
                    .into(imageView);
        } else {

            // Load from Firebase Storage
            Glide.with(imageView.getContext())
                    .using(new FirebaseImageLoader())
                    .load(image)
                    .into(imageView);
        }
    }

    @Bindable
    public StorageReference getAuthorImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mGuide.authorId + JPEG_EXT);
    }

    @BindingAdapter("authorImage")
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

        // Check whether to use the online copy of the GPX file or a locally stored file.
        if (mGuide.firebaseId == null) {

            if (mGuide.getGpxUri() == null) {

                // No Uri set yet.
                return null;
            } else {

                // Create a new File from the Uri stored in the Guide
                return new File(mGuide.getGpxUri().getPath());
            }
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
                                // Notify change so this method is called again, but this time it
                                // will return the downloaded File
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
    public LatLng getLatLng() {
        if (mGuide.latitude == 0 && mGuide.longitude == 0) {
            return null;
        }
        return new LatLng(mGuide.latitude, mGuide.longitude);
    }

    @Bindable
    public Context getContext() {
        return mContext;
    }

    @Bindable
    public int getElevationVisibility() {
        return mGuide.elevation != 0 ? View.VISIBLE : View.GONE;
    }

    @BindingAdapter(value = {"activity", "gpx", "viewModel", "trackUserPosition"}, requireAll = false)
    public static void loadGpxToMap(final SmartMapView mapView, MapboxActivity activity, final File gpx,
                                    final GuideViewModel viewModel, final boolean trackUserPosition) {

        // TODO: Fix bug with MapboxMap not being loaded some of the time. Still can't figure out
        // why. Only occurs in CreateGuideActivity.

        // The MapView will retain it's internal LifeCycle regardless of how many times it's
        // rendered
        mapView.startMapView(activity);

        // Only get the MapboxMap if it hasn't been set yet
        if (viewModel.getMapboxMap() == null) {
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(final MapboxMap mapboxMap) {

                    // Set the memvar MapboxMap to the loaded MapboxMap
                    viewModel.setMapboxMap(mapboxMap);

                    // Set the map style
                    mapboxMap.setStyle(Style.OUTDOORS);

                    // Load the GPX data into the MapboxMap
                    if (gpx != null) {
                        viewModel.loadGpx();
                    }

                    // Track user's location if appropriate
                    if (trackUserPosition) {
                        mapboxMap.setMyLocationEnabled(true);
                    }
                }
            });
        } else {
            // Load the GPX data into the MapboxMap
            if (gpx != null) {
                viewModel.loadGpx();
            }

            // Track user's location if appropriate
            if (trackUserPosition) {
                viewModel.getMapboxMap().setMyLocationEnabled(true);
            }
        }


    }

    /**
     * Loads the GPX data into the MapboxMap
     */
    private void loadGpx() {

        File gpx = getGpx();

        if (gpx == null) {
            // GPX File has not been loaded yet. Do nothing.
            return;
        } else {
            setShowGpxError(false);
        }

        // Set the camera to the correct position
        mMapboxMap.setCameraPosition(new CameraPosition.Builder()
                .target(getLatLng())
                .zoom(11)
                .build());

        // Check to make sure the Polyline hasn't already been added the MapboxMap
        // e.g. when scrolling the RecyclerView, the View will be reloaded from memory, so
        // it does not need to re-position the camera or add the Polyline again.
        if (mMapboxMap.getPolylines().size() > 0) {
            return;
        }

        // Parse the GPX File to get the Mapbox PolyLine and Marker
        addMapOptionsToMap(gpx, mMapboxMap);
    }

    @BindingAdapter({"gpx", "context"})
    public static void loadElevationData(final LineChart lineChart, final File gpx, final Context context) {

        if (gpx == null) {
            // GPX File has not been loaded yet. Do nothing.
            return;
        }

        // Parse the GPX data to get the elevation profile and add it the LineChart
        addElevationDataToLineChart(gpx, lineChart, context);
    }

    @Bindable
    public int getColor() {
        return ColorGenerator.getColor(mContext, mColorPosition);
    }

    @BindingAdapter("color")
    public static void setTrackColor(CircleImageView imageView, int color) {
        // Set the color swatch to match the Guide's track's color
        imageView.setImageDrawable(new ColorDrawable(color));
    }

    public void onClickHeroImage(View view) {
        if (getActivity() instanceof CreateGuideActivity) {
            ((CreateGuideActivity) getActivity()).onHeroImageClick();
        }
    }

    @Bindable
    public int getIconVisibility() {
        if (mGuide.hasImage) {
            mShowImageError = false;
            notifyPropertyChanged(BR.showImageError);
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @Bindable
    public boolean getShowImageError() {
        return mShowImageError;
    }

    @BindingAdapter({"errorIconIv", "imageIconIv", "showImageError"})
    public static void showImageMissingError(TextView errorTv, ImageView errorIconIv,
                                      ImageView imageIconIv, boolean showImageError) {

        // Show/hide the error Views
        if (showImageError) {
            errorTv.setVisibility(View.VISIBLE);
            errorIconIv.setVisibility(View.VISIBLE);
            imageIconIv.setVisibility(View.GONE);
        } else {
            errorTv.setVisibility(View.GONE);
            errorIconIv.setVisibility(View.GONE);
        }
    }

    public void setShowImageError(boolean showError) {
        mShowImageError = showError;

        notifyPropertyChanged(BR.showImageError);
    }

    private void setMapboxMap(MapboxMap mapboxMap) {
        mMapboxMap = mapboxMap;
    }

    @Bindable
    public GuideViewModel getViewModel() {
        return this;
    }

    private MapboxMap getMapboxMap() {
        return mMapboxMap;
    }

    public void setShowGpxError(boolean showError) {
        mShowGpxError = showError;

        notifyPropertyChanged(BR.showGpxError);
    }

    @Bindable
    public boolean getShowGpxError() {
        return mShowGpxError;
    }

    @BindingAdapter({"gpxErrorTv", "showGpxError"})
    public static void showGpxMissingError(ImageView errorIv, TextView errorTv, boolean showGpxError) {

        // Show/hide the error Views
        if (showGpxError) {
            errorIv.setVisibility(View.VISIBLE);
            errorTv.setVisibility(View.VISIBLE);
        } else {
            errorIv.setVisibility(View.GONE);
            errorTv.setVisibility(View.GONE);
        }
    }

    @Bindable
    public boolean getTrackUserPosition() {
        return mTrackUserPosition;
    }

    public void setTrackUserPosition(boolean trackUserPosition) {
        mTrackUserPosition = trackUserPosition;

        notifyPropertyChanged(BR.trackUserPosition);
    }

    /**
     * Pans the camera to the user's position
     *
     * @param view    The button that is clicked
     */
    public void onClickTrack(View view) {

        // Get the location of the user from the MapboxMap
        Location location = mMapboxMap.getMyLocation();

        // Check whether the Location returned is valid
        if (location != null) {

            // Create a LatLng Object from the coordinates of the Location
            LatLng cameraTarget = new LatLng(location.getLatitude(), location.getLongitude());

        } else {

            // Request permission for user's location
            if (mActivity.get() instanceof GuideDetailsActivity) {
                ((GuideDetailsActivity) mActivity.get()).requestLocationPermission();
            }
        }
    }

    /**
     * Launches an Intent to open a map application with the coordinates of the trail head
     *
     * @param view    Button that is clicked
     */
    public void onClickNavigate(View view) {

        // Get the marker for the trail head
        Marker marker = mMapboxMap.getMarkers().get(0);

        // Get the position of the Marker
        LatLng position = marker.getPosition();

        // Convert the coordinates of the Marker to a geo Uri
        String geoString = mActivity.get().getString(R.string.geo_uri_string, position.getLatitude(), position.getLongitude());

        // Create a new Intent to open the Uri
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(geoString));

        // Check to ensure that the user has an application that can handle the geo Uri
        if (intent.resolveActivity(mActivity.get().getPackageManager()) != null) {

            // Open an application that can handle the Intent
            mActivity.get().startActivity(intent);
        } else {

            // Notify the user that they do not have an application that can handle the Intent
            Toast.makeText(
                    mActivity.get(),
                    mActivity.get().getString(R.string.error_no_map_app),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }
}
