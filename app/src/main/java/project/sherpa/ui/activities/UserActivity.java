package project.sherpa.ui.activities;

import android.os.Bundle;

import project.sherpa.R;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.fragments.UserFragment;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_USER;
import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;

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
                        .add(R.id.fragment_container, fragment, FRAG_TAG_USER)
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
