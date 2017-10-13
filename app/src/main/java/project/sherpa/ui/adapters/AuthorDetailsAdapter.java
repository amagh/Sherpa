package project.sherpa.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import project.sherpa.R;
import project.sherpa.databinding.ListItemAuthorDetailsBinding;
import project.sherpa.databinding.ListItemAuthorDetailsEditBinding;
import project.sherpa.databinding.ListItemGuideBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.AuthorViewModel;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.models.viewmodels.ListItemAuthorDetailsEditViewModel;
import project.sherpa.models.viewmodels.ListItemFriendViewModel;
import project.sherpa.ui.adapters.interfaces.ClickHandler;

/**
 * Created by Alvin on 8/1/2017.
 */

public class AuthorDetailsAdapter extends RecyclerView.Adapter<AuthorDetailsAdapter.AuthorDetailsViewHolder> {
    // ** Constants ** //
    private static final int AUTHOR_VIEW_TYPE       = 0;
    private static final int AUTHOR_EDIT_VIEW_TYPE  = 1;
    private static final int GUIDE_VIEW_TYPE        = 2;

    // ** Member Variables ** //
    private boolean mInEditMode = false;
    private ClickHandler<Guide> mClickHandler;

    private Author mUser;
    private AuthorViewModel mAuthorViewModel;
    
    private SortedListAdapterCallback<BaseModel> mSortedAdapter = new SortedListAdapterCallback<BaseModel>(this) {
        @Override
        public int compare(BaseModel o1, BaseModel o2) {
            if (o1 instanceof Author) return -1;
            if (o2 instanceof Author) return 1;
            
            if (o1 instanceof Guide && o2 instanceof Guide) {
                return ((Guide) o1).rating > ((Guide) o2).rating
                        ? -1
                        : ((Guide) o1).rating < ((Guide) o2).rating
                        ? 1
                        : 0;
            }
            
            return 0;
        }

        @Override
        public boolean areContentsTheSame(BaseModel oldItem, BaseModel newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(BaseModel item1, BaseModel item2) {
            return item1.firebaseId.equals(item2.firebaseId);
        }
    };
    private SortedList<BaseModel> mSortedList = new SortedList<>(BaseModel.class, mSortedAdapter);

    public AuthorDetailsAdapter(ClickHandler<Guide> clickHandler) {
        mClickHandler = clickHandler;
    }

    @Override
    public AuthorDetailsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Init the LayoutInflater and layoutId that will be used for the ViewHolder
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId = -1;

        // Set the layoutId based on the ViewType
        switch (viewType) {
            case AUTHOR_VIEW_TYPE:
                layoutId = R.layout.list_item_author_details;
                break;

            case AUTHOR_EDIT_VIEW_TYPE:
                layoutId = R.layout.list_item_author_details_edit;
                break;

            case GUIDE_VIEW_TYPE:
                layoutId = R.layout.list_item_guide;
                break;
        }

        // Inflate the View using DataBindingUtil
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new AuthorDetailsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(AuthorDetailsViewHolder holder, int position) {

        if (position == 0) {
            StaggeredGridLayoutManager.LayoutParams params =
                    (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();

            params.setFullSpan(true);
        }

        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mSortedList.size();
    }

    @Override
    public int getItemViewType(int position) {

        // Return ViewType based on the type of Model in the position
        BaseModel model = mSortedList.get(position);

        if (model instanceof Author) {
            if (!mInEditMode) {
                return AUTHOR_VIEW_TYPE;
            } else {
                return AUTHOR_EDIT_VIEW_TYPE;
            }
        } else if (model instanceof Guide) {
            return GUIDE_VIEW_TYPE;
        } else {
            return super.getItemViewType(position);
        }

    }

    public void setAuthorViewModel(AuthorViewModel authorViewModel) {
        mAuthorViewModel = authorViewModel;
        notifyItemChanged(0);
    }

    /**
     * Sets the List of Models to be displayed by the Adapter
     *
     * @param modelList    List of Models to be used as a source of data
     */
    public void setModelList(List<BaseModel> modelList) {

        // Add all items from modelList to the Adapter
        mSortedList.addAll(modelList);
    }

    /**
     * Adds a single Model to the end of the List of Models to display
     *
     * @param model    Model to add to the List
     */
    public void addModel(BaseModel model) {
        mSortedList.add(model);
    }

    /**
     * Switches the layout used for the Author BaseModel between one for display or one for editing.
     */
    public void switchAuthorLayout() {
        mInEditMode = !mInEditMode;

        notifyItemChanged(0);
    }

    /**
     * Sets the Firebase User's profile to the Adapter's field so that it can be loaded to each
     * GuideViewModel to check whether that Guide has been favorite'd
     *
     * @param user    Author's whose favorites are to be synced to the Adapter's loaded Guides
     */
    public void setUser(Author user) {
        mUser = user;

        notifyDataSetChanged();
    }

    class AuthorDetailsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // ** Member Variables ** //
        private ViewDataBinding mBinding;

        public AuthorDetailsViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;

            if (mBinding instanceof ListItemGuideBinding) {
                mBinding.getRoot().setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            Guide guide = (Guide) mSortedList.get(position);

            mClickHandler.onClick(guide);
        }

        /**
         * Binds the data from the BaseModel to the ViewModel referenced by the View
         *
         * @param position    The position of the ViewHolder binding the data
         */
        public void bind(int position) {

            // Get reference to the Model to be displayed
            BaseModel model = mSortedList.get(position);

            // Cast the Model and ViewDataBinding based on the type of BaseModel
            if (model instanceof Author) {

                if (!mInEditMode) {
                    ((ListItemAuthorDetailsBinding) mBinding).setVm(mAuthorViewModel);
                } else {
                    ListItemAuthorDetailsEditViewModel evm = new ListItemAuthorDetailsEditViewModel(
                                    (AppCompatActivity) mBinding.getRoot().getContext(),
                                    (Author) model);

                    ((ListItemAuthorDetailsEditBinding) mBinding).setEvm(evm);
                    ((ListItemAuthorDetailsEditBinding) mBinding).setVm(mAuthorViewModel);
                }
            } else if (model instanceof Guide) {
                GuideViewModel vm = new GuideViewModel(mBinding.getRoot().getContext(), (Guide) model);

                if (mUser != null) {

                    // Add the Author to the ViewModel for checking favorites
                    vm.setAuthor(mUser);
                }

                ((ListItemGuideBinding) mBinding).top.setVm(vm);
                ((ListItemGuideBinding) mBinding).bottom.setVm(vm);
            }
        }
    }
}
