package project.hikerguide.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.databinding.FragmentSavedGuidesBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.adapters.GuideAdapter;

import static project.hikerguide.utilities.interfaces.IntentKeys.GUIDE_KEY;

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
                GuideContract.GuideEntry.IMAGE_URI + " IS NOT NULL AND NOT" + GuideContract.GuideEntry.DRAFT + " = ?",
                new String[] {"1"},
                GuideContract.GuideEntry.TRAIL_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Check to ensure the Cursor is valid
        if (data != null) {
            if (data.moveToFirst()) {

                // Add each Guide from the database to the Adapter
                do {
                    mAdapter.addGuide(Guide.createGuideFromCursor(data));
                } while (data.moveToNext());
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
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {

                // Start the GuideDetailsActivity for the selected Guide
                Intent intent = new Intent(getActivity(), GuideDetailsActivity.class);
                intent.putExtra(GUIDE_KEY, guide);

                startActivity(intent);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

            }
        });

        // Set the List for the Adapter
        mAdapter.setGuides(mGuideList);

        // Set the LayoutManager and Adapter for the RecyclerView
        mBinding.savedGuidesRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.savedGuidesRv.setAdapter(mAdapter);
    }
}
