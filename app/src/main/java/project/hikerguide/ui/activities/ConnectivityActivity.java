package project.hikerguide.ui.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Alvin on 8/9/2017.
 */

public class ConnectivityActivity extends AppCompatActivity {
    // ** Constants ** //
    private static final int PERMISSION_REQUEST_ACCESS_NETWORK_STATE = 5614;

    // ** Member Variables ** //
    private ConnectivityListener mConnectivityListener;
    private Set<ConnectivityCallback> mCallbackSet;
    private boolean connectivityRegistered;

    @Override
    protected void onStart() {
        super.onStart();

        if (!connectivityRegistered) {
            registerConnectivityListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (connectivityRegistered) {
            unregisterConnectivityListener();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_ACCESS_NETWORK_STATE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerConnectivityListener();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

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
