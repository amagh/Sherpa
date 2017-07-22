package project.hikerguide.sync;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Guide;

/**
 * Created by Alvin on 7/21/2017.
 */

public class SyncGuidesTaskLoader extends AsyncTaskLoader<Guide[]> {
    public SyncGuidesTaskLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        // Begin loading in background
        forceLoad();
    }

    @Override
    public Guide[] loadInBackground() {
        // Generate an Array of Guides using the DatabaseProvider
        return DatabaseProvider.getInstance().getRecentGuides();
    }
}
