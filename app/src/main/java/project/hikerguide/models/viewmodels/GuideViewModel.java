package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.lang.ref.WeakReference;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import de.hdodenhof.circleimageview.CircleImageView;
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.ui.fragments.FavoritesFragment;
import project.hikerguide.ui.fragments.MapboxFragment;
import project.hikerguide.ui.views.SmartMapView;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.activities.CreateGuideActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.utilities.ColorGenerator;
import project.hikerguide.utilities.ContentProviderUtils;
import project.hikerguide.utilities.FormattingUtils;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.SaveUtils;

import static project.hikerguide.utilities.Constants.FragmentTags.FRAG_TAG_FAVORITE;
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
    private Author mAuthor;

    private WeakReference<MapboxActivity> mActivity;
    private WeakReference<MapboxFragment> mFragment;

    private int mColorPosition;
    private boolean mShowImageError;
    private boolean mShowGpxError;
    private MapboxMap mMapboxMap;
    private boolean mTrackUserPosition;

    private Bundle mSavedInstanceState;

    public GuideViewModel(Context context, Guide guide) {
        mContext = context;

        // If the passed Context is a MapboxActivity, then set the mem var to reference it
        if (mContext instanceof MapboxActivity) {
            mActivity = new WeakReference<>((MapboxActivity) mContext);
        }

        mGuide = guide;
    }

    public GuideViewModel(Context context, Guide guide, int colorPosition) {
        mContext = context;

        // If the passed Context is a MapboxActivity, then set the mem var to reference it
        if (mContext instanceof MapboxActivity) {
            mActivity = new WeakReference<>((MapboxActivity) mContext);
        }

        mGuide = guide;
        mColorPosition = colorPosition;
    }

    public GuideViewModel(Context context, MapboxFragment fragment, Guide guide) {
        mContext = context;

        if (mContext instanceof MapboxActivity) {
            mActivity = new WeakReference<>((MapboxActivity) mContext);
        }

        mFragment = new WeakReference<>(fragment);
        mGuide = guide;
    }

    public void setAuthor(Author author) {

        // Set memvar to reference passed Author
        mAuthor = author;

        if (mAuthor != null && mAuthor.favorites != null && mAuthor.favorites.containsKey(mGuide.firebaseId)) {

            // Set the favorite status for the Guide if it within the Author's list of favorites
            mGuide.setFavorite(true);

            // Notify change
            notifyPropertyChanged(BR.favorite);
        } else if (mAuthor == null){

            // Query the database to see if the Guide is a favorite
            mGuide.setFavorite(ContentProviderUtils.isGuideFavorite(mContext, mGuide));

            // Notify change
            notifyPropertyChanged(BR.favorite);
        }
    }

    public void addSavedInstanceState(Bundle savedInstanceState) {
        mSavedInstanceState = savedInstanceState;
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
                FormattingUtils.convertDistance(mContext, mGuide.distance));
    }

    @Bindable
    public String getElevation() {
        return mContext.getString(
                R.string.list_guide_format_elevation_imperial,
                FormattingUtils.convertElevation(mContext, mGuide.elevation));
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
    public int getDifficultyRating() {
        return mGuide.difficulty;
    }

    @Bindable
    public String getDifficulty() {
        return FormattingUtils.formatDifficulty(mContext, mGuide.difficulty);
    }

    @Bindable
    public Uri getImage() {

        // Check whether the Guide has a Uri for an offline image File
        if (mGuide.firebaseId == null || mGuide.getImageUri() != null) {

            // Return the ImageUri
            return mGuide.getImageUri();
        } else {

            // Parse the StorageReference to a Uri
            return Uri.parse(FirebaseStorage.getInstance().getReference()
                    .child(IMAGE_PATH)
                    .child(mGuide.firebaseId + JPEG_EXT).toString());
        }
    }

    @BindingAdapter("image")
    public static void loadImage(ImageView imageView, Uri image) {

        if (image == null) return;

        // Check whether to load image from File or from Firebase Storage
        if (image.getScheme().matches("gs")) {
            // Load from Firebase Storage
            Glide.with(imageView.getContext())
                    .using(new FirebaseImageLoader())
                    .load(FirebaseProviderUtils.getReferenceFromUri(image))
                    .thumbnail(0.1f)
                    .into(imageView);
        } else {
            // No StorageReference, load local file using the File's Uri
            Glide.with(imageView.getContext())
                    .load(image)
                    .into(imageView);
        }
    }

    @Bindable
    public Uri getAuthorImage() {

        // Parse the StorageReference to a Uri
        return Uri.parse(FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mGuide.authorId + JPEG_EXT).toString());
    }

    @Bindable
    public String getAuthor() {
        return mGuide.authorName;
    }

    @Bindable
    public File getGpx() {

        // Check whether to use the online copy of the GPX file or a locally stored file.
        if (mGuide.getGpxUri() != null) {

            // Create a new File from the Uri stored in the Guide
            return new File(mGuide.getGpxUri().getPath());

        } else if (mGuide.firebaseId != null){

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
        } else {
            return null;
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

    @Bindable
    public Bundle getSavedInstanceState() {
        return mSavedInstanceState;
    }

    @Bindable
    public MapboxFragment getFragment() {
        return mFragment.get();
    }

    @BindingAdapter(
            value = {"activity", "gpx", "viewModel", "trackUserPosition", "savedInstanceState", "fragment"},
            requireAll = false)
    public static void loadGpxToMap(final SmartMapView mapView, MapboxActivity activity, final File gpx,
                                    final GuideViewModel viewModel, final boolean trackUserPosition,
                                    final Bundle savedInstanceState, MapboxFragment fragment) {

        // The MapView will retain it's internal LifeCycle regardless of how many times it's
        // rendered
        if (fragment != null) {
            mapView.startMapView(fragment, savedInstanceState);
        } else if (activity != null) {
            mapView.startMapView(activity, savedInstanceState);
        }

        if (viewModel == null) return;

        // Only get the MapboxMap if it hasn't been set yet
        if (viewModel.getMapboxMap() == null) {
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(final MapboxMap mapboxMap) {

                    // Set the memvar MapboxMap to the loaded MapboxMap
                    viewModel.setMapboxMap(mapboxMap);

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

    @Bindable
    public boolean getFavorite() {
        return mGuide.isFavorite();
    }

    @BindingAdapter("favorite")
    public static void setFavoriteButton(ImageView imageView, boolean favorite) {

        if (favorite) {
            AdditiveAnimator.animate(imageView)
                    .scale(1f)
                    .setDuration(100)
                    .start();
        } else {
            AdditiveAnimator.animate(imageView)
                    .scale(0f)
                    .setDuration(100)
                    .start();
        }
    }

    @BindingAdapter({"circle2", "circle3", "circle4", "circle5", "difficultyRating"})
    public static void setDifficultyCircles(ImageView circle1, ImageView circle2, ImageView circle3,
                                            ImageView circle4, ImageView circle5, int difficulty) {

        // Reset all images to their default icon
        circle1.setImageResource(R.drawable.ic_circle_stroke);
        circle2.setImageResource(R.drawable.ic_circle_stroke);
        circle3.setImageResource(R.drawable.ic_circle_stroke);
        circle4.setImageResource(R.drawable.ic_circle_stroke);
        circle5.setImageResource(R.drawable.ic_circle_stroke);

        // Change icons based on difficulty
        switch (difficulty) {
            case 5:
                circle5.setImageResource(R.drawable.ic_circle);

            case 4:
                circle4.setImageResource(R.drawable.ic_circle);

            case 3:
                circle3.setImageResource(R.drawable.ic_circle);

            case 2:
                circle2.setImageResource(R.drawable.ic_circle);

            case 1:
                circle1.setImageResource(R.drawable.ic_circle);
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

        // Check to make sure the Polyline hasn't already been added the MapboxMap
        // e.g. when scrolling the RecyclerView, the View will be reloaded from memory, so
        // it does not need to re-position the camera or add the Polyline again.
        if (mMapboxMap.getPolylines().size() > 0) {
            return;
        }

        boolean moveCamera = mSavedInstanceState == null;

        // Parse the GPX File to get the Mapbox PolyLine and Marker
        addMapOptionsToMap(gpx, mMapboxMap, moveCamera);
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
            mMapboxMap.animateCamera(CameraUpdateFactory.newLatLng(cameraTarget));
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

    /**
     * Click response for the favorite star button
     *
     * @param view    View that was clicked
     */
    public void onClickFavorite(View view) {

        // Add/remove the Guide from the Author's list of favorites
        if (mAuthor != null) {

            // Firebase Database
            FirebaseProviderUtils.toggleFirebaseFavorite(mAuthor, mGuide);

        } else {

            // Local Database
            ContentProviderUtils.toggleFavorite(mContext, mGuide);
        }

        // If the user is on the FavoriteFragment and removing a favorite, then it needs to be
        // removed from the Adapter when they click the favorite button
        if (mContext instanceof AppCompatActivity) {

            // Get a reference to the FavoriteFragment using the Fragment tag
            FavoritesFragment fragment = (FavoritesFragment) ((AppCompatActivity) mContext)
                    .getSupportFragmentManager()
                    .findFragmentByTag(FRAG_TAG_FAVORITE);

            if (!mGuide.isFavorite() && fragment != null) {

                // Remove the Guide from the Adapter
                fragment.removeGuideFromAdapter(mGuide);
            }
        }

        // Notify change
        notifyPropertyChanged(BR.favorite);
    }

    /**
     * Click response for clicking on a difficulty circle
     *
     * @param view    View that was clicked
     */
    public void onClickDifficultyCircle(View view) {

        // Only set difficulty if the user is creating a Guide
        if (mActivity.get() instanceof CreateGuideActivity) {

            // Change the difficulty of the Guide
            switch (view.getId()) {
                case R.id.list_guide_difficulty_circle_1:
                    mGuide.difficulty = 1;
                    break;

                case R.id.list_guide_difficulty_circle_2:
                    mGuide.difficulty = 2;
                    break;

                case R.id.list_guide_difficulty_circle_3:
                    mGuide.difficulty = 3;
                    break;

                case R.id.list_guide_difficulty_circle_4:
                    mGuide.difficulty = 4;
                    break;

                case R.id.list_guide_difficulty_circle_5:
                    mGuide.difficulty = 5;
                    break;
            }

            notifyPropertyChanged(BR.difficulty);
            notifyPropertyChanged(BR.difficultyRating);
        }
    }
}
