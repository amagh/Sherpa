package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import project.hikerguide.models.datamodels.PlaceModel;

/**
 * Created by Alvin on 8/2/2017.
 */

public class PlaceViewModel extends BaseObservable {
    // ** Member Variables ** //
    private PlaceModel mPlaceModel;
    private SearchAreaViewModel mViewModel;

    public PlaceViewModel(PlaceModel placeModel, SearchAreaViewModel viewModel) {
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
        mViewModel.changeMapCamera(mPlaceModel.placeId);
    }

}
