package project.hikerguide.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.FragmentGuideListBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.utilities.DataCache;
import project.hikerguide.utilities.FirebaseProviderUtils;

import static project.hikerguide.utilities.Constants.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideListFragment extends Fragment implements ConnectivityActivity.ConnectivityCallback{

    // ** Member Variables ** //
    private FragmentGuideListBinding mBinding;
    private GuideAdapter mAdapter;
    private Author mAuthor;
    private List<Guide> mGuideList;

    public GuideListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // DataBind inflation of the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_list, container, false);

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

        // Check to see if the device is connected to a network
        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).setConnectivityCallback(this);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onConnected() {

        FirebaseDatabase.getInstance().goOnline();

        if (mAuthor == null) {

            // Attempt to load a logged in user's favorite data
            loadFavoriteData();
        }

        // If Adapter is empty, load the Guides from Firebase
        if (mAdapter.isEmpty()) {
            loadGuides();
        } else {
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        }
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();
    }

    private void loadGuides() {

        final Query guideQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .orderByKey()
                .limitToLast(20);

        guideQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Guide[] guides = (Guide[]) FirebaseProviderUtils.getModelsFromSnapshot(
                        FirebaseProviderUtils.FirebaseType.GUIDE,
                        dataSnapshot);

                mGuideList = Arrays.asList(guides);
                Collections.reverse(mGuideList);
                mAdapter.setGuides(mGuideList);

                // Hide ProgressBar
                mBinding.guideListPb.setVisibility(View.GONE);

                DataCache cache = DataCache.getInstance();
                for (Guide guide : mGuideList) {
                    cache.store(guide);
                }

                guideQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                guideQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Loads a logged in user's favorite data from Firebase
     */
    private void loadFavoriteData() {

        // Check if the user is logged into their account
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Not logged in, nothing to do
        if (user == null) return;

        // Retrieve the current user from DataCache
        mAuthor = (Author) DataCache.getInstance().get(user.getUid());

        if (mAuthor != null) {

            // For checking to see if a guide has been favorite'd by the user
            mAdapter.setAuthor(mAuthor);
        } else {

            // User not found in cache. Load from Firebase
            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {
                    mAuthor = (Author) model;

                    // For checking to see if a guide has been favorite'd by the user
                    mAdapter.setAuthor(mAuthor);
                }
            });
        }
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
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {
                // Pass the clicked Guide to the Activity so it can start the GuideDetailsActivity
                ((MainActivity) getActivity()).onGuideClicked(guide);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

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
