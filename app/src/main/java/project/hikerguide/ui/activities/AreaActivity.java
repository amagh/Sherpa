package project.hikerguide.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityAreaBinding;
import project.hikerguide.models.viewmodels.SearchViewModel;

/**
 * Created by Alvin on 8/1/2017.
 */

public class AreaActivity extends MapboxActivity {
    // ** Member Variables ** //
    ActivityAreaBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_area);

        // Initialize ViewModel for the layout
        SearchViewModel vm = new SearchViewModel(this);
        mBinding.setVm(vm);
    }

}
