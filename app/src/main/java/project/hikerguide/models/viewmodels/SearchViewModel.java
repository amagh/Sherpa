package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

import project.hikerguide.BR;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.adapters.PlaceAdapter;
import project.hikerguide.utilities.GooglePlacesApiUtils;

/**
 * Created by Alvin on 8/17/2017.
 */

public class SearchViewModel extends BaseObservable implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    // ** Constants ** //

    // ** Member Variables ** //
    private ConnectivityActivity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private PlaceAdapter mAdapter;
    private String mQuery;
    private LatLng mLatLng;
    private boolean mSearchHasFocus = false;

    public SearchViewModel(ConnectivityActivity activity) {
        mActivity = activity;

        mGoogleApiClient = GooglePlacesApiUtils.initGoogleApiClient(mActivity, this, this);
    }

    @Bindable
    public PlaceAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PlaceAdapter(new PlaceAdapter.ClickHandler() {
                @Override
                public void onClickPlace(PlaceModel placeModel) {

                }
            });
        }

        return mAdapter;
    }

    @Bindable
    public ConnectivityActivity getActivity() {
        return mActivity;
    }

    @BindingAdapter({"adapter", "activity"})
    public static void initRecyclerView(RecyclerView recyclerView, PlaceAdapter adapter, ConnectivityActivity activity) {
        if (recyclerView.getAdapter() == null) {
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        }
    }

    @Bindable
    public boolean getHasFocus() {
        return mSearchHasFocus;
    }

    @Bindable
    public LatLng getLatLng() {
        return mLatLng;
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;

        if (mQuery == null || mQuery.isEmpty() || mQuery.length() < 2) {

            // Empty the Adapter
            mAdapter.setPlaceList(null);
        } else {
            mAdapter.setShowProgress(true);

            // Query the Firebase Database
            GooglePlacesApiUtils.queryGooglePlaces(mGoogleApiClient, mQuery, new GooglePlacesApiUtils.QueryCallback() {
                @Override
                public void onQueryReady(List<PlaceModel> placeModelList) {
                    mAdapter.setPlaceList(placeModelList);

                    mAdapter.setShowProgress(false);
                }
            });
        }
    }

    public void onFocusChanged(View view, boolean hasFocus) {
        mSearchHasFocus = hasFocus;

        // Show the attribution if the EditText has focus
        if (mSearchHasFocus) {
            mAdapter.setShowAttribution(true);
        } else {
            mAdapter.setShowAttribution(false);
        }

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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
