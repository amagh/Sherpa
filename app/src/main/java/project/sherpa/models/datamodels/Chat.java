package project.sherpa.models.datamodels;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 9/13/2017.
 */

public class Chat extends BaseModel {

    // ** Constants ** //
    public static final String MEMBERS          = "members";
    public static final String MESSAGE_COUNT    = "messageCount";
    public static final String LAST_AUTHOR_ID   = "lastAuthorId";
    public static final String LAST_MESSAGE_ID  = "lastMessageId";
    public static final String LAST_MESSAGE     = "lastMessage";
    public static final String MEMBER_ID        = "memberId";

    // ** Member Variables ** //
    private List<String> members;
    private int messageCount;
    private String lastAuthorId;
    private String lastMessageId;
    private String lastMessage;

    @Override
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put(MEMBERS, members);
        map.put(MESSAGE_COUNT, messageCount);
        map.put(LAST_AUTHOR_ID, lastAuthorId);
        map.put(LAST_MESSAGE_ID, lastMessageId);
        map.put(LAST_MESSAGE, lastMessage);

        return map;
    }

    /**
     * Creates a Chat data model from a Cursor describing a Chat
     *
     * @param cursor    Cursor describing a Chat
     * @return Chat with the details from the Cursor
     */
    public Chat createChatFromCursor(Cursor cursor) {

        // Get column indices
        int idxFirebaseId       = cursor.getColumnIndex(GuideContract.ChatEntry.FIREBASE_ID);
        int idxMemberId         = cursor.getColumnIndex(GuideContract.ChatEntry.MEMBER_ID);
        int idxMessageCount     = cursor.getColumnIndex(GuideContract.ChatEntry.MESSAGE_COUNT);
        int idxLastAuthorId     = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_AUTHOR_ID);
        int idxLastMessageId    = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE_ID);
        int idxLastMessage      = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE);

        // Get the value from the Cursor
        String firebaseId       = cursor.getString(idxFirebaseId);
        int messageCount        = cursor.getInt(idxMessageCount);
        String lastAuthorId     = cursor.getString(idxLastAuthorId);
        String lastMessageId    = cursor.getString(idxLastMessageId);
        String lastMessage      = cursor.getString(idxLastMessage);

        // Iterate through each value in the Cursor and add the memberId to the members List
        List<String> members    = new ArrayList<>();
        do {
            members.add(cursor.getString(idxMemberId));
        } while (cursor.moveToNext());

        // Create and populate a new Chat
        Chat chat = new Chat();

        chat.firebaseId     = firebaseId;
        chat.messageCount   = messageCount;
        chat.lastAuthorId   = lastAuthorId;
        chat.lastMessageId  = lastMessageId;
        chat.lastMessage    = lastMessage;
        chat.members        = members;

        return chat;
    }

    //********************************************************************************************//
    //*********************************** Getters & Setters **************************************//
    //********************************************************************************************//


    public List<String> getMembers() {
        return members;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String getLastAuthorId() {
        return lastAuthorId;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public void setLastAuthorId(String lastAuthorId) {
        this.lastAuthorId = lastAuthorId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
