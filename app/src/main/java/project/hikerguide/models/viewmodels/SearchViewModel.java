package project.hikerguide.models.viewmodels;

import android.content.IntentSender;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

import project.hikerguide.BR;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.adapters.PlaceAdapter;
import project.hikerguide.ui.fragments.SearchFragment;
import project.hikerguide.utilities.GeneralUtils;
import project.hikerguide.utilities.GooglePlacesApiUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.Constants.FragmentTags.FRAG_TAG_SEARCH;

/**
 * Created by Alvin on 8/17/2017.
 */

public class SearchViewModel extends BaseObservable implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // ** Member Variables ** //
    private ConnectivityActivity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private PlaceAdapter mAdapter;
    private String mQuery;
    private LatLng mLatLng;
    private boolean mSearchHasFocus = false;
    private Handler mSearchHandler;

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

                    // Get a reference to SearchFragment
                    final SearchFragment fragment = (SearchFragment) mActivity.getSupportFragmentManager()
                            .findFragmentByTag(FRAG_TAG_SEARCH);

                    if (fragment != null) {

                        // Get the LatLng corresponding to the Place selected
                        GooglePlacesApiUtils.getMapboxLatLngForPlaceId(
                                mGoogleApiClient,
                                placeModel.placeId,
                                new GooglePlacesApiUtils.CoordinateCallback() {

                                    @Override
                                    public void onCoordinatesReady(LatLng latLng) {

                                        // Move the camera to the LatLng for the selected Place
                                        fragment.moveMapCamera(latLng);
                                    }
                                });
                    }

                    // Remove focus from the search widget
                    mSearchHasFocus = false;
                    notifyPropertyChanged(BR.hasFocus);

                    mQuery = placeModel.primaryText;
                    notifyPropertyChanged(BR.query);

                    // Hide the soft keyboard so the results can be shown
                    GeneralUtils.hideKeyboard(mActivity, mActivity.getCurrentFocus());
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
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;

        if (mQuery == null || mQuery.isEmpty() || mQuery.length() < 2 || !mSearchHasFocus) {

            // Empty the Adapter
            mAdapter.setPlaceList(null);
        } else {

            // Show ProgressBar
            mAdapter.setShowProgress(true);

            if (mSearchHandler == null) {

                // Init the Handler for querying Google Places API
                mSearchHandler = new Handler();

            } else {

                // Cancel the previous search query
                mSearchHandler.removeCallbacksAndMessages(null);
            }

            // Query Google Places API with a delay of 500ms to allow time for the user to finish
            // typing
            mSearchHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GooglePlacesApiUtils.queryGooglePlaces(mGoogleApiClient, mQuery, new GooglePlacesApiUtils.QueryCallback() {
                        @Override
                        public void onQueryReady(List<PlaceModel> placeModelList) {
                            mAdapter.setPlaceList(placeModelList);

                            mAdapter.setShowProgress(false);
                        }
                    });
                }
            }, 500);

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

    public void onMapFocusChanged(View view, boolean hasFocus) {

        if (hasFocus) {
            Timber.d("Clearing focus");
            view.clearFocus();
        }
    }

    public void onClickClear(View view) {

        // Set the focus to false
        mSearchHasFocus = false;

        // Clear the query
        mQuery = null;

        notifyPropertyChanged(BR.query);
        notifyPropertyChanged(BR.hasFocus);

        GeneralUtils.hideKeyboard(mActivity, view);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        // Check if the failure has a resolution
        if (connectionResult.hasResolution()) {
            try {

                // Attempt to resolve the issue
                connectionResult.startResolutionForResult(mActivity, 0);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {

            // Prompt the user to fix the issue
            GoogleApiAvailability.getInstance().getErrorDialog(
                    mActivity,
                    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity),
                    0)
                    .show();
        }

    }
}
