package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.view.View;

import project.hikerguide.ui.activities.GuideDetailsActivity;

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
