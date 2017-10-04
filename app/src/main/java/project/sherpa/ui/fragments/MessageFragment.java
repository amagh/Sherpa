package project.sherpa.ui.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import project.sherpa.R;
import project.sherpa.data.GuideDatabase;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.FragmentMessageBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Message;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.ChatViewModel;
import project.sherpa.models.viewmodels.MessageViewModel;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.ui.activities.AttachActivity;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.ui.activities.MessageActivity;
import project.sherpa.ui.adapters.MessageAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.services.firebaseservice.SmartValueEventListener;
import project.sherpa.services.firebaseservice.FirebaseProviderService.*;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static project.sherpa.models.datamodels.Message.ATTACHMENT_TYPE;
import static project.sherpa.models.datamodels.Message.AttachmentType.GUIDE_TYPE;
import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_ATTACH_GUIDE;
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

    private ModelChangeListener<Chat> mChatListener;
    private boolean mBound;
    private FirebaseProviderService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            // Bind to the Service
            FirebaseProviderBinder binder = (FirebaseProviderBinder) iBinder;
            mService = binder.getService();
            mBound = true;

            Bundle args = getArguments();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (args == null || user == null) {
                getActivity().finish();
                return;
            }

            // Set the Author as the logged in Author
            mAuthor = (Author) DataCache.getInstance().get(user.getUid());

            // Start the Chat
            String chatId = GuideProvider.getIdFromUri((Uri) args.getParcelable(CHAT_KEY));
            setChatListener(chatId);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
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

        setHasOptionsMenu(true);
        initRecyclerView();

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Options to add to the group and leave the Chat should only be available in a group Chat
        if (mChat.getGroup()) {
            inflater.inflate(R.menu.menu_message, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_add_user:
                mChatViewModel.setAddMember(true);
                return true;

            case R.id.menu_leave_chat:
                mAuthor.removeChat(getActivity(), mChat.firebaseId);

                if (getActivity() instanceof MessageActivity) {
                    getActivity().finish();
                }

                return true;
        }

        return false;
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

        // Start listening for changes in data
        if (!mBound) {
            Intent intent = new Intent(getActivity(), FirebaseProviderService.class);
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        if (mChatListener != null) mService.registerModelChangeListener(mChatListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop listening for changes
        if (mChatListener != null) mService.unregisterModelChangeListener(mChatListener);
        if (mBound) getActivity().unbindService(mConnection);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CODE_ATTACH_GUIDE:
                if (resultCode == RESULT_OK) {

                    // Get the selected guide for attachment
                    String guideId = data.getStringExtra(GUIDE_KEY);
                    if (guideId == null) return;

                    // Attach the Guide and send the message
                    attachGuideToMessage(guideId);
                }

                break;
        }
    }

    /**
     * Initializes the RecyclerView and its components
     */
    private void initRecyclerView() {
        mAdapter = new MessageAdapter(getActivity(), new ClickHandler<Guide>() {
            @Override
            public void onClick(Guide guide) {

                // Start the Activity to show the Guide details for the clicked Guide
                startGuideDetailsActivity(guide);
            }
        });
        mBinding.messageRv.setAdapter(mAdapter);

        // Init the LayoutManager in reverse order
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        mBinding.messageRv.setLayoutManager(layoutManager);
    }

    /**
     * Sets the ModelChangeListener for the Chat.
     *
     * @param chatId    The FirebaseId of the Chat to set a ModelChangeListener for
     */
    private void setChatListener(String chatId) {
        mChatListener = new ModelChangeListener<Chat>(CHAT, chatId) {
            @Override
            public void onModelReady(Chat chat) {
                if (chat == null) return;

                // Set the Chat for the Fragment
                startChat(chat);
            }

            @Override
            public void onModelChanged() {

                // Retrieve new messages from Firebase
                getMessages(getModel().getNewMessageCount(getActivity()));

                // Update the database entry for the Chat
                ContentProviderUtils.insertModel(getActivity(), getModel());
            }
        };

        mService.registerModelChangeListener(mChatListener);
    }

    /**
     * Retrieves new messages from FirebaseDatabase for the
     *
     * @param numMessages    The number of messages to be downloaded from Firebase Database
     */
    private void getMessages(int numMessages) {

        // Check to ensure there are messages to retrieve before retrieving them
        if (numMessages <= 0) return;

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
     * Loads any new messages that have been added since the user last checked their chat
     */
    private void getNewMessagesSinceLastChat() {

        // Query the local database to see how many messages the last Chat accounted for
        Cursor cursor = getActivity().getContentResolver().query(
                GuideProvider.Chats.byId(mChat.firebaseId),
                null,
                null,
                null,
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Chat chat = Chat.createChatFromCursor(cursor);

                // Load the difference in number of messages between the updated chat and last
                // checked chat
                getMessages(mChat.getMessageCount() - chat.getMessageCount());
            } else {

                // New chat - get all messages
                getMessages(mChat.getMessageCount());
            }

            cursor.close();
        } else {
            // New chat - get all messages
            getMessages(mChat.getMessageCount());
        }
    }

    private void startChat(Chat chat) {
        setChat(chat);
        setMessageBinding();
    }

    /**
     * Sets the Chat to be used for this Fragment to retrieve messages for
     *
     * @param chat    The Chat to be set
     */
    public void setChat(Chat chat) {

        getActivity().invalidateOptionsMenu();
        mAdapter.clear();

        // Add the Chat to the Author if it's not included in it
        mAuthor.addChat(getActivity(), chat.firebaseId);

        // Start the Loader and pass in the Bundle
        Bundle args = new Bundle();
        args.putParcelable(CHAT_KEY, GuideProvider.Messages.forChat(chat.firebaseId));

        // Load messages from the database
        getActivity().getSupportLoaderManager().restartLoader(MESSAGE_LOADER, args, this);

        mChat = chat;
        getNewMessagesSinceLastChat();
        setChatBinding();

//        // Start listening for messages
//        setMessageListener();

        setActionBar();
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

    private void setActionBar() {
        ((AppCompatActivity) getActivity()).setSupportActionBar(mBinding.messageTb);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mChatViewModel.getMembers());
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
        MessageViewModel vm = new MessageViewModel((AppCompatActivity) getActivity(), mMessage, null);
        mBinding.setVm(vm);
    }

    /**
     * Starts the AttachActivity to allow for the user to select a Guide to attach to the message
     */
    public void startActivityToAttachGuide() {
        Intent intent = new Intent(getActivity(), AttachActivity.class);
        intent.putExtra(ATTACHMENT_TYPE, GUIDE_TYPE);

        startActivityForResult(intent, REQUEST_CODE_ATTACH_GUIDE);
    }

    /**
     * Start Activity to show guide details for clicking an attachment guide
     *
     * @param guide    Guide in the attachment that was clicked
     */
    private void startGuideDetailsActivity(Guide guide) {
        Intent intent = new Intent(getActivity(), GuideDetailsActivity.class);
        intent.putExtra(GUIDE_KEY, guide.firebaseId);

        DataCache.getInstance().store(guide);

        startActivity(intent);
    }

    /**
     * Attaches a Guide to a message and sends it
     *
     * @param guideId    The FirebaseId of the Guide to attach
     */
    public void attachGuideToMessage(String guideId) {
        mMessage.attachGuide(guideId);
        sendMessage();
    }

    /**
     * Uploads the message the User composed to Firebase Database
     */
    public void sendMessage() {

        // Check to ensure there is a message to send
        if (mMessage.getAttachment() == null
                && (mMessage.getMessage() == null || mMessage.getMessage().isEmpty())) return;

        mMessage.send(getActivity())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        boolean newChat = false;

                        if (mChat.getMessageCount() == 0) {
                            newChat = true;
                        }

                        // Update the Chat's last message fields on Firebase
                        mChat.updateChatWithNewMessage(mMessage);

                        if (newChat) {
                            // Add the Chat to each member of the Chat so that they receive the
                            // message
                            addChatToMembers();
                        }

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
     * Adds the Chat to the List of Chats for all members in the Chat. To be used if the Chat is
     * new and the members haven't been updated with the Chat yet.
     */
    private void addChatToMembers() {

        // Iterate through each member and add the Chat to their profile
        for (String authorId : mChat.getActiveMembers()) {
            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.AUTHOR,
                    authorId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            if (model != null) {
                                Author author = (Author) model;
                                author.addChat(getActivity(), mChat.firebaseId);
                            }
                        }
                    }
            );
        }

    }
}
