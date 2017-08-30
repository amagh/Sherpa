package project.hikerguide.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import project.hikerguide.R;
import project.hikerguide.ui.activities.GuideDetailsActivity;

/**
 * Implementation of App Widget functionality.
 */
public class FavoritesWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.favorites_widget);
        Intent intent = new Intent(context, FavoritesWidgetService.class);

        // Set the Adapter to the FavoritesWidgetService RemoteViewsFactory
        views.setRemoteAdapter(R.id.widget_lv, intent);

        // Build the Intent for the click response
        Intent clickIntent = new Intent(context, GuideDetailsActivity.class);

        // Wrap the Intent in a PendingIntent to be used as the template
        PendingIntent templateIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_lv, templateIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

