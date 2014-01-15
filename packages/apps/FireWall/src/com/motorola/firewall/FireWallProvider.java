package com.motorola.firewall;

import com.motorola.firewall.FireWall.Name;
import com.motorola.firewall.FireWall.BlockLog;
import com.motorola.firewall.FireWall.Settings;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
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
import java.util.HashMap;

public class FireWallProvider extends ContentProvider {
    private static final String TAG = "FireWallProvider";

    private static final String DATABASE_NAME = "firewall.db";
    private static final int DATABASE_VERSION = 6;
    private static final String LIST_TABLE_NAME = "namelist";
    private static final String LOG_TABLE_NAME = "loglist";
    private static final String SETTING_TABLE_NAME = "settinglist";

    private static HashMap<String, String> sNameListProjectionMap;
    private static HashMap<String, String> sLogListProjectionMap;
    private static HashMap<String, String> sSettingListProjectionMap;

    private static final int NAMELIST = 1;
    private static final int NAMELIST_ID = 2;
    
    private static final int LOGLIST = 3;
    private static final int LOGLIST_ID = 4;
    
    private static final int SETTINGLIST = 5;
    private static final int SETTINGLIST_ID = 6;
    

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + LIST_TABLE_NAME + " (" + Name._ID
                    + " INTEGER PRIMARY KEY," + Name.BLOCK_TYPE + " TEXT," + Name.PHONE_NUMBER_KEY + " TEXT," 
                    + Name.CACHED_NAME + " TEXT," + Name.CACHED_NUMBER_TYPE + " INTEGER," 
                    + Name.CACHED_NUMBER_LABEL + " TEXT," +  Name.FOR_CALL + " BOOLEAN,"
                    + Name.FOR_SMS + " BOOLEAN" + ");");
            db.execSQL("CREATE TABLE " + LOG_TABLE_NAME + " (" + BlockLog._ID
                      + " INTEGER PRIMARY KEY," + BlockLog.PHONE_NUMBER + " TEXT," + BlockLog.CACHED_NAME + " TEXT,"
                      + BlockLog.CACHED_NUMBER_TYPE + " INTEGER," + BlockLog.CACHED_NUMBER_LABEL + " TEXT,"
                      + BlockLog.BLOCKED_DATE + " INTEGER," + BlockLog.NETWORK + " INTEGER,"
                      + BlockLog.LOG_TYPE + " INTEGER," + BlockLog.LOG_DATA + " TEXT" + ");");
            db.execSQL("CREATE TABLE " + SETTING_TABLE_NAME + " (" + Settings._ID
                      + " INTEGER PRIMARY KEY," + Settings.NAME + " TEXT," + Settings.VALUE + " TEXT" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                        + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS namelist");
                db.execSQL("DROP TABLE IF EXISTS loglist");
                onCreate(db);
                return;
            }
            if (oldVersion == 2) {
                try {
                    db.execSQL("ALTER TABLE callloglist ADD network INTEGER");
                    db.execSQL("ALTER TABLE smsloglist ADD network INTEGER");
                } catch (SQLiteException sqle) {
                    // Maybe the table was altered already... Shouldn't be an issue.
                }
                oldVersion++;
            }
            if (oldVersion == 3) {
                try {
                    db.execSQL("ALTER TABLE namelist ADD for_call BOOLEAN");
                    db.execSQL("ALTER TABLE namelist ADD for_sms BOOLEAN");
                } catch (SQLiteException sqle) {
                    // Maybe the table was altered already... Shouldn't be an issue.
                }
                oldVersion++;
            }
            if (oldVersion == 4) {
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                        + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS callloglist");
                db.execSQL("DROP TABLE IF EXISTS smsloglist");
                db.execSQL("CREATE TABLE " + LOG_TABLE_NAME + " (" + BlockLog._ID
                        + " INTEGER PRIMARY KEY," + BlockLog.PHONE_NUMBER + " TEXT," + BlockLog.CACHED_NAME + " TEXT,"
                        + BlockLog.CACHED_NUMBER_TYPE + " INTEGER," + BlockLog.CACHED_NUMBER_LABEL + " TEXT,"
                        + BlockLog.BLOCKED_DATE + " INTEGER," + BlockLog.NETWORK + " INTEGER,"
                        + BlockLog.LOG_TYPE + " INTEGER," + BlockLog.LOG_DATA + " TEXT" + ");");
                oldVersion++;
            }
            if (oldVersion == 5) {
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                        + ", which will destroy all old data");
                db.execSQL("CREATE TABLE " + SETTING_TABLE_NAME + " (" + Settings._ID
                        + " INTEGER PRIMARY KEY," + Settings.NAME + " TEXT," + Settings.VALUE + " TEXT" + ");");
                oldVersion++;
            }
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case NAMELIST:
                qb.setTables(LIST_TABLE_NAME);
                qb.setProjectionMap(sNameListProjectionMap);
                break;

            case NAMELIST_ID:
                qb.setTables(LIST_TABLE_NAME);
                qb.setProjectionMap(sNameListProjectionMap);
                qb.appendWhere(Name._ID + "=" + uri.getPathSegments().get(1));
                break;
                
            case LOGLIST:
                qb.setTables(LOG_TABLE_NAME);
                qb.setProjectionMap(sLogListProjectionMap);
                break;

            case LOGLIST_ID:
                qb.setTables(LOG_TABLE_NAME);
                qb.setProjectionMap(sLogListProjectionMap);
                qb.appendWhere(BlockLog._ID + "=" + uri.getPathSegments().get(1));
                
            case SETTINGLIST:
                qb.setTables(SETTING_TABLE_NAME);
                qb.setProjectionMap(sSettingListProjectionMap);
                break;

            case SETTINGLIST_ID:
                qb.setTables(SETTING_TABLE_NAME);
                qb.setProjectionMap(sSettingListProjectionMap);
                qb.appendWhere(Settings._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = FireWall.Name.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        if (c != null) {
            // Tell the cursor what uri to watch, so it knows when its source data
            // changes
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NAMELIST:
                return Name.CONTENT_TYPE;

            case NAMELIST_ID:
                return Name.CONTENT_ITEM_TYPE;
                
            case LOGLIST:
                return BlockLog.CONTENT_TYPE;
                
            case LOGLIST_ID:
                return BlockLog.CONTENT_ITEM_TYPE;

            case SETTINGLIST:
                return Settings.CONTENT_TYPE;
                
            case SETTINGLIST_ID:
                return Settings.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        // Validate the requested uri
        if (sUriMatcher.match(uri) == NAMELIST) {
            if (values.containsKey(Name.BLOCK_TYPE) == false) {
                values.put(Name.BLOCK_TYPE, 0);
            }

            if (values.containsKey(Name.PHONE_NUMBER_KEY) == false) {
                values.put(Name.PHONE_NUMBER_KEY, "");
            }
            if (values.containsKey(Name.FOR_CALL) == false) {
                values.put(Name.FOR_CALL, true);
            }
            if (values.containsKey(Name.FOR_SMS) == false) {
                values.put(Name.FOR_SMS, true);
            }
        } else if (sUriMatcher.match(uri) == LOGLIST) {
            Long now = Long.valueOf(System.currentTimeMillis());
            if (values.containsKey(FireWall.BlockLog.PHONE_NUMBER) == false) {
                values.put(FireWall.BlockLog.PHONE_NUMBER, "");
            }
            if (values.containsKey(FireWall.BlockLog.BLOCKED_DATE) == false) {
                values.put(FireWall.BlockLog.BLOCKED_DATE, now);
            }
            if (values.containsKey(BlockLog.LOG_TYPE) == false) {
                values.put(BlockLog.LOG_TYPE, BlockLog.CALL_BLOCK);
            }
        } else if (sUriMatcher.match(uri) == SETTINGLIST) {
            if (values.containsKey(FireWall.Settings.VALUE) == false) {
                values.put(FireWall.Settings.VALUE, "");
            }
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if (sUriMatcher.match(uri) == NAMELIST) {
            long rowId = db.insert(LIST_TABLE_NAME, Name.PHONE_NUMBER_KEY, values);
            if (rowId > 0) {
                Uri nameUri = ContentUris.withAppendedId(Name.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(nameUri, null);
                return nameUri;
            }
        } else if (sUriMatcher.match(uri) == LOGLIST) {
            long rowId = db.insert(LOG_TABLE_NAME, BlockLog.PHONE_NUMBER, values);
            if (rowId > 0) {
                Uri logUri = ContentUris.withAppendedId(BlockLog.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(logUri, null);
                return logUri;
            }
        } else if (sUriMatcher.match(uri) == SETTINGLIST) {
            long rowId = db.insert(SETTING_TABLE_NAME, Settings.NAME, values);
            if (rowId > 0) {
                Uri settingUri = ContentUris.withAppendedId(Settings.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(settingUri, null);
                return settingUri;
            }
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NAMELIST:
                count = db.delete(LIST_TABLE_NAME, where, whereArgs);
                break;

            case NAMELIST_ID:
                String nameId = uri.getPathSegments().get(1);
                count = db.delete(LIST_TABLE_NAME, Name._ID + "=" + nameId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
                
            case LOGLIST:
                count = db.delete(LOG_TABLE_NAME, where, whereArgs);
                break;

            case LOGLIST_ID:
                String logId = uri.getPathSegments().get(1);
                count = db.delete(LOG_TABLE_NAME, BlockLog._ID + "=" + logId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            case SETTINGLIST:
                count = db.delete(SETTING_TABLE_NAME, where, whereArgs);
                break;

            case SETTINGLIST_ID:
                String settingId = uri.getPathSegments().get(1);
                count = db.delete(SETTING_TABLE_NAME, Settings._ID + "=" + settingId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case NAMELIST:
                count = db.update(LIST_TABLE_NAME, values, where, whereArgs);
                break;

            case NAMELIST_ID:
                String noteId = uri.getPathSegments().get(1);
                count = db.update(LIST_TABLE_NAME, values, Name._ID + "=" + noteId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
                
            case LOGLIST:
                count = db.update(LOG_TABLE_NAME, values, where, whereArgs);
                break;

            case LOGLIST_ID:
                String logId = uri.getPathSegments().get(1);
                count = db.update(LOG_TABLE_NAME, values, BlockLog._ID + "=" + logId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            case SETTINGLIST:
                count = db.update(SETTING_TABLE_NAME, values, where, whereArgs);
                break;

            case SETTINGLIST_ID:
                String settingId = uri.getPathSegments().get(1);
                count = db.update(SETTING_TABLE_NAME, values, Settings._ID + "=" + settingId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(FireWall.AUTHORITY, "namelist", NAMELIST);
        sUriMatcher.addURI(FireWall.AUTHORITY, "namelist/#", NAMELIST_ID);
        sUriMatcher.addURI(FireWall.AUTHORITY, "loglist", LOGLIST);
        sUriMatcher.addURI(FireWall.AUTHORITY, "loglist/#", LOGLIST_ID);
        sUriMatcher.addURI(FireWall.AUTHORITY, "settinglist", SETTINGLIST);
        sUriMatcher.addURI(FireWall.AUTHORITY, "settinglist/#", SETTINGLIST_ID);

        sNameListProjectionMap = new HashMap<String, String>();
        sNameListProjectionMap.put(Name._ID, Name._ID);
        sNameListProjectionMap.put(Name.BLOCK_TYPE, Name.BLOCK_TYPE);
        sNameListProjectionMap.put(Name.PHONE_NUMBER_KEY, Name.PHONE_NUMBER_KEY);
        sNameListProjectionMap.put(Name.CACHED_NAME, Name.CACHED_NAME);
        sNameListProjectionMap.put(Name.CACHED_NUMBER_TYPE, Name.CACHED_NUMBER_TYPE);
        sNameListProjectionMap.put(Name.CACHED_NUMBER_LABEL, Name.CACHED_NUMBER_LABEL);
        sNameListProjectionMap.put(Name.FOR_CALL, Name.FOR_CALL);
        sNameListProjectionMap.put(Name.FOR_SMS, Name.FOR_SMS);

        
        sLogListProjectionMap = new HashMap<String, String>();
        sLogListProjectionMap.put(BlockLog._ID, BlockLog._ID);
        sLogListProjectionMap.put(BlockLog.PHONE_NUMBER, BlockLog.PHONE_NUMBER);
        sLogListProjectionMap.put(BlockLog.CACHED_NAME, BlockLog.CACHED_NAME);
        sLogListProjectionMap.put(BlockLog.CACHED_NUMBER_TYPE, BlockLog.CACHED_NUMBER_TYPE);
        sLogListProjectionMap.put(BlockLog.CACHED_NUMBER_LABEL, BlockLog.CACHED_NUMBER_LABEL);
        sLogListProjectionMap.put(BlockLog.BLOCKED_DATE, BlockLog.BLOCKED_DATE);
        sLogListProjectionMap.put(BlockLog.NETWORK, BlockLog.NETWORK);
        sLogListProjectionMap.put(BlockLog.LOG_TYPE, BlockLog.LOG_TYPE);
        sLogListProjectionMap.put(BlockLog.LOG_DATA, BlockLog.LOG_DATA);
        
        sSettingListProjectionMap = new HashMap<String, String>();
        sSettingListProjectionMap.put(Settings._ID, Settings._ID);
        sSettingListProjectionMap.put(Settings.NAME, Settings.NAME);
        sSettingListProjectionMap.put(Settings.VALUE, Settings.VALUE);
    }
}
