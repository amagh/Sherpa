package project.hikerguide.models.viewmodels;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;

/**
 * Created by Alvin on 8/3/2017.
 */

public class SearchTrailViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Activity mActivity;
    private String mQuery;
    private boolean mSearchHasFocus = false;

    public SearchTrailViewModel(Activity activity) {
        mActivity = activity;
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;
    }
}
