package project.hikerguide.ui.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.view.menu.MenuAdapter;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.ActivityGuideDetailsBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.ui.adapters.GuideDetailsFragmentAdapter;
import project.hikerguide.ui.fragments.GuideDetailsFragment;
import project.hikerguide.ui.fragments.GuideDetailsMapFragment;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;

public class GuideDetailsActivity extends MapboxActivity {
    // ** Member Variables ** //
    private ActivityGuideDetailsBinding mBinding;
    private GuideDetailsFragmentAdapter mAdapter;
    private Guide mGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_guide_details);

        if (getIntent().getParcelableExtra(GUIDE_KEY) != null) {
            mGuide = getIntent().getParcelableExtra(GUIDE_KEY);
        } else {
            Timber.d("No guide passed in Intent!");
        }

        initViewPager();
    }

    private void initViewPager() {
        mAdapter = new GuideDetailsFragmentAdapter(getSupportFragmentManager());

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(GuideDetailsFragment.newInstance(mGuide));

        mAdapter.swapFragmentList(fragmentList);
        mBinding.guideDetailsVp.setAdapter(mAdapter);
    }
}
