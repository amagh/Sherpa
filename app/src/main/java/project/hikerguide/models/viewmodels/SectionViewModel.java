package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
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

import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Section;

import static project.hikerguide.utilities.StorageProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/23/2017.
 */

public class SectionViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Section mSection;
    private StorageProvider mStorage;

    public SectionViewModel(Section section) {
        mSection = section;

        mStorage = StorageProvider.getInstance();
    }

    @Bindable
    public String getContent() {
        return mSection.content;
    }

    @Bindable
    public void setContent(String content) {
        mSection.content = content;

        notifyPropertyChanged(BR.content);
    }

    @Bindable
    public int getContentVisibility() {
        return mSection.content != null ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public Uri getImageUri() {
        return mSection.getImageUri();
    }

    @Bindable
    public StorageReference getImage() {

        if (mSection.firebaseId == null) return null;

        // Generate StorageReference for the image using the Section's FirebaseId
        return FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mSection.firebaseId + JPEG_EXT);
    }

    @BindingAdapter({"bind:image", "bind:imageUri"})
    public static void loadImage(ImageView imageView, StorageReference image, Uri imageUri) {

        System.out.println(imageUri);

        // Check whether to load image from File or from Firebase Storage
        if (image == null) {

            // No StorageReference, load local file using the File's Uri
            Glide.with(imageView.getContext())
                    .load(imageUri)
                    .into(imageView);
        } else {

            // Load from Firebase Storage
            Glide.with(imageView.getContext())
                    .using(new FirebaseImageLoader())
                    .load(image)
                    .into(imageView);
        }
    }

    @Bindable
    public int getImeAction() {
        if (mSection.hasImage) {
            return EditorInfo.IME_ACTION_DONE;
        } else {
            return EditorInfo.IME_ACTION_NEXT;
        }
    }

    @BindingAdapter("bind:imeAction")
    public static void setupEditText(EditText editText, int imeAction) {

        // Work around for multiline EditText with IME option
        editText.setImeOptions(imeAction);
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
    }

}
