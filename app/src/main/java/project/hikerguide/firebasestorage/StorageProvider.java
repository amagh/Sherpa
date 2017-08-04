package project.hikerguide.firebasestorage;

import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.utilities.StorageProviderUtils;

import static project.hikerguide.firebasestorage.StorageProvider.FirebaseFileType.GPX_FILE;
import static project.hikerguide.firebasestorage.StorageProvider.FirebaseFileType.IMAGE_FILE;
import static project.hikerguide.utilities.StorageProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.StorageProviderUtils.JPEG_EXT;

/**
 * The Provider that will be used to interface with Firebase Storage.
 *
 * Always access these methods on a separate thread. The thread accessing these methods will be
 * blocked until the download/upload completes.
 *
 * * * * * * * * * * * * * * * * DO NOT USE ON UI THREAD * * * * * * * * * * * * * * * * * * * * * *
 */

public class StorageProvider {
    // ** Constants ** //
    @IntDef({IMAGE_FILE, GPX_FILE})
    public @interface FirebaseFileType {
        int IMAGE_FILE = 0;
        int GPX_FILE = 1;
    }

    // ** Member Variables ** //
    private static StorageProvider sProvider;
    private StorageReference mStorage;

    private StorageProvider() {
        if (mStorage == null) {
            mStorage = FirebaseStorage.getInstance().getReference();
        }
    }

    public static synchronized StorageProvider getInstance() {
        if (sProvider == null) {
            sProvider = new StorageProvider();
        }

        return sProvider;
    }

    /**
     * Uploads ImageFiles to the Firebase Storage
     *
     * @param file      The File to be uploaded
     */
    public boolean uploadFile(BaseFile file) {
        // Init a StorageListener that will return the status of the operation
        final StorageListener listener = new StorageListener();

        // Get a reference to the location it will be stored using the ImageFile's firebaseId
        StorageReference ref = StorageProviderUtils.getReferenceForFile(mStorage, file);

        try {
            // Upload the File
            InputStream inStream = new FileInputStream(file);
            ref.putStream(inStream)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Notify the listener of success
                            listener.onSuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Notify the listener of the failure
                            listener.onFailure(e);
                        }
                    });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Pause the calling thread until the upload finishes
        listener.pauseUntilComplete();

        return listener.getStatus();
    }

    /**
     * Downloads a File from Firebase Storage to a local File
     *
     * @param file    File to be downloaded
     * @return True if the download was successful. False otherwise.
     */
    public boolean downloadFile(BaseFile file)  {
        // Init a StorageListener that will return the status of the operation
        final StorageListener listener = new StorageListener();

        // Check to make sure the File exists
        Uri uri = getDownloadUrl(file);
        if (uri == null) {
            listener.onFailure(new FileNotFoundException("File does not exist on Firebase Storage"));
            return listener.getStatus();
        }


        // Get a reference to the location it will be stored using the ImageFile's firebaseId
        StorageReference ref = StorageProviderUtils.getReferenceForFile(mStorage, file);

        // Download the File to the BaseFile in the signature
        ref.getFile(file)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFailure(e);
                    }
                });

        // Pause the calling thread until the download completes
        listener.pauseUntilComplete();

        return listener.getStatus();
    }

    /**
     * Removes a file from Firebase Storage
     *
     * @param file    File to be removed
     * @return True if successfully deleted. False otherwise.
     */
    public boolean deleteFile(BaseFile file) {
        // Init a StorageListener that will return the status of the operation
        final StorageListener listener = new StorageListener();

        StorageReference ref = StorageProviderUtils.getReferenceForFile(mStorage, file);
        ref.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFailure(e);
                    }
                });

        listener.pauseUntilComplete();

        return listener.getStatus();
    }

    /**
     * Generates a URL to download the File from Firebase Storage
     *
     * @param file    File to generate a download URL for
     * @return Uri form of the URL for the File
     */
    public Uri getDownloadUrl(BaseFile file) {
        // Init a StorageListener that will return the status of the operation
        final StorageListener listener = new StorageListener();

        // Get a StorageReference using the variables
        StorageReference ref = StorageProviderUtils.getReferenceForFile(mStorage, file);

        // Get the URL for the File
        ref.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // Set the Uri in the listener so it can be returned
                        listener.onUrlObtained(uri);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFailure(e);
                    }
                });

        // Pause the thread until complete
        listener.pauseUntilComplete();

        return listener.getDownloadUri();
    }

    /**
     * Generates the StorageReference for an image based on the FirebaseId. This is used to load
     * the image using FirebaseUI and Glide
     *
     * @param firebaseId    FirebaseId of the image
     * @return StorageReference correlating to the FirebaseId in the signature
     */
    public StorageReference getReferenceForImage(String firebaseId) {
        return mStorage.child(IMAGE_PATH).child(firebaseId + JPEG_EXT);
    }

    private class StorageListener {
        // ** Member Variables ** //
        private boolean status = false;
        private boolean complete = false;
        private Uri uri;

        /**
         * To be called when an operation is successful.
         */
        public synchronized void onSuccess() {
            // Set the member variables to reflect the status of the operation
            status = true;
            complete = true;

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        public synchronized void onFailure(Exception e) {
            // Set the member variables to reflect the status of the operation
            status = false;
            complete = true;

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        public synchronized void onUrlObtained(Uri uri) {
            // Set the member Uri to the Uri in the signature
            this.uri = uri;

            // Set the member variables to reflect the status of the operation
            status = true;
            complete = true;

            // Notify any paused threads that the operation is complete
            notifyAll();
        }

        /**
         * Pauses the calling thread until the operation is complete
         */
        public synchronized void pauseUntilComplete() {
            // Do not continue until the operation is complete
            while (!complete) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Gives the boolean status for whether the operation was successful
         *
         * @return True if successful. False if unsuccessful.
         */
        public boolean getStatus() {
            return status;
        }

        /**
         * Returns the Uri corresponding to the download link for a File
         *
         * @return Uri corresponding to the download link for a File
         */
        public Uri getDownloadUri() {
            return uri;
        }
    }
}
