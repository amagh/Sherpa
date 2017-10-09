package project.sherpa.ui.fragments;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;

import java.util.List;

import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.activities.UserActivity;
import project.sherpa.ui.adapters.FriendAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.fragments.abstractfragments.BaseFriendFragment;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FollowingFragment extends BaseFriendFragment {

    // ** Member Variables ** //
    private FriendAdapter mAdapter;

    /**
     * Initializes the RecyclerView and its components
     */
    @Override
    protected void initRecyclerView() {
        mAdapter = new FriendAdapter(new ClickHandler<Author>() {
            @Override
            public void onClick(Author clickedItem) {
                Intent intent = new Intent(getActivity(), UserActivity.class);
                intent.putExtra(AUTHOR_KEY, clickedItem.firebaseId);

                startActivity(intent);
            }
        });

        mBinding.friendRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.friendRv.setAdapter(mAdapter);
    }

    @Override
    public void onAuthorChanged(Author user) {
        if (mUser == null) {
            mUser = user;
            loadFollowingList();
        } else {
            updateFollowingList();
        }
    }

    /**
     * Loads the user's list of users that they are following and adds them to the Adapter
     */
    private void loadFollowingList() {
        if (mUser.getFollowing() == null) return;

        Timber.d("Loading following");

        // Get each user from the user's list of people they are following and add them to the
        // Adapter
        for (String userId : mUser.getFollowing()) {
            addUserToAdapter(userId);
        }
    }

    /**
     * Updates the Adapter by removing users that are no longer being followed and adding new users
     * that the user has chosen to follow
     */
    private void updateFollowingList() {
        Timber.d("Updating following list");
        if (mUser.getFollowing() == null) {
            Timber.d("Clearing following adapter");
            mAdapter.clear();
            return;
        }

        List<String> adapterIdList = mAdapter.getFirebaseIds();

        // Remove any users that are no longer being followed
        for (String adapterUserId : adapterIdList) {
            if (!mUser.getFollowing().contains(adapterUserId)) {
                Timber.d("Removing " + adapterUserId + " from the adapter");
                mAdapter.removeFriend(adapterUserId);
            }
        }

        // Add new users being followed
        for (String newUserId : mUser.getFollowing()) {
            if (!adapterIdList.contains(newUserId)) {
                Timber.d("Adding " + newUserId + " to adapter");
                addUserToAdapter(newUserId);
            }
        }
    }

    /**
     * Downloads a user's profile adds them to the Adapter
     *
     * @param userId    FirebaseId of the user to be added to the Adapter
     */
    private void addUserToAdapter(String userId) {
        FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, userId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        if (model == null) return;

                        mAdapter.addFriend((Author) model);
                    }
                });
    }
}
