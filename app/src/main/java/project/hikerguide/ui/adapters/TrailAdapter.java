package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemTrailBinding;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.viewmodels.TrailViewModel;

/**
 * Created by Alvin on 8/3/2017.
 */

public class TrailAdapter extends RecyclerView.Adapter<TrailAdapter.TrailViewHolder> {
    // ** Member Variables ** //
    private final SortedList.Callback<Trail> mCallback = new SortedList.Callback<Trail>() {
        @Override
        public int compare(Trail o1, Trail o2) {
            return o1.name.compareTo(o2.name);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Trail oldItem, Trail newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Trail item1, Trail item2) {
            return item1 == item2;
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    };

    private final SortedList<Trail> mTrailList = new SortedList<>(Trail.class, mCallback);

    @Override
    public TrailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the resources that will be used for DataBinding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = R.layout.list_item_trail;

        ListItemTrailBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new TrailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(TrailViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mTrailList.size();
    }

    /**
     * Replaces the contents of mTrailList with the contents of the new list
     *
     * @param trailList    List to replace the contents of mTrailList with
     */
    public void replaceAll(List<Trail> trailList) {

        // Start a batched operation
        mTrailList.beginBatchedUpdates();

        // Iterate through and remove any Trails that are not the List from the signature
        for (int i = mTrailList.size() - 1; i >= 0; i--) {
            Trail trail = mTrailList.get(i);

            if (!trailList.contains(trail)) {
                mTrailList.remove(trail);
            }
        }

        // Add the contents of the signature to mTrailList
        mTrailList.addAll(trailList);
        mTrailList.endBatchedUpdates();
    }

    class TrailViewHolder extends RecyclerView.ViewHolder {
        // ** Member Variables ** //
        private ListItemTrailBinding mBinding;

        TrailViewHolder(ListItemTrailBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
        }

        void bind(int position) {

            // Get reference to the Trail for the corresponding ViewHolder position
            Trail trail = mTrailList.get(position);

            // Instantiate the ViewModel and bind it to the ListItemTrailBinding
            TrailViewModel vm = new TrailViewModel(trail);
            mBinding.setVm(vm);
        }
    }
}
