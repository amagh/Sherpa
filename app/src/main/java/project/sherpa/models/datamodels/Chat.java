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
    public static final String IS_GROUP             = "isGroup";

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
    private boolean isGroup;

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
        map.put(IS_GROUP,           isGroup);

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
        int idxMemberId             = cursor.getColumnIndex(GuideContract.ChatEntry.MEMBER_ID);
        int idxMessageCount         = cursor.getColumnIndex(GuideContract.ChatEntry.MESSAGE_COUNT);
        int idxLastAuthorId         = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_AUTHOR_ID);
        int idxLastAuthorName       = cursor.getColumnIndex(GuideContract.AuthorEntry.NAME);
        int idxLastMessageId        = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE_ID);
        int idxLastMessage          = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE);
        int idxLastMessageDate      = cursor.getColumnIndex(GuideContract.ChatEntry.LAST_MESSAGE_DATE);

        // Get the value from the Cursor
        String firebaseId           = cursor.getString(idxFirebaseId);
        int messageCount            = cursor.getInt(idxMessageCount);
        String lastAuthorId         = cursor.getString(idxLastAuthorId);
        String lastAuthorName       = cursor.getString(idxLastAuthorName);
        String lastMessageId        = cursor.getString(idxLastMessageId);
        String lastMessage          = cursor.getString(idxLastMessage);
        long lastMessageDate        = cursor.getLong(idxLastMessageDate);

        // Iterate through each value in the Cursor and add the memberId to the members List
        List<String> activeMembers  = new ArrayList<>();
        do {
            activeMembers.add(cursor.getString(idxMemberId));
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
        chat.activeMembers      = activeMembers;
        chat.memberCode         = buildMemberCode(activeMembers);

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

    public boolean isGroup() {
        return isGroup;
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

    public void setIsGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }
}
