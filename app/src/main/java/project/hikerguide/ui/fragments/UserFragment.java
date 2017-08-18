package project.hikerguide.ui.fragments;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.FragmentUserBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.AuthorViewModel;
import project.hikerguide.ui.activities.AccountActivity;
import project.hikerguide.ui.activities.AreaActivity;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.activities.OpenDraftActivity;
import project.hikerguide.ui.adapters.AuthorDetailsAdapter;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.ui.behaviors.FabSpeedDialScrollBehavior;
import project.hikerguide.utilities.ContentProviderUtils;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/15/2017.
 */

public class UserFragment extends Fragment implements FabSpeedDial.MenuListener,
        ConnectivityActivity.ConnectivityCallback {

    // ** Constants ** //
    public static final int ACCOUNT_ACTIVITY_REQUEST_CODE = 7219;

    // ** Member Variables ** //
    private FragmentUserBinding mBinding;
    private Author mAuthor;
    private AuthorDetailsAdapter mAdapter;
    private List<BaseModel> mModelList;

    public UserFragment() {}

    /**
     * Factory pattern for instantiating a new UserFragment
     *
     * @param author    Author to be attached to the UserFragment
     * @return UserFragment with an Author attached to load details for
     */
    public static UserFragment newInstance(Author author) {

        // Create a Bundle to pass the Author
        Bundle args = new Bundle();
        args.putParcelable(AUTHOR_KEY, author);

        // Create the UserFragment and attach the Bundle
        UserFragment fragment = new UserFragment();
        fragment.setArguments(args);


        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the Layout using DataBindingUtils
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false);

        // Set a blank Author to the ViewDataBinding so that the FabSpeedDial's visibility will be
        // properly triggered
        mBinding.setVm(new AuthorViewModel(getActivity(), this, new Author()));
        mBinding.fabDial.setMenuListener(this);

        initRecyclerView();

        if (getArguments() == null || getArguments().getParcelable(AUTHOR_KEY) == null) {

            // User is checking their own profile
            loadUserSelfProfile();
        } else {
            mAuthor = getArguments().getParcelable(AUTHOR_KEY);

            // Add the Author to the Adapter so their info can be displayed
            mAdapter.addModel(mAuthor);
            mBinding.setVm(new AuthorViewModel(getActivity(), this, mAuthor));

            // Load the Guides that the Author has created into the Adapter
            loadGuidesForAuthor();

            // Check if the User is accessing their own page
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null && mAuthor.firebaseId.equals(user.getUid())) {

                // Setup for someone viewing their own profile
                setupForSelfProfile();
            }
        }

        ((ConnectivityActivity) getActivity()).setConnectivityCallback(this);
        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_log_off:
                FirebaseAuth.getInstance().signOut();

                startActivityForResult(
                        new Intent(getActivity(), AccountActivity.class),
                        ACCOUNT_ACTIVITY_REQUEST_CODE);

                return true;
        }

        return false;
    }

    /**
     * Sets up the layouts to be appropriate for someone viewing their own profile
     */
    private void setupForSelfProfile() {

        // Enable option to edit their profile
        mAdapter.enableEditing();
        mBinding.getVm().enableEditing();

        // Add the SupportActionBar so the menu items can be created, but remove the title
        ((AppCompatActivity) getActivity()).setSupportActionBar(mBinding.toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(null);

        // Set the layout behavior for the FAB
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mBinding.fabDial.getLayoutParams();
        params.setBehavior(new FabSpeedDialScrollBehavior());
    }

    /**
     * Initializes components for RecyclerView to function
     */
    private void initRecyclerView() {
        // Init the Adapter
        mAdapter = new AuthorDetailsAdapter(getActivity(), this, new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {

                // Start the Activity to display Guide details
                Intent intent = new Intent(getActivity(), GuideDetailsActivity.class);
                intent.putExtra(GUIDE_KEY, guide);
                startActivity(intent);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

            }
        });

        // Set the LayoutManager and Adapter
        mBinding.userRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.userRv.setAdapter(mAdapter);

        // Init the List of Models
        mModelList = new ArrayList<>();

        // Set the List in the Adapter
        mAdapter.setModelList(mModelList);
    }

    /**
     * Checks that the Firebase User has been added to the Firebase Database and loads their
     * profile from Firebase Database.
     */
    private void loadUserSelfProfile() {
        // Get an instance of the FirebaseUser
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // No valid user, send to the AccountActivity to sign in
            startActivityForResult(
                    new Intent(getActivity(), AccountActivity.class),
                    ACCOUNT_ACTIVITY_REQUEST_CODE);

            return;
        }

        // Get a reference to the DatabaseReference corresponding to the user
        final DatabaseReference authorRef = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(user.getUid());

        authorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Check to ensure that the DataSnapshot is valid
                if (dataSnapshot.exists()) {

                    // Convert the DataSnapshot to an Author Object
                    mAuthor = (Author) FirebaseProviderUtils
                            .getModelFromSnapshot(DatabaseProvider.FirebaseType.AUTHOR, dataSnapshot);
                }

                // Remove Listener
                authorRef.removeEventListener(this);

                // If the Author does not exist in the Firebase Database, add them
                if (mAuthor == null) {

                    // Transfer the locally favorite'd Guides to their new online profile
                    mAuthor = ContentProviderUtils.generateAuthorFromDatabase(getActivity());
                    authorRef.setValue(mAuthor);
                } else {

                    // Sync the local database of favorite Guides to the online one
                    ContentProviderUtils.cleanDatabase(getActivity(), mAuthor);
                }

                // Add the Author to the Adapter so their info can be displayed
                mAdapter.addModel(mAuthor);
                mBinding.setVm(new AuthorViewModel(getActivity(), UserFragment.this, mAuthor));

                // Setup for someone viewing their own profile
                setupForSelfProfile();

                // Load the Guides that the Author has created into the Adapter
                loadGuidesForAuthor();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                authorRef.removeEventListener(this);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ACCOUNT_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK
                && data.getBooleanExtra(AUTHOR_KEY, false)) {
            loadUserSelfProfile();
        } else {
            ((MainActivity) getActivity()).switchFragments(R.id.navigation_home);
        }
    }

    /**
     * Queries the Firebase Database and loads Guides that have been authored by the user
     */
    private void loadGuidesForAuthor() {

        // Check to make sure mAuthor has been loaded
        if (mAuthor == null) return;

        // Query the Firebase Database
        final Query guideQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .orderByChild(GuideContract.GuideEntry.AUTHOR_ID);

        guideQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Check that the returned values are valid
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Guide[] guides = (Guide[]) FirebaseProviderUtils
                            .getModelsFromSnapshot(DatabaseProvider.FirebaseType.GUIDE, dataSnapshot);

                    // Add each Guide to the Adapter
                    for (Guide guide : guides) {
                        mAdapter.addModel(guide);
                    }
                }

                // Hide ProgressBar
                mBinding.userPb.setVisibility(View.GONE);

                // Remove Listener
                guideQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                guideQuery.removeEventListener(this);
            }
        });
    }

    /**
     * Switches the layout used by the AuthorDetailsAdapter to one with EditText so the user can
     * change the info on their profile
     */
    public void switchAuthorLayout() {
        mAdapter.switchAuthorLayout();
    }

    /**
     * Updates the user's values in FirebaseDatabase with their newly updated values
     */
    public void updateAuthorValues() {

        // Get the directory where the Author's info is stored on the FirebaseDatabase
        String directory = GuideDatabase.AUTHORS + "/" + mAuthor.firebaseId;

        // Create a Map for the update procedure
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(directory, mAuthor.toMap());

        // Modify the Author name for all Guides written by the Author
        for (int i = 1; i < mModelList.size(); i++) {
            Guide guide = (Guide) mModelList.get(i);
            guide.authorName = mAuthor.name;

            directory = GuideDatabase.GUIDES + "/" + guide.firebaseId;

            childUpdates.put(directory, guide.toMap());
        }

        mAdapter.notifyItemRangeChanged(1, mModelList.size());

        // Update the values
        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates);

    }

    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {
        return true;
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.fab_new_guide:

                // Launch Activity flow to start a new Guide
                Intent intent = new Intent(getActivity(), AreaActivity.class);
                intent.putExtra(AUTHOR_KEY, mAuthor);
                startActivity(intent);

                return true;

            case R.id.fab_open_draft:

                // Launch Activity for opening a saved draft
                startActivity(new Intent(getActivity(), OpenDraftActivity.class));
                return true;
        }

        return false;
    }

    @Override
    public void onMenuClosed() {

    }

    @Override
    public void onConnected() {
        FirebaseDatabase.getInstance().goOnline();
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();
    }
}
