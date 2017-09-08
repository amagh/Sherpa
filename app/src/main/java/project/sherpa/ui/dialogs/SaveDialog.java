package project.sherpa.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import project.sherpa.R;

/**
 * Created by Alvin on 9/5/2017.
 */

public class SaveDialog extends DialogFragment {

    // ** Member Variables ** //
    private DialogInterface.OnClickListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Init the Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set Dialog parameters
        builder.setTitle(getString(R.string.save_dialog_title));
        builder.setMessage(getString(R.string.save_dialog_message));

        // Set positive and negative buttons
        builder.setPositiveButton(getString(R.string.save_dialog_confirm_exit), mListener)
        .setNegativeButton(getString(R.string.save_dialog_return), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });

        return builder.create();
    }

    /**
     * Sets the Listener used for the positive click
     *
     * @param listener    Listener to be used for the positive click
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mListener = listener;
    }
}
