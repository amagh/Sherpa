package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemGuideBinding;
import project.hikerguide.databinding.ListItemGuideSearchBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.viewmodels.GuideViewModel;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {
    // ** Constants ** //
    private static final int NORMAL_VIEW_TYPE = 0;
    private static final int SEARCH_VIEW_TYPE = 1;

    // ** Member Variables ** //
    private Guide[] mGuides;
    private ClickHandler mHandler;
    private boolean useSearchLayout;

    public GuideAdapter(ClickHandler clickHandler) {
        mHandler = clickHandler;
    }

    @Override
    public GuideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = -1;

        // Set the layout based on the ViewType returned
        switch (viewType) {
            case NORMAL_VIEW_TYPE:
                layoutId = R.layout.list_item_guide;
                break;

            case SEARCH_VIEW_TYPE:
                layoutId = R.layout.list_item_guide_search;
                break;
        }

        // Create the ViewDataBinding by inflating the layout
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new GuideViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(GuideViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        // Return the length of mGuides if it is not null
        if (mGuides != null) {
            return mGuides.length;
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        // Only use search layout in FragmentSearch
        if (!useSearchLayout) {
            return NORMAL_VIEW_TYPE;
        } else {
            return SEARCH_VIEW_TYPE;
        }
    }

    /**
     * Sets the resources that will be used to populate the Views in the RecyclerView
     *
     * @param guides    Array of Guides that describing the Guides to populate
     */
    public void setGuides(Guide[] guides) {
        // Set the mem var to the array paramter
        mGuides = guides;

        if (mGuides != null) {
            // Notify of change in data
            notifyDataSetChanged();
        }
    }

    /**
     * Adds a Guide to the Array of Guides to be displayed
     *
     * @param guide    Guide to be added
     */
    public void addGuide(Guide guide) {
        // Check whether mGuides has already been instantiated
        if (mGuides == null) {
            // Has not been. Create a new Array and put the Guide from the signature into it
            mGuides = new Guide[] {guide};
        } else {
            // Guide Array already exists. Create a new Array one size larger than the one that
            // already exists
            Guide[] newGuides = new Guide[mGuides.length + 1];

            // Copy the current Array of Guides into the new one
            System.arraycopy(mGuides, 0, newGuides, 0, mGuides.length);

            // Add the Guide from the paramter to the new Array
            newGuides[newGuides.length - 1] = guide;

            // Set the mem var to the new Array
            mGuides = newGuides;
        }

        notifyItemInserted(mGuides.length - 1);
    }

    /**
     * Sets whether the Adapter should use the search layout for guides
     *
     * @param useSearchLayout    Boolean value for whether search layout should be enabled
     */
    public void setUseSearchLayout(boolean useSearchLayout) {
        this.useSearchLayout = useSearchLayout;
    }

    /**
     * For passing information about the clicked guide to the Activity/Fragment
     */
    public interface ClickHandler {
        void onGuideClicked(Guide guide);
    }

    public class GuideViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // ** Member Variables ** //
        ViewDataBinding mBinding;

        public GuideViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
        }

        public void bind(int position) {
            // Get the guide from the correlated position
            Guide guide = mGuides[position];

            // Initialize a GuideViewModel using the Guide from the array and set it to the
            // DataBinding
            if (!useSearchLayout) {
                ((ListItemGuideBinding) mBinding)
                        .setVm(new GuideViewModel(mBinding.getRoot().getContext(), guide));
            } else {
                ((ListItemGuideSearchBinding) mBinding)
                        // Add the position to the Constructor so that color position matches the
                        // color given the track.
                        .setVm(new GuideViewModel(mBinding.getRoot().getContext(), guide, position));
            }
            mBinding.executePendingBindings();
        }

        @Override
        public void onClick(View view) {
            // Get the position of the clicked Adapter
            int position = getAdapterPosition();

            // Pass the corresponding Guide through the Clickhandler
            mHandler.onGuideClicked(mGuides[position]);
        }
    }
}
