package project.hikerguide.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.EditText;

import com.android.databinding.library.baseAdapters.BR;

import project.hikerguide.R;

/**
 * Created by Alvin on 7/28/2017.
 */

public class AccountViewModel extends BaseObservable {
    // ** Member Variables ** //
    private Context mContext;
    private boolean mCreateAccount;

    private String email;
    private String password;
    private String passwordConfirm;

    public AccountViewModel(Context context) {
        mContext = context;
    }

    @Bindable
    public String getPasswordHint() {
        if (!mCreateAccount) {
            return mContext.getString(R.string.sign_in_password_hint);
        } else {
            return mContext.getString(R.string.sign_in_password_confirm);
        }
    }

    @Bindable
    public String getSignInButtonText() {
        if (!mCreateAccount) {
            return mContext.getString(R.string.sign_in_button_text);
        } else {
            return mContext.getString(R.string.create_account_button_text);
        }
    }

    @Bindable
    public String getCreateAccountButtonText() {
        if (!mCreateAccount) {
            return mContext.getString(R.string.create_account_button_text);
        } else {
            return mContext.getString(R.string.return_to_sign_in_button_text);
        }
    }

    @Bindable
    public int getPasswordConfirmVisibility() {
        if (!mCreateAccount) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @BindingAdapter("bind:passwordConfirmVisibility")
    public static void animatePasswordConfirm(EditText editText, int visibility) {

    }

    public void onClickCreateAccount(View view) {
        if (!mCreateAccount) {
            mCreateAccount = true;
            notifyPropertyChanged(BR._all);
        } else {
            mCreateAccount = false;
            //  Return to sign in
        }
    }

    public void onClickSignIn(View view) {
        if (!mCreateAccount) {
            // Sign in
        } else {
            if (!password.equals(passwordConfirm)) {
                // Passwords are different
            } else if (password.length() < 8 || passwordConfirm.length() < 8) {
                // Password does not meet min length
            } else {
                // Create Account
            }
        }
    }

    @Bindable
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Bindable
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Bindable
    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
