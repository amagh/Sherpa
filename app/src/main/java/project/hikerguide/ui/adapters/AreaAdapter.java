package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAreaBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.viewmodels.AreaViewModel;

/**
 * Created by Alvin on 8/2/2017.
 */

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.AreaViewHolder> {
    // ** Member Variables ** //
    private List<Area> mAreaList;
    private ClickHandler mClickHandler;

    public AreaAdapter(ClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    @Override
    public AreaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the variables for DataBinding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = R.layout.list_item_area;

        // Init the ViewDataBinding
        ListItemAreaBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new AreaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(AreaViewHolder holder, int position) {

        // Bind the Data to the View
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        if (mAreaList != null) {
            return mAreaList.size();
        }

        return 0;
    }

    public interface ClickHandler {
        void onClickArea(Area area);
    }

    class AreaViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // ** Member Variables ** //
        ListItemAreaBinding mBinding;

        public AreaViewHolder(ListItemAreaBinding binding) {
            super(binding.getRoot());

            mBinding = binding;

            mBinding.getRoot().setOnClickListener(this);
        }

        private void bind(int position) {

            // Get reference to the Area at the corresponding position
            Area area = mAreaList.get(position);

            AreaViewModel vm = new AreaViewModel(area);
            mBinding.setVm(vm);
        }

        @Override
        public void onClick(View view) {

            // Get the position that was clicked
            int position = getAdapterPosition();

            // Pass the Area associated with the ViewHolder to the ClickHandler
            mClickHandler.onClickArea(mAreaList.get(position));
        }
    }
}
