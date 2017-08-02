package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.mapbox.SmartMapView;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.ui.adapters.AreaAdapter;
import timber.log.Timber;

/**
 * Created by Alvin on 8/2/2017.
 */

public class SearchViewModel extends BaseObservable {
    // ** Member Variables ** //
    private AreaAdapter mAdapter;
    private MapboxActivity mActivity;
    private String mQuery;
    private boolean mSearchHasFocus = false;

    public SearchViewModel(MapboxActivity activity) {
        mActivity = activity;
    }

    @Bindable
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
    public MapboxActivity getActivity() {
        return mActivity;
    }

    @BindingAdapter({"bind:adapter", "bind:activity"})
    public static void setAdapter(RecyclerView recyclerView, AreaAdapter adapter, MapboxActivity activity) {

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
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
        Timber.d("Focus: " + hasFocus);
        mSearchHasFocus = hasFocus;

        notifyPropertyChanged(BR.hasFocus);
    }

    @BindingAdapter({"app:searchIv", "app:closeIv", "bind:hasFocus"})
    public static void animateFocus(CardView cardView, ImageView searchIv, ImageView closeIv, boolean hasFocus) {

        float cardAlpha     = 0.75f;
        float searchAlpha   = 0;
        float closeAlpha    = 0;

        if (hasFocus) {
            cardAlpha = 1;
            closeAlpha = 1;
        } else {
            searchAlpha = 1;
        }

        new AdditiveAnimator().setDuration(150)
                .target(cardView).alpha(cardAlpha)
                .target(searchIv).alpha(searchAlpha)
                .target(closeIv).alpha(closeAlpha)
                .start();
    }

    @BindingAdapter("bind:activity")
    public static void initMap(SmartMapView mapView, MapboxActivity activity) {
        mapView.startMapView(activity);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

            }
        });
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
