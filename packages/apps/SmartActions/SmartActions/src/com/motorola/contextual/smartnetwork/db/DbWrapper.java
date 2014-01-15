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

package com.motorola.contextual.smartnetwork.db;

import android.content.ContentValues;
import android.database.Cursor;

public interface DbWrapper {

    /**
     * Closes the database
     */
    public void close();

    /**
     * Convenience method for inserting a row into the database.
     * @param table the table to insert the row into
     * @param values this map contains the initial column values for the row. The keys should be
     * the column names and the values the column values
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long insert(String table, ContentValues values);

    /**
     * Query the given table, returning a Cursor over the result set.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will return all columns,
     * which is discouraged to prevent reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause
     * (excluding the WHERE itself). Passing null will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     * from selectionArgs, in order that they appear in the selection. The values will be bound as
     * Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL GROUP BY clause
     * (excluding the GROUP BY itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the result,
     * if row grouping is being used, formatted as an SQL HAVING clause
     * (excluding the HAVING itself). Passing null will cause all row groups to be included,
     * and is required when row grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     * (excluding the ORDER BY itself). Passing null will use the default sort order, which may be
     * unordered.
     * @param limit Limits the number of rows returned by the query, formatted as LIMIT clause.
     * Passing null denotes no LIMIT clause.
     * @return A Cursor object, which is positioned before the first entry
     */
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit);

    /**
     * Runs the provided SQL and returns a Cursor over the result set.
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query, which will be replaced
     * by the values from selectionArgs. The values will be bound as Strings.
     * @return
     */
    public Cursor rawQuery(String sql, String[] selectionArgs);

    /**
     * Convenience method for updating rows in the database.
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a valid value that will
     * be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating. Passing null will
     * update all rows.
     * @param whereArgs
     * @return the number of rows affected
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs);

    /**
     * Convenience method for deleting rows in the database.
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting. Passing null will delete all rows.
     * @param whereArgs
     * @return the number of rows affected if a whereClause is passed in, 0 otherwise.
     * To remove all rows and get a count pass "1" as the whereClause.
     */
    public int delete(String table, String whereClause, String[] whereArgs);
}
