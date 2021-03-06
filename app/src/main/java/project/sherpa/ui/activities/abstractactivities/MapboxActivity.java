package project.sherpa.ui.activities.abstractactivities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;

import java.util.HashSet;
import java.util.Set;

import project.sherpa.R;

/**
 * Created by Alvin on 7/24/2017.
 */

public abstract class MapboxActivity extends ConnectivityActivity {
    Set<MapView> mMapSet;
    Bundle mSavedInstanceState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        Mapbox.getInstance(this, getString(R.string.mapbox_token));

        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onCreate(savedInstanceState);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onStart();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onResume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onPause();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onStop();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onSaveInstanceState(outState);
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onLowMemory();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapSet != null) {
            for (MapView mapView : mMapSet) {
                mapView.onDestroy();
            }
        }
    }

    public void attachMapView(MapView mapView) {

        if (mMapSet == null) {
            mMapSet = new HashSet<>();
        }

        mMapSet.add(mapView);

    }

    public Bundle getSavedInstanceState() {
        return mSavedInstanceState;
    }
}
