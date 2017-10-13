package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;

import project.sherpa.R;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.fragments.UserFragment;
import project.sherpa.utilities.FirebaseProviderUtils;

import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_USER;

/**
 * Created by Alvin on 10/2/2017.
 */

public class ListItemAuthorDetailsEditViewModel extends BaseObservable {

    // ** Member Variables ** //
    private Author mAuthor;
    private boolean mAccepted;
    private AppCompatActivity mActivity;

    public ListItemAuthorDetailsEditViewModel(AppCompatActivity activity, Author author) {
        mActivity = activity;
        mAuthor = author;
    }

    public UserFragment getFragment() {
        return (UserFragment) mActivity.getSupportFragmentManager().findFragmentByTag(FRAG_TAG_USER);
    }

    public Author getAuthor() {
        return mAuthor;
    }

    @Bindable
    public boolean getAccepted() {
        return mAccepted;
    }

    @BindingAdapter({"nameTv", "descriptionTv", "author", "fragment", "accepted"})
    public static void saveInfo(Button button, EditText nameEditText, EditText descriptionEditText,
                                Author author, UserFragment fragment, boolean accepted) {

        // Check to see that the accept Button has been clicked as this function runs the first
        // time the ViewModel is loaded as well
        if (fragment != null && accepted) {
            // Set the Author parameters to match the text that the user has altered
            author.name = nameEditText.getText().toString().trim();
            author.description = descriptionEditText.getText().toString().trim();

            // Check to ensure the entered name is not blank
            if (author.name.isEmpty()) {

                // Show Toast to user to instruct them to enter a name
                Toast.makeText(
                        nameEditText.getContext(),
                        nameEditText.getContext().getString(R.string.author_name_empty_error_message),
                        Toast.LENGTH_LONG)
                        .show();

                return;
            }

            // Update the Author's values in the Firebase Database and switch the layout
            FirebaseProviderUtils.insertOrUpdateModel(author);
            fragment.switchAuthorLayout();
        }
    }

    public void onClickEdit(View view) {

        // Switch the layout between edit and display
        getFragment().switchAuthorLayout();
    }

    public void onClickAccept(View view) {

        // Switch the variable to indicate that the user has clicked accept
        mAccepted = true;
        notifyPropertyChanged(BR.accepted);
    }
}
