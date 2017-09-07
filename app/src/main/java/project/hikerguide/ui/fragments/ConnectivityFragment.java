package project.hikerguide.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.FirebaseDatabase;

import project.hikerguide.R;
import project.hikerguide.ui.activities.ConnectivityActivity;

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
            ((ConnectivityActivity) getActivity()).setConnectivityCallback(this);
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
