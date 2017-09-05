package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;

import project.hikerguide.models.datamodels.Trail;

/**
 * Created by Alvin on 8/3/2017.
 */

public class TrailViewModel extends BaseObservable {
    // ** Member Variables ** //
    private DoubleSearchViewModel mViewModel;
    private Trail mTrail;

    public TrailViewModel(DoubleSearchViewModel viewModel, Trail trail) {
        mViewModel = viewModel;
        mTrail = trail;
    }

    @Bindable
    public String getName() {
        return mTrail.name;
    }

    public void onClickGeoLocation(View view) {
        LatLng latLng = new LatLng(mTrail.getLatitude(), mTrail.getLongitude());
        mViewModel.changeMapCamera(latLng);
    }
}
