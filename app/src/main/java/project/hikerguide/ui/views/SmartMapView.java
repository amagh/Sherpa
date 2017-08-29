package project.hikerguide.ui.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;

import project.hikerguide.R;
import project.hikerguide.ui.activities.MapboxActivity;
import project.hikerguide.ui.fragments.MapboxFragment;

/**
 * Created by Alvin on 7/24/2017.
 */

public class SmartMapView extends MapView {
    private boolean mStarted = false;

    public SmartMapView(@NonNull Context context) {
        super(context);
    }

    public SmartMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SmartMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SmartMapView(@NonNull Context context, @Nullable MapboxMapOptions options) {
        super(context, options);
    }

    public void startMapView(MapboxActivity activity, Bundle savedInstanceState) {
        if (mStarted) {
            return;
        }

        mStarted = true;

        onCreate(savedInstanceState);

        setStyleUrl(activity.getString(R.string.outdoors_style));
        onStart();

        activity.attachMapView(this);
    }

    public void startMapView(MapboxFragment fragment, Bundle savedInstanceState) {
        if (mStarted) {
            return;
        }

        mStarted = true;

        onCreate(savedInstanceState);

        setStyleUrl(fragment.getString(R.string.outdoors_style));
        onStart();

        fragment.attachMapView(this);
    }
}
