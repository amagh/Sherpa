package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.view.View;

import project.sherpa.ui.activities.GuideDetailsActivity;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsMapViewModel extends BaseObservable {
    // ** Member Variables ** //
    private GuideDetailsActivity mActivity;

    public GuideDetailsMapViewModel(GuideDetailsActivity activity) {
        mActivity = activity;
    }

    public void onClickBack(View view) {
        mActivity.switchPage(0);
    }
}
