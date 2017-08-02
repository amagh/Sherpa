package project.hikerguide.ui.behaviors;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import timber.log.Timber;

import static android.support.v4.view.ViewCompat.SCROLL_AXIS_VERTICAL;


/**
 * Created by Alvin on 8/1/2017.
 */

public class FABScrollBehavior extends FloatingActionButton.Behavior {

    public FABScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);

        return true;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, final FloatingActionButton child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

        // Check the direction being scrolled
        if (dyConsumed > 0) {

            // Hide the FAB
            child.hide();

            // Show the FAB again after 1 second
            child.postDelayed(new Runnable() {
                @Override
                public void run() {

                    child.show();
                }
            }, 1000);
        } else {
            // Scrolling up, show the FAB
            child.show();
        }
    }


}
