package project.hikerguide.ui;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityGuideDetailsBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import timber.log.Timber;

import static project.hikerguide.ui.GuideDetailsActivity.IntentKeys.GUIDE_KEY;

public class GuideDetailsActivity extends MapboxActivity {
    // ** Constants ** //
    public interface IntentKeys {
        String GUIDE_KEY = "guides";
    }

    // ** Member Variables ** //
    private ActivityGuideDetailsBinding mBinding;
    private Guide mGuide;
    private GuideDetailsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_guide_details);

        setSupportActionBar(mBinding.guideDetailsTb);

        if ((mGuide = getIntent().getParcelableExtra(GUIDE_KEY)) == null) {
            Timber.d("No Guide passed from MainActivity");
            return;
        }

        // Setup the Adapter
        mAdapter = new GuideDetailsAdapter();

        // Setup the RecyclerView
        mBinding.setVm(new GuideViewModel(this, mGuide));
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(this));
        mBinding.guideDetailsRv.setAdapter(mAdapter);
    }
}
