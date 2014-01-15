package com.motorola.mmsp.rss.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.app.activity.RssArticleDetailActivity;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.service.RssService;
import com.motorola.mmsp.rss.util.WidgetUpdateUtil;

public class RssWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "RssWidgetProvider";
	public static RemoteViews view;
	private static List<String> sWidgetUpdating = new ArrayList<String>();
    private static final String KEY_WIDGET_ID = "widgetId";
    private static final String KEY_WIDGET_IDS = "widgetIds";
    private static final String KEY_WIDGET_AUTOUPDATE = "autoUpdate";
    private static final String KEY_SUBSCRIBED = "subscribed";
    private static final String KEY_NETWORK = "network";
    private static final String SHARED_NAME = "update-flags";
    

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Log.d(TAG, "onDeleted");
		DatabaseHelper helper = DatabaseHelper.getInstance(context);
		for(int widgetId : appWidgetIds){
			helper.markWidgetDeleted(widgetId);
			helper.markAllFeedDeleted(widgetId);
			helper.markAllItemDeleted(widgetId);
		}
		Intent intent = new Intent();
		intent.setAction(RssConstant.Intent.INTENT_RSSWIDGET_DELETE);
		intent.putExtra(KEY_WIDGET_IDS, appWidgetIds);
		context.startService(intent);
		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "onDisabled");
		super.onDisabled(context);
	}

	@Override
	public void onEnabled(Context context) {
		Log.d(TAG, "onEnabled");
		super.onEnabled(context);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG,"onReceive begin");
		if(intent != null){
			String action = intent.getAction();
			Log.d(TAG,"action = "+action);
			if (RssConstant.Intent.INTENT_RSSAPP_UPDATE_ITEM.equals(action) || RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED.equals(action) || RssConstant.Intent.INTENT_RSSAPP_SUBSCRIBE_FINISHED.equals(action)||RssConstant.Intent.INTENT_RSSAPP_FEED_ADDED_FROM_EMPTY_FINISHED.equals(action)) {
				boolean autoUpdate = intent.getBooleanExtra(KEY_WIDGET_AUTOUPDATE, false);
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				int updated = intent.getIntExtra(KEY_SUBSCRIBED, 0);
				boolean network = intent.getBooleanExtra(KEY_NETWORK, false);
				SharedPreferences preferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
				boolean existed = preferences.contains(String.valueOf(widgetId));
				Log.d(TAG, "updated = " + updated + " , existed = " + existed);
				if(updated > 0 ){
					if(!existed){
						preferences.edit().putBoolean(String.valueOf(widgetId), true).commit();
					}
				}else{
					if(existed){
						preferences.edit().remove(String.valueOf(widgetId)).commit();
					}
				}
				Log.d(TAG, "autoUpdate is " + autoUpdate + " widgetId is " + widgetId + " , updated = " + updated);
				if (!autoUpdate) {
					//TODO
				}
				Log.d(TAG, "widget id " + widgetId + " is removed");
				boolean exist = DatabaseHelper.getInstance(context).widgetDeleted(widgetId);
				Log.d(TAG, "Widtget Exist = " + exist);
				sWidgetUpdating.remove(String.valueOf(widgetId));
				updateWidget(context, widgetId, action);
				int feedCount = getFeedCount(context, widgetId);
				
//				boolean network = NetworkUtil.isNetworkAvailable(context);
				if(feedCount <= 0){
					notifyServiceToDeleteAlarm(context, widgetId);
					if(RssConstant.Intent.INTENT_RSSAPP_SUBSCRIBE_FINISHED.equals(action) && network && exist){
						String tips = context.getResources().getString(R.string.refresh_failed);
						Toast.makeText(context, tips, Toast.LENGTH_SHORT).show();
					}
				}else{
					int itemCount = getItemCount(context, widgetId);
					if(itemCount <= 0){
						if(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED.equals(action) && network && exist){
							String tips = context.getResources().getString(R.string.refresh_failed);
							Toast.makeText(context, tips, Toast.LENGTH_SHORT).show();
						}
					}
				}
			}else if(RssConstant.Intent.INTENT_RSSWIDGET_REFRESH.equals(action)){
				Log.d(TAG,"INTENT_RSSWIDGE_REFRESH");
				boolean autoUpdate = intent.getBooleanExtra(KEY_WIDGET_AUTOUPDATE, false);
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				Log.d(TAG,"refreshwidgetId = "+widgetId);
				if (!autoUpdate) {
					//TODO
				}
				Log.d(TAG, "widget id " + widgetId + " is added to list");
				if(!sWidgetUpdating.contains(String.valueOf(widgetId))){
					sWidgetUpdating.add(String.valueOf(widgetId));
				}
				startUpdateAnimation(context, widgetId);
			} else if(RssConstant.Intent.INTENT_RSSWIDGET_UPDATE.equals(action)) {
				int[] appWidgetIds = intent.getIntArrayExtra(KEY_WIDGET_IDS);
				for(int i = 0; i < appWidgetIds.length; i++) {
					int feedCount = getFeedCount(context, appWidgetIds[i]);
					if(feedCount > 0){
						updateWidget(context, appWidgetIds[i], action);
					}
				}
			}else if(RssConstant.Intent.INTENT_RSSAPP_NOTIFY_WIDGET_UPDATE.equals(action)){
				SharedPreferences preference = context.getSharedPreferences(RssConstant.Content.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
				int widgetId = preference.getInt(RssConstant.Key.KEY_WIDGET_ID,	-1);
				preference.edit().putInt(RssConstant.Key.KEY_WIDGET_ID, -1).commit();
				if (widgetId != -1) {
					updateWidget(context, widgetId, action);
				}
			}else if(RssConstant.Intent.INTENT_RSSAPP_INSTALL_EMPTY_VIEW.equals(action)){
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				Intent emptyIntent = new Intent(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
				emptyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				emptyIntent.putExtra(KEY_WIDGET_ID, widgetId);
				PendingIntent pendingIntent = PendingIntent.getActivity(context,
						(int) System.currentTimeMillis(), emptyIntent, 0);
				Log.d(TAG, "Install an empty view, it will display when the under view is empty!");
				view = new RemoteViews(context.getPackageName(), R.layout.rssnews);
				view.setEmptyView(R.id.rsslist, R.id.empty_article);
				SharedPreferences preferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
				boolean existed = preferences.contains(String.valueOf(widgetId));
				if(existed){
				        preferences.edit().remove(String.valueOf(widgetId)).commit();
				}
				view.setViewVisibility(R.id.unread_number, View.GONE);
				view.setViewVisibility(R.id.rssicon, View.VISIBLE);
				view.setViewVisibility(R.id.rssicon_new, View.INVISIBLE);
				
				WidgetUpdateUtil.updateWidgetName(context, widgetId, view);
				if(!sWidgetUpdating.contains(String.valueOf(widgetId))){
					Log.d(TAG, "widget id " + widgetId + " is normal");
					view.setTextViewText(R.id.empty_article, context.getResources().getString(R.string.empty_article_list));
					view.setViewVisibility(R.id.refreshbtn, View.VISIBLE);
					view.setViewVisibility(R.id.progressbtn, View.GONE);
					view.setOnClickPendingIntent(R.id.empty_article, pendingIntent);
					WidgetUpdateUtil.registerIntentToRefresh(context, view, widgetId);
				}else{
					Log.d(TAG, "widget id " + widgetId + " is updating");
					view.setTextViewText(R.id.empty_article, context.getResources().getString(R.string.loading));
					view.setViewVisibility(R.id.refreshbtn, View.GONE);
					view.setViewVisibility(R.id.progressbtn, View.VISIBLE);
				}
				AppWidgetManager.getInstance(context).updateAppWidget(widgetId, view);
			}else if(RssConstant.Intent.INTENT_RSSAPP_CLEAR_NOTIFICATION.equals(action)){
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);				
				SharedPreferences preferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
				boolean existed = preferences.contains(String.valueOf(widgetId));
				if(existed){
					preferences.edit().remove(String.valueOf(widgetId)).commit();
				}
				updateWidget(context, widgetId, action);
			}else if(RssConstant.Intent.INTENT_RSSAPP_UPDATE_WIDGET_NAME.equals(action)){
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				updateWidget(context, widgetId, action);
			}else if(RssConstant.Intent.INTENT_RSSAPP_UPDATE_WIDGET_FOR_EXCEPTION.equals(action)){
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				updateWidget(context, widgetId, action);
			}else if(RssConstant.Intent.INTENT_RSSAPP_SERVICE_RESTART.equals(action)){
				Log.d(TAG ,"Service restart");
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
				ComponentName provider = new ComponentName(context, RssWidgetProvider.class);
				int appWidgetIds[] = appWidgetManager.getAppWidgetIds(provider);
				int len = 0;
				if(appWidgetIds != null){
					SharedPreferences preferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
					Log.d(TAG, "len = " + appWidgetIds.length);
					for(int widgetId : appWidgetIds){
						if(sWidgetUpdating != null){
							sWidgetUpdating.remove(String.valueOf(widgetId));
						}
						boolean existed = preferences.contains(String.valueOf(widgetId));
						if(existed){
							preferences.edit().remove(String.valueOf(widgetId)).commit();
						}
						updateWidget(context, widgetId, action);
					}
				}
			}
		}
		super.onReceive(context, intent);
	}
	private void entureServiceStartup(Context context){
		Intent intent = new Intent(context, RssService.class);
		context.startService(intent);
	}
	private void updateWidget(Context context, int widgetId, String action) {
		Log.d(TAG, "updateWidget");
		if(RssConstant.Intent.INTENT_RSSAPP_FEED_ADDED_FROM_EMPTY_FINISHED.equals(action)){
			view = new RemoteViews(context.getPackageName(), R.layout.rssnonews);
			view.setTextViewText(R.id.nofeed_noarticle_loading, context.getResources().getString(R.string.loading));
			PendingIntent pendingIntent = PendingIntent.getActivity(context,
					(int) System.currentTimeMillis(), new Intent(), 0);
				view.setOnClickPendingIntent(R.id.rssnonews, pendingIntent);
			view.setViewVisibility(R.id.unread_number, View.GONE);
		}else{
			if (getFeedCount(context, widgetId) == 0) {
				Log.d(TAG, "no feed");
				view = new RemoteViews(context.getPackageName(), R.layout.rssnonews);
				view.setViewVisibility(R.id.unread_number, View.GONE);
				WidgetUpdateUtil.updateWidget(context, widgetId, view, WidgetUpdateUtil.STATE_NO_FEED);
			} else {
				Log.d(TAG, "update widget, has feed and item");
				int itemCount = getItemCount(context, widgetId);
				if(itemCount > 0){
					Intent intent = new Intent(context, RssWidgetService.class);
					intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
					intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
					view = new RemoteViews(context.getPackageName(), R.layout.rssnews);
					view.setRemoteAdapter(widgetId, R.id.rsslist, intent);
					
//					Intent itemIntent = new Intent(context, RssArticleDetailActivity.class);
					Intent itemIntent = new Intent(context, RssService.class);
//					itemIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
					itemIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_START_ARTICLE_ACTIVITY);
					itemIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
//					itemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//					PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(),
//							itemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
					PendingIntent pendingIntent = PendingIntent.getService(context, (int) System.currentTimeMillis(),
							itemIntent, 0);
					view.setPendingIntentTemplate(R.id.rsslist, pendingIntent);
				}else{
					Intent emptyIntent = new Intent(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
					emptyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
					emptyIntent.putExtra(KEY_WIDGET_ID, widgetId);
					PendingIntent pendingIntent = PendingIntent.getActivity(context,
							(int) System.currentTimeMillis(), emptyIntent, 0);
					view = new RemoteViews(context.getPackageName(), R.layout.rssnews);
					view.setTextViewText(R.id.empty_article, context.getResources().getString(R.string.empty_article_list));
					view.setEmptyView(R.id.rsslist, R.id.empty_article);
					view.setOnClickPendingIntent(R.id.empty_article, pendingIntent);
				}
//				int unReadCount = DatabaseHelper.getInstance(context).getUnReadItemCount(context, widgetId);
//				Log.d(TAG, "unReadCount = " + unReadCount);
				SharedPreferences preferences = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
				boolean existed = preferences.contains(String.valueOf(widgetId));
				Log.d(TAG, "itemCount = " + itemCount + " , existed = " + existed);
				if(itemCount <= 0){
					if(existed){
						preferences.edit().remove(String.valueOf(widgetId)).commit();
					}
				}
				boolean flagExisted = preferences.contains(String.valueOf(widgetId));
				Log.d(TAG, "flagExisted = " + flagExisted);
				if(flagExisted){
					view.setViewVisibility(R.id.unread_number, View.VISIBLE);
					
					view.setViewVisibility(R.id.rssicon, View.INVISIBLE);
					view.setViewVisibility(R.id.rssicon_new, View.VISIBLE);
					//view.setTextViewText(R.id.unread_number, "" + unReadCount);
					Intent i = new Intent(RssConstant.Intent.INTENT_RSSAPP_NEW_ITEMS_DISPLAY);
					i.putExtra(KEY_WIDGET_ID, widgetId);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					PendingIntent pending = PendingIntent.getActivity(context, (int)System.currentTimeMillis(), i, 0);
					view.setOnClickPendingIntent(R.id.unread_number, pending);
				}else{
					view.setViewVisibility(R.id.unread_number, View.GONE);
					
					view.setViewVisibility(R.id.rssicon, View.VISIBLE);
					view.setViewVisibility(R.id.rssicon_new, View.INVISIBLE);
				}
				WidgetUpdateUtil.registerIntentToRefresh(context, view, widgetId);
			}
			if(!sWidgetUpdating.contains(String.valueOf(widgetId))){
				Log.d(TAG, "widget id " + widgetId + " is normal");
				view.setViewVisibility(R.id.refreshbtn, View.VISIBLE);
				view.setViewVisibility(R.id.progressbtn, View.GONE);
			}else{
				Log.d(TAG, "widget id " + widgetId + " is updating");
				view.setViewVisibility(R.id.refreshbtn, View.GONE);
				view.setViewVisibility(R.id.progressbtn, View.VISIBLE);
			}
//			WidgetUpdateUtil.registerIntentToRefresh(context, view, widgetId);	
		}	
		WidgetUpdateUtil.updateWidgetName(context, widgetId, view);
		Log.d(TAG, "view = " + view.getLayoutId());
		AppWidgetManager.getInstance(context).updateAppWidget(widgetId, view);
		AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetId, R.id.rsslist);
	}
	
	private void startUpdateAnimation(Context context, int widgetId) {
		if (getFeedCount(context, widgetId) == 0) {
			view = new RemoteViews(context.getPackageName(), R.layout.rssnonews);
			view.setTextViewText(R.id.nofeed_noarticle_loading, context.getResources().getString(R.string.loading));
			PendingIntent pendingIntent = PendingIntent.getActivity(context,
					(int) System.currentTimeMillis(), new Intent(), 0);
			view.setOnClickPendingIntent(R.id.rssnonews, pendingIntent);
		} else {
			if(getItemCount(context, widgetId) == 0) {
				view = new RemoteViews(context.getPackageName(), R.layout.rssnonews);
				view.setTextViewText(R.id.nofeed_noarticle_loading, context.getResources().getString(R.string.loading));
				PendingIntent pendingIntent = PendingIntent.getActivity(context,
						(int) System.currentTimeMillis(), new Intent(), 0);
				view.setOnClickPendingIntent(R.id.rssnonews, pendingIntent);
				WidgetUpdateUtil.updateWidgetName(context, widgetId, view);
			} else {
				view = new RemoteViews(context.getPackageName(),R.layout.rssnews);
				
				Intent intent = new Intent(context, RssWidgetService.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				view.setRemoteAdapter(widgetId, R.id.rsslist, intent);
				Intent itemIntent = new Intent(context,
						RssArticleDetailActivity.class);
				itemIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
				itemIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				itemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(),
						itemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				view.setPendingIntentTemplate(R.id.rsslist, pendingIntent);
				WidgetUpdateUtil.updateWidgetName(context, widgetId, view);
			}
//			view.setViewVisibility(R.id.refreshbtn, View.GONE);
//			view.setViewVisibility(R.id.progressbtn, View.VISIBLE);
		}
		view.setViewVisibility(R.id.refreshbtn, View.GONE);
		view.setViewVisibility(R.id.progressbtn, View.VISIBLE);
		AppWidgetManager.getInstance(context).updateAppWidget(widgetId, view);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		entureServiceStartup(context);
		Log.d(TAG, "len = " + appWidgetIds.length);
		for (int i = 0; i < appWidgetIds.length; i++) {
			updateWidget(context, appWidgetIds[i], null);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	private int getFeedCount(Context context, int widgetId) {
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId + " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED;
		int count = 0;
		Cursor c = null;
		try {
			c = context.getContentResolver().query(RssConstant.Content.FEED_INDEX_URI, null, selection,
							null, null);
			if (c != null) {
				count = c.getCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(c != null) {
				c.close();
			}
		}
		Log.d(TAG, "count = " + count);
		return count;
	}
	
	private int getItemCount(Context context, int widgetId) {
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId + " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED;
		int count = 0;
		Cursor c = null;
		try {
			c = context.getContentResolver().query(RssConstant.Content.ITEM_INDEX_URI, null, selection,
							null, null);
			if (c != null) {
				count = c.getCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(c != null) {
				c.close();
			}
		}
		Log.d(TAG, "count = " + count);
		return count;
	}
	
	private void notifyServiceToDeleteAlarm(Context context, int widgetId) {
		Intent alarmIntent = new Intent();
		alarmIntent.putExtra(KEY_WIDGET_ID, widgetId);
		alarmIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_DELETE_ALARM);
		Log.d(TAG, "delete alarm, widgetId is " + widgetId);
		context.startService(alarmIntent);
	}
}
