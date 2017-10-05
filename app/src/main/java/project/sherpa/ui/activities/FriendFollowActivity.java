package project.sherpa.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.databinding.ActivityFriendFollowBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.viewmodels.FriendFollowViewModel;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.services.firebaseservice.FirebaseProviderService.FirebaseProviderBinder;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;

/**
 * Created by Alvin on 9/27/2017.
 */

public class FriendFollowActivity extends ConnectivityActivity implements ConnectivityActivity.ConnectivityCallback{

    // ** Member Variables ** //
    private ActivityFriendFollowBinding mBinding;
    private Author mSendUser;
    private Author mReceiveUser;
    private FriendFollowViewModel mViewModel;

    private ModelChangeListener<Author> mSendUserListener;
    private ModelChangeListener<Author> mReceiveUserListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_friend_follow);
        bindFirebaseProviderService(true);

        if (getIntent() == null) finish();

        addConnectivityCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSendUserListener != null) mService.registerModelChangeListener(mSendUserListener);
        if (mReceiveUserListener != null) mService.registerModelChangeListener(mReceiveUserListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSendUserListener != null) mService.unregisterModelChangeListener(mSendUserListener);
        if (mReceiveUserListener != null) mService.unregisterModelChangeListener(mReceiveUserListener);
    }

    @Override
    public void onConnected() {
        FirebaseDatabase.getInstance().goOnline();
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();
    }

    @Override
    protected void onServiceConnected() {

        // Load the user profile for the logged in FirebaseUser
        loadSendUserProfile();

        // Load the user profile for the user being accessed
        String userId = getIntent().getStringExtra(AUTHOR_KEY);
        loadReceiveUserProfile(userId);
    }

    /**
     * Sets the title for the ActionBar
     */
    private void initToolbar() {
        setSupportActionBar(mBinding.friendFollowTb);
        getSupportActionBar().setTitle(getString(R.string.friend_follow_title, mReceiveUser.getUsername()));
    }

    /**
     * Loads the Firebase profile for the logged in FirebaseUser
     */
    private void loadSendUserProfile() {

        // Get the FirebaseUser
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {

            // User is not logged in. They should not be able to access this Activity
            finish();
            return;
        }

        mSendUserListener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
            @Override
            public void onModelReady(Author model) {
                mSendUser = model;

                // ViewModel not instantiated, attempt to load the ViewModel
                if (mViewModel == null) loadViewModel();

                    // ViewModel is loaded, notify it to update its values
                else mViewModel.notifyPropertyChanged(BR._all);
            }

            @Override
            public void onModelChanged() {

                // ViewModel not instantiated, attempt to load the ViewModel
                if (mViewModel == null) loadViewModel();

                    // ViewModel is loaded, notify it to update its values
                else mViewModel.notifyPropertyChanged(BR._all);
            }
        };

        mService.registerModelChangeListener(mSendUserListener);
    }

    /**
     * Loads the Firebase Profile for the user that will be receiving requests
     *
     * @param userId    FirebaseId of the user to be retrieved
     */
    private void loadReceiveUserProfile(final String userId) {

        mReceiveUserListener = new ModelChangeListener<Author>(AUTHOR, userId) {
            @Override
            public void onModelReady(Author model) {
                mReceiveUser = model;

                // ViewModel not instantiated, attempt to load the ViewModel
                if (mViewModel == null) {
                    loadViewModel();
                    initToolbar();
                }

                    // ViewModel is loaded, notify it to update its values
                else mViewModel.notifyPropertyChanged(BR._all);
            }

            @Override
            public void onModelChanged() {
                // ViewModel not instantiated, attempt to load the ViewModel
                if (mViewModel == null) loadViewModel();

                    // ViewModel is loaded, notify it to update its values
                else mViewModel.notifyPropertyChanged(BR._all);
            }
        };

        mService.registerModelChangeListener(mReceiveUserListener);
    }

    /**
     * Loads the ViewModel for this Activity asynchronously
     */
    private void loadViewModel() {

        if (mReceiveUser == null || mSendUser == null) return;

        // Init and set the ViewModel
        mViewModel = new FriendFollowViewModel(FriendFollowActivity.this, mSendUser, mReceiveUser);

        mBinding.setVm(mViewModel);
        mBinding.notifyPropertyChanged(BR._all);
    }
}
