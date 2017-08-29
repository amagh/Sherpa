package project.hikerguide.ui.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * Created by Alvin on 8/29/2017.
 */

public class AspectRatioImageView extends AppCompatImageView {

    private float mAspectRatio = -1;

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Sets the aspect ratio to be used by the ImageView
     *
     * @param aspectRatio    Aspect ratio to be set for the ImageView
     */
    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // Check whether the ImageView should obey an aspect ratio or if it should simply
        // wrap_content
        if (mAspectRatio == -1) {

            // No ratio set. wrap_content
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {

            // Ratio is set. Set the height based on the ratio and width of the ImageView
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * mAspectRatio);

            setMeasuredDimension(width, height);
        }
    }
}
