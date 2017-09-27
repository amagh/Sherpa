package project.sherpa.ui.fragments;

import android.support.v7.widget.LinearLayoutManager;

import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.adapters.FriendAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.fragments.abstractfragments.BaseFriendFragment;
import project.sherpa.utilities.FirebaseProviderUtils;

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
            loadFollowingList(mUser);
        }
    }

    /**
     * Loads the user's list of users that they are following and adds them to the Adapter
     *
     * @param user    User to retrieve the follow list from
     */
    private void loadFollowingList(Author user) {

        // Get each user from the user's list of people they are following and add them to the
        // Adapter
        for (String userId : user.getFollowing()) {
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
}
