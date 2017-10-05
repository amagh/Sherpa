package project.sherpa.ui.activities.abstractactivities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

import project.sherpa.services.firebaseservice.FirebaseProviderService;

/**
 * Created by Alvin on 8/9/2017.
 */

public abstract class ConnectivityActivity extends AppCompatActivity {
    // ** Constants ** //
    private static final int PERMISSION_REQUEST_ACCESS_NETWORK_STATE = 5614;

    // ** Member Variables ** //
    private ConnectivityListener mConnectivityListener;
    private Set<ConnectivityCallback> mCallbackSet;
    private boolean connectivityRegistered;

    protected FirebaseProviderService mService;
    private boolean mBindService;
    private boolean mBound;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            FirebaseProviderService.FirebaseProviderBinder binder = (FirebaseProviderService.FirebaseProviderBinder) iBinder;
            mService = binder.getService();
            mBound = true;

            ConnectivityActivity.this.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        if (!connectivityRegistered) {
            registerConnectivityListener();
        }

        bindService();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (connectivityRegistered) {
            unregisterConnectivityListener();
        }

        unbindService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_ACCESS_NETWORK_STATE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerConnectivityListener();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Binds the FirebaseProviderService for ths Fragment
     */
    private synchronized void bindService() {
        if (!mBound && mBindService) {
            Intent intent = new Intent(this, FirebaseProviderService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Unbinds the FirebaseProviderService for this Fragment
     */
    private synchronized void unbindService() {
        if (mBound && mBindService) {
            unbindService(mConnection);
        }
    }

    /**
     * Sets whether the Activity should bind to FirebaseProviderService
     *
     * @param bindService    Boolean value for whether the Activity should connect to the
     *                       FirebaseProviderService
     */
    public void bindFirebaseProviderService(boolean bindService) {
        mBindService = bindService;
    }

    protected void onServiceConnected() {}

    /**
     * Sets the Callback for the ConnectivityListener
     *
     * @param callback    Callback to register to the ConnectivityListener
     */
    public void addConnectivityCallback(ConnectivityCallback callback) {

        if (mCallbackSet == null) {
            mCallbackSet = new HashSet<>();
        }

        mCallbackSet.add(callback);

        // Immediately inform the observer of the network status
        if (isConnectedToNetwork()) {
            callback.onConnected();
        } else {
            callback.onDisconnected();
        }
    }

    /**
     * Removes a Callback from the Set of Callbacks to trigger on network change
     *
     * @param callback    Callback to be removed
     */
    public void removeConnectivityCallback(ConnectivityCallback callback) {

        if (mCallbackSet != null) {
            mCallbackSet.remove(callback);
        }
    }

    /**
     * Checks whether the device is currently connected to an active network
     *
     * @return True if connected. False if not connected.
     */
    private boolean isConnectedToNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Registers a ConnectivityListener to listen for a broadcast due to change in network state
     */
    private void registerConnectivityListener() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_NETWORK_STATE},
                    PERMISSION_REQUEST_ACCESS_NETWORK_STATE);

            return;
        }

        // Initialize a ConnectivityListener if it hasn't already been initialized
        if (mConnectivityListener == null) {
            mConnectivityListener = new ConnectivityListener();
        }

        // Create an IntentFilter for listening to a Broadcast for a change in network state
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        // Register the receiver
        registerReceiver(mConnectivityListener, filter);

        // Set the boolean to indicate whether the ConnectivityListener is registered
        connectivityRegistered = true;
    }

    /**
     * Unregisters a registered ConnectivityListener
     */
    private void unregisterConnectivityListener() {
        // Unregister the ConnectivityListener
        unregisterReceiver(mConnectivityListener);

        // Set the boolean to indicate that no ConnectivityListener has been registered
        connectivityRegistered = false;
    }

    private class ConnectivityListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Notify observer of connectivity status
            if (isConnectedToNetwork() && mCallbackSet != null && mCallbackSet.size() > 0) {
                for (ConnectivityCallback callback : mCallbackSet) {
                    callback.onConnected();
                }
            } else if (mCallbackSet != null && mCallbackSet.size() > 0){
                for (ConnectivityCallback callback : mCallbackSet) {
                    callback.onDisconnected();
                }
            }
        }
    }

    public interface ConnectivityCallback {
        void onConnected();
        void onDisconnected();
    }
}
