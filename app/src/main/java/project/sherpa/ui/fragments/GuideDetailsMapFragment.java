package project.sherpa.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.sherpa.R;
import project.sherpa.databinding.FragmentGuideDetailsMapBinding;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.GuideDetailsMapViewModel;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.ui.fragments.abstractfragments.MapboxFragment;
import project.sherpa.ui.fragments.interfaces.FirebaseProviderInterface;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.GUIDE;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsMapFragment extends MapboxFragment {


    // ** Member Variables ** //
    private FragmentGuideDetailsMapBinding mBinding;
    private Guide mGuide;
    private boolean mTrackPosition;
    private Bundle mSavedInstanceState;

    public GuideDetailsMapFragment() {}

    /**
     * Factory for creating a GuideDetailsMapFragment for a specific Guide
     *
     * @param guideId    Guide whose details will be shown in the Fragment
     * @return A GuideDetailsMapFragment with a Bundle attached for displaying details for a Guide
     */
    public static GuideDetailsMapFragment newInstance(String guideId) {
        // Init the Bundle that will be passed with the Fragment
        Bundle args = new Bundle();

        // Put the Guide from the signature into the Bundle
        args.putString(GUIDE_KEY, guideId);

        // Initialize the Fragment and attach the args
        GuideDetailsMapFragment fragment = new GuideDetailsMapFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSavedInstanceState = savedInstanceState;

        // Inflate the View and set the ViewDataBinding
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_details_map, container, false);
        bindFirebaseProviderService(true);

        mBinding.setHandler(new GuideDetailsMapViewModel((GuideDetailsActivity) getActivity()));

        // Load ads if applicable
        loadAdViewModel(mBinding);

        // Request permission to track user on map
        ((GuideDetailsActivity) getActivity()).requestLocationPermission();

        return mBinding.getRoot();
    }

    @Override
    protected void onServiceConnected() {
        String guideId = getArguments().getString(GUIDE_KEY);
        loadGuide(guideId);
    }

    /**
     * Loads a Guide from Firebase and initializes the MapboxMap
     *
     * @param guideId    FirebaseId of the guide to load the map for
     */
    private void loadGuide(String guideId) {

        ModelChangeListener<Guide> guideListener = new ModelChangeListener<Guide>(GUIDE, guideId) {
            @Override
            public void onModelReady(Guide model) {
                mGuide = model;
                initMap();

                mService.unregisterModelChangeListener(this);
            }

            @Override
            public void onModelChanged() {

            }
        };

        mService.registerModelChangeListener(guideListener);
    }

    /**
     * Sets up the MapboxMap to show the trail
     */
    private void initMap() {
        if (mBinding == null || mGuide == null) return;

        // Create a GuideViewModel, passing in the Guide
        GuideViewModel vm = new GuideViewModel(getActivity(), this, mGuide);
        vm.addSavedInstanceState(mSavedInstanceState);

        if (mTrackPosition) {
            vm.setTrackUserPosition(true);
        }

        // Set the ViewModel to the binding
        mBinding.setVm(vm);
    }

    /**
     * Sets the MapView to begin tracking the user's position
     */
    public void trackUserPosition() {

        mTrackPosition = true;

        // Get the ViewModel that has the MapView and begin tracking location
        if (mTrackPosition && mBinding != null && mBinding.getVm() != null) {
            mBinding.getVm().setTrackUserPosition(true);
        }
    }
}
