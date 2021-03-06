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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import project.sherpa.BuildConfig;
import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.FragmentGuideDetailsBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Rating;
import project.sherpa.models.datamodels.Section;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.services.firebaseservice.QueryChangeListener;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.ui.activities.UserActivity;
import project.sherpa.ui.adapters.GuideDetailsAdapter;
import project.sherpa.ui.fragments.abstractfragments.ConnectivityFragment;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.utilities.MapUtils;
import project.sherpa.utilities.OfflineGuideManager;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.GUIDE;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.RATING;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.SECTION;

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
    private Author mUser;
    private GuideDetailsAdapter mAdapter;
    private MenuItem mCacheMenuItem;

    private QueryChangeListener<Rating> mUserRatingListener;

    /**
     * Factory for creating a GuideDetailsFragment for a specific Guide
     *
     * @param guideId    Guide whose details will be shown in the Fragment
     * @param authorId   FirebaseId of the Author of the Guide
     * @return A GuideDetailsFragment with a Bundle attached for displaying details for a Guide
     */
    public static GuideDetailsFragment newInstance(String guideId, String authorId) {
        // Init the Bundle that will be passed with the Fragment
        Bundle args = new Bundle();

        // Put the Guide from the signature into the Bundle
        args.putString(GUIDE_KEY, guideId);
        args.putString(AUTHOR_KEY, authorId);

        // Initialize the Fragment and attach the args
        GuideDetailsFragment fragment = new GuideDetailsFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_details, container, false);
        bindFirebaseProviderService(true);

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

        Timber.d("Initializing RecyclerView");
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

    public void onServiceConnected() {
        loadGuideFromFirebase();
    }

    /**
     * Loads all the information to display a Guide from Firebase
     */
    private void loadGuideFromFirebase() {

        // Get the guideId and authorId from the argument Bundle passed to the Fragment
        Bundle args = getArguments();
        String guideId = args.getString(GUIDE_KEY);
        String authorId = args.getString(AUTHOR_KEY);

        loadGuide(guideId);
        loadSections(guideId);
        loadAuthor(authorId);
        loadRatingForFirebaseUser(guideId);
        loadRatings(guideId);
    }

    /**
     * Loads a Guide from Firebase
     *
     * @param guideId    FirebaseId of the Guide to load
     */
    private void loadGuide(String guideId) {

        if (mGuide != null) return;

        ModelChangeListener<Guide> guideListener = new ModelChangeListener<Guide>(GUIDE, guideId) {
            @Override
            public void onModelReady(Guide model) {
                mGuide = model;
                mAdapter.addModel(mGuide);
                stopCacheIcon();

                mBinding.setVm(new GuideViewModel(getActivity(), mGuide));

                mService.unregisterModelChangeListener(this);
            }

            @Override
            public void onModelChanged() {

            }
        };

        mService.registerModelChangeListener(guideListener);
    }

    /**
     * Loads the author's profile from Firebase
     *
     * @param authorId    FirebaseId of the Author to load
     */
    private void loadAuthor(String authorId) {

        if (mAuthor != null) return;

        ModelChangeListener<Author> authorListener = new ModelChangeListener<Author>(AUTHOR, authorId) {
            @Override
            public void onModelReady(Author model) {
                mAuthor = model;
                mAdapter.addModel(mAuthor);
                stopCacheIcon();

                mService.unregisterModelChangeListener(this);
            }

            @Override
            public void onModelChanged() {

            }
        };

        mService.registerModelChangeListener(authorListener);
    }

    /**
     * Loads the Sections for a Guide from Firebase
     *
     * @param guideId    FirebaseId of the Guide to load Sections for
     */
    private void loadSections(final String guideId) {

        if (mSections != null) return;

        Query sectionQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.SECTIONS)
                .child(guideId)
                .orderByKey();

        QueryChangeListener<Section> sectionListener =
                new QueryChangeListener<Section>(SECTION, sectionQuery, guideId) {
                    @Override
                    public void onQueryChanged(Section[] models) {
                        mSections = models;
                        DataCache.getInstance().store(mSections);

                        for (Section section : mSections) {
                            mAdapter.addModel(section);
                        }

                        stopCacheIcon();

                        mService.unregisterQueryChangeListener(this);
                    }
                };

        mService.registerQueryChangeListener(sectionListener);
    }

    /**
     * Loads the Ratings for a Guide
     *
     * @param guideId    FirebaseId of the guide to load the ratings for
     */
    private void loadRatings(String guideId) {

        // Generate the Query for the Ratings for the Guide
        Query ratingsQuery = FirebaseDatabase.getInstance().getReference()
                .child(Rating.DIRECTORY)
                .child(guideId)
                .orderByChild(Rating.DATE_ADDED)
                .limitToLast(10);

        // Register the QueryChangeListener
        QueryChangeListener<Rating> ratingsListener = new QueryChangeListener<Rating>(RATING, ratingsQuery, guideId) {
            @Override
            public void onQueryChanged(Rating[] models) {
                if (models == null) return;

                // Add each Rating to the Adapter
                for (Rating rating : models) {
                    mAdapter.addModel(rating);
                }

                // Unregister the QueryChangeListener
                mService.unregisterQueryChangeListener(this);
            }
        };

        mService.registerQueryChangeListener(ratingsListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mUserRatingListener != null) mService.unregisterQueryChangeListener(mUserRatingListener);
    }

    /**
     * Loads the Rating that the user has written for the Guide being displayed if they have
     * written one. If the user has not given a Rating for this guide, then it loads a new Rating
     * to be filled by the user.
     *
     * @param guideId    FirebaseId of the Guide to load the corresponding Rating for
     */
    private void loadRatingForFirebaseUser(final String guideId) {

        // Check to ensure the user is logged in
        String authorId = getArguments().getString(AUTHOR_KEY);
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid().equals(authorId)) return;

        if (mUser == null) {

            // Download the user's profile
            ModelChangeListener<Author> userListener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
                @Override
                public void onModelReady(Author model) {

                    // Set the user and recall the function
                    if (model == null) return;
                    mUser = model;
                    mAdapter.setCurrentUser(mUser);

                    loadRatingForFirebaseUser(guideId);
                    mService.unregisterModelChangeListener(this);
                }

                @Override
                public void onModelChanged() {

                }
            };

            mService.registerModelChangeListener(userListener);
            return;
        }

        // Generate the Query for the Ratings written by the user for the guide
        Query ratingQuery = FirebaseDatabase.getInstance().getReference()
                .child(Rating.DIRECTORY)
                .child(guideId)
                .orderByChild(Rating.AUTHOR_ID)
                .equalTo(user.getUid());

        // Register the QueryChangeListener
        mUserRatingListener = new QueryChangeListener<Rating>(RATING, ratingQuery, user.getUid()) {
            @Override
            public void onQueryChanged(Rating[] models) {
                if (models == null || models.length == 0) {
                    // Has not been rated. Create a new Rating for the User to fill out
                    Rating rating = new Rating();

                    rating.setGuideId(mGuide.firebaseId);
                    rating.setGuideAuthorId(mGuide.authorId);
                    rating.setAuthorId(mUser.firebaseId);
                    rating.setAuthorName(mUser.name);

                    // Add the new Rating to the Adapter
                    mAdapter.addModel(rating);

                } else {
                    mAdapter.addModel(models[0]);
                }
            }
        };

        mService.registerQueryChangeListener(mUserRatingListener);
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
