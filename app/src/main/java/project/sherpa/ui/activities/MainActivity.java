package project.sherpa.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import com.mapbox.mapboxsdk.Mapbox;

import project.sherpa.R;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.ui.fragments.FavoritesFragment;
import project.sherpa.ui.fragments.GuideListFragment;
import project.sherpa.ui.fragments.SavedGuidesFragment;
import project.sherpa.ui.fragments.SearchFragment;
import project.sherpa.ui.fragments.UserFragment;

import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_ACCOUNT;
import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_FAVORITE;
import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_HOME;
import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_SAVED_GUIDES;
import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_SEARCH;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;

public class MainActivity extends ConnectivityActivity implements GuideListFragment.OnGuideClickListener {



    // ** Member Variables ** //
    private BottomNavigationView mNavigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            FragmentManager manager = getSupportFragmentManager();
            Fragment fragment;
            String tag;

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    fragment = new GuideListFragment();
                    tag = FRAG_TAG_HOME;
                    break;
                case R.id.navigation_search:
                    fragment = new SearchFragment();
                    tag = FRAG_TAG_SEARCH;
                    break;
                case R.id.navigation_account:
                    fragment = new UserFragment();
                    tag = FRAG_TAG_ACCOUNT;
                    break;

                case R.id.navigation_favorites:
                    fragment = new FavoritesFragment();
                    tag = FRAG_TAG_FAVORITE;
                    break;

                case R.id.navigation_saved:
                    fragment = new SavedGuidesFragment();
                    tag = FRAG_TAG_SAVED_GUIDES;
                    break;

                default: return false;
            }

            manager.beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();

            return true;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (savedInstanceState == null) {
            switchFragments(R.id.navigation_home);
        }

        Mapbox.getInstance(this, getString(R.string.mapbox_token));
    }

    @Override
    public void onGuideClicked(Guide guide) {
        // Create a new Intent to launch the GuideDetailsActivity and add the clicked Guide as an
        // extra
        Intent intent = new Intent(this, GuideDetailsActivity.class);
        intent.putExtra(GUIDE_KEY, guide.firebaseId);
        startActivity(intent);
    }

    public void switchFragments(int id) {
        mNavigation.setSelectedItemId(id);
    }
}
