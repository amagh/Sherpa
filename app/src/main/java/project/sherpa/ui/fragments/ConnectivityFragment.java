package project.sherpa.ui.fragments;

import android.databinding.ViewDataBinding;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import project.sherpa.R;
import project.sherpa.ads.viewmodels.AdViewModel;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;

/**
 * Created by Alvin on 9/7/2017.
 */

public abstract class ConnectivityFragment extends Fragment implements ConnectivityActivity.ConnectivityCallback {

    // ** Member Variables ** //
    private Snackbar mSnackbar;

    @Override
    public void onStart() {
        super.onStart();

        // Attach the Connectivity Callback
        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).addConnectivityCallback(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove the Callback
        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).removeConnectivityCallback(this);
        }
    }

    @Override
    public void onConnected() {

        // Re-connect to Firebase
        FirebaseDatabase.getInstance().goOnline();

        // Hide the Snackbar
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
        }
    }

    @Override
    public void onDisconnected() {

        // Disconnect from Firebase
        FirebaseDatabase.getInstance().goOffline();

        if (getActivity().findViewById(android.R.id.content) != null) {
            // Show a Snackbar to inform the user of network connectivity
            mSnackbar = Snackbar.make(
                    getActivity().findViewById(android.R.id.content),
                    getString(R.string.connectivity_error_no_network),
                    Snackbar.LENGTH_INDEFINITE);

            mSnackbar.show();

            mSnackbar.getView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSnackbar.dismiss();
                }
            });
        }
    }

    /**
     * Loads the ViewModel to serve ads
     */
    void loadAdViewModel(ViewDataBinding binding) {

        // Check if the ViewDataBinding contains a method for setting the AdViewModel. This method
        // should only exist in the free version of the app.
        Method setAdMethod = null;

        try {
            setAdMethod = binding.getClass().getMethod("setAd", AdViewModel.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        // If the binding contains the method, invoke it to set the AdViewModel and load the
        // AdRequest
        if (setAdMethod != null) {
            try {
                setAdMethod.invoke(binding, new AdViewModel(getActivity()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
