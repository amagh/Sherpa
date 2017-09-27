package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.sherpa.R;
import project.sherpa.databinding.ListItemFriendBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.viewmodels.AuthorViewModel;
import project.sherpa.ui.adapters.callbacks.AlphabeticalAuthorCallback;
import project.sherpa.ui.adapters.interfaces.ClickHandler;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    // ** Member Variables ** //
    private SortedListAdapterCallback<Author> mSortedCallback = new AlphabeticalAuthorCallback(this);
    private SortedList<Author> mSortedList = new SortedList<>(Author.class, mSortedCallback);
    private ClickHandler<Author> mClickHandler;

    public FriendAdapter(ClickHandler<Author> clickHandler) {
        mClickHandler = clickHandler;
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        ListItemFriendBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_friend, parent, false);

        return new FriendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(FriendViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mSortedList.size();
    }

    /**
     * Adds an Author to be displayed in the Adapter
     *
     * @param author    Author to be displayed
     */
    public void addFriend(Author author) {
        mSortedList.add(author);
    }

    class FriendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Member Variables ** //
        private ListItemFriendBinding mBinding;

        FriendViewHolder(ListItemFriendBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
        }

        void bind(int position) {

            // Bind the data for the Author at the ViewHolder's position to the ViewHolder's Views
            Author author = mSortedList.get(position);
            AuthorViewModel vm = new AuthorViewModel((AppCompatActivity) mBinding.getRoot().getContext(), author);
            mBinding.setVm(vm);
        }

        @Override
        public void onClick(View view) {

            // Return the Author at the clicked position
            int position = getAdapterPosition();
            Author author = mSortedList.get(position);
            mClickHandler.onClick(author);
        }
    }
}
