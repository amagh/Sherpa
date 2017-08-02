package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import project.hikerguide.models.datamodels.Area;

/**
 * Created by Alvin on 8/2/2017.
 */

public class AreaViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Area mArea;

    public AreaViewModel(Area area) {
        mArea = area;
    }

    @Bindable
    public String getName() {
        return mArea.name;
    }

    @Bindable
    public String getState() {
        return mArea.state;
    }

    public void onClickGeoLocation(View view) {

    }
}
