package project.hikerguide.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;

import project.hikerguide.R;

/**
 * Created by Alvin on 7/24/2017.
 */

public class MapboxActivity extends ConnectivityActivity {
    // ** Member Variables ** //
    MapView mMapView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_token));

        if (mMapView != null) {
            mMapView.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mMapView != null) {
            mMapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mMapView != null) {
            mMapView.onStop();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null) {
            mMapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
    }

    public void attachMapView(MapView mapView) {
        mMapView = mapView;
    }
}
