/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Use Tuple.rawPut for rawQuery
 */

package com.motorola.contextual.smartnetwork.db.table;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.motorola.contextual.smartnetwork.db.DbWrapper;
import com.motorola.contextual.smartnetwork.db.SmartNetworkDbFactory;
import com.motorola.contextual.smartnetwork.db.Table;
import com.motorola.contextual.smartnetwork.db.Tuple;

public abstract class SmartNetworkDbTable<T extends Tuple> implements Table<T> {

    public final long insert(Context context, T tuple) {
        long inserted = -1;

        if (context != null && tuple != null) {
            DbWrapper db = SmartNetworkDbFactory.create(context);
            inserted = db.insert(getTableName(), tuple.toContentValues());
            db.close();
        }
        return inserted;
    }

    public final List<T> query(Context context, String[] columns, String selection,
                               String[] selectionArgs, String groupBy, String having, String orderBy,
                               String limit) {
        List<T> tuples = null;

        if (context != null) {
            DbWrapper db = SmartNetworkDbFactory.create(context);
            Cursor cursor = db.query(getTableName(), columns, selection, selectionArgs, groupBy,
                                     having, orderBy, limit);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    tuples = new LinkedList<T>();
                    if (columns == null) {
                        columns = cursor.getColumnNames();
                    }
                    do {
                        T tuple = createNewTuple();
                        // only set values for requested columns
                        for (String column : columns) {
                            int columnIndex = cursor.getColumnIndex(column);
                            if (columnIndex >= 0) {
                                tuple.put(column, cursor.getString(columnIndex));
                            }
                        }
                        tuples.add(tuple);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        }
        return tuples;
    }

    public final List<T> rawQuery(Context context, String sql, String[] selectionArgs) {
        List<T> tuples = null;

        if (context != null) {
            DbWrapper db = SmartNetworkDbFactory.create(context);
            Cursor cursor = db.rawQuery(sql, selectionArgs);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    tuples = new LinkedList<T>();
                    String[] columns = cursor.getColumnNames();
                    do {
                        T tuple = createNewTuple();
                        for (int i = 0; i < columns.length; i++) {
                            tuple.rawPut(columns[i], cursor.getString(i));
                        }
                        tuples.add(tuple);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        }
        return tuples;
    }

    public final int update(Context context, T tuple, String whereClause,
                            String[] whereArgs) {
        int updated = 0;

        if (context != null && tuple != null) {
            DbWrapper db = SmartNetworkDbFactory.create(context);
            updated = db.update(getTableName(), tuple.toContentValues(), whereClause, whereArgs);
            db.close();
        }
        return updated;
    }

    public final int delete(Context context, String whereClause, String[] whereArgs) {
        int deleted = 0;
        if (context != null) {
            DbWrapper db = SmartNetworkDbFactory.create(context);
            deleted = db.delete(getTableName(), whereClause, whereArgs);
            db.close();
        }
        return deleted;
    }

    protected abstract T createNewTuple();
}
