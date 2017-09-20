package project.sherpa.models.viewmodels;

import android.database.Cursor;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideProvider;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.fragments.MessageFragment;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_MESSAGES;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatViewModel extends BaseObservable {

    // ** Member Variables ** //
    private AppCompatActivity mActivity;
    private Chat mChat;
    private String mAddUsername;
    private boolean mAddMember;

    public ChatViewModel(AppCompatActivity appCompatActivity, Chat chat) {
        mActivity = appCompatActivity;
        mChat = chat;
    }

    @Bindable
    public String getMembers() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        StringBuilder builder = new StringBuilder();

        // Add the name of each user in the chat
        for (String member : mChat.getMembers()) {

            // Do not display the user's own name
            if (user!= null && user.getUid().equals(member)) {
                continue;
            }

            Cursor cursor = mActivity.getContentResolver().query(
                    GuideProvider.Authors.CONTENT_URI,
                    null,
                    GuideContract.AuthorEntry.FIREBASE_ID + " = ?",
                    new String[]{member},
                    null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    member = Author.createAuthorFromCursor(cursor).name;
                }

                cursor.close();
            }

            if (builder.length() > 0) {

                // Add comma separator
                builder.append(", ");
            }

            builder.append(member);
        }

        return builder.toString();
    }

    @Bindable
    public String getLastMessage() {

        // Set the text to display as the last message as "Attachment" if the last message contained
        // an attachment.
        String message = mChat.getLastMessage() == null || mChat.getLastMessage().isEmpty()
                ? mActivity.getString(R.string.chat_attachment_text)
                : mChat.getLastMessage();

        return mActivity.getString(
                R.string.chat_last_message_text,
                mChat.getLastAuthorName(),
                message);
    }

    @Bindable
    public StorageReference getAuthorImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(FirebaseProviderUtils.IMAGE_PATH)
                .child(mChat.getLastAuthorId() + FirebaseProviderUtils.JPEG_EXT);
    }

    @Bindable
    public boolean getAddMember() {
        return mAddMember;
    }

    public void setAddMember(boolean addMember) {
        mAddMember = addMember;

        notifyPropertyChanged(BR.addMember);
        notifyPropertyChanged(BR.addMemberVisibility);
    }

    @Bindable
    public int getAddMemberVisibility() {

        return getAddMember() || mChat.getMembers().size() < 1
                ? View.VISIBLE
                : View.GONE;
    }

    @Bindable
    public String getAddUsername() {
        return mAddUsername;
    }

    public void setAddUsername(String username) {
        mAddUsername = username;
    }

    /**
     * Click response for the button for adding new Users to the Chat
     *
     * @param view    View that was clicked
     */
    public void onClickAddUser(View view) {

        // Check to ensure the user is not adding themselves to the Chat
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            Author currentUser = (Author) DataCache.getInstance().get(user.getUid());

            if (currentUser.getUsername().toLowerCase().equals(mAddUsername.toLowerCase().trim())) {
                Toast.makeText(
                        mActivity,
                        mActivity.getString(R.string.toast_error_add_self),
                        Toast.LENGTH_SHORT)
                        .show();

                return;
            }
        }

        // Query Firebase to see if the user exists
        FirebaseProviderUtils.queryForUsername(mAddUsername, new FirebaseProviderUtils.FirebaseListener() {
            @Override
            public void onModelReady(BaseModel model) {

                final Author author = (Author) model;

                if (author == null) {

                    // Inform the User that the selected User does not exist
                    Toast.makeText(
                            mActivity,
                            mActivity.getString(R.string.toast_error_user_does_not_exist),
                            Toast.LENGTH_SHORT)
                            .show();

                    return;
                }

                // Get the Chat's list of Authors and check if any other Chats would have the same
                // members if the Author was added to it
                List<String> chatAuthorIds = new ArrayList<>(mChat.getMembers());
                chatAuthorIds.add(author.firebaseId);

                Chat.checkDuplicateChats(chatAuthorIds, new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        if (model == null) {
                            // Add the member to the chat
                            mChat.addMember(mActivity, author.firebaseId);

                            // Add the Chat to the User's profile an update the local and Firebase Database
                            author.addChat(mActivity, mChat.firebaseId);

                        } else {
                            Chat chat = (Chat) model;

                            if (!author.getChats().contains(mChat.firebaseId)) {

                                // Add the chat to the member if it is not in the Author's list of
                                // Chats
                                author.addChat(mActivity, mChat.firebaseId);
                            }

                            MessageFragment fragment = (MessageFragment) mActivity.getSupportFragmentManager()
                                    .findFragmentByTag(FRAG_TAG_MESSAGES);

                            fragment.setChat(chat);

                            Timber.d("Found duplicate Chat: " + chat.firebaseId);
                        }

                        setAddMember(false);
                    }
                });
            }
        });
    }

    /**
     * Click response for the cancel icon when add a user to the chat
     *
     * @param view    View that was clicked
     */
    public void onClickCancel(View view) {

        if (mAddUsername == null || mAddUsername.isEmpty()) {
            // Hide the view for adding a user
            setAddMember(false);
        } else {
            // Clear the entered text
            setAddUsername(null);
            notifyPropertyChanged(BR.addUsername);
        }
    }
}
