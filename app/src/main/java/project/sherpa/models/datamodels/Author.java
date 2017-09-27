package project.sherpa.models.datamodels;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.models.datamodels.abstractmodels.BaseModelWithImage;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 7/17/2017.
 */

public class Author extends BaseModelWithImage implements Parcelable {

    // ** Constants ** //
    private static final String NAME                    = "name";
    private static final String USERNAME                = "username";
    private static final String LOWER_CASE_USERNAME     = "lowerCaseUsername";
    private static final String DESCRIPTION             = "description";
    private static final String LOWER_CASE_NAME         = "lowerCaseName";
    private static final String HAS_IMAGE               = "hasImage";
    private static final String SCORE                   = "score";
    private static final String FAVORITES               = "favorites";
    public static final String CHATS                    = "chats";
    public static final String FRIENDS                  = "friends";
    public static final String FOLLOWING                = "following";
    public static final String FOLLOWERS                = "followers";
    public static final String RECEIVED_REQUESTS        = "receivedRequests";
    public static final String SENT_REQUESTS            = "sentRequests";

    @IntDef({AuthorLists.FRIENDS, AuthorLists.FOLLOWING, AuthorLists.SENT_REQUESTS, AuthorLists.RECEIVED_REQUESTS})
    public @interface AuthorLists {
        int FRIENDS             = 0;
        int FOLLOWING           = 1;
        int SENT_REQUESTS       = 2;
        int RECEIVED_REQUESTS   = 3;
    }

    // ** Member Variables ** //
    public String name;
    private String username;
    public String description;
    public int score;
    public Map<String, String> favorites;
    private List<String> chats;
    private List<String> friends;
    private List<String> following;
    private List<String> followers;
    private List<String> receivedRequests;
    private List<String> sentRequests;

    public Author() {}

    public Author(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public Author(String name) {
        this.name = name;
    }

    /**
     * Creates an Author Object using values described in a Cursor
     *
     * @param cursor    Cursor describing an Author
     * @return An Author Object with values described in the Cursor
     */
    public static Author createAuthorFromCursor(Cursor cursor) {

        // Index the columns of the Cursor
        int idxFirebaseId       = cursor.getColumnIndex(GuideContract.AuthorEntry.FIREBASE_ID);
        int idxName             = cursor.getColumnIndex(GuideContract.AuthorEntry.NAME);
        int idxUsername         = cursor.getColumnIndex(GuideContract.AuthorEntry.USERNAME);
        int idxDescription      = cursor.getColumnIndex(GuideContract.AuthorEntry.DESCRIPTION);
        int idxScore            = cursor.getColumnIndex(GuideContract.AuthorEntry.SCORE);
        int idxImageUri         = cursor.getColumnIndex(GuideContract.AuthorEntry.IMAGE_URI);

        // Retrieve the values from the Cursor
        String firebaseId       = cursor.getString(idxFirebaseId);
        String name             = cursor.getString(idxName);
        String username         = cursor.getString(idxUsername);
        String description      = cursor.getString(idxDescription);
        int score               = cursor.getInt(idxScore);
        String imageUriString   = cursor.getString(idxImageUri);

        // Instantiate a new Author with the values
        Author author           = new Author();
        author.firebaseId       = firebaseId;
        author.name             = name;
        author.username         = username;
        author.description      = description;
        author.score            = score;

        if (imageUriString != null) {
            File imageFile = new File(Uri.parse(imageUriString).getPath());
            author.setImageUri(imageFile);
        }

        return author;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(NAME,                   name);
        map.put(USERNAME,               username);
        map.put(LOWER_CASE_USERNAME,    username.toLowerCase());
        map.put(DESCRIPTION,            description);
        map.put(LOWER_CASE_NAME,        name.toLowerCase());
        map.put(HAS_IMAGE,              hasImage);
        map.put(SCORE,                  score);
        map.put(FAVORITES,              favorites);
        map.put(CHATS,                  chats);
        map.put(FRIENDS,                friends);
        map.put(FOLLOWING,              following);
        map.put(FOLLOWERS,              followers);
        map.put(RECEIVED_REQUESTS,      receivedRequests);
        map.put(SENT_REQUESTS,          sentRequests);

        return map;
    }

    /**
     * Adds a Chat to the User's Firebase Profile
     *
     * @param context    Interface to global Context
     * @param chatId     FirebaseId of the Chat to be added to this Author's list of Chats
     */
    public void addChat(Context context, String chatId) {
        if (chats == null) {
            chats = new ArrayList<>();
        }

        // Add the Chat
        if (!chats.contains(chatId)) {

            chats.add(chatId);

            // Update Firebase Database
            FirebaseProviderUtils.insertOrUpdateModel(this);

            // Update the local database if modifying logged in User
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid().equals(firebaseId)) {
                ContentProviderUtils.insertModel(context, this);
            }
        }
    }

    /**
     * Removes a Chat from this Author's list of Chats
     *
     * @param context    Interface to global Context
     * @param chatId     FirebaseId of the Chat to be removed
     */
    public void removeChat(Context context, String chatId) {
        if (chats == null) return;

        // Remove the Chat
        if (chats.contains(chatId)) {

            chats.remove(chatId);

            // Update Firebase Database
            FirebaseProviderUtils.insertOrUpdateModel(this);

            // Update the local database if modifying logged in User
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid().equals(firebaseId)) {
                ContentProviderUtils.insertModel(context, this);
            }
        }
    }

    /**
     * Adds a user to one of the Author's lists
     *
     * @param listType    The type of List to modify
     * @param userId      The FirebaseId of the user to be added to the List
     */
    public void addUserToList(@AuthorLists int listType, String userId) {

        // Get a reference to the List that will be modified
        List<String> list = null;

        // Reference the List based on the listType
        switch (listType) {
            case AuthorLists.FRIENDS:           list = friends;
                break;
            case AuthorLists.FOLLOWING:         list = following;
                break;
            case AuthorLists.SENT_REQUESTS:     list = sentRequests;
                break;
            case AuthorLists.RECEIVED_REQUESTS: list = receivedRequests;
                break;
        }

        // Init the List if it does not exist
        if (list == null) {
            list = new ArrayList<>();
        }

        // Boolean check for whether to update the Firebase profile
        boolean update = false;

        // Add the user to the List
        if (!list.contains(userId)) {
            list.add(userId);

            update = true;
        }

        if (listType == AuthorLists.RECEIVED_REQUESTS && sentRequests.contains(userId) ||
                listType == AuthorLists.SENT_REQUESTS && receivedRequests.contains(userId)) {

            // If user has both sent and received a request for this user, accept the request and
            // become friends
            acceptUserAsFriend(userId);

            // Do not update the Firebase profile as it will be updated in of the methods called by
            // acceptUserAsFriend()
            update = false;
        }

        if (listType == AuthorLists.FRIENDS) {
            // Do not update the Firebase profile as it will be updated in of the methods called by
            // acceptUserAsFriend()
            update = false;
        }

        // Update the author's profile in Firebase
        if (update) FirebaseProviderUtils.insertOrUpdateModel(this);
    }

    /**
     * Removes a user from one of the Author's Lists
     *
     * @param listType    The type of List to remove the user from
     * @param userId      The FirebaseId of the user to be removed
     */
    public void removeUserFromList(@AuthorLists int listType, String userId) {

        // Get a reference to the List that will be modified
        List<String> list = null;

        // Reference the List based on the listType
        switch (listType) {
            case AuthorLists.FRIENDS:           list = friends;
                break;
            case AuthorLists.FOLLOWING:         list = following;
                break;
            case AuthorLists.SENT_REQUESTS:     list = sentRequests;
                break;
            case AuthorLists.RECEIVED_REQUESTS: list = receivedRequests;
                break;
        }

        if (list == null) return;

        // Boolean check for whether to update the Firebase profile
        boolean update = false;

        // Remove the userId from the List
        if (list.contains(userId)) {
            list.remove(userId);

            update = true;
        }

        if (listType == AuthorLists.RECEIVED_REQUESTS && sentRequests.contains(userId) ||
                listType == AuthorLists.SENT_REQUESTS && receivedRequests.contains(userId)) {

            // Do not update the Firebase profile as it will be updated by another method
            update = false;
        }

        if (update) FirebaseProviderUtils.insertOrUpdateModel(this);
    }

    /**
     * Accepts the user as a friend and adds them to the friends list
     *
     * @param friendId    The FirebaseId of the user to add to the friend list
     */
    private void acceptUserAsFriend(String friendId) {

        // Check whether the user has both sent and received a request to this user
        if (receivedRequests.contains(friendId) && sentRequests.contains(friendId)) {

            // Add the user to the friends list
            addUserToList(AuthorLists.FRIENDS, friendId);

            // Remove the user from the requests lists
            removeUserFromList(AuthorLists.SENT_REQUESTS, friendId);
            removeUserFromList(AuthorLists.RECEIVED_REQUESTS, friendId);
        }
    }

    //********************************************************************************************//
    //************************************ Getters & Setters *************************************//
    //********************************************************************************************//

    public String getUsername() {
        return username;
    }

    public List<String> getChats() {
        return chats;
    }

    public List<String> getFriends() {
        return friends;
    }

    public List<String> getFollowing() {
        return following;
    }

    public List<String> getFollowers() {
        return followers;
    }

    public List<String> getReceivedRequests() {
        return receivedRequests;
    }

    public List<String> getSentRequests() {
        return sentRequests;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setChats(List<String> chats) {
        this.chats = chats;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public void setReceivedRequests(List<String> receivedRequests) {
        this.receivedRequests = receivedRequests;
    }

    public void setSentRequests(List<String> sentRequests) {
        this.sentRequests = sentRequests;
    }

    //********************************************************************************************//
    //***************************** Parcelable Related Methods ***********************************//
    //********************************************************************************************//


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(firebaseId);
        parcel.writeString(name);
        parcel.writeString(username);
        parcel.writeString(description);
        parcel.writeInt(score);

        if (isDraft()) {
            parcel.writeInt(1);
        } else {
            parcel.writeInt(0);
        }
    }

    public static final Parcelable.Creator<Author> CREATOR = new Parcelable.Creator<Author>() {
        @Override
        public Author createFromParcel(Parcel parcel) {
            return new Author(parcel);
        }

        @Override
        public Author[] newArray(int i) {
            return new Author[i];
        }
    };

    private Author(Parcel parcel) {
        firebaseId  = parcel.readString();
        name        = parcel.readString();
        username    = parcel.readString();
        description = parcel.readString();
        score       = parcel.readInt();

        if (parcel.readInt() == 1) {
            setDraft(true);
        }
    }
}
