/*
 * @(#)ActionsDbAdapter.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/08/12  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class abstracts the queries to the Quick Actions database
 * <code><pre>
 * CLASS:
 *     Any class that needs to query the Quick Actions database should get an instance of this class
 *     and do queries on that instance
 *     Implements {@link Constants}
 *
 * RESPONSIBILITIES:
 *     Creates the database
 *     Opens the database for read operations
 *     Opens the database for write operations
 *     Upgrading the database
 *     Dropping and recreating the database
 *     High level methods for querying the quick actions database
 *
 * COLLABORATORS:
 *    {@link DatabaseUtilityService} - Creates/Edits custom contact tuples in the database.
 *    Deletes custom contact tuples from the database
 *
 * USAGE:
 * See each method
 *
 * </pre></code>
 */

public class ActionsDbAdapter implements Constants {

    private static final String TAG = TAG_PREFIX + ActionsDbAdapter.class.getSimpleName();

    private static final String CREATE_TABLE = " create table ";
    private static final String DROP_TABLE_IF_EXISTS = " drop table if exists ";
    private static final String TEXT_TYPE = " text ";
    private static final String INTEGER_TYPE = " integer ";
    private static final String REAL_TYPE = " real ";
    private static final String OPEN_BRACE = " ( ";
    private static final String CLOSE_BRACE = " ) ";
    private static final String COMMA = ",";
    private static final String SEMI_COLON = ";";
    private static final String SPACE = " ";
    private static final String ID = "_id";
    private static final String ALTER_TABLE 		= " ALTER TABLE ";
    private static final String ADD_COLUMN 		= " ADD COLUMN ";
    private static final String UPDATE           = "UPDATE ";
    private static final String SET              = " SET ";
    private static final String EQUALS           = " = ";

    /** Database Name to store contact details */
    private static final String DATABASE_NAME = "quick_actions.db";
    /** version */
    private static final int DATABASE_VERSION    = 4;

    /** Query to create Auto SMS table */
    private static final String AUTO_SMS_CREATE_TABLE_SQL =
        CREATE_TABLE + SPACE + AUTO_SMS_TABLE + SPACE + OPEN_BRACE
        + ID + SPACE + INTEGER_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.INTERNAL_NAME + SPACE + TEXT_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.NUMBER + SPACE + TEXT_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.RESPOND_TO + SPACE + INTEGER_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.MESSAGE + SPACE + TEXT_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.SENT_FLAG + SPACE + INTEGER_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.NAME + SPACE + TEXT_TYPE + COMMA + SPACE
        + AutoSmsTableColumns.IS_KNOWN + SPACE + INTEGER_TYPE + CLOSE_BRACE + SEMI_COLON;

    /**
     * SQL statement for dropping auto sms table
     */
    private static final String AUTO_SMS_DROP_TABLE_SQL = DROP_TABLE_IF_EXISTS
            + AUTO_SMS_TABLE;

    private static final String VIP_CALLER_CREATE_TABLE_SQL =
        CREATE_TABLE + SPACE + VIP_CALLER_TABLE + SPACE + OPEN_BRACE
        + ID + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.INTERNAL_NAME + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.NUMBER + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.NAME + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.RINGER_MODE + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.VIBE_STATUS + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.RINGER_VOLUME + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.RINGTONE_URI + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.RINGTONE_TITLE + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_RINGER_MODE + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_VIBE_STATUS + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_VIBE_SETTINGS + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_VIBE_IN_SILENT + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_RINGER_VOLUME + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_RINGTONE_URI + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.DEF_RINGTONE_TITLE + SPACE + TEXT_TYPE + COMMA + SPACE
        + VipCallerTableColumns.IS_KNOWN + SPACE + INTEGER_TYPE + COMMA + SPACE
        + VipCallerTableColumns.CONFIG_VERSION + SPACE + REAL_TYPE + CLOSE_BRACE + SEMI_COLON;

    /**
     * SQL statement for dropping vip caller table
     */
    private static final String VIP_CALLER_DROP_TABLE_SQL = DROP_TABLE_IF_EXISTS
            + VIP_CALLER_TABLE;

    /** Handle to the Database helper */
    private DatabaseHelper sDbHelper;

    /**
     * Constructor that takes in the Context
     * @param ctx - Application context
     */
    public ActionsDbAdapter(Context ctx) {
        sDbHelper = new DatabaseHelper(ctx);
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
            if (LOG_INFO) Log.i(TAG, "Creating tables");
            db.execSQL(AUTO_SMS_CREATE_TABLE_SQL);
            db.execSQL(VIP_CALLER_CREATE_TABLE_SQL);

        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion,
                int newVersion) {
            if (LOG_INFO) {
                Log.i(TAG, "onDowngrade invoked with oldVersion " + oldVersion
                        + " newVersion " + newVersion);
            }
            if (newVersion < oldVersion) {
                try {
                    db.execSQL(AUTO_SMS_DROP_TABLE_SQL);
                    db.execSQL(VIP_CALLER_DROP_TABLE_SQL);
                    onCreate(db);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
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
            if (LOG_INFO) Log.i(TAG, "Upgrading to " + version);

            boolean status = false;

            switch (version) {
            case 2:
                status = addColumn(db, VIP_CALLER_TABLE, VipCallerTableColumns.IS_KNOWN, INTEGER_TYPE);
                break;
            case 3:
                if((status = addColumn(db, AUTO_SMS_TABLE, AutoSmsTableColumns.NAME, TEXT_TYPE)) == true) {
                    status = addColumn(db, AUTO_SMS_TABLE, AutoSmsTableColumns.IS_KNOWN, INTEGER_TYPE);
                }
                break;
            case 4:
                status = addColumn(db, VIP_CALLER_TABLE, VipCallerTableColumns.CONFIG_VERSION, REAL_TYPE);
                if (status) {
                    //set default value for CONFIG_VERSION
                    db.beginTransaction();
                    try {
                        db.execSQL(UPDATE + VIP_CALLER_TABLE + SET + VipCallerTableColumns.CONFIG_VERSION + EQUALS
                                   + VipRinger.getInitialVersion());
                        db.setTransactionSuccessful();
                    } catch(Exception e) {
                        Log.e(TAG, "Exception when updating default value");
                        status = false;
                    } finally {
                        db.endTransaction();
                    }
                }
                break;
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
                Log.e(TAG, "Exception in addColumn");
                status = false;
            } finally {
                db.endTransaction();
            }

            return status;
        }
    }

    /**
     * Closes the database, if its open
     */
    public void close() {
        sDbHelper.close();
    }

    /**
     * Utility method to log the cursor
     * @param cursor - Cursor to log
     */
    private void logCursor(Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "Null Cursor");
            return;
        }
        DatabaseUtils.dumpCursor(cursor);
        return;
    }

    /**
     * Wrapper for the data base query
     *
     * @param tableName Name of the table to be queried
     * @param projection List of columns to return
     * @param whereClause A filter in the format of SQL where clause
     * @param args Replaces ?s used in the whereClause
     * @return result Cursor
     */
    public Cursor queryDatabase(String tableName, String[] projection, String whereClause, String[] args) {
        try {
            Cursor cursor = sDbHelper.getReadableDatabase().query(tableName, projection, whereClause, args, null, null, null);
            if (LOG_DEBUG) logCursor(cursor);
            return cursor;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Inserts a record in the specified table
     *
     * @param contactsTuple Information of the row to be inserted
     */
    public void insertRow(BaseTuple contactsTuple) {
        try {
            ContentValues contentValues = contactsTuple.toContentValues();
            sDbHelper.getWritableDatabase().insert(contactsTuple.getTableName(), null, contentValues);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * Updates a row in the Quick Actions database
     *
     * @param contactsTuple Information of the table row to be updated
     * @param whereClause A filter in the format of SQL where clause
     * @param args Replaces ?s used in the whereClause
     */
    public void updateRow(BaseTuple contactsTuple, String whereClause, String[] args) {
        if (LOG_DEBUG) Log.d(TAG,"Updating record ");
        try {
            ContentValues newValues = contactsTuple.toContentValues();
            sDbHelper.getWritableDatabase().update(contactsTuple.getTableName(), newValues, whereClause, args);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * Deletes records from the Quick Actions table based on the specified condition
     *
     * @param tableName Name of the table from which the row is to be deleted
     * @param whereClause A filter in the format of SQL where clause
     * @param args Replaces ?s used in the whereClause
     */
    public void deleteRecord(String tableName, String whereClause, String[] args) {
        try {
            sDbHelper.getWritableDatabase().delete(tableName, whereClause, args);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

}
