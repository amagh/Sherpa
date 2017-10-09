package project.sherpa.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.FragmentSavedGuidesBinding;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.ui.activities.MainActivity;
import project.sherpa.ui.adapters.GuideAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.utilities.DataCache;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/16/2017.
 */

public class SavedGuidesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    // ** Constants ** //
    private static final int LOADER_SAVED_GUIDES = 6642;

    // ** Member Variables ** //
    private FragmentSavedGuidesBinding mBinding;
    private GuideAdapter mAdapter;
    private List<Guide> mGuideList;

    public SavedGuidesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the Layout using DataBindingUtils
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_saved_guides, container, false);

        ((MainActivity) getActivity()).setSupportActionBar(mBinding.toolbar);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_saved_guides));

        // Initialize the RecyclerView
        initRecyclerView();

        // Load the saved Guides from database
        getActivity().getSupportLoaderManager().initLoader(LOADER_SAVED_GUIDES, null, this);
        return mBinding.getRoot();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Generate the CursorLoader for saved Guides
        return new CursorLoader(
                getActivity(),
                GuideProvider.Guides.CONTENT_URI,
                null,
                GuideContract.GuideEntry.IMAGE_URI + " IS NOT NULL AND " + GuideContract.GuideEntry.DRAFT + " IS NULL",
                null,
                GuideContract.GuideEntry.TRAIL_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Check to ensure the Cursor is valid
        if (data != null) {

            // Clear the Array
            mGuideList = new ArrayList<>();
            mAdapter.setGuides(mGuideList);

            if (data.moveToFirst()) {

                // Add each Guide from the database to the Adapter
                do {
                    Guide guide = Guide.createGuideFromCursor(data);
                    mAdapter.addGuide(guide);

                    DataCache.getInstance().store(guide);
                } while (data.moveToNext());
            } else {
                mBinding.savedGuidesEmptyTv.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Initializes all elements required for the RecyclerView
     */
    private void initRecyclerView() {

        // Initialize the List to hold all the Guides
        mGuideList = new ArrayList<>();

        // Init the Adapter
        mAdapter = new GuideAdapter(new ClickHandler<Guide>() {
            @Override
            public void onClick(Guide guide) {

                // Start the GuideDetailsActivity for the selected Guide
                Intent intent = new Intent(getActivity(), GuideDetailsActivity.class);
                intent.putExtra(GUIDE_KEY, guide.firebaseId);
                intent.putExtra(AUTHOR_KEY, guide.authorId);

                startActivity(intent);
            }
        });

        mAdapter.setHasStableIds(true);

        // Set the List for the Adapter
        mAdapter.setGuides(mGuideList);

        // Set the LayoutManager and Adapter for the RecyclerView
        mBinding.savedGuidesRv.setAdapter(mAdapter);
        mBinding.savedGuidesRv.setLayoutManager(new StaggeredGridLayoutManager(
                getResources().getInteger(R.integer.guide_columns),
                StaggeredGridLayoutManager.VERTICAL));
    }
}
