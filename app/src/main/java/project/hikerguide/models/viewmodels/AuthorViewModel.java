package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.StorageReference;

import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Author;

/**
 * Created by Alvin on 7/23/2017.
 */

public class AuthorViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Author mAuthor;
    private StorageProvider mStorage;

    public AuthorViewModel(Author author) {
        mAuthor = author;

        mStorage = StorageProvider.getInstance();
    }

    @Bindable
    public String getName() {
        return mAuthor.name;
    }

    @Bindable
    public StorageReference getImage() {
        return mStorage.getReferenceForImage(mAuthor.firebaseId);
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
        return Integer.toString(mAuthor.score);
    }
}
