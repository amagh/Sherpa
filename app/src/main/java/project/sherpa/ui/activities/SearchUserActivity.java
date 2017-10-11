package project.sherpa.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.databinding.ActivitySearchUserBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.SearchUserViewModel;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.ui.activities.interfaces.SearchUserInterface;
import project.sherpa.ui.adapters.FriendAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.services.firebaseservice.SmartValueEventListener;
import project.sherpa.services.firebaseservice.FirebaseProviderService.FirebaseProviderBinder;

import static project.sherpa.ui.activities.SearchUserActivity.SearchTypes.FOLLOW;
import static project.sherpa.ui.activities.SearchUserActivity.SearchTypes.FRIEND;
import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.SEARCH_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;

/**
 * Created by Alvin on 9/29/2017.
 */

public class SearchUserActivity extends ConnectivityActivity implements SearchUserInterface {

    // ** Constants ** //
    private static final int SEARCH_DELAY = 750;

    public interface SearchTypes {
        int FOLLOW = 0;
        int FRIEND = 1;
    }

    // ** Member Variables ** //
    private ActivitySearchUserBinding mBinding;
    private Author mUser;
    private FriendAdapter mAdapter;
    private Handler mSearchHandler = new Handler();
    private List<Author> mResultList;
    private int mSearchType;
    ModelChangeListener<Author> mUserListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_search_user);
        bindFirebaseProviderService(true);

        mSearchType = getIntent().getIntExtra(SEARCH_KEY, FOLLOW);

        initRecyclerView();
        initViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUserListener != null) mService.registerModelChangeListener(mUserListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mUserListener != null) mService.unregisterModelChangeListener(mUserListener);
    }

    @Override
    protected void onServiceConnected() {
        loadCurrentUser();
    }

    /**
     * Init the Adapter and its components
     */
    private void initRecyclerView() {
        mAdapter = new FriendAdapter(new ClickHandler<Author>() {
            @Override
            public void onClick(Author clickedItem) {

                // Start the UserActivity for the clicked user to allow the user to confirm this
                // is the person they are trying to connect with
                Intent intent = new Intent(SearchUserActivity.this, UserActivity.class);
                intent.putExtra(AUTHOR_KEY, clickedItem.firebaseId);

                startActivity(intent);
            }
        });
        mAdapter.showAddSocialButton(true);

        mBinding.searchUserLayout.searchUserRv.setLayoutManager(new LinearLayoutManager(this));
        mBinding.searchUserLayout.searchUserRv.setAdapter(mAdapter);
    }

    /**
     * Init the SearchUserViewModel that will be used to search for user's by username
     */
    private void initViewModel() {
        SearchUserViewModel uvm = new SearchUserViewModel(this);
        mBinding.searchUserLayout.setUvm(uvm);
    }

    /**
     * Sets a listener for the current user's profile on Firebase
     */
    private void loadCurrentUser() {

        // Confirm the user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        mUserListener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
            @Override
            public void onModelReady(Author model) {
                mUser = model;
            }

            @Override
            public void onModelChanged() {

                // Update the Adapter with the changes in the logged in user
                removeExistingFromResults(mResultList, mUser.getFriends());

                if (mSearchType == FOLLOW) {
                    removeExistingFromResults(mResultList, mUser.getFollowing());
                } else {
                    removeExistingFromResults(mResultList, mUser.getSentRequests());
                }

                mAdapter.setFriendList(mResultList);
            }
        };

        mService.registerModelChangeListener(mUserListener);
    }

    /**
     * Queries Firebase Database for users with a similar username to the one input by the user and
     * displays them in the Adapter for the user to select
     *
     * @param username    Username query input by the user
     */
    @Override
    public void runQueryForUsername(final String username) {

        if (username.length() <= 2) {
            resetAdapter();
            return;
        }

        // Cancel any queries queued to be run
        mSearchHandler.removeCallbacksAndMessages(null);

        // Generate the Query based on the username
        final Query usernameQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .orderByChild(GuideContract.AuthorEntry.LOWER_CASE_USERNAME)
                .startAt(username)
                .endAt(username + "\uf8ff");

        // Run the query after a short delay to allow the user time to finish typing
        mSearchHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                usernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (!dataSnapshot.exists()) return;

                        Author[] users =
                                (Author[]) FirebaseProviderUtils.getModelsFromSnapshot(AUTHOR, dataSnapshot);

                        if (mUser == null) return;

                        // Convert the Array to a List so elements can be removed
                        mResultList = new ArrayList<>(Arrays.asList(users));

                        // Remove items from resultList that are already in the user's friend/follow list
                        removeExistingFromResults(mResultList, mUser.getFriends());

                        if (mSearchType == FOLLOW) {
                            removeExistingFromResults(mResultList, mUser.getFollowing());
                        } else {
                            removeExistingFromResults(mResultList, mUser.getSentRequests());
                        }

                        mAdapter.setFriendList(mResultList);

                        usernameQuery.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        usernameQuery.removeEventListener(this);
                    }
                });
            }
        }, SEARCH_DELAY);
    }

    /**
     * Removes items from the result list that have a FirebaseId contained in the existingList.
     * This removes followers/friends from the List to be displayed in the Adapter because there is
     * they cannot friend/follow someone that they are already friends with/follow.
     *
     * @param resultList      The List containing Authors returned from the search query
     * @param existingList    The List of FirebaseIds of users in the friend/following list
     */
    private static void removeExistingFromResults(List<Author> resultList, List<String> existingList) {

        // Check that existingList is not null
        if (existingList == null) return;

        // Remove items from resultList that are already in the user's friend/follow list
        for (String userId : existingList) {
            for (int i = resultList.size() - 1; i >= 0; i--) {
                Author user = resultList.get(i);

                if (user.firebaseId.equals(userId)) {
                    resultList.remove(user);
                    break;
                }
            }
        }
    }

    /**
     * Resets the Adapter to its starting conditions
     */
    public void resetAdapter() {

        // Cancel any pending searches
        mSearchHandler.removeCallbacksAndMessages(null);

        // Clear the Adapter
        mAdapter.clear();
    }

    /**
     * Depending on the SearchType passed to this Activity, it either follows a user or sends a
     * friend request.
     *
     * @param user    User to follow/friend
     */
    public void followOrFriendUser(Author user) {

        switch (mSearchType) {
            case FOLLOW:
                mUser.addUserToList(Author.AuthorLists.FOLLOWING, user.firebaseId);
                user.addUserToList(Author.AuthorLists.FOLLOWERS, mUser.firebaseId);

                break;

            case FRIEND:
                mUser.addUserToList(Author.AuthorLists.SENT_REQUESTS, user.firebaseId);
                user.addUserToList(Author.AuthorLists.RECEIVED_REQUESTS, mUser.firebaseId);

                break;
        }
    }
}
