package project.hikerguide.ui.dialogs;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import project.hikerguide.R;
import project.hikerguide.databinding.DialogProgressBinding;

/**
 * Created by Alvin on 8/9/2017.
 */

public class ProgressDialog extends DialogFragment {
    // ** Member Variables ** //
    private DialogProgressBinding mBinding;
    private String mTitle;
    private boolean mIndeterminate;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Inflate the layout for the Dialog
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_progress, null, false);

        // Build the Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        mBinding.dialogPb.setIndeterminate(mIndeterminate);
        builder.setView(mBinding.getRoot());

        if (mTitle != null) {

            // Set the Title of the Dialog
            builder.setTitle(mTitle);
        }

        return builder.create();
    }

    /**
     * Updates the ProgressBar of the DialogFragment
     *
     * @param progress    Progress to update to
     */
    public void updateProgress(int progress) {

        mBinding.dialogPb.setProgress(progress);
    }

    /**
     * Sets the title of the DialogFragment
     *
     * @param title    Title of the Dialog
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    /**
     * Sets whether the ProgressBar style should be indeterminate
     *
     * @param indeterminate    Boolean value for whether the ProgressBar's style should be
     *                         indeterminate
     */
    public void setIndeterminate(boolean indeterminate) {
        mIndeterminate = indeterminate;
    }
}
