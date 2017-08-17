package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.support.v4.view.MotionEventCompat;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import project.hikerguide.models.datamodels.Section;
import project.hikerguide.ui.activities.CreateGuideActivity;
import timber.log.Timber;

import static project.hikerguide.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/23/2017.
 */

public class SectionViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Section mSection;
    private CreateGuideActivity mActivity;

    public SectionViewModel(Section section) {
        mSection = section;
    }

    public SectionViewModel(CreateGuideActivity activity, Section section) {
        mActivity = activity;
        mSection = section;
    }

    @Bindable
    public String getContent() {
        return mSection.content;
    }

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
    public Uri getImage() {

        if (mSection.firebaseId == null) {

            // Return the ImageUri
            return mSection.getImageUri();
        } else {

            // Parse the StorageReference to a Uri
            return Uri.parse(FirebaseStorage.getInstance().getReference()
                    .child(IMAGE_PATH)
                    .child(mSection.firebaseId + JPEG_EXT).toString());
        }
    }

    @Bindable
    public int getImeAction() {
        return EditorInfo.IME_ACTION_DONE;
    }

    @BindingAdapter({"imeAction"})
    public static void setupEditText(EditText editText, int imeAction) {

        // Work around for multiline EditText with IME option
        editText.setImeOptions(imeAction);
        editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
    }

    /**
     * Triggers the re-placement of the photo for the Section
     *
     * @param view    The ImageView of the photo
     */
    public void onSectionImageClick(View view) {

        // Trigger the replacement of the image
        if (mActivity != null) {
            mActivity.onSectionImageClick(mSection);
        }
    }

    /**
     * Starts re-ordering of the selected Model within the Adapter
     *
     * @param view     The ImageView with the touch pad
     * @param event    The type of MotionEvent triggered
     * @return True if the Touch is handled. False otherwise.
     */
    public boolean onReorderTouch(View view, MotionEvent event) {

        // Check the MotionEvent is a hold
        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
            if (mActivity != null) {

                // Trigger the re-ordering of the Model within the Adapter
                mActivity.reorderModel(mSection);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes the selected Model from the Adapter
     *
     * @param view    The ImageView with the trash icon
     */
    public void onDeleteClick(View view) {

        // Remove the Model
        if (mActivity != null) {
            mActivity.removeModel(mSection);
        }
    }
}
