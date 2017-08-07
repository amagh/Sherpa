package project.hikerguide.ui.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.FragmentGuideDetailsBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.UserActivity;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsFragment extends Fragment {
    // ** Member Variables ** //
    private FragmentGuideDetailsBinding mBinding;
    private Guide mGuide;
    private Section[] mSections;
    private Author mAuthor;
    private GuideDetailsAdapter mAdapter;

    public GuideDetailsFragment() {}

    /**
     * Factory for creating a GuideDetailsFragment for a specific Guide
     *
     * @param guide    Guide whose details will be shown in the Fragment
     * @return A GuideDetailsFragment with a Bundle attached for displaying details for a Guide
     */
    public static GuideDetailsFragment newInstance(Guide guide) {
        // Init the Bundle that will be passed with the Fragment
        Bundle args = new Bundle();

        // Put the Guide from the signature into the Bundle
        args.putParcelable(GUIDE_KEY, guide);

        // Initialize the Fragment and attach the args
        GuideDetailsFragment fragment = new GuideDetailsFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_details, container, false);

        ((GuideDetailsActivity) getActivity()).setSupportActionBar(mBinding.guideDetailsTb);

        if (getArguments() != null && getArguments().getParcelable(GUIDE_KEY) != null) {

            mGuide = getArguments().getParcelable(GUIDE_KEY);
        } else {
            Timber.d("No guide passed with the Fragment");
        }

        // Setup the Adapter
        mAdapter = new GuideDetailsAdapter(new GuideDetailsAdapter.ClickHandler() {
            @Override
            public void onClickAuthor(Author author) {
                Intent intent = new Intent(getActivity(), UserActivity.class);
                intent.putExtra(AUTHOR_KEY, author);

                startActivity(intent);
            }
        });

        // Setup the RecyclerView
        mBinding.setVm(new GuideViewModel(getActivity(), mGuide));
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.guideDetailsRv.setAdapter(mAdapter);

        // Add Guide
        mAdapter.setGuide(mGuide, (GuideDetailsActivity) getActivity());

        // Get the rest of the Guide's details
        getSections();
        getAuthor();

        return mBinding.getRoot();
    }

    /**
     * Loads the corresponding Sections for the Guide from FirebaseDatabase
     */
    private void getSections() {

        // Build the Query for the Sections using the FirebaseId of the Guide
        final Query sectionQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.SECTIONS)
                .orderByKey()
                .equalTo(mGuide.firebaseId);

        // Add a Listener for when the data is ready
        sectionQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Convert the DataSnapshot to the Section model
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // The DataSnapshot containing the Sections is a child of the child
                    // (grand-child?) of the DataSnapshot from the signature
                    mSections = (Section[]) FirebaseProviderUtils.getModelsFromSnapshot(
                            DatabaseProvider.FirebaseType.SECTION,
                            snapshot);
                }

                // Set the Sections to be used by the Adapter
                mAdapter.setSections(mSections);

                // Remove the Listener
                sectionQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.e(databaseError.getMessage());

                // Remove the Listener
                sectionQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Loads the corresponding Author of the Guide from the Firebase Database
     */
    private void getAuthor() {

        // Build a reference to the Guide in the Firebase Database
        final DatabaseReference authorReference = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(mGuide.authorId);

        // Add a Listener
        authorReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Convert the DataSnapshot to an Author
                mAuthor = (Author) FirebaseProviderUtils.getModelFromSnapshot(
                        DatabaseProvider.FirebaseType.AUTHOR,
                        dataSnapshot);

                // Set the Author to be used by the Adapter
                mAdapter.setAuthor(mAuthor);

                // Remove the Listener
                authorReference.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.e(databaseError.getMessage());

                // Remove the Listener
                authorReference.removeEventListener(this);
            }
        });
    }
}
