package project.sherpa.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import project.sherpa.R;

/**
 * Created by Alvin on 9/28/2017.
 */

public class AcceptFriendRequestDialog extends DialogFragment {

    // ** Member Variables ** //
    private String mUserName;
    private DialogInterface.OnClickListener mPositiveClickListener;
    private DialogInterface.OnClickListener mNegativeClickListener;

    /**
     * Factory patterns for instantiating AcceptFriendRequestDialog
     *
     * @param username                 The username to be displayed
     * @param positiveClickListener    ClickListener for the positive click
     * @param negativeClickListener    ClickListener for the negative click
     *
     * @return AcceptFriendRequestDialog with parameters in the signature set as member variables
     */
    public static AcceptFriendRequestDialog newInstance(String username,
                                                        DialogInterface.OnClickListener positiveClickListener,
                                                        DialogInterface.OnClickListener negativeClickListener) {

        // Init the Dialog
        AcceptFriendRequestDialog dialog = new AcceptFriendRequestDialog();

        // Set the field using the parameters
        dialog.mUserName = username;
        dialog.mPositiveClickListener = positiveClickListener;
        dialog.mNegativeClickListener = negativeClickListener;

        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Create the AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // Set the message, positive, and negative click listeners
        builder.setMessage(getString(R.string.accept_friend_request_dialog_message_text, mUserName))
                .setPositiveButton(getString(R.string.accept_friend_request_dialog_accept_text), mPositiveClickListener)
                .setNegativeButton(getString(R.string.accept_friend_request_dialog_reject_text), mNegativeClickListener);

        return builder.create();
    }
}
