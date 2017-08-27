package project.hikerguide.utilities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.files.GpxFile;
import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.ui.dialogs.ProgressDialog;

/**
 * Created by Alvin on 8/26/2017.
 */

public class OfflineGuideManager {

    // ** Member Variables ** //
    private Guide mGuide;
    private Section[] mSections;
    private Author mAuthor;

    private DownloadListener mListener;

    public OfflineGuideManager(@NonNull Guide guide, @NonNull Section[] sections, @NonNull Author author) {
        mGuide = guide;
        mSections = sections;
        mAuthor = author;
    }

    /**
     * Checks whether the Guide, Sections, and Author already exist in the local database
     *
     * @param context    Interface to global Context
     * @return True if all elements are in database. False otherwise.
     */
    public boolean isCachedOffline(Context context) {

        // Check if Guide is in database
        boolean isCached = ContentProviderUtils.isModelInDatabase(context, mGuide);

        // Iterate and check whether each Section is in the database
        for (Section section : mSections) {
            if (isCached) {
                isCached = ContentProviderUtils.isModelInDatabase(context, section);
            } else {
                break;
            }
        }

        // Check if the Author is the database
        if (isCached) {
            isCached = ContentProviderUtils.isModelInDatabase(context, mAuthor);
        }

        return isCached;
    }

    /**
     * Caches the Guide, Author, and Sections to the offline database, their files to internal
     * storage, and the corresponding Mapbox Tiles to internal storage
     *
     * @param activity    Interface to Activity for context and SupportFragmentManager
     * @param callback    The Callback to be triggered when downloading Mapbox Tiles
     */
    public void cache(AppCompatActivity activity, MapUtils.MapboxDownloadCallback callback) {

        // Ensure the guide isn't already cached
        if (!isCachedOffline(activity)) {
            initDownloadListener(activity, callback);
            cacheGuideFiles(activity);
        }
    }

    /**
     * Deletes a cached Guide, Author, and Sections from local the local database, their files
     * from local storage, and any associated Mapbox Tiles not being used by other cached Guides
     *
     * @param activity    Interface to Activity for context and SupportFragmentManager
     * @param callback    The Callback to be triggered when Mapbox Tiles have been deleted
     */
    public void delete(AppCompatActivity activity, MapUtils.MapboxDeleteCallback callback) {

        // Check to ensure the guide is cached
        if (isCachedOffline(activity)) {
            deleteCachedGuide(activity);
            deleteCachedFiles(activity);
            deleteMapboxTiles(activity, callback);
        }
    }

    /**
     * Sets up the DownloadListener to handle when downloads for files are complete
     *
     * @param activity    Interface to Activity for context and SupportFragmentManager
     * @param callback    Callback to be used when download Mapbox Tiles
     */
    private void initDownloadListener(AppCompatActivity activity, MapUtils.MapboxDownloadCallback callback) {

        // Setup the Dialog
        ProgressDialog dialog = new ProgressDialog();
        dialog.setCancelable(false);
        dialog.setTitle(activity.getString(R.string.progress_download_files_title));
        dialog.setIndeterminate(true);

        dialog.show(activity.getSupportFragmentManager(), null);

        // Init the DownloadListener
        if (mListener == null) mListener = new DownloadListener(activity, dialog, callback);
    }

    /**
     * Caches the Guide to the local database
     *
     * @param context    Interface to global Context
     */
    private void cacheGuide(Context context) {
        ContentProviderUtils.insertModel(context, mGuide);
        ContentProviderUtils.bulkInsertSections(context, mSections);
        ContentProviderUtils.insertModel(context, mAuthor);
    }

    /**
     * Saves the files associated with a Guide to Internal Storage
     *
     * @param context    Interface to global Context
     */
    private void cacheGuideFiles(Context context) {

        downloadFile(mGuide.generateGpxFileForDownload(context));
        downloadFile(mGuide.generateImageFileForDownload(context));
        downloadFile(mAuthor.generateImageFileForDownload(context));

        for (Section section : mSections) {

            // Download image for any Section with an image
            if (section.hasImage) {
                downloadFile(section.generateImageFileForDownload(context));
            }
        }
    }

    /**
     * Saves the Mapbox Map tiles required to show the trail for the Guide
     *
     * @param activity    Interface to Activity for context and SupportFragmentManager
     * @param callback    Callback to inform the calling Object of the download status
     */
    private void saveMapboxTiles(final AppCompatActivity activity, final MapUtils.MapboxDownloadCallback callback) {

        // Initialize the Progress Dialog
        final ProgressDialog dialog = new ProgressDialog();
        dialog.setTitle(activity.getString(R.string.progress_download_map_title));
        dialog.setCancelable(false);

        dialog.show(activity.getSupportFragmentManager(), null);

        // Save the map tiles for offline use
        MapUtils.saveMapboxOffline(activity, mGuide, new MapUtils.MapboxDownloadCallback() {
            @Override
            public void onDownloadComplete() {

                // Show a Toast to inform the user of successful download
                Toast.makeText(activity,
                        activity.getString(R.string.mapbox_downloaded),
                        Toast.LENGTH_LONG)
                        .show();

                // Dismiss the Dialog
                dialog.dismiss();

                callback.onDownloadComplete();
            }

            @Override
            public void onUpdateProgress(double progress) {

                // Update the progress for the Dialog
                dialog.updateProgress((int) progress);

                callback.onUpdateProgress(progress);
            }
        });
    }

    private void deleteCachedGuide(Context context) {
        ContentProviderUtils.deleteModel(context, mGuide);
        ContentProviderUtils.deleteSectionsForGuide(context, mGuide);

        if (ContentProviderUtils.getGuideCountForAuthor(context, mAuthor) == 0) {
            ContentProviderUtils.deleteModel(context, mAuthor);
        }
    }

    private void deleteCachedFiles(Context context) {
        mGuide.generateGpxFileForDownload(context).delete();
        mGuide.generateImageFileForDownload(context).delete();

        for (Section section : mSections) {
            if (section.hasImage) {
                section.generateImageFileForDownload(context).delete();
            }
        }

        if (ContentProviderUtils.isModelInDatabase(context, mAuthor)) {
            mAuthor.generateImageFileForDownload(context).delete();
        }
    }

    private void deleteMapboxTiles(final AppCompatActivity activity, final MapUtils.MapboxDeleteCallback callback) {

        // Show the ProgressDialog
        final ProgressDialog dialog = new ProgressDialog();
        dialog.setTitle(activity.getString(R.string.progress_delete_map_title));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        dialog.show(activity.getSupportFragmentManager(), null);

        // Delete the downloaded map tiles
        MapUtils.deleteMapboxOffline(activity, mGuide, new MapUtils.MapboxDeleteCallback() {
            @Override
            public void onComplete() {

                // Dismiss the Dialog
                dialog.dismiss();

                // Notify user of success
                Toast.makeText(activity,
                        activity.getString(R.string.mapbox_deleted),
                        Toast.LENGTH_LONG)
                        .show();

                callback.onComplete();
            }
        });
    }

    /**
     * Saves a BaseFile to Internal Storage
     *
     * @param file    BaseFile to save the download to
     */
    private void downloadFile(final BaseFile file) {

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

    private class DownloadListener {
        // ** Member Variables ** //
        private AppCompatActivity mActivity;
        private ProgressDialog mDialog;
        private MapUtils.MapboxDownloadCallback mCallback;
        private List<StorageTask<FileDownloadTask.TaskSnapshot>> mTaskList;

        DownloadListener(AppCompatActivity activity, ProgressDialog dialog, MapUtils.MapboxDownloadCallback callback) {
            mActivity = activity;
            mDialog = dialog;
            mCallback = callback;
        }

        /**
         * Adds a download task to be monitored
         *
         * @param task    Download task to be monitored
         */
        void addDownloadTask(StorageTask<FileDownloadTask.TaskSnapshot> task) {

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
        void removeDownloadTask(StorageTask<FileDownloadTask.TaskSnapshot> task) {

            // Remove the task from the List
            mTaskList.remove(task);

            if (mTaskList.size() == 0) {

                // Dismiss the dialog
                mDialog.dismiss();

                // If there are no more tasks left, then begin saving the guide to the database
                // The files must be downloaded first so that when the guide is saved to the
                // database the Uri of the files can also be saved to the database.
                cacheGuide(mActivity);
                saveMapboxTiles(mActivity, mCallback);
            }
        }
    }
}
