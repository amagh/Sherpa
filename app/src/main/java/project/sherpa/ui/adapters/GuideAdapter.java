package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.databinding.library.baseAdapters.BR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.R;
import project.sherpa.databinding.ListItemGuideBinding;
import project.sherpa.databinding.ListItemGuideCompactBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.viewmodels.GuideViewModel;

import static project.sherpa.utilities.ColorGenerator.HIGHLIGHT_POSITION;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {
    // ** Constants ** //
    private static final int NORMAL_VIEW_TYPE = 0;
    private static final int SEARCH_VIEW_TYPE = 1;

    // ** Member Variables ** //
    private List<Guide> mGuideList;
    private ClickHandler mHandler;
    private boolean useSearchLayout;
    private String mHighlighted;
    private Author mAuthor;
    private Map<Guide, GuideViewModel> mViewModelMap = new HashMap<>();

    public GuideAdapter(ClickHandler clickHandler) {
        mHandler = clickHandler;
    }

    /**
     * Sets the Author to be used to check the each Guide is on the Author's favorite list
     *
     * @param author    Author whose Favorite List is to be checked for the presence of the Guides
     */
    public void setAuthor(Author author) {
        mAuthor = author;

        notifyDataSetChanged();
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
                layoutId = R.layout.list_item_guide_compact;
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
        if (mGuideList != null) {
            return mGuideList.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        // Only use search layout in SearchFragment
        if (!useSearchLayout) {
            return NORMAL_VIEW_TYPE;
        } else {
            return SEARCH_VIEW_TYPE;
        }
    }

    @Override
    public long getItemId(int position) {
        // Use the hashcode of the FirebaseId as the Id for the Adapter. This will allow a
        // completely different list of Guides to be set and animated correctly.
        return mGuideList.get(position).firebaseId.hashCode();
    }

    /**
     * Sets the resources that will be used to populate the Views in the RecyclerView
     *
     * @param guideList    A List of Guides to be used to bind data to the ViewHolders
     */
    public void setGuides(List<Guide> guideList) {
        // Set the mem var to the List parameter
        mGuideList = guideList;

        if (mGuideList != null) {
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
        // Check whether mGuideList has already been instantiated
        if (mGuideList == null) {
            // Has not been. Create a new List and put the Guide from the signature into it
            mGuideList = new ArrayList<>();
        }

        // Check to see if Guide is already in the Adapter
        boolean newGuide = true;

        for (Guide listGuide : mGuideList) {
            if (listGuide.firebaseId.equals(guide.firebaseId)) {
                newGuide = false;
            }
        }

        if (newGuide) {
            // Add the Guide to the List
            mGuideList.add(guide);
            notifyItemInserted(mGuideList.size() - 1);
        }
    }

    /**
     * Removes a Guide from the Adapter's List
     *
     * @param firebaseId    The FirebaseId of the Guide to be removed
     */
    public void removeGuide(String firebaseId) {
        // Iterate through the List until a match to the FirebaseId is found
        for (Guide guide : mGuideList) {
            if (guide.firebaseId.equals(firebaseId)) {
                // Get the position of the item to be removed
                int position = mGuideList.indexOf(guide);

                // Remove the item and notify
                mGuideList.remove(position);
                notifyItemRemoved(position);
                return;
            }
        }
    }

    /**
     * Retrieves the position of a Guide in the Adapter
     *
     * @param firebaseId    The FirebaseId of the Guide to be queried for its position
     * @return The position of the Guide in the Adapter's List. Returns -1 if no match is found.
     */
    public int getPosition(String firebaseId) {
        // Iterate through the List and try to find a match
        for (int i = 0; i < mGuideList.size(); i++) {
            if (mGuideList.get(i).firebaseId.equals(firebaseId)) {
                // If it matches, return the position of the Guide
                return i;
            }
        }

        return -1;
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
     * Checks whether the Adapter has a valid list of data
     *
     * @return True if mGuideList is null or empty. False if mGuideList is populated.
     */
    public boolean isEmpty() {
        return mGuideList == null || mGuideList.isEmpty();
    }

    /**
     * Updates the favorite status of each guide in the Adapter
     */
    public void updateViewModelFavorites() {

        for (GuideViewModel vm : mViewModelMap.values()) {
            vm.notifyPropertyChanged(BR.favorite);
        }
    }

    /**
     * For passing information about the clicked guide to the Activity/Fragment
     */
    public interface ClickHandler {
        void onGuideClicked(Guide guide);
        void onGuideLongClicked(Guide guide);
    }

    public class GuideViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        // ** Member Variables ** //
        ViewDataBinding mBinding;

        public GuideViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
            mBinding.getRoot().setOnLongClickListener(this);
        }

        public void bind(int position) {
            // Get the guide from the correlated position
            Guide guide = mGuideList.get(position);

            // Initialize a GuideViewModel using the Guide from the array and set it to the
            // DataBinding
            if (!useSearchLayout) {

                GuideViewModel vm = mViewModelMap.get(guide) != null
                        ? mViewModelMap.get(guide)
                        : new GuideViewModel(mBinding.getRoot().getContext(), guide);

                vm.setAuthor(mAuthor);

                ((ListItemGuideBinding) mBinding).top.setVm(vm);
                ((ListItemGuideBinding) mBinding).bottom.setVm(vm);
            } else {
                if (guide.firebaseId.equals(mHighlighted)) {
                    // If Guide's track is highlighted, then set the color swatch appropriately
                    position = HIGHLIGHT_POSITION;
                }

                ((ListItemGuideCompactBinding) mBinding)
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

            // Pass the corresponding Guide through the ClickHandler
            mHandler.onGuideClicked(mGuideList.get(position));

        }

        @Override
        public boolean onLongClick(View view) {

            // Only usable while searching
            if (useSearchLayout) {
                // Get the position of the clicked ViewHolder
                int position = getAdapterPosition();

                // Get the associated Guide
                Guide guide = mGuideList.get(position);

                // Check to see if any other Guide is currently highlighted
                if (mHighlighted != null) {
                    if (mHighlighted.equals(guide.firebaseId)) {

                        // If the selected Guide is already highlighted, un-highlight it
                        mHighlighted = null;
                        notifyItemChanged(position);
                    } else {

                        // Get the position of the previously highlighted Guide
                        int previousHighlightedPosition = GuideAdapter.this.getPosition(mHighlighted);

                        // Set the clicked Guide as highlighted
                        mHighlighted = guide.firebaseId;

                        // Update the ViewHolders
                        notifyItemChanged(previousHighlightedPosition);
                        notifyItemChanged(position);
                    }
                } else {

                    // Set the selected Guide as the highlighted and update the ViewHolder
                    mHighlighted = guide.firebaseId;
                    notifyItemChanged(position);
                }

                // Pass the information about the long-pressed guide to the observer
                mHandler.onGuideLongClicked(guide);
                return true;
            }

            return false;
        }
    }
}
