package project.sherpa.models.viewmodels;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;
import android.widget.Toast;

import project.sherpa.R;
import project.sherpa.models.datamodels.Author;
import timber.log.Timber;

import static project.sherpa.models.datamodels.Author.AuthorLists.*;

/**
 * Created by Alvin on 9/27/2017.
 */

public class FriendFollowViewModel extends BaseObservable {

    private Context mContext;
    private Author mSendingUser;
    private Author mReceivingUser;

    public FriendFollowViewModel(Context context, Author sendingUser, Author receivingUser) {
        mContext = context;
        mSendingUser = sendingUser;
        mReceivingUser = receivingUser;
    }

    @Bindable
    public String getFriendRequestText() {

        // Change the text for the Friend Request option
        if (mSendingUser.getFriends() != null &&
                mSendingUser.getFriends().contains(mReceivingUser.firebaseId)) {

            // Users are already friends. Show text to un-friend the user
            return mContext.getString(R.string.friend_follow_un_friend_text);
        } else if (mSendingUser.getReceivedRequests() != null &&
                mSendingUser.getReceivedRequests().contains(mReceivingUser.firebaseId)) {

            // User has already received a friend request from this user. Show text to accept
            return mContext.getString(R.string.friend_follow_respond_friend_request_text);

        } else {
            // User has not received a request from this user. Show text to send a request
            return mContext.getString(R.string.friend_follow_friend_request_text);
        }


    }

    @Bindable
    public int getFollowVisibility() {

        if (mSendingUser == null || mReceivingUser == null) {
            return View.GONE;
        }

        // Change text for follow option
        if (mSendingUser.getFriends() != null &&
                mSendingUser.getFriends().contains(mReceivingUser.firebaseId)) {

            // User is a friend with this user. Must choose to un-friend
            return View.GONE;
        } else {

            return View.VISIBLE;
        }
    }

    @Bindable
    public int getFriendVisibility() {
        if (mSendingUser == null || mReceivingUser == null) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @Bindable
    public String getFollowText() {

        if (mSendingUser == null) {
            return null;
        }

        if (mSendingUser.getFollowing() != null &&
                mSendingUser.getFollowing().contains(mReceivingUser.firebaseId)) {

            // User is already following this author. Show text to stop following this author
            return mContext.getString(R.string.friend_follow_un_follow_text);
        } else {

            // User is not following this author. Show text to start following this author
            return mContext.getString(R.string.friend_follow_follow_text);
        }
    }

    public void onClickFriend(View view) {

        if (mSendingUser.getFriends() != null &&
                mSendingUser.getFriends().contains(mReceivingUser.firebaseId)) {

            // Users are already friends. Un-friend them
            mSendingUser.removeUserFromList(FRIENDS, mReceivingUser.firebaseId);
            mReceivingUser.removeUserFromList(FRIENDS, mSendingUser.firebaseId);
        } else if (mSendingUser.getReceivedRequests() != null &&
                mSendingUser.getReceivedRequests().contains(mReceivingUser.firebaseId)){


        } else {

            // Add the Friend request to both users' lists
            mSendingUser.addUserToList(SENT_REQUESTS, mReceivingUser.firebaseId);
            mReceivingUser.addUserToList(RECEIVED_REQUESTS, mSendingUser.firebaseId);
        }
    }

    public void onClickFollow(View view) {

        if (mSendingUser.getFollowing() != null &&
                mSendingUser.getFollowing().contains(mReceivingUser.firebaseId)) {

            // User is already following this Author. Stop following
            mSendingUser.removeUserFromList(FOLLOWING, mReceivingUser.firebaseId);
            mReceivingUser.removeUserFromList(FOLLOWERS, mSendingUser.firebaseId);
        } else {

            // User is not following this Author. Start following
            mReceivingUser.addUserToList(FOLLOWERS, mSendingUser.firebaseId);
            mSendingUser.addUserToList(FOLLOWING, mReceivingUser.firebaseId);
        }
    }
}
