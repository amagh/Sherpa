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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.databinding.FragmentFavoritesBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/16/2017.
 */

public class FavoritesFragment extends Fragment implements ConnectivityActivity.ConnectivityCallback,
        LoaderManager.LoaderCallbacks<Cursor> {

    // ** Constants ** //
    private static final int FAVORITES_LOADER = 1126;

    // ** Member Variables ** //
    private FragmentFavoritesBinding mBinding;
    private GuideAdapter mAdapter;
    private List<Guide> mGuideList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the View using DataBindingUtils
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_favorites, container, false);

        // Initialize the RecyclerView
        initRecyclerView();

        // Begin listening for network status changes
        ((MainActivity) getActivity()).setConnectivityCallback(this);

        return mBinding.getRoot();
    }

    /**
     * Initializes the elements required for the RecyclerView
     */
    private void initRecyclerView() {

        // Initialize the List to be used by the Adapter
        mGuideList = new ArrayList<>();

        // Init the Adapter and set the click response
        mAdapter = new GuideAdapter(new GuideAdapter.ClickHandler() {
            @Override
            public void onGuideClicked(Guide guide) {

                // Open the GuideDetailsActivity
                Intent intent = new Intent(getActivity(), GuideDetailsActivity.class);
                intent.putExtra(GUIDE_KEY, guide);

                startActivity(intent);
            }

            @Override
            public void onGuideLongClicked(Guide guide) {

            }
        });

        // Set the list that will be used by the Adapter
        mAdapter.setGuides(mGuideList);

        // Set the Adapter and the LayoutManager
        mBinding.favoritesRv.setAdapter(mAdapter);
        mBinding.favoritesRv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onConnected() {
        FirebaseDatabase.getInstance().goOnline();

        loadFavorites();
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();

        // Reset the List used by the Adapter so it displays fresh data
        if (mGuideList.size() > 0) {

            mAdapter.notifyItemRangeRemoved(0, mGuideList.size());
            mGuideList = new ArrayList<>();
            mAdapter.setGuides(mGuideList);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Generate the CursorLoader for the favorites in the database
        return new CursorLoader(
                getActivity(),
                GuideProvider.Guides.CONTENT_URI,
                null,
                GuideContract.GuideEntry.FAVORITE + " = ?",
                new String[] {"1"},
                GuideContract.GuideEntry.TRAIL_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Populate the Adapter with the database entries
        if (mGuideList.size() == 0 && data != null && data.moveToFirst()) {

            // Retrieve of list of FirebaseIds corresponding to the favorite Guides
            List<String> guideIdList = new ArrayList<>();
            do {
                guideIdList.add(Guide.createGuideFromCursor(data).firebaseId);
            } while (data.moveToNext());

            // Retrieve the Guides from Firebase Databse
            getGuides(guideIdList);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Loads the favorites for the user either from a local database if they do not have a Firebase
     * Account or the online database if they do
     */
    private void loadFavorites() {

        // Check whether the user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {

            // Load from local database
            loadFavoritesFromDatabase();
        } else {

            // Load from online
            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {

                    // Pass the Author data model to the Adapter so the Guides can set their
                    // favorite status appropriately
                    Author author = (Author) model;
                    mAdapter.setAuthor(author);

                    // Verify that the Author has a list of favorites
                    if (author.favorites == null) {
                        return;
                    }

                    // Retrieve the User's favorite'd Guides and sort them alphabetically by Trail
                    // name
                    List<Map.Entry<String, String>> entryList = new LinkedList<>(author.favorites.entrySet());
                    Collections.sort(entryList, new Comparator<Map.Entry<String, String>>() {
                        @Override
                        public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                            return o1.getValue().compareTo(o2.getValue());
                        }
                    });

                    // Init List of Guide FirebaseIds to pass to be retrieved from Firebase Database
                    List<String> guideIdList = new ArrayList<>();

                    // Add each key from the sorted List
                    for (Map.Entry<String, String> entry : entryList) {
                        guideIdList.add(entry.getKey());
                    }

                    // Retrieve the Guides from Firebase Database
                    getGuides(guideIdList);
                }
            });
        }
    }

    /**
     * Retrieves each Guide on the Author's list of favorite Guides
     *
     * @param guideIdList    List of FirebaseIds representing the favorite Guides to be retrieved
     *                         from Firebase Databse
     */
    private void getGuides(List<String> guideIdList) {

        if (guideIdList == null || guideIdList.size() == 0) {
            return;
        }

        // Iterate through the List and retrieve each Guide from Firebase
        for (String firebaseId : guideIdList) {

            // Get StorageReference for Guide
            final DatabaseReference guideRef = FirebaseDatabase.getInstance().getReference()
                    .child(GuideDatabase.GUIDES)
                    .child(firebaseId);

            guideRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    // Check that DataSnapshop is valid
                    if (dataSnapshot.exists()) {

                        // Create Guide from DataSnapshot
                        Guide guide = (Guide) FirebaseProviderUtils.getModelFromSnapshot(
                                DatabaseProvider.FirebaseType.GUIDE,
                                dataSnapshot);

                        // Add the Guide to the Adapter
                        mAdapter.addGuide(guide);
                    }

                    // Remove the Listener
                    guideRef.removeEventListener(this);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                    // Remove the Listener
                    guideRef.removeEventListener(this);
                }
            });
        }
    }

    /**
     * Loads the list of favorites from the database
     */
    private void loadFavoritesFromDatabase() {

        // Init the CursorLoader
        getActivity().getSupportLoaderManager().initLoader(FAVORITES_LOADER, null, this);
    }

    /**
     * Removes a Guide from the Adapter
     *
     * @param guide    Guide to be removed
     */
    public void removeGuideFromAdapter(Guide guide) {
        mAdapter.removeGuide(guide.firebaseId);
    }
}