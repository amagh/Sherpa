package project.hikerguide;

import android.support.multidex.MultiDexApplication;

import timber.log.Timber;

/**
 * Created by Alvin on 7/21/2017.
 */

public class HikerGuideApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
