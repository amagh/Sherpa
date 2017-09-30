package project.sherpa.utilities.objects;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 9/29/2017.
 */

public abstract class SmartQueryValueListener<T extends BaseModel> implements ValueEventListener {

    // ** Member Variables ** //
    @FirebaseProviderUtils.FirebaseType
    private int mType;
    private Query mQuery;
    private boolean mStarted;

    public SmartQueryValueListener(@FirebaseProviderUtils.FirebaseType int type, Query query) {
        mType = type;
        mQuery = query;
    }

    /**
     * Starts listening for data changes at the mQuery
     */
    public void start() {
        if (!mStarted) {
            mQuery.addValueEventListener(this);
            mStarted = true;
        }
    }

    /**
     * Stops listening for data changes at mQuery
     */
    public void stop() {
        if (mStarted) {
            mQuery.removeEventListener(this);
            mStarted = false;
        }
    }

    /**
     * Returns the data retrieved at mQuery. Called every time the data changes
     *
     * @param models    An Array of BaseModels matching mType describing the data at mQuery
     */
    public abstract void onQueryChanged(T[] models);

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
            T[] models = (T[]) FirebaseProviderUtils.getModelsFromSnapshot(mType, dataSnapshot);
            onQueryChanged(models);
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Timber.d(databaseError.getMessage());
    }
}
