package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.hikerguide.R;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.ui.activities.UserActivity;
import project.hikerguide.ui.fragments.UserFragment;
import timber.log.Timber;

import static project.hikerguide.utilities.FirebaseProviderUtils.BACKDROP_SUFFIX;
import static project.hikerguide.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/23/2017.
 */

public class AuthorViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Author mAuthor;
    private Context mContext;
    private UserFragment mFragment;
    private int mEditVisibility = View.INVISIBLE;
    private boolean mAccepted = false;

    public AuthorViewModel(@NonNull Context context, @NonNull UserFragment fragment, @NonNull Author author) {
        mAuthor = author;
        mContext = context;
        mFragment = fragment;
    }

    public AuthorViewModel(@NonNull Context context, @NonNull Author author) {
        mContext = context;
        mAuthor = author;
    }

    @Bindable
    public Author getAuthor() {
        return mAuthor;
    }

    @Bindable
    public UserFragment getFragment() {

        return mFragment;
    }

    @Bindable
    public String getName() {
        return mAuthor.name;
    }

    @Bindable
    public StorageReference getImage() {
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mAuthor.firebaseId + JPEG_EXT);
    }

    @BindingAdapter("image")
    public static void loadImage(ImageView imageView, StorageReference image) {
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                .load(image)
                .into(imageView);
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
        return mContext.getString(R.string.list_author_format_score, mAuthor.score);
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
        mFragment.switchAuthorLayout();
    }

    public void onClickAccept(View view) {

        // Switch the variable to indicate that the user has clicked accept
        mAccepted = true;
        notifyPropertyChanged(BR.accepted);
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
