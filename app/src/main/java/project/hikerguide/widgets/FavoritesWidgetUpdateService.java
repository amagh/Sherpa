package project.hikerguide.widgets;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import project.hikerguide.R;

/**
 * Created by Alvin on 8/30/2017.
 */

public class FavoritesWidgetUpdateService extends IntentService {

    public FavoritesWidgetUpdateService() {
        super("FavoritesWidgetUpdateService");
    }

    /**
     * Initiates the Service to begin updating Widgets
     *
     * @param context    Interface to global Context
     */
    public static void updateWidgets(Context context) {

        // Generate the Intent and start the Service
        Intent intent = new Intent(context, FavoritesWidgetUpdateService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {

            // Update the Widgets
            handleUpdateFavoritesWidget();
        }
    }

    /**
     * Updates the Widgets to reflect changes in favorite'd guides
     */
    private void handleUpdateFavoritesWidget() {

        // Initialize variables required to update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, FavoritesWidget.class));

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_lv);

        for (int appWidgetId : appWidgetIds) {
            // Update widgets
            FavoritesWidget.updateAppWidget(this, appWidgetManager, appWidgetId);
        }
    }
}

