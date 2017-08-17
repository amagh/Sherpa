package project.hikerguide.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.FragmentGuideListBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideListFragment extends Fragment implements ConnectivityActivity.ConnectivityCallback{
    // ** Constants ** //
    private static final int GUIDES_LOADER = 4349;

    // ** Member Variables ** //
    private FragmentGuideListBinding mBinding;
    private GuideAdapter mAdapter;
    private Author mAuthor;

    public GuideListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // DataBind inflation of the View
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_list, container, false);

        ((MainActivity) getActivity()).setSupportActionBar(mBinding.toolbar);

        // Initialize the GuideAdapter
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
        mBinding.guideListRv.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).setConnectivityCallback(this);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onConnected() {

        FirebaseDatabase.getInstance().goOnline();

        if (mAuthor == null) {
            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {
                    mAuthor = (Author) model;

                    mAdapter.setAuthor(mAuthor);
                }
            });
        }

        // If Adapter is empty, load the Guides from Firebase
        if (mAdapter.isEmpty()) {
            loadGuides();
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
                        DatabaseProvider.FirebaseType.GUIDE,
                        dataSnapshot);

                List<Guide> guideList = Arrays.asList(guides);
                Collections.reverse(guideList);
                mAdapter.setGuides(guideList);
                guideQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                guideQuery.removeEventListener(this);
            }
        });
    }

    public interface OnGuideClickListener {
        void onGuideClicked(Guide guide);
    }
}
