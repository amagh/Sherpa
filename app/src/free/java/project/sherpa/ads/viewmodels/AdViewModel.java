package project.sherpa.ads.viewmodels;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

/**
 * Created by Alvin on 9/12/2017.
 */

public class AdViewModel extends BaseObservable {

    // ** Member Variables ** //
    private Activity  mActivity;
    private AdRequest mAdRequest;

    public AdViewModel(Activity activity) {
        mActivity = activity;

        // Initialize MobileAds
        MobileAds.initialize(mActivity, "ca-app-pub-9368220731151233~3907953792");
    }

    @Bindable
    public AdRequest getAdRequest() {

        // Check to see if the AdRequest has been initialized
        if (mAdRequest == null) {
            mAdRequest = new AdRequest.Builder().build();
        }

        return mAdRequest;
    }

    @BindingAdapter("adRequest")
    public static void loadAdView(AdView adView, AdRequest adRequest) {

        // Load the AdRequest to the AdView
        adView.loadAd(adRequest);
    }
}
