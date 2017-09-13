package project.sherpa.models.datamodels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 9/13/2017.
 */

public class Chat extends BaseModel {

    // ** Constants ** //
    public static final String MEMBERS          = "members";
    public static final String MESSAGE_COUNT    = "messageCount";
    public static final String LAST_MESSAGE_ID  = "lastMessageId";
    public static final String LAST_MESSAGE     = "lastMessage";
    public static final String MEMBER_ID        = "memberId";

    // ** Member Variables ** //
    private List<String> members;
    private int messageCount;
    private String lastMessageId;
    private String lastMessage;

    @Override
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put(MEMBERS, members);
        map.put(MESSAGE_COUNT, messageCount);
        map.put(LAST_MESSAGE_ID, lastMessageId);
        map.put(LAST_MESSAGE, lastMessage);

        return map;
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

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
