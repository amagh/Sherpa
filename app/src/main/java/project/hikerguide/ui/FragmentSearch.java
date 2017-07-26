package project.hikerguide.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
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
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.FragmentSearchBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.utilities.ColorGenerator;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.GpxUtils;
import project.hikerguide.utilities.SaveUtils;

import static android.app.Activity.RESULT_OK;
import static project.hikerguide.firebasedatabase.DatabaseProvider.GEOFIRE_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.GPX_EXT;
import static project.hikerguide.utilities.StorageProviderUtils.GPX_PATH;

/**
 * Created by Alvin on 7/25/2017.
 */

public class FragmentSearch extends MapboxFragment {
    // ** Constants ** //
    private static final int PLACES_REQUEST_CODE = 6498;

    // ** Member Variables ** //
    private FragmentSearchBinding mBinding;
    private MapboxMap mMapboxMap;
    private GuideAdapter mAdapter;
    private int mColorPosition;

    public FragmentSearch() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get a reference to the ViewDataBinding and inflate the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);

        // Launch the AutoCompleteSearchWidget
        launchPlacesSearch();

        // Attach the MapView to the Fragment's Lifecycle
        attachMapView(mBinding.searchMv);
        mBinding.searchMv.onCreate(savedInstanceState);

        // Get a reference of the MapboxMap
        mBinding.searchMv.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mMapboxMap = mapboxMap;
            }
        });

        // Set an OnClickListener to launch the PlaceAutocompleteSearchWidget when clicked
        mBinding.searchSv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPlacesSearch();
            }
        });

        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {

            }
        });

        // Set the GuideAdapter to use the search layout
        mAdapter.setUseSearchLayout(true);
        mBinding.searchResultsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.searchResultsRv.setAdapter(mAdapter);

        return mBinding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACES_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Get the Place selected by the user
                Place place = PlaceAutocomplete.getPlace(getActivity(), data);

                // Convert the Google LatLng to the Mapbox LatLng so the camera can be properly
                // positioned
                com.google.android.gms.maps.model.LatLng result = place.getLatLng();
                LatLng target = new LatLng(result.latitude, result.longitude);

                if (mMapboxMap != null) {
                    // Move the camera to the correct position
                    mMapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .target(target)
                            .zoom(12)
                            .build());
                }

                // Create a GeoLocation that can be used to search Firebase Database for guides in
                // the nearby area
                GeoLocation location = new GeoLocation(result.latitude, result.longitude);

                // Use GeoFire to query for Guides in the area that was searched
                queryGeoFire(location);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Uses GeoFire to query the FirebaseDatabase for Guides in the vicinity of the search location
     *
     * @param location    Coordinates to search the Firebase Database for nearby Guides
     */
    private void queryGeoFire(GeoLocation location) {

        // Get a reference to the GeoFire path in the Firebase Database
        DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference()
                .child(GEOFIRE_PATH);

        // Use GeoFire to build a query
        GeoFire geofire = new GeoFire(firebaseRef);
        GeoQuery geoQuery = geofire.queryAtLocation(location, 10);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Get the Guide data model that entered the search area
                getGuide(key, mColorPosition);
                mColorPosition++;
            }

            @Override
            public void onKeyExited(String key) {

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
        });
    }

    /**
     * Retrievs a Guide data model from the Firebase Database
     *
     * @param firebaseId       The ID of the guide to be retrieved
     * @param colorPosition    The position of the Guide that will be used to generate its track's
     *                         color
     */
    private void getGuide(String firebaseId, final int colorPosition) {

        // Get a reference to the Guide in the Firebase Database using the firebaseId
        final DatabaseReference guideReference = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .child(firebaseId);

        guideReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Convert the DataSnapshot to a Guide
                Guide guide = (Guide) FirebaseProviderUtils.getModelFromSnapshot(
                        DatabaseProvider.FirebaseType.GUIDE,
                        dataSnapshot);

                // Add the Guide to the Adapter
                mAdapter.addGuide(guide);

                // Get the GPX File for the Guide
                getGpxForGuide(guide, colorPosition);

                // Remove the Listener
                guideReference.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Downloads the GPX File for a Guide
     *
     * @param guide         Guide whose GPX File is to be downloaded
     * @param colorPosition The position of the Guide in the Adapter that will be used to generate
     *                      its track's color
     */
    private void getGpxForGuide(Guide guide, final int colorPosition) {

        // Create a file in the temp files directory that has a constant name so it can be
        // referenced later without downloading another copy while it exists in the cache
        final File tempGpxFile = SaveUtils.createTempFile(
                StorageProvider.FirebaseFileType.GPX_FILE,
                guide.firebaseId);

        // Get a reference to the GPX File in Firebase Storage
        StorageReference gpxReference = FirebaseStorage.getInstance().getReference()
                .child(GPX_PATH)
                .child(guide.firebaseId + GPX_EXT);

        // Download the file
        gpxReference.getFile(tempGpxFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Add the Polyline representing the trail to the map
                        addPolylineOptionsToMap(tempGpxFile, colorPosition);
                    }
                });


    }

    /**
     * Adds a Polyline representing a Guide's trail as calculated by its GPX File
     *
     * @param gpxFile           GPX File containing coordinates representing a trail
     * @param colorPosition    The position of the Guide that will be used to generate its track's
     *                         color
     */
    private void addPolylineOptionsToMap(File gpxFile, final int colorPosition) {

        // Generate the MapboxOptions that will contain the Polyline
        GpxUtils.getMapboxOptions(gpxFile, new GpxUtils.MapboxOptionsListener() {
            @Override
            public void onOptionReady(MarkerOptions markerOptions, PolylineOptions polylineOptions) {

                // Add the Polyline to the MapboxMap
                mMapboxMap.addPolyline(polylineOptions
                        .width(2)
                        // Set the color using the the colorPosition and the ColorGenerator
                        .color(ColorGenerator.getColor(getActivity(), colorPosition)));
            }
        });
    }

    /**
     * Launches the Places AutoCompleteSearch Widget in an overlay for searching for areas by name
     */
    private void launchPlacesSearch() {
        try {
            // Build the Intent to launch the Widget in overlay mode
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(getActivity());

            // Start Intent
            startActivityForResult(intent, PLACES_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

}