package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import project.sherpa.R;
import project.sherpa.databinding.ListItemFriendBinding;
import project.sherpa.databinding.ListItemSearchUserBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.viewmodels.AuthorViewModel;
import project.sherpa.models.viewmodels.ListItemFriendViewModel;
import project.sherpa.models.viewmodels.SearchUserViewModel;
import timber.log.Timber;

/**
 * Created by Alvin on 9/20/2017.
 */

public class ChatAuthorAdapter extends RecyclerView.Adapter<ChatAuthorAdapter.AuthorViewHolder> {

    // ** Constants ** //
    private static final int AUTHOR_VIEW_TYPE       = 0;
    private static final int SEARCH_USER_VIEW_TYPE  = 1;

    // ** Member Variables ** //
    private SortedListAdapterCallback<Author> mSortedCallback = new SortedListAdapterCallback<Author>(this) {
        @Override
        public int compare(Author o1, Author o2) {
            return o1.name.compareTo(o2.name);
        }

        @Override
        public boolean areContentsTheSame(Author oldItem, Author newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Author item1, Author item2) {
            return item1.firebaseId.equals(item2.firebaseId);
        }
    };
    private SortedList<Author> mSortedList = new SortedList<>(Author.class, mSortedCallback);
    private Set<Author> mSelected = new HashSet<>();
    private SearchUserViewModel mViewModel;

    public ChatAuthorAdapter(@NonNull SearchUserViewModel viewModel) {
        mViewModel = viewModel;
    }

    @Override
    public AuthorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = -1;

        // Inflate layout based on ViewType
        switch (viewType) {
            case AUTHOR_VIEW_TYPE:      layoutId = R.layout.list_item_friend;
                break;

            case SEARCH_USER_VIEW_TYPE: layoutId = R.layout.list_item_search_user;
                break;
        }

        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new AuthorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(AuthorViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mSortedList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {

        if (position < mSortedList.size()) {
            return AUTHOR_VIEW_TYPE;
        } else {
            return SEARCH_USER_VIEW_TYPE;
        }
    }

    /**
     * Sets the List of friends to display in the Adapter
     *
     * @param friendList    List of friends to display
     */
    public void setFriendList(Collection<Author> friendList) {

        mSortedList.beginBatchedUpdates();

        // Iterate through the two Lists and check if any items are to be removed
        for (int i = mSortedList.size() - 1; i >= 0; i--) {
            Author existingFriend = mSortedList.get(i);
            boolean keep = false;

            // If the friend has been selected, do not remove it from the Adapter
            if (mSelected.contains(existingFriend)) continue;
            for (Author newFriend : friendList) {
                if (newFriend.firebaseId.equals(existingFriend.firebaseId)) {
                    keep = true;
                    break;
                }
            }

            // Remove the item as it is new in the friendList
            if (!keep) mSortedList.removeItemAt(i);
        }

        // Add all other items from the friendList to the SortedList
        for (Author friend : friendList) {
            mSortedList.add(friend);
        }

        mSortedList.endBatchedUpdates();
    }

    /**
     * Adds an Author to be displayed by the Adapter
     *
     * @param author    Author to be added
     */
    public void addAuthor(Author author) {

        // Check to ensure the Author isn't already in the List
        for (int i = 0; i < mSortedList.size(); i++) {
            Author listAuthor = mSortedList.get(i);

            if (author.firebaseId.equals(listAuthor.firebaseId)) {
                return;
            }
        }

        mSortedList.add(author);
    }

    /**
     * Returns a List of the items that have been selected by the user to add a new Chat
     *
     * @return Items that were selected
     */
    public List<String> getSelectedIds() {

        Set<String> selectedIdSet = new HashSet<>();

        for (int i = 0; i < mSortedList.size(); i++) {
            Author selected = mSortedList.get(i);
            selectedIdSet.add(selected.firebaseId);
        }

        return new ArrayList<>(selectedIdSet);
    }

    /**
     * Sets an item to be selected/unselected by the user
     *
     * @param author      Author to set the selected status for
     * @param selected    boolean value for whether it should be selected
     */
    private void setSelected(Author author, boolean selected) {

        // Add/remove the Author from the List of selected items
        if (selected && !mSelected.contains(author)) {
            mSelected.add(author);
        } else if (!selected && mSelected.contains(author)) {
            mSelected.remove(author);
        }

        // Notify change
        if (mSortedList.indexOf(author) != SortedList.INVALID_POSITION) {
            notifyItemChanged(mSortedList.indexOf(author));
        }
    }

    class AuthorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Member Variables ** //
        private ViewDataBinding mBinding;

        AuthorViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            mBinding.getRoot().setOnClickListener(this);
        }

        void bind(int position) {

            if (position < mSortedList.size()) {
                Author author = mSortedList.get(position);
                AuthorViewModel vm =
                        new AuthorViewModel((AppCompatActivity) mBinding.getRoot().getContext(), author);
                ListItemFriendViewModel fvm = new ListItemFriendViewModel(author);

                // Set the item background based on whether the user has selected this Author for the
                // chat
                vm.setSelected(mSelected.contains(author));

                ((ListItemFriendBinding) mBinding).setVm(vm);
                ((ListItemFriendBinding) mBinding).setFvm(fvm);
            } else {
                ((ListItemSearchUserBinding) mBinding).setVm(mViewModel);
            }
        }

        @Override
        public void onClick(View view) {

            // Get the Author corresponding to the clicked ViewHolder and toggle its selected status
            int position = getAdapterPosition();

            if (position < mSortedList.size()) {
                Author author = mSortedList.get(position);

                boolean selected = !mSelected.contains(author);
                setSelected(author, selected);
            } else {
                SearchUserViewModel vm = ((ListItemSearchUserBinding) mBinding).getVm();
                Author author = vm.getAuthor();

                if (author != null) {
                    addAuthor(author);
                    setSelected(author, true);
                    vm.reset();
                } else {
                    Toast.makeText(
                            view.getContext(),
                            "Username not found.",
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }
}
