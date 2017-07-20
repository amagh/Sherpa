package project.hikerguide.models.abstractmodels;

import android.content.Context;
import android.net.Uri;

import java.io.File;

import project.hikerguide.files.ImageFile;

/**
 * Created by Alvin on 7/19/2017.
 */

public abstract class BaseModelWithImage extends BaseModel {
    // ** Member Variables ** //
    private Uri imageUri;
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

    /**
     * Creates and returns the ImageFile corresponding to the model's imageUri
     *
     * @return ImageFile corresponding to the model's imageUri
     */
    public ImageFile getImageFile() {
        return new ImageFile(this.firebaseId, this.imageUri.getPath());
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
