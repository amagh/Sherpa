package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAuthorBinding;
import project.hikerguide.databinding.ListItemGuideDetailsBinding;
import project.hikerguide.databinding.ListItemSectionImageBinding;
import project.hikerguide.databinding.ListItemSectionTextBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.AuthorViewModel;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.models.viewmodels.SectionViewModel;
import project.hikerguide.ui.activities.MapboxActivity;

/**
 * Created by Alvin on 7/22/2017.
 */

public class GuideDetailsAdapter extends RecyclerView.Adapter<GuideDetailsAdapter.GuideDetailsViewHolder>{
    // ** Constants ** //
    private static final int GUIDE_VIEW_TYPE = 0;
    private static final int SECTION_VIEW_TYPE = 1;
    private static final int SECTION_IMAGE_VIEW_TYPE = 2;
    private static final int AUTHOR_VIEW_TYPE = 3;

    // ** Member Variables ** //
    private Guide mGuide;
    private Section[] mSections;
    private Author mAuthor;

    private BaseModel[] mModels;

    private MapboxActivity mActivity;

    @Override
    public GuideDetailsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout based on the viewType
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
    public void onBindViewHolder(GuideDetailsViewHolder holder, int position) {
        // Bind the data to the Views
        holder.bind(position);
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
        } else if (position == mModels.length - 1) {
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

    /**
     * Sets the Guide whose details are to be shown
     *
     * @param guide       Guide whose details need to be shown
     * @param activity    MapboxActivity so that the MapView can follow its lifecycle
     */
    public void setGuide(Guide guide, @NonNull MapboxActivity activity) {
        mGuide = guide;
        mActivity = activity;

        updateModels();
    }

    /**
     * Sets the Sections to display
     *
     * @param sections    An Array of Sections corresponding to the Guide to display
     */
    public void setSections(Section[] sections) {
        mSections = sections;

        updateModels();
    }

    /**
     * Sets the Author of the Guide to be displayed
     *
     * @param author    Author of the Guide
     */
    public void setAuthor(Author author) {
        mAuthor = author;

        updateModels();
    }

    /**
     * Creates the Array that will actually be used by the Adapter to create ViewHolders from
     */
    private void updateModels() {

        // Check to ensure all required items are present
        if (mGuide != null && mSections != null && mAuthor != null) {

            // Create a new Array 2 larger than the length of mSections to leave room for the Guide
            // details and Author
            mModels = new BaseModel[mSections.length + 2];

            // First item is the Guide's details
            mModels[0] = mGuide;

            // Copy the mSections Array into the middle sections of mModels
            System.arraycopy(mSections, 0, mModels, 1, mSections.length);

            // Set the last item to the Author
            mModels[mModels.length - 1] = mAuthor;

            notifyDataSetChanged();
        }
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
                        // Pass in the MapboxActivity for lifecycle purposes
                        new GuideViewModel(mActivity, (Guide) model));
            } else if (model instanceof Section) {
                // Load the correct ViewDataBinding depending on whether the section has an image
                if (((Section) mModels[position]).hasImage) {
                    ((ListItemSectionImageBinding) mBinding).setVm(
                            new SectionViewModel((Section) model));
                } else {
                    ((ListItemSectionTextBinding) mBinding).setVm(
                            new SectionViewModel((Section) model));
                }
            } else if (model instanceof Author) {
                ((ListItemAuthorBinding) mBinding).setVm(
                        new AuthorViewModel(mActivity, (Author) model));
            }

            // Immediately bind the data into the Views
            mBinding.executePendingBindings();
        }
    }
}
