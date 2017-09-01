package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAreaBinding;
import project.hikerguide.databinding.ListItemGoogleAttributionBinding;
import project.hikerguide.databinding.ListItemPlaceNavBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.viewmodels.AreaViewModel;
import project.hikerguide.models.viewmodels.AttributionViewModel;
import project.hikerguide.models.viewmodels.PlaceViewModel;
import project.hikerguide.models.viewmodels.SearchAreaViewModel;

/**
 * Created by Alvin on 8/2/2017.
 */

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.AreaViewHolder> {
    // ** Constants ** //
    private static final int AREA_VIEW_TYPE         = 0;
    private static final int PLACE_VIEW_TYPE        = 1;
    private static final int SEARCH_MORE_VIEW_TYPE  = 2;
    private static final int ATTRIBUTION_VIEW_TYPE  = 3;

    // ** Member Variables ** //
    private List<Object> mAreaList;
    private ClickHandler mClickHandler;
    private SearchAreaViewModel mViewModel;
    private boolean mShowSearchMore             = false;
    private boolean mShowGoogleAttribution      = false;
    private boolean mShowAttribution            = false;
    private boolean mShowAttributionProgressBar = false;

    public AreaAdapter(SearchAreaViewModel viewModel, ClickHandler clickHandler) {
        mViewModel = viewModel;
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
                layoutId = R.layout.list_item_place_nav;
                break;

            case SEARCH_MORE_VIEW_TYPE:
                layoutId = R.layout.list_item_search_more;
                break;

            case ATTRIBUTION_VIEW_TYPE:
                layoutId = R.layout.list_item_google_attribution;
                break;
        }

        // Init the ViewDataBinding
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new AreaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(AreaViewHolder holder, int position) {
        if (position == mAreaList.size() && mShowSearchMore) {
            return;
        }

        // Bind the Data to the View
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        if (mAreaList != null) {
            if ((mShowSearchMore && mAreaList.size() > 0) || mShowAttribution) {
                return mAreaList.size() + 1;
            }
            return mAreaList.size();
        }

        return 0;
    }

    @Override
    public int getItemViewType(int position) {

        // Return the ViewType based on the type of Objects stored in the List
        if (position == mAreaList.size()) {
            if (mShowSearchMore) {
                return SEARCH_MORE_VIEW_TYPE;
            } else if (mShowAttribution) {
                return ATTRIBUTION_VIEW_TYPE;
            }
        } else if (mAreaList.get(position) instanceof Area) {
            return AREA_VIEW_TYPE;
        } else if (mAreaList.get(position) instanceof PlaceModel) {
            return PLACE_VIEW_TYPE;
        }

        return super.getItemViewType(position);
    }

    public void setAreaList(List<Object> areaList) {
        mAreaList = areaList;

        // Hide the search more list item
        mShowSearchMore = false;

        notifyDataSetChanged();
    }

    public void clearAdapter() {
        mAreaList = new ArrayList<>();

        mShowSearchMore             = false;
        mShowAttribution            = false;
        mShowGoogleAttribution      = false;
        mShowAttributionProgressBar = false;

        notifyDataSetChanged();
    }

    /**
     * Adds an additional list item to allow users to search Google Places for their query
     */
    public void showSearchMore() {
        if (!mShowSearchMore) {
            mShowSearchMore = true;
            mShowAttribution = false;

            if (mAreaList == null) {
                mAreaList = new ArrayList<>();
            }

            notifyDataSetChanged();
        }
    }

    /**
     * Sets whether the attribution bar should be shown
     *
     * @param showAttribution    Boolean value for whether attribution bar should be shown
     */
    public void setShowAttribution(boolean showAttribution) {

        mShowAttribution = showAttribution;

        // Instantiate mAreaList if necessary
        if (mAreaList == null) {
            mAreaList = new ArrayList<>();
        }

        if (mShowAttribution) {

            // Hide the search more list item as they do not show at the same time
            mShowSearchMore = false;
            notifyItemChanged(mAreaList.size());
        } else {

            // Hide the progress bar so that it doesn't show when the search bar regains focus
            mShowAttributionProgressBar = false;
            notifyItemRemoved(mAreaList.size());
        }
    }

    /**
     * Sets whether the Google Attribution should be shown in the attribution bar. When running
     * queries against the Firebase Database, there is no need to show the Google Attribution.
     * However, when running a query against Google Places API, the attribution is necessary.
     *
     * @param showGoogleAttribution    Boolean value for whether the attribution bar should show
     *                                 the Google logo
     */
    public void setShowGoogleAttribution(boolean showGoogleAttribution) {

        mShowGoogleAttribution = showGoogleAttribution;

       if (mShowGoogleAttribution) {

           // If showing the Google Attribution, then the attribution bar must also be showing
           setShowAttribution(true);
       }

        notifyItemChanged(mAreaList.size());
    }

    /**
     * Sets whether the progress bar in the attribution bar should be showing to indicate an on-
     * going search.
     *
     * @param showProgressBar    Boolean value for whether the ProgressBar should show in the
     *                           attribution bar
     */
    public void setShowAttributionProgressBar(boolean showProgressBar) {

        mShowAttributionProgressBar = showProgressBar;

        if (mShowAttribution) {
            notifyItemChanged(mAreaList.size());
        }
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

            if (!(mBinding instanceof ListItemGoogleAttributionBinding)) {
                mBinding.getRoot().setOnClickListener(this);
            }
        }

        private void bind(int position) {

            // ViewHolder for attribution
            if (position == mAreaList.size() && mShowAttribution) {

                // Initialize the ViewModel
                AttributionViewModel vm = new AttributionViewModel();

                // Set whether the Progressbar and Google logo should be showing the in attribution
                // bar
                vm.setShowProgress(mShowAttributionProgressBar);
                vm.setShowAttribution(mShowGoogleAttribution);

                ((ListItemGoogleAttributionBinding) mBinding).setVm(vm);

                return;
            }

            // Get reference to the Object at the corresponding position
            Object object = mAreaList.get(position);

            // Check the type of Object it is and load the proper ViewModel
            if (object instanceof Area) {
                AreaViewModel vm = new AreaViewModel((Area) object, mViewModel);
                ((ListItemAreaBinding) mBinding).setVm(vm);
            } else if (object instanceof PlaceModel) {
                PlaceViewModel vm = new PlaceViewModel((PlaceModel) object, mViewModel);
                ((ListItemPlaceNavBinding) mBinding).setVm(vm);
            }
        }

        @Override
        public void onClick(View view) {

            // Get the position that was clicked
            int position = getAdapterPosition();

            if (position == mAreaList.size()) {
                mClickHandler.onClickArea(null);
                return;
            }

            // Pass the Area associated with the ViewHolder to the ClickHandler
            mClickHandler.onClickArea(mAreaList.get(position));
        }
    }
}
