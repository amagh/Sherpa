package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemTrailBinding;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.viewmodels.DoubleSearchViewModel;
import project.hikerguide.models.viewmodels.TrailViewModel;
import project.hikerguide.ui.adapters.abstractadapters.HideableAdapter;
import project.hikerguide.ui.adapters.interfaces.ClickHandler;

/**
 * Created by Alvin on 8/3/2017.
 */

public class TrailAdapter extends HideableAdapter<TrailAdapter.TrailViewHolder> {

    // ** Constants ** //
    private static final int TRAIL_VIEW_TYPE        = 6684;
    private static final int ADD_TRAIL_VIEW_TYPE    = 5342;

    // ** Member Variables ** //
    private DoubleSearchViewModel mViewModel;
    private ClickHandler<Trail> mClickHandler;
    private boolean mHideAdapter;

    private final SortedListAdapterCallback<Trail> mCallback = new SortedListAdapterCallback<Trail>(this) {
        @Override
        public int compare(Trail o1, Trail o2) {
            return o1.name.compareTo(o2.name);
        }

        @Override
        public boolean areContentsTheSame(Trail oldItem, Trail newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Trail item1, Trail item2) {
            return item1 == item2;
        }
    };
    private final SortedList<Trail> mTrailList = new SortedList<>(Trail.class, mCallback);

    public TrailAdapter(DoubleSearchViewModel viewModel, ClickHandler<Trail> clickHandler) {
        mViewModel = viewModel;
        mClickHandler = clickHandler;
    }

    @Override
    public TrailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the resources that will be used for DataBinding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = R.layout.list_item_trail;

        switch (viewType) {
            case TRAIL_VIEW_TYPE: layoutId = R.layout.list_item_trail;
                break;

            case ADD_TRAIL_VIEW_TYPE: layoutId = R.layout.list_item_add_trail;
                break;
        }

        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new TrailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(TrailViewHolder holder, int position) {
        if (position < mTrailList.size()) {
            holder.bind(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mTrailList.size()) {
            return ADD_TRAIL_VIEW_TYPE;
        } else {
            return TRAIL_VIEW_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        if (mHideAdapter) {
            return 0;
        } else {
            return mTrailList.size() + 1;
        }
    }

    /**
     * Replaces the contents of mTrailList with the contents of the new list
     *
     * @param trailList    List to replace the contents of mTrailList with
     */
    public void setAdapterItems(List<Trail> trailList) {

        // Start a batched operation
        mTrailList.beginBatchedUpdates();

        // Iterate through and remove any Trails that are not the List from the signature
        for (int i = mTrailList.size() - 1; i >= 0; i--) {
            if (!trailList.contains(mTrailList.get(i))) {
                mTrailList.removeItemAt(i);
            }
        }

        // Add the contents of the signature to mTrailList
        mTrailList.addAll(trailList);
        mTrailList.endBatchedUpdates();
    }

    /**
     * Hides the Adapter, but keeps the items in the Adapter so they can be re-displayed later in
     * {@link #show()}
     */
    public void hide() {
        mHideAdapter = true;

        notifyDataSetChanged();
    }

    /**
     * Shows the Adapter and any items that were in place before {@link #hide()} was called
     */
    public void show() {
        mHideAdapter = false;

        notifyDataSetChanged();
    }

    class TrailViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // ** Member Variables ** //
        private ViewDataBinding mBinding;

        TrailViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
        }

        void bind(int position) {

            // Get reference to the Trail for the corresponding ViewHolder position
            Trail trail = mTrailList.get(position);

            // Instantiate the ViewModel and bind it to the ListItemTrailBinding
            TrailViewModel vm = new TrailViewModel(mViewModel, trail);
            ((ListItemTrailBinding) mBinding).setVm(vm);
        }

        @Override
        public void onClick(View view) {

            // Get the Trail that was Clicked
            int position = getAdapterPosition();

            Trail trail = null;

            // Get reference to the corresponding Trail
            if (position < mTrailList.size()) {
                trail = mTrailList.get(position);
            }

            // Pass the Trail through the ClickHandler
            mClickHandler.onClick(trail);
        }
    }
}
