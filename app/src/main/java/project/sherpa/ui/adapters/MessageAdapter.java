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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.R;
import project.sherpa.databinding.ListItemMessageReceiveBinding;
import project.sherpa.databinding.ListItemMessageReceiveGuideBinding;
import project.sherpa.databinding.ListItemMessageSendBinding;
import project.sherpa.databinding.ListItemMessageSendGuideBinding;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Message;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.models.viewmodels.MessageViewModel;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.sherpa.models.datamodels.Message.AttachmentType.GUIDE_TYPE;
import static project.sherpa.models.datamodels.Message.AttachmentType.NONE;

/**
 * Created by Alvin on 9/14/2017.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // ** Constants ** //
    private static final int SEND_MESSAGE_VIEW_TYPE             = 0;
    private static final int RECEIVE_MESSAGE_VIEW_TYPE          = 1;
    private static final int SEND_MESSAGE_GUIDE_VIEW_TYPE       = 2;
    private static final int RECEIVE_MESSAGE_GUIDE_VIEW_TYPE    = 3;
    private static final int SPACER_VIEW_TYPE                   = 4;

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
    private Map<String, Guide> mGuideAttachmentMap = new HashMap<>();

    public MessageAdapter(Activity activity) {
        mActivity = activity;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = 0;

        switch (viewType) {
            case SEND_MESSAGE_VIEW_TYPE:            layoutId = R.layout.list_item_message_send;
                break;
            case RECEIVE_MESSAGE_VIEW_TYPE:         layoutId = R.layout.list_item_message_receive;
                break;
            case SEND_MESSAGE_GUIDE_VIEW_TYPE:      layoutId = R.layout.list_item_message_send_guide;
                break;
            case RECEIVE_MESSAGE_GUIDE_VIEW_TYPE:   layoutId = R.layout.list_item_message_receive_guide;
                break;
            case SPACER_VIEW_TYPE:                  layoutId = R.layout.list_item_message_bottom_spacer;
                break;
        }

        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);

        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        if (holder.getItemViewType() != SPACER_VIEW_TYPE) holder.bind(position);
    }

    @Override
    public int getItemCount() {

        // Add item to the count for the spacer
        return mSortedList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {

        if (position == mSortedList.size()) return SPACER_VIEW_TYPE;

        // Get the ViewType based on the Message's attachments and author
        Message message = mSortedList.get(position);
        switch (message.getAttachmentType()) {
            case NONE:
                if (mUser != null && message.getAuthorId().equals(mUser.getUid())) {
                    return SEND_MESSAGE_VIEW_TYPE;
                } else {
                    return RECEIVE_MESSAGE_VIEW_TYPE;
                }

            case GUIDE_TYPE:
                if (mUser != null && message.getAuthorId().equals(mUser.getUid())) {
                    return SEND_MESSAGE_GUIDE_VIEW_TYPE;
                } else {
                    return RECEIVE_MESSAGE_GUIDE_VIEW_TYPE;
                }
        }

        return super.getItemViewType(position);
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

    public void clear() {
        mSortedList.clear();
    }

    /**
     * Adds a Message to be displayed by the Adapter if it does not already exist in the Adapter
     *
     * @param message    Message to be added
     */
    public void addMessage(Message message) {

        // Add the item if the SortedList does not already contain it
        boolean newItem = true;

        // Iterate through the SortedList and check to ensure that an item with the same FirebaseId
        // doesn't already exist in the List
        for (int i = 0; i < mSortedList.size(); i++) {
            Message oldMessage = mSortedList.get(i);
            if (oldMessage.firebaseId.equals(message.firebaseId)) {
                newItem = false;
                oldMessage.setDate(message.getDate());
                notifyItemChanged(i);
                break;
            }
        }

        if (newItem) {
            mSortedList.add(message);
            notifyItemChanged(mSortedList.size() - 2);
        }
    }

    /**
     * Loads a Guide to be displayed as an attachment to a message
     *
     * @param position    Position of the message to get the Guide for
     */
    private boolean loadGuide(int position) {
        final Message message = mSortedList.get(position);

        if (message.getAttachmentType() ==  GUIDE_TYPE) {
            String guideId = message.getAttachment();

            // See if the Guide has been cached
            Guide cachedGuide = (Guide) DataCache.getInstance().get(guideId);
            if (cachedGuide != null) {

                // Put the cached Guide into the Map and notify
                mGuideAttachmentMap.put(cachedGuide.firebaseId, cachedGuide);
                return true;
            }

            // Load Guide from Firebase if not cached
            FirebaseProviderUtils.getModel(
                    FirebaseProviderUtils.FirebaseType.GUIDE,
                    guideId,
                    new FirebaseProviderUtils.FirebaseListener() {
                        @Override
                        public void onModelReady(BaseModel model) {

                            // Put the Guide in the Map and notify
                            Guide guide = (Guide) model;
                            mGuideAttachmentMap.put(guide.firebaseId, guide);
                            notifyItemChanged(mSortedList.indexOf(message));
                        }
                    }
            );
        }

        return false;
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

            Message nextMessage = position < mSortedList.size() - 1
                    ? mSortedList.get(position + 1)
                    : null;

            MessageViewModel vm = new MessageViewModel((AppCompatActivity) mActivity, message, prevMessage);
            vm.setNextMessage(nextMessage);

            // Bind the data
            if (mBinding instanceof ListItemMessageSendBinding) {
                ((ListItemMessageSendBinding) mBinding).setVm(vm);
            } else if (mBinding instanceof ListItemMessageReceiveBinding) {
                ((ListItemMessageReceiveBinding) mBinding).setVm(vm);
            } else if (mBinding instanceof ListItemMessageSendGuideBinding) {

                // Bind the Guide data
                bindGuideViewModel(message);

                ((ListItemMessageSendGuideBinding) mBinding).setVm(vm);

            } else if (mBinding instanceof ListItemMessageReceiveGuideBinding) {

                // Bind the Guide data
                bindGuideViewModel(message);

                ((ListItemMessageReceiveGuideBinding) mBinding).setVm(vm);
            }
        }

        /**
         * Binds the attached GuideViewModel for a Message to the ViewDataBinding
         *
         * @param message    Message to load the Guide for
         */
        private void bindGuideViewModel(Message message) {

            // Load the Guide that is supposed to be attached
            Guide guide = mGuideAttachmentMap.get(message.getAttachment());

            if (guide == null) {

                // Guide not loaded yet. Load it
                if (loadGuide(mSortedList.indexOf(message))) {
                    bindGuideViewModel(message);
                }
            } else {

                // Bind the GuideViewModel to the ViewDataBinding
                GuideViewModel gvm = new GuideViewModel(mActivity, guide);

                if (mBinding instanceof ListItemMessageSendGuideBinding) {
                    ((ListItemMessageSendGuideBinding) mBinding).messageGuide.setVm(gvm);
                } else {
                    ((ListItemMessageReceiveGuideBinding) mBinding).messageGuide.setVm(gvm);
                }
            }
        }
    }
}
