package com.motorola.mmsp.rss.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.common.ViewItems;

public class RssWidgetService extends RemoteViewsService {
	private static final String TAG = "RssWidgetService";
	private static final String KEY_WIDGET_ID = "widgetId";
    private static final String KEY_ID = "id";
    private static final int WIDGET_ITEM_SHOW_COUNT = 40;
    private static final int MAX_DESCRIPTION_LENGTH = 50;

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		Log.d(TAG, "intent is " + intent);
		return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
	}
	
	class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
		private Context mContext;
		private int mAppWidgetId;
		private ArrayList<ViewItems> mItemsList = null;
		private HashMap<Integer, Bitmap>  mHashMap = null;
		public ListRemoteViewsFactory(Context context, Intent intent) {
			Log.d(TAG, "ListRemoteViewsFactory create");
			mContext = context;
			mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		private void updateItemState(int _id){
			String where = RssConstant.Content._ID + "=" + _id;
			ContentValues values = new ContentValues();
			values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
			mContext.getContentResolver().update(RssConstant.Content.ITEM_INDEX_URI, values, where, null);
		}
		private void fillItemsList(Context context, int widgetId) {
			mItemsList.clear();
			Cursor itemCursor = null;
			String temp = null;
			int itemCount = 0;
			try {
				String selection = RssConstant.Content.WIDGET_ID + "="
						+ widgetId;
				String orderBy = RssConstant.Content.ITEM_STATE + " asc, "
						+ RssConstant.Content.ITEM_PUBDATE + " desc";
				String []projection = new String[]{RssConstant.Content._ID, 
						RssConstant.Content.WIDGET_ID, RssConstant.Content.FEED_ID, 
						RssConstant.Content.ITEM_TITLE,RssConstant.Content.ITEM_DES_BRIEF,
						RssConstant.Content.ITEM_PUBDATE, RssConstant.Content.ITEM_STATE,
						RssConstant.Content.FEED_ICON};
				itemCursor = context.getContentResolver().query(RssConstant.Content.VIEW_LIMITED_ITEM_URI, 
						projection, selection, null, orderBy);
				if (itemCursor != null) {
					itemCount = itemCursor.getCount();
					Log.d(TAG, "itemCount is " + itemCount);
					if (itemCount != 0) {
						itemCursor.moveToFirst();
						do {
							ViewItems item = new ViewItems();
							item._id = itemCursor.getInt(itemCursor.getColumnIndex(RssConstant.Content._ID));
							item.widgetId = itemCursor.getInt(itemCursor.getColumnIndex(RssConstant.Content.WIDGET_ID));
							item.feedId = itemCursor.getInt(itemCursor.getColumnIndex(RssConstant.Content.FEED_ID));
							item.itemTitle = itemCursor.getString(itemCursor.getColumnIndex(RssConstant.Content.ITEM_TITLE));
							if(item.itemTitle == null || (item.itemTitle != null && item.itemTitle.trim().equals(""))){
								Log.d(TAG, "Exception has occured , so mark the item to deleted state !");
								updateItemState(item._id);
								continue;
							}
							item.itemTitle = removeTags(item.itemTitle);
							temp = itemCursor.getString(itemCursor.getColumnIndex(RssConstant.Content.ITEM_DES_BRIEF));
							if(temp == null) {
								item.itemDescription = "";
							} else {
								item.itemDescription = temp;
							}
							item.itemPubdate = itemCursor.getLong(itemCursor.getColumnIndex(RssConstant.Content.ITEM_PUBDATE));
							item.itemState = itemCursor.getInt(itemCursor.getColumnIndex(RssConstant.Content.ITEM_STATE));
							item.feeIcon = getIcon(itemCursor, item);
							mItemsList.add(item);
						} while(mItemsList.size() < WIDGET_ITEM_SHOW_COUNT && itemCursor.moveToNext());
					} else {
						Log.d(TAG, "item count is 0");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (itemCursor != null) {
					itemCursor.close();
				}
			}
			if(mItemsList.size() <= 0 && itemCount > 0){
				Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_INSTALL_EMPTY_VIEW);
				intent.putExtra(KEY_WIDGET_ID, widgetId);
				context.sendBroadcast(intent);
			}
		}
		
		/*private ViewItems fillViewItems(int _id){
			Cursor c = null;
			ViewItems item = null;
			String selection = RssConstant.Content._ID + "=" + _id;
			try{
				c = DatabaseHelper.getInstance(mContext).query(RssConstant.Content.VIEW_ITEM_URI, null, selection, null, null);
				if(c != null){
					if(c.moveToFirst()){
						item = new ViewItems();
						item._id = _id;
						item.widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
						item.feedId = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_ID));
						item.itemId = c.getLong(c.getColumnIndex(RssConstant.Content.ITEM_ID));
						item.feedTitle = c.getString(c.getColumnIndex(RssConstant.Content.FEED_TITLE));
						item.itemTitle = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_TITLE));
						item.itemUrl = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_URL));
						item.itemDescription = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_DESCRIPTION));
						item.itemPubdate = c.getLong(c.getColumnIndex(RssConstant.Content.ITEM_PUBDATE));
						item.itemState = c.getInt(c.getColumnIndex(RssConstant.Content.ITEM_STATE));
						item.feeIcon = getIcon(c, item);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(c != null){
					c.close();
				}
			}
			return item;
		}*/
		
		private Bitmap getIcon(Cursor c, ViewItems item){
			if(mHashMap != null){
				if(mHashMap.containsKey(item.feedId)){
					return mHashMap.get(item.feedId);
				}
				byte []image = c.getBlob(c.getColumnIndex(RssConstant.Content.FEED_ICON));
				if(image != null){
					Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
					mHashMap.put(item.feedId, bitmap);
					return bitmap;
				}
			}
			return null;
		}
		
		private void setItemInfo(RemoteViews rv, ViewItems item) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			if (item != null) {
				if (item.itemState == RssConstant.State.STATE_ITEM_UNREAD) {
					rv.setViewVisibility(R.id.widget_list_unreaditem_bg,
							View.VISIBLE);
					rv.setViewVisibility(R.id.widget_list_readitem_bg,
							View.INVISIBLE);
				} else {
					rv.setViewVisibility(R.id.widget_list_unreaditem_bg,
							View.INVISIBLE);
					rv.setViewVisibility(R.id.widget_list_readitem_bg,
							View.VISIBLE);
				}
				if (item.feeIcon != null) {
					rv.setBitmap(R.id.img, "setImageBitmap", item.feeIcon);
				} else {
					rv.setBitmap(R.id.img, "setImageBitmap", BitmapFactory
							.decodeResource(getResources(),
									R.drawable.ic_rss_small));
				}
				rv.setTextViewText(R.id.title, item.itemTitle);
				if (item.itemDescription != null && item.itemDescription.trim().length() != 0) {
					rv.setTextViewText(R.id.description, item.itemDescription);
					rv.setViewVisibility(R.id.description, View.VISIBLE);
				} else {
					rv.setViewVisibility(R.id.description, View.GONE);
				}
				rv.setTextViewText(R.id.datetime,
						sdf.format(new Date(item.itemPubdate)));
			}
		}
		
		private void registerOnClickPendingIntent(RemoteViews rv, ViewItems item) {
			Intent intent = new Intent();
			intent.putExtra(KEY_ID, item._id);
			intent.putExtra(KEY_WIDGET_ID, item.widgetId);
			rv.setOnClickFillInIntent(R.id.widget_listitem, intent);
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mItemsList.size();
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public RemoteViews getLoadingView() {
			Log.d(TAG, "getLoadingView");
//			resetItemIdList(mContext, mAppWidgetId);
			return new RemoteViews(mContext.getPackageName(), R.layout.rsswidget_listitem);
		}

		@Override
		public RemoteViews getViewAt(int position) {
/*			Log.d(TAG, "getViewAt position is " + position);*/
			if(position >= mItemsList.size()) {
				return null;
			}
			RemoteViews rv = new RemoteViews(mContext.getPackageName(),
					R.layout.rsswidget_listitem);
			ViewItems item = mItemsList.get(position);
			if(item != null){
				setItemInfo(rv, item);
				registerOnClickPendingIntent(rv, item);
			}
			return rv;
		}
		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onCreate() {
			Log.d(TAG, "onCreate");	
			mItemsList = new ArrayList<ViewItems>();
			mHashMap = new HashMap<Integer, Bitmap>();
			fillItemsList(mContext, mAppWidgetId);			
		}

		@Override
		public void onDataSetChanged() {
			Log.d(TAG, "onDataChanged");
			long token = Binder.clearCallingIdentity();
			fillItemsList(mContext, mAppWidgetId);
			Binder.restoreCallingIdentity(token);
		}

		@Override
		public void onDestroy() {
			Log.d(TAG, "onDestroy");
			mItemsList.clear();
			mHashMap.clear();
		}
	}
	private String removeTags(String str) {
		if(str != null){
			str = str.replaceAll("&gt;", ">");
			str = str.replaceAll("&lt;", "<");
			str = str.replaceAll("&amp;", "&");
			str = str.replaceAll("&quot;", "\"");
			str = str.replaceAll("&nbsp;", " ");
			str = str.replaceAll("<.*?>", " ");
			str = str.replaceAll("\\s+", " ");
		}
		return str;
	}
}
