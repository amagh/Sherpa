package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;

import project.sherpa.models.datamodels.PlaceModel;
import project.sherpa.utilities.GooglePlacesApiUtils;

/**
 * Created by Alvin on 8/2/2017.
 */

public class PlaceViewModel extends BaseObservable {
    // ** Member Variables ** //
    private PlaceModel mPlaceModel;
    private DoubleSearchViewModel mViewModel;

    public PlaceViewModel(PlaceModel placeModel, DoubleSearchViewModel viewModel) {
        mPlaceModel = placeModel;
        mViewModel = viewModel;
    }

    @Bindable
    public String getPrimaryText() {
        return mPlaceModel.primaryText;
    }

    @Bindable
    public String getSecondaryText() {
        return mPlaceModel.secondaryText;
    }

    public void onClickGeolocation(View view) {

        // Get the LatLng corresponding to the PlaceId of the PlaceModel
        GooglePlacesApiUtils.getMapboxLatLngForPlaceId(mViewModel.getGoogleApiClient(),
                mPlaceModel.placeId,
                new GooglePlacesApiUtils.CoordinateListener() {
            @Override
            public void onCoordinatesReady(LatLng latLng) {

                // Move the camera to the coordinates
                mViewModel.changeMapCamera(latLng);
            }
        });
    }

}
