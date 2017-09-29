package project.sherpa.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;

import project.sherpa.R;
import project.sherpa.databinding.ActivityFriendBinding;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.ui.adapters.FriendFragmentAdapter;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FriendActivity extends ConnectivityActivity {

    // ** Member Variables ** //
    private ActivityFriendBinding mBinding;
    private FriendFragmentAdapter mAdapter;
    private int mSelectedPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_friend);

        initViewPager();
    }

    /**
     * Initializes the ViewPager and its components
     */
    private void initViewPager() {
        mAdapter = new FriendFragmentAdapter(this, getSupportFragmentManager());

        // Listener to be attached to FAB when hiding it
        final FloatingActionButton.OnVisibilityChangedListener listener = new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);

                // Only re-show the FAB if the the page should have a FAB
                if (mSelectedPage < 2) fab.show();
            }
        };

        mBinding.friendVp.setAdapter(mAdapter);
        mBinding.friendTs.setViewPager(mBinding.friendVp);
        mBinding.friendTs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                // Show the FAB if it has been hidden
                if (positionOffset == 0) {
                    switch (position) {
                        case 0:
                        case 1:
                            if (!mBinding.friendFab.isShown()) {
                                mBinding.friendFab.show();
                            }
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                // Set the selected position as member variable to be accessed by the
                // OnVisibilityChangedListener
                mSelectedPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

                // Hide the FAB while scrolling
                switch (state) {
                    case ViewPager.SCROLL_STATE_SETTLING:
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        mBinding.friendFab.hide(listener);
                        break;
                }
            }
        });
    }
}
