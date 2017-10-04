package project.sherpa.ui.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

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
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.ui.activities.MessageActivity;
import project.sherpa.ui.activities.NewChatActivity;
import project.sherpa.ui.adapters.ChatAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.services.firebaseservice.FirebaseProviderService.*;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.CHAT;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatFragment extends ConnectivityFragment {

    // ** Constants ** //
    private static final int CHAT_LOADER = 5884;

    // ** Member Variables ** //
    private FragmentChatBinding mBinding;
    private Author mAuthor;
    private ChatAdapter mAdapter;

    private Map<String, ModelChangeListener> mListenerMap = new HashMap<>();
    private List<String> mAuthorIdList = new ArrayList<>();
    private Map<String, Chat> mDatabaseChatMap = new HashMap<>();

    private FirebaseProviderService mService;
    private boolean mBound;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FirebaseProviderBinder binder = (FirebaseProviderBinder) iBinder;
            mService = binder.getService();
            mBound = true;

            loadCurrentUser();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);
        initRecyclerView();
        return mBinding.getRoot();
    }

    /**
     * Loads the current logged in Firebase User
     */
    private void loadCurrentUser() {

        if (!mBound) return;

        // Get the User and load their profile from the DataCache
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            getActivity().finish();
            return;
        }

        // Initialize the ModelChangeListener for the logged in User
        ModelChangeListener<Author> modelChangeListener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
            @Override
            public void onModelReady(Author author) {

                // Set the field to the Author
                mAuthor = author;
                loadChats();
            }

            @Override
            public void onModelChanged() {
                if (mAuthor.getChats().size() < mAdapter.getItemCount()) {

                    // There are less Chats in the user's profile than in the Adapter. Clear the
                    // Adapter and re-load the Chats
                    mAdapter.clear();
                    unregisterChatListeners();

                }

                loadChats();
            }
        };

        mListenerMap.put(user.getUid(), modelChangeListener);
        mService.registerModelChangeListener(mListenerMap.get(user.getUid()));
    }

    /**
     * Sets ModelChangeListeners for each of the Chats that the user is a part of to notify the
     * user if they have a new message
     */
    private void loadChats() {

        if (!mBound) return;

        // Remove any Chats from the local database that have been deleted from the Firebase profile
        checkAndRemoveDeletedChats();

        // Register a ModelChangeListener for each Chat in the Author's List of Chats
        for (final String chatId : mAuthor.getChats()) {

            // Skip any Chats that already have ModelChangeListeners registered
            if (mListenerMap.get(chatId) != null) return;

            mListenerMap.put(chatId, new ModelChangeListener<Chat>(CHAT, chatId) {
                @Override
                public void onModelReady(Chat chat) {

                    if (chat == null) {

                        // Chat does not exist. Stop listening for changes
                        mService.unregisterModelChangeListener(this);
                        mListenerMap.remove(getFirebaseId());

                        mAuthor.removeChat(getActivity(), getFirebaseId());

                        return;
                    } else if (chat.getLastMessageId() == null) {

                        // Chat has no messages. Delete the Chat from Firebase.
                        FirebaseDatabase.getInstance().getReference()
                                .child(GuideDatabase.CHATS)
                                .child(chatId)
                                .removeValue();

                        mService.unregisterModelChangeListener(this);
                        mListenerMap.remove(chat.firebaseId);

                        return;
                    }

                    // Add the members of the Chat to the local database so their name can be
                    // stored
                    getChatMembers(chat);

                    // Check to see if there are any unread messages and set the layout to reflect
                    // it in the Adapter
                    mAdapter.setHasNewMessage(chat.firebaseId, chat.getNewMessageCount(getActivity()) > 0);
                }

                @Override
                public void onModelChanged() {

                    // Check to see if there are any unread messages and set the layout to reflect
                    // it in the Adapter
                    mAdapter.setHasNewMessage(
                            getModel().firebaseId,
                            getModel().getNewMessageCount(getActivity()) > 0);
                }
            });

            mService.registerModelChangeListener(mListenerMap.get(chatId));
        }
    }

    /**
     * Unregisters the ModelChangeListeners for the Chats and removes them from the Map so that
     * they can be registered again.
     */
    private void unregisterChatListeners() {

        // Iterate and unregister and remove each ModelChangeListener
        List<String> keyList = new ArrayList<>(mListenerMap.keySet());
        for (int i = keyList.size() - 1; i >= 0; i--) {
            String key = keyList.get(i);
            if (key.equals(mAuthor.firebaseId)) continue;

            ModelChangeListener listener = mListenerMap.get(key);
            mService.unregisterModelChangeListener(listener);
            mListenerMap.remove(key);
        }
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
    public void onResume() {
        super.onResume();

        // Start listening to changes in the ModelChangeListeners
        for (ModelChangeListener listener : mListenerMap.values()) {
            mAdapter.setHasNewMessage(listener.getFirebaseId(), false);
            mService.registerModelChangeListener(listener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop listening to changes in the ModelChangeListeners
        for (ModelChangeListener listener : mListenerMap.values()) {
            mService.unregisterModelChangeListener(listener);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(getActivity(), FirebaseProviderService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unbind the FirebaseProviderService
        getActivity().unbindService(mConnection);
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
                    AUTHOR,
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
}
