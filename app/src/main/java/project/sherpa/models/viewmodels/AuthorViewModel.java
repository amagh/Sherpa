package project.sherpa.models.viewmodels;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.lang.ref.WeakReference;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import droidninja.filepicker.FilePickerConst;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.sherpa.R;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.activities.ChatActivity;
import project.sherpa.ui.activities.FriendActivity;
import project.sherpa.ui.activities.FriendFollowActivity;
import project.sherpa.ui.behaviors.VanishingBehavior;
import project.sherpa.ui.fragments.UserFragment;
import project.sherpa.utilities.Constants;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.utilities.GeneralUtils;

import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_BACKDROP;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_PROFILE_PIC;
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
    private int mEditVisibility = View.INVISIBLE;
    private boolean mAccepted = false;
    private boolean mSelected = false;
    private boolean mEditMode = false;

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

        if (mSelected) {
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
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mAuthor.firebaseId + BACKDROP_SUFFIX + JPEG_EXT);
    }

    @BindingAdapter("backdrop")
    public static void loadBackdrop(final ImageView imageView, final StorageReference backdrop) {

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
    public int getEditVisibility() {
        return mEditVisibility;
    }

    @BindingAdapter("editVisibility")
    public static void setEditVisibility(ImageView imageView, int editVisibility) {

        // Enable/disable click on ImageView depending on whether use is viewing their own profile.
        // The edit button is only visible when user is viewing their own profile.
        if (editVisibility == View.INVISIBLE) {
            imageView.setClickable(false);
        } else {
            imageView.setClickable(true);
        }

        // Set visibility of the edit button
        imageView.setVisibility(editVisibility);
    }

    @Bindable
    public boolean getAccepted() {
        return mAccepted;
    }

    @BindingAdapter({"nameTv", "descriptionTv", "author", "fragment", "accepted"})
    public static void saveInfo(Button button, EditText nameEditText, EditText descriptionEditText,
                                Author author, UserFragment fragment, boolean accepted) {

        // Check to see that the accept Button has been clicked as this function runs the first
        // time the ViewModel is loaded as well
        if (fragment != null && accepted) {
            // Set the Author parameters to match the text that the user has altered
            author.name = nameEditText.getText().toString().trim();
            author.description = descriptionEditText.getText().toString().trim();

            // Check to ensure the entered name is not blank
            if (author.name.isEmpty()) {

                // Show Toast to user to instruct them to enter a name
                Toast.makeText(
                        nameEditText.getContext(),
                        nameEditText.getContext().getString(R.string.author_name_empty_error_message),
                        Toast.LENGTH_LONG)
                        .show();

                return;
            }

            // Update the Author's values in the Firebase Database and switch the layout
            fragment.updateAuthorValues();
            fragment.switchAuthorLayout();
        }
    }

    @Bindable
    public int getFabVisibility() {
        if (mEditVisibility == View.INVISIBLE) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @BindingAdapter("fabVisibility")
    public static void setFabVisibility(FabSpeedDial fab, int fabVisibility) {

        // Set the Visibility of the FAB
        fab.setVisibility(fabVisibility);
    }

    @Bindable
    public int getMessageVisibility() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        return user != null
                ? View.VISIBLE
                : View.GONE;
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

    @Bindable
    public boolean getInEditMode() {
        return mEditMode;
    }

    public void setInEditMode(boolean isInEditMode) {
        mEditMode = isInEditMode;

        notifyPropertyChanged(BR.inEditMode);
    }

    @BindingAdapter({"messageIv", "inEditMode"})
    public static void animateSocialVisibility(final ImageView friendIv, final ImageView messageIv, boolean inEditMode) {

        // Get the parameters for the parent ViewGroup so that the Behavior can be modified
        ConstraintLayout layout = (ConstraintLayout) friendIv.getParent();
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layout.getLayoutParams();

        if (inEditMode) {
            if (friendIv.getAlpha() == 0) {

                // Views are already hidden. No need to animate. Just set visibility to GONE
                friendIv.setVisibility(View.GONE);
                messageIv.setVisibility(View.GONE);
                return;
            }

            // Disable Behavior so animation does not clash
            params.setBehavior(null);

            // Hide the social buttons as they do not need to be visible when editing profile
            new AdditiveAnimator().setDuration(300)
                    .target(friendIv).scale(0).alpha(0)
                    .target(messageIv).scale(0).alpha(0)
                    .addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            // Set the visibility to GONE
                            friendIv.setVisibility(View.GONE);
                            messageIv.setVisibility(View.GONE);

                            // Reset the layout Behavior so that the profile image still animates
                            params.setBehavior(new VanishingBehavior());
                        }
                    })
                    .start();

        } else {

            if (friendIv.getAlpha() == 0) {

                // Views are invisible, so just set visibility to VISIBLE
                friendIv.setVisibility(View.VISIBLE);
                messageIv.setVisibility(View.VISIBLE);
                return;
            }

            // Disable Behavior so animation does not clash
            params.setBehavior(null);

            // Hide the Views temporarily
            new AdditiveAnimator().setDuration(0)
                    .target(friendIv).scale(0).alpha(0)
                    .target(messageIv).scale(0).alpha(0)
                    .start();

            // Animate the Views in
            new AdditiveAnimator().setDuration(300)
                    .target(friendIv).scale(1).alpha(1)
                    .target(messageIv).scale(1).alpha(1)
                    .addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);

                            // Set visibility to VISIBLE on animation start
                            friendIv.setVisibility(View.VISIBLE);
                            messageIv.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            // Reset the Behavior so it acts normally
                            params.setBehavior(new VanishingBehavior());
                        }
                    })
                    .start();
        }
    }

    public void onClickEdit(View view) {

        // Switch the layout between edit and display
        getFragment().switchAuthorLayout();
    }

    public void onClickAccept(View view) {

        // Switch the variable to indicate that the user has clicked accept
        mAccepted = true;
        notifyPropertyChanged(BR.accepted);
    }

    public void onClickBackdrop(View view) {

        // Check to ensure the user is clicking their own backdrop image
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(mAuthor.firebaseId) || !mEditMode) {
            return;
        }

        // Open FilePicker for selecting backdrop image
        GeneralUtils.openFilePicker(
                getFragment(),
                REQUEST_CODE_BACKDROP,
                FilePickerConst.FILE_TYPE_MEDIA);
    }

    public void onClickProfileImage(View view) {

        // Check to ensure the user is clicking their own profile image
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(mAuthor.firebaseId) || !mEditMode) {
            return;
        }

        // Open FilePicker for selecting profile image
        GeneralUtils.openFilePicker(
                getFragment(),
                REQUEST_CODE_PROFILE_PIC,
                FilePickerConst.FILE_TYPE_MEDIA);
    }

    /**
     * Click response for the messaging button in the UserFragment
     *
     * @param view    View that was clicked
     */
    public void onClickMessage(View view) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid().equals(mAuthor.firebaseId)) {
            Intent intent = new Intent(mActivity.get(), ChatActivity.class);
            getFragment().startActivity(intent);
        } else {
            getFragment().startMessageActivityToUser();
        }
    }

    public void onClickFriend(View view) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid().equals(mAuthor.firebaseId)) {
            Intent intent = new Intent(mActivity.get(), FriendActivity.class);
            getFragment().startActivity(intent);
        } else {
            getFragment().startFriendFollowActivity();
        }
    }

    /**
     * Enables editing of the information in the author's profile
     */
    public void enableEditing() {

        // Set the edit button to visible
        mEditVisibility = View.VISIBLE;

        notifyPropertyChanged(BR.editVisibility);
        notifyPropertyChanged(BR.fabVisibility);
    }

}
