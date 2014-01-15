package com.motorola.mmsp.rss.service.database;

import com.motorola.mmsp.rss.common.RssConstant;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SqlDatabaseHelper extends SQLiteOpenHelper {

	public String TAG = "SQLDatabaseHelper";

	/**
	private String TRIGGER_FEED = "trigger_feed";
	private String TRIGGER_WIDGET = "trigger_widget";
	private String TRIGGER_ITEM = "trigger_items";
	private String VIEW_QUERY_FEED = "view_query_feed";
	private String VIEW_QUERY_ITEM = "view_query_item";
	*/

	private final String WIDGETS_TABLE_CREATE = 
			"create table if not exists " + RssConstant.Content.TABLE_WIDGETS
			+ "("
			+ RssConstant.Content._ID + " integer primary key autoincrement,"
			+ RssConstant.Content.WIDGET_ID + " integer,"
			+ RssConstant.Content.WIDGET_TITLE + " text," 
			+ RssConstant.Content.WIDGET_UPDATE_FREQUENCY + " integer default 0," 
			+ RssConstant.Content.WIDGET_VALIDITY_TIME + " integer default 0,"
			+ RssConstant.Content.DELETED + " integer default 0,"
			+ RssConstant.Content.FOO + " text" 
			+ ")";

	private final String FEED_TABLE_CREATE = 
		"create table if not exists " + RssConstant.Content.TABLE_FEEDS 
		+ "(" 
		+ RssConstant.Content._ID + " integer primary key autoincrement,"
		+ RssConstant.Content.FEED_TITLE + " text," 
		+ RssConstant.Content.FEED_URL + " text," 
		+ RssConstant.Content.FEED_IS_BUNDLE + " integer,"
		+ RssConstant.Content.FEED_ICON + " blob,"
		+ RssConstant.Content.FEED_PUBDATE + " long,"
		+ RssConstant.Content.FEED_GUID + " text,"
		+ RssConstant.Content.FOO + " text" 
		+ ")";


	private final String ITEM_TABLE_CREATE = 
		"create table if not exists " + RssConstant.Content.TABLE_ITEMS 
		+ "("
		+ RssConstant.Content._ID + " integer primary key autoincrement," 
		+ RssConstant.Content.ITEM_TITLE + " text," 
		+ RssConstant.Content.ITEM_URL + " text,"
		+ RssConstant.Content.ITEM_DESCRIPTION + " text,"
		+ RssConstant.Content.ITEM_DES_BRIEF + " text," 
		+ RssConstant.Content.ITEM_GUID + " text,"
		+ RssConstant.Content.ITEM_AUTHOR + " text,"
		+ RssConstant.Content.ITEM_PUBDATE + " long not null,"
		+ RssConstant.Content.FOO + " text" 
		+ ")";

	/**
	private final String FEED_INDEX_TABLE_CREATE = 
		"create table if not exists " + RssConstant.Content.TABLE_FEED_INDEXS
		+"("
		+ RssConstant.Content._ID + " integer,"
		+ RssConstant.Content.WIDGET_ID + " integer references " + RssConstant.Content.TABLE_WIDGETS + "(" + RssConstant.Content.WIDGET_ID + "),"
		+ RssConstant.Content.FEED_ID + " integer references " + RssConstant.Content.TABLE_FEEDS + "(" + RssConstant.Content._ID + "),"
		+ "primary key (" + RssConstant.Content.WIDGET_ID + "," + RssConstant.Content.FEED_ID + "))";
	
	private final String ITEM_INDEX_TABLE_CREATE = 
		"create table if not exists " + RssConstant.Content.TABLE_ITEM_INDEXS
		+"("
		+ RssConstant.Content._ID + " integer,"
		+ RssConstant.Content.WIDGET_ID + " integer references " + RssConstant.Content.TABLE_WIDGETS + "(" + RssConstant.Content.WIDGET_ID + "),"
		+ RssConstant.Content.FEED_ID + " integer references " + RssConstant.Content.TABLE_FEEDS + "(" + RssConstant.Content._ID + "),"
		+ RssConstant.Content.ITEM_ID + " long references " + RssConstant.Content.TABLE_ITEMS + "(" + RssConstant.Content._ID +"),"
		+ RssConstant.Content.ITEM_STATE + " integer default 0,"
		+ "primary key (" + RssConstant.Content.WIDGET_ID + "," + RssConstant.Content.FEED_ID + "," + RssConstant.Content.ITEM_ID + "))";
	 */
	private final String FEED_INDEX_TABLE_CREATE = 
			"create table if not exists " + RssConstant.Content.TABLE_FEED_INDEXS
			+"("
			+ RssConstant.Content._ID + " integer primary key autoincrement,"
			+ RssConstant.Content.WIDGET_ID + " integer references " + RssConstant.Content.TABLE_WIDGETS + "(" + RssConstant.Content.WIDGET_ID + "),"
			+ RssConstant.Content.FEED_ID + " integer references " + RssConstant.Content.TABLE_FEEDS + "(" + RssConstant.Content._ID + "),"
			+ RssConstant.Content.DELETED + " integer default 0,"
			+ RssConstant.Content.FOO + " text" 
			+ ")";
		
		private final String ITEM_INDEX_TABLE_CREATE = 
			"create table if not exists " + RssConstant.Content.TABLE_ITEM_INDEXS
			+"("
			+ RssConstant.Content._ID + " integer primary key autoincrement,"
			+ RssConstant.Content.WIDGET_ID + " integer references " + RssConstant.Content.TABLE_WIDGETS + "(" + RssConstant.Content.WIDGET_ID + "),"
			+ RssConstant.Content.FEED_ID + " integer references " + RssConstant.Content.TABLE_FEEDS + "(" + RssConstant.Content._ID + "),"
			+ RssConstant.Content.ITEM_ID + " long references " + RssConstant.Content.TABLE_ITEMS + "(" + RssConstant.Content._ID +"),"
			+ RssConstant.Content.ITEM_STATE + " integer default 0,"
			+ RssConstant.Content.DELETED + " integer default 0,"
			+ RssConstant.Content.FOO + " text" 
			+ ")";


	private final String HISTORYS_TABLE_CREATE = 
		"create table if not exists " + RssConstant.Content.TABLE_HISTORYS
		+ "("
		+ RssConstant.Content._ID + " integer primary key autoincrement," 
		+ RssConstant.Content.HISTORY_TITLE + " text," 
		+ RssConstant.Content.HISTORY_URL + " text," 
		+ RssConstant.Content.HISTORY_TIMES + " integer default 0,"
		+ RssConstant.Content.HISTORY_DATE + " long,"
		+ RssConstant.Content.FOO + " text" 
		+ ")";
	
	private final String ACTIVITYCOUNT_TABLE_CREATE = "create table if not exists " +  RssConstant.Content.TABLE_ACTIVITYCOUNT
			+ "("
			+ RssConstant.Content.ACTIVITY_COUNT + " integer integer default 0,"
			+ RssConstant.Content.FOO + " text" 
			+ ")";
	/**
	private final String QUERY_ITEMS_VIEW = 
		"create view " + RssConstant.Content.VIEW_QUERY_ITEM + " as select "
				+ RssConstant.Content.TABLE_INDEXS + "." + RssConstant.Content.WIDGET_ID + " as " + RssConstant.Content.VIEW_WIDGET_ID
				+ " , " + RssConstant.Content.TABLE_INDEXS + "." + RssConstant.Content.ITEM_ID + " as " + RssConstant.Content.VIEW_ITEM_ID
				+ " , " + RssConstant.Content.TABLE_INDEXS + "." + RssConstant.Content.ITEM_STATE + " as " + RssConstant.Content.VIEW_ITEM_STATE
				+ " , " + RssConstant.Content.TABLE_ITEMS + "." + RssConstant.Content.ITEM_TITLE + " as " + RssConstant.Content.VIEW_ITEM_TITLE
				+ " , " + RssConstant.Content.TABLE_ITEMS + "." + RssConstant.Content.ITEM_URL + " as " + RssConstant.Content.VIEW_ITEM_URL
				+ " , " + RssConstant.Content.TABLE_ITEMS + "." + RssConstant.Content.ITEM_DESCRIPTION + " as " + RssConstant.Content.VIEW_ITEM_DESCRIPTION
				+ " , " + RssConstant.Content.TABLE_ITEMS + "." + RssConstant.Content.ITEM_AUTHOR + " as " + RssConstant.Content.VIEW_ITEM_AUTHOR
				+ " , " + RssConstant.Content.TABLE_ITEMS + "." + RssConstant.Content.ITEM_PUBDATE + " as " + RssConstant.Content.VIEW_ITEM_PUBDATE
				+ " from (select * from " + RssConstant.Content.TABLE_INDEXS + ") as indexs " + "left join" 
				+ " (select * from " + RssConstant.Content.TABLE_ITEMS + ") as items "
				+ " on indexs." + RssConstant.Content.ITEM_ID + "" + "=" + "items." + RssConstant.Content._ID + "";
	*/
	private final String VIEW_FEEDS_CREATE = 
			 "create view " + RssConstant.Content.VIEW_QUERY_FEED + " as select "
					+         RssConstant.Content._ID
					+ " , " + RssConstant.Content.WIDGET_ID
					+ " , " + RssConstant.Content.FEED_ID
					+ " , " + RssConstant.Content.DELETED
					+ " , " + RssConstant.Content.FEED_TITLE
					+ " , " + RssConstant.Content.FEED_URL
					+ " , " + RssConstant.Content.FEED_IS_BUNDLE
					+ " , " + RssConstant.Content.FEED_ICON
					+ " , " + RssConstant.Content.FEED_PUBDATE
					+ " , " + RssConstant.Content.FEED_GUID
					+ " from (select * from " + RssConstant.Content.TABLE_FEED_INDEXS 
					+ " where " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED
					+ ") as indexfeeds " + "left join"
					+ " (select "
					+ RssConstant.Content._ID + " as " + "feed_id_view"
					+ " , " + RssConstant.Content.FEED_TITLE
					+ " , " + RssConstant.Content.FEED_URL
					+ " , " + RssConstant.Content.FEED_IS_BUNDLE
					+ " , " + RssConstant.Content.FEED_ICON
					+ " , " + RssConstant.Content.FEED_PUBDATE
					+ " , " + RssConstant.Content.FEED_GUID
					+ " from " + RssConstant.Content.TABLE_FEEDS + ") as feeds "
					+ " on indexfeeds." + RssConstant.Content.FEED_ID + "=" + "feeds." + "feed_id_view";
 	private final String VIEW_ITEMS_CREATE = 
			 "create view " + RssConstant.Content.VIEW_QUERY_ITEM + " as select "
			 		+         RssConstant.Content._ID
					+ " , " + RssConstant.Content.WIDGET_ID
					+ " , " + RssConstant.Content.FEED_ID
					+ " , " + RssConstant.Content.DELETED
					+ " , " + RssConstant.Content.FEED_TITLE
					+ " , " + RssConstant.Content.FEED_URL
					+ " , " + RssConstant.Content.FEED_IS_BUNDLE
					+ " , " + RssConstant.Content.FEED_ICON
					+ " , " + RssConstant.Content.FEED_PUBDATE
					+ " , " + RssConstant.Content.FEED_GUID
					+ " , " + RssConstant.Content.ITEM_ID 
					+ " , " + RssConstant.Content.ITEM_STATE
					+ " , " + RssConstant.Content.ITEM_TITLE
					+ " , " + RssConstant.Content.ITEM_URL
					+ " , " + RssConstant.Content.ITEM_DESCRIPTION
					+ " , " + RssConstant.Content.ITEM_DES_BRIEF
					+ " , " + RssConstant.Content.ITEM_AUTHOR
					+ " , " + RssConstant.Content.ITEM_PUBDATE
					+ " from (select * "			
					+ " from " + RssConstant.Content.TABLE_ITEM_INDEXS 
					+ " where " + RssConstant.Content.DELETED + "=" + RssConstant.State.STATE_UNDELETED
					+ ") as indexitems" 
					+ "	left join" 
					+ " (select "
					+ RssConstant.Content._ID + " as " + "feed_id_view"
					+ " , " + RssConstant.Content.FEED_TITLE
					+ " , " + RssConstant.Content.FEED_URL
					+ " , " + RssConstant.Content.FEED_IS_BUNDLE
					+ " , " + RssConstant.Content.FEED_ICON
					+ " , " + RssConstant.Content.FEED_PUBDATE
					+ " , " + RssConstant.Content.FEED_GUID
					+ " from " + RssConstant.Content.TABLE_FEEDS + ") as feeds "
					+ " on indexitems." + RssConstant.Content.FEED_ID + "=" + "feeds." + "feed_id_view"
					+ "	left join"
					+ " (select "
					+ RssConstant.Content._ID + " as " + "item_id_view"
					+ " , " + RssConstant.Content.ITEM_TITLE
					+ " , " + RssConstant.Content.ITEM_URL
					+ " , " + RssConstant.Content.ITEM_DESCRIPTION
					+ " , " + RssConstant.Content.ITEM_DES_BRIEF
					+ " , " + RssConstant.Content.ITEM_GUID
					+ " , " + RssConstant.Content.ITEM_AUTHOR
					+ " , " + RssConstant.Content.ITEM_PUBDATE
					+ " from " + RssConstant.Content.TABLE_ITEMS + ") as items "
					+ " on indexitems." + RssConstant.Content.ITEM_ID + "=" + "items." + "item_id_view";

	private final String WIDGET_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_WIDGETS + " if exists";
	private final String FEED_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_FEEDS + " if exists";
	private final String ITEM_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_ITEMS + " if exists";
	private final String FEED_INDEX_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_FEED_INDEXS + " if exists";
	private final String ITEM_INDEX_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_ITEM_INDEXS + " if exists";
	private final String ACTIVITYCOUNT_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_ACTIVITYCOUNT + " if exists";
	private final String HISTORY_TABLE_DROP = "drop table " + RssConstant.Content.TABLE_HISTORYS + " if exists";
	private final String VIEW_FEED_DROP = "drop view " + RssConstant.Content.VIEW_QUERY_FEED + " if exists";
	private final String VIEW_ITEM_DROP = "drop view " + RssConstant.Content.VIEW_QUERY_ITEM + " if exists";

	public SqlDatabaseHelper(Context context) {
		super(context, RssConstant.Content.DB_NAME, null, RssConstant.Content.DB_VERSION);
		Log.d(TAG, "SQLDatabaseHelper --- DB_NAME = " + RssConstant.Content.DB_NAME);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "SQLDatabaseHelper --- onCreate");
		try{
			db.execSQL(WIDGETS_TABLE_CREATE);
			db.execSQL(FEED_INDEX_TABLE_CREATE);
			db.execSQL(ITEM_INDEX_TABLE_CREATE);
			db.execSQL(FEED_TABLE_CREATE);
			db.execSQL(ITEM_TABLE_CREATE);
			db.execSQL(HISTORYS_TABLE_CREATE);
			db.execSQL(ACTIVITYCOUNT_TABLE_CREATE);
			Log.d(TAG, VIEW_ITEMS_CREATE);
			
			
			
			db.execSQL(VIEW_FEEDS_CREATE);
			db.execSQL(VIEW_ITEMS_CREATE);
		} catch(SQLException e){
			Log.d(TAG, "create table failed");
			e.printStackTrace();
		} finally{
			
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "SQLDatabaseHelper --- onUpgrade oldVersion = " + oldVersion + " , newVersion = " + newVersion);
		if(newVersion > oldVersion){
			try{
				db.execSQL(WIDGET_TABLE_DROP);
				db.execSQL(FEED_TABLE_DROP);
				db.execSQL(ITEM_TABLE_DROP);
				db.execSQL(FEED_INDEX_TABLE_DROP);
				db.execSQL(ITEM_INDEX_TABLE_DROP);
				db.execSQL(HISTORY_TABLE_DROP);
				db.execSQL(ACTIVITYCOUNT_TABLE_DROP);
				db.execSQL(VIEW_FEED_DROP);
				db.execSQL(VIEW_ITEM_DROP);
			} catch(SQLException e){
				e.printStackTrace();
			} finally{
				onCreate(db);
			}
		}
	}

}
