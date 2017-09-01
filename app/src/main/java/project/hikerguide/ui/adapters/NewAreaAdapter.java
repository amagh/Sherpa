package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.IntDef;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAreaBinding;
import project.hikerguide.databinding.ListItemGoogleAttributionBinding;
import project.hikerguide.databinding.ListItemPlaceNavBinding;
import project.hikerguide.models.AreaAdapterSortable;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.PlaceModel;
import project.hikerguide.models.viewmodels.AreaViewModel;
import project.hikerguide.models.viewmodels.AttributionViewModel;
import project.hikerguide.models.viewmodels.DoubleSearchViewModel;
import project.hikerguide.models.viewmodels.PlaceViewModel;

import static project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType.ATTRIBUTION;
import static project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType.ATTRIBUTION_GOOGLE;
import static project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType.ATTRIBUTION_GOOGLE_PROGRESS;
import static project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType.ATTRIBUTION_PROGRESS;
import static project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType.HIDDEN;
import static project.hikerguide.ui.adapters.NewAreaAdapter.ExtraListItemType.SEARCH_MORE;

/**
 * Created by Alvin on 9/1/2017.
 */

public class NewAreaAdapter extends RecyclerView.Adapter<NewAreaAdapter.AreaViewHolder> implements Hideable {

    // ** Constants ** //
    private static final int AREA_VIEW_TYPE         = 5096;
    private static final int PLACE_VIEW_TYPE        = 3025;
    private static final int SEARCH_MORE_VIEW_TYPE  = 5395;
    private static final int ATTRIBUTION_VIEW_TYPE  = 7329;

    @IntDef({HIDDEN, SEARCH_MORE, ATTRIBUTION, ATTRIBUTION_PROGRESS, ATTRIBUTION_GOOGLE, ATTRIBUTION_GOOGLE_PROGRESS})
    public @interface ExtraListItemType {
        int HIDDEN                          = 0;
        int SEARCH_MORE                     = 1;
        int ATTRIBUTION                     = 2;
        int ATTRIBUTION_PROGRESS            = 3;
        int ATTRIBUTION_GOOGLE              = 4;
        int ATTRIBUTION_GOOGLE_PROGRESS     = 5;
    }

    // ** Member Variables ** //
    private DoubleSearchViewModel mViewModel;
    private ClickHandler<Object> mClickHandler;
    private boolean mHideAdapter;

    @NewAreaAdapter.ExtraListItemType
    private int mExtraItemType;

    private SortedListAdapterCallback<AreaAdapterSortable> mCallback = new SortedListAdapterCallback<AreaAdapterSortable>(this) {
        @Override
        public int compare(AreaAdapterSortable o1, AreaAdapterSortable o2) {
            return o1.getName().compareTo(o2.getName());
        }

        @Override
        public boolean areContentsTheSame(AreaAdapterSortable oldItem, AreaAdapterSortable newItem) {
            return oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areItemsTheSame(AreaAdapterSortable item1, AreaAdapterSortable item2) {
            return item1 == item2;
        }
    };
    private SortedList<AreaAdapterSortable> mSortedList = new SortedList<>(AreaAdapterSortable.class, mCallback);

    public NewAreaAdapter(DoubleSearchViewModel viewModel, ClickHandler<Object> clickHandler) {
        mViewModel = viewModel;
        mClickHandler = clickHandler;
    }

    @Override
    public AreaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the LayoutInflater to be used for DataBinding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Set the layoutId to be inflated
        int layoutId = -1;
        switch (viewType) {

            case AREA_VIEW_TYPE:            layoutId = R.layout.list_item_area;
                break;

            case PLACE_VIEW_TYPE:           layoutId = R.layout.list_item_place_nav;
                break;

            case SEARCH_MORE_VIEW_TYPE:     layoutId = R.layout.list_item_search_more;
                break;

            case ATTRIBUTION_VIEW_TYPE:     layoutId = R.layout.list_item_google_attribution;
                break;
        }

        // Inflate the View
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);

        return new AreaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(AreaViewHolder holder, int position) {
        if (position != mSortedList.size() || mExtraItemType != SEARCH_MORE) {
            holder.bind(position);
        }
    }

    @Override
    public int getItemViewType(int position) {

        // Handle the ViewType for the extra list item
        if (position == mSortedList.size()) {
            if (mExtraItemType == SEARCH_MORE) {
                return SEARCH_MORE_VIEW_TYPE;
            } else {
                return ATTRIBUTION_VIEW_TYPE;
            }
        }

        // Handle the ViewType for any item in mSortedList
        Object object = mSortedList.get(position);

        if (object instanceof Area) {
            return AREA_VIEW_TYPE;
        } else if (object instanceof PlaceModel) {
            return PLACE_VIEW_TYPE;
        }

        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        if (mHideAdapter) return 0;

        if (mExtraItemType == HIDDEN) return mSortedList.size();

        return mSortedList.size() + 1;
    }

    /**
     * Sets the List of items to be displayed by the Adapter
     *
     * @param sortableList    List of Items to be displayed in the Adapter
     */
    public void setAdapterItems(List<? extends AreaAdapterSortable> sortableList) {

        // Start batch operation
        mSortedList.beginBatchedUpdates();

        // Remove any items from the Adapter that are not in the sortableList
        for (int i = mSortedList.size() - 1; i >= 0; i--) {
            if (!sortableList.contains(mSortedList.get(i))) {
                mSortedList.removeItemAt(i);
            }
        }

        // Add all items from the sortableList to the Adapter
        for (AreaAdapterSortable sortable : sortableList) {
            if (mSortedList.indexOf(sortable) == SortedList.INVALID_POSITION) {
                mSortedList.add(sortable);
            }
        }

        // End the batch operation
        mSortedList.endBatchedUpdates();
    }

    /**
     * Removes all items from the Adapter
     */
    public void clear() {

        setExtraItemType(HIDDEN);
        mSortedList.clear();
    }

    /**
     * Hides the Adapter, but keeps the items in the Adapter so they can be re-displayed later in
     * {@link #show()}
     */
    public void hide() {
        mHideAdapter = true;

        notifyDataSetChanged();
    }

    /**
     * Shows the Adapter and any items that were in place before {@link #hide()} was called
     */
    public void show() {
        mHideAdapter = false;

        notifyDataSetChanged();
    }

    /**
     * Sets the layout for the extra item in the Adapter that is not a data model to be displayed.
     * This can be used to:
     *
     *      Hide the extra list item in the case that the search box is not
     *      in focus
     *
     *      Show the list item to allow the user to search for more items using the Google Places
     *      API
     *
     *      Show the attribution bar (with or without the Google attribution logo)
     *
     *      Show the attribution bar (with or without the Google attribution logo) with a progress
     *      bar to indicate an ongoing search
     *
     * @param type    The type of layout to be used
     */
    public void setExtraItemType(@ExtraListItemType int type) {
        mExtraItemType = type;

        if (mExtraItemType == HIDDEN) {
            notifyItemRemoved(mSortedList.size());
        } else {
            notifyItemChanged(mSortedList.size());
        }
    }

    class AreaViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // ** Constants ** //
        private ViewDataBinding mBinding;

        AreaViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;

            // Add the ClickListener for any item that is not the attribution bar
            if (!(mBinding instanceof ListItemGoogleAttributionBinding)) {
                mBinding.getRoot().setOnClickListener(this);
            }
        }

        void bind(int position) {

            // Check whether the position corresponds to an Object in mSortedList
            if (position == mSortedList.size() && mExtraItemType != SEARCH_MORE) {

                // Handle Attribution Bar layout
                AttributionViewModel vm = new AttributionViewModel();

                switch (mExtraItemType) {

                    case ExtraListItemType.ATTRIBUTION_GOOGLE_PROGRESS:
                        vm.setShowProgress(true);

                    case ExtraListItemType.ATTRIBUTION_GOOGLE:
                        vm.setShowAttribution(true);
                        break;

                    case ExtraListItemType.ATTRIBUTION_PROGRESS:
                        vm.setShowProgress(true);

                    case ExtraListItemType.ATTRIBUTION:
                        vm.setShowAttribution(false);
                        break;
                }

                ((ListItemGoogleAttributionBinding) mBinding).setVm(vm);;
            } else {

                // Handle layout for any item in mSortedList
                Object object = mSortedList.get(position);

                if (object instanceof Area) {
                    AreaViewModel vm = new AreaViewModel((Area) object, mViewModel);

                    ((ListItemAreaBinding) mBinding).setVm(vm);
                } else {
                    PlaceViewModel vm = new PlaceViewModel((PlaceModel) object, mViewModel);

                    ((ListItemPlaceNavBinding) mBinding).setVm(vm);
                }
            }
        }

        @Override
        public void onClick(View view) {

            // Get the position of the clicked ViewHolder in the Adapter
            int position = getAdapterPosition();

            Object object = null;

            // Set the Object to the corresponding Object in mSortedList if valid
            if (position < mSortedList.size()) {
                object = mSortedList.get(position);
            }

            mClickHandler.onClick(object);
        }
    }
}
