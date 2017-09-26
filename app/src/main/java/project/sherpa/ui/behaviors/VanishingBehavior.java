package project.sherpa.ui.behaviors;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import at.wirecube.additiveanimations.additive_animator.AdditiveAnimator;
import project.sherpa.R;
import timber.log.Timber;

/**
 * Created by Alvin on 8/1/2017.
 */

public class VanishingBehavior extends CoordinatorLayout.Behavior<View> {

    public VanishingBehavior() {
    }

    public VanishingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {

        float alpha;

        // Hide the Views when the AppBarLayout is fully contracted
        if (((int) parent.findViewById(R.id.user_ab).getY()) ==
                -((parent.findViewById(R.id.user_ab).getHeight() - parent.findViewById(R.id.toolbar).getHeight()))) {
            alpha = 0;
        } else {
            alpha = 1;
        }

        if (child instanceof ConstraintLayout) {
            ConstraintLayout layout = ((ConstraintLayout) child);

            // Set the height of the social buttons - This is necessary because of some weird bug
            // causing the layout to move around if the user scrolls and lets go, letting the
            // inertia of the scroll take over.
            layout.setY(dependency.getY() + dependency.getHeight() - (layout.getHeight() / 2));

            for (int i = 0; i < layout.getChildCount(); i++) {
                View view = layout.getChildAt(i);

                AdditiveAnimator.animate(view).setDuration(200)
                        .scale(alpha)
                        .alpha(alpha)
                        .start();
            }
        } else {
            AdditiveAnimator.animate(child).setDuration(200)
                    .scale(alpha)
                    .alpha(alpha)
                    .start();
        }
        return true;
    }
}
