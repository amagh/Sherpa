package project.hikerguide.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityTrailBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.viewmodels.SearchTrailViewModel;

import static project.hikerguide.utilities.IntentKeys.AREA_KEY;
import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;

/**
 * Created by Alvin on 8/3/2017.
 */

public class TrailActivity extends AppCompatActivity {
    // ** Member Variables ** //
    private ActivityTrailBinding mBinding;
    private Author mAuthor;
    private Area mArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_trail);
        setSupportActionBar(mBinding.toolbar);
        getSupportActionBar().setTitle(getString(R.string.trail_activity_title));

        if (getIntent().getParcelableExtra(AREA_KEY) != null) {
            mAuthor = getIntent().getParcelableExtra(AUTHOR_KEY);
            mArea = getIntent().getParcelableExtra(AREA_KEY);
        }

        SearchTrailViewModel vm = new SearchTrailViewModel(this);
        mBinding.setVm(vm);
    }

    public Area getArea() {
        return mArea;
    }

    public Author getAuthor() {
        return mAuthor;
    }
}
