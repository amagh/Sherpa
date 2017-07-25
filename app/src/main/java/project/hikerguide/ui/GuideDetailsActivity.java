package project.hikerguide.ui;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityGuideDetailsBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.viewmodels.GuideViewModel;
import timber.log.Timber;

import static project.hikerguide.ui.GuideDetailsActivity.IntentKeys.GUIDE_KEY;

public class GuideDetailsActivity extends AppCompatActivity {
    // ** Constants ** //
    public interface IntentKeys {
        String GUIDE_KEY = "guides";
    }

    // ** Member Variables ** //
    private ActivityGuideDetailsBinding mBinding;
    private Guide mGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_guide_details);

        setSupportActionBar(mBinding.guideDetailsTb);

        if ((mGuide = getIntent().getParcelableExtra(GUIDE_KEY)) == null) {
            Timber.d("No Guide passed from MainActivity");
            return;
        }

        mBinding.setVm(new GuideViewModel(this, mGuide));
    }
}
