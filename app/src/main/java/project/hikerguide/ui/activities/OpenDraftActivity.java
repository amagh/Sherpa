package project.hikerguide.ui.activities;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.databinding.ActivityOpenDraftBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.adapters.GuideAdapter;

/**
 * Created by Alvin on 8/15/2017.
 */

public class OpenDraftActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // ** Constants ** //
    private static final int LOADER_DRAFT = 9912;

    // ** Member Variables ** //
    private ActivityOpenDraftBinding mBinding;
    private List<Guide> mGuideList;
    private GuideAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_open_draft);
        setSupportActionBar(mBinding.draftTb);
        getSupportActionBar().setTitle(getString(R.string.title_drafts));

        // Init RecyclerView
        initRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Clear the Adapter and reload from the database in case a draft was deleted
        mGuideList = new ArrayList<>();
        mAdapter.setGuides(mGuideList);

        // Init the CursorLoader for Guide drafts
        getSupportLoaderManager().restartLoader(LOADER_DRAFT, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Generate the CursorLoader for loading all draft Guides
        switch (id) {
            case LOADER_DRAFT:
                return new CursorLoader(
                        this,
                        GuideProvider.Guides.CONTENT_URI,
                        null,
                        GuideContract.GuideEntry.DRAFT + " = ?",
                        new String[] {"1"},
                        GuideContract.GuideEntry.FIREBASE_ID + " DESC"
                );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {

            // Init the List of Guides
            mGuideList = new ArrayList<>();

            if (data.moveToFirst()) {
                // Populate the List by generating Guides from the Cursor
                do {
                    Guide guide = Guide.createGuideFromCursor(data);
                    mGuideList.add(guide);
                } while (data.moveToNext());

                // Set the List to the Adapter
                mAdapter.setGuides(mGuideList);
            }

            if (mGuideList.size() == 0) {

                // Inform the user that there are no saved Guide drafts
                mBinding.draftEmptyTv.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Initializes the elements for the RecyclerView
     */
    private void initRecyclerView() {

        // Init the GuideAdapter
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {

                // Start the CreateGuideActivity and set the data to the Uri for the Guide to be
                // opened
                Intent intent = new Intent(OpenDraftActivity.this, CreateGuideActivity.class);
                intent.setData(GuideProvider.Guides.withId(guide.firebaseId));

                startActivity(intent);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

            }
        });

        // Set the Adapter and LayoutManager for the RecyclerView
        mBinding.draftRv.setAdapter(mAdapter);
        mBinding.draftRv.setLayoutManager(new StaggeredGridLayoutManager(
                getResources().getInteger(R.integer.guide_columns),
                StaggeredGridLayoutManager.VERTICAL));
    }
}