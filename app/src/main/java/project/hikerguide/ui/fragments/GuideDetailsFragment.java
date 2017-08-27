package project.hikerguide.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.databinding.FragmentGuideDetailsBinding;
import project.hikerguide.files.GpxFile;
import project.hikerguide.files.ImageFile;
import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.UserActivity;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.ui.dialogs.ProgressDialog;
import project.hikerguide.utilities.ContentProviderUtils;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.MapUtils;
import project.hikerguide.utilities.OfflineGuideManager;
import timber.log.Timber;

import static project.hikerguide.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.Constants.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ConnectivityActivity.ConnectivityCallback {

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

        // Initialize the RecyclerView
        initRecyclerView();

        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).setConnectivityCallback(this);
        }

        // Check whether the Guide has been cached
        if (ContentProviderUtils.isModelInDatabase(getActivity(), mGuide)) {

            // Load the Guide from the database
            getActivity().getSupportLoaderManager().initLoader(LOADER_GUIDE, null, this);
            getActivity().getSupportLoaderManager().initLoader(LOADER_SECTION, null, this);
            getActivity().getSupportLoaderManager().initLoader(LOADER_AUTHOR, null, this);
        }

        // Show the menu
        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_guide_details, menu);

        mCacheMenuItem = menu.getItem(0);
        if (!ContentProviderUtils.isModelInDatabase(getActivity(), mGuide)) {
            mCacheMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_save));
        } else {
            mCacheMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_white));
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
                if (!ContentProviderUtils.isModelInDatabase(getActivity(), mGuide)) {
                    saveGuide();
                    animateCacheIcon();
                    item.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_white));
                } else {
                    deleteGuide();
                    animateCacheIcon();
                    item.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_save));
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
                }

                break;

            case LOADER_AUTHOR:

                // Move the Cursor to the first position
                if (data.moveToFirst()) {

                    // Create an Author from the data in the Cursor
                    mAuthor = Author.createAuthorFromCursor(data);

                    // Set the Author to the Adapter
                    mAdapter.addModel(mAuthor);
                }

                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onConnected() {
        FirebaseDatabase.getInstance().goOnline();

        if (!ContentProviderUtils.isModelInDatabase(getActivity(), mGuide) && mSections == null) {

            // Set the data for the Adapter
            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.GUIDE,
                    mGuide.firebaseId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            mGuide = (Guide) model;
                            mAdapter.addModel(mGuide);

                            stopCacheIcon();
                        }
                    });

            FirebaseProviderUtils.getSectionsForGuide(
                    mGuide.firebaseId,
                    new FirebaseProviderUtils.FirebaseArrayListener() {
                        @Override
                        public void onModelsReady(BaseModel[] models) {
                            mSections = (Section[]) models;
                            for (Section section : mSections) {
                                mAdapter.addModel(section);
                            }

                            stopCacheIcon();
                        }
                    });

            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.AUTHOR,
                    mGuide.authorId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {
                            mAuthor = (Author) model;
                            mAdapter.addModel(mAuthor);

                            stopCacheIcon();
                        }
                    });
        }
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();
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
                intent.putExtra(AUTHOR_KEY, author);

                startActivity(intent);
            }
        });

        // Setup the RecyclerView
        mBinding.setVm(new GuideViewModel(getActivity(), mGuide));
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.guideDetailsRv.setAdapter(mAdapter);
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
