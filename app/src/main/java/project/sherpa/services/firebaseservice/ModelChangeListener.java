package project.sherpa.services.firebaseservice;

import android.support.annotation.NonNull;

import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

/**
 * Listens for changes to data on Firebase Database. It is attached to SmartValueEventListeners by
 * registering them with FirebaseProviderService.
 *
 * Created by Alvin on 10/2/2017.
 */

public abstract class ModelChangeListener<T extends BaseModel> {

    // ** Member Variables ** //
    @FirebaseProviderUtils.FirebaseType
    private int mType;
    private String mFirebaseId;
    private T mModel;

    public ModelChangeListener(@FirebaseProviderUtils.FirebaseType int type,
                               @NonNull String firebaseId) {
        mType = type;
        mFirebaseId = firebaseId;
    }

    /**
     * Notifies the observer that the data has changed
     */
    void updateModel() {

        // Check whether this is the first time the data is being delivered to the receiver
        if (mModel == null) {

            // First time being delivered. Set the memvar to the data from the DataCache
            mModel = (T) DataCache.getInstance().get(mFirebaseId);
            onModelReady(mModel);
        } else {

            // Receiver has already received the original data. Notify them of changes to it so
            // they can update their UI
            onModelChanged();
        }
    }

    /**
     * Delivers the data corresponding to the receiver when it becomes ready
     *
     * @param model    The data model being requested
     */
    public abstract void onModelReady(T model);

    /**
     * Called when the data being observed has changed, allowing the UI to update its components
     */
    public abstract void onModelChanged();

    //********************************************************************************************//
    //*********************************** Getters & Setters **************************************//
    //********************************************************************************************//

    public String getFirebaseId() {
        return mFirebaseId;
    }

    @FirebaseProviderUtils.FirebaseType
    public int getType() {
        return mType;
    }
}
