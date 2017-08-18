package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import project.hikerguide.BR;

/**
 * Created by Alvin on 8/17/2017.
 */

public class AttributionViewModel extends BaseObservable {
    private boolean mShowProgress = false;

    @Bindable
    public int getProgressVisibility() {
        if (mShowProgress) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    public void setShowProgress(boolean showProgress) {
        mShowProgress = showProgress;

        notifyPropertyChanged(BR.progressVisibility);
    }
}
