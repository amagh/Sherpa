package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;

import project.hikerguide.models.datamodels.Area;

/**
 * Created by Alvin on 8/2/2017.
 */

public class AreaViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Area mArea;
    private DoubleSearchViewModel mViewModel;
    private int mLatLngVisibility;

    public AreaViewModel(Area area, DoubleSearchViewModel viewModel) {
        mArea = area;
        mViewModel = viewModel;

        if (mArea.latitude != 0 && mArea.longitude != 0) {
            mLatLngVisibility = View.VISIBLE;
        } else {
            mLatLngVisibility = View.GONE;
        }
    }

    @Bindable
    public String getName() {
        return mArea.name;
    }

    @Bindable
    public String getState() {
        return mArea.state;
    }

    @Bindable
    public int getLatLngVisibility() {
        return mLatLngVisibility;
    }

    public void onClickGeolocation(View view) {
        LatLng latLng = new LatLng(mArea.latitude, mArea.longitude);
        mViewModel.changeMapCamera(latLng);
    }
}
