package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAuthorDetailsBinding;
import project.hikerguide.databinding.ListItemGuideCompactBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.AuthorViewModel;
import project.hikerguide.models.viewmodels.GuideViewModel;

/**
 * Created by Alvin on 8/1/2017.
 */

public class AuthorDetailsAdapter extends RecyclerView.Adapter<AuthorDetailsAdapter.AuthorDetailsViewHolder> {
    // ** Constants ** //
    private static final int AUTHOR_VIEW_TYPE   = 0;
    private static final int GUIDE_VIEW_TYPE    = 1;

    // ** Member Variables ** //
    private List<BaseModel> mModelList;
    private boolean mEnableEdit = false;
    private boolean mIsInEditMode = false;

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

            case GUIDE_VIEW_TYPE:
                layoutId = R.layout.list_item_guide_compact;
                break;
        }

        // Inflate the View using DataBindingUtil
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new AuthorDetailsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(AuthorDetailsViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        if (mModelList != null) {
            return mModelList.size();
        }

        return 0;
    }

    @Override
    public int getItemViewType(int position) {

        // Return ViewType based on the type of Model in the position
        BaseModel model = mModelList.get(position);

        if (model instanceof Author) {
            return AUTHOR_VIEW_TYPE;
        } else if (model instanceof Guide) {
            return GUIDE_VIEW_TYPE;
        } else {
            return super.getItemViewType(position);
        }

    }

    /**
     * Sets the List of Models to be displayed by the Adapter
     *
     * @param modelList    List of Models to be used as a source of data
     */
    public void setModelList(List<BaseModel> modelList) {

        // Set List in signature to the memvar
        mModelList = modelList;

        notifyDataSetChanged();
    }

    /**
     * Adds a single Model to the end of the List of Models to display
     *
     * @param model    Model to add to the List
     */
    public void addModel(BaseModel model) {
        mModelList.add(model);

        notifyItemInserted(mModelList.size() - 1);
    }

    public void enableEditing() {
        mEnableEdit = true;
    }

    class AuthorDetailsViewHolder extends RecyclerView.ViewHolder {
        // ** Member Variables ** //
        private ViewDataBinding mBinding;

        public AuthorDetailsViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
        }

        public void bind(int position) {

            // Get reference to the Model to be displayed
            BaseModel model = mModelList.get(position);

            // Cast the Model and ViewDataBinding based on the type of BaseModel
            if (model instanceof Author) {
                AuthorViewModel vm = new AuthorViewModel(mBinding.getRoot().getContext(), (Author) model);

                if (mEnableEdit) {
                    // Enable editing of info if user is viewing their own profile
                    vm.enableEditing();
                }

                ((ListItemAuthorDetailsBinding) mBinding).setVm(vm);
            } else if (model instanceof Guide) {
                GuideViewModel vm = new GuideViewModel(mBinding.getRoot().getContext(), (Guide) model);
            }
        }
    }
}
