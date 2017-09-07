package project.hikerguide.models.viewmodels;

import android.content.IntentSender;
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
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.adapters.PlaceAdapter;
import project.hikerguide.ui.adapters.interfaces.ClickHandler;
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
    private boolean mSearchHasFocus = false;
    private Handler mSearchHandler;

    public SearchViewModel(ConnectivityActivity activity) {
        mActivity = activity;

        mGoogleApiClient = GooglePlacesApiUtils.initGoogleApiClient(mActivity, this, this);
    }

    @Bindable
    public PlaceAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PlaceAdapter(new ClickHandler<PlaceModel>() {
                @Override
                public void onClick(PlaceModel placeModel) {

                    // Get a reference to SearchFragment
                    final SearchFragment fragment = (SearchFragment) mActivity.getSupportFragmentManager()
                            .findFragmentByTag(FRAG_TAG_SEARCH);

                    if (fragment != null) {

                        // Get the LatLng corresponding to the Place selected
                        GooglePlacesApiUtils.getMapboxLatLngForPlaceId(
                                mGoogleApiClient,
                                placeModel.placeId,
                                new GooglePlacesApiUtils.CoordinateListener() {

                                    @Override
                                    public void onCoordinatesReady(LatLng latLng) {

                                        // Move the camera to the LatLng for the selected Place
                                        fragment.moveMapCamera(latLng);
                                    }
                                });
                    }

                    // Remove focus from the search widget
                    setFocus(false);

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

    @BindingAdapter({"searchTv", "searchIv", "closeIv", "hasFocus"})
    public static void animateFocus(CardView cardView, EditText searchTv, ImageView searchIv, ImageView closeIv, boolean hasFocus) {

        TypedValue activatedAlphaValue      = new TypedValue();
        TypedValue deactivatedAlphaValue    = new TypedValue();
        TypedValue hiddenAlphaValue         = new TypedValue();

        cardView.getContext().getResources().getValue(R.dimen.activated_alpha, activatedAlphaValue, true);
        cardView.getContext().getResources().getValue(R.dimen.search_widget_deactivated_alpha, deactivatedAlphaValue, true);
        cardView.getContext().getResources().getValue(R.dimen.hidden_alpha, hiddenAlphaValue, true);

        float cardAlpha     = deactivatedAlphaValue.getFloat();
        float searchAlpha   = hiddenAlphaValue.getFloat();
        float closeAlpha    = hiddenAlphaValue.getFloat();

        if (hasFocus) {
            // Prevent clicking on the close ImageView when it is not visible
            closeIv.setClickable(true);
            cardAlpha   = activatedAlphaValue.getFloat();
            closeAlpha  = activatedAlphaValue.getFloat();
        } else {
            searchAlpha = activatedAlphaValue.getFloat();

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

    @Bindable
    public boolean getHasFocus() {
        return mSearchHasFocus;
    }

    private void setFocus(boolean focus) {

        // Set the member variable focus
        mSearchHasFocus = focus;

        // Show/hide the Adapter
        if (focus) {
            mAdapter.show();

            // Show the attribution if the EditText has focus
            mAdapter.setShowAttribution(true);
        } else {
            mAdapter.hide();
            mAdapter.setShowAttribution(false);
        }

        notifyPropertyChanged(BR.hasFocus);
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
                    GooglePlacesApiUtils.queryGooglePlaces(mGoogleApiClient, mQuery, new GooglePlacesApiUtils.GooglePlacesQueryListener() {
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
        setFocus(hasFocus);
    }

    public void onClickClear(View view) {

        // Set the focus to false
        setFocus(false);

        // Clear the query
        setQuery(null);

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
