package project.sherpa.utilities.objects;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import project.sherpa.models.datamodels.abstractmodels.BaseModel;
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

    public SmartValueEventListener(@FirebaseProviderUtils.FirebaseType int type, String firebaseId) {
        mType = type;
        mFirebaseId = firebaseId;

        String directory = FirebaseProviderUtils.getDirectoryFromType(mType);
        mReference = FirebaseDatabase.getInstance().getReference()
                .child(directory)
                .child(mFirebaseId);
    }

    public void start() {
        mReference.addValueEventListener(this);
    }

    public void stop() {
        mReference.removeEventListener(this);
    }

    public abstract void onModelChange(BaseModel model);

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        BaseModel model = FirebaseProviderUtils.getModelFromSnapshot(mType, dataSnapshot);
        onModelChange(model);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Timber.d(databaseError.getMessage());
    }
}
