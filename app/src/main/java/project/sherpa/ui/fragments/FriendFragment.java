package project.sherpa.ui.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import project.sherpa.R;
import project.sherpa.databinding.FragmentFriendBinding;
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

public class FriendFragment extends BaseFriendFragment {

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
            loadFriendsList();
        } else {
            updateFriendsList();
        }
    }

    /**
     * Loads the friend list and adds them to the Adapter
     */
    private void loadFriendsList() {
        if (mUser.getFriends() == null) {
            hideProgressBar();
            return;
        }

        Timber.d("Loading friends");

        // Get each friend from the friend list and add them to the Adapter
        for (String friendId : mUser.getFriends()) {
            addFriendToAdapter(friendId);
        }
    }

    /**
     * Updates the friend List
     */
    private void updateFriendsList() {
        Timber.d("Updating friends list");
        if (mUser.getFriends() == null || mUser.getFriends().size() == 0) {
            Timber.d("Clearing friend adapter");
            hideProgressBar();
            mAdapter.clear();
            return;
        }

        List<String> adapterIdList = mAdapter.getFirebaseIds();

        // Remove any users that are no longer friends with the user
        for (String adapterFriendId : adapterIdList) {
            if (!mUser.getFriends().contains(adapterFriendId)) {
                Timber.d("Removing " + adapterFriendId + " from the Adapter");
                mAdapter.removeFriend(adapterFriendId);
            }
        }

        // Add any users that have become friends with the user
        for (String newFriendId : mUser.getFriends()) {
            if (!adapterIdList.contains(newFriendId)) {
                Timber.d("Adding " + newFriendId + " to the Adapter");
                addFriendToAdapter(newFriendId);
            }
        }
    }

    /**
     * Downloads a user's profile and adds them to the Adapter
     *
     * @param friendId    FirebaseId of the friend to be added to the Adapter
     */
    private void addFriendToAdapter(String friendId) {
        FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, friendId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        hideProgressBar();

                        if (model == null) return;
                        mAdapter.addFriend((Author) model);
                    }
                });
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        hideProgressBar();
    }
}
