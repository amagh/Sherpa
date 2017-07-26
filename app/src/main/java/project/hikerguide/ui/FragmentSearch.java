package project.hikerguide.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import project.hikerguide.R;
import project.hikerguide.databinding.FragmentSearchBinding;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Alvin on 7/25/2017.
 */

public class FragmentSearch extends MapboxFragment {
    // ** Constants ** //
    private static final int PLACES_REQUEST_CODE = 6498;

    // ** Member Variables ** //
    FragmentSearchBinding mBinding;
    MapboxMap mMapboxMap;

    public FragmentSearch() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get a reference to the ViewDataBinding and inflate the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);

        // Launch the AutoCompleteSearchWidget
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(getActivity());
            startActivityForResult(intent, PLACES_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

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
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}