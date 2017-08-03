package project.hikerguide.models.viewmodels;

import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.ui.activities.TrailActivity;
import project.hikerguide.ui.adapters.AreaAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.ui.activities.TrailActivity.IntentKeys.AREA;

/**
 * Created by Alvin on 8/2/2017.
 */

public class SearchAreaViewModel extends BaseObservable implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    // ** Constants ** //
    private static final int SEARCH_DELAY = 1500;

    // ** Member Variables ** //
    private AreaAdapter mAdapter;
    private MapboxActivity mActivity;
    private String mQuery;
    private com.mapbox.mapboxsdk.geometry.LatLng mLatLng;
    private boolean mSearchHasFocus = false;
    private GoogleApiClient mGoogleApiClient;

    public SearchAreaViewModel(MapboxActivity activity) {
        mActivity = activity;

        initGoogleApiClient();
    }

    @Bindable
    public AreaAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new AreaAdapter(this, new AreaAdapter.ClickHandler() {
                @Override
                public void onClickArea(Object object) {
                    if (object == null) {
                        queryGooglePlaces(mQuery);
                    } else {
                        // Start TrailActivity
                        if (object instanceof Area) {
                            Intent intent = new Intent(mActivity, TrailActivity.class);
                            intent.putExtra(AREA, (Area) object);
                            mActivity.startActivity(intent);
                        } else if (object instanceof PlaceModel) {
                            startTrailActivityWithPlaceModel((PlaceModel) object);
                        }
                    }
                }
            });
        }

        return mAdapter;
    }

    @Bindable
    public MapboxActivity getActivity() {
        return mActivity;
    }

    @BindingAdapter({"bind:adapter", "bind:activity"})
    public static void setAdapter(RecyclerView recyclerView, AreaAdapter adapter, MapboxActivity activity) {

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
    }

    @Bindable
    public boolean getHasFocus() {
        return mSearchHasFocus;
    }

    @Bindable
    public com.mapbox.mapboxsdk.geometry.LatLng getLatLng() {
        return mLatLng;
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;

        if (!mQuery.isEmpty() && mQuery.length() > 2) {
            // Query the Firebase Database
            queryFirebaseDatabase(mQuery);
        } else {
            // Empty the Adapter
            mAdapter.setAreaList(null);
        }
    }

    public void onFocusChanged(View view, boolean hasFocus) {
        mSearchHasFocus = hasFocus;

        notifyPropertyChanged(BR.hasFocus);
    }


    public void onClickClear(View view) {

        // Set the focus to false
        mSearchHasFocus = false;

        // Clear the query
        mQuery = null;

        notifyPropertyChanged(BR.query);
        notifyPropertyChanged(BR.hasFocus);
    }

    @BindingAdapter({"app:searchTv", "app:searchIv", "app:closeIv", "bind:hasFocus"})
    public static void animateFocus(CardView cardView, EditText searchTv, ImageView searchIv, ImageView closeIv, boolean hasFocus) {

        float cardAlpha     = 0.75f;
        float searchAlpha   = 0;
        float closeAlpha    = 0;

        if (hasFocus) {
            // Prevent clicking on the close ImageView when it is not visible
            closeIv.setClickable(true);
            cardAlpha = 1;
            closeAlpha = 1;
        } else {
            searchAlpha = 1;

            // Allow clicking on the close ImageView
            closeIv.setClickable(false);

            // Clear the Focus from the EditText
            searchTv.clearFocus();
        }

        // Animate changes
        new AdditiveAnimator().setDuration(150)
                .target(cardView).alpha(cardAlpha)
                .target(searchIv).alpha(searchAlpha)
                .target(closeIv).alpha(closeAlpha)
                .start();
    }

    @BindingAdapter("bind:activity")
    public static void initMap(SmartMapView mapView, MapboxActivity activity) {

        // Start the Map's LifeCycle and attach it to the Activity LifeCycle
        mapView.startMapView(activity);
    }

    @BindingAdapter("bind:latLng")
    public static void moveCamera(SmartMapView mapView, final com.mapbox.mapboxsdk.geometry.LatLng latLng) {

        // Do not move the Camera if there are no valid coordinates
        if (latLng == null || (latLng.getLatitude() == 0 && latLng.getLongitude() == 0)) {
            return;
        }

        // Animate the camera movement to the new location
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(8)
                        .build()), 1500);
            }
        });
    }

    /**
     * Queries the Firebase Database for a matching Area
     *
     * @param query    The query for the Firebase Database
     */
    private void queryFirebaseDatabase(String query) {

        // Build a Query for the Firebase Database
        final Query firebaseQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AREAS)
                .orderByChild("lowerCaseName")
                .startAt(query.toLowerCase())
                .endAt(query + "z");

        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int delay = 0;

                // Check that the DataSnapshot is valid
                if (dataSnapshot.exists()) {

                    // Convert to a List of Areas and then pass it to the Adapter
                    Object[] areas = (Object[]) FirebaseProviderUtils.getModelsFromSnapshot(DatabaseProvider.FirebaseType.AREA, dataSnapshot);
                    mAdapter.setAreaList(Arrays.asList(areas));

                    delay = SEARCH_DELAY;
                }

                // Show option to search for more after 1.5s
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.showSearchMore();
                    }
                }, delay);

                // Remove Listener
                firebaseQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                firebaseQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Initializes the GoogleApiClient to use the Places API
     */
    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

    /**
     * Query the Google Places API for a Place that matches
     *
     * @param query    Query for the Google Places API
     */
    private void queryGooglePlaces(String query) {

        // Max LatLngBounds possible
        LatLngBounds bounds = new LatLngBounds(
                new LatLng(-85, 180),   // Max NE bound
                new LatLng(85, -180));  // Max SW bound

        // Query Places API
        PendingResult<AutocompletePredictionBuffer> result = Places.GeoDataApi
                .getAutocompletePredictions(mGoogleApiClient, query, bounds, null);

        result.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
            @Override
            public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                if (autocompletePredictions.getStatus().isSuccess() && autocompletePredictions.getCount() > 0) {
                    List<Object> placeList = new ArrayList<>();

                    for (AutocompletePrediction prediction : autocompletePredictions) {

                        // Create a new PlaceModel from the List and populate it with info
                        PlaceModel placeModel = new PlaceModel();
                        placeModel.primaryText = prediction.getPrimaryText(null).toString();
                        placeModel.secondaryText = prediction.getSecondaryText(null).toString();
                        placeModel.placeId = prediction.getPlaceId();

                        placeList.add(placeModel);
                    }

                    autocompletePredictions.release();

                    mAdapter.setAreaList(placeList);
                }
            }
        });
    }

    /**
     * Moves the camera to the coordinates provided by Google Places API for a given PlaceId
     *
     * @param placeId    PlaceId of the PlaceModel to move the camera to
     */
    void changeMapCamera(String placeId) {

        // Query Places API to get the Place
        Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {

                // Check to ensure results are valid
                if (places.getStatus().isSuccess() && places.getCount() > 0) {

                    // Get the Place, generate the LatLng and move the camera
                    Place place = places.get(0);
                    LatLng location = place.getLatLng();
                    changeMapCamera(new com.mapbox.mapboxsdk.geometry.LatLng(location.latitude, location.longitude));

                    places.release();
                }
            }
        });
    }

    /**
     * Starts the TrailActivity after converting the PlaceModel to an Area so it can be passed in
     * the Intent
     *
     * @param placeModel    PlaceModel to be converted to Area to be passed via Intent
     */
    private void startTrailActivityWithPlaceModel(PlaceModel placeModel) {

        // Query the Places API to get the details required for the Area data model
        Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeModel.placeId)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places) {

                        // Check that the result is valid
                        if (places.getStatus().isSuccess() && places.getCount() > 0) {

                            // Init a new Area
                            Area area = new Area();

                            // Get the Place
                            Place place = places.get(0);

                            // Add the details to the new Area and pass it to the Intent
                            LatLng latLng = place.getLatLng();
                            area.name = place.getName().toString();
                            area.state = place.getAddress().toString();

                            Timber.d("State: " + area.state);
                            area.latitude = latLng.latitude;
                            area.longitude = latLng.longitude;

                            places.release();

                            Intent intent = new Intent(mActivity, TrailActivity.class);
                            intent.putExtra(AREA, area);
                            mActivity.startActivity(intent);
                        }
                    }
                });
    }

    /**
     * Causes the camera in the MapView to move to a given coordinate
     *
     * @param latLng    The coordinates to move the camera to
     */
    void changeMapCamera(com.mapbox.mapboxsdk.geometry.LatLng latLng) {
        mLatLng = latLng;
        notifyPropertyChanged(BR.latLng);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.d("Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
