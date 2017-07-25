package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import project.hikerguide.R;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.MapboxActivity;
import project.hikerguide.utilities.GpxUtils;
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
    private static final double METERS_TO_MILE = 1609.34;

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
        return mContext.getString(R.string.list_guide_format_distance_imperial, mGuide.distance / METERS_TO_MILE);
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

    @BindingAdapter({"bind:firebaseId", "bind:activity"})
    public static void loadGpxToMap(SmartMapView mapView, final String firebaseId, MapboxActivity activity) {
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

                try {
                    // Create a temporary File where the GPX will be downloaded
                    final File tempGpx = File.createTempFile(firebaseId, GPX_EXT);

                    // Download the GPX File
                    FirebaseStorage.getInstance().getReference()
                            .child(GPX_PATH)
                            .child(firebaseId + GPX_EXT)
                            .getFile(tempGpx)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // Parse the GPX File to get the MapboxOptions
                                    GpxUtils.getMapboxOptions(tempGpx, new MapboxOptions.MapboxListener() {
                                        @Override
                                        public void onOptionReady(MapboxOptions options) {
                                            // Set the Polyline representing the trail
                                            mapboxMap.addPolyline(options.getPolylineOptions()
                                                    .width(3));

                                            // Set the camera to the correct position
                                            mapboxMap.setCameraPosition(new CameraPosition.Builder()
                                                    .target(options.getCenter())
                                                    .zoom(13)
                                                    .build());
                                        }
                                    });
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
