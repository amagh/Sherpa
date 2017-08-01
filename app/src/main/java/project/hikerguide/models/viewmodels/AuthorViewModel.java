package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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

    public void setName(String name) {
        mAuthor.name = name;
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

    public void setDescription(String description) {
        mAuthor.description = description;
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
    public int getImeAction() {
        return EditorInfo.IME_ACTION_DONE;
    }

    @BindingAdapter({"bind:imeAction"})
    public static void setupEditText(EditText editText, int imeAction) {

        // Work around for multiline EditText with IME option
        editText.setImeOptions(imeAction);
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
    }

    public void onClickEdit(View view) {

    }

    public void onClickAccept(View view) {

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
