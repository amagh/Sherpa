package project.hikerguide.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;

/**
 * Created by Alvin on 7/22/2017.
 */

public class SyncGuideDetailsTaskLoader extends AsyncTaskLoader<Section[]> {
    private Guide mGuide;

    public SyncGuideDetailsTaskLoader(@NonNull Context context, @NonNull Guide guide) {
        super(context);

        // Get a reference to the Guide to pull the Sections for
        mGuide = guide;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        // Begin loading in background
        forceLoad();
    }

    @Override
    public Section[] loadInBackground() {
        // Generate an Array of Sections to return by passing the Guide into the provider's function
        DatabaseProvider provider = DatabaseProvider.getInstance();
        return provider.getSectionsForGuide(mGuide);
    }
}
