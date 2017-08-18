package project.hikerguide.utilities;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by Alvin on 8/18/2017.
 */

public class GeneralUtils {

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
}
