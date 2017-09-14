package project.sherpa.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import droidninja.filepicker.FilePickerConst;
import project.sherpa.R;
import project.sherpa.filepicker.CustomFilePickerBuilder;

import static project.sherpa.utilities.FirebaseProviderUtils.GPX_EXT;

/**
 * Created by Alvin on 8/18/2017.
 */

public class GeneralUtils {
    // ** Constants ** //
    private static final int PERMISSION_REQUEST_EXT_STORAGE = 9687;
    public static final int DEFAULT_REQUEST_CODE = -1;

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
        if (view == null) return;

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Opens Android-FilePicker Activity so the user may select a File to add to the Guide
     *
     * @param activity           Activity to pass onActivityResult to. Also used as interface to
     *                           global Context
     * @param requestCode        Request code to be used for ActivityResult
     * @param type               Type of file to be selected. Either document or media.
     */
    public static void openFilePicker(@NonNull Activity activity, int requestCode, int type) {
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

                    CustomFilePickerBuilder builder = CustomFilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .addFileSupport(activity.getString(R.string.gpx_label), new String[]{GPX_EXT})
                            .enableDocSupport(false);

                    if (requestCode != DEFAULT_REQUEST_CODE) {
                        builder.setRequestCode(requestCode);
                    }

                    builder.pickFile(activity);

                    break;
                }

                case FilePickerConst.FILE_TYPE_MEDIA: {

                    CustomFilePickerBuilder builder = CustomFilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .enableVideoPicker(false);

                    if (requestCode != DEFAULT_REQUEST_CODE) {
                        builder.setRequestCode(requestCode);
                    }

                    builder.pickPhoto(activity);

                    break;
                }
            }
        }
    }

    /**
     * Opens Android-FilePicker Activity so the user may select a File to add to the Guide
     *
     * @param fragment       Fragment calling for the FilePicker
     * @param requestCode    RequestCode for ActivityResult
     * @param type               Type of file to be selected. Either document or media.
     */
    public static void openFilePicker(@NonNull Fragment fragment, int requestCode, int type) {
        // Check to ensure the app has the required permissions first
        if (ContextCompat.checkSelfPermission(fragment.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Request permission to read external storage
            requestPermission(
                    fragment.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_EXT_STORAGE);
        } else {

            // Launch the FilePicker based on the type of file to be added to the Guide
            switch (type) {
                case FilePickerConst.FILE_TYPE_DOCUMENT: {

                    CustomFilePickerBuilder builder = CustomFilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .addFileSupport(fragment.getString(R.string.gpx_label), new String[]{GPX_EXT})
                            .enableDocSupport(false);

                    if (requestCode != DEFAULT_REQUEST_CODE) {
                        builder.setRequestCode(requestCode);
                    }

                    builder.pickFile(fragment);

                    break;
                }

                case FilePickerConst.FILE_TYPE_MEDIA: {

                    CustomFilePickerBuilder builder = CustomFilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .enableVideoPicker(false);

                    if (requestCode != DEFAULT_REQUEST_CODE) {
                        builder.setRequestCode(requestCode);
                    }

                    builder.pickPhoto(fragment);

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

    /**
     * Checks the users unit preferences and returns a boolean value for whether that preference is
     * for metric units
     *
     * @param context    Interface to global Context
     * @return True if user prefers metric. False otherwise.
     */
    public static boolean isUnitPreferenceMetric(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getString(
                context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_default))
                .equals(context.getString(R.string.pref_units_metric));
    }
}
