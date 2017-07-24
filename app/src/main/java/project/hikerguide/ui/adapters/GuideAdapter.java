package project.hikerguide.ui.adapters;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.hikerguide.R;
import project.hikerguide.databinding.ListItemGuideBinding;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.viewmodels.GuideViewModel;

/**
 * Created by Alvin on 7/21/2017.
 */

public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideViewHolder> {
    // ** Member Variables ** //
    private Guide[] mGuides;
    private ClickHandler mHandler;

    public GuideAdapter(ClickHandler clickHandler) {
        mHandler = clickHandler;
    }

    @Override
    public GuideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        ListItemGuideBinding binding = DataBindingUtil.inflate(inflater, R.layout.list_item_guide, parent, false);
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
     * For passing information about the clicked guide to the Activity/Fragment
     */
    public interface ClickHandler {
        void onGuideClicked(Guide guide);
    }

    public class GuideViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // ** Member Variables ** //
        ListItemGuideBinding mBinding;

        public GuideViewHolder(ListItemGuideBinding binding) {
            super(binding.getRoot());

            mBinding = binding;
            mBinding.getRoot().setOnClickListener(this);
        }

        public void bind(int position) {
            // Get the guide from the correlated position
            Guide guide = mGuides[position];

            // Initialize a GuideViewModel using the Guide from the array and set it to the
            // DataBinding
            mBinding.setVm(new GuideViewModel(mBinding.getRoot().getContext(), guide));
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
