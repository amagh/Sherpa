package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.v7.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;
import project.sherpa.R;
import project.sherpa.models.datamodels.Chat;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatViewModel extends BaseObservable {

    // ** Member Variables ** //
    private AppCompatActivity mActivity;
    private Chat mChat;

    @Bindable
    public String getMembers() {
        StringBuilder builder = new StringBuilder();

        for (String member : mChat.getMembers()) {
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
}
