package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.StorageReference;

import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Section;

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
    public StorageReference getImage() {
        // Generate StorageReference for the image using the Section's FirebaseId
        return mStorage.getReferenceForImage(mSection.firebaseId);
    }

    @BindingAdapter("bind:image")
    public static void loadImage(ImageView imageView, StorageReference image) {
        // Use FirebaseUI's API for Glide to load the image using the StorageReference
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                .load(image)
                .into(imageView);
    }

}
