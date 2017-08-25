package project.hikerguide.ui.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityGuideDetailsBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.adapters.GuideDetailsFragmentAdapter;
import project.hikerguide.ui.fragments.GuideDetailsFragment;
import project.hikerguide.ui.fragments.GuideDetailsMapFragment;
import timber.log.Timber;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static project.hikerguide.utilities.Constants.IntentKeys.GUIDE_KEY;

public class GuideDetailsActivity extends MapboxActivity implements ViewPager.OnPageChangeListener {
    // ** Constants ** //
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1654;

    // ** Member Variables ** //
    private ActivityGuideDetailsBinding mBinding;
    private GuideDetailsFragmentAdapter mAdapter;
    private Guide mGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_guide_details);

        // Get the Guide passed in the Intent
        if (getIntent().getParcelableExtra(GUIDE_KEY) != null) {
            mGuide = getIntent().getParcelableExtra(GUIDE_KEY);
        } else {
            Timber.d("No guide passed in Intent!");
        }

        // Init the ViewPager
        initViewPager();
    }

    /**
     * Initializes the variables required for the ViewPager
     */
    private void initViewPager() {

        // Initialize the Adapter
        mAdapter = new GuideDetailsFragmentAdapter(getSupportFragmentManager());

        // Initialize the Fragments that will be contained by the ViewPager
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(GuideDetailsFragment.newInstance(mGuide));
        fragmentList.add(GuideDetailsMapFragment.newInstance(mGuide));

        // Set the List of Fragments for the Adapter
        mAdapter.swapFragmentList(fragmentList);

        // Set the Adapter
        mBinding.guideDetailsVp.addOnPageChangeListener(this);
        mBinding.guideDetailsVp.setAdapter(mAdapter);
    }

    /**
     * Changes the page currently displayed in the ViewPager
     *
     * @param page    The page to switch to
     */
    public void switchPage(int page) {
        mBinding.guideDetailsVp.setCurrentItem(page);
    }

    @Override
    public void onBackPressed() {

        // If the user is viewing the map, pressing the back button should have the same action as
        // pressing the back arrow in the top left (i.e. go back to the Guide details)
        if (mBinding.guideDetailsVp.getCurrentItem() == 1) {
            switchPage(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        if (position == 0) {
            // Enable swiping if on the Guide details page
            mBinding.guideDetailsVp.setSwipe(true);
        } else {
            // Disable on map page to allow for panning of map
            mBinding.guideDetailsVp.setSwipe(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * Requests permission to access FINE_LOCATION for the device
     */
    public void requestLocationPermission() {

        // Request permission for FINE_LOCATION if it has not been granted yet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            if (mAdapter.getItem(1) != null) {
                ((GuideDetailsMapFragment) mAdapter.getItem(1)).trackUserPosition();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION && grantResults[0] == PERMISSION_GRANTED) {

            // Permission granted. Begin tracking user's location
            ((GuideDetailsMapFragment) mAdapter.getItem(1)).trackUserPosition();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
