package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import project.hikerguide.R;
import project.hikerguide.models.datamodels.Author;

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

    public AuthorViewModel(Context context, Author author) {
        mAuthor = author;
        mContext = context;
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

    public void onClickEdit(View view) {

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
