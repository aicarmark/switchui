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
import java.util.Random;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartnetwork.db.Table;

public class PoiTable implements Table<PoiTuple> {
    private static final String TAG = PoiTable.class.getSimpleName();

    public static final String TABLE_NAME = "poi";
    private static final Uri CONTENT_URI = Uri.parse(
            "content://com.motorola.contextual.virtualsensor.locationsensor/poi");

    public static final String POI_TYPE = "hidden_network";

    public String getTableName() {
        return TABLE_NAME;
    }

    public long insert(Context context, PoiTuple tuple) {
        long retVal = 0;
        if (context != null && tuple != null) {
            ContentValues cv = tuple.toContentValues();
            // enforce poi type
            cv.put(PoiTuple.COL_POI_TYPE, POI_TYPE);
            /* TODO:
             * LocationSensor provider checks for overlapping POI using lat,lgt,radius values.
             * It requires lat/lgt/radius to be filled in.
             * For prototype, just fill in random values for lat/lgt so we can get past
             * the overlap filter.
             * Haijin's code should be modified to not check for lat/lgt if
             * PoiType is hidden_network.
             * I am not sure why Haijin's code is checking for overlapping location using lat/lgt,
             * when the rows I am using are consolidated rows using wifi.
             */
            Random random = new Random();
            if (!cv.containsKey(PoiTuple.COL_LAT)) {
                double randomLat = random.nextInt() + random.nextDouble();
                cv.put(PoiTuple.COL_LAT, randomLat);
            }
            if (!cv.containsKey(PoiTuple.COL_LGT)) {
                double randomLgt = random.nextInt() + random.nextDouble();
                cv.put(PoiTuple.COL_LGT, randomLgt);
            }
            if (!cv.containsKey(PoiTuple.COL_RADIUS)) {
                cv.put(PoiTuple.COL_RADIUS, 150);
            }

            ContentResolver cr = context.getContentResolver();
            Uri insertedUri = cr.insert(CONTENT_URI, cv);
            if (insertedUri != null) {
                String rowId = insertedUri.getLastPathSegment();
                try {
                    retVal = Long.parseLong(rowId);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Unable to parse inserted row id.");
                }
            } else {
                Log.e(TAG, "Unable to insert into POI cr.");
            }
        }
        return retVal;
    }

    public List<PoiTuple> query(Context context, String[] columns, String selection,
                                String[] selectionArgs, String groupBy, String having, String orderBy,
                                String limit) {
        List<PoiTuple> tuples = null;

        if (context != null) {
            // groupBy, having, and limit clauses not supported through ContentResolver
            if (groupBy != null || having != null || limit != null) {
                throw new IllegalArgumentException("groupBy, having, and limit clauses are not " +
                                                   "supported on PoiTable query");
            }

            // only select our POI_TYPE by adding an extra selectionArg to filter POI_TYPE
            selection = formatSelection(selection);
            selectionArgs = formatSelectionArgs(selectionArgs);

            tuples = new LinkedList<PoiTuple>();

            ContentResolver cr = context.getContentResolver();
            /* fetch all columns defined in PoiTuple to make sure we get all needed columns.
             * we don't all all columns defined by the actual Poi table,
             * we just want all columns defined in PoiTuple
             */
            Cursor cursor = cr.query(CONTENT_URI, PoiTuple.COLUMNS, selection, selectionArgs,
                                     orderBy);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    if (columns == null) {
                        // get all columns
                        columns = PoiTuple.COLUMNS;
                    }
                    do {
                        PoiTuple poiTuple = new PoiTuple();
                        // only set values for requested columns
                        for (String column : columns) {
                            int columnIndex = cursor.getColumnIndex(column);
                            if (columnIndex >= 0) {
                                poiTuple.put(column, cursor.getString(columnIndex));
                            }
                        }
                        tuples.add(poiTuple);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }
        return tuples;
    }

    public List<PoiTuple> rawQuery(Context context, String sql, String[] selectionArgs) {
        // not supported
        return null;
    }

    public int update(Context context, PoiTuple tuple, String whereClause, String[] whereArgs) {
        int updated = 0;
        if (context != null && tuple != null) {
            ContentResolver cr = context.getContentResolver();
            ContentValues cv = tuple.toContentValues();
            whereClause = formatSelection(whereClause);
            whereArgs = formatSelectionArgs(whereArgs);
            updated = cr.update(CONTENT_URI, cv, whereClause, whereArgs);
        }
        return updated;
    }

    public int delete(Context context, String whereClause, String[] whereArgs) {
        int deleted = 0;
        if (context != null) {
            ContentResolver cr = context.getContentResolver();
            whereClause = formatSelection(whereClause);
            whereArgs = formatSelectionArgs(whereArgs);
            deleted = cr.delete(CONTENT_URI, whereClause, whereArgs);
        }
        return deleted;
    }

    /**
     * Formats the selection statement to force POI_TYPE
     * @param selection selection statement to format
     * @return selection statement with POI_TYPE appended
     */
    private static String formatSelection(String selection) {
        if (selection != null && !selection.isEmpty()) {
            selection = "(" + selection + ") AND " + PoiTuple.COL_POI_TYPE + " = ?";
        } else {
            selection = PoiTuple.COL_POI_TYPE + " = ?";
        }
        return selection;
    }

    /**
     * Formats the selection args to force POI_TYPE
     * @param selectionArgs selectionArgs to format
     * @return selection arguments with POI_TYPE appended
     */
    private static String[] formatSelectionArgs(String[] selectionArgs) {
        String[] newArgs;
        if (selectionArgs != null) {
            newArgs = new String[selectionArgs.length + 1];
            for (int i = 0; i < selectionArgs.length; i++) {
                newArgs[i] = selectionArgs[i];
            }
            newArgs[selectionArgs.length] = POI_TYPE;
        } else {
            newArgs = new String[] { POI_TYPE };
        }
        return newArgs;
    }

}
