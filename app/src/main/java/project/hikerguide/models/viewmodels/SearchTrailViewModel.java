package project.hikerguide.models.viewmodels;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.BR;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.ui.adapters.TrailAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 8/3/2017.
 */

public class SearchTrailViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Activity mActivity;
    private Area mArea;
    private TrailAdapter mAdapter;
    private List<Trail> mTrailList;
    private String mQuery;
    private boolean mSearchHasFocus = false;

    public SearchTrailViewModel(Activity activity, Area area) {
        mActivity = activity;
        mArea = area;

        getTrailsFromFirebase();
    }

    @Bindable
    public String getQuery() {
        return mQuery;
    }

    public void setQuery(String query) {
        mQuery = query;

        // Filter the List by the query
        if (mQuery.length() > 0) {
            filter(mQuery);
        } else {
            resetAdapterList();
        }
    }

    @Bindable
    public Activity getActivity() {
        return mActivity;
    }

    @Bindable
    public TrailAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new TrailAdapter();
        }

        return mAdapter;
    }

    @BindingAdapter({"bind:activity", "bind:adapter"})
    public static void initRecyclerView(RecyclerView recyclerView, Activity activity, TrailAdapter adapter) {

        // Setup the RecyclerView
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
    }

    @Bindable
    public boolean getHasFocus() {
        return mSearchHasFocus;
    }

    @BindingAdapter({"app:searchIv", "app:closeIv", "bind:hasFocus"})
    public static void animateFocus(EditText searchTv, ImageView searchIv, ImageView closeIv, boolean hasFocus) {

        float searchAlpha   = 0;
        float closeAlpha    = 0;

        if (hasFocus) {
            // Prevent clicking on the close ImageView when it is not visible
            closeIv.setClickable(true);
            closeAlpha = 1;
        } else {
            searchAlpha = 1;

            // Allow clicking on the close ImageView
            closeIv.setClickable(false);

            // Clear the Focus from the EditText
            searchTv.clearFocus();
        }

        // Animate changes
        new AdditiveAnimator().setDuration(150)
                .target(searchIv).alpha(searchAlpha)
                .target(closeIv).alpha(closeAlpha)
                .start();
    }

    private void getTrailsFromFirebase() {

        // Query the Firebase Database
        final Query trailQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.TRAILS)
                // TODO: Replace hardcoded String
                .orderByChild("areaId")
                .equalTo(mArea.firebaseId);

        trailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Check that the result is valid
                if (dataSnapshot.exists()) {

                    // Retrieve the Trails from the DataSnapshot
                    Trail[] trails = (Trail[]) FirebaseProviderUtils.getModelsFromSnapshot(DatabaseProvider.FirebaseType.TRAIL, dataSnapshot);

                    // Pass the Trails to the Adapter
                    mTrailList = Arrays.asList(trails);
                    resetAdapterList();
                }

                // Remove Listener
                trailQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                trailQuery.removeEventListener(this);
            }
        });
    }

    public void onFocusChanged(View view, boolean hasFocus) {
        mSearchHasFocus = hasFocus;

        notifyPropertyChanged(BR.hasFocus);
    }

    public void onClickClear(View view) {

        // Set the focus to false
        mSearchHasFocus = false;

        // Clear the query
        mQuery = null;

        notifyPropertyChanged(BR.query);
        notifyPropertyChanged(BR.hasFocus);
    }

    /**
     * Replaces the contents of the Adapter with the master list of all trails in the Area
     */
    private void resetAdapterList() {
        mAdapter.replaceAll(mTrailList);
    }

    /**
     * Filters the list of Trails for only those that match the query
     *
     * @param query    Query to filter for
     */
    private void filter(String query) {

        // Convert to lower case
        String queryLowerCase = query.toLowerCase();

        // Create a List that will contain all the Trails that match the query
        List<Trail> filteredTrailList = new ArrayList<>();

        // Iterate through and check each Trail for those that match
        for (Trail trail : mTrailList) {
            if (trail.name.toLowerCase().contains(queryLowerCase)) {
                filteredTrailList.add(trail);
            }
        }

        // Replace the contents of the Adapter with the filtered List
        mAdapter.replaceAll(filteredTrailList);
    }
}
