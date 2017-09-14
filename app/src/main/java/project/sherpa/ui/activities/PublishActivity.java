package project.sherpa.ui.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.R;
import project.sherpa.databinding.ActivityPublishBinding;
import project.sherpa.files.abstractfiles.BaseFile;
import project.sherpa.models.datamodels.Area;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Section;
import project.sherpa.models.datamodels.Trail;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.PublishViewModel;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.utilities.GpxUtils;
import project.sherpa.utilities.SaveUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.AREA_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.TRAIL_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.getReferenceForFile;

/**
 * Created by Alvin on 8/3/2017.
 */

public class PublishActivity extends MapboxActivity implements ConnectivityActivity.ConnectivityCallback {
    // ** Member Variables ** //
    private ActivityPublishBinding mBinding;
    private PublishViewModel mViewModel;

    private Author mAuthor;
    private Guide mGuide;
    private Area mArea;
    private Trail mTrail;
    private Section[] mSections;

    // Models with the original FirebaseId of the models so they can properly deleted upon publishing
    private Guide mDeleteGuide;
    private Area mDeleteArea;
    private Trail mDeleteTrail;

    private List<StorageReference> mUploadReferenceList;
    private UploadListener mUploadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_publish);

        setFinishOnTouchOutside(false);
        setTitle(getString(R.string.publish_title_text));

        mViewModel = new PublishViewModel();
        mBinding.setVm(mViewModel);

        // Retrieve the data from the cache
        Intent intent = getIntent();

        String authorId = intent.getStringExtra(AUTHOR_KEY);
        String guideId = intent.getStringExtra(GUIDE_KEY);
        String areaId = intent.getStringExtra(AREA_KEY);
        String trailId = intent.getStringExtra(TRAIL_KEY);

        mAuthor = (Author) DataCache.getInstance().get(authorId);
        mGuide = (Guide) DataCache.getInstance().get(guideId);
        mArea = (Area) DataCache.getInstance().get(areaId);
        mTrail = (Trail) DataCache.getInstance().get(trailId);
        mSections = DataCache.getInstance().getSections(guideId);

        // Create dummy copies of the BaseModels with their original FirebaseIds
        mDeleteGuide = new Guide();
        mDeleteGuide.firebaseId = mGuide.firebaseId;

        mDeleteArea = new Area();
        mDeleteArea.firebaseId = mArea.firebaseId;

        mDeleteTrail = new Trail();
        mDeleteTrail.firebaseId = mTrail.firebaseId;

        addConnectivityCallback(this);
    }

    @Override
    public void onConnected() {

        if (mUploadListener == null) {
            // Resize any images associated with the models
            resizeImages();

            mUploadListener = new UploadListener(getChildUpdates());
        }
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this,
                getString(R.string.publish_error_no_network),
                Toast.LENGTH_LONG)
                .show();

        finish();
    }

    /**
     * Resize the images of any data models that have associated image Files
     */
    private void resizeImages() {

        // Resize the image for the Guide
        SaveUtils.resizeImageForModel(mGuide);

        // Resize the image for any Sections that have images
        for (Section section : mSections) {
            if (section.hasImage) SaveUtils.resizeImageForModel(section);
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
        if (mArea.firebaseId.equals(AREA_KEY) || mArea.isDraft()) {
            addChildUpdate(mArea, childUpdates);
        }

        // Upload the trail
        if (mTrail.firebaseId.equals(TRAIL_KEY) || mTrail.isDraft()) {

            mTrail.areaId = mArea.firebaseId;

            // Set the mid-point of the Trail
            LatLng trailMidPoint = GpxUtils.getMidPoint(mGuide.getGpxFile());

            if (trailMidPoint != null) {
                mTrail.setLatitude(trailMidPoint.getLatitude());
                mTrail.setLongitude(trailMidPoint.getLongitude());
            }

            addChildUpdate(mTrail, childUpdates);
        }

        // Upload the Guide
        if (mGuide.firebaseId.equals(GUIDE_KEY) || mGuide.isDraft()) {

            // Set the variables of the Guide to those from the associated Area and trail
            mGuide.trailId = mTrail.firebaseId;
            mGuide.trailName = mTrail.name;
            mGuide.authorId = mAuthor.firebaseId;
            mGuide.authorName = mAuthor.name;
            mGuide.area = mArea.name;
            mGuide.addDate();

            addChildUpdate(mGuide, childUpdates);

            // Upload the Files for the Guide
            uploadFile(mGuide.getGpxFile());
            uploadFile(mGuide.getImageFile());

            // Add the location to the GeoFire database so it can be queried
            GeoFire geoFire = FirebaseProviderUtils.getGeoFireInstance();
            GeoLocation location = new GeoLocation(mGuide.latitude, mGuide.longitude);

            geoFire.setLocation(mGuide.firebaseId, location);
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

        reference.putFile(Uri.fromFile(file))
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
                        Toast.makeText(PublishActivity.this, getString(R.string.publish_error_failed), Toast.LENGTH_LONG).show();
                    }
                });
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

        mViewModel.setTotalUploads(mUploadReferenceList.size());
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
        } else {
            mViewModel.setCurrentUpload(mViewModel.getTotalUploads() - mUploadReferenceList.size() + 1);
        }
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
                    .updateChildren(mChildUpdates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            // Delete the draft from the database
                            ContentProviderUtils.deleteModel(PublishActivity.this, mDeleteGuide);
                            ContentProviderUtils.deleteModel(PublishActivity.this, mDeleteTrail);
                            ContentProviderUtils.deleteModel(PublishActivity.this, mDeleteArea);
                            ContentProviderUtils.deleteSectionsForGuide(PublishActivity.this, mDeleteGuide);

                            if (ContentProviderUtils.getGuideCountForAuthor(PublishActivity.this, mAuthor) == 0) {
                                ContentProviderUtils.deleteModel(PublishActivity.this, mAuthor);
                            }

                            // Add
                            Intent data = new Intent();
                            data.putExtra(GUIDE_KEY, mGuide);

                            setResult(RESULT_OK, data);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Timber.e(e, e.getMessage());
                            finish();
                        }
                    });
        }
    }
}
