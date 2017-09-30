package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import project.sherpa.BR;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.activities.SearchUserActivity;

/**
 * Created by Alvin on 9/29/2017.
 */

public class ListItemFriendViewModel extends BaseObservable {

    // ** Member Variables ** //
    private Author mUser;
    private boolean mShowSocialButton = false;

    public ListItemFriendViewModel(Author user) {
        mUser = user;
    }

    @Bindable
    public int getSocialButtonVisibility() {
        return mShowSocialButton
                ? View.VISIBLE
                : View.GONE;
    }

    public void setShowSocialButton(boolean showSocialButton) {
        mShowSocialButton = showSocialButton;
        notifyPropertyChanged(BR.socialButtonVisibility);
    }

    public void onClickSocialButton(View view) {
        if (view.getContext() instanceof SearchUserActivity) {
            ((SearchUserActivity) view.getContext()).followOrFriendUser(mUser);
        }
    }
}
