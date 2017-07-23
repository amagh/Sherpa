package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAuthorBinding;
import project.hikerguide.databinding.ListItemGuideDetailsBinding;
import project.hikerguide.databinding.ListItemSectionImageBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.AuthorViewModel;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.models.viewmodels.SectionViewModel;

/**
 * Created by Alvin on 7/22/2017.
 */

public class GuideDetailsAdapter extends RecyclerView.Adapter {
    // ** Constants ** //
    private static final int GUIDE_VIEW_TYPE = 0;
    private static final int SECTION_VIEW_TYPE = 1;
    private static final int SECTION_IMAGE_VIEW_TYPE = 2;
    private static final int AUTHOR_VIEW_TYPE = 3;

    // ** Member Variables ** //
    private BaseModel[] mModels;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutId;

         switch (viewType) {
             case GUIDE_VIEW_TYPE:
                 layoutId = R.layout.list_item_guide_details;
                 break;

             case SECTION_VIEW_TYPE:
                 layoutId = R.layout.list_item_section_text;
                 break;

             case SECTION_IMAGE_VIEW_TYPE:
                 layoutId = R.layout.list_item_section_image;
                 break;

             case AUTHOR_VIEW_TYPE:
                 layoutId = R.layout.list_item_author;
                 break;

             default: throw new UnsupportedOperationException("Unknown view type: " + viewType);
         }

        return new GuideDetailsViewHolder(DataBindingUtil.inflate(inflater, layoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        if (mModels != null) {
            // There should be as many items to load as there are data models in mModels
            return mModels.length;
        }

        return 0;
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0) {
            // The first item is reserved for the guide details
            return GUIDE_VIEW_TYPE;
        } else if (position == mModels.length) {
            // The last item is for the author's details
            return AUTHOR_VIEW_TYPE;
        } else if (!((Section) mModels[position]).hasImage){
            // If the Section does not have an image, inflate the layout without an image
            return SECTION_VIEW_TYPE;
        } else {
            // Otherwise, the Section does have an image and requires the alternate layout
            return SECTION_IMAGE_VIEW_TYPE;
        }
    }

    public void swapModels(BaseModel[] models) {
        mModels = models;

        notifyDataSetChanged();
    }

    public class GuideDetailsViewHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding mBinding;

        public GuideDetailsViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
        }

        public void bind(int position) {
            BaseModel model = mModels[position];

            // Load the correct ViewModel into the ViewDataBinding based on the type of model for
            // the position
            if (model instanceof Guide) {
                ((ListItemGuideDetailsBinding) mBinding).setVm(
                        new GuideViewModel(mBinding.getRoot().getContext(), (Guide) model));
            } else if (model instanceof Section) {
                ((ListItemSectionImageBinding) mBinding).setVm(
                        new SectionViewModel((Section) model));
            } else if (model instanceof Author) {
                ((ListItemAuthorBinding) mBinding).setVm(
                        new AuthorViewModel((Author) model));
            }

            // Immediately bind the data into the Views
            mBinding.executePendingBindings();
        }
    }
}
