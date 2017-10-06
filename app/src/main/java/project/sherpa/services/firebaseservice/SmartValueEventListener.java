package project.sherpa.services.firebaseservice;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 9/28/2017.
 */

public abstract class SmartValueEventListener implements ValueEventListener {

    // ** Member Variables ** //
    @FirebaseProviderUtils.FirebaseType
    private int mType;
    private DatabaseReference mReference;
    private String mFirebaseId;
    private boolean mStarted;
    private BaseModel mModel;

    public SmartValueEventListener(@FirebaseProviderUtils.FirebaseType int type, String firebaseId) {
        mType = type;
        mFirebaseId = firebaseId;

        String directory = FirebaseProviderUtils.getDirectoryFromType(mType);
        mReference = FirebaseDatabase.getInstance().getReference()
                .child(directory)
                .child(mFirebaseId);
    }

    /**
     * Starts Listening for data on mReference
     */
    public void start() {
        if (!mStarted) {
            mReference.addValueEventListener(this);
            mStarted = true;

            if (mModel == null && DataCache.getInstance().get(mFirebaseId) != null) {
                mModel = DataCache.getInstance().get(mFirebaseId);

                onModelChange();
            }
        }
    }

    /**
     * Stops listening for data on mReference
     */
    public void stop() {
        if (mStarted) {
            mReference.removeEventListener(this);
            mStarted = false;
        }
    }

    /**
     * Returns the data that is being observed by the SmartValueEventListener
     *
     * @return Data that is being observed
     */
    public BaseModel getModel() {
        return mModel;
    }

    /**
     * Returns the data from Firebase at mReference. Called every time the data changes.
     *
     * To be deprecated once all Activities/Fragments switch over to the FirebaseProviderService.
     */
    public abstract void onModelChange(BaseModel model);

    /**
     * Called when the underlying data has changed
     */
    public void onModelChange() {

    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

        if (dataSnapshot.exists()) {

            // Retrieve the data from the DataSnapshot
            BaseModel model = FirebaseProviderUtils.getModelFromSnapshot(mType, dataSnapshot);

            // Set the memvar to the returned data if it has not been set yet
            if (mModel == null) {
                mModel = model;
            }

            // Cache the updated data - DataCache will update an entry that already exists instead
            // of replacing it, allowing all references to maintain fresh data.
            DataCache.getInstance().store(model);

            onModelChange();
            onModelChange(mModel);
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Timber.d(databaseError.getMessage());
    }
}
