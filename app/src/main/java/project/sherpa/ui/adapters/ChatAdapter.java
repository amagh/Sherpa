package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import project.sherpa.R;
import project.sherpa.databinding.ListItemChatBinding;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.viewmodels.ChatViewModel;
import project.sherpa.ui.adapters.interfaces.ClickHandler;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    // ** Member Variables ** //
    private AppCompatActivity mActivity;
    private ClickHandler<Chat> mClickHandler;
    private SortedListAdapterCallback<Chat> mSortedListCallback = new SortedListAdapterCallback<Chat>(this) {
        @Override
        public int compare(Chat o1, Chat o2) {
            return o1.compareTo(o2);
        }

        @Override
        public boolean areContentsTheSame(Chat oldItem, Chat newItem) {
            return oldItem.firebaseId.equals(newItem.firebaseId);
        }

        @Override
        public boolean areItemsTheSame(Chat item1, Chat item2) {
            return item1 == item2;
        }
    };
    private SortedList<Chat> mSortedList = new SortedList<Chat>(Chat.class, mSortedListCallback);
    private List<String> mNewMessageList = new ArrayList<>();

    public ChatAdapter(AppCompatActivity activity, ClickHandler<Chat> clickHandler) {
        mActivity = activity;
        mClickHandler = clickHandler;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        ListItemChatBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_chat, parent, false);

        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mSortedList.size();
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return mSortedList.get(position).firebaseId.hashCode();
    }

    /**
     * Adds the chat to be displayed by the Adapter
     * @param chat
     */
    public void addChat(Chat chat) {

        boolean newChat = true;
        for (int i = 0; i < mSortedList.size(); i++) {

            Chat oldChat = mSortedList.get(i);

            if (oldChat.firebaseId.equals(chat.firebaseId)) {

                newChat = false;

                oldChat.setActiveMembers(chat.getActiveMembers());
                oldChat.setMemberCode(chat.getMemberCode());
                oldChat.setLastMessageDate(chat.getLastMessageDate());
                oldChat.setLastMessage(chat.getLastMessage());
                oldChat.setMessageCount(chat.getMessageCount());
                oldChat.setLastAuthorId(chat.getLastAuthorId());
                oldChat.setLastAuthorName(chat.getLastAuthorName());

                notifyItemChanged(i);
                mSortedList.recalculatePositionOfItemAt(i);
            }
        }

        if (newChat) {
            mSortedList.add(chat);
        }
    }

    /**
     * Removes a Chat from the Adapter
     *
     * @param chatId    FirebaseId of the Chat to be removed
     */
    public void removeChat(String chatId) {

        // Iterate through the List of Chats and remove the Chat that matches the ChatId
        for (int i = 0; i < mSortedList.size(); i++) {
            Chat chat = mSortedList.get(i);
            if (chat.firebaseId.equals(chatId)) {
                mSortedList.removeItemAt(i);
                break;
            }
        }
    }

    /**
     * Clears the Adapter
     */
    public void clear() {
        mSortedList.clear();
        notifyDataSetChanged();
    }

    /**
     * Sets whether a Chat has an unread, new message
     *
     * @param chatId           The FirebaseId of the Chat to change the newMessage status for
     * @param hasNewMessage    Boolean value whether this Chat should change to indicate an unread
     *                         message
     */
    public void setHasNewMessage(String chatId, boolean hasNewMessage) {

        // Check for whether any values were actually changed
        boolean changed = false;

        // Check whether it's newMessage status should be changed and change it
        if (hasNewMessage && !mNewMessageList.contains(chatId)) {
            mNewMessageList.add(chatId);
            changed = true;

        } else if (!hasNewMessage && mNewMessageList.contains(chatId)) {
            mNewMessageList.remove(chatId);
            changed = true;
        }

        // Notify change on the changed item
        if (changed) {
            for (int i = 0; i < mSortedList.size(); i++) {
                Chat chat = mSortedList.get(i);
                if (chat.firebaseId.equals(chatId)) {
                    notifyItemChanged(i);
                    return;
                }
            }
        }
    }

    class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Member Variables ** //
        private ListItemChatBinding mBinding;

        public ChatViewHolder(ListItemChatBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
        }

        void bind(int position) {

            // Bind the Chat at the ViewHolder's position to the ViewHolder's binding
            Chat chat = mSortedList.get(position);
            ChatViewModel vm = new ChatViewModel(mActivity, chat);

            // Set the Typeface to indicate whether the Chat has an unread message
            vm.setNewMessage(mNewMessageList.contains(chat.firebaseId));

            mBinding.setVm(vm);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            Chat chat = mSortedList.get(position);
            mClickHandler.onClick(chat);
        }
    }
}
