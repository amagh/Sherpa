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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 9/13/2017.
 */

public class Chat extends BaseModel {

    // ** Constants ** //
    public static final String ACTIVE_MEMBERS       = "activeMembers";
    public static final String ALL_MEMBERS          = "allMembers";
    public static final String MESSAGE_COUNT        = "messageCount";
    public static final String LAST_AUTHOR_ID       = "lastAuthorId";
    public static final String LAST_AUTHOR_NAME     = "lastAuthorName";
    public static final String LAST_MESSAGE_ID      = "lastMessageId";
    public static final String LAST_MESSAGE         = "lastMessage";
    public static final String LAST_MESSAGE_DATE    = "lastMessageDate";
    public static final String MEMBER_ID            = "memberId";
    public static final String MEMBER_CODE          = "memberCode";
    public static final String GROUP                = "group";

    // ** Member Variables ** //
    private List<String> activeMembers;
    private List<String> allMembers;
    private int messageCount;
    private String lastAuthorId;
    private String lastAuthorName;
    private String lastMessageId;
    private String lastMessage;
    private long lastMessageDate;
    private String memberCode;
    private boolean group;

    @Override
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put(ACTIVE_MEMBERS,     activeMembers);
        map.put(ALL_MEMBERS,        allMembers);
        map.put(MESSAGE_COUNT,      messageCount);
        map.put(LAST_AUTHOR_ID,     lastAuthorId);
        map.put(LAST_AUTHOR_NAME,   lastAuthorName);
        map.put(LAST_MESSAGE_ID,    lastMessageId);
        map.put(LAST_MESSAGE,       lastMessage);
        map.put(LAST_MESSAGE_DATE,  ServerValue.TIMESTAMP);
        map.put(MEMBER_CODE,        buildMemberCode());
        map.put(GROUP,              group);

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
        int idxFirebaseId           = 1;
        int idxMessageCount         = cursor.getColumnIndex(GuideContract.ChatEntry.MESSAGE_COUNT);

        // Get the value from the Cursor
        String firebaseId           = cursor.getString(idxFirebaseId);
        int messageCount            = cursor.getInt(idxMessageCount);

        // Create and populate a new Chat
        Chat chat = new Chat();

        chat.firebaseId         = firebaseId;
        chat.messageCount       = messageCount;

        return chat;
    }

    public int compareTo(Chat otherChat) {

        return lastMessageDate < otherChat.lastMessageDate
                ? 1
                : lastMessageDate > otherChat.lastMessageDate
                ? -1
                : 0;
    }

    /**
     * Adds a member to the Chat and updates the Chat in the local database and Firebase Database
     *
     * @param context     Interface to global Context
     * @param authorId    FirebaseId of the author to add to the chat
     */
    public void addMember(Context context, String authorId) {

        // Add the user to the Active and All members Lists
        if (!activeMembers.contains(authorId)) {
            activeMembers.add(authorId);
        }

        if (!allMembers.contains(authorId)) {
            allMembers.add(authorId);
        }


        addMemberToFirebase(authorId);
        ContentProviderUtils.insertModel(context, this);
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
                        if (!chat.getActiveMembers().contains(authorId)) {
                            chat.getActiveMembers().add(authorId);
                        }

                        if (!chat.getAllMembers().contains(authorId)) {
                            chat.getAllMembers().remove(authorId);
                        }

                        mutableData.setValue(chat.toMap());
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        if (databaseError != null) {
                            Timber.e("Error adding member to chat on Firebase: " + databaseError.getDetails());
                        }
                    }
                });
    }

    /**
     * Removes a member from the Chat
     *
     * @param authorId    FirebaseId of the user to be removed
     */
    public void removeMember(String authorId) {
        activeMembers.remove(authorId);
        removeMemberFromFirebase(authorId);
    }

    /**
     * Removes a user from the list of active members of the Chat on Firebase
     *
     * @param authorId    FirebaseId of the Author to be removed
     */
    private void removeMemberFromFirebase(final String authorId) {
        FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.CHATS)
                .child(firebaseId)
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Chat chat = mutableData.getValue(Chat.class);

                        // Check to ensure that the FirebaseDatabase returns a valid item
                        if (chat == null) {
                            removeMemberFromFirebase(authorId);
                            return Transaction.abort();
                        }

                        // Modify the Chat
                        chat.getActiveMembers().remove(authorId);

                        mutableData.setValue(chat.toMap());
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });
    }

    /**
     * Updates the Chat's message count and last message info on Firebase
     *
     * @param message    Message details to be set as the last message details for the Chat
     */
    public void updateChatWithNewMessage(final Message message) {

        // Check if this is a new Chat
        if (messageCount == 0) {

            // Update Chat with the values of the new message
            lastMessage = message.getMessage();
            lastMessageId = message.firebaseId;
            lastAuthorId = message.getAuthorId();
            lastAuthorName = message.getAuthorName();
            messageCount++;

            FirebaseProviderUtils.insertOrUpdateModel(this);

            return;
        }

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
     * Builds the memberCode for the Chat. {@link #buildMemberCode(List)}
     *
     * @return memberCode
     */
    private String buildMemberCode() {
        return buildMemberCode(activeMembers);
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

    /**
     * Generates a FirebaseId for a new Chat. This allows a new Chat to be passed to another
     * Activity without uploading it to Firebase Database until a message is sent.
     */
    public void generateFirebaseId() {
        firebaseId = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.CHATS)
                .push()
                .getKey();
    }

    /**
     * Updates the values of the Chat with new values from an update Chat with the same FirebaseId
     *
     * @param newModelValues    Chat with new values to replace the current values
     */
    @Override
    public void updateValues(BaseModel newModelValues) {

        // Cast the BaseModel to a Chat
        if (!(newModelValues instanceof Chat)) return;
        Chat newChatValues = (Chat) newModelValues;

        // Check to ensure the new Chat has the same FirebaseId as the one it is replacing
        if (!newChatValues.firebaseId.equals(firebaseId)) return;

        activeMembers   = newChatValues.activeMembers;
        allMembers      = newChatValues.allMembers;
        messageCount    = newChatValues.messageCount;
        lastAuthorId    = newChatValues.lastAuthorId;
        lastAuthorName  = newChatValues.lastAuthorName;
        lastMessageId   = newChatValues.lastMessageId;
        lastMessage     = newChatValues.lastMessage;
        lastMessageDate = newChatValues.lastMessageDate;
        memberCode      = newChatValues.memberCode;
    }

    /**
     * Checks to see if there are any new messages compared to the Chat's copy on the local database
     *
     * @param context    Interface to global Context
     * @return True if there are unread messages. False otherwise.
     */
    public boolean hasNewMessages(Context context) {

        // Query database for number of messages on local copy of Chat
        int localMessageCount = ContentProviderUtils.getMessageCount(context, firebaseId);
        return messageCount > localMessageCount;
    }

    //********************************************************************************************//
    //*********************************** Getters & Setters **************************************//
    //********************************************************************************************//


    public List<String> getActiveMembers() {
        return activeMembers;
    }

    public List<String> getAllMembers() {
        return allMembers;
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

    public long getLastMessageDate() {
        return lastMessageDate;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public boolean getGroup() {
        return group;
    }

    public void setActiveMembers(List<String> activeMembers) {
        this.activeMembers = activeMembers;
    }

    public void setAllMembers(List<String> allMembers) {
        this.allMembers = allMembers;
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

    public void setGroup(boolean group) {
        this.group = group;
    }
}
