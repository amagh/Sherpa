package project.hikerguide.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Toast;

import project.hikerguide.R;
import project.hikerguide.databinding.DialogAddTrailBinding;
import project.hikerguide.models.datamodels.Trail;

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
        builder.setView(mBinding.getRoot());
        builder.setTitle("Add a New Trail");

        builder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String trailName = mBinding.addTrailTv.getText().toString().trim();

                if (trailName.isEmpty()) {
                    Toast.makeText(getActivity(), "Please add a trail name before continuing.", Toast.LENGTH_LONG).show();
                } else if (mListener != null) {
                    Trail trail = new Trail();
                    trail.name = trailName;

                    mListener.onTrailNamed(trail);
                }
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
}
