package project.hikerguide.ui.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by Alvin on 8/7/2017.
 */

public class GuideDetailsFragmentAdapter extends FragmentPagerAdapter {
    // ** Member Variables ** //
    List<Fragment> mFragmentList;

    public GuideDetailsFragmentAdapter(FragmentManager manager) {
        super(manager);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        if (mFragmentList != null) {
            return mFragmentList.size();
        }

        return 0;
    }

    /**
     * Loads the Fragments that will be displayed in the ViewPagerAdapter
     *
     * @param fragmentList    List of Fragments to be displayed
     */
    public void swapFragmentList(List<Fragment> fragmentList) {

        // Set the member variable to the paramter
        mFragmentList = fragmentList;

        // Notify
        notifyDataSetChanged();
    }
}
