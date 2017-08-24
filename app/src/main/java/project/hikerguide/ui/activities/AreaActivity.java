package project.hikerguide.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityAreaBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.viewmodels.SearchAreaViewModel;

import static project.hikerguide.utilities.Constants.IntentKeys.AUTHOR_KEY;

/**
 * Created by Alvin on 8/1/2017.
 */

public class AreaActivity extends MapboxActivity {
    // ** Member Variables ** //
    ActivityAreaBinding mBinding;
    Author mAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_area);

        mAuthor = getIntent().getParcelableExtra(AUTHOR_KEY);

        // Initialize ViewModel for the layout
        SearchAreaViewModel vm = new SearchAreaViewModel(this);
        mBinding.setVm(vm);
    }

    public Author getAuthor() {
        return mAuthor;
    }
}
