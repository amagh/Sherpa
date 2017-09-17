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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;
import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideProvider;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.FirebaseProviderUtils;

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

                // Add the user to the Chat
                mChat.addMember(mActivity, author.firebaseId);

                // Add the Chat to the User's profile an update the local and Firebase Database
                author.addChat(mChat.firebaseId);

                setAddMember(false);
            }
        });
    }
}
