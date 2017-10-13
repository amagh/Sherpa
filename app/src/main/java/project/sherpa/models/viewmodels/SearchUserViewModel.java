package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;

import project.sherpa.BR;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.activities.NewChatActivity;
import project.sherpa.ui.activities.interfaces.SearchUserInterface;

import static project.sherpa.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.sherpa.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 9/20/2017.
 */

public class SearchUserViewModel extends BaseObservable {

    private SearchUserInterface mSearchInterface;
    private Author mAuthor;
    private String mQuery;
    private boolean mShowProgress;

    public SearchUserViewModel(SearchUserInterface searchInterface) {
        mSearchInterface = searchInterface;
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(final String query) {
        mQuery = query;

        // Query Firebase to see if there are any matching users
        mSearchInterface.runQueryForUsername(query);

        notifyPropertyChanged(BR.query);
    }

    @Bindable
    public Author getAuthor() {
        return mAuthor;
    }

    public void setAuthor(Author author) {
        mAuthor = author;
        notifyPropertyChanged(BR.author);
        notifyPropertyChanged(BR.name);
        notifyPropertyChanged(BR.authorImage);
    }

    @BindingAdapter("author")
    public static void getAuthorVisibility(TextView textView, Author author) {

        // Set the Visibility of the TextView for the author's name based on whether there is a
        // corresponding author for the entered username query
        if (author == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
        }
    }

    @Bindable
    public String getName() {
        if (mAuthor != null) {
            return mAuthor.name;
        } else {
            return null;
        }
    }

    @Bindable
    public Uri getAuthorImage() {
        if (mAuthor == null) {
            return null;
        }

        // Return the Uri for the StorageReference for the author's image
        return Uri.parse(FirebaseStorage.getInstance().getReference()
                .child(IMAGE_PATH)
                .child(mAuthor.firebaseId + JPEG_EXT)
                .toString());
    }

    @Bindable
    public boolean getShowProgress() {
        return mShowProgress;
    }

    @BindingAdapter({"searchUserIv", "showProgress"})
    public static void setShowProgressBar(ProgressBar searchUserPb, ImageView searchUserIv,
                                          boolean showProgress) {

        if (showProgress) {
            searchUserPb.setVisibility(View.VISIBLE);
            searchUserIv.setVisibility(View.INVISIBLE);
        } else {
            searchUserPb.setVisibility(View.INVISIBLE);
            searchUserIv.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Resets the ViewModel to accept a new query
     */
    public void reset() {
        mQuery = null;
        mAuthor = null;

        notifyPropertyChanged(BR.query);
        notifyPropertyChanged(BR.author);
        notifyPropertyChanged(BR.authorImage);
    }

    public void showProgress() {
        mShowProgress = true;
        notifyPropertyChanged(BR.showProgress);
    }

    public void hideProgress() {
        mShowProgress = false;
        notifyPropertyChanged(BR.showProgress);
    }
}
