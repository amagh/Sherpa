package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemGoogleAttributionBinding;
import project.hikerguide.databinding.ListItemPlaceBinding;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.viewmodels.AttributionViewModel;
import project.hikerguide.models.viewmodels.PlaceViewModel;

/**
 * Created by Alvin on 8/17/2017.
 */

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    // ** Constants ** //
    private static final int VIEW_TYPE_PLACE        = 0;
    private static final int VIEW_TYPE_ATTRIBUTION  = 1;

    // ** Member Variables ** //
    private List<PlaceModel> mPlaceList;
    private ClickHandler mClickHandler;
    private boolean mShowAttribution = false;
    private boolean mShowProgress = false;

    public PlaceAdapter(ClickHandler clickHandler) {
        this.mClickHandler = clickHandler;
    }

    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the LayoutInflater
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = -1;

        // Select the layout based on the ViewType
        switch (viewType) {
            case VIEW_TYPE_PLACE:
                layoutId = R.layout.list_item_place;
                break;

            case VIEW_TYPE_ATTRIBUTION:
                layoutId = R.layout.list_item_google_attribution;
                break;
        }

        // DataBind inflate the layout
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);

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

            // Add one for the list item for the attribution
            return mPlaceList.size() + 1;
        }

        // If the search widget has focus, then it will show the attribution.
        if (mShowAttribution) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {

        // Show the attribution if there is no List of Places --or-- if it the List is populated,
        // then the attribution should occupy the last space
        if (mPlaceList == null || position == mPlaceList.size()) {
            return VIEW_TYPE_ATTRIBUTION;
        } else {
            return VIEW_TYPE_PLACE;
        }
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

    /**
     * Setter for whether the attribution list item should be showing
     *
     * @param showAttribution    Boolean value for whether the attribution should be showing
     */
    public void setShowAttribution(boolean showAttribution) {

        // Set the memvar to the parameter and notify of change
        mShowAttribution = showAttribution;

        if (mShowAttribution) {
            notifyItemInserted(0);
        } else {
            notifyItemRemoved(0);
        }
    }

    /**
     * Setter for whether the ProgressBar for the attributation list item should be showing
     *
     * @param showProgress    Boolean value for whether the ProgressBar should be showing
     */
    public void setShowProgress(boolean showProgress) {

        // Set the memvar and notify change
        mShowProgress = showProgress;

        if (mPlaceList == null) {
            notifyItemChanged(0);
        } else {
            notifyItemChanged(mPlaceList.size());
        }
    }

    public interface ClickHandler {
        void onClickPlace(PlaceModel placeModel);
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Member Variables ** //
        ViewDataBinding mBinding;

        public PlaceViewHolder(ViewDataBinding binding) {
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

            // Check if binding to the attribution list item
            if (mPlaceList == null || position == mPlaceList.size()) {

                // Create the ViewModel
                AttributionViewModel vm = new AttributionViewModel();

                // Set whether the ProgressBar should be showing
                if (mShowProgress) {
                    vm.setShowProgress(true);
                } else {
                    vm.setShowProgress(false);
                }

                // Bind the ViewModel
                ((ListItemGoogleAttributionBinding) mBinding).setVm(vm);
            } else {

                // Get the PlaceModel corresponding to the ViewHolder
                PlaceModel placeModel = mPlaceList.get(position);

                // Init the PlaceViewModel and pass the PlaceModel to it
                PlaceViewModel vm = new PlaceViewModel(placeModel, null);
                ((ListItemPlaceBinding) mBinding).setVm(vm);
            }
        }
    }
}
