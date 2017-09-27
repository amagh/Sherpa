package project.sherpa.ui.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.databinding.ActivityFriendFollowBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.FriendFollowViewModel;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;

/**
 * Created by Alvin on 9/27/2017.
 */

public class FriendFollowActivity extends ConnectivityActivity {

    // ** Member Variables ** //
    private ActivityFriendFollowBinding mBinding;
    private Author mSendUser;
    private Author mReceiveUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_friend_follow);

        if (getIntent() == null) finish();

        // Start the process that will load the ViewModel
        loadViewModel();

        // Load the user profile for the logged in FirebaseUser
        loadSendUserProfile();

        // Load the user profile for the user being accessed
        String userId = getIntent().getStringExtra(AUTHOR_KEY);
        loadReceiveUserProfile(userId);
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

        if (mSendUser == null) {

            // Retrieve the user from Firebase
            FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, user.getUid(),
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model == null) return;
                            DataCache.getInstance().store(model);

                            loadSendUserProfile();
                        }
                    });
        } else {
            notifyAll();
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

        if (mReceiveUser == null) {

            // Retrieve the user from Firebase
            FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, userId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model == null) return;
                            DataCache.getInstance().store(model);

                            loadReceiveUserProfile(userId);
                        }
                    });
        } else {
            notifyAll();
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
                    FriendFollowViewModel vm =
                            new FriendFollowViewModel(FriendFollowActivity.this, mSendUser, mReceiveUser);

                    mBinding.setVm(vm);
                    mBinding.notifyPropertyChanged(BR._all);
                }
            }
        });

        thread.start();
    }
}
