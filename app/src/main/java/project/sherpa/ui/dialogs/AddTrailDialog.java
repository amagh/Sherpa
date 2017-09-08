package project.sherpa.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import project.sherpa.R;
import project.sherpa.databinding.DialogAddTrailBinding;
import project.sherpa.models.datamodels.Trail;

/**
 * Created by Alvin on 8/3/2017.
 */

public class AddTrailDialog extends DialogFragment {
    // ** Member Variables ** //
    DialogAddTrailBinding mBinding;
    DialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_add_trail, null, false);
        mBinding.addTrailTv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionCode, KeyEvent keyEvent) {

                if (actionCode == EditorInfo.IME_ACTION_GO) {

                    // Create a trail from the input text
                    createNewTrail(textView.getText().toString());

                    return true;
                }

                return false;
            };
        });

        // Set the layout for the Dialog. Set the title for the Dialog.
        builder.setView(mBinding.getRoot());
        builder.setTitle(getActivity().getString(R.string.add_trail_title));

        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String trailName = mBinding.addTrailTv.getText().toString().trim();

                // Create a new Trail from the input text
                createNewTrail(trailName);

            }
        })
        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });

        return builder.create();
    }

    public void setDialogListener(DialogListener listener) {
        mListener = listener;
    }

    public interface DialogListener {
        void onTrailNamed(Trail trail);
    }

    /**
     * Validates the input text and creates a new Trail if valid
     *
     * @param trailName    The input from the user for the Trail's name
     */
    private void createNewTrail(String trailName) {

        // Check to ensure the user entered text
        if (trailName.isEmpty()) {

            // Notify the user
            Toast.makeText(getActivity(), getActivity().getString(R.string.add_trail_warn_add_text), Toast.LENGTH_LONG).show();
        } else if (mListener != null) {

            // Create a new Trail from the entered text
            Trail trail = new Trail();
            trail.name = trailName;

            mListener.onTrailNamed(trail);
        }
    }
}
