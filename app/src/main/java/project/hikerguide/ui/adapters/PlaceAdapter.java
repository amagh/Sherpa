package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemPlaceBinding;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.viewmodels.PlaceViewModel;

/**
 * Created by Alvin on 8/17/2017.
 */

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    // ** Member Variables ** //
    private List<PlaceModel> mPlaceList;
    private ClickHandler mClickHandler;

    public PlaceAdapter(ClickHandler clickHandler) {
        this.mClickHandler = clickHandler;
    }

    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the LayoutInflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // DataBind inflate the layout
        ListItemPlaceBinding binding = DataBindingUtil.inflate(inflater, R.layout.list_item_place, parent, false);

        return new PlaceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(PlaceViewHolder holder, int position) {

        // Bind the data model to the Views
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        if (mPlaceList != null) {
            return mPlaceList.size();
        }
        return 0;
    }

    /**
     * Sets the List of PlaceModels that will be used to to populate the ViewHolders
     *
     * @param placeList    List of PlaceModels to be used by the Adapter to populate the Views
     */
    public void setPlaceList(List<PlaceModel> placeList) {

        // Set the memvar to the List in the signature
        mPlaceList = placeList;

        // Notify change
        notifyDataSetChanged();
    }

    public interface ClickHandler {
        void onClickPlace(PlaceModel placeModel);
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Member Variables ** //
        ListItemPlaceBinding mBinding;

        public PlaceViewHolder(ListItemPlaceBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            // Get the position of the ViewHolder
            int position = getAdapterPosition();

            // Get the PlaceModel corresponding to the ViewHolder's position
            PlaceModel placeModel = mPlaceList.get(position);

            // Pass the PlaceModel to the ClickHandler
            mClickHandler.onClickPlace(placeModel);
        }

        private void bind(int position) {

            // Get the PlaceModel corresponding to the ViewHolder
            PlaceModel placeModel = mPlaceList.get(position);

            // Init the PlaceViewModel and pass the PlaceModel to it
            PlaceViewModel vm = new PlaceViewModel(placeModel, null);
            mBinding.setVm(vm);
        }
    }
}
