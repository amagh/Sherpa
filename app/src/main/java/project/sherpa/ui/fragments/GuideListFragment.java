package project.sherpa.ui.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import project.sherpa.prefs.SettingsActivity;
import project.sherpa.R;
import project.sherpa.data.GuideDatabase;
import project.sherpa.databinding.FragmentGuideListBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.services.firebaseservice.QueryChangeListener;
import project.sherpa.ui.activities.MainActivity;
import project.sherpa.ui.adapters.GuideAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.adapters.interfaces.LongClickHandler;
import project.sherpa.ui.fragments.abstractfragments.ConnectivityFragment;
import project.sherpa.utilities.DataCache;

import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.GUIDE;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideListFragment extends ConnectivityFragment {

    // ** Member Variables ** //
    private FragmentGuideListBinding mBinding;
    private GuideAdapter mAdapter;
    private Author mAuthor;
    private List<Guide> mGuideList;
    private QueryChangeListener<Guide> mGuideQueryListener;
    private ModelChangeListener<Author> mUserListener;

    public GuideListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // DataBind inflation of the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_list, container, false);
        bindFirebaseProviderService(true);

        ((MainActivity) getActivity()).setSupportActionBar(mBinding.toolbar);

        // Initialize the GuideAdapter
        initRecyclerView();

        // Check if there are guides to be loaded from savedInstanceState
        if (savedInstanceState != null) {

            // Retrieve the list of guideIds to be retrieved
            List<String> guideIdList = savedInstanceState.getStringArrayList(GUIDE_KEY);

            // Load the Guide associated with each guideId from cache
            loadDataFromCache(guideIdList);
        }

        // Load ads if applicable
        loadAdViewModel(mBinding);

        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_general, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_prefs:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return false;
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();

        mBinding.guideListPb.setVisibility(View.GONE);
    }

    @Override
    protected void onServiceConnected() {

        if (mAuthor == null) {

            // Attempt to load a logged in user's favorite data
            loadCurrentUser();
        }

        // If Adapter is empty, load the Guides from Firebase
        if (mAdapter.isEmpty()) {

            // Show the ProgressBar
            mBinding.guideListPb.setVisibility(View.VISIBLE);

            loadGuides();
        } else {
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        }
    }

    /**
     * Initiates the QueryChangeListener for the latest guides
     */
    private void loadGuides() {

        Query guideQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .orderByKey()
                .limitToLast(20);

        mGuideQueryListener = new QueryChangeListener<Guide>(GUIDE, guideQuery, null) {
            @Override
            public void onQueryChanged(Guide[] models) {

                // Convert the Array of Guides to a List, reverse them so the newest guides are
                // first and then set it to the Adapter
                mGuideList = new ArrayList<>(Arrays.asList(models));
                Collections.reverse(mGuideList);
                mAdapter.setGuides(mGuideList);

                // Hide the ProgressBar
                mBinding.guideListPb.setVisibility(View.GONE);
            }
        };

        mService.registerQueryChangeListener(mGuideQueryListener);
    }

    /**
     * Loads a logged in user's favorite data from Firebase
     */
    private void loadCurrentUser() {

        // Check if the user is logged into their account
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Not logged in, nothing to do
        if (user == null) return;

        mUserListener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
            @Override
            public void onModelReady(Author model) {
                mAuthor = model;

                // Set the Author to the Adapter so that the favorite data can be synchronized
                mAdapter.setAuthor(mAuthor);
            }

            @Override
            public void onModelChanged() {

                // Update the favorite status in the Adapter
                mAdapter.updateViewModelFavorites();
            }
        };

        mService.registerModelChangeListener(mUserListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGuideList == null || mGuideList.size() == 0) return;

        // Save the FirebaseId of the Guides to be loaded when Fragment is re-created
        ArrayList<String> guideIdList = new ArrayList<>();

        for (Guide guide : mGuideList) {
            guideIdList.add(guide.firebaseId);
        }

        outState.putStringArrayList(GUIDE_KEY, guideIdList);
    }

    /**
     * Sets up the Recycler View and the elements required to make it work
     */
    private void initRecyclerView() {

        // Set up the GuideAdapter to populate the RecyclerView
        mAdapter = new GuideAdapter(new ClickHandler<Guide>() {
            @Override
            public void onClick(Guide guide) {
                // Pass the clicked Guide to the Activity so it can start the GuideDetailsActivity
                ((MainActivity) getActivity()).onGuideClicked(guide);
            }
        });

        // Set the Adapter and LayoutManager for the RecyclerView
        mBinding.guideListRv.setAdapter(mAdapter);
        mBinding.guideListRv.setLayoutManager(new StaggeredGridLayoutManager(
                getResources().getInteger(R.integer.guide_columns),
                StaggeredGridLayoutManager.VERTICAL));
    }

    /**
     * Retrieves Guides from cache
     *
     * @param guideIdList    A list of FirebaseIds for guides to retrieve from DataCache
     */
    private void loadDataFromCache(List<String> guideIdList) {

        // Initialize mGuideList
        mGuideList = new ArrayList<>();

        for (String guideId : guideIdList) {

            // Retrieve Guides from DataCache
            Guide guide = (Guide) DataCache.getInstance().get(guideId);

            // Add the Guide to the mGuideList and Adapter
            mGuideList.add(guide);
            mAdapter.addGuide(guide);

            mBinding.guideListPb.setVisibility(View.GONE);
        }
    }

    public interface OnGuideClickListener {
        void onGuideClicked(Guide guide);
    }
}
