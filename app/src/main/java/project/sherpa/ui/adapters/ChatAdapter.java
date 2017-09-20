package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
            return o1.compare(o2);
        }

        @Override
        public boolean areContentsTheSame(Chat oldItem, Chat newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Chat item1, Chat item2) {
            return item1 == item2;
        }
    };
    private SortedList<Chat> mSortedList = new SortedList<Chat>(Chat.class, mSortedListCallback);

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

                oldChat.setMembers(chat.getMembers());
                oldChat.setMemberCode(chat.getMemberCode());
                oldChat.setLastMessageDate((long) chat.getLastMessageDate());
                oldChat.setLastMessage(chat.getLastMessage());
                oldChat.setMessageCount(chat.getMessageCount());
                oldChat.setLastAuthorId(chat.getLastAuthorId());
                oldChat.setLastAuthorName(chat.getLastAuthorName());

                notifyDataSetChanged();
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
