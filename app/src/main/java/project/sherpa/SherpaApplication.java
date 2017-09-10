package project.sherpa;

import android.support.multidex.BuildConfig;
import android.support.multidex.MultiDexApplication;

import timber.log.Timber;

/**
 * Created by Alvin on 7/21/2017.
 */

public class SherpaApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
