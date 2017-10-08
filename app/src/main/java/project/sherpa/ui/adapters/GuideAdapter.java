package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.IntDef;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
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
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.adapters.interfaces.LongClickHandler;

import static project.sherpa.ui.adapters.GuideAdapter.SortingMethods.ALPHABETICAL;
import static project.sherpa.ui.adapters.GuideAdapter.SortingMethods.DATE;
import static project.sherpa.ui.adapters.GuideAdapter.SortingMethods.RATING;
import static project.sherpa.utilities.ColorGenerator.HIGHLIGHT_POSITION;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {
    // ** Constants ** //
    private static final int NORMAL_VIEW_TYPE = 0;
    private static final int SEARCH_VIEW_TYPE = 1;

    @IntDef({ALPHABETICAL, DATE, RATING})
    public @interface SortingMethods {
        int DATE            = 0;
        int RATING          = 1;
        int ALPHABETICAL    = 2;
    }

    // ** Member Variables ** //
    private ClickHandler<Guide> mClickHandler;
    private LongClickHandler<Guide> mLongClickHandler;
    private boolean useSearchLayout;
    private String mHighlighted;
    private Author mAuthor;

    @SortingMethods
    private int mSortMode;

    private SortedListAdapterCallback<Guide> mSortedAdapter = new SortedListAdapterCallback<Guide>(this) {
        @Override
        public int compare(Guide o1, Guide o2) {
            switch (mSortMode) {
                case DATE:
                    return o1.getDate() > o2.getDate()
                            ? 1
                            : o1.getDate() < o2.getDate()
                            ? -1
                            : 0;
                case RATING:
                    return o1.rating > o2.rating
                            ? 1
                            : o1.rating < o2.rating
                            ? -1
                            : 0;

                case ALPHABETICAL:
                    return o1.getTitle().compareTo(o2.getTitle());
            }

            return 0;
        }

        @Override
        public boolean areContentsTheSame(Guide oldItem, Guide newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Guide item1, Guide item2) {
            return item1.firebaseId.equals(item2.firebaseId);
        }
    };
    private SortedList<Guide> mSortedList = new SortedList<Guide>(Guide.class, mSortedAdapter);
    private Map<Guide, GuideViewModel> mViewModelMap = new HashMap<>();

    public GuideAdapter(ClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public GuideAdapter(ClickHandler<Guide> clickHandler, LongClickHandler<Guide> longClickHandler) {
        mClickHandler = clickHandler;
        mLongClickHandler = longClickHandler;
    }

    public void setSortingMode(@SortingMethods int sortMode) {
        mSortMode = sortMode;
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
        return mSortedList.size();
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
        return mSortedList.get(position).firebaseId.hashCode();
    }

    /**
     * Sets the resources that will be used to populate the Views in the RecyclerView
     *
     * @param guideList    A List of Guides to be used to bind data to the ViewHolders
     */
    public void setGuides(List<Guide> guideList) {

        // Clear the SortedList and then add all the items in the guideList to the SortedList
        mSortedList.beginBatchedUpdates();
        mSortedList.clear();
        mSortedList.addAll(guideList);
        mSortedList.endBatchedUpdates();
    }

    /**
     * Adds a Guide to the Array of Guides to be displayed
     *
     * @param guide    Guide to be added
     */
    public void addGuide(Guide guide) {
        mSortedList.add(guide);
    }

    /**
     * Removes a Guide from the Adapter's List
     *
     * @param firebaseId    The FirebaseId of the Guide to be removed
     */
    public void removeGuide(String firebaseId) {
        // Iterate through the List until a match to the FirebaseId is found
        for (int i = mSortedList.size() - 1; i >= 0; i--) {
            Guide guide = mSortedList.get(i);

            if (guide.firebaseId.equals(firebaseId)) {
                mSortedList.removeItemAt(i);
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
        for (int i = 0; i < mSortedList.size(); i++) {
            if (mSortedList.get(i).firebaseId.equals(firebaseId)) {
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
     * Updates the favorite status of each guide in the Adapter
     */
    public void updateViewModelFavorites() {

        for (GuideViewModel vm : mViewModelMap.values()) {
            vm.notifyPropertyChanged(BR.favorite);
        }
    }

    /**
     * Returns a List of FirebaseIds for all the Guides in the Adapter
     *
     * @return List of FirebaseIds of all Guides in the Adapter
     */
    public List<String> getFirebaseIds() {

        List<String> firebaseIdList = new ArrayList<>();

        for (int i = 0; i < mSortedList.size(); i++) {
            firebaseIdList.add(mSortedList.get(i).firebaseId);
        }

        return firebaseIdList;
    }

    /**
     * Removes all elements from the Adapter
     */
    public void clear() {
        mSortedList.clear();
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
            Guide guide = mSortedList.get(position);

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
            mClickHandler.onClick(mSortedList.get(position));

        }

        @Override
        public boolean onLongClick(View view) {

            // Only usable while searching
            if (useSearchLayout) {
                // Get the position of the clicked ViewHolder
                int position = getAdapterPosition();

                // Get the associated Guide
                Guide guide = mSortedList.get(position);

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
                mLongClickHandler.onLongClick(guide);
                return true;
            }

            return false;
        }
    }
}
