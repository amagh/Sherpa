package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.StorageReference;

import project.hikerguide.R;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Guide;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Context mContext;
    private Guide mGuide;
    private StorageProvider mStorage;

    public GuideViewModel(Context context, Guide guide) {
        mContext = context;
        mGuide = guide;

        mStorage = StorageProvider.getInstance();
    }

    @Bindable
    public String getTrailName() {
        return mGuide.trailName;
    }

    @Bindable
    public String getAreaName() {
        return mGuide.area;
    }

    @Bindable
    public String getDistance() {
        return mContext.getString(R.string.list_guide_format_distance_imperial, mGuide.distance);
    }

    @Bindable
    public String getRating() {
        if (mGuide.reviews != 0) {
            return mContext.getString(R.string.list_guide_format_rating, mGuide.rating / mGuide.reviews);
        } else {
            return mContext.getString(R.string.list_guide_format_rating_zero);
        }
    }

    @Bindable
    public String getReviews() {
        return mContext.getString(R.string.list_guide_format_reviews, mGuide.reviews);
    }

    @Bindable
    public String getDifficulty() {
        return mGuide.difficulty;
    }

    @Bindable
    public StorageReference getImage() {
        return mStorage.getReferenceForImage(mGuide.firebaseId);
    }

    @BindingAdapter("bind:image")
    public static void loadImage(ImageView imageView, StorageReference image) {
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                .load(image)
                .into(imageView);
    }

    @Bindable
    public StorageReference getAuthorImage() {
        return mStorage.getReferenceForImage(mGuide.authorId);
    }

    @BindingAdapter("bind:authorImage")
    public static void loadAuthorImage(ImageView imageView, StorageReference authorImage) {
        Glide.with(imageView.getContext())
                .using(new FirebaseImageLoader())
                // Use default profile image if the author does not have one
                .load(authorImage)
                .error(R.drawable.ic_account_circle)
                .into(imageView);
    }

    @Bindable
    public String getAuthor() {
        return mGuide.authorName;
    }
}
