package project.sherpa.models.viewmodels;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;
import project.sherpa.R;
import project.sherpa.models.datamodels.Message;
import project.sherpa.ui.fragments.MessageFragment;
import project.sherpa.utilities.DimensionUtils;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_MESSAGES;

/**
 * Created by Alvin on 9/14/2017.
 */

public class MessageViewModel extends BaseObservable {

    // ** Member Variables ** //
    private AppCompatActivity mActivity;
    private Message mMessage;
    private Message mPrevMessage;
    private Message mNextMessage;

    public MessageViewModel(AppCompatActivity activity, Message message, Message prevMessage) {
        mActivity = activity;
        mMessage = message;
        mPrevMessage = prevMessage;
    }

    public void setNextMessage(Message nextMessage) {
        mNextMessage = nextMessage;
    }

    @Bindable
    public String getMessage() {
        return mMessage.getMessage();
    }

    public void setMessage(String message) {
        mMessage.setMessage(message);
    }


    @Bindable
    public String getAuthorName() {
        return mMessage.getAuthorName();
    }

    @Bindable
    public StorageReference getAuthorImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(FirebaseProviderUtils.IMAGE_PATH)
                .child(mMessage.getAuthorId() + FirebaseProviderUtils.JPEG_EXT);
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
                            .placeholder(R.drawable.ic_account_circle)
                            .error(R.drawable.ic_account_circle)
                            .into(imageView);
                }
            }
        });
    }

    @Bindable
    public int getAuthorVisibility() {
        if (mNextMessage != null) {
            if (mNextMessage.getAuthorId().equals(mMessage.getAuthorId())) {
                return View.GONE;
            } else {
                return View.VISIBLE;
            }
        } else {
            return View.VISIBLE;
        }
    }

    @Bindable
    public int getAuthorImageVisibility() {
        if (mNextMessage != null) {
            if (mNextMessage.getAuthorId().equals(mMessage.getAuthorId())) {
                return View.INVISIBLE;
            } else {
                return View.VISIBLE;
            }
        } else {
            return View.VISIBLE;
        }
    }

    @Bindable
    public int getTopMargin() {

        // Get the number of dips to use for the top padding of the parent layout depending on
        // whether the previous message has the same author
        TypedValue dpValue = new TypedValue();

        int floatRes = mPrevMessage == null || mPrevMessage.getAuthorId().equals(mMessage.getAuthorId())
                ? R.dimen.message_contracted_top_padding
                : R.dimen.message_default_top_padding;

        mActivity.getResources().getValue(floatRes, dpValue, true);

        float dp = dpValue.getFloat();

        // Convert dips to pixels
        return (int) DimensionUtils.convertDpToPixel(mActivity, dp);
    }

    @BindingAdapter("topMargin")
    public static void setTopMargin(ConstraintLayout layout, int topMargin) {

        // Set the top margin for the layout
        ((RecyclerView.LayoutParams) layout.getLayoutParams()).topMargin = topMargin;
    }

    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {

            // When the user presses the IME option to send, the Message is sent
            MessageFragment fragment = (MessageFragment) mActivity.getSupportFragmentManager()
                    .findFragmentByTag(FRAG_TAG_MESSAGES);

            fragment.sendMessage();

            return true;
        }

        return false;
    }
}
