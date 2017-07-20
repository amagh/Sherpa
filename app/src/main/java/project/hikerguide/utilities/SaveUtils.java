package project.hikerguide.utilities;

import android.content.Context;

import java.io.File;

import project.hikerguide.R;
import project.hikerguide.firebasedatabase.FirebaseProvider;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.Area;
import project.hikerguide.models.Author;
import project.hikerguide.models.Guide;
import project.hikerguide.models.Section;
import project.hikerguide.models.Trail;

/**
 * Created by Alvin on 7/19/2017.
 */

public class SaveUtils {
    // ** Constants ** //
    public static final String GPX_PATH = "/gpx";
    public static final String IMAGE_PATH = "/images";

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

        // Get an instance of the FirebaseProvider
        FirebaseProvider database = FirebaseProvider.getInstance();
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
            guide.authorId = author.firebaseId;
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
}
