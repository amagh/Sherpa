package project.sherpa.ui.behaviors;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;

/**
 * Created by Alvin on 8/1/2017.
 */

public class VanishingBehavior extends CoordinatorLayout.Behavior<View> {

    public VanishingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof Toolbar;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {

        if (-dependency.getY() - child.getY() >= child.getHeight()) {
            AdditiveAnimator.animate(child).setDuration(200)
                    .scale(0)
                    .alpha(0)
                    .start();
        } else {
            AdditiveAnimator.animate(child).setDuration(200)
                    .scale(1)
                    .alpha(1)
                    .start();
        }

        return true;
    }
}
