package project.hikerguide.utilities;

import com.google.firebase.storage.StorageReference;

import project.hikerguide.files.GpxFile;
import project.hikerguide.files.ImageFile;
import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.firebasestorage.StorageProvider;

import static project.hikerguide.firebasestorage.StorageProvider.FirebaseFileType.GPX_FILE;
import static project.hikerguide.firebasestorage.StorageProvider.FirebaseFileType.IMAGE_FILE;

/**
 * Created by Alvin on 7/19/2017.
 */

public class StorageProviderUtils {
    // ** Constants ** //
    public static final String IMAGE_PATH = "images";
    public static final String GPX_PATH = "gpx";
    public static final String JPEG_EXT = ".jpg";
    public static final String GPX_EXT = ".gpx";
    public static final String BACKDROP_SUFFIX = "_bd";

//    public static String getDirectoryFromType(@StorageProvider.FirebaseFileType int type) {
//        String directory;
//        // Resolve the references based on the FirebaseFileType
//        switch (type) {
//            case IMAGE_FILE:
//                directory = IMAGE_PATH;
//                break;
//
//            case GPX_FILE:
//                directory = GPX_PATH;
//                break;
//
//            default: throw new UnsupportedOperationException("Unknown FileType: " + type);
//        }
//
//        return directory;
//    }
//
//    public static String getFileExtensionFromType(@StorageProvider.FirebaseFileType int type) {
//        String fileExtension;
//
//        switch (type) {
//            case IMAGE_FILE:
//                fileExtension = JPEG_EXT;
//                break;
//
//            case GPX_FILE:
//                fileExtension = GPX_EXT;
//                break;
//
//            default: throw new UnsupportedOperationException("Unknown FileType: " + type);
//        }
//
//        return fileExtension;
//    }


    public static String getDirectoryFromType(BaseFile file) {
        // Return the directory based on the class of the File
        if (file instanceof ImageFile) {
            return IMAGE_PATH;
        } else if (file instanceof GpxFile){
            return GPX_PATH;
        } else {
            throw new UnsupportedOperationException("Unknown BaseFile: " + file.getClass());
        }
    };

    public static String getFileExtensionFromType(BaseFile file) {
        // Return the file extension based on the class of the File
        if (file instanceof ImageFile) {
            return JPEG_EXT;
        } else if (file instanceof GpxFile){
            return GPX_EXT;
        } else {
            throw new UnsupportedOperationException("Unknown BaseFile: " + file.getClass());
        }
    }

    /**
     * Generates the StorageReference for where a File is stored on Firebase Storage
     *
     * @param file    File to get the StorageReference for
     * @return The StorageReference for a File
     */
    public static StorageReference getReferenceForFile(StorageReference storageReference, BaseFile file) {
        // Get the directory and file extension based on the File's type
        String directory = getDirectoryFromType(file);
        String fileExtension = getFileExtensionFromType(file);

        // Generate the StorageReference using the directory and file extension
        return storageReference.child(directory).child(file.firebaseId + fileExtension);
    }
}
