package com.motorola.mmsp.rss.common;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


public class DatabaseHelper {
	
	private final static String TAG = "DatabaseHelper";
	private Context mContext;
	private static DatabaseHelper sDatabaseHelper = null;
	private boolean mPauseDelete = false;
	
	private static final int MAX_DESCRIPTION_LENGTH = 50;
	private DatabaseHelper(Context context){
		mContext = context;
	}
	
	public static DatabaseHelper getInstance(Context context){
		if(sDatabaseHelper == null){
			sDatabaseHelper = new DatabaseHelper(context);
		}
		return sDatabaseHelper;
	}
	
	private synchronized Uri addWidgetIntoDatabase(WidgetInfo info){
		if(info == null){
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.WIDGET_ID, info.widgetId);
		values.put(RssConstant.Content.WIDGET_TITLE, info.widgetTitle);
		values.put(RssConstant.Content.WIDGET_UPDATE_FREQUENCY, info.updateFrequency);
		values.put(RssConstant.Content.WIDGET_VALIDITY_TIME, info.widgetValidaty);
		return mContext.getContentResolver().insert(RssConstant.Content.WIDGET_URI, values);
	}
	
	private synchronized Uri addFeedIntoDatabase(FeedInfo info){
		if(info == null){
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.FEED_TITLE, info.feedTitle);
        values.put(RssConstant.Content.FEED_URL, info.feedUrl);
        values.put(RssConstant.Content.FEED_IS_BUNDLE, info.feedIsBundle);
        values.put(RssConstant.Content.FEED_PUBDATE, info.feedPubdate); 
        values.put(RssConstant.Content.FEED_GUID, info.feedGuid);
        return mContext.getContentResolver().insert(RssConstant.Content.FEED_URI, values);
	}
	
	private synchronized Uri addItemIntoDatabase(ItemInfo info){
		if(info == null){
			return null;
		}
		String itemDesBrief = null;
		if(info.itemDescription != null) {
			itemDesBrief = removeTags(info.itemDescription);
			if(itemDesBrief.length() > MAX_DESCRIPTION_LENGTH){
				itemDesBrief = itemDesBrief.substring(0, MAX_DESCRIPTION_LENGTH);
			}
		}
		ContentValues values = new ContentValues();
//		values.put(RssConstant.Content._ID, info.itemId);
		values.put(RssConstant.Content.ITEM_TITLE, info.itemTitle);
        values.put(RssConstant.Content.ITEM_URL, info.itemUrl);
        values.put(RssConstant.Content.ITEM_DESCRIPTION, info.itemDescription);
        values.put(RssConstant.Content.ITEM_DES_BRIEF, itemDesBrief);
        values.put(RssConstant.Content.ITEM_GUID, info.itemGuid); 
        values.put(RssConstant.Content.ITEM_AUTHOR, info.itemAuthor);
        values.put(RssConstant.Content.ITEM_PUBDATE, info.itemPubdate);
        return mContext.getContentResolver().insert(RssConstant.Content.ITEM_URI, values);
	}
	
	public synchronized Uri addHistoryFeedToDatabase(String historyTitle, String historyUrl, int times, long date) {
		if(historyTitle == null || historyUrl == null){
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.HISTORY_TITLE, historyTitle);
		values.put(RssConstant.Content.HISTORY_URL, historyUrl);
        values.put(RssConstant.Content.HISTORY_TIMES, times);
        values.put(RssConstant.Content.HISTORY_DATE, date);
        return mContext.getContentResolver().insert(RssConstant.Content.HISTORY_URI, values);
	}
	
	public int addWidget(WidgetInfo info){
		if(info == null){
			return -1;
		}
		Log.d(TAG, "AddWidget");
		addWidgetIntoDatabase(info);
		return info.widgetId;
	}
	public boolean addFeed(FeedInfo info){
		boolean success = false;
		if(info == null){
			return false;
		}
		String selection = RssConstant.Content.FEED_GUID + "=" + info.feedGuid;
		int feedCount = 0;
		Uri feedUri = null;
		int feedId = -1;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.FEED_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					feedCount = c.getCount();
				}
				if(feedCount <= 0){				
					feedUri = addFeedIntoDatabase(info);
					feedId = (int) ContentUris.parseId(feedUri);
					success = true;
				}else{
					feedId = c.getInt(c.getColumnIndex(RssConstant.Content._ID));
					success = false;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		Log.d(TAG, "add feed, feedId = " + feedId + " , feedTitle = "  + info.feedTitle);
		info.feedId = feedId;
		return success;
	}
	
	public int addItem(ItemInfo info){
		if(info == null){
			return -1;
		}
		String selection = RssConstant.Content.ITEM_GUID + "=" + info.itemGuid;
		int itemCount = 0;
		Uri itemUri = null;
		int itemId = -1;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.ITEM_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					itemCount = c.getCount();
				}
				if(itemCount <= 0){
					itemUri = addItemIntoDatabase(info);
					itemId = (int) ContentUris.parseId(itemUri);
//					itemId = info.itemId;
					info.itemId = itemId;
				}else{
					itemId = c.getInt(c.getColumnIndex(RssConstant.Content._ID));
					info.itemId = itemId;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
/*		Log.d(TAG, "AddItem >>>  " + info.toString());*/
		return itemId;
	}
	public synchronized Uri addFeedIndex(IndexInfo info){
		if(info == null){
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.WIDGET_ID, info.widgetId);
		values.put(RssConstant.Content.FEED_ID, info.feedId);
		return mContext.getContentResolver().insert(RssConstant.Content.FEED_INDEX_URI, values);
	}
	public synchronized Uri addItemIndex(IndexInfo info){
		if(info == null){
			return null;
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.WIDGET_ID, info.widgetId);
		values.put(RssConstant.Content.FEED_ID, info.feedId);
		values.put(RssConstant.Content.ITEM_ID, info.itemId);
		values.put(RssConstant.Content.ITEM_STATE, info.itemState);
		return mContext.getContentResolver().insert(RssConstant.Content.ITEM_INDEX_URI, values);
	}
	public synchronized int updateAllItemsState(int state){
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.ITEM_STATE, state);
		return mContext.getContentResolver().update(RssConstant.Content.ITEM_INDEX_URI, values, null, null);
	}
	public synchronized int updateItemState(IndexInfo info){
		if(info == null){
			return -1;
		}
		String where = RssConstant.Content.WIDGET_ID + "=" + info.widgetId 
				+ " and " + RssConstant.Content.FEED_ID + "=" + info.feedId
				+ " and " + RssConstant.Content.ITEM_ID + "=" + info.itemId;
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.ITEM_STATE, info.itemState);
		return mContext.getContentResolver().update(RssConstant.Content.ITEM_INDEX_URI, values, where, null);
	}
	
	public synchronized void updateHistoryFeed(String url, int times) {
		if(url == null){
			return ;
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.HISTORY_URL, url);
		mContext.getContentResolver().update(RssConstant.Content.HISTORY_URI, values, "times = '" + times + "'", null);
	}
	
	private synchronized int deleteWidgetFromDatabase(int widgetId){
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId
				+ " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_DELETED;
		return mContext.getContentResolver().delete(RssConstant.Content.WIDGET_URI, where, null);
	}
	
	private synchronized int deleteFeedFromDatabase(int feedId){
		String where = RssConstant.Content._ID + "=" + feedId;
		return mContext.getContentResolver().delete(RssConstant.Content.FEED_URI, where, null);
	}
	
	private synchronized int deleteItemFromDatabase(long itemId){
		String where = RssConstant.Content._ID + "=" + itemId;
		return mContext.getContentResolver().delete(RssConstant.Content.ITEM_URI, where, null);
	}
	
	private synchronized int deleteFeedIndexFromDatabase(int widgetId, int feedId){
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId 
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId
				+ " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_DELETED; 
		return mContext.getContentResolver().delete(RssConstant.Content.FEED_INDEX_URI, where, null);
	}
	private synchronized int deleteItemIndexFromDatabase(int widgetId, int feedId, long itemId){
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId 
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId 
				+ " and " + RssConstant.Content.ITEM_ID + "=" + itemId
				+ " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_DELETED;
		
		return mContext.getContentResolver().delete(RssConstant.Content.ITEM_INDEX_URI, where, null);
	}
	
	public void deleteWidget(int widgetId){
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId;
		String []projection = new String[]{RssConstant.Content.WIDGET_ID, RssConstant.Content.FEED_ID};
		Cursor c = null;
		try {
			c = mContext.getContentResolver().query(
					RssConstant.Content.ITEM_INDEX_URI, projection, selection,
					null, null);
			if (c != null) {
				if (c.moveToFirst()) {
					do {
						int feedId = c.getInt(c
								.getColumnIndex(RssConstant.Content.FEED_ID));
						deleteFeed(widgetId, feedId);
					} while (c.moveToNext());
				}

			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(c != null){
				c.close();
			}
		}
		deleteWidgetFromDatabase(widgetId);
	}
	
	public void deleteFeed(int widgetId, int feedId){
		/**
		 * delete all item followed by feedId before delete feed
		 */
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId;
		Cursor c = null;
		try {
			c = mContext.getContentResolver().query(
					RssConstant.Content.ITEM_INDEX_URI, null, selection, null,
					null);
			if (c != null) {

				if (c.moveToFirst()) {
					do {
						long itemId = c.getLong(c
								.getColumnIndex(RssConstant.Content.ITEM_ID));
						deleteItem(widgetId, feedId, itemId);
					} while (c.moveToNext());
				}

			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(c != null){
				c.close();
			}
		}
		
		selection = RssConstant.Content.FEED_ID + "=" + feedId;
		int feedCount = 0;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.FEED_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					feedCount = c.getCount();
				}
				if(feedCount <= 1){
					deleteFeedFromDatabase(feedId);
				}
				deleteFeedIndexFromDatabase(widgetId, feedId);
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
	}
	public void deleteItem(int widgetId, int feedId, long itemId){
/*		Log.d(TAG, "deleteItem --- widgetId = " + widgetId + " , feedId = " + feedId + " , itemId = " + itemId);*/
		String selection = RssConstant.Content.ITEM_ID + "=" + itemId;
		long itemCount = 0;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.ITEM_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					itemCount = c.getCount();
				}
				if(itemCount <= 1){
					deleteItemFromDatabase(itemId);
				}
				deleteItemIndexFromDatabase(widgetId, feedId, itemId);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		
	}
	
	public int updateItem(ItemInfo info){
		if(info == null){
			return -1;
		}
		String selection = RssConstant.Content.ITEM_GUID + "=" + info.itemGuid;
		int itemCount = 0;
		Uri itemUri = null;
		int itemId = -1;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.ITEM_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					itemCount = c.getCount();
				}
				if(itemCount <= 0){
					itemUri = addItemIntoDatabase(info);
//					itemId = info.itemId;
					itemId = (int) ContentUris.parseId(itemUri);
					info.itemId = itemId;
/*					Log.d(TAG, "Added itemTitle = " + info.itemTitle);*/
				}else{
					itemId = c.getInt(c.getColumnIndex(RssConstant.Content._ID));
					info.itemId = itemId;
/*					Log.d(TAG, "Exist itemTitle = " + info.itemTitle);*/
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		return itemId;
	}
	
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
		return mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
	}
	
	public long getNextItemId(){
		long max = 0;
		String sortBy = RssConstant.Content._ID + " desc";
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.ITEM_URI, null, null, null, sortBy);
			if(c != null){
				if(c.moveToFirst()){
					max = c.getLong(c.getColumnIndex(RssConstant.Content._ID));
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		
		/*
		if(max != 0){
			max = max + 1;
		}
		*/
		max = max + 1;
		return max;
	}
	
	/**liuzhao.wei added**/
	public int getFeedsByWidgetId(int widgetId, List<FeedInfo> list){
        int ret = 0;
        if(list == null){
                return 0;
        }
        Cursor c = null;
        String orderBy = RssConstant.Content.FEED_ID + " asc";
        try{
        	c = query(RssConstant.Content.VIEW_FEED_URI, null, RssConstant.Content.WIDGET_ID + "=" + widgetId, null, orderBy);
	        if(c != null){
	            if(c.moveToFirst()){
	                do{
	                        FeedInfo info = new FeedInfo();
	                        info.widgetId = widgetId;
	                        info.feedId = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_ID));
	                        info.feedTitle = c.getString(c.getColumnIndex(RssConstant.Content.FEED_TITLE));
	                        info.feedUrl = c.getString(c.getColumnIndex(RssConstant.Content.FEED_URL));
	                        info.feedIsBundle = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_IS_BUNDLE));
	                        info.feedPubdate = c.getLong(c.getColumnIndex(RssConstant.Content.FEED_PUBDATE));
	                        info.feedGuid = c.getString(c.getColumnIndex(RssConstant.Content.FEED_GUID));
	                        list.add(info);
	                        ret++;
	                }while(c.moveToNext());
	            }
	        }
        }catch(Exception e){
        	e.printStackTrace();
        }finally{
        	if(c != null){
        		c.close();
        	}
        }
        return ret;
	}
	
	public int getWidgetCount() {
		Cursor cursor = null;
		int count = 0;
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.WIDGET_URI, null, null, null, null);
			if(cursor != null){
				count = cursor.getCount();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
        }
        return count;
	}
	
	
	public int getWidgetInfo(List<WidgetInfo> list) {
        int ret = 0;
        if(list == null){
                return 0;
        }

        Cursor c = null;
        try{
        	c = mContext.getContentResolver().query(RssConstant.Content.WIDGET_URI, null, null, null, null);
	        if(c != null){
	            if(c.moveToFirst()){
	                do{
	                        WidgetInfo info = new WidgetInfo();
	                        info.widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
	                        info.widgetTitle = c.getString(c.getColumnIndex(RssConstant.Content.WIDGET_TITLE));
	                        info.widgetValidaty = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_VALIDITY_TIME));
	                        info.updateFrequency = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_UPDATE_FREQUENCY));
	                        list.add(info);
	                        ret++;
	                }while(c.moveToNext());
	            }
	        }
        }catch(Exception e){
        	e.printStackTrace();
        }finally{
        	if(c != null){
        		c.close();
        	}
    	}
        return ret;
	}
	
	public int getWidgetInfo(WidgetInfo info, int widgetId) {
		int ret = 0;
		if(info == null) {
			return -1;
		}
		Cursor c = null;
		try {
			c = mContext.getContentResolver().query(RssConstant.Content.WIDGET_URI, null, RssConstant.Content.WIDGET_ID + "=" + widgetId, null, null);
			if(c != null && c.getCount() > 0) {
				c.moveToFirst();
				info.widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
                info.widgetTitle = c.getString(c.getColumnIndex(RssConstant.Content.WIDGET_TITLE));
                info.widgetValidaty = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_VALIDITY_TIME));
                info.updateFrequency = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_UPDATE_FREQUENCY));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(c != null){
        		c.close();
        	}
		}
		return ret;
	}
	
	public void deleteItemsOutofDate(int widgetId, long validatyTime) {
		Log.d(TAG, "deleteItemsOutofDate -------------- widgetId = " + widgetId);
		long startTime = System.currentTimeMillis();
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId + " and " + RssConstant.Content.ITEM_PUBDATE + "<" + validatyTime;
		Cursor c = null;
		try{
			c = query(RssConstant.Content.VIEW_ITEM_URI, null, selection, null, null);
			if(c != null){
				Log.d(TAG, "The items of need to deleted Count = " + c.getCount());
				if(c.moveToFirst()){
					do{
						int feedId = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_ID));
						long itemId = c.getInt(c.getColumnIndex(RssConstant.Content.ITEM_ID));
						deleteItem(widgetId, feedId, itemId);
					}while(c.moveToNext());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			long endTime = System.currentTimeMillis();
			Log.d(TAG, "Time consumed by deleteItemsOutofDate is " + (endTime - startTime));
			if(c != null){
				c.close();
			}
		}
	}
	
	public int getHistoryTimes(String url) {
		if(url == null){
			return -1;
		}
		Cursor cursor = null;
		int times = 0;
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.HISTORY_URI, null, "url = '" + url + "'", null, null);
			if(cursor != null) {
				if(cursor.moveToFirst()) {
					times = cursor.getInt(3);
					Log.d(TAG, "times is " + times);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		return times;
	}
	
	public int getHistoryCount(){
		String orderBy = RssConstant.Content._ID + " asc";
		Cursor cursor = null;
		int count = 0;
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.HISTORY_URI, null, null, null, orderBy);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					count = cursor.getCount();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		
		return count;
	}
	
	public void markItemsOutofDate(int widgetId, long validatyTime) {
		Log.d(TAG, "deleteItemsOutofDate -------------- widgetId = " + widgetId);
		String where  = RssConstant.Content.ITEM_ID + " in ( select " + RssConstant.Content.ITEM_ID + " from " + RssConstant.Content.TABLE_ITEM_INDEXS
				+ " inner join " + RssConstant.Content.TABLE_ITEMS + " on item_index_table.item_id = item_table._id and " + RssConstant.Content.ITEM_PUBDATE + "<" + validatyTime + ")"  + " and " + RssConstant.Content.WIDGET_ID + " = "
						+ widgetId;
		ContentValues values = new ContentValues();
		Log.d(TAG, "Mark out of date news : " + where);
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.ITEM_INDEX_URI, values, where, null);
		mContext.getContentResolver().delete(RssConstant.Content.ITEM_INDEX_URI, where, null);
		
		where = RssConstant.Content._ID 
				+ " not in (select " + RssConstant.Content.ITEM_ID + " from " + RssConstant.Content.TABLE_ITEM_INDEXS
				+ " left join " + RssConstant.Content.TABLE_ITEMS + " on item_index_table.item_id = item_table._id)";
		Log.d(TAG, "Delete the news : " + where);
		mContext.getContentResolver().delete(RssConstant.Content.ITEM_URI, where, null);
	}
	
	public int getActivityCount() {
		Cursor cursor = null;
		int count = 0;
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.ACTIVITYCOUNT_URI, null, null, null, null);
			if (cursor != null) {
				if(cursor.getCount() <= 0) {
					ContentValues values = new ContentValues();
					values.put(RssConstant.Content.ACTIVITY_COUNT, 0);
					mContext.getContentResolver().insert(RssConstant.Content.ACTIVITYCOUNT_URI, values);
				} else {
					cursor.moveToFirst();
					count = cursor.getInt(0);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}		
		return count;
	}
	
	public synchronized void resetActivityCount() {
		Cursor cursor = null;
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.ACTIVITYCOUNT_URI, null, null, null, null);
			if (cursor != null) {
				if(cursor.getCount() <= 0) {
					ContentValues values = new ContentValues();
					values.put(RssConstant.Content.ACTIVITY_COUNT, 0);
					mContext.getContentResolver().insert(RssConstant.Content.ACTIVITYCOUNT_URI, values);
				} else {
					ContentValues values = new ContentValues();
					values.put(RssConstant.Content.ACTIVITY_COUNT, 0);
					mContext.getContentResolver().update(RssConstant.Content.ACTIVITYCOUNT_URI, values, null, null);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
	}
	
	public ArrayList<String> getHistoryUrl() {
		String orderBy = RssConstant.Content._ID + " asc";
		Cursor cursor = null;
		ArrayList<String> list = new ArrayList<String>();
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.HISTORY_URI, null, null, null, orderBy);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						list.add(cursor.getString(2));
					} while(cursor.moveToNext());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		return list;
	}
	
	public int deleteHistoryOutofLimit(){
		String orderBy = RssConstant.Content._ID + " asc";
		Cursor cursor = null;
		int count = 0;
		int id = -1;
		try{
			cursor = mContext.getContentResolver().query(RssConstant.Content.HISTORY_URI, null, null, null, orderBy);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					count = cursor.getCount();
					id = cursor.getInt(cursor
							.getColumnIndex(RssConstant.Content._ID));
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor != null){
				cursor.close();
			}
		}
		String where = RssConstant.Content._ID + "=" + id;
		if(count >= RssConstant.Value.MAX_HISTORY){
			return mContext.getContentResolver().delete(RssConstant.Content.HISTORY_URI, where, null); 
		}
		return 0;
	}
	/****************************************************************************************/
	public void deleteDataMarkedDeleted(){
		Log.d(TAG, "deleteDataMarkedDeleted~~~~~~~~~~~~~~~~~~~~~~~~");
		String selection = RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_DELETED;
		Cursor c = null;
		try{
			c = query(RssConstant.Content.WIDGET_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					do{
						synchronized (DatabaseHelper.class) {
							if (mPauseDelete) {
								mPauseDelete = false;
								try {
									Log.d(TAG, "Function deleteDataMarkedDeleted() need waiting");
									DatabaseHelper.class.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						int widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
						deleteWidgetMarkedDeleted(widgetId);
					}while(c.moveToNext());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		
		c = null;
		try{
			c = query(RssConstant.Content.FEED_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					do{
						synchronized (DatabaseHelper.class) {
							if (mPauseDelete) {
								mPauseDelete = false;
								try {
									Log.d(TAG, "Function deleteDataMarkedDeleted() need waiting");
									DatabaseHelper.class.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						int widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
						int feedId = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_ID));
//						deleteFeedMarkedDeleted(widgetId, feedId);
						deleteFeedDirectly(widgetId, feedId);
					}while(c.moveToNext());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		
		c = null;
		try{
			c = query(RssConstant.Content.ITEM_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					do{
						try{
							Thread.sleep(100);
						}catch(Exception e){
							e.printStackTrace();
						}
						synchronized (DatabaseHelper.class) {
							if (mPauseDelete) {
								Log.d(TAG, "Function deleteDataMarkedDeleted() need waiting");
								mPauseDelete = false;
								try {
									DatabaseHelper.class.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						int widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
						int feedId = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_ID));
						long itemId = c.getLong(c.getColumnIndex(RssConstant.Content.ITEM_ID));
//						deleteItemMarkedDeleted(widgetId, feedId, itemId);
						deleteItemDirectly(widgetId, feedId, itemId);
					}while(c.moveToNext());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
	}
	
	
	public void deleteWidgetMarkedDeleted(int widgetId){
		deleteWidgetFromDatabase(widgetId);
	}
	
	public void deleteFeedMarkedDeleted(int widgetId, int feedId){
		/**
		 * delete all item followed by feedId before delete feed
		 */
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId;
		Cursor c = null;
		try{
		c = mContext.getContentResolver().query(RssConstant.Content.ITEM_INDEX_URI, null, selection, null, null);
			if(c != null){
					if(c.moveToFirst()){
						do{
							long itemId = c.getLong(c.getColumnIndex(RssConstant.Content.ITEM_ID));
							deleteItem(widgetId, feedId, itemId);
						}while(c.moveToNext());
					}

			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		selection = RssConstant.Content.FEED_ID + "=" + feedId;
		int feedCount = 0;
		try{
		c = mContext.getContentResolver().query(RssConstant.Content.FEED_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					feedCount = c.getCount();
				}
				if(feedCount <= 1){
					deleteFeedFromDatabase(feedId);
				}
				deleteFeedIndexFromDatabase(widgetId, feedId);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
	}
	public void deleteItemMarkedDeleted(int widgetId, int feedId, long itemId){
/*		Log.d(TAG, "deleteItem --- widgetId = " + widgetId + " , feedId = " + feedId + " , itemId = " + itemId);*/
		String selection = RssConstant.Content.ITEM_ID + "=" + itemId;
		long itemCount = 0;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.ITEM_INDEX_URI, null, selection, null, null);
			if(c != null){
					if(c.moveToFirst()){
						itemCount = c.getCount();
					}
					if(itemCount <= 1){
						deleteItemFromDatabase(itemId);
					}
					deleteItemIndexFromDatabase(widgetId, feedId, itemId);
				}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		
	}
	
	public void updateActivityCount(int count) {
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.ACTIVITY_COUNT, count);
		update(RssConstant.Content.ACTIVITYCOUNT_URI, values, null, null);
	}
	
	public void markWidgetDeleted(int widgetId){
		Log.d(TAG, "markWidgetDeleted");
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId;
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.WIDGET_URI, values, where, null);
	}
	public void markAllWidgetDeleted(){
		Log.d(TAG, "markAllWidgetDeleted");
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.WIDGET_URI, values, null, null);
	}
	public void markFeedDeleted(int widgetId, int feedId){
		Log.d(TAG, "markFeedDeleted");
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId 
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId;
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.FEED_INDEX_URI, values, where, null);
		
		// New added
		mContext.getContentResolver().delete(RssConstant.Content.FEED_INDEX_URI, where, null);
		where = RssConstant.Content._ID 
				+ " not in (select " + RssConstant.Content.FEED_ID + " from " + RssConstant.Content.TABLE_FEED_INDEXS
				+ " left join " + RssConstant.Content.TABLE_FEEDS + " on feed_index_table.feed_id = feed_table._id)";
		mContext.getContentResolver().delete(RssConstant.Content.FEED_URI, where, null);
	}
	public void markAllFeedDeleted(int widgetId){
		Log.d(TAG, "markAllFeedDeleted");
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId;
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.FEED_INDEX_URI, values, where, null);
		
		// New added
		mContext.getContentResolver().delete(RssConstant.Content.FEED_INDEX_URI, where, null);
		where = RssConstant.Content._ID 
				+ " not in (select " + RssConstant.Content.FEED_ID + " from " + RssConstant.Content.TABLE_FEED_INDEXS
				+ " left join " + RssConstant.Content.TABLE_FEEDS + " on feed_index_table.feed_id = feed_table._id)";
		mContext.getContentResolver().delete(RssConstant.Content.FEED_URI, where, null);
	}
	public void markItemDeleted(int widgetId, int feedId){
		Log.d(TAG, "markItemDeleted");
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId 
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId;
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.ITEM_INDEX_URI, values, where, null);
		
		//New added
		mContext.getContentResolver().delete(RssConstant.Content.ITEM_INDEX_URI, where, null);
		where = RssConstant.Content._ID 
				+ " not in (select " + RssConstant.Content.ITEM_ID + " from " + RssConstant.Content.TABLE_ITEM_INDEXS
				+ " left join " + RssConstant.Content.TABLE_ITEMS + " on item_index_table.item_id = item_table._id)";
		mContext.getContentResolver().delete(RssConstant.Content.ITEM_URI, where, null);
	}
	
	public void markAllItemDeleted(int widgetId){
		Log.d(TAG, "markAllItemDeleted");
		String where = RssConstant.Content.WIDGET_ID + "=" + widgetId;
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.ITEM_INDEX_URI, values, where, null);
		
		//New added
		mContext.getContentResolver().delete(RssConstant.Content.ITEM_INDEX_URI, where, null);
		where = RssConstant.Content._ID 
				+ " not in (select " + RssConstant.Content.ITEM_ID + " from " + RssConstant.Content.TABLE_ITEM_INDEXS
				+ " left join " + RssConstant.Content.TABLE_ITEMS + " on item_index_table.item_id = item_table._id)";
		mContext.getContentResolver().delete(RssConstant.Content.ITEM_URI, where, null);
	}
	
	public void markFeedIndexTableDeleted(){
		Log.d(TAG, "markFeedIndexTableDeleted");
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.FEED_INDEX_URI, values, null, null);
	}
	public void markItemIndexTableDeleted(){
		Log.d(TAG, "markItemIndexTableDeleted");
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
		update(RssConstant.Content.ITEM_INDEX_URI, values, null, null);
	}
	public void markRssDeleted(){
		
	}
	public synchronized int update(Uri uri, ContentValues values, String where, String[] selectionArgs){
		return mContext.getContentResolver().update(uri, values, where, selectionArgs);
	}
	public boolean feedAdded(FeedInfo info){
		boolean added = false;
		String guid = String.valueOf(info.feedUrl.hashCode());
		info.feedGuid = guid;
		String selection = RssConstant.Content.FEED_GUID + "=" + guid 
				+ " and " + RssConstant.Content.WIDGET_ID + "=" + info.widgetId
				+ " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED;
		Log.d(TAG, selection);
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.VIEW_FEED_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					added = (c.getCount() > 0);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		return added;	
	}
	
	public boolean itemAdded(int widgetId, int feedId, long itemId){
		boolean added = false;
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId 
				+ " and " + RssConstant.Content.FEED_ID + "=" + feedId
				+ " and " + RssConstant.Content.ITEM_ID + "=" + itemId
				+ " and " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED;
		Cursor c = null;
		try{
			c = query(RssConstant.Content.ITEM_INDEX_URI, null, selection, null, null);
			int count = 0;
			if(c != null){
				count = c.getCount();
			}
/*			Log.d(TAG, "COUNT = " + count);*/
			added = count > 0;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		return added;
	}
	
	public void deleteFeedDirectly(int widgetId, int feedId){
		String selection = RssConstant.Content.FEED_ID + "=" + feedId;
		int feedCount = 0;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.FEED_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					feedCount = c.getCount();
				}
				if(feedCount <= 1){
					deleteFeedFromDatabase(feedId);
				}
				deleteFeedIndexFromDatabase(widgetId, feedId);
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
	}
	
	public void deleteItemDirectly(int widgetId, int feedId, long itemId){
/*		Log.d(TAG, "deleteItem --- widgetId = " + widgetId + " , feedId = " + feedId + " , itemId = " + itemId);*/
		String selection = RssConstant.Content.ITEM_ID + "=" + itemId;
		long itemCount = 0;
		Cursor c = null;
		try{
			c = mContext.getContentResolver().query(RssConstant.Content.ITEM_INDEX_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					itemCount = c.getCount();
				}
				if(itemCount <= 1){
					deleteItemFromDatabase(itemId);
				}
				deleteItemIndexFromDatabase(widgetId, feedId, itemId);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		
	}
	
	public synchronized void pauseDeleteOperation(){
		mPauseDelete = true;
	}
	
	
	public int getFeedCount(Context context, int widgetId) {
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
	
	public int getItemCount(Context context, int widgetId) {
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
	
	public int getUnReadItemCount(Context context, int widgetId) {
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId + " and "
					+ RssConstant.Content.ITEM_STATE + "=" + RssConstant.State.STATE_ITEM_UNREAD + " and "
					+ RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED;
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
	public boolean widgetDeleted(int widgetId){
		Cursor c = null;
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId + " and "
				+ RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED;
		boolean exist = false;
		try{
			c = query(RssConstant.Content.WIDGET_URI, null, selection, null, null);
			if(c != null){
				exist = c.getCount() > 0;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}finally{
			if(c != null){
				c.close();
			}
		}
		return exist;
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
