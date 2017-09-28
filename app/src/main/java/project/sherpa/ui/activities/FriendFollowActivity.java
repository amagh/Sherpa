package project.sherpa.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.databinding.ActivityFriendFollowBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.FriendFollowViewModel;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.objects.SmartValueEventListener;
import timber.log.Timber;

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
    private SmartValueEventListener[] listeners = new SmartValueEventListener[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_friend_follow);

        if (getIntent() == null) finish();

        addConnectivityCallback(this);

        // Start the process that will load the ViewModel
        loadViewModel();

        // Load the user profile for the logged in FirebaseUser
        loadSendUserProfile();

        // Load the user profile for the user being accessed
        String userId = getIntent().getStringExtra(AUTHOR_KEY);
        loadReceiveUserProfile(userId);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start listening for changes
        for (SmartValueEventListener listener : listeners) {
            if (listener != null) listener.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop listening for changes
        for (SmartValueEventListener listener : listeners) {
            if (listener != null) listener.stop();
        }
    }

    @Override
    public void onConnected() {
        FirebaseDatabase.getInstance().goOnline();
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();
    }

    /**
     * Loads the Firebase profile for the logged in FirebaseUser
     */
    private synchronized void loadSendUserProfile() {

        // Get the FirebaseUser
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {

            // User is not logged in. They should not be able to access this Activity
            finish();
            return;
        }

        // Attempt to retrieve the user from the cache
        mSendUser = (Author) DataCache.getInstance().get(user.getUid());

        if (listeners[0] == null) {
            // Author does not exist in the cache, load it from Firebase
            SmartValueEventListener listener = new SmartValueEventListener(AUTHOR, user.getUid()) {
                @Override
                public void onModelChange(BaseModel model) {
                    if (model == null) return;
                    DataCache.getInstance().store(model);

                    loadSendUserProfile();
                }
            };

            listener.start();
            listeners[0] = listener;
        }

        if (mSendUser != null && mViewModel == null) {

            // ViewModel not instantiated, notify the Thread that mSendUser is loaded
            notifyAll();
        } else if (mSendUser != null) {

            // ViewModel is loaded, notify it to update its values
            mViewModel.notifyPropertyChanged(BR._all);
        }
    }

    /**
     * Loads the Firebase Profile for the user that will be receiving requests
     *
     * @param userId    FirebaseId of the user to be retrieved
     */
    private synchronized void loadReceiveUserProfile(final String userId) {

        // Attempt to retrieve the user from the cache
        mReceiveUser = (Author) DataCache.getInstance().get(userId);

        if (listeners[1] == null) {
            // Author does not exist in the cache, load it from Firebase
            SmartValueEventListener listener = new SmartValueEventListener(AUTHOR, userId) {
                @Override
                public void onModelChange(BaseModel model) {
                    if (model == null) return;
                    DataCache.getInstance().store(model);

                    loadReceiveUserProfile(userId);
                }
            };

            listener.start();
            listeners[1] = listener;
        }

        if (mReceiveUser != null && mViewModel == null) {

            // ViewModel not instantiated, notify the Thread that mReceiveUser is loaded
            notifyAll();
        } else if (mReceiveUser != null) {

            // ViewModel is loaded, notify it to update its values
            mViewModel.notifyPropertyChanged(BR._all);
        }
    }

    /**
     * Loads the ViewModel for this Activity asynchronously
     */
    private void loadViewModel() {

        // Init the Thread that will set the ViewModel
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                // Lock to the Activity
                synchronized (FriendFollowActivity.this) {

                    // Wait while mReceiveUser and mSendUser are loading
                    while (mReceiveUser == null || mSendUser == null) {
                        try {
                            FriendFollowActivity.this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Init and set the ViewModel
                    mViewModel = new FriendFollowViewModel(FriendFollowActivity.this, mSendUser, mReceiveUser);

                    mBinding.setVm(mViewModel);
                    mBinding.notifyPropertyChanged(BR._all);
                }
            }
        });

        thread.start();
    }
}
