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
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.activities.ConnectivityActivity;
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.UserActivity;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.ui.dialogs.ProgressDialog;
import project.hikerguide.utilities.ContentProviderUtils;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.MapUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;

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
    private DownloadListener mListener;
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

        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).setConnectivityCallback(this);
        }

        // Check whether the Guide has been cached
        if (isGuideCached()) {

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
        if (!isGuideCached()) {
            mCacheMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_save));
        } else {
            mCacheMenuItem.setIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_white));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_map:
                ((GuideDetailsActivity) getActivity()).switchPage(1);
                return true;

            case R.id.menu_save:
                if (!isGuideCached()) {
                    saveFilesForGuide();
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

                break;

            case LOADER_SECTION:
                uri = GuideProvider.Sections.CONTENT_URI;
                selection = GuideContract.SectionEntry.GUIDE_ID + " = ?";
                selectionArgs = new String[] {mGuide.firebaseId};
                sortOrder = GuideContract.SectionEntry.SECTION + " ASC";

                break;

            case LOADER_AUTHOR:
                uri = GuideProvider.Authors.CONTENT_URI;
                selection = GuideContract.AuthorEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {mGuide.authorId};

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
                    mAdapter.setGuide(mGuide, (GuideDetailsActivity) getActivity());
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
                    }

                    // Pass mSections to the Adapter
                    mAdapter.setSections(mSections);
                }

                break;

            case LOADER_AUTHOR:

                // Move the Cursor to the first position
                if (data.moveToFirst()) {

                    // Create an Author from the data in the Cursor
                    mAuthor = Author.createAuthorFromCursor(data);

                    // Set the Author to the Adapter
                    mAdapter.setAuthor(mAuthor);
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

        if (!isGuideCached() && mSections == null) {

            // Set the data for the Adapter
            getGuideFromFirebase();
            getSectionsFromFirebase();
            getAuthorFromFirebase();
        }
    }

    @Override
    public void onDisconnected() {
        FirebaseDatabase.getInstance().goOffline();
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

        // Remove the ActionView of the menu icon
        mCacheMenuItem.setActionView(null);
    }

    /**
     * Loads the Guide from Firebase to ensure the data is fresh
     */
    private void getGuideFromFirebase() {

        // Get Reference for Guide
        final DatabaseReference guideRef = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .child(mGuide.firebaseId);

        guideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Set the memvar to the retreived Guide
                mGuide = (Guide) FirebaseProviderUtils.getModelFromSnapshot(
                        DatabaseProvider.FirebaseType.GUIDE,
                        dataSnapshot);

                // Add the Guide to the Adapter
                mAdapter.setGuide(mGuide, ((GuideDetailsActivity) getActivity()));

                // Remove Listener
                guideRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                guideRef.removeEventListener(this);
            }
        });
    }

    /**
     * Loads the corresponding Sections for the Guide from FirebaseDatabase
     */
    private void getSectionsFromFirebase() {

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
    private void getAuthorFromFirebase() {

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

    /**
     * Checks whether the Guide is already saved in the database
     *
     * @return True if Guide exists in the database. False otherwise.
     */
    private boolean isGuideCached() {

        // Query the database to see if the Guide exists in the database
        Cursor cursor = getActivity().getContentResolver().query(
                GuideProvider.Guides.CONTENT_URI,
                null,
                GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                new String[] {mGuide.firebaseId},
                null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {

                    // Guide is in database
                    return true;
                }
            } finally {
                // Close the Cursor
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Saves all the Files associated with a Guide
     * e.g. Image files & GPX files
     */
    private void saveFilesForGuide() {

        ProgressDialog dialog = new ProgressDialog();
        dialog.setCancelable(false);
        dialog.setTitle(getActivity().getString(R.string.progress_download_files_title));
        dialog.setIndeterminate(true);

        dialog.show(getActivity().getSupportFragmentManager(), null);

        // Init the DownloadListener
        if (mListener == null) {
            mListener = new DownloadListener(dialog);
        }

        // Save the GPX and Image associated with the Guide
        if (mGuide != null) {
            saveFile(mGuide.generateGpxFileForDownload(getActivity()));
            saveFile(mGuide.generateImageFileForDownload(getActivity()));
        }

        // Save the image for the Author
        if (mAuthor != null) {
            ImageFile file = mAuthor.generateImageFileForDownload(getActivity());

            // If the Author's image has previously been saved, then do nothing
            if (!file.exists()) {
                saveFile(file);
            }
        }

        // Iterate through the Sections and save any images associated with them
        if (mSections != null && mSections.length > 0) {

            for (Section section : mSections) {
                if (section.hasImage) {
                    saveFile(section.generateImageFileForDownload(getActivity()));
                }
            }
        }
    }

    /**
     * Saves a BaseFile to Internal Storage
     *
     * @param file    BaseFile to save the download to
     */
    private void saveFile(final BaseFile file) {

        // Generate a StorageTask for the download
        StorageReference reference = FirebaseStorage.getInstance().getReference();
        StorageTask<FileDownloadTask.TaskSnapshot> task =
                FirebaseProviderUtils.getReferenceForFile(reference, file)
                        .getFile(file);

        // Add the Task to the Listener
        mListener.addDownloadTask(task);

        task.addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                // Remove the Task from the Listener
                mListener.removeDownloadTask(taskSnapshot.getTask());

                if (file instanceof GpxFile) {
                    mGuide.setGpxUri(file);
                }
            }
        });
    }

    /**
     * Saves a Guide and all related Sections/Author in the database so it can be accessed offline
     */
    private void saveGuide() {

        // Save the data models to the database
        if (mGuide != null) {
            ContentProviderUtils.insertModel(getActivity(), mGuide);
        }

        if (mAuthor != null) {
            ContentProviderUtils.insertModel(getActivity(), mAuthor);
        }

        if (mSections != null && mSections.length > 0) {
            ContentProviderUtils.bulkInsertSections(getActivity(), mSections);
        }

        // Show a ProgressDialog to inform the user of the progress of the map download
        final ProgressDialog dialog = new ProgressDialog();

        // Set the Title for the Dialog
        dialog.setTitle(getActivity().getString(R.string.progress_download_map_title));

        // Prevent user from closing the Dialog when clicking outside
        dialog.setCancelable(false);

        dialog.show(getActivity().getSupportFragmentManager(), null);

        // Save the map tiles for offline use
        MapUtils.saveMapboxOffline(getActivity(), mGuide, new MapUtils.MapboxDownloadCallback() {
            @Override
            public void onDownloadComplete() {
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.mapbox_downloaded),
                        Toast.LENGTH_LONG)
                        .show();

                dialog.dismiss();

                // Reset the ActionBar icon
                stopCacheIcon();
            }

            @Override
            public void onUpdateProgress(double progress) {

                // Update the ProgressBar
                dialog.updateProgress((int) progress);
            }
        });
    }


    /**
     * Deletes the Guide, Sections, and Author from the database
     */
    private void deleteGuide() {

        // Delete the Guide from the database
        getActivity().getContentResolver().delete(
                GuideProvider.Guides.CONTENT_URI,
                GuideContract.GuideEntry.FIREBASE_ID + " = ?",
                new String[] {mGuide.firebaseId});

        // Delete the associated GPX and image files
        mGuide.generateGpxFileForDownload(getActivity()).delete();
        mGuide.generateImageFileForDownload(getActivity()).delete();

        // Delete the sections from the database
        getActivity().getContentResolver().delete(
                GuideProvider.Sections.CONTENT_URI,
                GuideContract.SectionEntry.GUIDE_ID + " = ?",
                new String[] {mGuide.firebaseId});

        // Delete any localled saved image files
        for (Section section : mSections) {
            if (section.hasImage) {
                section.generateImageFileForDownload(getActivity()).delete();
            }
        }

        // Query the database to see if any other saved guides are authored by the same Author
        Cursor cursor = getActivity().getContentResolver().query(
                GuideProvider.Guides.CONTENT_URI,
                null,
                GuideContract.GuideEntry.AUTHOR_ID + " = ?",
                new String[] {mAuthor.firebaseId},
                null);

        // Check that the Cursor is valid
        if (cursor != null) {

            // Check the count of the Cursor
            if (cursor.getCount() == 0) {

                // No other Guides with same Author. Delete Author from database
                getActivity().getContentResolver().delete(
                        GuideProvider.Authors.CONTENT_URI,
                        GuideContract.AuthorEntry.FIREBASE_ID + " = ?",
                        new String[] {mAuthor.firebaseId});

                mAuthor.generateImageFileForDownload(getActivity()).delete();
            }

            // Close the Cursor
            cursor.close();
        }

        // Show the ProgressDialog
        final ProgressDialog dialog = new ProgressDialog();
        dialog.setTitle(getActivity().getString(R.string.progress_delete_map_title));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        dialog.show(getActivity().getSupportFragmentManager(), null);

        // Delete the downloaded map tiles
        MapUtils.deleteMapboxOffline(getActivity(), mGuide, new MapUtils.MapboxDeleteCallback() {
            @Override
            public void onComplete() {

                // Dismiss the Dialog
                dialog.dismiss();

                // Notify user of success
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.mapbox_deleted),
                        Toast.LENGTH_LONG)
                        .show();

                // Reset the ActionBar icon
                stopCacheIcon();
            }
        });
    }

    private class DownloadListener {
        // ** Member Variables ** //
        private ProgressDialog mDialog;
        private List<StorageTask<FileDownloadTask.TaskSnapshot>> mTaskList;


        public DownloadListener(ProgressDialog dialog) {
            mDialog = dialog;
        }

        /**
         * Adds a download task to be monitored
         *
         * @param task    Download task to be monitored
         */
        public void addDownloadTask(StorageTask<FileDownloadTask.TaskSnapshot> task) {

            // Init the List to keep track of tasks
            if (mTaskList == null) {
                mTaskList = new ArrayList<>();
            }

            // Add the task to the List
            mTaskList.add(task);
        }

        /**
         * Removes a download task as it finishes
         *
         * @param task    The task to be removed
         */
        public void removeDownloadTask(StorageTask<FileDownloadTask.TaskSnapshot> task) {

            // Remove the task from the List
            mTaskList.remove(task);

            if (mTaskList.size() == 0) {

                // If there are no more tasks left, then begin saving the guide to the database
                // The files must be downloaded first so that when the guide is saved to the
                // database the Uri of the files can also be saved to the database.
                saveGuide();

                mDialog.dismiss();
            }
        }
    }
}
