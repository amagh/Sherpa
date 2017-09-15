package project.sherpa.models.viewmodels;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.util.TypedValue;

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
import project.sherpa.utilities.DimensionUtils;
import project.sherpa.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 9/14/2017.
 */

public class MessageViewModel extends BaseObservable {

    // ** Member Variables ** //
    private Activity mActivity;
    private Message mMessage;
    private boolean mSameAuthor;

    public MessageViewModel(Activity activity, Message message, boolean sameAuthor) {
        mActivity = activity;
        mMessage = message;
        mSameAuthor = sameAuthor;
    }

    @Bindable
    public String getMessage() {
        return mMessage.getMessage();
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
                            .error(R.drawable.ic_account_circle)
                            .into(imageView);
                }
            }
        });
    }

    @Bindable
    public float getTopPadding() {

        // Get the number of dips to use for the top padding of the parent layout depending on
        // whether the previous message has the same author
        TypedValue dpValue = new TypedValue();

        int floatRes = mSameAuthor
                ? R.dimen.message_contracted_top_padding
                : R.dimen.message_default_top_padding;

        mActivity.getResources().getValue(floatRes, dpValue, true);

        float dp = dpValue.getFloat();

        // Convert dips to pixels
        return DimensionUtils.convertDpToPixel(mActivity, dp);
    }
}
