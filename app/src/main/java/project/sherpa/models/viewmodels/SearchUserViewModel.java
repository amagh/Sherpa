package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;

import project.sherpa.BR;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.activities.NewChatActivity;

import static project.sherpa.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.sherpa.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 9/20/2017.
 */

public class SearchUserViewModel extends BaseObservable {

    private NewChatActivity mActivity;
    private Author mAuthor;
    private String mQuery;

    public SearchUserViewModel(NewChatActivity activity) {
        mActivity = activity;
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(final String query) {
        mQuery = query;

        // Query Firebase to see if there are any matching users
        if (mQuery.length() > 2) {
            mActivity.runQueryForUsername(query);
        }

        // Set Author so it cannot be selected
        mAuthor = null;

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
}
