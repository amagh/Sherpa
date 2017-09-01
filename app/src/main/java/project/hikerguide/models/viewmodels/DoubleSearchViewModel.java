package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.Arrays;
import java.util.List;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.ui.adapters.abstractadapters.HideableAdapter;
import project.hikerguide.ui.adapters.interfaces.ClickHandler;
import project.hikerguide.ui.adapters.NewAreaAdapter;
import project.hikerguide.ui.views.SmartMapView;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.GeneralUtils;
import project.hikerguide.utilities.GooglePlacesApiUtils;
import project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType;

import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.AREA_FOCUS;
import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.NO_FOCUS;
import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.TRAIL_FOCUS;

/**
 * Created by Alvin on 9/1/2017.
 */

public class DoubleSearchViewModel extends BaseObservable implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // ** Constants ** //
    private static final int SEARCH_DELAY = 1000;

    @IntDef({NO_FOCUS, AREA_FOCUS, TRAIL_FOCUS})
    @interface FocusItems {
        int NO_FOCUS    = 0;
        int AREA_FOCUS  = 1;
        int TRAIL_FOCUS = 2;
    }

    // ** Member Variables ** //
    private MapboxActivity mActivity;
    private HideableAdapter mAdapter;

    private Area mArea;
    private Trail mTrail;

    private String mQuery;
    private MapboxMap mMapboxMap;

    private Handler mSearchHandler;
    private GoogleApiClient mGoogleApiClient;

    @FocusItems
    private int mFocus;

    public DoubleSearchViewModel(MapboxActivity activity) {
        mActivity = activity;

        // Initialize the GoogleApiClient
        mGoogleApiClient = GooglePlacesApiUtils.initGoogleApiClient(mActivity, this, this);
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(final String query) {

        mQuery = query;

        if (mSearchHandler != null) {
            // Cancel any ongoing searches
            mSearchHandler.removeCallbacksAndMessages(null);
        } else {
            // Initialize the Handler
            mSearchHandler = new Handler();
        }

        if (query.length() > 2) {
            if (mArea == null && mAdapter instanceof NewAreaAdapter) {

                // Show the ProgressBar in the attribution bar
                ((NewAreaAdapter) mAdapter).setExtraItemType(ExtraListItemType.ATTRIBUTION_PROGRESS);

                // Search Firebase after a delay to allow the user time to finish typing their query
                mSearchHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        queryFirebase(query);
                    }
                }, SEARCH_DELAY);
            }
        }
    }

    @Bindable
    public MapboxActivity getActivity() {
        return mActivity;
    }

    @Bindable
    public HideableAdapter getAdapter() {

        // Check which type of Adapter needs to be returned
        if (mArea == null && (mAdapter == null || !(mAdapter instanceof NewAreaAdapter))) {

            // If the Area has not yet been set, then return area AreaAdapter
            mAdapter = new NewAreaAdapter(this, new ClickHandler<Object>() {
                @Override
                public void onClick(Object clickedItem) {

                    // Click response depends on the clickedItem
                    if (clickedItem == null) {

                        // Show the Google attribution and progress bar
                        if (mAdapter instanceof NewAreaAdapter) {
                            ((NewAreaAdapter) mAdapter)
                                    .setExtraItemType(ExtraListItemType.ATTRIBUTION_GOOGLE_PROGRESS);
                        }

                        // Query Google Places for additional results
                        queryGooglePlaces(mQuery);

                    } else if (clickedItem instanceof PlaceModel) {

                        // Convert the clicked PlaceModel to an Area
                        GooglePlacesApiUtils.convertPlaceModelToArea(mGoogleApiClient,
                                (PlaceModel) clickedItem,
                                new GooglePlacesApiUtils.ConversionListener() {
                                    @Override
                                    public void onPlaceConvertedToArea(Area area) {

                                        // Set the Area to the memvar and notify
                                        mArea = area;
                                        changeMapCamera(new LatLng(mArea.latitude, mArea.longitude));
                                        notifyPropertyChanged(BR.adapter);
                                    }
                        });
                    } else {

                        // Set the Area to the memvar and notify
                        mArea = (Area) clickedItem;
                        changeMapCamera(new LatLng(mArea.latitude, mArea.longitude));
                        notifyPropertyChanged(BR.adapter);
                    }
                }
            });
        }

        return mAdapter;
    }

    @BindingAdapter({"activity", "adapter"})
    public static void initRecyclerView(RecyclerView recyclerView, MapboxActivity activity, HideableAdapter adapter) {

        // Set the Adapter and LayoutManager for the RecyclerView
        recyclerView.setAdapter(adapter);

        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        }
    }

    @Bindable
    @FocusItems
    public int getFocus() {
        return mFocus;
    }

    public void setFocus(@FocusItems int focus) {
        mFocus = focus;

        switch (mFocus) {
            case NO_FOCUS:
                mAdapter.hide();
                break;

            case AREA_FOCUS:
                mArea = null;

            case TRAIL_FOCUS:
                mAdapter.show();
        }

        notifyPropertyChanged(BR.focus);
    }

    @BindingAdapter({"searchTv", "searchIv", "closeIv", "focus"})
    public static void animateFocus(CardView cardView, EditText searchTv, ImageView searchIv,
                                    ImageView closeIv, @FocusItems int focus) {

        // Get default values from resources
        TypedValue activatedAlphaValue      = new TypedValue();
        TypedValue deactivatedAlphaValue    = new TypedValue();
        TypedValue hiddenAlphaValue         = new TypedValue();

        cardView.getContext().getResources().getValue(R.dimen.activated_alpha, activatedAlphaValue, true);
        cardView.getContext().getResources().getValue(R.dimen.search_widget_deactivated_alpha, deactivatedAlphaValue, true);
        cardView.getContext().getResources().getValue(R.dimen.hidden_alpha, hiddenAlphaValue, true);

        // Set the initial alphas for the Views
        float cardAlpha     = deactivatedAlphaValue.getFloat();
        float searchAlpha   = hiddenAlphaValue.getFloat();
        float closeAlpha    = hiddenAlphaValue.getFloat();

        if (focus != NO_FOCUS) {

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
    public DoubleSearchViewModel getViewModel() {
        return this;
    }

    @BindingAdapter({"activity", "viewModel"})
    public static void initMap(SmartMapView mapView, MapboxActivity activity, final DoubleSearchViewModel viewModel) {

        // Start the Map's LifeCycle and attach it to the Activity LifeCycle
        mapView.startMapView(activity, null);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                // Set the memvar mMapboxMap
                viewModel.setMapboxMap(mapboxMap);
            }
        });
    }

    /**
     * Clears focus and text from the search bar
     *
     * @param view    View that was clicked
     */
    public void onClickClear(View view) {

        // Set the focus to false
        setFocus(NO_FOCUS);

        // Clear the query
        mQuery = null;

        // Clear the Adapter
        ((NewAreaAdapter) mAdapter).clear();

        // Hide the keyboard
        GeneralUtils.hideKeyboard(mActivity, view);

        notifyPropertyChanged(BR.query);
        notifyPropertyChanged(BR.hasFocus);
    }

    /**
     * Listener for when the focus of a View changes.
     *
     * @param view        View whose focus has changed
     * @param hasFocus    Boolean value for whether the View has focus
     */
    public void onFocusChanged(View view, boolean hasFocus) {
        if (hasFocus) {

            // Set the focus depending on which View has focus
            switch (view.getId()) {

                case R.id.search_area_tv:
                    if (mArea == null) {
                        setFocus(AREA_FOCUS);
                    } else {
                        setFocus(TRAIL_FOCUS);
                    }
                    break;

                case R.id.search_area_dummy_tv: {
                    setFocus(AREA_FOCUS);
                }
            }
        }
    }

    /**
     * Queries Google Places API for results that match the user's query
     *
     * @param query    Query to send to Google Places API
     */
    private void queryGooglePlaces(String query) {

        // Case the Adapter to NewAreaAdapter for convenience
        final NewAreaAdapter adapter = (NewAreaAdapter) mAdapter;

        // Query Google Place API
        GooglePlacesApiUtils.queryGooglePlaces(mGoogleApiClient, query, new GooglePlacesApiUtils.GooglePlacesQueryListener() {
            @Override
            public void onQueryReady(List<PlaceModel> placeModelList) {

                // Hide the ProgressBar from the attribution
                adapter.setExtraItemType(ExtraListItemType.ATTRIBUTION_GOOGLE);

                // Set the returned items to the Adapter
                if (placeModelList.size() > 0) {
                    adapter.setAdapterItems(placeModelList);
                } else {
                    adapter.clear();
                }
            }
        });
    }

    /**
     * Queries the Firebase Database for Areas that match the user's query
     *
     * @param query    Query to send to the Firebase Database
     */
    private void queryFirebase(String query) {

        // Cast the Adapter to NewAreaAdapter for convenience
        final NewAreaAdapter adapter = (NewAreaAdapter) mAdapter;

        // Query Firebase with the user's query
        FirebaseProviderUtils.queryFirebaseForAreas(query, new FirebaseProviderUtils.FirebaseArrayListener() {
            @Override
            public void onModelsReady(BaseModel[] models) {

                // Hide the attribution bar
                adapter.setExtraItemType(NewAreaAdapter.ExtraListItemType.HIDDEN);

                // Time to delay the search more list item from showing
                int delaySearchMore = SEARCH_DELAY;

                if (models.length > 0) {

                    // Convert the Array to a List and set it to the Adapter
                    Area[] areas = (Area[]) models;
                    List<Area> areaList = Arrays.asList(areas);

                    adapter.setAdapterItems(areaList);
                } else {

                    // No results. Clear the Adapter
                    adapter.clear();

                    // Remove the delay and immediately show the option to search Google
                    delaySearchMore = 0;
                }

                // Show the list item to allow the user to search for additional items after a delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // Only show the option if the user hasn't already selected an Area
                        if (mArea == null) {
                            adapter.setExtraItemType(NewAreaAdapter.ExtraListItemType.SEARCH_MORE);
                        }
                    }
                }, delaySearchMore);
            }
        });
    }

    /**
     * Setter for MapboxMap for this ViewModel. This allows the variable to be set when the
     * ViewModel is passed as a parameter in
     * {@link #initMap(SmartMapView, MapboxActivity, DoubleSearchViewModel)}
     *
     * @param mapboxMap    The MapboxMap that will be set as the memver variable
     */
    private void setMapboxMap(MapboxMap mapboxMap) {
        mMapboxMap = mapboxMap;
    }

    GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    /**
     * Animates the camera movement when a user selects a location
     *
     * @param cameraCoordinates    The coordinates to move the camera to
     */
    void changeMapCamera(LatLng cameraCoordinates) {

        // Do not move the Camera if there are no valid coordinates
        if (cameraCoordinates == null || (cameraCoordinates.getLatitude() == 0 && cameraCoordinates.getLongitude() == 0)) {
            return;
        }

        // Hide the Adapter
        setFocus(NO_FOCUS);

        // Animate the camera movement
        mMapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(cameraCoordinates)
                .zoom(8)
                .build()), 1500);
    }

    //********************************************************************************************//
    //********************************* Google API Callbacks *************************************//
    //********************************************************************************************//

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
