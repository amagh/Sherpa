package project.hikerguide.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Alvin on 8/9/2017.
 */

public class ConnectivityActivity extends AppCompatActivity {
    // ** Member Variables ** //
    private ConnectivityListener mConnectivityListener;
    private ConnectivityCallback mCallback;
    private boolean connectivityRegistered;

    @Override
    protected void onStart() {
        super.onStart();

        registerConnectivityListener();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterConnectivityListener();
    }

    /**
     * Sets the Callback for the ConnectivityListener
     *
     * @param callback    Callback to register to the ConnectivityListener
     */
    public void setConnectivityCallback(ConnectivityCallback callback) {
        mCallback = callback;

        // Immediately inform the observer of the network status
        if (isConnectedToNetwork()) {
            mCallback.onConnected();
        } else {
            mCallback.onDisconnected();
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
            if (isConnectedToNetwork()) {
                if (mCallback != null) {
                    mCallback.onConnected();
                }
            } else {
                if (mCallback != null) {
                    mCallback.onDisconnected();
                }
            }
        }
    }

    public interface ConnectivityCallback {
        void onConnected();
        void onDisconnected();
    }
}
