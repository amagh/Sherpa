package project.hikerguide.ui;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.ActivityGuideDetailsBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.ui.GuideDetailsActivity.IntentKeys.GUIDE_KEY;

public class GuideDetailsActivity extends MapboxActivity {
    // ** Constants ** //
    public interface IntentKeys {
        String GUIDE_KEY = "guides";
    }

    // ** Member Variables ** //
    private ActivityGuideDetailsBinding mBinding;
    private Guide mGuide;
    private Section[] mSections;
    private Author mAuthor;
    private GuideDetailsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_guide_details);

        setSupportActionBar(mBinding.guideDetailsTb);

        if ((mGuide = getIntent().getParcelableExtra(GUIDE_KEY)) == null) {
            Timber.d("No Guide passed from MainActivity");
            return;
        }

        // Setup the Adapter
        mAdapter = new GuideDetailsAdapter();

        // Setup the RecyclerView
        mBinding.setVm(new GuideViewModel(this, mGuide));
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(this));
        mBinding.guideDetailsRv.setAdapter(mAdapter);

        // Add Guide
        mAdapter.setGuide(mGuide, this);

        // Get the rest of the Guide's details
        getSections();
        getAuthor();
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
