/*
 * @(#)DbAdapter.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 *
 */
package com.motorola.contextual.virtualsensor.locationsensor.dbhelper;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.text.format.DateUtils;

/** This class handles opening/closing of the Db, creation of the database tables,
 * and upgrades of the database (adding columns and such).
 *
 *<code><pre>
 * CLASS:
 * 	Every class that abstracts a database table uses this class to open the database.
 *  Implements DbSyntax to perform SQL operations.
 *
 * RESPONSIBILITIES:
 * 	Opening the database and the adapter
 * 	Closing the database and the adapter
 * 	Instantiating the adapter
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 * 	See FriendTable.java
 *
 *</pre></code>
 */
public class DbAdapter implements DbSyntax {

    private final static String TAG = "LSAPP_DBA";

    /** will instantiate at open(). Make sure to close with close() */
    protected LocationDatabaseHelper mDbHelper = null;
    /** will instantiate at open(). Make sure to close with close() */
    protected SQLiteDatabase mDb = null;

    // instance variables
    @SuppressWarnings("unused")
    private Context mContext;   // give this class a context...not used for now. but Will be used in some cases later.

    private DbAdapter() { }

    /** Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param context -  the Context within which to work
     */
    private DbAdapter(Context context) {
        this.mContext = context;
    }

    /** Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure.
     *
     * NOTE - YOU MUST CLOSE THE ADAPTER RETURNED WHEN FINISHED
     *
     * @param context - context
     * @return this (self reference, allowing this to be chained in an initialization
     * 				call)
     * @throws SQLException - if the database could be neither opened or created
     */
    public static DbAdapter openForWrite(Context context, final String openFrom) throws SQLException {

        DbAdapter adapter = new DbAdapter(context);
        adapter.mDbHelper = LocationDatabaseHelper.getInstance(context);

        adapter.mDb = adapter.mDbHelper.getWritableDatabase();
        return adapter;
    }

    /** Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure.
     *
     * NOTE - YOU MUST CLOSE THE ADAPTER RETURNED WHEN FINISHED
     *
     * @param context - context
     * @return this (self reference, allowing this to be chained in an initialization
     * 				call)
     * @throws SQLException - if the database could be neither opened or created
     */
    public static DbAdapter openForRead(Context context, final String openFrom) throws SQLException {

        DbAdapter adapter = new DbAdapter(context);
        adapter.mDbHelper = LocationDatabaseHelper.getInstance(context);

        adapter.mDb = adapter.mDbHelper.getReadableDatabase();
        return adapter;
    }

    /** Closes the database and the adapter.
     */
    public void close(final String closeFrom) {

        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
        }
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }


    /** Accessor method to get to the database - getter.
     *
     * @return SQLiteDatabase instance opened.
     */
    public SQLiteDatabase getDb() {
        return mDb;
    }

    /**
     * purge old records (more than 100 days) from database.
     * return 1 if success, 0 otherwise
     */
    private int purgeStaleDbRecords() {
        int ret=1;   // no error
        LSAppLog.e(TAG, "purgeDBStaleRecords upon SQLiteFullException:");
        long lately = System.currentTimeMillis() - (150*DateUtils.DAY_IN_MILLIS);  // stale = older than 150 days.
        final String oldRecords = "( " +  LocationDatabase.LocTimeTable.Columns.STARTTIME + " <= " + lately + " )";

        mDb.beginTransaction();
        try {
            ret = mDb.delete(LocationDatabase.LocTimeTable.TABLE_NAME, oldRecords, null);
            if (ret == 0) {
                // Craig's formular, oldest record = min(starttime) and start from there within 10 days.
                final String deleteOldest = "delete from loctime where starttime <= (select min(starttime) + 10*24*60*60*1000 from loctime)";
                mDb.execSQL(deleteOldest);
                mDb.setTransactionSuccessful();
                ret = 1;
            }
        } catch (Exception e) {
            LSAppLog.e(TAG, "purgeDBStaleRecords Exception:" + e.toString());
            // I should really close the db, drop the db, and restart.
            ret = 0;
        } finally {
        	try {
        		mDb.endTransaction();
        	} catch (Exception e) {
        		LSAppLog.e(TAG, "Exception during end transcation:" + e.toString());
                ret = 0;
        	}
        }
        return ret;
    }

    /**
     * insert into database, wrap all the exception handlings
     */
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues values, int conflictAlgorithm) {
        // NoSQL style. one record only, no need for db.beginTransaction()
        long rowId = -1;
        int retries = 2;  // try at most 2 twice
        do {
            try {
                retries--;
                rowId = mDb.insertWithOnConflict(table, nullColumnHack, values, conflictAlgorithm);
                retries = 0; // done
            } catch (SQLiteFullException e) {
                purgeStaleDbRecords();  // reset tries
            } catch (Exception e) {
                LSAppLog.e(TAG, "insert DB exception:" + e.toString());
            }
        } while (retries > 0);

        return rowId;
    }

    /**
     * update database entry, wrap all exception handlings.
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        int count = 0;
        int retries = 2;
        do {
            try {
                retries--;
                count = mDb.update(table, values, whereClause, whereArgs);
                retries = 0; // done
            } catch (SQLiteFullException e) {
                purgeStaleDbRecords();  // reset tries
            } catch (Exception e) {
                LSAppLog.e(TAG, "update DB exception:" + e.toString());
            }
        } while (retries > 0);

        return count;
    }
}
