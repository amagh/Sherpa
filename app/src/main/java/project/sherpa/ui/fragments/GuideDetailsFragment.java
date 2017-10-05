package project.sherpa.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import project.sherpa.BuildConfig;
import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.FragmentGuideDetailsBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Section;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.ui.activities.UserActivity;
import project.sherpa.ui.adapters.GuideDetailsAdapter;
import project.sherpa.ui.fragments.abstractfragments.ConnectivityFragment;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.utilities.MapUtils;
import project.sherpa.utilities.OfflineGuideManager;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsFragment extends ConnectivityFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // ** Constants ** //
    private static final int LOADER_GUIDE       = 3564;
    private static final int LOADER_SECTION     = 1654;
    private static final int LOADER_AUTHOR      = 6188;

    // ** Member Variables ** //
    private FragmentGuideDetailsBinding mBinding;
    private Guide mGuide;
    private Section[] mSections;
    private Author mAuthor;
    private GuideDetailsAdapter mAdapter;
    private MenuItem mCacheMenuItem;

    public GuideDetailsFragment() {}

    /**
     * Factory for creating a GuideDetailsFragment for a specific Guide
     *
     * @param guideId    Guide whose details will be shown in the Fragment
     * @return A GuideDetailsFragment with a Bundle attached for displaying details for a Guide
     */
    public static GuideDetailsFragment newInstance(String guideId) {
        // Init the Bundle that will be passed with the Fragment
        Bundle args = new Bundle();

        // Put the Guide from the signature into the Bundle
        args.putString(GUIDE_KEY, guideId);

        // Initialize the Fragment and attach the args
        GuideDetailsFragment fragment = new GuideDetailsFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_details, container, false);

        ((GuideDetailsActivity) getActivity()).setSupportActionBar(mBinding.guideDetailsTb);

        // Initialize the RecyclerView
        initRecyclerView();

        if (getArguments() != null && getArguments().containsKey(GUIDE_KEY)) {

            String guideId = getArguments().getString(GUIDE_KEY);

            // Attempt to get the data models from cache
            getCachedData(guideId);

            if (mGuide == null) {

                // Data cache did not contain the Guide, instantiate a new Guide and load data from
                // other sources
                mGuide = new Guide();
                mGuide.firebaseId = guideId;
            }
        }

        if (mGuide.trailName != null) {

            // Set the ViewModel for the Fragment
            mBinding.setVm(new GuideViewModel(getActivity(), mGuide));
        }

        // Load ads if applicable
        loadAdViewModel(mBinding);

        // Show the menu
        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_guide_details, menu);

        mCacheMenuItem = menu.getItem(1);
        if (!ContentProviderUtils.isGuideCachedInDatabase(getActivity(), mGuide)) {
            mCacheMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_save));
        } else {
            mCacheMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_white));
        }

        if (ContentProviderUtils.isGuideFavorite(getActivity(), mGuide)) {
            menu.getItem(0).setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_white));
        } else {
            menu.getItem(0).setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_border_white));
        }

        // Prevent the User from saving the Guide before all elements have been loaded
        if (mGuide == null || mSections == null || mAuthor == null) animateCacheIcon();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_map:
                ((GuideDetailsActivity) getActivity()).switchPage(1);
                return true;

            case R.id.menu_save:

                // In free version, only allow a single cached guide at a time
                if (BuildConfig.FLAVOR.equals("free") && ContentProviderUtils.containsCachedGuide(getActivity())) {
                    Toast.makeText(
                            getActivity(),
                            getString(R.string.toast_free_cached_limit),
                            Toast.LENGTH_LONG)
                            .show();

                    return true;
                }

                if (!ContentProviderUtils.isGuideCachedInDatabase(getActivity(), mGuide)) {
                    saveGuide();
                    animateCacheIcon();
                    item.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_white));
                } else {
                    deleteGuide();
                    animateCacheIcon();
                    item.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_save));
                }

                return true;

            case R.id.menu_favorite:

                // Check if the user is logged in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                // Toggle the favorite status of the Guide
                mGuide.setFavorite(!mGuide.isFavorite());

                if (user != null) {
                    FirebaseProviderUtils.toggleFirebaseFavorite(mGuide);
                }

                ContentProviderUtils.toggleFavorite(getActivity(), mGuide);

                // Update the icon
                if (ContentProviderUtils.isGuideFavorite(getActivity(), mGuide)) {
                    item.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_white));
                } else {
                    item.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_star_border_white));
                }

                return true;
        }

        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Variables for CursorLoader
        Uri uri = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;

        // Set the variables based on the Loader's id
        switch (id) {
            case LOADER_GUIDE:
                uri = GuideProvider.Guides.CONTENT_URI;
                selection = GuideContract.GuideEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {mGuide.firebaseId};

                stopCacheIcon();
                break;

            case LOADER_SECTION:
                uri = GuideProvider.Sections.CONTENT_URI;
                selection = GuideContract.SectionEntry.GUIDE_ID + " = ?";
                selectionArgs = new String[] {mGuide.firebaseId};
                sortOrder = GuideContract.SectionEntry.SECTION + " ASC";

                stopCacheIcon();
                break;

            case LOADER_AUTHOR:
                uri = GuideProvider.Authors.CONTENT_URI;
                selection = GuideContract.AuthorEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {mGuide.authorId};

                stopCacheIcon();
                break;
        }

        return new CursorLoader(
                getActivity(),
                uri,
                null,
                selection,
                selectionArgs,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Check the Cursor has valid data to be loaded to prevent crashing if there is none
        if (data.getCount() < 1) {
            return;
        }

        // Load the data into the Adapter based on the Loader's id
        switch (loader.getId()) {
            case LOADER_GUIDE:

                // Move the Cursor to the first position
                if (data.moveToFirst()) {

                    // Create a Guide from the data in the Cursor
                    mGuide = Guide.createGuideFromCursor(data);

                    // Set the Guide to the Adapter
                    mAdapter.addModel(mGuide);

                    // Set the ViewModel for the Fragment
                    mBinding.setVm(new GuideViewModel(getActivity(), mGuide));

                    stopCacheIcon();
                }

                break;

            case LOADER_SECTION:

                if (data.getCount() > 0) {
                    // Init mSections
                    mSections = new Section[data.getCount()];

                    // Populate the Array using the data from the Cursor
                    for (int i = 0; i < data.getCount(); i++) {
                        data.moveToPosition(i);
                        mSections[i] = Section.createSectionFromCursor(data);

                        // Pass mSections to the Adapter
                        mAdapter.addModel(mSections[i]);
                    }

                    stopCacheIcon();
                }

                break;

            case LOADER_AUTHOR:

                // Move the Cursor to the first position
                if (data.moveToFirst()) {

                    // Create an Author from the data in the Cursor
                    mAuthor = Author.createAuthorFromCursor(data);

                    // Set the Author to the Adapter
                    mAdapter.addModel(mAuthor);

                    stopCacheIcon();
                }

                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onConnected() {
        super.onConnected();

        if (mGuide.authorId != null && mSections != null && mAuthor != null) return;

        if (!ContentProviderUtils.isGuideCachedInDatabase(getActivity(), mGuide) && mSections == null) {
            getDataFromFirebase();
        } else {
            initLoaders();
        }
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();

        if (mGuide.authorId != null && mSections != null && mAuthor != null) return;

        if (ContentProviderUtils.isGuideCachedInDatabase(getActivity(), mGuide)) {
            initLoaders();
        }
    }

    /**
     * Sets up the RecyclerView, Adapter, and LayoutManager required to make it work
     */
    private void initRecyclerView() {
        // Setup the Adapter
        mAdapter = new GuideDetailsAdapter((GuideDetailsActivity) getActivity(), new GuideDetailsAdapter.ClickHandler() {
            @Override
            public void onClickAuthor(Author author) {
                Intent intent = new Intent(getActivity(), UserActivity.class);
                intent.putExtra(AUTHOR_KEY, author.firebaseId);

                startActivity(intent);
            }
        });

        mAdapter.setHasStableIds(true);

        // Setup the RecyclerView
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.guideDetailsRv.setAdapter(mAdapter);
    }

    /**
     * Initializes the CursorLoaders to retrieve the data to populate the Adapter from the database
     */
    private void initLoaders() {
        // Load the Guide from the database
        getActivity().getSupportLoaderManager().initLoader(LOADER_GUIDE, null, this);
        getActivity().getSupportLoaderManager().initLoader(LOADER_SECTION, null, this);
        getActivity().getSupportLoaderManager().initLoader(LOADER_AUTHOR, null, this);
    }

    /**
     * Retrieves the required data models to populate the Adapter from cache if they are stored
     *
     * @param guideId    The FirebaseId of the Guide to be retrieved from DataCache
     */
    private void getCachedData(String guideId) {

        // Retrieve the Guide from cache
        Guide guide = (Guide) DataCache.getInstance().get(guideId);

        // Check to ensure that the data returned from the cache is not null
        if (guide == null) return;

        // Set mGuide to the retrieved Guide
        mGuide = guide;

        // Retrieve the Sections and Author from cache
        mSections = DataCache.getInstance().getSections(guideId);
        mAuthor = (Author) DataCache.getInstance().get(mGuide.authorId);

        // Add every non-null data model to the Adapter
        if (mGuide != null) mAdapter.addModel(mGuide);

        if (mSections != null) {
            for (Section section : mSections) {
                mAdapter.addModel(section);
            }
        }

        if (mAuthor != null) mAdapter.addModel(mAuthor);
    }

    /**
     * Downloads the data models to populate the Adapter from Firebase Database
     */
    private void getDataFromFirebase() {
        final DataCache cache = DataCache.getInstance();

        // Set the data for the Adapter and store the data models in the cache
        if (mGuide.authorId == null) {
            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.GUIDE,
                    mGuide.firebaseId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            mGuide = (Guide) model;
                            mAdapter.addModel(mGuide);

                            cache.store(mGuide);

                            mBinding.setVm(new GuideViewModel(getActivity(), mGuide));

                            // Retrieve author info if needed
                            if (mAuthor == null) {
                                getAuthorFromFirebase(mGuide.authorId);
                            }

                            stopCacheIcon();
                        }
                    });
        } else if (mAuthor == null) {

            // Get the Author info from Firebase
            getAuthorFromFirebase(mGuide.authorId);
        }

        if (mSections == null) {

            FirebaseProviderUtils.getSectionsForGuide(
                    mGuide.firebaseId,
                    new FirebaseProviderUtils.FirebaseArrayListener() {
                        @Override
                        public void onModelsReady(BaseModel[] models) {
                            mSections = (Section[]) models;
                            for (Section section : mSections) {
                                mAdapter.addModel(section);
                            }

                            cache.store(mSections);

                            stopCacheIcon();
                        }
                    });
        }
    }

    /**
     * Retrieves Author information from Firebase and adds it to the Adapter
     *
     * @param authorId    The FirebaseId of the author to be retrieved from Firebase
     */
    private void getAuthorFromFirebase(String authorId) {
        FirebaseProviderUtils.getModel(
                FirebaseProviderUtils.FirebaseType.AUTHOR,
                authorId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {
                        mAuthor = (Author) model;
                        mAdapter.addModel(mAuthor);

                        DataCache.getInstance().store(mAuthor);

                        stopCacheIcon();
                    }
                });
    }

    /**
     * Replaces the save icon with an indeterminate ProgressBar to inform the user of background
     * actions.
     */
    private void animateCacheIcon() {

        // Inflate the View
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ProgressBar progressBar = (ProgressBar) inflater.inflate(R.layout.menu_progress_actionview, null);

        // Change the color of the ProgressBar to white
        progressBar.getIndeterminateDrawable()
                .setColorFilter(
                        ContextCompat.getColor(getActivity(), android.R.color.white),
                        PorterDuff.Mode.SRC_IN);

        // Set the ActionView of the menu icon
        mCacheMenuItem.setActionView(progressBar);
    }

    /**
     * Removes the ProgressBar from the ActionBar replacing the save icon
     */
    private void stopCacheIcon() {

        // Only stop the loading icon once all required items have been loaded
        if (mGuide == null || mSections == null || mAuthor == null) return;

        // Remove the ActionView of the menu icon
        if (mCacheMenuItem != null) mCacheMenuItem.setActionView(null);
    }

    /**
     * Saves the Guide to be used offline
     */
    private void saveGuide() {

        // Init the OfflineGuideManager
        OfflineGuideManager manager = new OfflineGuideManager(mGuide, mSections, mAuthor);

        // Cache the Guide to local storage
        manager.cache((GuideDetailsActivity) getActivity(), new MapUtils.MapboxDownloadCallback() {
            @Override
            public void onDownloadComplete() {
                stopCacheIcon();
            }

            @Override
            public void onUpdateProgress(double progress) {

            }
        });
    }

    /**
     * Deletes the guide from local storage
     */
    private void deleteGuide() {
        OfflineGuideManager manager = new OfflineGuideManager(mGuide, mSections, mAuthor);
        manager.delete((GuideDetailsActivity) getActivity(), new MapUtils.MapboxDeleteCallback() {
            @Override
            public void onComplete() {
                stopCacheIcon();
            }
        });
    }
}
