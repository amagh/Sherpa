package project.sherpa.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import project.sherpa.R;
import project.sherpa.databinding.FragmentFriendBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.adapters.FriendAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.fragments.abstractfragments.BaseFriendFragment;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

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

            }
        });
        mBinding.friendRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.friendRv.setAdapter(mAdapter);
    }

    /**
     * Loads the user's profile
     */
    @Override
    protected void loadUser() {
        super.loadUser();

        if (mUser != null) {

            // Load the friends list for this user
            loadFriendsList(mUser);
        }
    }

    /**
     * Loads the friend list and adds them to the Adapter
     *
     * @param user    User to load the friend's list for
     */
    private void loadFriendsList(Author user) {

        // Get each friend from the friend list and add them to the Adapter
        for (String friendId : user.getFriends()) {
            FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, friendId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model == null) return;

                            mAdapter.addFriend((Author) model);
                        }
                    });
        }
    }
}
