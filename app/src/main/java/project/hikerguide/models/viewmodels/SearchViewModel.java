package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import project.hikerguide.BR;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.ui.adapters.AreaAdapter;

/**
 * Created by Alvin on 8/2/2017.
 */

public class SearchViewModel extends BaseObservable {
    // ** Member Variables ** //
    private AreaAdapter mAdapter;
    private String mQuery;
    private boolean mSearchHasFocus = false;
    private Handler mHandler;

    public AreaAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new AreaAdapter(new AreaAdapter.ClickHandler() {
                @Override
                public void onClickArea(Area area) {

                }
            });
        }

        return mAdapter;
    }

    @Bindable
    public boolean getHasFocus() {
        return mSearchHasFocus;
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;

        // Query the Firebase Database
        queryFirebaseDatabase(mQuery);
    }

    public void onFocusChanged(View view, boolean hasFocus) {
        mSearchHasFocus = hasFocus;

        notifyPropertyChanged(BR.hasFocus);
    }

    @BindingAdapter({"app:searchIv", "app:closeIv"})
    public static void animateFocus(EditText editText, ImageView searchIv, ImageView closeiv) {

    }

    private void queryFirebaseDatabase(String query) {

        final Query firebaseQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AREAS)
                .orderByChild(GuideContract.AreaEntry.NAME);

        firebaseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {



                firebaseQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                firebaseQuery.removeEventListener(this);
            }
        });
    }
}
