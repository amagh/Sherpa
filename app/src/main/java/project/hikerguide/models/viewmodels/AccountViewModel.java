package project.hikerguide.models.viewmodels;

import android.animation.ValueAnimator;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.IntDef;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.databinding.library.baseAdapters.BR;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.R;

import static project.hikerguide.models.viewmodels.AccountViewModel.UiModes.CREATE_ACCOUNT;
import static project.hikerguide.models.viewmodels.AccountViewModel.UiModes.SIGN_IN;

/**
 * Created by Alvin on 7/28/2017.
 */

public class AccountViewModel extends BaseObservable {
    // ** Constants ** //
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SIGN_IN, CREATE_ACCOUNT})
    @interface UiModes {
        int SIGN_IN = 0;
        int CREATE_ACCOUNT = 1;
    }

    // ** Member Variables ** //
    private Context mContext;
    @UiModes private int mUiMode;

    private String email;
    private String password;
    private String passwordConfirm;

    public AccountViewModel(Context context) {
        mContext = context;
    }

    @Bindable
    public @AccountViewModel.UiModes int getUiMode() {
        return mUiMode;
    }

    @Bindable
    public String getPasswordHint() {
        switch (mUiMode) {
            case SIGN_IN: return mContext.getString(R.string.sign_in_password_hint);
            case CREATE_ACCOUNT: return mContext.getString(R.string.sign_in_password_confirm);
        }

        return null;
    }

    @Bindable
    public String getSwitchBtnText() {
        switch(mUiMode) {
            case SIGN_IN: return mContext.getString(R.string.create_account_button_text);
            case CREATE_ACCOUNT: return mContext.getString(R.string.return_to_sign_in_button_text);
        }

        return null;
    }

    @BindingAdapter({"app:passwordTv", "app:confirmTv", "app:signInBtn", "app:createAccountBtn", "bind:switchBtnText", "bind:uiMode"})
    public static void switchUi(Button switchUiButton, EditText passwordTv, EditText confirmTv,
                                Button signInButton, Button createAccountButton, String switchBtnText, @UiModes int uiMode) {

        // Default state variables
        float confirmAlpha          = 0;
        float signInAlpha           = 1;
        float height                = 0;

        // Modify variables if in CREATE_ACCOUNT mode
        if (uiMode == CREATE_ACCOUNT) {
            signInAlpha = 0;                                        // Hide sign in button
            confirmAlpha = 1;                                       // Show password confirmation
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) passwordTv.getLayoutParams();
            height = params.topMargin + passwordTv.getHeight();     // Move password field down
            confirmTv.setVisibility(View.VISIBLE);                  // Show confirm password field
        }

        // Animate state changes
        new AdditiveAnimator().setDuration(150)
                .target(switchUiButton).alpha(0)
                .target(signInButton).alpha(signInAlpha)
                .target(createAccountButton).alpha(confirmAlpha)
                .target(passwordTv).translationY(height)
                .target(confirmTv).alpha(confirmAlpha)
                .start();

        // Switch text on the button
        switchUiButton.setText(switchBtnText);

        // Re-show the switch UI button
        AdditiveAnimator.animate(switchUiButton).alpha(1).start();

        // If in sign-in mode, hide the confirm password EditText so that pressing "Next" on the
        // email EditText, skips directly to the password EditText
        if (uiMode == SIGN_IN) {
            confirmTv.setVisibility(View.GONE);
        }
    }

    /**
     * Changes the UI to add a password confirmation EditText so that the user can create an
     * account
     *
     * @param view    The Button being clicked
     */
    public void onClickSwitchUi(View view) {
        switch (mUiMode) {
            case SIGN_IN: mUiMode = CREATE_ACCOUNT;
                break;
            case CREATE_ACCOUNT: mUiMode = SIGN_IN;
                break;
        }

        notifyPropertyChanged(BR._all);
    }

    public void onClickSignIn(View view) {
        // Sign in
    }

    public void onClickCreateAccount(View view) {
        // Create account
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
