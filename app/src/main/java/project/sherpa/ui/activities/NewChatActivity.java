package project.sherpa.ui.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.databinding.ActivityNewChatBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.SearchUserViewModel;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.ui.activities.interfaces.SearchUserInterface;
import project.sherpa.ui.adapters.ChatAuthorAdapter;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;

/**
 * Created by Alvin on 9/19/2017.
 */

public class NewChatActivity extends ConnectivityActivity implements SearchUserInterface {

    // ** Constants ** //
    private static final int SEARCH_DELAY   = 750;

    // ** Member Variables ** //
    private Author mAuthor;

    private ActivityNewChatBinding mBinding;
    private SearchUserViewModel mViewModel;
    private ChatAuthorAdapter mAdapter;

    private Handler mHandler = new Handler();

    private Set<Author> mFriends = new HashSet<>();
    private Set<Author> mFriendsFiltered = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_new_chat);

        String authorId = getIntent().getStringExtra(AUTHOR_KEY);

        mAuthor = (Author) DataCache.getInstance().get(authorId);

        setupViewModel();
        initRecyclerView();
        loadFriends();
    }

    /**
     * Sets up the ViewModel that will be shared by the Activity's DataBinding and the corresponding
     * list item in the Adapter
     */
    private void setupViewModel() {
        mViewModel = new SearchUserViewModel(this);
        mBinding.searchUserLayout.setUvm(mViewModel);
    }

    /**
     * Sets up the RecyclerView and required components
     */
    private void initRecyclerView() {

        // Pass in the shared ViewModel
        mAdapter = new ChatAuthorAdapter(mViewModel);

        mBinding.searchUserLayout.searchUserRv.setLayoutManager(new LinearLayoutManager(this));
        mBinding.searchUserLayout.searchUserRv.setAdapter(mAdapter);
    }

    private void loadFriends() {

    }

    /**
     * Queries the Firebase Database for Authors that match the user's username query
     *
     * @param query    Username entered by the user
     */
    @Override
    public void runQueryForUsername(final String query) {

        // Set the Author to null so their information isn't showing when the query changes
        mViewModel.setAuthor(null);

        // Cancel any pending searches
        mHandler.removeCallbacksAndMessages(null);

        // Initiate a delayed search to allow the user time to finish typing
        mHandler.postDelayed(new Runnable() {
                 @Override
                 public void run() {

                     FirebaseProviderUtils.queryForUsername(query, new FirebaseProviderUtils.FirebaseListener() {
                         @Override
                         public void onModelReady(BaseModel model) {
                             if (model == null) return;

                             // Set the Author in the ViewModel to update its Views
                             Author author = (Author) model;
                             mViewModel.setAuthor(author);
                         }
                     });
                 }
             }, SEARCH_DELAY);

        // Filter the friend's list for any friends that match the query
        filter(query);
    }

    @Override
    public void resetAdapter() {

        // Cancel any pending searches
        mHandler.removeCallbacksAndMessages(null);

        // Set Author so it cannot be selected
        mViewModel.setAuthor(null);
    }

    /**
     * Click response for when the User selects the button to start a Chat
     *
     * @param view    View that was clicked
     */
    public void onClickStartChat(View view) {

        // List that will be set to active and all members of the Chat
        final List<String> selected = new ArrayList<>();

        // Add the user's FirebaseId as a member
        selected.add(mAuthor.firebaseId);

        // Add all the FirebaseIds of the selected user's to the Chat's list of members
        for (Author author : mAdapter.getSelected()) {
            selected.add(author.firebaseId);
        }

        // Check for duplicate Chats
        Chat.checkDuplicateChats(selected, new FirebaseProviderUtils.FirebaseListener() {
            @Override
            public void onModelReady(BaseModel model) {

                Chat chat;

                if (model == null) {

                    // No duplicate Chat, start a new Chat
                    chat = new Chat();
                    chat.generateFirebaseId();
                    chat.setActiveMembers(selected);
                    chat.setAllMembers(selected);
                    chat.setGroup(selected.size() > 2);
                } else {

                    // Get a reference to the duplicate Chat
                    chat = (Chat) model;
                }

                // Start the MessageActivity for the Chat
                startMessageActivityForChat(chat);
            }
        });
    }

    /**
     * Starts the MessageActivity for a Chat
     *
     * @param chat    Chat to start the MessageActivity for
     */
    private void startMessageActivityForChat(Chat chat) {

        // Build the Intent and launch the Activity
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra(CHAT_KEY, chat.firebaseId);

        DataCache.getInstance().store(chat);

        startActivity(intent);

        // Remove this Activity from the back stack
        finish();
    }

    /**
     * Filters the user's friend's list for users that match the query
     *
     * @param query    Query entered by the user
     */
    private void filter(String query) {

    }
}
