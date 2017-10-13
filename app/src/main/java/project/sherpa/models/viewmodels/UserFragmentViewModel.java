package project.sherpa.models.viewmodels;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.support.annotation.IntDef;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import com.android.databinding.library.baseAdapters.BR;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import droidninja.filepicker.FilePickerConst;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.sherpa.R;
import project.sherpa.models.datamodels.Author;
import project.sherpa.ui.activities.ChatActivity;
import project.sherpa.ui.activities.FriendActivity;
import project.sherpa.ui.behaviors.VanishingBehavior;
import project.sherpa.ui.fragments.UserFragment;
import project.sherpa.utilities.GeneralUtils;

import static project.sherpa.models.viewmodels.UserFragmentViewModel.FriendIconTypes.*;
import static project.sherpa.models.viewmodels.UserFragmentViewModel.MessageIconTypes.*;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_BACKDROP;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_PROFILE_PIC;

/**
 * Created by Alvin on 10/2/2017.
 */

public class UserFragmentViewModel extends BaseObservable {

    // ** Constants ** //
    @IntDef({INVALID, CONNECT, SOCIAL, SOCIAL_WITH_REQUEST})
    @interface FriendIconTypes {
        int INVALID                 = -1;
        int CONNECT                 = 0;
        int SOCIAL                  = 1;
        int SOCIAL_WITH_REQUEST     = 2;
    }

    @IntDef({MESSAGE, NEW_MESSAGE})
    @interface MessageIconTypes {
        int MESSAGE     = 0;
        int NEW_MESSAGE = 1;
    }
    
    // ** Member Variables ** //
    private Author mUser;
    private UserFragment mFragment;
    private boolean mEditMode;
    private boolean mHasNewMessages;


    public UserFragmentViewModel(Author user, UserFragment fragment) {
        mUser = user;
        mFragment = fragment;
    }

    @Bindable
    public int getFabVisibility() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || !user.getUid().equals(mUser.firebaseId)) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @BindingAdapter("fabVisibility")
    public static void setFabVisibility(FabSpeedDial fab, int fabVisibility) {

        // Set the Visibility of the FAB
        fab.setVisibility(fabVisibility);
    }

    @Bindable
    public boolean getInEditMode() {
        return mEditMode;
    }

    public void setInEditMode(boolean isInEditMode) {
        mEditMode = isInEditMode;

        notifyPropertyChanged(BR.inEditMode);
    }

    @BindingAdapter({"messageIv", "inEditMode"})
    public static void animateSocialVisibility(final ImageView friendIv, final ImageView messageIv, boolean inEditMode) {

        // User is not logged in, no social buttons to animate
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Get the parameters for the parent ViewGroup so that the Behavior can be modified
        ConstraintLayout layout = (ConstraintLayout) friendIv.getParent();
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) layout.getLayoutParams();

        if (inEditMode) {
            if (friendIv.getAlpha() == 0) {

                // Views are already hidden. No need to animate. Just set visibility to GONE
                friendIv.setVisibility(View.GONE);
                messageIv.setVisibility(View.GONE);
                return;
            }

            // Disable Behavior so animation does not clash
            params.setBehavior(null);

            // Hide the social buttons as they do not need to be visible when editing profile
            new AdditiveAnimator().setDuration(300)
                    .target(friendIv).scale(0).alpha(0)
                    .target(messageIv).scale(0).alpha(0)
                    .addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            // Set the visibility to GONE
                            friendIv.setVisibility(View.GONE);
                            messageIv.setVisibility(View.GONE);

                            // Reset the layout Behavior so that the profile image still animates
                            params.setBehavior(new VanishingBehavior());
                        }
                    })
                    .start();

        } else {

            if (friendIv.getAlpha() == 0) {

                // Views are invisible, so just set visibility to VISIBLE
                friendIv.setVisibility(View.VISIBLE);
                messageIv.setVisibility(View.VISIBLE);
                return;
            }

            // Disable Behavior so animation does not clash
            params.setBehavior(null);

            // Hide the Views temporarily
            new AdditiveAnimator().setDuration(0)
                    .target(friendIv).scale(0).alpha(0)
                    .target(messageIv).scale(0).alpha(0)
                    .start();

            // Animate the Views in
            new AdditiveAnimator().setDuration(300)
                    .target(friendIv).scale(1).alpha(1)
                    .target(messageIv).scale(1).alpha(1)
                    .addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);

                            // Set visibility to VISIBLE on animation start
                            friendIv.setVisibility(View.VISIBLE);
                            messageIv.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            // Reset the Behavior so it acts normally
                            params.setBehavior(new VanishingBehavior());
                        }
                    })
                    .start();
        }
    }

    @Bindable
    public int getSocialVisibility() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Only show social buttons if the user is logged in
        if (user != null) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    public void setHasNewMessages(boolean hasNewMessages) {
        mHasNewMessages = hasNewMessages;
        notifyPropertyChanged(BR.messageIcon);
    }

    @Bindable
    @MessageIconTypes
    public int getMessageIcon() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Checks to see whether this process should check for new messages
        if (user == null) return MESSAGE;
        if (!user.getUid().equals(mUser.firebaseId)) return MESSAGE;
        if (mUser.getChats() == null) return MESSAGE;

        // Boolean is already set to display new messages, no need to check for new messages
        if (mHasNewMessages) {
            mHasNewMessages = false;
            return NEW_MESSAGE;
        }

        return MESSAGE;
    }

    @BindingAdapter("messageIcon")
    public static void loadMessageIcon(ImageView messageImageView, @MessageIconTypes int messageIcon) {

        Context context = messageImageView.getContext();
        switch (messageIcon) {
            case MESSAGE:
                messageImageView.setBackground(ContextCompat.getDrawable(
                        context,
                        R.drawable.social_button_background));
                break;

            case NEW_MESSAGE:
                messageImageView.setBackground(ContextCompat.getDrawable(
                        context,
                        R.drawable.social_button_notification_background));
                break;
        }
    }

    @Bindable
    @FriendIconTypes
    public int getFriendIcon() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return INVALID;

        if (user.getUid().equals(mUser.firebaseId) && mUser.getReceivedRequests() != null) {
            return SOCIAL_WITH_REQUEST;
        } else if (user.getUid().equals(mUser.firebaseId) && mUser.getReceivedRequests() == null) {
            return SOCIAL;
        } else {
            return CONNECT;
        }
    }

    @BindingAdapter("friendIcon")
    public static void loadFriendIcon(ImageView friendImageView, @FriendIconTypes int friendIcon) {

        Context context = friendImageView.getContext();

        switch (friendIcon) {
            case SOCIAL_WITH_REQUEST:
                friendImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_group));
                friendImageView.setBackground(ContextCompat.getDrawable(
                        context,
                        R.drawable.social_button_notification_background));
                break;

            case SOCIAL:
                friendImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_group));
                friendImageView.setBackground(ContextCompat.getDrawable(
                        context,
                        R.drawable.social_button_background));
                break;

            case CONNECT:
                friendImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_person_add));
                friendImageView.setBackground(ContextCompat.getDrawable(
                        context,
                        R.drawable.social_button_background));
        }
    }

    public void onClickBackdrop(View view) {

        // Check to ensure the user is clicking their own backdrop image
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(mUser.firebaseId) || !mEditMode) {
            return;
        }

        // Open FilePicker for selecting backdrop image
        GeneralUtils.openFilePicker(
                mFragment,
                REQUEST_CODE_BACKDROP,
                FilePickerConst.FILE_TYPE_MEDIA);
    }

    public void onClickProfileImage(View view) {

        // Check to ensure the user is clicking their own profile image
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.getUid().equals(mUser.firebaseId) || !mEditMode) {
            return;
        }

        // Open FilePicker for selecting profile image
        GeneralUtils.openFilePicker(
                mFragment,
                REQUEST_CODE_PROFILE_PIC,
                FilePickerConst.FILE_TYPE_MEDIA);
    }

    /**
     * Click response for the messaging button in the UserFragment
     *
     * @param view    View that was clicked
     */
    public void onClickMessage(View view) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid().equals(mUser.firebaseId)) {
            Intent intent = new Intent(mFragment.getActivity(), ChatActivity.class);
            mFragment.startActivity(intent);
        } else {
            mFragment.startMessageActivityToUser();
        }
    }

    public void onClickFriend(View view) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getUid().equals(mUser.firebaseId)) {
            Intent intent = new Intent(mFragment.getActivity(), FriendActivity.class);
            mFragment.startActivity(intent);
        } else {
            mFragment.startFriendFollowActivity();
        }
    }
}
