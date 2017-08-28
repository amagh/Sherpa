package project.hikerguide.ui.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticMeasurement;
import org.gavaghan.geodesy.GlobalPosition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.FragmentSearchBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.SearchViewModel;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.utilities.ColorGenerator;
import project.hikerguide.utilities.DataCache;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.GpxUtils;
import project.hikerguide.utilities.SaveUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_EXT;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_PATH;

/**
 * Created by Alvin on 7/25/2017.
 */

public class SearchFragment extends MapboxFragment {

    // ** Member Variables ** //
    private FragmentSearchBinding mBinding;
    private MapboxMap mMapboxMap;
    private GuideAdapter mAdapter;
    private GeoQuery mGeoQuery;
    private List<Guide> mGuideList;
    private Map<String, PolylineOptions> mGuidePolylineMap;
    private String highlightedId;

    public SearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get a reference to the ViewDataBinding and inflate the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);
        mBinding.setVm(new SearchViewModel((MainActivity) getActivity()));

        // Initialize the Mapbox MapView
        initMapView(savedInstanceState);

        // Initialize the RecyclerView
        initRecyclerView();

        return mBinding.getRoot();
    }

    /**
     * Animates the movement of the camera for MapboxMap to a new target
     *
     * @param target    Location to move the camera to
     */
    public void moveMapCamera(LatLng target) {
        if (mMapboxMap != null) {
            // Move the camera to the correct position
            mMapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(target)
                    .zoom(10)
                    .build()), 1500);
        }
    }

    /**
     * Initializes the Mapbox MapView to respond to to user actions on the Map
     *
     * @param savedInstanceState    The SavedInstanceState Bundle to pass to the MapView
     */
    private void initMapView(Bundle savedInstanceState) {

        // Attach the MapView to the Fragment's Lifecycle
        attachMapView(mBinding.searchMv);
        mBinding.searchMv.onCreate(savedInstanceState);

        // Get a reference of the MapboxMap to manipulate camera position and add Polylines
        mBinding.searchMv.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mMapboxMap = mapboxMap;

                // Re-run GeoFire Query whenever the user moves the map
                mMapboxMap.setOnCameraIdleListener(new MapboxMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {

                        // Get the camera's position
                        LatLng position = mMapboxMap.getCameraPosition().target;

                        // Create a GeoLocation that can be used to search Firebase Database for
                        // guides in the nearby area
                        GeoLocation location = new GeoLocation(
                                position.getLatitude(),
                                position.getLongitude());

                        // Adjust the radius of the search radius to fit the map
                        if (mMapboxMap.getCameraPosition().zoom > 8) {

                            // Get the coordinates of the corner of the map
                            LatLng corner = mMapboxMap.getProjection().fromScreenLocation(new PointF(0, mMapView.getY()));

                            double searchRadius = calculateSearchRadius(location, corner);

                            // Use GeoFire to query for Guides in the area that was searched
                            queryGeoFire(location, searchRadius);

                        } else {

                            // Alter the query so that the search radius returns no results
                            queryGeoFire(location, 0);

                            // Inform user that they need to zoom in to trigger a search
                            Toast.makeText(getActivity(), getString(R.string.search_instruction_zoom_in), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * Sets up the RecyclerView and all components required to allow it to function
     */
    private void initRecyclerView() {

        // Set up the Adapter
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {

                // Launch the Activity detailing the Guide when the user clicks on it
                ((MainActivity) getActivity()).onGuideClicked(guide);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

                // Highlight the trail associated with the long-pressed Guide
                highLightPolylineForGuide(guide);
            }
        });

        // Set the GuideAdapter to use the search layout
        mAdapter.setUseSearchLayout(true);

        // Set the LayoutManager and Adapter for the RecyclerView
        mBinding.searchResultsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.searchResultsRv.setAdapter(mAdapter);
    }

    /**
     * Uses GeoFire to query the FirebaseDatabase for Guides in the vicinity of the search location
     *
     * @param location      Coordinates to search the Firebase Database for nearby Guides
     * @param searchRadius  The distance from the center to search for locations in kilometers
     */
    private void queryGeoFire(GeoLocation location, double searchRadius) {

        // Get a reference to the GeoFire path in the Firebase Database
        if (mGeoQuery == null) {

            // Use GeoFire to build a query
            GeoFire geofire = FirebaseProviderUtils.getGeoFireInstance();
            mGeoQuery = geofire.queryAtLocation(location, searchRadius);

            mAdapter.setGuides(mGuideList);

            mGeoQuery.addGeoQueryEventListener(geoQueryEventListener);
        } else {

            // Set the center to the new location. Set the new search radius
            mGeoQuery.setCenter(location);
            mGeoQuery.setRadius(searchRadius);
        }
    }

    /**
     * Calculates the distance from the center of the Mapbox MapView to the corner to be used as a
     * search radius for GeoFire Queries
     *
     * @param mapCenter    GeoLocation center of the Map
     * @param mapCorner    LatLng coordinates for the corner of the Mapbox MapView
     * @return The distance from the mapCenter to the mapCorner in kilometers
     */
    private double calculateSearchRadius(GeoLocation mapCenter, LatLng mapCorner) {

        // Calculate the distance from the center to the corner of the map
        GeodeticCalculator calculator = new GeodeticCalculator();
        GeodeticMeasurement measurement = calculator.calculateGeodeticMeasurement(Ellipsoid.WGS84,
                new GlobalPosition(mapCenter.latitude, mapCenter.longitude, 0),
                new GlobalPosition(mapCorner.getLatitude(), mapCorner.getLongitude(), 0));

        // Convert distance from meters to kilometers
        return measurement.getPointToPointDistance() / 1000;
    }

    /**
     * Retrieves a Guide data model from the Firebase Database
     *
     * @param firebaseId       The ID of the guide to be retrieved
     */
    private void getGuide(String firebaseId) {

        // Check to see if the Guide has been cached
        Guide guide = (Guide) DataCache.getInstance().get(firebaseId);

        if (guide != null) {

            // Add the cached Guide to the Adapter
            mAdapter.addGuide(guide);

            // Get the GPX File for the Guide
            getGpxForGuide(guide);

        } else {

            // Guide not in cache. Download the data from Firebase
            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.GUIDE,
                    firebaseId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            Guide guide = (Guide) model;

                            // Add the Guide to the Adapter
                            mAdapter.addGuide(guide);

                            DataCache.getInstance().store(guide);

                            // Get the GPX File for the Guide
                            getGpxForGuide(guide);
                        }
                    });
        }
    }

    /**
     * Downloads the GPX File for a Guide
     *
     * @param guide         Guide whose GPX File is to be downloaded
     */
    private void getGpxForGuide(final Guide guide) {

        // Create a file in the temp files directory that has a constant name so it can be
        // referenced later without downloading another copy while it exists in the cache
        final File tempGpxFile = SaveUtils.createTempFile(
                StorageProvider.FirebaseFileType.GPX_FILE,
                guide.firebaseId);

        // Get a reference to the GPX File in Firebase Storage
        StorageReference gpxReference = FirebaseStorage.getInstance().getReference()
                .child(GPX_PATH)
                .child(guide.firebaseId + GPX_EXT);

        // Check if the File has been previously downloaded
        if (tempGpxFile.length() == 0) {
            // Download the file
            gpxReference.getFile(tempGpxFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // Add the Polyline representing the trail to the map
                            addPolylineOptionsToMap(guide, tempGpxFile);
                        }
                    });
        } else {
            addPolylineOptionsToMap(guide, tempGpxFile);
        }
    }

    /**
     * Adds a Polyline representing a Guide's trail as calculated by its GPX File
     *
     * @param guide     Guide associated with the GPX File
     * @param gpxFile   GPX File containing coordinates representing a trail
     */
    private void addPolylineOptionsToMap(final Guide guide, File gpxFile) {

        // Generate the MapboxOptions that will contain the Polyline
        GpxUtils.getMapboxOptions(gpxFile, new GpxUtils.MapboxOptionsListener() {
            @Override
            public void onOptionReady(MarkerOptions markerOptions, PolylineOptions polylineOptions) {

                mGuidePolylineMap.put(guide.firebaseId, polylineOptions);

                // Add the Polyline to the MapboxMap
                mMapboxMap.addPolyline(polylineOptions
                        .width(2)
                        // Set the color using the the colorPosition and the ColorGenerator
                        .color(ColorGenerator.getColor(getActivity(), mAdapter.getPosition(guide.firebaseId))));
            }
        });
    }

    /**
     * Updates the Polyline so that their colors match the colors of their Guide based on the
     * position of the Guide in mAdapter
     */
    private void updatePolylineColors() {
        for (String firebaseId : mGuidePolylineMap.keySet()) {
            mGuidePolylineMap.get(firebaseId)
                    .color(ColorGenerator.getColor(getActivity(), mAdapter.getPosition(firebaseId)));
        }
    }

    /**
     * Highlights a Polyline associated with a Guide to make it more visible on the map
     *
     * @param guide    Guide that was long-pressed by the user
     */
    private void highLightPolylineForGuide(Guide guide) {

        // Highlight the track when the user long-clicks a guide to make it more visible
        if (highlightedId != null) {

            // Reset the color of the previously highlighted track
            mGuidePolylineMap.get(highlightedId)
                    .color(ColorGenerator.getColor(getActivity(), mAdapter.getPosition(highlightedId)))
                    .width(2);

            if (guide.firebaseId.equals(highlightedId)) {

                // If the highlighted track is the same as the one that was long-pressed,
                // set the variable for the highlighted track to null to de-select it
                highlightedId = null;
                return;
            }
        }

        // Set the variable for the highlighted track to the Firebaseid of the long-pressed
        // item
        highlightedId = guide.firebaseId;

        // Highlight the selected track
        PolylineOptions highlightedPolyLine = mGuidePolylineMap.get(guide.firebaseId);
        highlightedPolyLine
                .color(ContextCompat.getColor(getActivity(), R.color.yellow_a200))
                .width(4);

        // Remove and re-add the Polyline to set it to the top of the map and ensure it
        // isn't obscured by other Polylines
        mMapboxMap.removePolyline(highlightedPolyLine.getPolyline());
        mMapboxMap.addPolyline(highlightedPolyLine);
    }

    private final GeoQueryEventListener geoQueryEventListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {

            // Initialize the list to hold the Guides
            if (mGuideList == null) {
                mGuideList = new ArrayList<>();
            }

            // Init a List to hold the PolylineOptions if it is null
            if (mGuidePolylineMap == null) {
                mGuidePolylineMap = new HashMap<>();
            }

            // Hide the search instructions
            if (mBinding.searchInstructionTv.getVisibility() == View.VISIBLE) {
                mBinding.searchInstructionTv.setVisibility(View.GONE);
            }

            // Get the Guide data model that entered the search area

            getGuide(key);
        }

        @Override
        public void onKeyExited(String key) {

            // Remove the Guide and its Polyline track
            mAdapter.removeGuide(key);
            PolylineOptions options = mGuidePolylineMap.get(key);

            if (options != null ) mMapboxMap.removePolyline(options.getPolyline());
            mGuidePolylineMap.remove(key);

            // Update the colors of the lines so they match the new position of the Guides
            updatePolylineColors();

            // Show a TextView indicating no results if there are no Guides in the search area
            if (mAdapter.getItemCount() == 0) {
                mBinding.searchInstructionTv.setText(getString(R.string.search_instruction_no_guides_found));
                mBinding.searchInstructionTv.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };
}