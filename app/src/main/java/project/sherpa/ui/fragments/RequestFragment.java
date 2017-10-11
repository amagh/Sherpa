package project.sherpa.ui.fragments;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;

import java.sql.Time;
import java.util.List;

import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.activities.UserActivity;
import project.sherpa.ui.adapters.RequestAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.fragments.abstractfragments.BaseFriendFragment;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;

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
            loadReceivedRequests();
            loadSentRequests();
        } else {
            updateReceivedRequests();
            updateSentRequests();
        }
    }

    /**
     * Loads the friend requests the user has received and adds them to the Adapter
     */
    private void loadReceivedRequests() {
        if (mUser.getReceivedRequests() == null) {
            hideProgressBar();
            return;
        }

        Timber.d("Loading received requests");

        for (String requestId : mUser.getReceivedRequests()) {
            addReceivedRequestToAdapter(requestId);
        }
    }

    /**
     * Loads the friend requests the user has sent and adds them to the Adapter
     */
    private void loadSentRequests() {
        if (mUser.getSentRequests() == null) {
            hideProgressBar();
            return;
        }

        Timber.d("Loading sent requests");

        for (String requestId : mUser.getSentRequests()) {
            addSentRequestToAdapter(requestId);
        }
    }

    /**
     * Updates the Adapter by removing any received requests that are no longer valid and adding
     * any new received requests
     */
    private void updateReceivedRequests() {
        Timber.d("Updating received requests adapter");
        if (mUser.getReceivedRequests() == null) {
            mAdapter.clearReceivedRequests();
            Timber.d("Clearing received request adapter");
            return;
        }

        List<String> adapterIdList = mAdapter.getReceivedFirebaseIds();

        // Remove any received that are no longer there
        for (String adapterUserId : adapterIdList) {
            if (!mUser.getReceivedRequests().contains(adapterUserId)) {
                Timber.d("Removing " + adapterUserId + " from adapter");
                mAdapter.removeReceivedRequest(adapterUserId);
            }
        }

        // Add new received requests
        for (String receivedRequest : mUser.getReceivedRequests()) {
            if (!adapterIdList.contains(receivedRequest)) {
                Timber.d("Adding " + receivedRequest + " to adapter");
                addReceivedRequestToAdapter(receivedRequest);
            }
        }
    }

    /**
     * Updates the Adapter by removing any sent requests that are no longer valid and adding any
     * new sent requests
     */
    private void updateSentRequests() {
        Timber.d("Updating sent requests adapter");
        if (mUser.getSentRequests() == null) {
            mAdapter.clearSentRequests();
            Timber.d("Clearing sent requests adapter");
            return;
        }

        List<String> adapterIdList = mAdapter.getSentFirebaseIds();

        // Remove any sent requests that are no longer there
        for (String adapterUserId : adapterIdList) {
            if (!mUser.getSentRequests().contains(adapterUserId)) {
                Timber.d("Removing " + adapterUserId + " from adapter");
                mAdapter.removeSentRequest(adapterUserId);
            }
        }

        // Add new sent requests
        for (String sentRequest : mUser.getSentRequests()) {
            if (!adapterIdList.contains(sentRequest)) {
                Timber.d("Adding " + sentRequest + " to adapter");
                addSentRequestToAdapter(sentRequest);
            }
        }
    }

    /**
     * Downloads the user profile of a user that has sent a request to mUser
     *
     * @param requestId    FirebaseId of the received request
     */
    private void addReceivedRequestToAdapter(String requestId) {
        FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, requestId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        hideProgressBar();
                        if (model == null) return;
                        mAdapter.addReceivedRequest((Author) model);
                    }
                });
    }

    /**
     * Downloads the user profile of a user that mUser has sent a request to
     *
     * @param requestId    FirebaseId of the sent request
     */
    private void addSentRequestToAdapter(String requestId) {
        FirebaseProviderUtils.getModel(FirebaseProviderUtils.FirebaseType.AUTHOR, requestId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        hideProgressBar();
                        if (model == null) return;
                        mAdapter.addSentRequest((Author) model);
                    }
                });
    }
}
