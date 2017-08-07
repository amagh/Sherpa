package project.hikerguide.ui.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Alvin on 8/7/2017.
 */

public class TogglableViewPager extends ViewPager {
    // ** Member Variables ** //
    boolean mSwipe = true;

    public TogglableViewPager(Context context) {
        super(context);
    }

    public TogglableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mSwipe && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mSwipe && super.onTouchEvent(ev);
    }

    /**
     * Sets whether the ViewPager should allow swiping to change tabs
     *
     * @param swipe    Boolean value for whether swiping should be allowed
     */
    public void setSwipe(boolean swipe) {
        mSwipe = swipe;
    }
}
