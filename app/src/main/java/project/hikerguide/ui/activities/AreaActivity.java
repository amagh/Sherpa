package project.hikerguide.ui.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import project.hikerguide.R;
import project.hikerguide.databinding.ActivityAreaBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.viewmodels.DoubleSearchViewModel;

import static project.hikerguide.utilities.Constants.IntentKeys.AREA_KEY;
import static project.hikerguide.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.Constants.IntentKeys.TRAIL_KEY;

/**
 * Created by Alvin on 8/1/2017.
 */

public class AreaActivity extends MapboxActivity {

    // ** Constants ** //
    private static final String QUERY_KEY = "query";

    // ** Member Variables ** //
    ActivityAreaBinding mBinding;
    Author mAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_area);

        mAuthor = getIntent().getParcelableExtra(AUTHOR_KEY);

        // Initialize ViewModel for the layout
        DoubleSearchViewModel vm = new DoubleSearchViewModel(this);
        mBinding.setVm(vm);

        if (savedInstanceState != null) {
            Area area = savedInstanceState.getParcelable(AREA_KEY);
            Trail trail = savedInstanceState.getParcelable(TRAIL_KEY);
            String query = savedInstanceState.getString(QUERY_KEY);

            if (area != null) {
                mBinding.getVm().setArea(area);
            }

            if (trail != null) {
                mBinding.getVm().setTrail(trail);
            }

            if (query != null) {
                mBinding.getVm().setQuery(query);
            }
        }
    }

    /**
     * Launches the CreateGuideActivity
     *
     * @param area     Area selected by the user to be passed in the Intent
     * @param trail    Trail selected by the user to be passed in the Intent
     */
    public void startCreateGuideActivity(Area area, Trail trail) {

        // Init the Intent to launch CreateGuideActivity
        Intent intent = new Intent(this, CreateGuideActivity.class);

        // Add the models to the Intent
        intent.putExtra(AUTHOR_KEY, mAuthor);
        intent.putExtra(AREA_KEY, area);
        intent.putExtra(TRAIL_KEY, trail);

        // Start the Activity
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Area area = mBinding.getVm().getArea();
        Trail trail = mBinding.getVm().getTrail();
        String query = mBinding.getVm().getQuery();

        outState.putParcelable(AREA_KEY, area);
        outState.putParcelable(TRAIL_KEY, trail);
        outState.putString(QUERY_KEY, query);
    }
}
