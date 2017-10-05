package project.sherpa.ui.fragments.abstractfragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.ViewDataBinding;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import project.sherpa.R;
import project.sherpa.ads.viewmodels.AdViewModel;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.services.firebaseservice.FirebaseProviderService.FirebaseProviderBinder;

/**
 * Created by Alvin on 9/7/2017.
 */

public abstract class ConnectivityFragment extends Fragment implements ConnectivityActivity.ConnectivityCallback {

    // ** Member Variables ** //
    private Snackbar mSnackbar;

    protected FirebaseProviderService mService;
    private boolean mBindService;
    private boolean mBound;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FirebaseProviderBinder binder = (FirebaseProviderBinder) iBinder;
            mService = binder.getService();
            mBound = true;

            ConnectivityFragment.this.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        // Attach the Connectivity Callback
        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).addConnectivityCallback(this);
        }

        bindService();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove the Callback
        if (getActivity() instanceof ConnectivityActivity) {
            ((ConnectivityActivity) getActivity()).removeConnectivityCallback(this);
        }

        unbindService();
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
     * Binds the FirebaseProviderService for ths Fragment
     */
    private synchronized void bindService() {
        if (!mBound && mBindService) {
            Intent intent = new Intent(getActivity(), FirebaseProviderService.class);
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Unbinds the FirebaseProviderService for this Fragment
     */
    private synchronized void unbindService() {
        if (mBound && mBindService) {
            getActivity().unbindService(mConnection);
        }
    }

    /**
     * Sets whether the Fragment should bind to FirebaseProviderService
     *
     * @param bindService    Boolean value for whether the Fragment should connect to the
     *                       FirebaseProviderService
     */
    public void bindFirebaseProviderService(boolean bindService) {
        mBindService = bindService;
    }

    /**
     * Called when the FirebaseProviderService is bound
     */
    protected void onServiceConnected() {}

    /**
     * Loads the ViewModel to serve ads
     */
    public void loadAdViewModel(ViewDataBinding binding) {

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
