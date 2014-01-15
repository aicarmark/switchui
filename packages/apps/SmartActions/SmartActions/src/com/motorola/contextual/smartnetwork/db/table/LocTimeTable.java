/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 */

package com.motorola.contextual.smartnetwork.db.table;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.motorola.contextual.smartnetwork.db.Table;

/**
 * Virtual table that wraps LocationSensor's LocTime table
 */
public class LocTimeTable implements Table<LocTimeTuple> {
    public static final String TABLE_NAME = "loctime";
    private static final Uri CONTENT_URI = Uri.parse(
            "content://com.motorola.contextual.virtualsensor.locationsensor/loctime");

    public String getTableName() {
        return TABLE_NAME;
    }

    public long insert(Context context, LocTimeTuple tuple) {
        // not allowing inserts
        return -1;
    }

    public List<LocTimeTuple> query(Context context, String[] columns, String selection,
                                    String[] selectionArgs, String groupBy, String having, String orderBy,
                                    String limit) {
        List<LocTimeTuple> tuples  = null;
        if (context != null) {
            // groupBy, having, and limit clauses not supported through ContentResolver
            if (groupBy != null || having != null || limit != null) {
                throw new IllegalArgumentException("groupBy, having, and limit clauses are not " +
                                                   "supported on LocTimeTable query");
            }
            tuples = new LinkedList<LocTimeTuple>();

            ContentResolver cr = context.getContentResolver();
            /* fetch all columns defined in LocTimeTuple to make sure we get all needed columns.
             * we don't all all columns defined by the actual LocTime table,
             * we just want all columns defined in LocTimeTuple
             */
            Cursor cursor = cr.query(CONTENT_URI, LocTimeTuple.COLUMNS, selection, selectionArgs,
                                     orderBy);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    if (columns == null) {
                        // get all columns
                        columns = LocTimeTuple.COLUMNS;
                    }
                    do {
                        LocTimeTuple locTimeTuple = new LocTimeTuple();
                        // only set values for requested columns
                        for (String column : columns) {
                            int columnIndex = cursor.getColumnIndex(column);
                            if (columnIndex >= 0) {
                                locTimeTuple.put(column, cursor.getString(columnIndex));
                            }
                        }
                        tuples.add(locTimeTuple);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }
        return tuples;
    }

    public List<LocTimeTuple> rawQuery(Context context, String sql, String[] selectionArgs) {
        // not supported
        return null;
    }

    public int update(Context context, LocTimeTuple tuple, String whereClause, String[] whereArgs) {
        // not allowing updates
        return -1;
    }

    public int delete(Context context, String whereClause, String[] whereArgs) {
        // not allowing deletes
        return -1;
    }
}
