package project.sherpa.services.firebaseservice;

import com.google.firebase.database.Query;


import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 10/3/2017.
 */

public abstract class QueryChangeListener<T extends BaseModel> {

    // ** Member Variables ** //
    @FirebaseProviderUtils.FirebaseType
    private int mType;
    private Query mQuery;
    private T[] mModels;

    public QueryChangeListener(@FirebaseProviderUtils.FirebaseType int type, Query query) {
        mType = type;
        mQuery = query;
    }

    /**
     * Notifies the observer that the underlying data has changed
     *
     * @param models    An Array of BaseModels corresponding to the data returned by the Query
     */
    void updateModels(BaseModel[] models) {

        mModels = (T[]) models;
        onQueryChanged(mModels);
    }

    /**
     * Called when the data being queried is updated
     *
     * @param models    An Array of BaseModels corresponding to the data returned by the Query
     */
    public abstract void onQueryChanged(T[] models);

    //********************************************************************************************//
    //*********************************** Getters & Setters **************************************//
    //********************************************************************************************//

    @FirebaseProviderUtils.FirebaseType
    public int getType() {
        return mType;
    }

    public Query getQuery() {
        return mQuery;
    }

    public String getUriString() {
        return mQuery.toString();
    }


}
