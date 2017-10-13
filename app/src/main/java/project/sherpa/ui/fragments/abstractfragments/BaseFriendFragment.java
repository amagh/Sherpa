package project.sherpa.ui.fragments.abstractfragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.sherpa.R;
import project.sherpa.databinding.FragmentFriendBinding;
import project.sherpa.models.datamodels.Author;

/**
 * Created by Alvin on 9/26/2017.
 */

public abstract class BaseFriendFragment extends ConnectivityFragment {

    // ** Member Variables ** //
    protected FragmentFriendBinding mBinding;
    protected Author mUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_friend, container, false);
        initRecyclerView();

        return mBinding.getRoot();
    }

    /**
     * Initializes the RecyclerView and its components
     */
    protected abstract void initRecyclerView();

    /**
     * Called when the Author is updated
     */
    public abstract void onAuthorChanged(Author user);

    /**
     * Hides the ProgressBar
     */
    protected void hideProgressBar() {
        mBinding.friendPb.setVisibility(View.GONE);
    }
}
