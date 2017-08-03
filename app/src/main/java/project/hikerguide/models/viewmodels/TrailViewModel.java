package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import project.hikerguide.models.datamodels.Trail;

/**
 * Created by Alvin on 8/3/2017.
 */

public class TrailViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Trail mTrail;

    public TrailViewModel(Trail trail) {
        mTrail = trail;
    }

    @Bindable
    public String getName() {
        return mTrail.name;
    }
}
