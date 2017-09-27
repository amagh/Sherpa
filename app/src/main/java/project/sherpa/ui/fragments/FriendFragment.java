package project.sherpa.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FriendFragment extends ConnectivityFragment {

    // ** Member Variables ** //
    private FragmentFriendBinding mBinding;
    private FriendAdapter mAdapter;
    private Author mUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_friend, container, false);

        initRecyclerView();
        loadUser();

        return mBinding.getRoot();
    }

    /**
     * Initializes the RecyclerView and its components
     */
    private void initRecyclerView() {
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
    private void loadUser() {

        // Attempt to retrieve the user from the cache
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        mUser = (Author) DataCache.getInstance().get(user.getUid());

        if (mUser != null) {

            // Load the friends list for this user
            loadFriendsList(mUser);
        } else {

            // User's profile was not stored in cache. Load it from Firebase
            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {
                    if (model == null) return;

                    // Store the user in cache and recursively call the loadUser function
                    DataCache.getInstance().store(model);
                    loadUser();
                }
            });
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
