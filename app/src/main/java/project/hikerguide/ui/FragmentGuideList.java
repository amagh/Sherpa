package project.hikerguide.ui;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import project.hikerguide.R;
import project.hikerguide.databinding.FragmentGuideListBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.sync.SyncGuidesTaskLoader;
import project.hikerguide.ui.adapters.GuideAdapter;
import timber.log.Timber;

/**
 * Created by Alvin on 7/21/2017.
 */

public class FragmentGuideList extends Fragment implements LoaderManager.LoaderCallbacks<Guide[]>{
    // ** Constants ** //
    private static final int GUIDES_LOADER = 4349;
    // ** Member Variables ** //
    private FragmentGuideListBinding mBinding;
    private GuideAdapter mAdapter;

    public FragmentGuideList() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Databind inflation of the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_list, container, false);

        // Initialize the GuideAdapter
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {
                // Pass the clicked Guide to the Activity so it can start the GuideDetailsActivity
                ((MainActivity) getActivity()).onGuideClicked(guide);
            }
        });

        // Set the Adapter and LayoutManager for the RecyclerView
        mBinding.guideListRv.setAdapter(mAdapter);
        mBinding.guideListRv.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Begin loading Guides using the Loader
        getActivity().getSupportLoaderManager().initLoader(GUIDES_LOADER, null, this);
        return mBinding.getRoot();
    }

    @Override
    public Loader<Guide[]> onCreateLoader(int id, Bundle args) {
        return new SyncGuidesTaskLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Guide[]> loader, Guide[] data) {
        // Set the retrieved Guides in the Adapter
        mAdapter.setGuides(Arrays.asList(data));
    }

    @Override
    public void onLoaderReset(Loader<Guide[]> loader) {

    }

    public interface OnGuideClickListener {
        void onGuideClicked(Guide guide);
    }
}
