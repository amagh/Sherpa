package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
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
    private List<Trail> mTrailList;

    @Override
    public TrailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        if (mTrailList != null) {
            return mTrailList.size();
        }
        return 0;
    }

    class TrailViewHolder extends RecyclerView.ViewHolder {
        // ** Member Variables ** //
        private ListItemTrailBinding mBinding;

        TrailViewHolder(ListItemTrailBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
        }

        void bind(int position) {
            Trail trail = mTrailList.get(position);
            TrailViewModel vm = new TrailViewModel(trail);
            mBinding.setVm(vm);
        }
    }
}
