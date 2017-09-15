package project.sherpa.ui.adapters;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import project.sherpa.R;
import project.sherpa.databinding.ListItemMessageReceiveBinding;
import project.sherpa.databinding.ListItemMessageSendBinding;
import project.sherpa.models.datamodels.Message;
import project.sherpa.models.viewmodels.MessageViewModel;

/**
 * Created by Alvin on 9/14/2017.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // ** Constants ** //
    private static final int SEND_MESSAGE_VIEW_TYPE     = 0;
    private static final int RECEIVE_MESSAGE_VIEW_TYPE  = 1;
    private static final int SPACER_VIEW_TYPE           = 2;

    // ** Member Variables ** //
    private Activity mActivity;
    private FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
    private SortedListAdapterCallback<Message> mCallback = new SortedListAdapterCallback<Message>(this) {
        @Override
        public int compare(Message o1, Message o2) {

            // Messages will be sorted by date
            return o1.compare(o2);
        }

        @Override
        public boolean areContentsTheSame(Message oldItem, Message newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Message item1, Message item2) {
            return item1 == item2;
        }
    };
    private SortedList<Message> mSortedList = new SortedList<>(Message.class, mCallback);

    public MessageAdapter(Activity activity) {
        mActivity = activity;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = 0;

        switch (viewType) {
            case SEND_MESSAGE_VIEW_TYPE:    layoutId = R.layout.list_item_message_send;
                break;
            case RECEIVE_MESSAGE_VIEW_TYPE: layoutId = R.layout.list_item_message_receive;
                break;
            case SPACER_VIEW_TYPE:          layoutId = R.layout.list_item_message_bottom_spacer;
                break;
        }

        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);

        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        if (position <= mSortedList.size()) holder.bind(position);
    }

    @Override
    public int getItemCount() {

        // Add item to the count for the spacer
        return mSortedList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {

        Message message = mSortedList.get(position);

        if (position == mSortedList.size()) {
            return SPACER_VIEW_TYPE;
        } else if (mUser != null && message.getAuthorId().equals(mUser.getUid())) {
            return SEND_MESSAGE_VIEW_TYPE;
        } else {
            return RECEIVE_MESSAGE_VIEW_TYPE;
        }
    }

    public void setMessageList(List<Message> messageList) {

        // Remove any items from the SortedList that are not in messageList
        if (mSortedList.size() > 0) {
            for (int i = 0; i < mSortedList.size(); i++) {
                Message message = mSortedList.get(i);

                if (!messageList.contains(message)) {
                    mSortedList.removeItemAt(i);
                }
            }
        }

        // Add all items from messageList to the SortedList
        mSortedList.addAll(messageList);
    }

    public void addMessage(Message message) {

        // Add the item if the SortedList does not already contain it
        if (mSortedList.indexOf(message) != SortedList.INVALID_POSITION) {
            mSortedList.add(message);
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        // ** Member Variables ** //
        ViewDataBinding mBinding;

        public MessageViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
        }

        void bind(int position) {

            // Get the message for the position and the message prior to it
            Message message = mSortedList.get(position);
            Message prevMessage = position > 0
                    ? mSortedList.get(position - 1)
                    : null;

            // Check if the messages have the same Author
            boolean sameAuthor = prevMessage != null
                    && prevMessage.getAuthorId().equals(message.getAuthorId());

            MessageViewModel vm = new MessageViewModel((AppCompatActivity) mActivity, message, sameAuthor);

            // Bind the data
            if (mBinding instanceof ListItemMessageSendBinding) {
                ((ListItemMessageSendBinding) mBinding).setVm(vm);
            } else if (mBinding instanceof ListItemMessageReceiveBinding) {
                ((ListItemMessageReceiveBinding) mBinding).setVm(vm);
            }
        }
    }
}
