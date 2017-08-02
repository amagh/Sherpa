package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import project.hikerguide.R;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.ui.activities.UserActivity;

import static project.hikerguide.utilities.StorageProviderUtils.BACKDROP_SUFFIX;
import static project.hikerguide.utilities.StorageProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/23/2017.
 */

public class AuthorViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Author mAuthor;
    private Context mContext;
    private int mEditVisibility = View.INVISIBLE;
    private boolean mAccepted = false;

    public AuthorViewModel(Context context, Author author) {
        mAuthor = author;
        mContext = context;
    }

    @Bindable
    public Author getAuthor() {
        return mAuthor;
    }

    @Bindable
    public UserActivity getActivity() {

        // Check that the passed Context is instance of UserActivity
        if (mContext instanceof UserActivity) {

            // Cast and return mContext
            return (UserActivity) mContext;
        } else {
            return null;
        }
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

    @BindingAdapter("bind:image")
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

    @BindingAdapter("bind:backdrop")
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

    @BindingAdapter("bind:editVisibility")
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

    @BindingAdapter({"bind:nameTv", "bind:descriptionTv", "bind:author", "bind:activity", "bind:accepted"})
    public static void saveInfo(Button button, EditText nameEditText, EditText descriptionEditText,
                                Author author, UserActivity activity, boolean accepted) {

        // Check to see that the accept Button has been clicked as this function runs the first
        // time the ViewModel is loaded as well
        if (activity != null && accepted) {
            // Set the Author parameters to match the text that the user has altered
            author.name = nameEditText.getText().toString();
            author.description = descriptionEditText.getText().toString();

            // Update the Author's values in the Firebase Database and switch the layout
            activity.updateAuthorValues();
            activity.switchAuthorLayout();
        }
    }

    public void onClickEdit(View view) {

        // Switch the layout between edit and display
        if (mContext instanceof UserActivity) {
            ((UserActivity) mContext).switchAuthorLayout();
        }
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
    }

}
