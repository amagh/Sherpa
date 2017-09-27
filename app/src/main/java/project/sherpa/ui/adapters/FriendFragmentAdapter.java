package project.sherpa.ui.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import project.sherpa.ui.fragments.FollowingFragment;
import project.sherpa.ui.fragments.FriendFragment;
import project.sherpa.ui.fragments.RequestFragment;

/**
 * Created by Alvin on 9/26/2017.
 */

public class FriendFragmentAdapter extends FragmentPagerAdapter {

    public FriendFragmentAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new FriendFragment();
            case 1:
                return new FollowingFragment();
            case 2:
                return new RequestFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Friends";
            case 1:
                return "Following";
            case 2:
                return "Friend Requests";
        }

        return super.getPageTitle(position);
    }
}
