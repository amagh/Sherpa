package project.sherpa.ui.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import project.sherpa.R;
import project.sherpa.databinding.FragmentChatBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.activities.MessageActivity;
import project.sherpa.ui.adapters.ChatAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatFragment extends ConnectivityFragment {

    // ** Member Variables ** //
    private FragmentChatBinding mBinding;
    private Author mAuthor;
    private ChatAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);

        loadCurrentUser();

        initRecyclerView();

        loadChats();

        return mBinding.getRoot();
    }

    /**
     * Loads the current logged in Firebase User
     */
    private void loadCurrentUser() {

        // Get the User and load their profile from the DataCache
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        mAuthor = (Author) DataCache.getInstance().get(user.getUid());
    }

    /**
     * Initializes the RecyclerView and its required components
     */
    private void initRecyclerView() {

        mAdapter = new ChatAdapter((AppCompatActivity) getActivity(), new ClickHandler<Chat>() {
            @Override
            public void onClick(Chat clickedItem) {

                // Start the MessageActivity for the clicked Chat
                startMessageActivity(clickedItem.firebaseId);
            }
        });
        mBinding.chatRv.setAdapter(mAdapter);
        mBinding.chatRv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    /**
     * Loads all the chats that the user is involved in
     */
    private void loadChats() {

        FirebaseProviderUtils.getChatsForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
            @Override
            public void onModelReady(BaseModel model) {

                if (model == null) {

                    // User is not involved in any chats. Start a new Chat
                    addNewChat();
                    return;
                }

                // Load each chat into the DataCache and local database
                Chat chat = (Chat) model;
                DataCache.getInstance().store(chat);
                ContentProviderUtils.insertChat(getActivity(), chat);

                if (chat.getMembers().size() > 0) {

                    // Add the Chat to be displayed by the Adapter
                    mAdapter.addChat(chat);
                }
            }
        });
    }

    /**
     * Initiates a new Chat
     */
    public void addNewChat() {

        // Generate a new Chat and add the current user as a member of the Chat
        Chat chat = new Chat();
        chat.addMember(getActivity(), mAuthor.firebaseId);

        // Add the chat to the User and update it in Firebase
        mAuthor.addChat(chat.firebaseId);

        DataCache.getInstance().store(chat);

        // Start the MessageActivity for the Chat
        startMessageActivity(chat.firebaseId);
    }

    /**
     * Starts the MessageActivity for a specific Chat
     *
     * @param chatId    FirebaseId of the Chat to be initiated
     */
    private void startMessageActivity(String chatId) {
        Intent intent = new Intent(getActivity(), MessageActivity.class);
        intent.putExtra(CHAT_KEY, chatId);

        startActivity(intent);
    }
}
