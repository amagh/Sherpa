package project.hikerguide.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.hikerguide.files.abstractfiles.BaseFile;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.utilities.FirebaseProviderUtils;
import project.hikerguide.utilities.SaveUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.IntentKeys.AREA_KEY;
import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;
import static project.hikerguide.utilities.IntentKeys.SECTION_KEY;
import static project.hikerguide.utilities.IntentKeys.TRAIL_KEY;
import static project.hikerguide.utilities.FirebaseProviderUtils.getReferenceForFile;

/**
 * Created by Alvin on 8/3/2017.
 */

public class PublishActivity extends MapboxActivity {
    // ** Member Variables ** //
    private Author mAuthor;
    private Guide mGuide;
    private Area mArea;
    private Trail mTrail;
    private Section[] mSections;

    private List<StorageReference> mUploadReferenceList;
    private UploadListener mUploadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the data objects passed from the Intent
        Intent intent = getIntent();

        mAuthor = intent.getParcelableExtra(AUTHOR_KEY);
        mGuide = intent.getParcelableExtra(GUIDE_KEY);
        mArea = intent.getParcelableExtra(AREA_KEY);
        mTrail = intent.getParcelableExtra(TRAIL_KEY);

        // Get the Parcelable[] for the Sections
        Parcelable[] parcelables = intent.getParcelableArrayExtra(SECTION_KEY);
        mSections = new Section[parcelables.length];

        // Copy the elements from parcelables to mSections as it cannot be directly cast to Section[]
        System.arraycopy(parcelables, 0, mSections, 0, parcelables.length);

        // Resize any images associated with the models
        resizeImages();

        mUploadListener = new UploadListener(getChildUpdates());
    }

    /**
     * Resize the images of any data models that have associated image Files
     */
    private void resizeImages() {

        // Resize the image for the Guide
        SaveUtils.resizeImageForModel(mGuide);

        // Resize the image for any Sections that have images
        for (Section section : mSections) {
            if (section.hasImage) {
                SaveUtils.resizeImageForModel(section);
            }
        }
    }

    /**
     * Creates the Map that will be used to push the data for the models to Firebase Database
     *
     * @return The Map including all the data for the models that will be pushed to Firebase
     * Database
     */
    private Map<String, Object> getChildUpdates() {

        // Initialize the Map
        Map<String, Object> childUpdates = new HashMap<>();

        // Upload the Area if it does not exist in the Firebase Database
        if (mArea.firebaseId == null) {
            addChildUpdate(mArea, childUpdates);
        }

        // Upload the trail
        if (mTrail.firebaseId == null) {
            mTrail.areaId = mArea.firebaseId;
            addChildUpdate(mTrail, childUpdates);
        }

        // Upload the Guide
        if (mGuide.firebaseId == null) {

            // Set the variables of the Guide to those from the associated Area and trail
            mGuide.trailId = mTrail.firebaseId;
            mGuide.trailName = mTrail.name;
            mGuide.authorId = mAuthor.firebaseId;
            mGuide.authorName = mAuthor.name;
            mGuide.area = mArea.name;

            addChildUpdate(mGuide, childUpdates);

            // Upload the Files for the Guide
            uploadFile(mGuide.getGpxFile());
            uploadFile(mGuide.getImageFile());
        }

        // Upload each Section
        for (Section section : mSections) {
            section.guideId = mGuide.firebaseId;

            addChildUpdate(section, childUpdates);

            if (section.hasImage) {

                // If the Section has an image, upload it
                uploadFile(section.getImageFile());
            }
        }

        return childUpdates;
    }

    /**
     * Adds a data models' data to the Map of updates to push to the FirebaseDatabase
     *
     * @param model           The model to be updated
     * @param childUpdates    The Map containing all update operations
     */
    private void addChildUpdate(BaseModel model, Map<String, Object> childUpdates) {

        // Get the Firebase Database directory specific to the model
        String directory = FirebaseProviderUtils.getDirectoryFromModel(model);

        // Push an update to get the key for the update
        String firebaseId;
        if (model instanceof Section) {
            firebaseId = FirebaseDatabase.getInstance().getReference()
                    .child(directory)
                    .child(((Section) model).guideId)
                    .push()
                    .getKey();

            directory = FirebaseProviderUtils.getDirectoryFromModel(model) + "/" + ((Section) model).guideId;
        } else {
            firebaseId = FirebaseDatabase.getInstance().getReference()
                    .child(directory)
                    .push()
                    .getKey();
        }

        // Modify the directory to include the FirebaseId (key)
        directory = directory + "/" + firebaseId;

        // Add the update to the Map
        childUpdates.put(directory, model.toMap());

        // Set the Model's FirebaseId to the upload key
        model.firebaseId = firebaseId;
    }

    /**
     * Uploads a File to FirebaseStorage
     *
     * @param file    The File to be uploaded
     */
    private void uploadFile(BaseFile file) {

        // Get a reference to Firebase Storage
        StorageReference reference = FirebaseStorage.getInstance().getReference();

        // Get the File-specific Storage Reference
        reference = getReferenceForFile(reference, file);

        // Add the Reference to the list of uploads to track
        addUploadReference(reference);

        try {

            // Create a FIS from the File
            FileInputStream inStream = new FileInputStream(file);

            // Upload
            reference.putStream(inStream)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot snapshot) {

                            // Remove the task from the tracked List
                            onUploadComplete(snapshot.getStorage());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            // Inform the user that it did not complete and will allow them
                            // to try again
                            Toast.makeText(PublishActivity.this, "Upload failed! Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * Adds an upload task to track for completion to mUploadReferenceList
     *
     * @param uploadReference    The StorageReference for the task to be tracked
     */
    private void addUploadReference(StorageReference uploadReference) {

        // Init the List if necessary
        if (mUploadReferenceList == null) {
            mUploadReferenceList = new ArrayList<>();
        }

        // Add the StorageReference to the List
        mUploadReferenceList.add(uploadReference);

        Timber.d("Uploads remaining: " + mUploadReferenceList.size());
    }

    /**
     * Removes the StorageReference from mUploadReferenceList that was completed
     *
     * @param uploadReference    The StorageReference that was completed
     */
    private void onUploadComplete(StorageReference uploadReference) {

        // Iterate through each the StorageReferences to find the matching StorageReference
        for (StorageReference reference : mUploadReferenceList) {
            if (reference.equals(uploadReference)) {

                // Remove the Reference from the List
                mUploadReferenceList.remove(mUploadReferenceList.indexOf(reference));
                break;
            }
        }

        if (mUploadReferenceList.size() == 0) {

            // If all upload tasks have completed, update the Firebase Database
            mUploadListener.onUploadComplete();
        }

        Timber.d("Removing task: " + mUploadReferenceList.size() + " remaining");
    }

    private class UploadListener {
        // ** Member Variables ** //
        private Map<String, Object> mChildUpdates;

        private UploadListener(Map<String, Object> childUpdates) {
            mChildUpdates = childUpdates;
        }

        /**
         * Updates the Firebase Database with the data loaded into mChildUpdates
         */
        private void onUploadComplete() {
            FirebaseDatabase.getInstance().getReference()
                    .updateChildren(mChildUpdates);
        }
    }
}
