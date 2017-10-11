package project.sherpa.models.viewmodels;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.lang.ref.WeakReference;

import project.sherpa.R;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.fragments.UserFragment;
import project.sherpa.utilities.Constants;
import project.sherpa.utilities.FirebaseProviderUtils;

import static project.sherpa.utilities.FirebaseProviderUtils.BACKDROP_SUFFIX;
import static project.sherpa.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.sherpa.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/23/2017.
 */

public class AuthorViewModel extends BaseObservable {

    // ** Member Variables ** //
    private Author mAuthor;
    private WeakReference<AppCompatActivity> mActivity;
    private boolean mSelected = false;

    public AuthorViewModel(@NonNull AppCompatActivity activity, @NonNull Author author) {
        mAuthor = author;
        mActivity = new WeakReference<>(activity);
    }

    @Bindable
    public Author getAuthor() {
        return mAuthor;
    }

    @Bindable
    public UserFragment getFragment() {

        if (mActivity.get() == null) return null;

        // Retrieve the Fragment using the FragmentManager
        return (UserFragment) mActivity.get().getSupportFragmentManager()
                .findFragmentByTag(Constants.FragmentTags.FRAG_TAG_USER);
    }

    @Bindable
    public String getName() {
        return mAuthor.name;
    }

    @Bindable
    public String getUsername() {
        return mAuthor.getUsername();
    }

    @Bindable
    public Uri getAuthorImage() {

        if (mSelected || !mAuthor.hasImage) {
            return null;
        }

        // Check whether the Author has a Uri for an offline ImageUri
        if (mAuthor.getImageUri() != null) {

            // Return the ImageUri
            return mAuthor.getImageUri();
        } else {

            // Parse the StorageReference to a Uri
            return Uri.parse(FirebaseStorage.getInstance().getReference()
                    .child(IMAGE_PATH)
                    .child(mAuthor.firebaseId + JPEG_EXT).toString());
        }
    }

    @BindingAdapter("authorImage")
    public static void loadImage(final ImageView imageView, final Uri authorImage) {

        if (authorImage == null) {
            imageView.setImageDrawable(
                    ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_account_circle));

            return;
        }

        // Check whether to load image from File or from Firebase Storage
        if (authorImage.getScheme().matches("gs")) {
            StorageReference authorRef = FirebaseProviderUtils.getReferenceFromUri(authorImage);

            if (authorRef == null) return;

            authorRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    // Load from Firebase Storage
                    if (imageView.getContext() != null && !((Activity) imageView.getContext()).isFinishing()) {
                        Glide.with(imageView.getContext())
                                .using(new FirebaseImageLoader())
                                .load(FirebaseProviderUtils.getReferenceFromUri(authorImage))
                                .signature(new StringSignature(storageMetadata.getMd5Hash()))
                                .error(R.drawable.ic_account_circle)
                                .into(imageView);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    imageView.setImageDrawable(
                            ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_account_circle));
                }
            });

        } else {
            // No StorageReference, load local file using the File's Uri
            Glide.with(imageView.getContext())
                    .load(authorImage)
                    .error(R.drawable.ic_account_circle)
                    .into(imageView);
        }
    }

    @Bindable
    public StorageReference getBackdrop() {

        if (!mAuthor.isHasBackdrop()) return null;
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mAuthor.firebaseId + BACKDROP_SUFFIX + JPEG_EXT);
    }

    @BindingAdapter("backdrop")
    public static void loadBackdrop(final ImageView imageView, final StorageReference backdrop) {

        if (backdrop == null) return;

        backdrop.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {

                // Check to ensure Activity is still active prior to loading image
                if (imageView.getContext() == null) return;

                Glide.with(imageView.getContext())
                        .using(new FirebaseImageLoader())
                        .load(backdrop)
                        .signature(new StringSignature(storageMetadata.getMd5Hash()))
                        .into(imageView);
            }
        });
    }

    @Bindable
    public String getScore() {
        return mActivity.get().getString(R.string.list_author_format_score, mAuthor.score);
    }

    @Bindable
    public String getDescription() {
        return mAuthor.description;
    }

    @Bindable
    public boolean getSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;

        notifyPropertyChanged(BR.selected);
    }

    @BindingAdapter({"imageView", "authorImage", "selected"})
    public static void setSelectedBackground(ConstraintLayout layout, ImageView imageView, Uri authorImage, boolean selected) {
        layout.setSelected(selected);

        if (selected) {
            imageView.setImageDrawable(ContextCompat.getDrawable(imageView.getContext(), R.drawable.chat_selected_user));
        } else {
            loadImage(imageView, authorImage);
        }
    }
}
