/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: DatabaseIface.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 22, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import android.util.Log;

/**
 * @author bluremployee
 *
 */
public class DatabaseIface extends SQLiteOpenHelper {
    
    public interface IDbInitListener {
        public void onCreate(SQLiteDatabase db);
        public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer);
    }

    private final static String TAG = "DatabaseIface";
    private final static boolean DUMP = false;
    
    private final static String DB_NAME = "statistics.db";
    private final static int DB_VERSION = 2;
    
    private static DatabaseIface sDb;
    private static int sRefCount;

    private IDbInitListener mListener;

    // RAHUL: I read somewhere that sqlite db is not thread safe.
    // This lock is used to prevent the BatteryStatsAccumulator from writing to the database,
    // when the StatsUploader is reading from the database. Prior to this, I was occassionally
    // observing forcecloses of the form "database statistics.db is already closed"
    static final Object sqliteDbLock = new Object();
    
    public synchronized static DatabaseIface getInstance(Context context, IDbInitListener cb) {
        if(sRefCount == 0) {
            if(sDb != null) {
                Log.v(TAG, "my ref is 0 but im still here");
            }
            sDb = new DatabaseIface(context, DB_NAME, null, DB_VERSION, cb);
        }
        sRefCount++;
        return sDb;
    }

    public synchronized static void putInstance() {
        if(DUMP) Log.v(TAG, "at put ref is " + sRefCount + ", sDb = " + sDb);
        if(--sRefCount <= 0) {
            sDb = null;
        }
    }

    /**
     * @param context
     * @param name
     * @param factory
     * @param version
     * @param cb
     */
    private DatabaseIface(Context context, String name, CursorFactory factory,
            int version, IDbInitListener cb) {
        this(context, name, factory, version);
        mListener = cb; // We are OK in doing this here as the onCreate/onUpgrade methods
                        // are only called when the db is accessed and not via the ctor
    }
    
    /**
     * @param context
     * @param name
     * @param factory
     * @param version
     */
    private DatabaseIface(Context context, String name, CursorFactory factory,
            int version) {
        super(context, name, factory, version);
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        if(mListener != null) {
            mListener.onCreate(db);
        }
    }

    /* (non-Javadoc)
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(mListener != null) {
            mListener.onUpgrade(db, oldVersion, newVersion);
        }
    }
    
    public Cursor doQuery(String query) {
        return doQuery(query, null);
    }
    
    public Cursor doQuery(String query, String[] args) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = runQuery(db, query, args);
        // close(); caller needs to call this after cursor is used
        return c;
    }
    
    private Cursor runQuery(SQLiteDatabase db, String query, String[] args) {   
        Cursor c = null;
        if(db != null) {
            if(DUMP) Log.v(TAG, "runQuery - will run: " + query);
            c = db.rawQuery(query, args);
            if(DUMP) Log.v(TAG, "runQuery - cursor is " + c);
        }
        return c;
    }
    
    public boolean runSql(String query) {
        SQLiteDatabase db = getWritableDatabase();
        boolean result = execSql(db, query);
        close();
        return result;
    }
    
    private boolean execSql(SQLiteDatabase db, String query) {
        boolean done = false;
        if(db != null) {
            try {
                if(DUMP) Log.v(TAG, "runSql to run " + query);
                db.execSQL(query);
                done = true;
            }catch(SQLException sqlEx) {
                if(DUMP) Log.v(TAG, "runSql failed with ", sqlEx); 
            }
        }
        if(DUMP) Log.v(TAG, "runSql status " + done);
        return done;
    }
    
    // For transaction based use
    public static class Transaction {
        SQLiteDatabase mDb;
        
        public void startTransaction() {
            mDb.beginTransaction();
        }
        
        public Cursor doQuery(String query, String[] args) {
            Cursor c = null;
            if(mDb != null) {
                if(DUMP) Log.v(TAG, "Transaction: rawQ: " + query);
                c = mDb.rawQuery(query, args);
                if(DUMP) Log.v(TAG, "Transaction: rawQ: result c:" + c);
            }
            return c;
        }
        
        public boolean runSql(String query, Object[] args) {
            boolean done = false;
            if(mDb != null) {
                try {
                    if(DUMP) Log.v(TAG, "Transaction: runSql:" + query);
                    if(args != null) {
                        mDb.execSQL(query, args);
                    }else {
                        mDb.execSQL(query);
                    }
                    done = true;
                }catch(SQLException sqlEx) {
                    if(DUMP) Log.v(TAG, "Transaction: runSql ex:", sqlEx);
                }
            }
            if(DUMP) Log.v(TAG, "Transaction: runSql status:" + done);
            return done;
        }
        
        public void markTransaction(boolean success) {
            if(success) mDb.setTransactionSuccessful();
        }
        
        public void stopTransaction() {
            mDb.endTransaction();
            mDb.close();
        }
    }
    
    public Transaction getTransaction() {
        Transaction t = new Transaction();
        t.mDb = getWritableDatabase();
        return t;
    }
}

