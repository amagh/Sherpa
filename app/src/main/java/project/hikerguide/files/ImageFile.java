package project.hikerguide.files;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;

import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.utilities.SaveUtils;

import static project.hikerguide.utilities.FirebaseProviderUtils.IMAGE_PATH;

/**
 * Created by Alvin on 7/19/2017.
 */

public class ImageFile extends BaseFile {
    // ** Constants ** //
    private static final String JPEG_EXT = ".jpg";

    public ImageFile(String firebaseId, String pathname) {
        super(pathname);
        this.firebaseId = firebaseId;
    }

    public ImageFile(String firebaseId, String parent, String child) {
        super(parent, child);
        this.firebaseId = firebaseId;
    }

    public ImageFile(String firebaseId, File parent, String child) {
        super(parent, child);
        this.firebaseId = firebaseId;
    }

    public ImageFile(String firebaseId, File parent) {
        super(parent, firebaseId + JPEG_EXT);
        this.firebaseId = firebaseId;
    }

    public ImageFile(String firebaseId, @NonNull URI uri) {
        super(uri, firebaseId);
        this.firebaseId = firebaseId;
    }

    /**
     * Creates a new File in the image subdirectory of the internal storage
     *
     * @param context       Interface to Global Context
     * @param firebaseId    ID of the image to be downloaded
     * @return A new ImageFile within the internal storage's image subdirectory
     */
    public static ImageFile getDestinationFile(Context context, String firebaseId) {
        // Get a reference to the subdirectory
        File imageDirectory = new File(context.getFilesDir() + IMAGE_PATH);

        // Create the directory if it does not yet exist
        if (!imageDirectory.exists()) {
            imageDirectory.mkdir();
        }

        return new ImageFile(firebaseId, imageDirectory);
    }

    /**
     * Copies the image file from the directory it currently exists in to the internal storage
     *
     * @param context    Interface to global Context
     * @return True if copy operation was a success. False otherwise.
     */
    public boolean saveToInternalStorage(Context context) {
        SaveUtils.makeSubdirectories(context);

        // Get a reference to the location that the new ImageFile will be saved
        ImageFile file = new ImageFile(this.firebaseId, context.getFilesDir() + IMAGE_PATH);

        // Check to see if the file already exists in internal storage
        if (file.exists() && this.equals(file)) {
            return true;
        }

        try {
            // Open Input and Output Streams from the source and destination
            FileInputStream inStream = new FileInputStream(this);
            FileOutputStream outStream = new FileOutputStream(file);

            // Byte array for buffer to copy in chunks
            byte[] buffer = new byte[1024];

            int length;

            // Copy the file over in chunks using the buffer
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

            // Close the streams
            inStream.close();
            outStream.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return file.exists();
    }
}
