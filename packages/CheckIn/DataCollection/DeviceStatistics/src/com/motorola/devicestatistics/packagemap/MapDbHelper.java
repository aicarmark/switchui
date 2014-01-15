/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: MapDbHelper.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Oct 18, 2010       A24178      Created file
 **********************************************************
 */

package com.motorola.devicestatistics.packagemap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.CRC32;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

/**
 * @author A24178
 *
 */
public class MapDbHelper extends SQLiteOpenHelper {
    
    private final static String TAG = "MapDBHelper";
    private final static boolean DUMP = false;
    
    private final static String DB_NAME = "packagemapper.db";
    private final static int DB_VERSION = 2;
    private final static int COLLISION_CHECK_COUNT = 5;
    
    private final static String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
            "packagemap " +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "pkg TEXT UNIQUE," +
            "preloaded INTEGER)";
    
    private final static String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS packagemap";
    
    private static MapDbHelper mMe = null;
    private Cache mCache;
    private HashMap<String, Long> mChangeLog =
            new HashMap<String, Long>();
    
    public static synchronized MapDbHelper getInstance(Context ctx) {
        if(mMe == null) {
            mMe = new MapDbHelper(ctx, DB_NAME, null, DB_VERSION);
        }
        return mMe;
    }

    /**
     * @param context
     * @param name
     * @param factory
     * @param version
     */
    public MapDbHelper(Context context, String name, CursorFactory factory,
            int version) {
        super(context, name, factory, version);
        mCache = new Cache();
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    /**
     * @param db
     */
    private void createTables(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
        initTable(db);
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        deleteTables(db);
        createTables(db);
    }

    /**
     * 
     */
    private void deleteTables(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_TABLE);
    }

    private void initTable(SQLiteDatabase db) {
        PreloadMap.init();
        HashMap<String, Long> toLoad = PreloadMap.sMaps;
        Iterator<String> keySet = toLoad.keySet().iterator();
        while(keySet.hasNext()) {
            String key = keySet.next();
            if(key != null) {
                Long id = toLoad.get(key);
                if(id != null) {
                    tryInsert(db, key, id, 1);
                }
            }
        }
    }
    
    private void tryInsert(SQLiteDatabase db, String pkg, long id, int pre) {
        String query = "INSERT INTO packagemap ('id','pkg','preloaded') " +
                " VALUES (" + id + ",'" + pkg + "'," + pre + ")";
        try {
            db.execSQL(query);
        }catch(SQLException sqlEx) {
            if(DUMP) Log.v(TAG, "tryInsert for " +
                    query + " failed with sql Ex", sqlEx);
        }
    }
    
    private long tryInsert(String pkg, int pre) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("pkg", pkg);
        cv.put("preloaded", pre);

        CRC32 crc32 = new CRC32();
        crc32.update(pkg.getBytes());
        long id = crc32.getValue();
        for (int i=0; i<COLLISION_CHECK_COUNT; i++) {
            if (!PreloadMap.isPreloaded(id) && !mCache.hasId(id)) break;
            id += PreloadMap.MAX_PRELOAD_ID;
        }
        cv.put("id", crc32.getValue());

        long i = db.insert("packagemap", null, cv);
        db.close();
        return i;
    }
    
    private void initCache(MapDbHelper helper) {
        mCache.init(helper);
    }
    
    public long getId(String pkg, boolean doInsert) {
        initCache(this);
        Long i = mCache.get(pkg);
        if(i == null) {
            if(!doInsert) return -1;

            i = tryInsert(pkg, 0);
            mCache.put(pkg, i);
            mChangeLog.put(pkg, i);
        }
        return i;
    }
    
    public void startLog() {
        mChangeLog.clear();
    }
    
    public HashMap<String, Long> stopLog() {
        return mChangeLog;
    }
    
    public HashMap<String, Long> generateDump() {
        HashMap<String, Long> dump = new HashMap<String, Long>();
        
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query("packagemap", 
                new String[] {"id", "pkg"}, " preloaded != 1 ", null, null, null, null);
        if(c != null) {
            if(c.getCount() > 0 && c.moveToFirst()) {
                while(!c.isAfterLast()) {
                    dump.put(c.getString(1), c.getLong(0));
                    c.moveToNext();
                }
            }
            c.close();
        }
        db.close();
        return dump;
    }
    
    static class Cache {
        HashMap<String, Long> mMap = new HashMap<String, Long>();
        boolean mInitDone = false;
        
        public void init(MapDbHelper dbHelper) {
            if(!mInitDone) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                Cursor c = db.query("packagemap", 
                        new String[] {"id", "pkg"}, null, null, null, null, null);
                if(c != null) {
                    if(c.getCount() > 0 && c.moveToFirst()) {
                        while(!c.isAfterLast()) {
                            mMap.put(c.getString(1), c.getLong(0));
                            c.moveToNext();
                        }
                    }
                    c.close();
                }
                mInitDone = true;
                db.close();
            }
        }
        
        public Long get(String key) {
            return mMap.get(key);
        }
        
        public void put(String key, Long value) {
            mMap.put(key, value);
        }

        public boolean hasId(Long value) {
            return mMap.containsValue(value);
        }
    }
}

