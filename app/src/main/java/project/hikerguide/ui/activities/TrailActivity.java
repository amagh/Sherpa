package project.hikerguide.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityTrailBinding;
import project.hikerguide.models.viewmodels.SearchTrailViewModel;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_trail);

        SearchTrailViewModel vm = new SearchTrailViewModel(this);
        mBinding.setVm(vm);
    }
}
