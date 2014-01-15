package com.motorola.mmsp.rss.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.RssConstant;

public class WidgetUpdateUtil {
	private static final String TAG = "WidgetUpdateUtil";
    private static final String KEY_WIDGET_ID = "widgetId";
    private static final String KEY_ID = "id";
    private static final String KEY_ITEM_STATE = "itemState";
    private static final String KEY_ITEM_ENTERWAY = "isEnterSettingsFromWidget";
    private static final String KEY_AUTO_UPDATE = "autoUpdate";
    public static final int STATE_HAS_FEED = 0;
    public static final int STATE_NO_FEED = 1;
    public static final int STATE_HAS_FEED_BUT_NO_ITEM = 2;
    
	public static void registerOnClickPendingIntent(Context context,
			RemoteViews remoteViews, int widgetId, int state, int viewId, int _id,
			int itemState) {
		Log.d(TAG, "mAppWidgetId is " + widgetId);
		Intent intent = new Intent();
		if (viewId == R.id.rssnonews) {
			if(state == STATE_NO_FEED) {
				intent.putExtra(KEY_ITEM_ENTERWAY, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				intent.setAction(RssConstant.Intent.INTENT_RSSAPP_SETTINGS);
			} else if(state == STATE_HAS_FEED_BUT_NO_ITEM) {
				intent.setAction(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				intent.putExtra(KEY_ID, _id);
				intent.putExtra(KEY_ITEM_STATE, itemState);
			}			
		} else {
			intent.setAction(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(KEY_ID, _id);
			intent.putExtra(KEY_ITEM_STATE, itemState);
		}
		intent.putExtra(KEY_WIDGET_ID, widgetId);
		PendingIntent pendingIntent = PendingIntent.getActivity(context,
				(int) System.currentTimeMillis(), intent, 0);
		remoteViews.setOnClickPendingIntent(viewId, pendingIntent);
	}

	public static void registerIntentToRefresh(Context context,
			RemoteViews remoteViews, int widgetId) {
		Log.d(TAG, "mAppWidgetIdrefresh is " + widgetId);
		Intent refreshIntent = new Intent();
		refreshIntent.setAction(RssConstant.Intent.INTENT_RSSSERVICE_STARTREFRESH);
		refreshIntent.putExtra(KEY_WIDGET_ID, widgetId);
		refreshIntent.putExtra(KEY_AUTO_UPDATE, false);
		PendingIntent refreshPendingIntent = PendingIntent.getService(context,
				(int)System.currentTimeMillis(), refreshIntent, 0);
		remoteViews.setOnClickPendingIntent(R.id.refreshbtn,
				refreshPendingIntent);
	}

	public static void updateWidgetName(Context context, int appWidgetId,
			RemoteViews view) {
		String widgetTitle = "";
		String selection = RssConstant.Content.WIDGET_ID + "=" + appWidgetId;
		Cursor widgetsCursor = null;
		try {
			widgetsCursor = context.getContentResolver().query(
					RssConstant.Content.WIDGET_URI, null, selection, null, null);
			if (widgetsCursor != null && widgetsCursor.getCount() > 0) {
				widgetsCursor.moveToFirst();
				widgetTitle = widgetsCursor.getString(widgetsCursor
						.getColumnIndex(RssConstant.Content.WIDGET_TITLE));
			}					
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(widgetsCursor != null) {
				widgetsCursor.close();
			}
		}
		if (view.getLayoutId() == R.layout.rssnews) {
			view.setTextViewText(R.id.widgetname, widgetTitle);
		}
		if (view.getLayoutId() == R.layout.rssnonews) {
			view.setTextViewText(R.id.widgetnamenonews, widgetTitle);
		}
	}
	
	public static void updateWidget(Context context, int appWidgetId,
			RemoteViews view, int state) {
		Log.d(TAG, "updateWidget begin");
		if (state == STATE_NO_FEED) {
			Log.d(TAG,"no feed");
			view.setTextViewText(R.id.nofeed_noarticle_loading, context.getResources().getString(R.string.feedaddtips));
			registerOnClickPendingIntent(context, view, appWidgetId, state,
					R.id.rssnonews, RssConstant.Flag.WIDGET_NOFEED,
					RssConstant.Flag.WIDGET_NOFEED_ITEMSTATE);
		} else if (state == STATE_HAS_FEED_BUT_NO_ITEM) {
			Log.d(TAG,"no article");
			view.setTextViewText(R.id.nofeed_noarticle_loading, context.getResources().getString(R.string.empty_article_list));
			registerOnClickPendingIntent(context, view, appWidgetId, state,
					R.id.rssnonews, RssConstant.Flag.WIDGET_NOFEED,
					RssConstant.Flag.WIDGET_NOFEED_ITEMSTATE);
		}
	}
	
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}
}
