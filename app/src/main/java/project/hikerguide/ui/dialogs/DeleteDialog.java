package project.hikerguide.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import project.hikerguide.R;

/**
 * Created by Alvin on 9/5/2017.
 */

public class DeleteDialog extends DialogFragment {

    // ** Member Variables ** //
    private DialogInterface.OnClickListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Init the AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set parameters for Dialog
        builder.setTitle(getString(R.string.delete_dialog_title));
        builder.setMessage(getString(R.string.delete_dialog_message));

        // Set positive and negative buttons
        builder.setPositiveButton(getString(android.R.string.ok), mListener)
        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });

        return builder.create();
    }

    /**
     * Sets the OnClickListener to be used on the positive button click
     *
     * @param listener    OnClickListener for positive button click
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mListener = listener;
    }
}
