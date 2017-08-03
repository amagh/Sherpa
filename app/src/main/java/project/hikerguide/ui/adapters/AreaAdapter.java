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
import project.hikerguide.databinding.ListItemPlaceBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.viewmodels.AreaViewModel;
import project.hikerguide.models.viewmodels.PlaceViewModel;

/**
 * Created by Alvin on 8/2/2017.
 */

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.AreaViewHolder> {
    // ** Constants ** //
    private static final int AREA_VIEW_TYPE     = 0;
    private static final int PLACE_VIEW_TYPE    = 1;

    // ** Member Variables ** //
    private List<Object> mAreaList;
    private ClickHandler mClickHandler;

    public AreaAdapter(ClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    @Override
    public AreaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Init the variables for DataBinding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = -1;

        switch (viewType) {
            case AREA_VIEW_TYPE:
                layoutId = R.layout.list_item_area;
                break;

            case PLACE_VIEW_TYPE:
                layoutId = R.layout.list_item_place;
                break;
        }

        // Init the ViewDataBinding
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
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

    @Override
    public int getItemViewType(int position) {

        // Return the ViewType based on the type of Objects stored in the List
        if (mAreaList.get(position) instanceof Area) {
            return AREA_VIEW_TYPE;
        } else if (mAreaList.get(position) instanceof PlaceModel) {
            return PLACE_VIEW_TYPE;
        }
        return super.getItemViewType(position);
    }

    public void setAreaList(List<Object> areaList) {
        mAreaList = areaList;

        notifyDataSetChanged();
    }

    public interface ClickHandler {
        void onClickArea(Object object);
    }

    class AreaViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // ** Member Variables ** //
        ViewDataBinding mBinding;

        public AreaViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;

            mBinding.getRoot().setOnClickListener(this);
        }

        private void bind(int position) {

            // Get reference to the Object at the corresponding position
            Object object = mAreaList.get(position);

            // Check the type of Object it is and load the proper ViewModel
            if (object instanceof Area) {
                AreaViewModel vm = new AreaViewModel((Area) object);
                ((ListItemAreaBinding) mBinding).setVm(vm);
            } else if (object instanceof PlaceModel) {
                PlaceViewModel vm = new PlaceViewModel((PlaceModel) object);
                ((ListItemPlaceBinding) mBinding).setVm(vm);
            }


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
