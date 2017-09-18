package project.sherpa.models.viewmodels;

import android.database.Cursor;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
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
        StringBuilder builder = new StringBuilder();

        for (String member : mChat.getMembers()) {

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

            if (builder.length() == 0) {
                builder.append(member);
            } else {
                builder.append(", ");
                builder.append(member);
            }
        }

        return builder.toString();
    }

    @Bindable
    public String getLastMessage() {
        return mActivity.getString(
                R.string.chat_last_message_text,
                mChat.getLastAuthorName(),
                mChat.getLastMessage());
    }

    @Bindable
    public StorageReference getAuthorImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(FirebaseProviderUtils.IMAGE_PATH)
                .child(mChat.getLastAuthorId() + FirebaseProviderUtils.JPEG_EXT);
    }

    @BindingAdapter("authorImage")
    public static void loadAuthorImage(final CircleImageView imageView, final StorageReference authorImage) {

        if (authorImage == null) return;

        // Load the author's image from Firebase Storage
        authorImage.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                // Load from Firebase Storage
                if (imageView.getContext() != null) {
                    Glide.with(imageView.getContext())
                            .using(new FirebaseImageLoader())
                            .load(authorImage)
                            .signature(new StringSignature(storageMetadata.getMd5Hash()))
                            .error(R.drawable.ic_account_circle)
                            .into(imageView);
                }
            }
        });
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
                        "Can't add yourself to the chat",
                        Toast.LENGTH_SHORT)
                        .show();

                return;
            }
        }

        // Query Firebase to see if the user exists
        FirebaseProviderUtils.queryForUsername(mAddUsername, new FirebaseProviderUtils.FirebaseListener() {
            @Override
            public void onModelReady(BaseModel model) {

                Author author = (Author) model;

                if (author == null) {

                    // Inform the User that the selected User does not exist
                    Toast.makeText(
                            mActivity,
                            "User does not exist",
                            Toast.LENGTH_SHORT)
                            .show();

                    return;
                }

                // Get the Chat's list of Authors and check if any other Chats would have the same
                // members if the Author was added to it
                List<String> chatAuthorIds = new ArrayList<>(mChat.getMembers());
                chatAuthorIds.add(author.firebaseId);

                if (Chat.checkDuplicateChats(mActivity, chatAuthorIds) == null) {

                    // Add the member to the chat
                    mChat.addMember(mActivity, author.firebaseId);

                    // Add the Chat to the User's profile an update the local and Firebase Database
                    author.addChat(mChat.firebaseId);

                    setAddMember(false);
                }

            }
        });
    }

}
