package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.ref.WeakReference;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.hikerguide.R;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.ui.fragments.UserFragment;

import static project.hikerguide.utilities.FirebaseProviderUtils.BACKDROP_SUFFIX;
import static project.hikerguide.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.JPEG_EXT;
import static project.hikerguide.utilities.interfaces.FragmentTags.FRAG_TAG_ACCOUNT;

/**
 * Created by Alvin on 7/23/2017.
 */

public class AuthorViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Author mAuthor;
    private WeakReference<AppCompatActivity> mActivity;
    private int mEditVisibility = View.INVISIBLE;
    private boolean mAccepted = false;

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
        UserFragment fragment = (UserFragment) mActivity.get().getSupportFragmentManager()
                .findFragmentByTag(FRAG_TAG_ACCOUNT);

        return fragment;
    }

    @Bindable
    public String getName() {
        return mAuthor.name;
    }

    @Bindable
    public Uri getImage() {

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

    @Bindable
    public StorageReference getBackdrop() {
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mAuthor.firebaseId + BACKDROP_SUFFIX + JPEG_EXT);
    }

    @BindingAdapter("backdrop")
    public static void loadBackdrop(ImageView imageView, StorageReference backdrop) {
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                .load(backdrop)
                .into(imageView);
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
            author.name = nameEditText.getText().toString();
            author.description = descriptionEditText.getText().toString();

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
