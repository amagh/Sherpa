package project.hikerguide.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.mapbox.mapboxsdk.Mapbox;

import project.hikerguide.R;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.fragments.GuideListFragment;
import project.hikerguide.ui.fragments.SearchFragment;
import project.hikerguide.ui.fragments.UserFragment;

import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;

public class MainActivity extends ConnectivityActivity implements GuideListFragment.OnGuideClickListener {
    // ** Member Variables ** //
    private BottomNavigationView mNavigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            FragmentManager manager = getSupportFragmentManager();
            Fragment fragment;

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    fragment = new GuideListFragment();
                    break;
                case R.id.navigation_search:
                    fragment = new SearchFragment();
                    break;
                case R.id.navigation_account:
                    fragment = new UserFragment();
                    break;

                case R.id.navigation_favorites:
                    return true;

                default: return false;
            }

            manager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
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

        Mapbox.getInstance(this, getString(R.string.mapbox_token));
    }

    @Override
    public void onGuideClicked(Guide guide) {
        // Create a new Intent to launch the GuideDetailsActivity and add the clicked Guide as an
        // extra
        Intent intent = new Intent(this, GuideDetailsActivity.class);
        intent.putExtra(GUIDE_KEY, guide);
        startActivity(intent);
    }

    public void switchFragments(int id) {
        mNavigation.setSelectedItemId(id);
    }
}
