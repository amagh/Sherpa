package project.sherpa.ui.fragments;

import android.support.v7.widget.LinearLayoutManager;

import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.adapters.RequestAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.fragments.abstractfragments.BaseFriendFragment;
import project.sherpa.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 9/26/2017.
 */

public class RequestFragment extends BaseFriendFragment {

    // ** Member Variables ** //
    private RequestAdapter mAdapter;

    /**
     * Initializes the RecyclerView and its components
     */
    @Override
    protected void initRecyclerView() {
        mAdapter = new RequestAdapter(new ClickHandler<Author>() {
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
            loadReceivedRequests(mUser);
            loadSentRequests(mUser);
        }
    }

    /**
     * Loads the friend requests the user has received and adds them to the Adapter
     *
     * @param user    The user whose friend requests are to be added
     */
    private void loadReceivedRequests(Author user) {
        if (user.getReceivedRequests() == null) return;

        for (String requestId : user.getReceivedRequests()) {
            FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, requestId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model == null) return;

                            mAdapter.addReceivedRequest((Author) model);
                        }
                    });
        }
    }

    /**
     * Loads the friend requests the user has sent and adds them to the Adapter
     *
     * @param user    The user whose friend requests are to be added
     */
    private void loadSentRequests(Author user) {
        if (user.getSentRequests() == null) return;

        for (String requestId : user.getSentRequests()) {
            FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, requestId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model == null) return;

                            mAdapter.addSentRequest((Author) model);
                        }
                    });
        }
    }
}
