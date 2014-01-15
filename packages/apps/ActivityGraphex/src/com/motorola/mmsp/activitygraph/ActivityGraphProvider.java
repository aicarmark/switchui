package com.motorola.mmsp.activitygraph;

import java.util.Map;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.motorola.mmsp.activitygraph.AppRank.Apps;
import com.motorola.mmsp.activitygraph.AppRank.ManualList;
import com.motorola.mmsp.activitygraph.AppRank.Ranks;

public class ActivityGraphProvider extends ContentProvider {
    private SQLiteOpenHelper mOpenHelper;
    private SQLiteDatabase mDatabase;
    private static final String DATABASE_NAME = "graphy.db";
    private static final int DATABASE_VERSION = 8;
    private static final String TAG = "GraphProvider";
    private static final UriMatcher graphyUri = new UriMatcher(UriMatcher.NO_MATCH);
    
    private static final int ALL_APPS = 0;
    private static final int ONE_APP = 1;
    private static final int ALL_RANKS = 2;
    private static final int ONE_RANK = 3;
    private static final int ALL_MANUALS = 4;
    private static final int ONE_MANUAL = 5;
    private static final int EXCUTESQL = 6;
    private static final int SET_RANK = 7;
    private static final int UPDATE_RANK = 8;
    
    static {
        graphyUri.addURI(AppRank.AUTHORITY, Apps.TABLE_NAME, ALL_APPS);
        graphyUri.addURI(AppRank.AUTHORITY, Apps.TABLE_NAME + "/#", ONE_APP);
        graphyUri.addURI(AppRank.AUTHORITY, Ranks.TABLE_NAME, ALL_RANKS);
        graphyUri.addURI(AppRank.AUTHORITY, Ranks.TABLE_NAME + "/#", ONE_RANK);
        graphyUri.addURI(AppRank.AUTHORITY, ManualList.TABLE_NAME, ALL_MANUALS);
        graphyUri.addURI(AppRank.AUTHORITY, ManualList.TABLE_NAME + "/#", ONE_MANUAL);
        graphyUri.addURI(AppRank.AUTHORITY, Tables.EXCUTESQL, EXCUTESQL);
        graphyUri.addURI(AppRank.AUTHORITY, Tables.SET_ONE_RANK, SET_RANK);
        graphyUri.addURI(AppRank.AUTHORITY, Tables.UPDAT_ALL_RANK, UPDATE_RANK);
        
    }
    
    private static final String[] contentType = {
        Apps.CONTENT_TYPE,
        Apps.CONTENT_ITEM_TYPE,
        Ranks.CONTENT_TYPE,
        Ranks.CONTENT_ITEM_TYPE,
        ManualList.CONTENT_TYPE,
        ManualList.CONTENT_ITEM_TYPE,
    };
    
    public interface Tables {
        public static final String DAILY_RANK = "daily_rank";
        public static final String HIRERA_HISTORY_RANK = "hirera_history_rank";
        public static final String MANUAL_APP = "manual_app";
        public static final String ALL_APP = "app_list";
        public static final String EXCUTESQL = "execSql";
        public static final String SET_ONE_RANK = "set_rank";
        public static final String UPDAT_ALL_RANK = "set_all_rank";
    }
    
    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }
    
    @Override
    public String getType(Uri arg0) {
        // TODO Auto-generated method stub
        /*SqlArguments args = new SqlArguments(arg0, null, null);
		if (TextUtils.isEmpty(args.where)) {
			return "vnd.android.cursor.dir/" + args.table;
		} else {
			return "vnd.android.cursor.item/" + args.table;
		}*/
        final int match = graphyUri.match(arg0);
        if (match < 0 && match >= contentType.length) {
            return null;
        }
        return contentType[match];
    }
    public synchronized SQLiteDatabase getDatabase(Context context) {
        if (mDatabase != null) {
            return mDatabase;
        }
        if (mOpenHelper == null) {
            mOpenHelper = new DatabaseHelper(context);
        }
	// Modifyed by amt_chenjing for switchui-2192 20120715 begin
        try {
            mDatabase = mOpenHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	// Modifyed by amt_chenjing for switchui-2192 20120715 end 
        return mDatabase;
    }
    
    public SQLiteDatabase getReadableDatabase(Context context) {
        SQLiteDatabase db = null;
        if (mOpenHelper != null) {
            db = mOpenHelper.getReadableDatabase();
        }
        return db;
    }
    
    @Override
    public Uri insert(Uri url, ContentValues init) {
        // TODO Auto-generated method stub
        String table = null;
        Uri origin = null;
        ContentValues value;
        if (init != null) {
            value = new ContentValues(init);
        } else {
            value = new ContentValues();
        }
        
        SQLiteDatabase db;
	// Modifyed by amt_chenjing for switchui-2192 20120715 begin
        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
	// Modifyed by amt_chenjing for switchui-2192 20120715 end
        long rowId = 0;
        switch(graphyUri.match(url)){
        case ALL_APPS:
        case ONE_APP:
            table = Apps.TABLE_NAME;
            origin = Apps.CONTENT_URI;
            rowId = db.insert(table, Apps.COMPONENT, value);
            break;
        case ALL_RANKS:
        case ONE_RANK:
            table = Ranks.TABLE_NAME;
            origin = Ranks.CONTENT_URI;
            rowId = db.insert(table, null, value);
            break;
        case ALL_MANUALS:
        case ONE_MANUAL:
            table = ManualList.TABLE_NAME;
            origin = ManualList.CONTENT_URI;
            rowId = db.insert(table, null, value);
            break;	
        default:
            throw new IllegalArgumentException("cannot delete from uri:"+url);
        }
        
        if (rowId < 0) {
            throw new SQLException("Failed to insert row into"+url);
        }
        
        Uri newUrl = ContentUris.withAppendedId(origin, rowId);
        if (table.equals(ManualList.TABLE_NAME)) {
            getContext().getContentResolver().notifyChange(newUrl, null);
        }
        return newUrl;
    }
    
    @Override
    public int delete(Uri uri, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        SQLiteDatabase db;
	// Modifyed by amt_chenjing for switchui-2192 20120715 begin
        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
	// Modifyed by amt_chenjing for switchui-2192 20120715 end
        int count = 0;
        long rowId = 0;
        switch (graphyUri.match(uri)) {
        case ALL_APPS:
            db.beginTransaction();
            count = db.delete(Apps.TABLE_NAME, arg1, arg2);
            db.setTransactionSuccessful();
            db.endTransaction();
            break;
        case ONE_APP:
            String segment = uri.getPathSegments().get(1);
            //String segment = arg2[0];
            rowId = Long.getLong(segment);
            if (TextUtils.isEmpty(arg1)) {
                arg1 = "_id=" + segment;
            } else {
                arg1 = "_id=" + segment + " AND (" + arg1 + ")";
            }
            count = db.delete(Apps.TABLE_NAME, arg1, arg2);
            break;
        case ALL_RANKS:
            db.beginTransaction();
            count = db.delete(Ranks.TABLE_NAME, arg1, arg2);
            db.setTransactionSuccessful();
            db.endTransaction();
            break;
        case ONE_RANK:
            String segment1 = uri.getPathSegments().get(1);
            rowId = Long.getLong(segment1);
            if (TextUtils.isEmpty(arg1)) {
                arg1 = "_id=" + segment1;
            } else {
                arg1 = "_id=" + segment1 + " AND (" + arg1 + ")";
            }
            count = db.delete(Ranks.TABLE_NAME, arg1, arg2);
            break;
        case ALL_MANUALS:
            db.beginTransaction();
            count = db.delete(ManualList.TABLE_NAME, arg1, arg2);
            db.setTransactionSuccessful();
            db.endTransaction();
            break;
        case ONE_MANUAL:
            String segment2 = arg2[0]; //uri.getPathSegments().get(1);
            rowId = Long.getLong(segment2);
            if (TextUtils.isEmpty(arg1)) {
                arg1 = "_id=" + segment2;
            } else {
                arg1 = "_id=" + segment2 + " AND (" + arg1 + ")";
            }
            count = db.delete(ManualList.TABLE_NAME, arg1, arg2);
            break;	
        default:
            throw new IllegalArgumentException("cannot delete from uri:"+uri);
        }
        if (count>0 && (graphyUri.match(uri) == ALL_MANUALS
                || graphyUri.match(uri) == ONE_MANUAL)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
    
    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection, 
            String[] selectionArgs, String sort) {
        // TODO Auto-generated method stub
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = getDatabase(getContext()); //mOpenHelper.getReadableDatabase();
        // Modifyed by amt_chenjing for switchui-2192 20120715 begin
	if(db == null) {
		Log.v(TAG, "open database: Failed");
		return null;
	}
	// Modifyed by amt_chenjing for switchui-2192 20120715 end
        int match = graphyUri.match(url);
        Cursor cur = null;
        switch (match) {
        case ALL_APPS:
            qb.setTables(Apps.TABLE_NAME);
            cur = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
            break;
        case ONE_APP:
            qb.setTables(Apps.TABLE_NAME);
            qb.appendWhere("_id=");
            qb.appendWhere(url.getPathSegments().get(1));
            cur = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
            break;
        case ALL_RANKS:
            qb.setTables(Ranks.TABLE_NAME);
            cur = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
            break;
        case ONE_RANK:
            qb.setTables(Ranks.TABLE_NAME);
            qb.appendWhere("_id=");
            qb.appendWhere(url.getPathSegments().get(1));
            cur = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
            break;
        case ALL_MANUALS:
            qb.setTables(ManualList.TABLE_NAME);
            cur = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
            break;
        case ONE_MANUAL:
            qb.setTables(ManualList.TABLE_NAME);
            qb.appendWhere("_id=");
            qb.appendWhere(url.getPathSegments().get(1));
            cur = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
            break;
        case EXCUTESQL:
            cur = db.rawQuery(selection, null);
            break;
        default:
            throw new IllegalArgumentException("Unknown URi " + url);
        }
        
        if (cur == null) {
            Log.v(TAG, "query: failed");
        } 		
        return cur;
    }
    
    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        int count = 0;
        long rowId = 0;
        String segment = null;
        int match = graphyUri.match(arg0);
        SQLiteDatabase db;
	// Modifyed by amt_chenjing for switchui-2192 20120715 begin
        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
	// Modifyed by amt_chenjing for switchui-2192 20120715 end
        Log.d(TAG, "update datbase");
        switch (match) {
        case ALL_APPS:
            count = db.update(Apps.TABLE_NAME, arg1, arg2, arg3);
            break;
        case ONE_APP:
            segment = arg0.getPathSegments().get(1);
            //segment = arg3[0];
            rowId = Long.parseLong(segment);
            count = db.update(Apps.TABLE_NAME, arg1, "_id=" + rowId, null);
            break;
        case ALL_RANKS:
            count = db.update(Ranks.TABLE_NAME, arg1, arg2, arg3);
            break;
        case ONE_RANK:
            segment = arg0.getPathSegments().get(1);
            //segment = arg3[0];
            rowId = Long.parseLong(segment);
            count = db.update(Ranks.TABLE_NAME, arg1, "_id=" + rowId, null);
            break;
        case ALL_MANUALS:
            count = db.update(ManualList.TABLE_NAME, arg1, arg2, arg3);
            break;
        case ONE_MANUAL:
            //segment = arg3[0];
            segment = arg0.getPathSegments().get(1);
            rowId = Long.parseLong(segment);
            count = db.update(ManualList.TABLE_NAME, arg1, "_id=" + rowId, null);
            break;
        case EXCUTESQL:
            db.execSQL(arg2);
            count = 1;
            break;
        case SET_RANK:
            
            Set<Map.Entry<String, Object>> set = arg1.valueSet();
            db.beginTransaction();
            for (Map.Entry<String, Object> entry: set) {
                String k = entry.getKey();
                String v = String.valueOf((Integer)entry.getValue());
                db.execSQL(String.format("update %s set %s=%s where %s='%s' and %s=%s", 
                        Ranks.TABLE_NAME,
                        Ranks.CURRENT_RANK,
                        v,
                        Ranks.APPName,
                        k,
                        Ranks.DAY,
                        arg2));
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            count = 1;
            break;
            
        case UPDATE_RANK:	       	
            Set<Map.Entry<String, Object>> totalRanking = arg1.valueSet();
            db.beginTransaction();
            
            db.execSQL(String.format("update %s set %s=-1 where %s=-1",
                    Ranks.TABLE_NAME,
                    Ranks.CURRENT_RANK,
                    Ranks.DAY));
            for (Map.Entry<String, Object> entry: totalRanking) {
                String k = entry.getKey();
                String v = String.valueOf((Integer)entry.getValue());
                db.execSQL(String.format("update %s set %s=%s where %s='%s' and %s=-1", 
                        Ranks.TABLE_NAME,
                        Ranks.CURRENT_RANK,
                        v,
                        Ranks.APPName,
                        k,
                        Ranks.DAY));        
            }
            db.execSQL(String.format("delete from %s where %s=-1 and %s=-1 and %s = 0",
                    Ranks.TABLE_NAME,
                    Ranks.DAY,
                    Ranks.CURRENT_RANK,
                    Ranks.HIDDEN ));
            db.setTransactionSuccessful();
            db.endTransaction();
            count = 1;
            break;
        default:
            Log.d(TAG, "other uri");
            throw new UnsupportedOperationException("Cannot updeate uri:" + arg0);
        }
        
        if (match == ALL_MANUALS || match == ONE_MANUAL
                || match == UPDATE_RANK ) {
            getContext().getContentResolver().notifyChange(arg0, null);
        }
        return 0;
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        Context mContext = null;
        
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            Log.d(TAG, "creating new graphy database");
            db.execSQL(Apps.CREATE_STATEMENT);
            
            db.execSQL("CREATE TABLE " + Tables.DAILY_RANK + "(" +
                    "_id INTEGER PRIMARY KEY," +
                    "appId INTEGER NOT NULL," +
                    "quantity_access INTEGER NOT NULL DEFAULT 0," +
                    "today_rank INTEGER" +
                    ");");
            
            db.execSQL(Ranks.CREATE_STATEMENT);
            db.execSQL(ManualList.CREATE_STATEMENT);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            Log.d(TAG, "update sqlite database");
            int version = oldVersion;
            if (version < 8) {
                version = 8;
            }
            if (version != DATABASE_VERSION) {
                Log.d(TAG, "destorying all old data");
                db.execSQL("DROP TABLE IF EXISTS " + Tables.DAILY_RANK);
                db.execSQL("DROP TABLE IF EXISTS " + Ranks.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + ManualList.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + Apps.TABLE_NAME);
                onCreate(db);
            }	
        }
        
        
    }
    
    private static class SqlArguments {
        public String table;
        public final String where;
        public final String[] args;
        
        /** Operate on existing rows. */
        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }
        
        /** Insert new rows (no where clause allowed). */
        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = null;
                this.args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
    
}
