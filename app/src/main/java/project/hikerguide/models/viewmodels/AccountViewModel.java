package project.hikerguide.models.viewmodels;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.hikerguide.R;
import project.hikerguide.ui.activities.AccountActivity;
import project.hikerguide.ui.activities.UserActivity;

import static project.hikerguide.models.viewmodels.AccountViewModel.UiModes.CREATE_ACCOUNT;
import static project.hikerguide.models.viewmodels.AccountViewModel.UiModes.SIGN_IN;
import static project.hikerguide.utilities.Constants.IntentKeys.AUTHOR_KEY;

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

    private static final String emailValidationRegex = "^(?:(?:[\\w`~!#$%^&*\\-=+;:{}'|,?\\/]+(?:(?:\\.(?:\"(?:\\\\?[\\w`~!#$%^&*\\-=+;:{}'|,?\\/\\.()<>\\[\\] @]|\\\\\"|\\\\\\\\)*\"|[\\w`~!#$%^&*\\-=+;:{}'|,?\\/]+))*\\.[\\w`~!#$%^&*\\-=+;:{}'|,?\\/]+)?)|(?:\"(?:\\\\?[\\w`~!#$%^&*\\-=+;:{}'|,?\\/\\.()<>\\[\\] @]|\\\\\"|\\\\\\\\)+\"))@(?:[a-zA-Z\\d\\-]+(?:\\.[a-zA-Z\\d\\-]+)*|\\[\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\])$";

    // ** Member Variables ** //
    private Context mContext;
    @UiModes private int mUiMode;

    private String email;
    private String password;
    private String passwordConfirm;
    private int progressVisibility = View.GONE;

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

    @Bindable
    public int getProgressVisibility() {
        return progressVisibility;
    }

    @BindingAdapter({"passwordTv", "confirmTv", "signInBtn", "createAccountBtn", "switchBtnText", "uiMode"})
    public static void switchUi(Button switchUiButton, final EditText passwordTv, final EditText confirmTv,
                                Button signInButton, Button createAccountButton, String switchBtnText, @UiModes int uiMode) {

        // Default state variables
        float confirmAlpha          = 0;
        float signInAlpha           = 1;
        float height                = 0;

        if (confirmTv.getHeight() != 0) {
            // Modify variables if in CREATE_ACCOUNT mode
            if (uiMode == CREATE_ACCOUNT) {
                signInAlpha = 0;                                        // Hide sign in button
                confirmAlpha = 1;                                       // Show password confirmation
                confirmTv.setVisibility(View.VISIBLE);                  // Show confirm password field
                createAccountButton.setVisibility(View.VISIBLE);        // Allow click create acct button
                signInButton.setVisibility(View.GONE);                  // Prevent click sign in button
            } else {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) confirmTv.getLayoutParams();
                height = -(params.topMargin + passwordTv.getHeight());  // Move password field up
                signInButton.setVisibility(View.VISIBLE);               // Allow click sign in button
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
                createAccountButton.setVisibility(View.GONE);
            }
        } else {
            confirmTv.post(new Runnable() {
                @Override
                public void run() {
                    passwordTv.setY(confirmTv.getY());
                    confirmTv.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * Changes the UI to add a password confirmation EditText so that the user can create an
     * account
     *
     * @param view    The Button being clicked
     */
    public void onClickSwitchUi(View view) {
        // Switch the UI Mode
        switch (mUiMode) {
            case SIGN_IN: mUiMode = CREATE_ACCOUNT;
                break;
            case CREATE_ACCOUNT: mUiMode = SIGN_IN;
                break;
        }

        notifyPropertyChanged(BR._all);
    }

    /**
     * Signs a user in using FirebaseAuth
     *
     * @param view    View that was clicked
     */
    public void onClickSignIn(View view) {
        // Check that the email and password are valid
        if (email == null || !email.matches(emailValidationRegex)) {
            Toast.makeText(mContext, mContext.getString(R.string.invalid_email_error), Toast.LENGTH_LONG).show();
            return;
        } else if (password == null) {
            Toast.makeText(mContext, mContext.getString(R.string.no_password_error), Toast.LENGTH_LONG).show();
            return;
        }

        // Make ProgressBar visible
        progressVisibility = View.VISIBLE;
        notifyPropertyChanged(BR.progressVisibility);

        // Attempt to sign in with credentials
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        // Hide ProgressBar
                        progressVisibility = View.GONE;
                        notifyPropertyChanged(BR.progressVisibility);

                        if (task.isSuccessful()) {
                            Intent intent = new Intent();
                            intent.putExtra(AUTHOR_KEY, task.isSuccessful());

                            ((AccountActivity) mContext).setResult(Activity.RESULT_OK, intent);
                            ((AccountActivity) mContext).finish();
                        } else {
                            Toast.makeText(mContext, mContext.getString(R.string.authentication_error), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Creates a new account using FirebaseAuth
     *
     * @param view    View that was clicked
     */
    public void onClickCreateAccount(View view) {

        // Check that email and password are valid
        if (email == null || !email.matches(emailValidationRegex)) {
            Toast.makeText(mContext, mContext.getString(R.string.invalid_email_error), Toast.LENGTH_LONG).show();
        } else if (password == null || passwordConfirm == null) {
            Toast.makeText(mContext, mContext.getString(R.string.no_password_error), Toast.LENGTH_LONG).show();
        } else if (!password.equals(passwordConfirm)) {
            Toast.makeText(mContext, mContext.getString(R.string.no_password_match), Toast.LENGTH_LONG).show();
        } else if (password.length() < 8) {
            Toast.makeText(mContext, mContext.getString(R.string.invalid_password_length_error), Toast.LENGTH_LONG).show();
        } else {

            // Make ProgressBar visible
            progressVisibility = View.VISIBLE;
            notifyPropertyChanged(BR.progressVisibility);

            // Attempt to create a new account
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            // Hide ProgressBar
                            progressVisibility = View.GONE;
                            notifyPropertyChanged(BR.progressVisibility);

                            if (task.isSuccessful()) {
                                mContext.startActivity(new Intent(mContext, UserActivity.class));
                                ((Activity) mContext).finish();
                            }
                        }
                    });
        }
    }

    /**
     * Listener for EditorAction on password EditText
     *
     * @param textView    The password EditText
     * @param actionId    The IME action that was performed
     * @param event       The event triggering the IME action
     * @return True if EditorAction was handled. False otherwise
     */
    public boolean onPasswordEditorAction(TextView textView, int actionId, KeyEvent event) {

        // Check that the ActionId is IME_ACTION_DONE
        if (actionId == EditorInfo.IME_ACTION_DONE) {

            // Sign in / create an account depending on the UI Mode
            switch(mUiMode) {
                case SIGN_IN:
                    onClickSignIn(textView);
                    break;
                case CREATE_ACCOUNT:
                    onClickCreateAccount(textView);
                    break;
            }

            return true;
        }

        return false;
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
