package project.sherpa.models.datamodels;

import android.database.Cursor;

import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 9/13/2017.
 */

public class Message extends BaseModel {

    // ** Constants ** //
    public static final String AUTHOR_ID    = "authorId";
    public static final String AUTHOR_NAME  = "authorName";
    public static final String MESSAGE      = "message";
    public static final String CHAT_ID      = "chatId";
    public static final String DATE         = "date";
    public static final String STATUS       = "status";

    public static final String[] PROJECTION = {
            GuideDatabase.MESSAGES  + "." + FIREBASE_ID,
            AUTHOR_ID,
            GuideDatabase.AUTHORS   + "." + GuideContract.AuthorEntry.NAME,
            MESSAGE,
            CHAT_ID,
            DATE,
            STATUS
    };

    public interface ProjectionIndex {
        int FIREBASE_ID = 0;
        int AUTHOR_ID   = 1;
        int AUTHOR_NAME = 2;
        int MESSAGE     = 3;
        int CHAT_ID     = 4;
        int DATE        = 5;
        int STATUS      = 6;
    }

    // ** Member Variables ** //
    private String authorId;
    private String authorName;
    private String message;
    private String chatId;
    private long date;
    private int status;

    @Override
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put(AUTHOR_ID, authorId);
        map.put(AUTHOR_NAME, authorName);
        map.put(MESSAGE, message);
        map.put(CHAT_ID, chatId);
        map.put(DATE, date != 0 ? date : ServerValue.TIMESTAMP);

        return map;
    }

    /**
     * Generates a Message data object from a Cursor describing a Message
     *
     * @param cursor    Cursor describing a Message
     * @return Message data object with contents from the Cursor
     */
    public static Message createMessageFromCursor(Cursor cursor) {

        // Get the data from the Cursor
        String firebaseId   = cursor.getString(ProjectionIndex.FIREBASE_ID);
        String authorId     = cursor.getString(ProjectionIndex.AUTHOR_ID);
        String authorName   = cursor.getString(ProjectionIndex.AUTHOR_NAME);
        String message      = cursor.getString(ProjectionIndex.MESSAGE);
        String chatId       = cursor.getString(ProjectionIndex.CHAT_ID);
        long date           = cursor.getLong(ProjectionIndex.DATE);
        int status          = cursor.getInt(ProjectionIndex.STATUS);

        // Create a new Message with the information
        Message messageObj = new Message();

        messageObj.firebaseId   = firebaseId;
        messageObj.authorId     = authorId;
        messageObj.authorName   = authorName;
        messageObj.message      = message;
        messageObj.chatId       = chatId;
        messageObj.date         = date;
        messageObj.status       = status;

        return messageObj;
    }

    public int compare(Message otherMessage) {
        return this.date < otherMessage.date
                ? -1
                : this.date > otherMessage.date
                ? 1
                : 0;
    }

    //********************************************************************************************//
    //*********************************** Getters & Setters **************************************//
    //********************************************************************************************//


    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getMessage() {
        return message;
    }

    public String getChatId() {
        return chatId;
    }

    public long getDate() {
        return date;
    }

    public int getStatus() {
        return status;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
