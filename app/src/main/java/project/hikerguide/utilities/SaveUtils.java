package project.hikerguide.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModelWithImage;

import static project.hikerguide.firebasestorage.StorageProvider.FirebaseFileType.GPX_FILE;
import static project.hikerguide.firebasestorage.StorageProvider.FirebaseFileType.IMAGE_FILE;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_EXT;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.hikerguide.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 7/19/2017.
 */

public class SaveUtils {
    // ** Constants ** //
    private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir", "."));
    private static final int MAX_SIZE = 1280;

    /**
     * Saves a completed guide to the Firebase Database
     *
     * @param area        Area corresponding to the trail detailed by the guide
     * @param author      Author of the guide
     * @param trail       Trail described by the guide
     * @param guide       The Guide data model describing the details of the guide
     * @param sections    An Array of Section data models detailing the trail taken by the guide
     */
    public static void saveGuide(Area area, Author author, Trail trail, Guide guide, Section... sections) {
        // Check that the data models have all required info
        if (guide.getImageFile() == null) {
            throw new IllegalArgumentException("Guide does not have a hero image.");
        }

        if (guide.getGpxFile() == null) {
            throw new IllegalArgumentException("Guide does not have a Gpx file");
        }

        for (Section section : sections) {
            if ((section.content == null || section.content.isEmpty()) && section.getImageFile() == null) {
                throw new IllegalArgumentException("Some of the sections have no text content and no image");
            }
        }

        // Get an instance of the DatabaseProvider
        DatabaseProvider database = DatabaseProvider.getInstance();
        StorageProvider storage = StorageProvider.getInstance();

        // Insert the Area into the Firebase Database if needed
        if (area.firebaseId == null || area.firebaseId.isEmpty()) {
            database.insertRecord(area);
        }

        // Insert the Trail into the Firebase Database if needed
        if (trail.firebaseId == null || trail.firebaseId.isEmpty()) {
            trail.areaId = area.firebaseId;
            database.insertRecord(trail);
        }

        // Insert the Guide into the Firebase Database if needed
        if (guide.firebaseId == null || trail.firebaseId.isEmpty()) {
            guide.trailId = trail.firebaseId;
            guide.trailName = trail.name;
            guide.authorId = author.firebaseId;
            guide.authorName = author.name;
            guide.area = area.name;
            database.insertRecord(guide);

            // Upload the Image and Gpx File associated with the Guide
            storage.uploadFile(guide.getImageFile());
            storage.uploadFile(guide.getGpxFile());
        }

        // Set the guideId in each Section
        for (Section section: sections) {
            section.guideId = guide.firebaseId;

        }

        // Insert all Sections into the Firebase Database
        database.insertRecord(sections);

        for (Section section : sections) {
            if (section.hasImage) {
                storage.uploadFile(section.getImageFile());
            }
        }
    }

    /**
     * Checks whether the subdirectories for images and gpx files have been created on the Internal
     * storage. If they are have not been created it, they will be made.
     *
     * @param context    Interface to global Context
     */
    public static void makeSubdirectories(Context context) {
        // Create the File for the image directory
        File imageDir = new File(context.getFilesDir() + IMAGE_PATH);

        // Create a File for the gpx directory
        File gpxDir = new File(context.getFilesDir() + GPX_PATH);

        // Check to see whether the directories have been created and create them if needed
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        if (!gpxDir.exists()) {
            gpxDir.mkdir();
        }
    }

    /**
     * Creates a temporary file for either an image or a GPX file that has a reproducible location
     * so that it can be referenced later so long as it exists in the cache
     *
     * @param type          Type of File to create the temporary File for - used to generate the
     *                      file extension
     * @param firebaseId    To be used as the Filename
     * @return File stored in the temporary file's location with the FirebaeId as the name and the
     * correct file extension according to the type
     */
    public static File createTempFile(@StorageProvider.FirebaseFileType int type, String firebaseId) {

        // Get a reference to the String that will be used for the file extension
        String fileExtension = "";

        // Set the file extension based on the type of File
        switch (type) {
            case GPX_FILE:
                fileExtension = GPX_EXT;
                break;

            case IMAGE_FILE:
                fileExtension = JPEG_EXT;
                break;
        }

        return new File(TEMP_DIRECTORY, firebaseId + fileExtension);
    }

    /**
     * Resize an ImageFile associated with a data model and compresses it with JPEG compression to
     * lower the file size when stored on Firebase Storage
     *
     * @param model    The BaseModel containing an ImageFile to be resized
     */
    public static void resizeImageForModel(BaseModelWithImage model) {

        if (!model.hasImage) {
            // Model does not contain an associated ImageFile. Nothing to resize.
            return;
        }

        // Resize the image
        File resizedImage = resizeImage(model.getImageFile());

        // Set the model's image to point to the resized image
        model.setImageUri(resizedImage);
    }

    /**
     * Resize an image file and compresses it with JPEG compression to lower the file size when
     * stored on Firebase Storage.
     *
     * @param image    Image File to be resized
     * @return Resized Image File.
     */
    public static File resizeImage(File image) {
        // Create a Bitmap from the model's associated imageUri
        Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());

        // Get the dimensions of the Bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Check which dimension is greater and resize it so the largest side is no longer than
        // MAX_SIZE
        if (width > MAX_SIZE || height > MAX_SIZE) {

            // References for the new output dimensions
            int newWidth, newHeight;

            if (width > height) {
                newWidth = MAX_SIZE;
                newHeight = (int) (((float) height / (float) width) * MAX_SIZE);
            } else {
                newHeight = MAX_SIZE;
                newWidth = (int) (((float) width / (float) height) * MAX_SIZE);
            }

            // Scale the Bitmap to the new dimensions
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
        }

        try {

            // Create a temporary file with the same name as the input File
            File imageFile = File.createTempFile(Uri.fromFile(image).getLastPathSegment(), null);

            // Create a FOS to the File
            FileOutputStream outStream = new FileOutputStream(imageFile);

            // Compress and write to the FOS
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream);
            outStream.flush();
            outStream.close();

            // Set the Uri for the model based on the newly compressed File
            return imageFile;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

