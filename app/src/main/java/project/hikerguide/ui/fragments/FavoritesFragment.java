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
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.databinding.FragmentFavoritesBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.MainActivity;
import project.hikerguide.ui.adapters.GuideAdapter;
import project.hikerguide.utilities.DataCache;
import project.hikerguide.utilities.FirebaseProviderUtils;

import static project.hikerguide.utilities.Constants.IntentKeys.GUIDE_KEY;

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

        ((MainActivity) getActivity()).setSupportActionBar(mBinding.toolbar);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_favorites));

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
                intent.putExtra(GUIDE_KEY, guide.firebaseId);

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

        if (mGuideList == null || mGuideList.size() == 0) {
            loadUser();
        } else {
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        }
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
        if (mGuideList.size() == 0 && data != null) {
            if (data.moveToFirst()) {

                // Retrieve of list of FirebaseIds corresponding to the favorite Guides
                List<String> guideIdList = new ArrayList<>();
                do {
                    guideIdList.add(Guide.createGuideFromCursor(data).firebaseId);
                } while (data.moveToNext());

                // Hide ProgressBar
                mBinding.favoritesPb.setVisibility(View.GONE);

                // Retrieve the Guides from Firebase Databse
                getGuides(guideIdList);
            } else {
                showEmptyText();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Loads the favorites for the user either from a local database if they do not have a Firebase
     * Account or the online database if they do
     */
    private void loadUser() {

        // Check whether the user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {

            // Load from local database
            loadFavoritesFromDatabase();
        } else {

            Author author = (Author) DataCache.getInstance().get(user.getUid());

            if (author != null) {

                // Load the user's favorites
                loadFavorites(author);
            }

            // Load from online
            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {

                    // Load the user's favorites
                    loadFavorites((Author) model);
                }
            });
        }
    }

    /**
     * Retrieves the user's favorite'd guides from Firebase
     *
     * @param author    User to retrieve favorite items for
     */
    private void loadFavorites(Author author) {

        // Pass the Author data model to the Adapter so the Guides can set their
        // favorite status appropriately
        mAdapter.setAuthor(author);

        // Verify that the Author has a list of favorites
        if (author.favorites == null) {
            showEmptyText();

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

        if (guideIdList.size() == 0) {
            showEmptyText();
        }
    }

    /**
     * Hides the ProgressBar and shows the text indicating there are not favorite items to be
     * displayed
     */
    private void showEmptyText() {
        mBinding.favoritesPb.setVisibility(View.GONE);
        mBinding.favoritesEmptyTv.setVisibility(View.VISIBLE);
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

            // Check to see if the Guide exists in cache
            Guide guide = (Guide) DataCache.getInstance().get(firebaseId);

            if (guide != null) {

                // Add the Guide to the Adapter
                mAdapter.addGuide(guide);

                // Hide ProgressBar
                mBinding.favoritesPb.setVisibility(View.GONE);
            } else {

                // Guide not in cache, download from Firebase Database
                FirebaseProviderUtils.getModel(
                        FirebaseProviderUtils.FirebaseType.GUIDE,
                        firebaseId,
                        new FirebaseProviderUtils.FirebaseListener() {
                            @Override
                            public void onModelReady(BaseModel model) {

                                // Add the Guide to the Adapter
                                mAdapter.addGuide((Guide) model);

                                // Store the Model in DataCache
                                DataCache.getInstance().store(model);

                                // Hide ProgressBar
                                mBinding.favoritesPb.setVisibility(View.GONE);
                            }
                        });
            }
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

        if (mGuideList.size() == 0) {
            showEmptyText();
        }
    }
}
