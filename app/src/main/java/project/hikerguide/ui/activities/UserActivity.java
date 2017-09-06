package project.hikerguide.ui.activities;

import android.os.Bundle;

import project.hikerguide.R;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.fragments.UserFragment;
import project.hikerguide.utilities.DataCache;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.Constants.FragmentTags.FRAG_TAG_ACCOUNT;
import static project.hikerguide.utilities.Constants.IntentKeys.AUTHOR_KEY;

/**
 * Created by Alvin on 7/31/2017.
 */

public class UserActivity extends ConnectivityActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user);

        // Pass the Author from the Intent to the Fragment to be inflated into the fragment_container
        if (savedInstanceState == null && getIntent().getStringExtra(AUTHOR_KEY) != null) {

            // Retrieve the Author from cache
            String authorId = getIntent().getStringExtra(AUTHOR_KEY);
            Author author = (Author) DataCache.getInstance().get(authorId);

            if (author != null) {
                UserFragment fragment = UserFragment.newInstance(author);

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, fragment, FRAG_TAG_ACCOUNT)
                        .commit();
            } else {
                loadAuthorFromFirebase(authorId);
            }
        }
    }

    /**
     * Loads an author's details from Firebase
     *
     * @param authorId    FirebaseId of the Author to be loaded
     */
    private void loadAuthorFromFirebase(String authorId) {
        FirebaseProviderUtils.getModel(
                FirebaseProviderUtils.FirebaseType.AUTHOR,
                authorId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        Author author = (Author) model;

                        UserFragment fragment = UserFragment.newInstance(author);

                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, fragment)
                                .commit();
                    }
                });
    }
}
