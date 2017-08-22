package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemAddRatingBinding;
import project.hikerguide.databinding.ListItemAuthorBinding;
import project.hikerguide.databinding.ListItemGuideDetailsBinding;
import project.hikerguide.databinding.ListItemRatingBinding;
import project.hikerguide.databinding.ListItemSectionImageBinding;
import project.hikerguide.databinding.ListItemSectionTextBinding;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Rating;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.viewmodels.AuthorViewModel;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.models.viewmodels.RatingViewModel;
import project.hikerguide.models.viewmodels.SectionViewModel;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 7/22/2017.
 */

public class GuideDetailsAdapter extends RecyclerView.Adapter<GuideDetailsAdapter.GuideDetailsViewHolder>{
    // ** Constants ** //
    private static final int GUIDE_VIEW_TYPE            = 0;
    private static final int SECTION_VIEW_TYPE          = 1;
    private static final int SECTION_IMAGE_VIEW_TYPE    = 2;
    private static final int AUTHOR_VIEW_TYPE           = 3;
    private static final int RATING_VIEW_TYPE           = 4;
    private static final int ADD_RATING_VIEW_TYPE       = 5;

    // ** Member Variables ** //
    private Guide mGuide;
    private Section[] mSections;
    private Author mAuthor;
    private Rating[] mRatings;
    private Author mUser;

    private Object[] mModels;

    private MapboxActivity mActivity;

    private ClickHandler mClickHandler;

    public GuideDetailsAdapter(ClickHandler clickHandler) {
        mClickHandler = clickHandler;

        // If User is logged in, get the Author data model that represents them
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {

                    mUser = (Author) model;
                    updateModels();
                }
            });
        }
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

             case RATING_VIEW_TYPE:
                 layoutId = R.layout.list_item_rating;
                 break;

             case ADD_RATING_VIEW_TYPE:
                 layoutId = R.layout.list_item_add_rating;
                 break;

             default: throw new UnsupportedOperationException("Unknown view type: " + viewType);
         }

         ViewDataBinding binding = DataBindingUtil.inflate(inflater, layoutId, parent, false);
        return new GuideDetailsViewHolder(binding, this);
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

        Object model = mModels[position];

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
        } else if (model instanceof Rating) {

            // For adding a new rating or editing a user's previous rating
            if (((Rating) model).getAuthorId() == null) {
                return ADD_RATING_VIEW_TYPE;
            } else {
                return RATING_VIEW_TYPE;
            }
        }

        return super.getItemViewType(position);
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
        if (mGuide == null || mSections == null || mAuthor == null) {
            return;
        }

        if (mGuide.raters != null && mGuide.raters.size() > 0) {
            mRatings = mGuide.getRatings();
        }

        // Check if there are Ratings to be added to the Adapter
        if (mUser != null && !mGuide.authorId.equals(mUser.firebaseId)) {

            if (mGuide.raters != null && mGuide.raters.containsKey(mUser.firebaseId)) {

                mModels = new Object[mSections.length + mRatings.length + 2];

                // Iterate through the Ratings and find the User's rating and place it at the top
                for (int i = 0; i < mRatings.length; i++) {
                    Rating rating = mRatings[i];

                    if (rating.getAuthorId().equals(mUser.firebaseId)) {

                        // Set the first review to be viewed to the user's own ratin
                        mModels[mSections.length + 2] = rating;

                        // Copy the rest of the ratings after the user's rating
                        if (i != 0) {
                            System.arraycopy(mRatings, 0, mModels, mSections.length + 3, i);
                        }

                        if (i < mRatings.length - 1) {
                            System.arraycopy(mRatings, i + 1, mModels, mSections.length + 3, mRatings.length - i - 1);
                        }

                        break;
                    }
                }

            } else {
                if (mRatings == null) {
                    // Create a new Array for mRatings
                    mRatings = new Rating[0];
                }

                mModels = new Object[mSections.length + mRatings.length + 3];

                // Create a new Rating to allow the User to rate the Guide
                mModels[mModels.length - 1] = new Rating();

                // Copy the rest of the Ratings after the new Rating
                System.arraycopy(mRatings, 0, mModels, mSections.length + 2, mRatings.length);
            }
        } else if (mRatings != null) {
            mModels = new Object[mSections.length + mRatings.length + 2];

            // Copy the Ratings to the end of the new Array if the user is not logged in
            System.arraycopy(mRatings, 0, mModels, mSections.length + 2, mRatings.length);

        } else {

            mModels = new Object[mSections.length + 2];
        }

        // First item is the Guide's details
        mModels[0] = mGuide;

        // Copy the mSections Array into the middle sections of mModels
        System.arraycopy(mSections, 0, mModels, 1, mSections.length);

        // Set Author to come after Sections
        mModels[mSections.length + 1] = mAuthor;

        notifyDataSetChanged();
    }

    /**
     * Forces the Adapter to re-load its data
     */
    public void updateRating() {

        updateModels();
    }

    public interface ClickHandler {
        void onClickAuthor(Author author);
    }

    public class GuideDetailsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ViewDataBinding mBinding;
        private GuideDetailsAdapter mAdapter;

        public GuideDetailsViewHolder(ViewDataBinding binding, GuideDetailsAdapter adapter) {
            super(binding.getRoot());

            mBinding = binding;
            mAdapter = adapter;

            if (mBinding instanceof ListItemAuthorBinding) {
                mBinding.getRoot().setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            Author author = (Author) mModels[position];
            mClickHandler.onClickAuthor(author);
        }

        public void bind(int position) {
            Object model = mModels[position];

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
            } else if (model instanceof Rating) {

                if (((Rating) model).getAuthorId() == null) {

                    // If the Rating does not contain an AuthorId, it is a new Rating for the user
                    // to rate the Guide with
                    RatingViewModel vm = new RatingViewModel((Rating) model, mGuide, mAdapter, mUser);

                    ((ListItemAddRatingBinding) mBinding).setVm(vm);
                } else {

                    RatingViewModel vm = new RatingViewModel((Rating) model, mGuide, mAdapter);

                    ((ListItemRatingBinding) mBinding).setVm(vm);
                }
            }

            // Immediately bind the data into the Views
            mBinding.executePendingBindings();
        }
    }
}
