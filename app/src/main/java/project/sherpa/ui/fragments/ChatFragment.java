package project.sherpa.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.FragmentChatBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.activities.MessageActivity;
import project.sherpa.ui.activities.NewChatActivity;
import project.sherpa.ui.adapters.ChatAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatFragment extends ConnectivityFragment {

    // ** Member Variables ** //
    private FragmentChatBinding mBinding;
    private Author mAuthor;
    private ChatAdapter mAdapter;

    private Pair<DatabaseReference, ValueEventListener> mAuthorReferenceListenerPair;
    private Map<String, ChatValueEventListener> mEventListenerMap = new HashMap<>();
    private Set<String> mChatSet = new HashSet<>();
    private List<String> mAuthorIdList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);

        loadCurrentUser();

        initRecyclerView();

        return mBinding.getRoot();
    }

    /**
     * Loads the current logged in Firebase User
     */
    private void loadCurrentUser() {

        // Get the User and load their profile from the DataCache
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;

        final DatabaseReference authorRef = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(user.getUid());

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Author author = (Author) FirebaseProviderUtils.getModelFromSnapshot(
                        FirebaseProviderUtils.FirebaseType.AUTHOR,
                        dataSnapshot);

                if (author.getChats() == null) {

                    // Stop and remove all ChatValueEventListeners
                    for (ChatValueEventListener listener : mEventListenerMap.values()) {
                        listener.stop();
                    }

                    mEventListenerMap.clear();
                    mAdapter.clear();
                }

                // Remove any Chats that are no longer in the User's list of Chats
                if (mAuthor != null && mAuthor.getChats() != null && author.getChats() != null) {

                    // Check the new list of Chats against the List of Chats that the Author
                    // previously had and remove any Chats that aren't in the new List
                    for (String chatId : mAuthor.getChats()) {
                        if (!author.getChats().contains(chatId)) {
                            mAdapter.removeChat(chatId);
                            mEventListenerMap.remove(chatId);
                        }
                    }
                }

                mAuthor = author;

                loadChats();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mAuthorReferenceListenerPair  = new Pair<>(authorRef, listener);
    }

    /**
     * Initializes the RecyclerView and its required components
     */
    private void initRecyclerView() {

        mAdapter = new ChatAdapter((AppCompatActivity) getActivity(), new ClickHandler<Chat>() {
            @Override
            public void onClick(Chat clickedItem) {

                // Start the MessageActivity for the clicked Chat
                startMessageActivity(clickedItem);
            }
        });
        mBinding.chatRv.setAdapter(mAdapter);
        mBinding.chatRv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAuthorReferenceListenerPair != null) {
            mAuthorReferenceListenerPair.first.removeEventListener(mAuthorReferenceListenerPair.second);
        }

        if (mEventListenerMap != null) {
            for (ChatValueEventListener listener : mEventListenerMap.values()) {
                listener.stop();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAuthorReferenceListenerPair != null) {
            mAuthorReferenceListenerPair.first.addValueEventListener(mAuthorReferenceListenerPair.second);
        }

        if (mEventListenerMap != null) {
            for (ChatValueEventListener listener : mEventListenerMap.values()) {
                listener.start();
            }
        }
    }

    /**
     * Loads all the chats that the user is involved in
     */
    private void loadChats() {

        // Remove any Chats from the database that don't exist on the Firebase Database
        checkAndRemoveDeletedChats();

        if (mAuthor.getChats() == null) return;

        // Start a ValueEventListener for each Chat the user is involved in
        for (String chatId : mAuthor.getChats()) {

            // Do not add another Listener for items that already have a Listener attached to them
            if (mChatSet.contains(chatId)) return;

            ChatValueEventListener listener = new ChatValueEventListener(chatId);
            listener.start();

            // Add both to the Set so the ValueEventListener can be added and removed in
            // onStart/onPause
            mEventListenerMap.put(chatId, listener);
            mChatSet.add(chatId);
        }
    }

    /**
     * Checks the Firebase Database list of Chats for the User against the local database and
     * removes any Chats from the local database that do not exist on the Firebase Database
     */
    private void checkAndRemoveDeletedChats() {

        // Query the local database to get all ChatIds that exist in the local database
        Cursor cursor = getActivity().getContentResolver().query(
                GuideProvider.Chats.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Set<String> databaseChatSet = new HashSet<>();

            do {
                databaseChatSet.add(cursor.getString(1));
            } while (cursor.moveToNext());

            cursor.close();

            // Remove any ChatIds from the database Set that also exist in the Firebase Database
            if (mAuthor.getChats() != null && mAuthor.getChats().size() > 0) {
                for (String chatId : mAuthor.getChats()) {
                    databaseChatSet.remove(chatId);
                }
            }

            // Delete any Chats remaining in the Set and Messages associated with it
            if (databaseChatSet.size() > 0) {
                for (String chatId : databaseChatSet) {
                    getActivity().getContentResolver().delete(
                            GuideProvider.Chats.CONTENT_URI,
                            GuideContract.ChatEntry.FIREBASE_ID + " = ?",
                            new String[] {chatId});

                    getActivity().getContentResolver().delete(
                            GuideProvider.Messages.CONTENT_URI,
                            GuideContract.MessageEntry.CHAT_ID + " = ?",
                            new String[] {chatId});

                    mAuthor.removeChat(getActivity(), chatId);
                }
            }
        }
    }

    /**
     * Retrieves the members involved in a chat from Firebase
     *
     * @param chat    Chat whose members are to be retrieved
     */
    private void getChatMembers(final Chat chat) {

        // Iterate through the member list and retrieve each member
        for (String authorId : chat.getAllMembers()) {

            // Skip any members that have already been retrieved
            if (mAuthorIdList.contains(authorId)) {

                // Add the Chat to the Adapter
                mAdapter.addChat(chat);

                continue;
            }

            // Add the member to the List so that they aren't downloaded again
            mAuthorIdList.add(authorId);

            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.AUTHOR,
                    authorId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {

                            // Add the Author to the DataCache and the local database
                            Author author = (Author) model;

                            DataCache.getInstance().store(author);
                            ContentProviderUtils.insertModel(getActivity(), author);

                            // Add the Chat to the Adapter
                            mAdapter.addChat(chat);
                        }
                    }
            );
        }
    }

    /**
     * Initiates a new Chat
     */
    public void addNewChat() {

        Intent intent = new Intent(getActivity(), NewChatActivity.class);
        intent.putExtra(AUTHOR_KEY, mAuthor.firebaseId);

        DataCache.getInstance().store(mAuthor);

        startActivity(intent);
    }

    /**
     * Starts the MessageActivity for a specific Chat
     *
     * @param chat    Chat to be initiated
     */
    private void startMessageActivity(Chat chat) {
        Intent intent = new Intent(getActivity(), MessageActivity.class);
        intent.putExtra(CHAT_KEY, chat.firebaseId);

        // Store the Chat and Author
        DataCache.getInstance().store(chat);
        DataCache.getInstance().store(mAuthor);

        startActivity(intent);
    }

    private class ChatValueEventListener implements ValueEventListener {

        // ** Member Variables ** //
        private String chatId;
        private DatabaseReference reference;

        ChatValueEventListener(String chatId) {
            this.chatId = chatId;

            reference = FirebaseDatabase.getInstance().getReference()
                    .child(GuideDatabase.CHATS)
                    .child(chatId);
        }

        String getChatId() {
            return this.chatId;
        }

        void start() {
            reference.addValueEventListener(this);
        }

        void stop() {
            reference.removeEventListener(this);
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Chat chat = (Chat) FirebaseProviderUtils.getModelFromSnapshot(
                    FirebaseProviderUtils.FirebaseType.CHAT,
                    dataSnapshot);

            if (chat == null) {
                reference.removeEventListener(this);
                mAuthor.removeChat(getActivity(), this.chatId);

                mEventListenerMap.remove(this.chatId);
                return;
            }

            // Retrieve the members from each Chat
            if (chat.getActiveMembers().size() > 1 && chat.getLastMessageId() != null) {
                getChatMembers(chat);
                DataCache.getInstance().store(chat);

            } else {
                // Remove any Chats that do not have any members or any messages
                reference.removeValue();
                reference.removeEventListener(this);
                mAuthor.removeChat(getActivity(), chat.firebaseId);

                mEventListenerMap.remove(this.chatId);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
