/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add MonitorSession
 */

package com.motorola.contextual.smartnetwork.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SmartNetworkSqliteDb extends SQLiteOpenHelper
    implements DbWrapper, SmartNetworkDbSchema {
    private static final String TAG = SmartNetworkSqliteDb.class.getSimpleName();

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "SmartNetwork.db";
    private static final String ENABLE_FOREIGN_KEY_SUPPORT = "PRAGMA foreign_keys=ON;";

    private enum AccessType {
        READ_ONLY,
        WRITABLE
    }

    public SmartNetworkSqliteDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String statement : CREATE_DATABASE) {
            db.execSQL(statement);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.beginTransaction();
            try {
                // create MonitorSession table
                db.execSQL(CREATE_TBL_MONITOR_SESSION);
                db.execSQL(CREATE_INDEX_FK_TOP_LOCATION_ON_TBL_MONITOR_SESSION);
                db.execSQL(CREATE_INDEX_START_TIME_TBL_MONITOR_SESSION);

                // recreate DataConnectionState table
                db.execSQL(DROP_TABLE_IF_EXISTS + TBL_DATA_CONNECTION_STATE);
                db.execSQL(CREATE_TBL_DATA_CONNECTION_STATE);
                db.execSQL(CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_DATA_CONNECTION_STATE);

                // recreate ServiceState table
                db.execSQL(DROP_TABLE_IF_EXISTS + TBL_SERVICE_STATE);
                db.execSQL(CREATE_TBL_SERVICE_STATE);
                db.execSQL(CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_SERVICE_STATE);

                // recreate SignalStrength table
                db.execSQL(DROP_TABLE_IF_EXISTS + TBL_SIGNAL_STRENGTH);
                db.execSQL(CREATE_TBL_SIGNAL_STRENGTH);
                db.execSQL(CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_SIGNAL_STRENGTH);

                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Unable to upgrade from db " + oldVersion + " to " + newVersion);
            } finally {
                db.endTransaction();
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            /* foreign key must be explicitly enabled for each connection
             * http://sqlite.org/foreignkeys.html
             */
            db.execSQL(ENABLE_FOREIGN_KEY_SUPPORT);
        }
    }

    private SQLiteDatabase getDb(AccessType acess) {
        SQLiteDatabase db = null;

        switch (acess) {
        case READ_ONLY:
            try {
                db = getReadableDatabase();
            } catch (SQLiteException sqle) {
                Log.e(TAG, "Unable to get read-only database.");
            }
            break;

        case WRITABLE:
            try {
                db = getWritableDatabase();
            } catch (SQLiteException sqle) {
                Log.e(TAG, "Unable to get writable database: " + sqle);
            }
            break;
        }
        return db;
    }

    public long insert(String table, ContentValues values) {
        long inserted = -1;

        if (table != null && !table.isEmpty() && values != null && values.size() > 0) {
            SQLiteDatabase db = getDb(AccessType.WRITABLE);
            if (db != null) {
                inserted = db.insert(table, null, values);
            }
        } else {
            Log.e(TAG, "Table name and values cannot be null or empty for row insertion.");
        }

        return inserted;
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit) {
        Cursor cursor = null;
        if (table != null && !table.isEmpty()) {
            SQLiteDatabase db = getDb(AccessType.READ_ONLY);
            if (db != null) {
                cursor = db.query(table, columns, selection, selectionArgs, groupBy, having,
                                  orderBy, limit);
            }
        }
        return cursor;
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        Cursor cursor = null;
        if (sql != null && !sql.isEmpty()) {
            SQLiteDatabase db = getDb(AccessType.READ_ONLY);
            if (db != null) {
                cursor = db.rawQuery(sql, selectionArgs);
            }
        }
        return cursor;
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        int updated = 0;
        if (table != null && !table.isEmpty() && values != null && values.size() > 0) {
            SQLiteDatabase db = getDb(AccessType.WRITABLE);
            if (db != null) {
                updated = db.update(table, values, whereClause, whereArgs);
            }
        }
        return updated;
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        int deleted = 0;
        if (table != null && !table.isEmpty()) {
            SQLiteDatabase db = getDb(AccessType.WRITABLE);
            if (db != null) {
                deleted = db.delete(table, whereClause, whereArgs);
            }
        }
        return deleted;
    }
}
