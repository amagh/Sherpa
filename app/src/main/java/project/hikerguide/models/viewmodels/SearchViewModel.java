package project.hikerguide.models.viewmodels;

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
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.ui.adapters.AreaAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 8/2/2017.
 */

public class SearchViewModel extends BaseObservable implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    // ** Constants ** //
    private static final int SEARCH_DELAY = 1500;

    // ** Member Variables ** //
    private AreaAdapter mAdapter;
    private MapboxActivity mActivity;
    private String mQuery;
    private boolean mSearchHasFocus = false;
    private GoogleApiClient mGoogleApiClient;

    public SearchViewModel(MapboxActivity activity) {
        mActivity = activity;

        initGoogleApiClient();
    }

    @Bindable
    public AreaAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new AreaAdapter(new AreaAdapter.ClickHandler() {
                @Override
                public void onClickArea(Object object) {
                    if (object == null) {
                        queryGooglePlaces(mQuery);
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
        Timber.d("Focus: " + hasFocus);
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

    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }

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

                    mAdapter.setAreaList(placeList);
                }
            }
        });
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
