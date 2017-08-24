package project.hikerguide.ui.activities;

import android.os.Bundle;

import project.hikerguide.R;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.ui.fragments.UserFragment;

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
        if (getIntent().getParcelableExtra(AUTHOR_KEY) != null) {
            Author author = getIntent().getParcelableExtra(AUTHOR_KEY);

            UserFragment fragment = UserFragment.newInstance(author);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
