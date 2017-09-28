package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import project.sherpa.R;
import project.sherpa.databinding.ListItemFriendBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.viewmodels.AuthorViewModel;
import project.sherpa.ui.adapters.interfaces.ClickHandler;

/**
 * Created by Alvin on 9/26/2017.
 */

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    // ** Constants ** //
    private static final int FRIEND_VIEW_TYPE           = 0;
    private static final int RECEIVED_HEADER_VIEW_TYPE  = 1;
    private static final int SENT_HEADER_VIEW_TYPE      = 2;

    // ** Member Variables ** //
    private SortedList.Callback<Author> mSortedCallback = new SortedList.Callback<Author>() {
        @Override
        public int compare(Author o1, Author o2) {
            return o1.name.compareTo(o2.name);
        }

        @Override
        public void onChanged(int position, int count) {

            // Modify the position based on which list it is in
            if (position < mReceivedRequestList.size()) {
                notifyItemRangeChanged(position + 1, count);
            } else {
                notifyItemRangeChanged(position + 2 + mReceivedRequestList.size(), count);
            }
        }

        @Override
        public boolean areContentsTheSame(Author oldItem, Author newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Author item1, Author item2) {
            return item1.firebaseId.equals(item2.firebaseId);
        }

        @Override
        public void onInserted(int position, int count) {

            // Modify the position based on which list it is in
            if (position < mReceivedRequestList.size()) {
                notifyItemRangeInserted(position + 1, count);
            } else {
                notifyItemRangeInserted(position + 2 + mReceivedRequestList.size(), count);
            }
        }

        @Override
        public void onRemoved(int position, int count) {

            // Modify the position based on which list it is in
            if (position < mReceivedRequestList.size()) {
                notifyItemRangeRemoved(position + 1, count);
            } else {
                notifyItemRangeRemoved(position + 2 + mReceivedRequestList.size(), count);
            }
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {

            // Modify the position based on which list it is in
            if (fromPosition < mReceivedRequestList.size()) {
                fromPosition++;
            } else {
                fromPosition += 2 + mReceivedRequestList.size();
            }

            if (toPosition < mReceivedRequestList.size()) {
                toPosition++;
            } else {
                toPosition += 2 + mReceivedRequestList.size();
            }

            notifyItemMoved(fromPosition + 1, toPosition + 1);
        }
    };
    private SortedList<Author> mSentRequestList = new SortedList<>(Author.class, mSortedCallback);
    private SortedList<Author> mReceivedRequestList = new SortedList<>(Author.class, mSortedCallback);
    private ClickHandler<Author> mClickHandler;

    public RequestAdapter(ClickHandler<Author> clickHandler) {
        mClickHandler = clickHandler;
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        int layoutId = -1;

        // Switch layouts based on ViewType
        switch (viewType) {
            case FRIEND_VIEW_TYPE:          layoutId = R.layout.list_item_friend;
                break;
            case RECEIVED_HEADER_VIEW_TYPE: layoutId = R.layout.header_received_requests;
                break;
            case SENT_HEADER_VIEW_TYPE:     layoutId = R.layout.header_sent_requests;
                break;
        }

        ViewDataBinding binding =
                DataBindingUtil.inflate(inflater, layoutId, parent, false);

        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(RequestViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {

        // Return the total size of the sent and received request list sizes
        int totalSize = mSentRequestList.size() + mReceivedRequestList.size();

        // Add one for each header that is required
        if (mSentRequestList.size() > 0) {
            totalSize++;
        }

        if (mReceivedRequestList.size() > 0) {
            totalSize++;
        }

        return totalSize;
    }

    @Override
    public int getItemViewType(int position) {

        // Header ViewTypes
        if (mReceivedRequestList.size() > 0 && position == 0) {
            return RECEIVED_HEADER_VIEW_TYPE;
        } else if ((mReceivedRequestList.size() > 0 && position == mReceivedRequestList.size() + 1) ||
                mReceivedRequestList.size() == 0 && position == 0) {
            return SENT_HEADER_VIEW_TYPE;
        }

        return FRIEND_VIEW_TYPE;
    }

    /**
     * Adds a received request to be displayed by the Adapter
     *
     * @param request    Request to be added to the Adapter
     */
    public void addReceivedRequest(Author request) {
        mReceivedRequestList.add(request);
    }

    /**
     * Adds a sent request to be displayed by the Adapter
     *
     * @param request    Request to be added to the Adapter
     */
    public void addSentRequest(Author request) {
        mSentRequestList.add(request);
    }

    class RequestViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Member Variables ** //
        ListItemFriendBinding mBinding;

        RequestViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            if (binding instanceof ListItemFriendBinding) {
                mBinding = (ListItemFriendBinding) binding;
            }
        }

        void bind(int position) {

            if (getItemViewType() == SENT_HEADER_VIEW_TYPE || getItemViewType() == RECEIVED_HEADER_VIEW_TYPE) {
                return;
            }

            Author request;

            // Get the Author at the modified position
            if (mReceivedRequestList.size() > 0 && position < mReceivedRequestList.size() + 1) {

                // Take into account the header for received requests
                request = mReceivedRequestList.get(position - 1);
            } else if (mReceivedRequestList.size() > 0){
                // Take into account the headers for received and sent requests
                request = mSentRequestList.get(position - 2 - mReceivedRequestList.size());
            } else {
                request = mSentRequestList.get(position - 1);
            }

            AuthorViewModel vm = new AuthorViewModel((AppCompatActivity) mBinding.getRoot().getContext(), request);
            mBinding.setVm(vm);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            Author request;

            // Get the Author at the modified position
            if (position < mReceivedRequestList.size() + 1) {

                // Take into account the header for received requests
                request = mReceivedRequestList.get(position - 1);
            } else {
                // Take into account the headers for received and sent requests
                request = mSentRequestList.get(position - 2 - mReceivedRequestList.size());
            }

            mClickHandler.onClick(request);
        }
    }
}
