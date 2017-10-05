package project.sherpa.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import project.sherpa.R;
import project.sherpa.databinding.ActivityFriendBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.ui.adapters.FriendFragmentAdapter;
import project.sherpa.services.firebaseservice.FirebaseProviderService.*;
import project.sherpa.ui.fragments.abstractfragments.BaseFriendFragment;

import static project.sherpa.ui.activities.SearchUserActivity.SearchTypes.FOLLOW;
import static project.sherpa.ui.activities.SearchUserActivity.SearchTypes.FRIEND;
import static project.sherpa.utilities.Constants.IntentKeys.SEARCH_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FriendActivity extends ConnectivityActivity {

    // ** Member Variables ** //
    private ActivityFriendBinding mBinding;
    private FriendFragmentAdapter mAdapter;
    private int mSelectedPage;
    private ModelChangeListener<Author> mAuthorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_friend);
        bindFirebaseProviderService(true);

        initViewPager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthorListener != null) mService.registerModelChangeListener(mAuthorListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthorListener != null) mService.unregisterModelChangeListener(mAuthorListener);
    }

    @Override
    protected void onServiceConnected() {
        setAuthorChangeListener();
        getCurrentFragment().onAuthorChanged(mAuthorListener.getModel());
    }

    /**
     * Sets the ModelChangeListener for the logged in user
     */
    private void setAuthorChangeListener() {

        if (mAuthorListener != null) return;

        // Check to ensure the user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        // Initialize the ModelChangeListener
        mAuthorListener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
            @Override
            public void onModelReady(Author model) {
                getCurrentFragment().onAuthorChanged(getModel());
            }

            @Override
            public void onModelChanged() {
                getCurrentFragment().onAuthorChanged(getModel());
            }
        };

        mService.registerModelChangeListener(mAuthorListener);
    }

    /**
     * Gets the Fragment currently being displayed to the user
     * @return Fragment being displayed
     */
    private BaseFriendFragment getCurrentFragment() {
        return (BaseFriendFragment) mAdapter.getItem(mBinding.friendVp.getCurrentItem());
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
                getCurrentFragment().onAuthorChanged(mAuthorListener.getModel());
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

    /**
     * Click response for the FAB to start the SearchUserActivity for either following or sending
     * friend requests
     *
     * @param view    FAB that was clicked
     */
    public void onClickAddSocialFab(View view) {

        // Build the Intent to launch the SearchUserActivity
        Intent intent = new Intent(this, SearchUserActivity.class);

        // Put an extra for whether the user is looking for users to follow or friend
        if (mBinding.friendVp.getCurrentItem() == 0) {
            intent.putExtra(SEARCH_KEY, FRIEND);
        } else if (mBinding.friendVp.getCurrentItem() == 1) {
            intent.putExtra(SEARCH_KEY, FOLLOW);
        }

        startActivity(intent);
    }
}
