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
import java.util.HashSet;
import java.util.List;
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

    private List<Pair<DatabaseReference, ValueEventListener>> mReferenceListenerPairList;
    private List<String> mAuthorIdList = new ArrayList<>();

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
                startMessageActivity(clickedItem);
            }
        });
        mBinding.chatRv.setAdapter(mAdapter);
        mBinding.chatRv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mReferenceListenerPairList != null) {
            for (Pair<DatabaseReference, ValueEventListener> pair : mReferenceListenerPairList) {
                pair.first.removeEventListener(pair.second);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mReferenceListenerPairList != null) {
            for (Pair<DatabaseReference, ValueEventListener> pair : mReferenceListenerPairList) {
                pair.first.addValueEventListener(pair.second);
            }
        }
    }

    /**
     * Loads all the chats that the user is involved in
     */
    private void loadChats() {

        // Remove any Chats from the database that don't exist on the Firebase Database
        checkAndRemoveDeletedChats();

        // Start a new chat if there are no chats
        if (mAuthor.getChats() == null || mAuthor.getChats().size() == 0) {
            addNewChat();
            return;
        }

        // Start a ValueEventListener for each Chat the user is involved in
        for (String chatId : mAuthor.getChats()) {

            final DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                    .child(GuideDatabase.CHATS)
                    .child(chatId);

            ValueEventListener listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Chat chat = (Chat) FirebaseProviderUtils.getModelFromSnapshot(
                                    FirebaseProviderUtils.FirebaseType.CHAT,
                                    dataSnapshot);

                            if (chat == null) return;

                            // Retrieve the members from each Chat
                            if (chat.getMembers().size() > 1) {
                                getChatMembers(chat);
                                DataCache.getInstance().store(chat);

                            } else {
                                // Remove any Chats that do not have any members
                                reference.removeValue();
                                reference.removeEventListener(this);

                                mAuthor.removeChat(getActivity(), chat.firebaseId);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };

            // Init the List
            if (mReferenceListenerPairList == null) mReferenceListenerPairList = new ArrayList<>();

            // Add both to the List so the ValueEventListener can be added and removed in
            // onStart/onPause
            mReferenceListenerPairList.add(new Pair<>(reference, listener));
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

            // Delete any Chats remaining in the Set
            if (databaseChatSet.size() > 0) {
                for (String chatId : databaseChatSet) {
                    getActivity().getContentResolver().delete(
                            GuideProvider.Chats.CONTENT_URI,
                            GuideContract.ChatEntry.FIREBASE_ID + " = ?",
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
        for (String authorId : chat.getMembers()) {

            // Skip any members that have already been retrieved
            if (mAuthorIdList.contains(authorId)) continue;

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

        // Generate a new Chat and add the current user as a member of the Chat
        Chat chat = new Chat();
        chat.addMember(getActivity(), mAuthor.firebaseId);

        // Add the chat to the User and update it in Firebase
        mAuthor.addChat(getActivity(), chat.firebaseId);

        // Start the MessageActivity for the Chat
        startMessageActivity(chat);
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
