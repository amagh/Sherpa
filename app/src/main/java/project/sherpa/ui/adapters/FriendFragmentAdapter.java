package project.sherpa.ui.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;

import java.lang.ref.WeakReference;

import project.sherpa.R;
import project.sherpa.ui.fragments.FollowingFragment;
import project.sherpa.ui.fragments.FriendFragment;
import project.sherpa.ui.fragments.RequestFragment;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FriendFragmentAdapter extends FragmentPagerAdapter {

    // ** Member Variables ** //
    private SparseArray<Fragment> fragmentArray = new SparseArray<>();
    private WeakReference<Context> mContext;

    public FriendFragmentAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = new WeakReference<>(context);
    }

    @Override
    public Fragment getItem(int position) {

        // Retrieve the Fragment from the SparseArray
        Fragment fragment = fragmentArray.get(position);

        // Instantiate the Fragment if it is null
        if (fragment == null) {
            switch (position) {
                case 0: fragmentArray.put(position, new FriendFragment());
                    break;
                case 1: fragmentArray.put(position, new FollowingFragment());
                    break;
                case 2: fragmentArray.put(position, new RequestFragment());
                    break;
            }

            return getItem(position);
        } else {
            return fragment;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.get().getString(R.string.page_title_friends);
            case 1:
                return mContext.get().getString(R.string.page_title_following);
            case 2:
                return mContext.get().getString(R.string.page_title_requests);
        }

        return super.getPageTitle(position);
    }
}
