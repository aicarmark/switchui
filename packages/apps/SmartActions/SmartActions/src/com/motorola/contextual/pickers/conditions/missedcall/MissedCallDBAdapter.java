/*
 * @(#)MissedCallDBAdapter.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/12/01   NA               Initial Version
 *
 */
package com.motorola.contextual.pickers.conditions.missedcall;


import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;

/**
 * This class abstracts the queries to the Missed Call database
 * <code><pre>
 * CLASS:
 *     Any class that needs to query the missed calls database should get an instance of this class
 *     and do queries on that instance
 *
 * RESPONSIBILITIES:
 *     Creates the database
 *     Opens the database for read operations
 *     Opens the database for write operations
 *     Upgrading the database
 *     Dropping and recreating the database
 *     High level methods for querying the missed call database
 *
 * COLLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 * See each method
 *
 * </pre></code>
 */
public class MissedCallDBAdapter implements  DbSyntax, Constants, MissedCallConstants {
    private static final String TAG = MissedCallDBAdapter.class.getSimpleName();

    private static final String SPACE                   = " ";

    /** _id is not used. But this field is required for SimpleCursorAdapter to work */
    private static final String _ID = MissedCallTableColumns.ID;
    /** Missed call name given by the user and is visible to the world */
    private static final String MISSED_CALL_NAME = MissedCallTableColumns.NAME;
    /** Missed call number. */
    private static final String MISSED_CALL_NUMBER = MissedCallTableColumns.NUMBER;
    /** Missed call current count */
    private static final String MISSED_CALL_CURRENT_COUNT = MissedCallTableColumns.CURRENT_COUNT;
    /** Missed Call Database Name */
    private static final String DATABASE_NAME       = "missed_calls.db";

    /** Missed Call table name */
    private static final String MISSEDCALLS_TABLE    = "missed_calls";

    /** version */
    private static final int    DATABASE_VERSION    = 1;

    //table creation statement
    private static final String CREATE_MISSEDCALL_TABLE_SQL =
        CREATE_TABLE + SPACE + MISSEDCALLS_TABLE + SPACE + LP
        + _ID + SPACE + INTEGER_TYPE + COMMA + SPACE
        + MISSED_CALL_NUMBER + SPACE + TEXT_TYPE + PRIMARY_KEY + COMMA + SPACE
        + MISSED_CALL_NAME + SPACE + TEXT_TYPE + COMMA + SPACE
        + MISSED_CALL_CURRENT_COUNT + SPACE + INTEGER_TYPE + RP + SEMI_COLON;

    /** Handle to the Database helper */
    private static DatabaseHelper sDbHelper;


    /**
     * Initializes the DB Helper
     *
     * @param context
     */
    private static synchronized void initializeDbHelper(Context context) {
        if (sDbHelper == null) {
            sDbHelper = new DatabaseHelper(context);
        }
    }
    /**
     * Constructor that takes in the Context
     *
     * @param ctx - Application context
     */
    public MissedCallDBAdapter(Context context) {
        initializeDbHelper(context);
    }

    /**
     * This class extends the SQLiteOpenHelper and provide methods to create and upgrade database
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {


        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOG_INFO) Log.i(TAG, "DB Create statememt :" + CREATE_MISSEDCALL_TABLE_SQL);
            try {
                db.execSQL(CREATE_MISSEDCALL_TABLE_SQL);
            } catch (Exception e) {
                Log.e(TAG, "Exception in MissedCallDBAdapter::createTable");
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOG_INFO) Log.i(TAG, "Upgrading database from version " + oldVersion
                                    + " to " + newVersion);

            boolean status = true;
            for (int version = oldVersion + 1; version <= newVersion && status; version++) {
                status = upgradeTo(db, version);
            }
        }

        /**
         * Call this function to upgrade the db to version 'version'
         * only when you are sure that db is on version 'version-1'
         * @param db      - db to work on
         * @param version - new version
         */
        private boolean upgradeTo(SQLiteDatabase db, int version) {
            if(LOG_INFO) Log.i(TAG, "Upgrading to " + version);
            boolean status = false;

            switch (version) {

            default:
                Log.e(TAG, "Incorrect version.  Database not upgraded to " + version);
            }

            return status;
        }

        /**
         * Adds a single column into the table
         * @param db                - db to act on
         * @param dbTable           - Table in db to work on
         * @param columnName        - column name to add
         * @param columnDefinition  - new column's definition
         */
        private boolean addColumn(SQLiteDatabase db, String dbTable, String columnName,
                                  String columnDefinition) {

            boolean status = true;

            try {
                db.beginTransaction();
                db.execSQL(ALTER_TABLE + dbTable + ADD_COLUMN + columnName + SPACE + columnDefinition + SEMI_COLON);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Exception in MissedCallDBAdapter::addColumn");
                status = false;
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }

            return status;
        }
    }


    /**
     * close
     */
    public void close() {
        if (LOG_DEBUG) Log.d(TAG, "close");
        // Does nothing, since we want to reuse the db instance
        // retained for backward compatibility
    }

    /**
     * Inserts a missed call record in the table
     *
     * @param numArray - List of numbers
     * @param nameArray - List of names
     */
    public void insertRowToMissedCallTable(ArrayList<String> numArray, ArrayList<String> nameArray) {

        ContentValues contentValues = new ContentValues();

        int size = numArray.size();
        for(int i=0; i<size; i++) {
            String whereClause =  MISSED_CALL_NUMBER + EQUALS + Q + numArray.get(i) + Q;
            Cursor cursor = null;
            try {
                cursor = sDbHelper.getReadableDatabase().query(MISSEDCALLS_TABLE, new String[] {MISSED_CALL_NUMBER},
                         whereClause, null, null, null, null);

                if (LOG_INFO) Log.i(TAG, "insertRowToMissedCallTable :" + numArray.get(i));

                if((cursor == null) || (cursor.getCount() == 0)) {
                    contentValues.put(MISSED_CALL_NUMBER, numArray.get(i));
                    contentValues.put(MISSED_CALL_NAME, nameArray.get(i));
                    contentValues.put(MISSED_CALL_CURRENT_COUNT, 0);
                    sDbHelper.getWritableDatabase().insert(MISSEDCALLS_TABLE, null, contentValues);
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

    }

    /**
     * Deletes the record from the table, with the given numbers
     * @param numArray - Number list
     */
    public void deleteRows(ArrayList<String> numArray) {

        try {
            int size = numArray.size();
            for(int i=0; i<size; i++) {
                String whereClause = MISSED_CALL_NUMBER + EQUALS + Q + numArray.get(i) + Q;
                int count = sDbHelper.getWritableDatabase().delete(MISSEDCALLS_TABLE, whereClause, null);
                if (LOG_DEBUG) Log.d(TAG, count + " rows deleted with name as " + numArray.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets missed call table
     *
     * @param context - Application context
     */
    public void resetMissedCallTable(Context context) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MISSED_CALL_CURRENT_COUNT, 0);
            sDbHelper.getWritableDatabase().update(MISSEDCALLS_TABLE, contentValues, null, null);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Updates a missed call record in the table
     *
     * @param number - Number to be updated
     */
    public int updateRowToMissedCallTable(String number) {

        String whereClause = MISSED_CALL_NUMBER + LIKE + Q + WILD + number + WILD + Q
                             + OR + MISSED_CALL_NUMBER + EQUALS + Q  + MISSED_CALL_ANY_NUMBER + Q;
        Cursor cursor = null;
        int curCount = -1;

        try {
            cursor = sDbHelper.getReadableDatabase().query(MISSEDCALLS_TABLE, new String[] {MISSED_CALL_NUMBER, MISSED_CALL_CURRENT_COUNT},
                     whereClause, null, null, null, null);

            if((cursor != null) && (cursor.moveToFirst())) {
                curCount = cursor.getInt(cursor.getColumnIndex(MISSED_CALL_CURRENT_COUNT));
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if(curCount == -1) return -1;

        curCount++;


        if (LOG_DEBUG) Log.d(TAG, "updateRowToMissedCallTable :" + number + " : " + curCount);
        try {
            whereClause = MISSED_CALL_NUMBER +  LIKE + Q + WILD + number + WILD + Q
                          + OR + MISSED_CALL_NUMBER + EQUALS + Q + MISSED_CALL_ANY_NUMBER +  Q;

            ContentValues contentValues = new ContentValues();
            contentValues.put(MISSED_CALL_CURRENT_COUNT, curCount);
            sDbHelper.getWritableDatabase().update(MISSEDCALLS_TABLE, contentValues, whereClause,null);
            return curCount;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Gets current count corresponding to a number
     *
     * @param number - number
     * @return curCount - current count
     */
    public int getCurrentCount(String number) {
        String whereClause = MISSED_CALL_NUMBER + EQUALS + Q + number + Q;
        Cursor cursor = null;
        int curCount = -1;

        try {
            cursor = sDbHelper.getReadableDatabase().query(MISSEDCALLS_TABLE, new String[] {MISSED_CALL_CURRENT_COUNT},
                     whereClause, null, null, null, null);

            if((cursor != null) && (cursor.moveToFirst())) {
                if (LOG_DEBUG) Log.d(TAG, "getCurrentCount - db number : " + number);
                curCount = cursor.getInt(cursor.getColumnIndex(MISSED_CALL_CURRENT_COUNT));
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return curCount;

    }
}
