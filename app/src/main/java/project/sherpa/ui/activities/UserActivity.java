package project.sherpa.ui.activities;

import android.os.Bundle;

import project.sherpa.R;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.ui.fragments.UserFragment;

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

            // Retrieve the authorId to be loaded
            String authorId = getIntent().getStringExtra(AUTHOR_KEY);

            // Instantiate the Fragment and inflate it
            UserFragment fragment = UserFragment.newInstance(authorId);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment, FRAG_TAG_USER)
                    .commit();
        }
    }
}
