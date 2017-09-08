package project.sherpa.models.datamodels.abstractmodels;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.database.Exclude;

import java.io.File;

import project.sherpa.files.ImageFile;

/**
 * Created by Alvin on 7/19/2017.
 */

public abstract class BaseModelWithImage extends BaseModel {
    // ** Member Variables ** //
    protected Uri imageUri;
    public boolean hasImage;


    /**
     * Sets the Uri for the object's corresponding image file
     *
     * @param imageUri  Uri describing the location of an image file
     */
//    public void setImageUri(Uri imageUri) {
//        this.imageUri = imageUri;
//    }

    /**
     * Sets the Uri for the Object's image from a File that describes the location of an image file
     *
     * @param imageFile    File corresponding to an image file
     */
    public void setImageUri(File imageFile) {
        hasImage = true;
        this.imageUri = Uri.fromFile(imageFile);
    }

    @Exclude
    public Uri getImageUri() {
        return imageUri;
    }

    /**
     * Creates and returns the ImageFile corresponding to the model's imageUri
     *
     * @return ImageFile corresponding to the model's imageUri
     */
    public ImageFile getImageFile() {
        if (this.imageUri != null) {
            return new ImageFile(this.firebaseId, this.imageUri.getPath());
        } else {
            return null;
        }
    }

    /**
     * Creates the File where the downloaded ImageFile from the Firebase Storage will exist
     *
     * @param context    Interface to global Context
     * @return ImageFile corresponding to where the ImageFile should be saved on Internal Storage
     */
    public ImageFile generateImageFileForDownload(Context context) {
        // Generate the ImageFile
        ImageFile image = ImageFile.getDestinationFile(context, this.firebaseId);

        // Set the imageUri to the new File's path
        setImageUri(image);

        return image;
    }
}
