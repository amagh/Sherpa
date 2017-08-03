package project.hikerguide.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityTrailBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.viewmodels.SearchTrailViewModel;

import static project.hikerguide.ui.activities.TrailActivity.IntentKeys.AREA;

/**
 * Created by Alvin on 8/3/2017.
 */

public class TrailActivity extends AppCompatActivity {
    // ** Constants ** //
    public interface IntentKeys {
        String AREA = "area";
    }

    // ** Member Variables ** //
    private ActivityTrailBinding mBinding;
    private Area mArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_trail);

        if (getIntent().getParcelableExtra(AREA) != null) {
            mArea = getIntent().getParcelableExtra(AREA);
        }

        SearchTrailViewModel vm = new SearchTrailViewModel(this, mArea);
        mBinding.setVm(vm);
    }
}
