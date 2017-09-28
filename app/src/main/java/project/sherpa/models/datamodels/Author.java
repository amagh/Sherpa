package project.sherpa.models.datamodels;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.models.datamodels.abstractmodels.BaseModelWithImage;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

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

    @IntDef({AuthorLists.FRIENDS, AuthorLists.FOLLOWING, AuthorLists.FOLLOWERS,
            AuthorLists.SENT_REQUESTS, AuthorLists.RECEIVED_REQUESTS})
    public @interface AuthorLists {
        int FRIENDS             = 0;
        int FOLLOWING           = 1;
        int FOLLOWERS           = 2;
        int SENT_REQUESTS       = 3;
        int RECEIVED_REQUESTS   = 4;
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
    public void addUserToList(@AuthorLists final int listType, final String userId) {

        // Init the Handler for the Firebase Transaction
        Transaction.Handler handler = new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                // Convert the data to an Author
                Author author = mutableData.getValue(Author.class);

                if (author == null) {

                    // Weird glitch, re-run the transaction with the same parameters
                    addUserToList(listType, userId);
                    return Transaction.abort();
                }

                // Get a reference to the List that will be modified
                List<String> list = null;

                // Reference the List based on the listType
                switch (listType) {
                    case AuthorLists.FOLLOWING:
                        list = author.getFollowing();
                        if (list == null) author.setFollowing(list = new ArrayList<>());
                        break;
                    case AuthorLists.FOLLOWERS:
                        list = author.getFollowers();
                        if (list == null) author.setFollowers(list = new ArrayList<>());
                        break;
                    case AuthorLists.SENT_REQUESTS:
                        list = author.getSentRequests();
                        if (list == null) author.setSentRequests(list = new ArrayList<>());
                        break;
                    case AuthorLists.RECEIVED_REQUESTS:
                        list = author.getReceivedRequests();
                        if (list == null) author.setReceivedRequests(list = new ArrayList<>());
                        break;
                }

               if (!list.contains(userId)) {

                    // Add the user to the List
                    list.add(userId);
                }

                if (listType == AuthorLists.RECEIVED_REQUESTS && sentRequests.contains(userId) ||
                        listType == AuthorLists.SENT_REQUESTS && receivedRequests.contains(userId)) {

                    // If user has both sent and received a request for this user, accept the request and
                    // become friends
                    author.getFriends().add(userId);
                    author.getSentRequests().remove(userId);
                    author.getReceivedRequests().remove(userId);
                    if (author.getFollowing().contains(userId)) author.getFollowing().remove(userId);
                }

                mutableData.setValue(author.toMap());
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Timber.e("Error adding user to list: " + databaseError.getDetails());
                }
            }
        };

        // Run the update operation as a Transaction
        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(firebaseId)
                .runTransaction(handler);
    }

    /**
     * Removes a user from one of the Author's Lists
     *
     * @param listType    The type of List to remove the user from
     * @param userId      The FirebaseId of the user to be removed
     */
    public void removeUserFromList(@AuthorLists final int listType, final String userId) {

        // Init the Handler for the Firebase Transaction
        Transaction.Handler handler = new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                // Convert the data to an Author
                Author author = mutableData.getValue(Author.class);

                if (author == null) {

                    // Weird glitch, re-run the transaction with the same parameters
                    removeUserFromList(listType, userId);
                    return Transaction.abort();
                }

                // Get a reference to the List that will be modified
                List<String> list = null;

                // Reference the List based on the listType
                switch (listType) {
                    case AuthorLists.FRIENDS:           list = author.getFriends();
                        break;
                    case AuthorLists.FOLLOWING:         list = author.getFollowing();
                        break;
                    case AuthorLists.FOLLOWERS:         list = author.getFollowers();
                        break;
                    case AuthorLists.SENT_REQUESTS:     list = author.getSentRequests();
                        break;
                    case AuthorLists.RECEIVED_REQUESTS: list = author.getReceivedRequests();
                        break;
                }

                if (list == null) return Transaction.abort();

                // Remove the userId from the List
                if (list.contains(userId)) {
                    list.remove(userId);
                }

                mutableData.setValue(author.toMap());
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Timber.e("Error removing user from list: " + databaseError.getDetails());
                }
            }
        };

        // Run the update operation as a Transaction
        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(firebaseId)
                .runTransaction(handler);
    }

    /**
     * Updates the Author with new values from another Author. This can be used to update the
     * cached Author, allowing it to update all Authors that are referencing the cached Object.
     *
     * @param newAuthorValues    Author with the values that are to replace the existing Author
     */
    public void updateAuthorValues(Author newAuthorValues) {

        // Check to ensure that the Author values used to replace the current Author has the same
        // FirebaseId
        if (!newAuthorValues.firebaseId.equals(firebaseId)) return;

        name                = newAuthorValues.name;
        username            = newAuthorValues.username;
        description         = newAuthorValues.description;
        hasImage            = newAuthorValues.hasImage;
        score               = newAuthorValues.score;
        favorites           = newAuthorValues.favorites;
        chats               = newAuthorValues.chats;
        friends             = newAuthorValues.friends;
        following           = newAuthorValues.following;
        followers           = newAuthorValues.followers;
        receivedRequests    = newAuthorValues.receivedRequests;
        sentRequests        = newAuthorValues.sentRequests;
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
