package project.sherpa.ui.fragments.abstractfragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import project.sherpa.R;
import project.sherpa.databinding.FragmentFriendBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.ui.adapters.FriendAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.fragments.ConnectivityFragment;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;

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
        loadUser();

        return mBinding.getRoot();
    }

    /**
     * Initializes the RecyclerView and its components
     */
    protected abstract void initRecyclerView();

    /**
     * Loads the user's profile
     */
    protected void loadUser() {

        // Attempt to retrieve the user from the cache
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        mUser = (Author) DataCache.getInstance().get(user.getUid());

        if (mUser == null) {

            // User's profile was not stored in cache. Load it from Firebase
            FirebaseProviderUtils.getAuthorForFirebaseUser(new FirebaseProviderUtils.FirebaseListener() {
                @Override
                public void onModelReady(BaseModel model) {
                    if (model == null) return;

                    // Store the user in cache and recursively call the loadUser function
                    DataCache.getInstance().store(model);
                    loadUser();
                }
            });
        }
    }
}
