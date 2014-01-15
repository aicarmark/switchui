package com.motorola.mmsp.rss.service.database;

import com.motorola.mmsp.rss.common.RssConstant;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class RssContentProvider extends ContentProvider {
	public final String TAG = "RssContentProvider";

	private SqlDatabaseHelper mSQLDatabaseHelper = null;
	
	
	private static final int WIDGET_TABLE = 1;
    private static final int WIDGET_TABLE_ID = 2;
	private static final int FEED_TABLE = 3;  
    private static final int FEED_TABLE_ID = 4; 
    private static final int ITEM_TABLE = 5;
    private static final int ITEM_TABLE_ID = 6;
    private static final int ITEM_INDEX_TABLE = 7;
    private static final int ITEM_INDEX_TABLE_ID = 8;
    private static final int HISTORY_TABLE = 9;
    private static final int HISTORY_TABLE_ID = 10;
    private static final int FEED_VIEW = 11;
    private static final int ITEM_VIEW = 12;
    private static final int FEED_INDEX_TABLE = 13;
    private static final int FEED_INDEX_TABLE_ID = 14;
    private static final int ACTIVITY_COUNT_TABLE = 15;
    private static final int ITEM_LIMITED_VIEW = 16;
    
    private static final String QUERY_LIMIT = "40";
    private static final UriMatcher sUriMatcher;     
    static {  
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_WIDGETS, WIDGET_TABLE);  
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_WIDGETS + "/#", WIDGET_TABLE_ID);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_FEEDS, FEED_TABLE);  
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_FEEDS + "/#", FEED_TABLE_ID);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_ITEMS, ITEM_TABLE);  
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_ITEMS + "/#", ITEM_TABLE_ID);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_ITEM_INDEXS, ITEM_INDEX_TABLE);  
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_ITEM_INDEXS + "/#", ITEM_INDEX_TABLE_ID);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_HISTORYS, HISTORY_TABLE);  
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_HISTORYS + "/#", HISTORY_TABLE_ID);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_ACTIVITYCOUNT, ACTIVITY_COUNT_TABLE);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.VIEW_QUERY_FEED, FEED_VIEW); 
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.VIEW_QUERY_ITEM, ITEM_VIEW);  
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_FEED_INDEXS, FEED_INDEX_TABLE);  
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.TABLE_FEED_INDEXS + "/#", FEED_INDEX_TABLE_ID);
        
        sUriMatcher.addURI(RssConstant.Content.AUTHORITY, RssConstant.Content.VIEW_LIMITED_ITEM, ITEM_LIMITED_VIEW);
    }
    
    @Override
	public boolean onCreate() {
		mSQLDatabaseHelper = new SqlDatabaseHelper(getContext());
		if(mSQLDatabaseHelper != null){
			return true;
		}
		return false;
	}
    
    @Override
	public String getType(Uri uri) {
		Log.d(TAG, "getType");
		switch(sUriMatcher.match(uri)){
		case WIDGET_TABLE:
			return RssConstant.Content.WIDGET_CONTENT_TYPE;
		case WIDGET_TABLE_ID:
			return RssConstant.Content.WIDGET_CONTENT_ITEM_TYPE;
		case FEED_TABLE:
			return RssConstant.Content.FEED_CONTENT_TYPE;
		case FEED_TABLE_ID:
			return RssConstant.Content.FEED_CONTENT_ITEM_TYPE;
		case ITEM_TABLE:
			return RssConstant.Content.ITEM_CONTENT_TYPE;
		case ITEM_TABLE_ID:
			return RssConstant.Content.ITEM_CONTENT_ITEM_TYPE;
		case ITEM_INDEX_TABLE:
			return RssConstant.Content.INDEX_CONTENT_TYPE;
		case ITEM_INDEX_TABLE_ID:
			return RssConstant.Content.INDEX_CONTENT_ITEM_TYPE;
		case HISTORY_TABLE:
			return RssConstant.Content.HISTORY_CONTENT_TYPE;
		case HISTORY_TABLE_ID:
			return RssConstant.Content.HISTORY_CONTENT_ITEM_TYPE;
		case ACTIVITY_COUNT_TABLE:
			return RssConstant.Content.ACTIVITY_COUNT_CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);		
		}
	}
    
	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		SQLiteDatabase db = mSQLDatabaseHelper.getReadableDatabase();
		int ret = -1;
		long id = -1;
		try{
			switch(sUriMatcher.match(uri)){
			case WIDGET_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_WIDGETS, whereClause, whereArgs);
				notifyWidgetTableChanged();
				break;
			case WIDGET_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.delete(RssConstant.Content.TABLE_WIDGETS, RssConstant.Content._ID + "=" + id, whereArgs);
				notifyWidgetTableChanged();
				break;
			case FEED_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_FEEDS, whereClause, whereArgs);
				notifyFeedTableChanged();
				break;
			case FEED_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.delete(RssConstant.Content.TABLE_FEEDS, RssConstant.Content._ID + "=" + id, whereArgs);
				notifyFeedTableChanged();
				break;
			case ITEM_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_ITEMS, whereClause, whereArgs);
				notifyItemTableChanged();
				break;
			case ITEM_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.delete(RssConstant.Content.TABLE_ITEMS, RssConstant.Content._ID + "=" + id, whereArgs);
				notifyItemTableChanged();
				break;
			case ITEM_INDEX_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_ITEM_INDEXS, whereClause, whereArgs);
//				notifyWidgetTableChanged();
				break;
			case FEED_INDEX_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_FEED_INDEXS, whereClause, whereArgs);
//				notifyWidgetTableChanged();
				break;
			case HISTORY_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_HISTORYS, whereClause, whereArgs);
				notifyHistoryTableChanged();
				break;
			case HISTORY_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.delete(RssConstant.Content.TABLE_HISTORYS, RssConstant.Content._ID + "=" + id, whereArgs);
				notifyHistoryTableChanged();
				break;
			case ACTIVITY_COUNT_TABLE:
				ret = db.delete(RssConstant.Content.TABLE_ACTIVITYCOUNT, whereClause, whereArgs);
				notifyActivityCountTableChanged();
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);		
			}
		}catch(SQLException e){
			Log.d(TAG, e.getMessage());
			return 0;
		}
		return ret;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mSQLDatabaseHelper.getReadableDatabase();
		long id = -1;
		try{
			switch(sUriMatcher.match(uri)){
			case WIDGET_TABLE:
				id = db.insert(RssConstant.Content.TABLE_WIDGETS, RssConstant.Content.FOO, values);
				notifyWidgetTableChanged();
			break;
			case FEED_TABLE:
				id = db.insert(RssConstant.Content.TABLE_FEEDS, RssConstant.Content.FOO, values);
				notifyFeedTableChanged();
				break;
			case ITEM_TABLE:
				id = db.insert(RssConstant.Content.TABLE_ITEMS, RssConstant.Content.FOO, values);
				notifyItemTableChanged();
				break;
			case ITEM_INDEX_TABLE:
				id = db.insert(RssConstant.Content.TABLE_ITEM_INDEXS, RssConstant.Content.FOO, values);
//				notifyIndexTableChanged();
				break;
			case FEED_INDEX_TABLE:
				id = db.insert(RssConstant.Content.TABLE_FEED_INDEXS, RssConstant.Content.FOO, values);
//				notifyIndexTableChanged();
				break;
			case HISTORY_TABLE:
				id = db.insert(RssConstant.Content.TABLE_HISTORYS, RssConstant.Content.FOO, values);
				notifyHistoryTableChanged();
				break;
			case ACTIVITY_COUNT_TABLE:
				id = db.insert(RssConstant.Content.TABLE_ACTIVITYCOUNT, RssConstant.Content.FOO, values);
				notifyActivityCountTableChanged();
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);		
			}
		}catch(SQLException e){
			Log.d(TAG, e.getMessage());
			return null;
		}
		Uri resultUri = ContentUris.withAppendedId(uri, id);
		return resultUri;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mSQLDatabaseHelper.getReadableDatabase();
		Cursor c = null;
		long id = -1;
		try{
			switch(sUriMatcher.match(uri)){
			case WIDGET_TABLE:
				c = db.query(RssConstant.Content.TABLE_WIDGETS, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case WIDGET_TABLE_ID:
				id = ContentUris.parseId(uri);
				c = db.query(RssConstant.Content.TABLE_WIDGETS, projection, RssConstant.Content._ID + "=" + id, selectionArgs, null, null, sortOrder);
				break;
			case FEED_TABLE:
				c = db.query(RssConstant.Content.TABLE_FEEDS, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case FEED_TABLE_ID:
				id = ContentUris.parseId(uri);
				c = db.query(RssConstant.Content.TABLE_FEEDS, projection, RssConstant.Content._ID + "=" + id, selectionArgs, null, null, sortOrder);
				break;
			case ITEM_TABLE:
				c = db.query(RssConstant.Content.TABLE_ITEMS, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case ITEM_TABLE_ID:
				id = ContentUris.parseId(uri);
				c = db.query(RssConstant.Content.TABLE_ITEMS, projection, RssConstant.Content._ID + "=" + id, selectionArgs, null, null, sortOrder);
				break;
			case ITEM_INDEX_TABLE:
				c = db.query(true, RssConstant.Content.TABLE_ITEM_INDEXS, projection, selection, selectionArgs, null, null, sortOrder, null);
				break;
			case FEED_INDEX_TABLE:
				c = db.query(true, RssConstant.Content.TABLE_FEED_INDEXS, projection, selection, selectionArgs, null, null, sortOrder, null);
				break;
			case HISTORY_TABLE:
				c = db.query(RssConstant.Content.TABLE_HISTORYS, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case HISTORY_TABLE_ID:
				id = ContentUris.parseId(uri);
				c = db.query(RssConstant.Content.TABLE_HISTORYS, projection, RssConstant.Content._ID + "=" + id, selectionArgs, null, null, sortOrder);
				break;
			case FEED_VIEW:
				c = db.query(true, RssConstant.Content.VIEW_QUERY_FEED, projection, selection, selectionArgs, null, null, sortOrder, null);
				break;
			case ITEM_VIEW:
				c = db.query(true, RssConstant.Content.VIEW_QUERY_ITEM, projection, selection, selectionArgs, null, null, sortOrder, null);
				break;
			case ITEM_LIMITED_VIEW:
				c = db.query(true, RssConstant.Content.VIEW_QUERY_ITEM, projection, selection, selectionArgs, null, null, sortOrder, QUERY_LIMIT);
				break;
			case ACTIVITY_COUNT_TABLE:
				c = db.query(RssConstant.Content.TABLE_ACTIVITYCOUNT, projection, selection, selectionArgs, null, null, sortOrder);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);		
			}
		}catch(SQLException e){
			Log.d(TAG, e.getMessage());
			return null;
		}
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int ret = -1;
		long id = -1;
		SQLiteDatabase db = mSQLDatabaseHelper.getReadableDatabase();
		try{
			switch(sUriMatcher.match(uri)){
			case WIDGET_TABLE:
				ret = db.update(RssConstant.Content.TABLE_WIDGETS, values, selection, selectionArgs);
				notifyWidgetTableChanged();
				break;
			case WIDGET_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.update(RssConstant.Content.TABLE_WIDGETS, values, RssConstant.Content._ID + "=" + id, selectionArgs);
				notifyWidgetTableChanged();
				break;
			case FEED_TABLE:
				ret = db.update(RssConstant.Content.TABLE_FEEDS, values, selection, selectionArgs);
				notifyFeedTableChanged();
				break;
			case FEED_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.update(RssConstant.Content.TABLE_FEEDS, values, RssConstant.Content._ID + "=" + id, selectionArgs);
				notifyFeedTableChanged();
				break;
			case ITEM_TABLE:
				ret = db.update(RssConstant.Content.TABLE_ITEMS, values, selection, selectionArgs);
				notifyItemTableChanged();
				break;
			case ITEM_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.update(RssConstant.Content.TABLE_ITEMS, values, RssConstant.Content._ID + "=" + id, selectionArgs);
				notifyItemTableChanged();
				break;
			case FEED_INDEX_TABLE:
				ret = db.update(RssConstant.Content.TABLE_FEED_INDEXS, values, selection, selectionArgs);
//				notifyFeedIndexTableChanged();
				break;
			case ITEM_INDEX_TABLE:
				ret = db.update(RssConstant.Content.TABLE_ITEM_INDEXS, values, selection, selectionArgs);
				notifyIndexTableChanged();
//				notifyItemIndexTableChanged();
				break;
			case HISTORY_TABLE:
				ret = db.update(RssConstant.Content.TABLE_HISTORYS, values, selection, selectionArgs);
				notifyHistoryTableChanged();
				break;
			case HISTORY_TABLE_ID:
				id = ContentUris.parseId(uri);
				ret = db.update(RssConstant.Content.TABLE_HISTORYS, values, RssConstant.Content._ID + "=" + id, selectionArgs);
				notifyHistoryTableChanged();
				break;
			case ACTIVITY_COUNT_TABLE:
				ret = db.update(RssConstant.Content.TABLE_ACTIVITYCOUNT, values, selection, selectionArgs);
				notifyActivityCountTableChanged();
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);		
			}
		}catch(SQLException e){
			Log.d(TAG, e.getMessage());
			return -1;
		}
		return ret;
	}
	
	protected void notifyWidgetTableChanged(){
		getContext().getContentResolver().notifyChange(RssConstant.Content.WIDGET_URI, null);
	}
	protected void notifyFeedTableChanged(){
		getContext().getContentResolver().notifyChange(RssConstant.Content.FEED_URI, null);
	}
	protected void notifyItemTableChanged(){
		getContext().getContentResolver().notifyChange(RssConstant.Content.ITEM_URI, null);
	}
	protected void notifyIndexTableChanged(){
		getContext().getContentResolver().notifyChange(RssConstant.Content.ITEM_INDEX_URI, null);
	}
	protected void notifyHistoryTableChanged(){
		getContext().getContentResolver().notifyChange(RssConstant.Content.HISTORY_URI, null);
	}
	
	protected void notifyActivityCountTableChanged() {
		getContext().getContentResolver().notifyChange(RssConstant.Content.ACTIVITYCOUNT_URI, null);
	}

}
