package project.hikerguide;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by Alvin on 7/21/2017.
 */

public class HikerGuideApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
