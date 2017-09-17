package project.sherpa.ui.fragments;

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import project.sherpa.R;
import project.sherpa.data.GuideDatabase;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.FragmentMessageBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.Message;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.ChatViewModel;
import project.sherpa.models.viewmodels.MessageViewModel;
import project.sherpa.ui.adapters.MessageAdapter;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.CHAT;

/**
 * Created by Alvin on 9/14/2017.
 */

public class MessageFragment extends ConnectivityFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // ** Constants ** //
    private static final int MESSAGE_LOADER = 3531;

    // ** Member Variables ** //
    private FragmentMessageBinding mBinding;
    private MessageAdapter mAdapter;

    private Chat mChat;
    private Message mMessage;
    private Author mAuthor;

    private ChatViewModel mChatViewModel;

    private DatabaseReference mChatReference;
    private ValueEventListener mMessageListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if (dataSnapshot.exists()) {
                Chat chat = (Chat) FirebaseProviderUtils.getModelFromSnapshot(CHAT, dataSnapshot);

                if (chat != null && chat.getMessageCount() > mChat.getMessageCount()) {

                    // Retrieve the number of new messages that the current chat does not contain
                    getMessages(chat.getMessageCount() - mChat.getMessageCount());
                }


                // Re-reference the member field to the new Chat and cache it
                mChat = chat;
                DataCache.getInstance().store(mChat);

                // Update the database entry for the Chat
                ContentProviderUtils.insertChat(getActivity(), mChat);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    /**
     * Factory pattern for instantiating MessageFragment
     *
     * @param chatId    FirebaseId for the Chat to load Messages for
     * @return MessageFragment set to load the Messages corresponding to the chatId
     */
    public static MessageFragment newInstance(@NonNull String chatId) {

        // Generate the Uri to access the Messages for the chat from the database
        Uri chatMessagesUri = GuideProvider.Messages.forChat(chatId);

        // Pass the Uri to the new Fragment in a Bundle
        Bundle args = new Bundle();
        args.putParcelable(CHAT_KEY, chatMessagesUri);

        MessageFragment fragment = new MessageFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_message, container, false);

        initRecyclerView();

        // Retrieve the Bundle containing the Uri
        Bundle args = getArguments();
        if (args != null) {

            // Retrieve the FirebaseId of the Chat to retrieve messages for
            String chatId = GuideProvider.getIdFromUri((Uri) args.getParcelable(CHAT_KEY));

            mChat = (Chat) DataCache.getInstance().get(chatId);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                mAuthor = (Author) DataCache.getInstance().get(user.getUid());
                setMessageBinding();
            }

            // Download all the Users involved in the chat to the database
            downloadUsersForChat(mChat);

            // Start the Loader and pass in the Bundle
            getActivity().getSupportLoaderManager().initLoader(MESSAGE_LOADER, args, this);
        }

        // Bind the Views related to the Chat
        setChatBinding();

        // If the device user is the only member of the chat, then set up the
        if (mChat.getMembers().size() == 1) {
            mChatViewModel.setAddMember(true);
        }

        return mBinding.getRoot();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Get the Uri for the messages corresponding to a chatId
        Uri chatMessagesUri = args.getParcelable(CHAT_KEY);

        return new CursorLoader(getActivity(), chatMessagesUri, Message.PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            do {
                Message message = Message.createMessageFromCursor(data);
                mAdapter.addMessage(message);
            } while (data.moveToNext());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onStart() {
        super.onStart();

        setMessageListener();
    }

    @Override
    public void onPause() {
        super.onPause();

        removeMessageListener();
    }

    private void initRecyclerView() {
        mAdapter = new MessageAdapter(getActivity());
        mBinding.messageRv.setAdapter(mAdapter);
        mBinding.messageRv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    /**
     * Sets a ValueListener on the DatabaseReference for the chat to listen for new messages
     */
    private void setMessageListener() {

        if (mChatReference == null) {
            mChatReference = FirebaseDatabase.getInstance().getReference()
                    .child(GuideDatabase.CHATS)
                    .child(mChat.firebaseId);
        }

        mChatReference.addValueEventListener(mMessageListener);
    }

    /**
     * Removes the ValueListener from the DatabaseReference for the chat
     */
    private void removeMessageListener() {

        if (mChatReference == null) return;

        mChatReference.removeEventListener(mMessageListener);
    }

    /**
     * Retrieves new messages from FirebaseDatabase for the
     *
     * @param numMessages    The number of messages to be downloaded from Firebase Database
     */
    private void getMessages(int numMessages) {

        // Query Firebase for new messages
        Query query = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.MESSAGES)
                .child(mChat.firebaseId)
                .orderByChild(Message.DATE)
                .limitToLast(numMessages);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {

                    // Get the new messages from Firebase
                    Message[] messages = (Message[]) FirebaseProviderUtils.getModelsFromSnapshot(
                            FirebaseProviderUtils.FirebaseType.MESSAGE,
                            dataSnapshot);

                    // Insert the Message into the local database
                    ContentProviderUtils.bulkInsertMessages(getActivity(), messages);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Sets up the Views related to the Chat
     */
    private void setChatBinding() {
        if (mChatViewModel == null) {
            mChatViewModel = new ChatViewModel((AppCompatActivity) getActivity(), mChat);
        }

        mBinding.setChat(mChatViewModel);
    }

    /**
     * Sets a new Message to the ViewDataBinding for the user to add a message and send to Firebase
     */
    private void setMessageBinding() {

        // Init a new Message and set the initial parameters
        mMessage = new Message();

        mMessage.setAuthorId(mAuthor.firebaseId);
        mMessage.setAuthorName(mAuthor.name);
        mMessage.setChatId(mChat.firebaseId);

        // Set the message to the ViewModel
        MessageViewModel vm = new MessageViewModel((AppCompatActivity) getActivity(), mMessage, false);
        mBinding.setVm(vm);
    }

    /**
     * Uploads the message the User composed to Firebase Database
     */
    public void sendMessage() {

        mMessage.send(getActivity())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // Add the message to the local database
                        mChat.updateChatWithNewMessage(mMessage);

                        mMessage.setStatus(0);
                        ContentProviderUtils.insertModel(getActivity(), mMessage);

                        setMessageBinding();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Timber.e("Failed to send message: " + e.getMessage());

                        mMessage.setStatus(1);
                        ContentProviderUtils.insertModel(getActivity(), mMessage);

                        setMessageBinding();
                    }
                });
    }

    /**
     * Downloads the profile of all Authors in the chat
     *
     * @param chat    The Chat containing members
     */
    private void downloadUsersForChat(Chat chat) {

        // Iterate through each member and download their profile from Firebase
        for (String authorId : chat.getMembers()) {
            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.AUTHOR,
                    authorId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model != null) {
                                ContentProviderUtils.insertModel(getActivity(), model);
                            }
                        }
                    }
            );
        }

    }
}
