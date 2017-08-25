package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

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
import project.hikerguide.utilities.FirebaseProviderUtils;

/**
 * Created by Alvin on 7/22/2017.
 */

public class GuideDetailsAdapter extends RecyclerView.Adapter<GuideDetailsAdapter.GuideDetailsViewHolder> {

    // ** Constants ** //
    private static final int GUIDE_VIEW_TYPE            = 0;
    private static final int SECTION_VIEW_TYPE          = 1;
    private static final int SECTION_IMAGE_VIEW_TYPE    = 2;
    private static final int AUTHOR_VIEW_TYPE           = 3;

    // ** Member Variables ** //
    private SortedList.Callback<BaseModel> mSortedListCallback = new SortedList.Callback<BaseModel>() {
        @Override
        public int compare(BaseModel o1, BaseModel o2) {

            // Create a List of Classes this Adapter accepts to use as an index
            List<Class> classList = new ArrayList<>();
            classList.add(Guide.class);
            classList.add(Section.class);
            classList.add(Author.class);

            // Compare the items
            if (o1.getClass().equals(Section.class) && o2.getClass().equals(Section.class)) {

                // If both item are Sections, they are compared by their section numbering
                return ((Section) o1).section < ((Section) o2).section
                        ? -1
                        : ((Section) o1).section > ((Section) o2).section
                        ? 1
                        : 0;
            } else {

                // Compare them using their index in the classList
                return classList.indexOf(o1.getClass()) < classList.indexOf(o2.getClass())
                        ? -1
                        : classList.indexOf(o1.getClass()) > classList.indexOf(o2.getClass())
                        ? 1
                        : 0;
            }
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(BaseModel oldItem, BaseModel newItem) {
            return oldItem.firebaseId.equals(newItem.firebaseId);
        }

        @Override
        public boolean areItemsTheSame(BaseModel item1, BaseModel item2) {
            return item1 == item2;
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemChanged(fromPosition, toPosition);
        }
    };
    private SortedList<BaseModel> mModelsList = new SortedList<>(BaseModel.class, mSortedListCallback);

    private MapboxActivity mActivity;
    private ClickHandler mClickHandler;

    public GuideDetailsAdapter(MapboxActivity activity, ClickHandler clickHandler) {
        mActivity = activity;
        mClickHandler = clickHandler;
    }

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

        ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new GuideDetailsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(GuideDetailsViewHolder holder, int position) {
        // Bind the data to the Views
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mModelsList.size();
    }

    @Override
    public int getItemViewType(int position) {

        Object model = mModelsList.get(position);

        // Return the ViewType based on the type of model in that position
        if (model instanceof Guide) {
            return GUIDE_VIEW_TYPE;
        } else if (model instanceof Author) {
            return AUTHOR_VIEW_TYPE;
        } else if (model instanceof Section) {
            if (((Section) model).hasImage) {
                return SECTION_IMAGE_VIEW_TYPE;
            } else {
                return SECTION_VIEW_TYPE;
            }
        }

        return super.getItemViewType(position);
    }

    /**
     * Adds a model to the Adapter to be displayed
     *
     * @param model    Model to be added to the Adapter
     */
    public void addModel(BaseModel model) {

        // Iterate through and check to see if the model is already in the Adapter
        for (int i = 0; i < mModelsList.size(); i++) {

            if (mModelsList.get(i).firebaseId.equals(model.firebaseId)) {

                // Model is in Adapter, do nothing
                return;
            }
        }

        // Add the Model to the Adapter
        mModelsList.add(model);
    }

    public interface ClickHandler {
        void onClickAuthor(Author author);
    }

    public class GuideDetailsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ViewDataBinding mBinding;

        public GuideDetailsViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());

            mBinding = binding;

            if (mBinding instanceof ListItemAuthorBinding) {
                mBinding.getRoot().setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            Author author = (Author) mModelsList.get(position);
            mClickHandler.onClickAuthor(author);
        }

        public void bind(int position) {
            Object model = mModelsList.get(position);

            // Load the correct ViewModel into the ViewDataBinding based on the type of model for
            // the position
            if (model instanceof Guide) {

                ((ListItemGuideDetailsBinding) mBinding).setVm(
                        // Pass in the MapboxActivity for lifecycle purposes
                        new GuideViewModel(mActivity, (Guide) model));
            } else if (model instanceof Section) {

                // Load the correct ViewDataBinding depending on whether the section has an image
                if (((Section) mModelsList.get(position)).hasImage) {

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
