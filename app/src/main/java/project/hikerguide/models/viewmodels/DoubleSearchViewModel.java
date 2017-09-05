package project.hikerguide.models.viewmodels;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.activities.AreaActivity;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.ui.adapters.TrailAdapter;
import project.hikerguide.ui.adapters.abstractadapters.HideableAdapter;
import project.hikerguide.ui.adapters.interfaces.ClickHandler;
import project.hikerguide.ui.adapters.NewAreaAdapter;
import project.hikerguide.ui.dialogs.AddTrailDialog;
import project.hikerguide.ui.views.SmartMapView;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.GeneralUtils;
import project.hikerguide.utilities.GooglePlacesApiUtils;
import project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType;
import timber.log.Timber;

import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.AREA_FOCUS;
import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.AREA_NO_FOCUS;
import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.TRAIL_FOCUS;
import static project.hikerguide.models.viewmodels.DoubleSearchViewModel.FocusItems.TRAIL_NO_FOCUS;

/**
 * Created by Alvin on 9/1/2017.
 */

public class DoubleSearchViewModel extends BaseObservable implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // ** Constants ** //
    private static final int SEARCH_DELAY = 1000;

    @IntDef({AREA_NO_FOCUS, TRAIL_NO_FOCUS, AREA_FOCUS, TRAIL_FOCUS})
    @interface FocusItems {
        int AREA_NO_FOCUS   = 0;
        int TRAIL_NO_FOCUS  = 1;
        int AREA_FOCUS      = 2;
        int TRAIL_FOCUS     = 3;
    }

    // ** Member Variables ** //
    private AreaActivity mActivity;
    private HideableAdapter mAdapter;

    private Area mArea;
    private Trail mTrail;

    private String mQuery;
    private MapboxMap mMapboxMap;
    private List<Trail> mTrailList;
    private Map<Trail, Marker> mTrailMap;

    private Handler mSearchHandler;
    private GoogleApiClient mGoogleApiClient;

    @FocusItems
    private int mFocus;

    public DoubleSearchViewModel(AreaActivity activity) {
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

        if (mArea == null && mAdapter instanceof NewAreaAdapter) {

            if (query.length() > 2) {

                // Show the ProgressBar in the attribution bar
                ((NewAreaAdapter) mAdapter).setExtraItemType(ExtraListItemType.ATTRIBUTION_PROGRESS);

                // Search Firebase after a delay to allow the user time to finish typing their query
                mSearchHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        queryFirebaseArea(query);
                    }
                }, SEARCH_DELAY);
            } else {
                ((NewAreaAdapter) mAdapter).clear();
                ((NewAreaAdapter) mAdapter).setExtraItemType(ExtraListItemType.ATTRIBUTION);
            }
        } else if (mArea != null && mAdapter instanceof TrailAdapter) {

            // Filter the Trail list to only show things that match the user's query
            filter(query);
        }
    }

    @Bindable
    public AreaActivity getActivity() {
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
                                        setArea(area);
                                    }
                        });
                    } else {

                        // Set the Area to the memvar and notify
                        setArea((Area) clickedItem);
                    }
                }
            });
        } else if (mArea != null && (mAdapter == null || !(mAdapter instanceof TrailAdapter))) {

            mAdapter = new TrailAdapter(this, new ClickHandler<Trail>() {
                @Override
                public void onClick(Trail clickedItem) {
                    if (clickedItem == null) {
                        AddTrailDialog dialog = new AddTrailDialog();
                        dialog.setDialogListener(new AddTrailDialog.DialogListener() {
                            @Override
                            public void onTrailNamed(Trail trail) {
                                setTrail(trail);
                            }
                        });

                        dialog.show(mActivity.getSupportFragmentManager(), null);
                    } else {
                        setTrail(clickedItem);
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
            case AREA_NO_FOCUS:
                if (mArea != null) {
                    setArea(null);
                }

            case TRAIL_NO_FOCUS:
                GeneralUtils.hideKeyboard(mActivity, mActivity.getCurrentFocus());
                mAdapter.hide();
                break;

            case AREA_FOCUS:
                if (mArea != null) {
                    setArea(null);
                }

            case TRAIL_FOCUS:
                setTrail(null);
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

        // Set new alphas based on what is focused
        switch (focus) {

            case FocusItems.AREA_FOCUS:
            case FocusItems.TRAIL_FOCUS:
            case FocusItems.TRAIL_NO_FOCUS:
                searchTv.requestFocus();

                // Prevent clicking on the close ImageView when it is not visible
                closeIv.setClickable(true);
                cardAlpha   = activatedAlphaValue.getFloat();
                closeAlpha  = activatedAlphaValue.getFloat();

                break;

            case FocusItems.AREA_NO_FOCUS:
                searchAlpha = activatedAlphaValue.getFloat();

                // Allow clicking on the close ImageView
                closeIv.setClickable(false);

                // Clear the Focus from the EditText
                searchTv.clearFocus();

                break;
        }

        if (focus == TRAIL_NO_FOCUS) {

            // Clear the focus from the search box
            searchTv.clearFocus();
        }

        // Animate changes
        new AdditiveAnimator().setDuration(150)
                .target(cardView).alpha(cardAlpha)
                .target(searchIv).alpha(searchAlpha)
                .target(closeIv).alpha(closeAlpha)
                .start();
    }

    @BindingAdapter({"searchCardView", "focus"})
    public static void animateMovement(final CardView dummyCardView, CardView searchCardView, @FocusItems int focus) {
        switch (focus) {

            case AREA_FOCUS:
            case AREA_NO_FOCUS:

                // Set the search box to the original position
                if (dummyCardView.getVisibility() != View.INVISIBLE) {
                    searchCardView.setY(dummyCardView.getY());
                }

                // Hide the dummy view
                dummyCardView.setVisibility(View.INVISIBLE);
                break;

            case TRAIL_NO_FOCUS:
                break;

            case TRAIL_FOCUS:

                // Animate the card to its new position under the dummy search box
                new AdditiveAnimator().setDuration(200)
                        .addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                                // Show the dummy search box
                                dummyCardView.setVisibility(View.VISIBLE);

                                super.onAnimationStart(animation);
                            }
                        })
                        .target(searchCardView).translationY(dummyCardView.getHeight())
                        .start();

        }
    }

    @Bindable
    public DoubleSearchViewModel getViewModel() {
        return this;
    }

    @BindingAdapter({"activity", "viewModel"})
    public static void initMap(SmartMapView mapView, AreaActivity activity, final DoubleSearchViewModel viewModel) {

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
     * Sets the Area selected by the User to be passed to the CreateGuideActivity. It also triggers
     * multiple actions to be done to switch the interface to allow the user to select a Trail
     *
     * @param area    Area that is selected
     */
    private void setArea(Area area) {

        // Check is the Area is being reset to null or if a user selected an actual Area
        if (area == null && mArea != null) {

            // Area is being reset. Set the query to match the name of the area that was previously
            // selected
            mQuery = mArea.name;
        } else {

            // Set the query to null to allow the user to filter for a Trail
            mQuery = null;
        }

        // Set the memvar to the selected Area
        mArea = area;

        if (mAdapter instanceof NewAreaAdapter) {

            // Clear the Adapter if applicable
            ((NewAreaAdapter) mAdapter).clear();
        }

        GeneralUtils.hideKeyboard(mActivity, mActivity.getCurrentFocus());

        // Notify changes
        notifyPropertyChanged(BR.adapter);
        notifyPropertyChanged(BR.area);
        notifyPropertyChanged(BR.query);

        if (mArea != null) {

            // If the user has selected an actual area, move the camera to the location
            changeMapCamera(new LatLng(mArea.latitude, mArea.longitude));

            // Set the Adapter to display Trails within the area
            getTrailsFromFirebase();

            // Set the focus correctly to animate changes in UI
            setFocus(TRAIL_FOCUS);
        }
    }

    @Bindable
    public Area getArea() {
        return mArea;
    }

    @BindingAdapter("area")
    public static void setEditText(EditText dummyText, Area area) {
        if (area != null) {

            // Set the dummy text to the name of the Area
            dummyText.setText(area.name);
        }
    }

    @Bindable
    public Trail getTrail() {
        return mTrail;
    }

    /**
     * Sets the Trail selected by the User to be used as the basis for the Guide
     *
     * @param trail    Trail that was selected
     */
    public void setTrail(Trail trail) {

        // Get a reference to the selected Trail
        mTrail = trail;

        // Check whether the user has selected a Trail or has cleared a selected Trail
        if (mTrail == null) {

            // Cleared selected trail - reset the Adapter
            if (mAdapter instanceof TrailAdapter) {
                resetAdapterList();
            }
        } else {

            // Trail selected - set the Trail name as the query in the search box
            mQuery = trail.name;

            // Move the camera to the trail location
            changeMapCamera(new LatLng(mTrail.getLatitude(), mTrail.getLongitude()));

            // Set the focus
            setFocus(TRAIL_NO_FOCUS);
        }

        // Notify changes
        notifyPropertyChanged(BR.query);
        notifyPropertyChanged(BR.trail);
    }

    @BindingAdapter("trail")
    public static void animateNextButton(FloatingActionButton fab, Trail trail) {

        // Animate the button to allow the user to progress to the next stage depending on whether
        // mTrail has been set
        float scale = 0;

        if (trail != null) {

            // Trail has been selected. Show the button
            scale = 1;
        }

        new AdditiveAnimator().setDuration(150)
                .target(fab)
                .scale(scale)
                .start();;
    }

    /**
     * Clears focus and text from the search bar
     *
     * @param view    View that was clicked
     */
    public void onClickClear(View view) {

        if (view.getId() == R.id.search_area_close_iv) {
            if (mArea == null) {
                setFocus(AREA_NO_FOCUS);
            } else {
                setFocus(TRAIL_NO_FOCUS);
            }
        } else {

            // Set the focus to false
            setFocus(AREA_NO_FOCUS);
        }

        // Clear the query
        mQuery = null;

        // Clear the Adapter
        if (mAdapter instanceof NewAreaAdapter) {
            ((NewAreaAdapter) mAdapter).clear();
        }

        // Hide the keyboard
        GeneralUtils.hideKeyboard(mActivity, view);

        notifyPropertyChanged(BR.query);
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
                    mQuery = mArea.name;
                    setFocus(AREA_FOCUS);
                    notifyPropertyChanged(BR.query);
                }
            }
        }
    }

    /**
     * Clicks respose for when user clicks the next FAB
     *
     * @param view    View that was clicked
     */
    public void onClickNextFab(View view) {
        if (mArea != null && mTrail != null) {

            // Start the CreateGuideActivity, passing in the selected Area and Trail
            mActivity.startCreateGuideActivity(mArea, mTrail);
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
    private void queryFirebaseArea(String query) {

        query = query.trim();

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
     * Pulls the list of Trails that belong to an Area and displays them in the Adapter
     */
    private void getTrailsFromFirebase() {

        FirebaseProviderUtils.queryFirebaseForTrails(mArea, new FirebaseProviderUtils.FirebaseArrayListener() {
            @Override
            public void onModelsReady(BaseModel[] models) {
                Trail[] trails = (Trail[]) models;

                // Pass the Trails to the Adapter
                mTrailList = Arrays.asList(trails);
                resetAdapterList();
            }
        });
    }

    /**
     * Filters the list of Trails for only those that match the query
     *
     * @param query    Query to filter for
     */
    private void filter(String query) {

        query = query.trim();

        // Convert to lower case
        String queryLowerCase = query.toLowerCase();

        // Create a List that will contain all the Trails that match the query
        List<Trail> filteredTrailList = new ArrayList<>();

        // Iterate through and check each Trail for those that match
        if (mTrailList != null && mTrailList.size() > 0) {
            for (Trail trail : mTrailList) {
                if (trail.name.toLowerCase().contains(queryLowerCase)) {
                    filteredTrailList.add(trail);
                }
            }
        }

        // Remove Markers from the Map
        if (mTrailMap != null) {
            for (Trail trail : mTrailMap.keySet()) {
                if (!filteredTrailList.contains(trail)) {
                    mMapboxMap.removeMarker(mTrailMap.get(trail));
                    mTrailMap.remove(trail);
                }
            }
        } else {
            mTrailMap = new HashMap<>();
        }

        // Add Markers to the Map to represent the approximate location of each Trail
        for (Trail trail : filteredTrailList) {
            if (!mTrailMap.keySet().contains(trail)) {
                addMarkerForTrail(trail);
            }
        }

        // Replace the contents of the Adapter with the filtered List
        ((TrailAdapter) mAdapter).setAdapterItems(filteredTrailList);
    }

    /**
     * Replaces the contents of the Adapter with the master list of all trails in the Area
     */
    private void resetAdapterList() {
        ((TrailAdapter) mAdapter).setAdapterItems(mTrailList);

        // Init the Map to hold the Markers
        if (mTrailMap == null) {
            mTrailMap = new HashMap<>();
        }

        // Add the Marker for each Trail to the Map
        for (Trail trail : mTrailList) {
            if (!mTrailMap.containsKey(trail)) {
                addMarkerForTrail(trail);
            }
        }
    }

    /**
     * Adds a Marker to the MapboxMap at the location of a Trail
     *
     * @param trail    Trail to be given a Marker on the MapboxMap
     */
    private void addMarkerForTrail(Trail trail) {
        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(trail.getLatitude(), trail.getLongitude()));

        mMapboxMap.addMarker(options);

        List<Marker> markerList = mMapboxMap.getMarkers();
        mTrailMap.put(trail, markerList.get(markerList.size() - 1));
    }

    /**
     * Setter for MapboxMap for this ViewModel. This allows the variable to be set when the
     * ViewModel is passed as a parameter in
     * {@link #initMap(SmartMapView, AreaActivity, DoubleSearchViewModel)}
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
        if (mArea != null) {
            setFocus(TRAIL_NO_FOCUS);
        } else {
            setFocus(AREA_NO_FOCUS);
        }

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
