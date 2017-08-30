package project.hikerguide.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.data.GuideProvider;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.utilities.FormattingUtils;

/**
 * Created by Alvin on 8/30/2017.
 */

public class FavoritesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return new FavoriteRemoteViewsFactory(this);
    }

    private class FavoriteRemoteViewsFactory implements RemoteViewsFactory {

        // ** Member Variables ** //
        private Context mContext;
        private Cursor mCursor;

        FavoriteRemoteViewsFactory(Context context) {
            mContext = context;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {

            // Close the previous Cursor
            if (mCursor != null) {
                mCursor.close();
            }

            // Query the database for favorites
            mCursor = mContext.getContentResolver().query(
                    GuideProvider.Guides.CONTENT_URI,
                    null,
                    GuideContract.GuideEntry.FAVORITE + " = ?",
                    new String[] {"1"},
                    null);
        }

        @Override
        public void onDestroy() {
            if (mCursor != null) {
                mCursor.close();
            }
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int i) {

            // Move the Cursor to the correct position
            mCursor.moveToPosition(i);

            // Retrieve the data from the Cursor
            Guide guide = Guide.createGuideFromCursor(mCursor);

            // Initialize the RemoteViews
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.list_item_widget);

            // Bind the data to the Views
            return bind(remoteViews, guide);
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        /**
         * Binds data from a Guide data model to a RemoteViews
         *
         * @param remoteViews    The RemoteViews whose Views will be populated
         * @param guide          The Guide whose information will be used to populate the
         *                       RemoteViews
         * @return A RemoteViews with its Views populated
         */
        private RemoteViews bind(RemoteViews remoteViews, Guide guide) {

            // Format the Strings for populating the RemoteViews
            String trailName    = guide.trailName;

            String rating;
            if (guide.rating != 0) {
                rating          = mContext.getString(R.string.list_guide_format_rating, (guide.rating / guide.reviews));
            } else {
                rating          = mContext.getString(R.string.list_guide_format_rating_zero);
            }

            String reviews      = mContext.getString(R.string.list_guide_format_reviews, guide.reviews);
            String distance     = mContext.getString(R.string.list_guide_format_distance_imperial, FormattingUtils.convertDistance(mContext, guide.distance));
            String author       = guide.authorName;
            String difficulty   = FormattingUtils.formatDifficulty(mContext, guide.difficulty);

            // Set the Strings to the TextViews
            remoteViews.setTextViewText(R.id.list_widget_tv, trailName);
            remoteViews.setTextViewText(R.id.list_widget_rating_tv, rating);
            remoteViews.setTextViewText(R.id.list_widget_review_tv, reviews);
            remoteViews.setTextViewText(R.id.list_widget_distance_tv, distance);
            remoteViews.setTextViewText(R.id.list_widget_author_tv, author);
            remoteViews.setTextViewText(R.id.list_widget_difficulty_tv, difficulty);

            return remoteViews;
        }
    }
}
