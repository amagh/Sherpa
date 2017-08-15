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

        initRecyclerView();

        getSupportLoaderManager().initLoader(LOADER_DRAFT, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

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
            mGuideList = new ArrayList<>();

            if (data.moveToFirst()) {
                do {
                    Guide guide = Guide.createGuideFromCursor(data);
                    mGuideList.add(guide);
                    mAdapter.setGuides(mGuideList);
                } while (data.moveToNext());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void initRecyclerView() {
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {
                Intent intent = new Intent(OpenDraftActivity.this, CreateGuideActivity.class);
                intent.setData(GuideProvider.Guides.withId(guide.firebaseId));

                startActivity(intent);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

            }
        });

        mBinding.draftRv.setAdapter(mAdapter);
        mBinding.draftRv.setLayoutManager(new LinearLayoutManager(this));
    }
}