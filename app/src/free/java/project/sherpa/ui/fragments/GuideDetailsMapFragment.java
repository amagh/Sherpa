package project.sherpa.ui.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.sherpa.R;
import project.sherpa.ads.viewmodels.AdViewModel;
import project.sherpa.databinding.FragmentGuideDetailsMapBinding;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.GuideDetailsMapViewModel;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsMapFragment extends MapboxFragment {


    // ** Member Variables ** //
    private FragmentGuideDetailsMapBinding mBinding;
    private Guide mGuide;
    private boolean mTrackPosition;

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

        // Inflate the View and set the ViewDataBinding
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_guide_details_map, container, false);

        // Retrieve the Guide to populate the GuideViewModel
        if (getArguments() != null && getArguments().getString(GUIDE_KEY) != null) {
            String guideId = getArguments().getString(GUIDE_KEY);
            mGuide = (Guide) DataCache.getInstance().get(guideId);

            if (mGuide == null) {
                mGuide = new Guide();
                mGuide.firebaseId = guideId;
            }

        } else {
            Timber.d("No Guide passed in the Bundle!");
        }

        // Initialize the Map
        if (mGuide.authorId != null) {
            initMap(savedInstanceState);
        } else {
            getGuideFromFirebase();
        }

        mBinding.setHandler(new GuideDetailsMapViewModel((GuideDetailsActivity) getActivity()));

        loadAdViewModel();

        // Request permission to track user on map
        ((GuideDetailsActivity) getActivity()).requestLocationPermission();

        return mBinding.getRoot();
    }

    private void loadAdViewModel() {
        AdViewModel vm = new AdViewModel(getActivity());
        mBinding.setAd(vm);
    }

    /**
     * Sets up the MapboxMap to show the trail
     */
    private void initMap(Bundle savedInstanceState) {
        Timber.d("Initializing MapView");

        // Create a GuideViewModel, passing in the Guide
        GuideViewModel vm = new GuideViewModel(getActivity(), this, mGuide);
        vm.addSavedInstanceState(savedInstanceState);

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

    /**
     * Load the Guide details from Firebase and initialize the MapView for the Guide
     */
    private void getGuideFromFirebase() {

        FirebaseProviderUtils.getModel(
                FirebaseProviderUtils.FirebaseType.GUIDE,
                mGuide.firebaseId,
                new FirebaseProviderUtils.FirebaseListener() {
                    @Override
                    public void onModelReady(BaseModel model) {

                        // Retrieve the Guide
                        mGuide = (Guide) model;

                        // Cache the Guide
                        DataCache.getInstance().store(mGuide);

                        // Initialize the MapView
                        initMap(null);
                    }
                }
        );
    }
}
