package project.sherpa.models.datamodels;

import android.content.Context;
import android.database.Cursor;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.data.GuideProvider;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 9/13/2017.
 */

public class Chat extends BaseModel {

    // ** Constants ** //
    public static final String MEMBERS              = "members";
    public static final String MESSAGE_COUNT        = "messageCount";
    public static final String LAST_AUTHOR_ID       = "lastAuthorId";
    public static final String LAST_AUTHOR_NAME     = "lastAuthorName";
    public static final String LAST_MESSAGE_ID      = "lastMessageId";
    public static final String LAST_MESSAGE         = "lastMessage";
    public static final String LAST_MESSAGE_DATE    = "lastMessageDate";
    public static final String MEMBER_ID            = "memberId";
    public static final String MEMBER_CODE          = "memberCode";


    // ** Member Variables ** //
    private List<String> members;
    private int messageCount;
    private String lastAuthorId;
    private String lastAuthorName;
    private String lastMessageId;
    private String lastMessage;
    private long lastMessageDate;
    private String memberCode;

    private boolean updateTime;

    @Override
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put(MEMBERS,            members);
        map.put(MESSAGE_COUNT,      messageCount);
        map.put(LAST_AUTHOR_ID,     lastAuthorId);
        map.put(LAST_AUTHOR_NAME,   lastAuthorName);
        map.put(LAST_MESSAGE_ID,    lastMessageId);
        map.put(LAST_MESSAGE,       lastMessage);
        map.put(LAST_MESSAGE_DATE,  lastMessageDate);
        map.put(MEMBER_CODE,        buildMemberCode());

        return map;
    }

    /**
     * Creates a Chat data model from a Cursor describing a Chat
     *
     * @param cursor    Cursor describing a Chat
     * @return Chat with the details from the Cursor
     */
    public static Chat createChatFromCursor(Cursor cursor) {

        // Get column indices
        int idxFirebaseId       = 1;
        int idxMemberId         = cursor.getColumnIndex(GuideContract.ChatEntry.MEMBER_ID);
        int idxMessageCount     = cursor.getColumnIndex(GuideContract.ChatEntry.MESSAGE_COUNT);
        int idxLastAuthorId     = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_AUTHOR_ID);
        int idxLastAuthorName   = cursor.getColumnIndex(GuideContract.AuthorEntry.NAME);
        int idxLastMessageId    = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE_ID);
        int idxLastMessage      = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE);
        int idxLastMessageDate  = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE_DATE);

        // Get the value from the Cursor
        String firebaseId       = cursor.getString(idxFirebaseId);
        int messageCount        = cursor.getInt(idxMessageCount);
        String lastAuthorId     = cursor.getString(idxLastAuthorId);
        String lastAuthorName   = cursor.getString(idxLastAuthorName);
        String lastMessageId    = cursor.getString(idxLastMessageId);
        String lastMessage      = cursor.getString(idxLastMessage);
        long lastMessageDate    = cursor.getLong(idxLastMessageDate);

        // Iterate through each value in the Cursor and add the memberId to the members List
        List<String> members    = new ArrayList<>();
        do {
            members.add(cursor.getString(idxMemberId));
        } while (cursor.moveToNext());

        // Create and populate a new Chat
        Chat chat = new Chat();

        chat.firebaseId         = firebaseId;
        chat.messageCount       = messageCount;
        chat.lastAuthorId       = lastAuthorId;
        chat.lastAuthorName     = lastAuthorName;
        chat.lastMessageId      = lastMessageId;
        chat.lastMessage        = lastMessage;
        chat.lastMessageDate    = lastMessageDate;
        chat.members            = members;
        chat.memberCode         = buildMemberCode(members);

        return chat;
    }

    public int compare(Chat otherChat) {

        return lastMessageDate < otherChat.lastMessageDate
                ? -1
                : lastMessageDate > otherChat.lastMessageDate
                ? 1
                : 0;
    }

    /**
     * Sets whether the Chat should use the server time when the {@link #getLastMessageDate()} is
     * called
     */
    private void updateTimeWithServerValue(boolean update) {
        updateTime = update;
    }

    /**
     * Adds a member to the Chat and updates the Chat in the local database and Firebase Database
     *
     * @param context     Interface to global Context
     * @param authorId    FirebaseId of the author to add to the chat
     */
    public void addMember(Context context, String authorId) {
        boolean newChat = false;

        if (members == null) {
            newChat = true;
            members = new ArrayList<>();
        }

        members.add(authorId);

        if (newChat) {
            FirebaseProviderUtils.insertOrUpdateModel(this);
        } else {
            addMemberToFirebase(authorId);
        }

        ContentProviderUtils.insertChat(context, this);
    }

    /**
     * Updates the Firebase entry for the Chat by adding a member.
     *
     * @param authorId    FirebaseId of the Author to be added to the Chat
     */
    private void addMemberToFirebase(final String authorId) {
        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.CHATS)
                .child(firebaseId)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Chat chat = mutableData.getValue(Chat.class);

                        // Check to ensure that the FirebaseDatabase returns a valid item
                        if (chat == null) {
                            addMemberToFirebase(authorId);
                            return Transaction.abort();
                        }

                        // Modify the Chat
                        chat.getMembers().add(authorId);
                        chat.setMemberCode(buildMemberCode(chat.getMembers()));
                        mutableData.setValue(chat);

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });
    }

    private String buildMemberCode() {
        return buildMemberCode(members);
    }

    /**
     * Updates the Chat's message count and last message info on Firebase
     *
     * @param message    Message details to be set as the last message details for the Chat
     */
    public void updateChatWithNewMessage(final Message message) {
        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.CHATS)
                .child(firebaseId)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {

                        Chat chat = mutableData.getValue(Chat.class);
                        if (chat == null) {

                            // Re-run the Transaction
                            updateChatWithNewMessage(message);

                            // Abort the current Transaction
                            return Transaction.abort();
                        } else {

                            // Update the Chat values
                            chat.setLastMessage(message.getMessage());
                            chat.setLastMessageId(message.firebaseId);
                            chat.setLastAuthorId(message.getAuthorId());
                            chat.setLastAuthorName(message.getAuthorName());
                            chat.updateTimeWithServerValue(true);
                            chat.setMessageCount(chat.getMessageCount() + 1);

                            mutableData.setValue(chat.toMap());
                        }

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        if (databaseError != null) {
                            Timber.e("Error updating Chat message count: " + databaseError.getMessage());
                        }
                    }
                });
    }

    /**
     * Generates the memberCode from the List of members of a Chat. The code is simply a String of
     * all the members in alphabetical order. This can be used to easily check if a group with the
     * same members already exists.
     *
     * @param memberList    List of members to generate the member code for
     * @return Member code consisting of a String of all the members in alphabetical order
     */
    public static String buildMemberCode(List<String> memberList) {

        // Sort the List into alphabetical order
        Collections.sort(memberList, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareTo(t1);
            }
        });

        // Build the memberCode
        StringBuilder builder = new StringBuilder();
        for (String authorId : memberList) {
            builder.append(authorId);
        }

        return builder.toString();
    }

    /**
     * Checks the FirebaseDatabase to see if there are any chats with the same members as the List
     * of members in the signature
     *
     * @param chatMemberList    List of members to check against the FirebaseDatabase
     * @param listener          Returns the Chat that has the sasme members from Firebase
     */
    public static void checkDuplicateChats(
            List<String> chatMemberList,
            final FirebaseProviderUtils.FirebaseListener listener) {

        // Generate the memberCode that will be used to check against the FirebaseDatabse to check
        // if there is another Chat with the same members
        String memberCode = buildMemberCode(chatMemberList);

        // Query Firebase to see if there are any duplicate Chats
        final Query chatQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.CHATS)
                .orderByChild(Chat.MEMBER_CODE)
                .equalTo(memberCode);

        chatQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Chat[] chats = (Chat[]) FirebaseProviderUtils.getModelsFromSnapshot(
                            FirebaseProviderUtils.FirebaseType.CHAT,
                            dataSnapshot);

                    listener.onModelReady(chats[0]);
                } else {
                    listener.onModelReady(null);
                }

                chatQuery.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                chatQuery.removeEventListener(this);
            }
        });
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

    public String getLastAuthorName() {
        return lastAuthorName;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Object getLastMessageDate() {
        return updateTime
                ? ServerValue.TIMESTAMP
                : lastMessageDate;
    }

    public String getMemberCode() {
        return memberCode;
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

    public void setLastAuthorName(String lastAuthorName) {
        this.lastAuthorName = lastAuthorName;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageDate(long lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }
}
