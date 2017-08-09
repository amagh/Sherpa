package project.hikerguide.ui.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import project.hikerguide.ui.activities.GuideDetailsActivity;
import project.hikerguide.ui.activities.UserActivity;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.utilities.ContentProviderUtils;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.MapUtils;
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
    private DownloadListener mListener;

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

        // Show the menu
        setHasOptionsMenu(true);

        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_guide_details, menu);
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
                } else {
                    deleteGuide();
                }

                return true;
        }

        return false;
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
            if (cursor.moveToFirst()) {

                // Guide is in database
                return true;
            }

            // Close the Cursor
            cursor.close();
        }

        return false;
    }

    /**
     * Saves all the Files associated with a Guide
     * e.g. Image files & GPX files
     */
    private void saveFilesForGuide() {

        // Init the DownloadListener
        if (mListener == null) {
            mListener = new DownloadListener();
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

        // Save the map tiles for offline use
        MapUtils.saveMapboxOffline(getActivity(), mGuide, new MapUtils.MapboxDownloadCallback() {
            @Override
            public void onDownloadComplete() {
                Toast.makeText(getActivity(),
                        "Map downloaded!",
                        Toast.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onUpdateProgress(double progress) {
//                Toast.makeText(getActivity(),
//                        String.format(Locale.getDefault(), "%f percent", progress),
//                        Toast.LENGTH_SHORT)
//                        .show();

                // TODO: Implement progress bar
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

        // Delete the downloaded map tiles
        MapUtils.deleteMapboxOffline(getActivity(), mGuide);
    }

    private class DownloadListener {
        // ** Member Variables ** //
        private List<StorageTask<FileDownloadTask.TaskSnapshot>> mTaskList;

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
            }
        }
    }
}
