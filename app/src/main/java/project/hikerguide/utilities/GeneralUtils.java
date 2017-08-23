package project.hikerguide.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import project.hikerguide.R;

import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_EXT;

/**
 * Created by Alvin on 8/18/2017.
 */

public class GeneralUtils {
    // ** Constants ** //
    private static final int PERMISSION_REQUEST_EXT_STORAGE = 9687;

    /**
     * Shows the soft keyboard
     *
     * @param context     Interface to global Context
     * @param editText    EditText to focus on
     */
    public static void showKeyboard(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager. SHOW_IMPLICIT);
    }

    /**
     * Hides the soft keyboard
     *
     * @param context    Interface to global Context
     * @param view       View to use to get the WindowToken
     */
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Opens Android-FilePicker Activity so the user may select a File to add to the Guide
     *
     * @param activity  Activity to pass onActivityResult to
     * @param type      Type of file to be selected. Either document or media.
     */
    public static void openFilePicker(Activity activity, int type) {
        // Check to ensure the app has the required permissions first
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Request permission to read external storage
            requestPermission(
                    activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_EXT_STORAGE);
        } else {

            // Launch the FilePicker based on the type of file to be added to the Guide
            switch (type) {
                case FilePickerConst.FILE_TYPE_DOCUMENT: {

                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .addFileSupport(activity.getString(R.string.gpx_label), new String[]{GPX_EXT})
                            .enableDocSupport(false)
                            .pickFile(activity);

                    break;
                }

                case FilePickerConst.FILE_TYPE_MEDIA: {

                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .enableVideoPicker(false)
                            .pickPhoto(activity);

                    break;
                }
            }
        }

    }

    /**
     * Requests Android permission for the application
     *
     * @param activity       The Activity to return the permission request to
     * @param permissions    The Permission(s) to request
     * @param requestCode    Code to request it with. For identifying the result.
     */
    public static void requestPermission(Activity activity, String[] permissions, int requestCode) {

        ActivityCompat.requestPermissions(activity,
                permissions,
                requestCode);
    }
}
