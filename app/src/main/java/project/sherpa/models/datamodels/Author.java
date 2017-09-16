package project.sherpa.models.datamodels;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

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

    // ** Member Variables ** //
    public String name;
    private String username;
    public String description;
    public int score;
    public Map<String, String> favorites;
    private List<String> chats;

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

        return map;
    }

    /**
     * Adds a Chat to the User's Firebase Profile
     * @param chatId
     */
    public void addChat(String chatId) {
        if (chats == null) {
            chats = new ArrayList<>();
        }

        chats.add(chatId);

        FirebaseProviderUtils.insertOrUpdateModel(this, null);
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setChats(List<String> chats) {
        this.chats = chats;
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
