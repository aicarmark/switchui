/*

 * @(#)TableBase.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21693        2012/02/14                   Initial version
 *
 */
package com.motorola.contextual.rule.publisher.db;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/** class TableBase - Defines constants used by all tables implements Constants
 *<code><pre>
 * CLASS:
 *     TableBase - Defines constants used by all tables implements Constants
 *                 Functions required by Content Providers are defined here (generalization)
 *
 * RESPONSIBILITIES:
 *     Defines constants used by all tables
 *     Base class for each table required in Inference Manager
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public abstract class TableBase {

    /**
     *
     * @return - Table Name to be implemented by derived class
     */
    public abstract String getTableName();

    /**
     *
     * @return - Content uri to be implemented by derived class
     */
    public abstract Uri getContentUri();

    /**
     *
     */
    public static final String SINGLE_SPACE         = " ";

    public String              TAG       = TableBase.class.getSimpleName();

    private interface UriMatch {
        static final int TABLE_LOGIC = 0x1;
    }

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(RulePublisherTable.AUTHORITY, RulePublisherTable.TABLE_NAME, UriMatch.TABLE_LOGIC);
    }

    /**
     *
     * @param context   -  Context to work with
     * @param tag       -  Tag for logging purposes
     * @param uri       -  uri that determines which table object to return
     * @param dbase     -  dbase instance to work with
     * @return          -  The right table object
     */
    public static TableBase getTable(String tag, Uri uri) {
        int match = sUriMatcher.match(uri);
        TableBase table = null;

        switch (match) {
            case UriMatch.TABLE_LOGIC:
            {
                table = new RulePublisherTable(tag);
                break;
            }

            default:
            {
                break;
            }
        }
        return table;
    }

    /**
     * Constructor
     * @param context - Context to work with
     * @param tag     - Tag used for logging
     * @param dbase   - database instance to operate on
     */
    public TableBase(String tag) {
        TAG      = tag;
    }

    /**
     * Content Resolver's delete functionality with URI supplied by children
     * @param selection
     * @param selectionArgs
     * @return - Number of rows deleted
     */
    public int delete(SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
        int count = 0 ;

        if (db == null) {
            throw new IllegalArgumentException("db cannot be null");
        }

        synchronized(db) {
            count = db.delete(getTableName(), selection, selectionArgs);
        }

        return count;
    }

    /**
     * Content Resolver's insert functionality with URI supplied by children
     * @param values - column values to be inserted
     * @return - Uri of the row just inserted
     */
    public long insert(SQLiteDatabase db, ContentValues values) {
        long result = -1;

        try {
            synchronized (db) {
                result = db.insertOrThrow(this.getTableName(), SINGLE_SPACE, values);
            }

        } catch (Exception e) {
            Log.w(TAG, "Insert failed for "+ values.getAsString(RulePublisherTable.Columns.RULE_KEY));
            e.printStackTrace();

        }

        return result;
    }

    /**
     * Content Resolver's query functionality with URI supplied by children
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @param projectionMap
     * @return - Cursor containing the result set.  Caller to manage the cursor
     */
    public Cursor query(SQLiteDatabase db, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder, HashMap<String, String> projectionMap) {

        if (db == null) {
            throw new IllegalArgumentException("db cannot be null");
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(getTableName());
        qb.setProjectionMap(projectionMap);
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,sortOrder);

        return c;
    }

    /**
     * Content Resolver's update functionality with URI supplied by children
     * @param values
     * @param selection
     * @param selectionArgs
     * @return - Number of rows updated
     */
    public int update(SQLiteDatabase db, Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count;

        if (db == null) {
            throw new IllegalArgumentException("db cannot be null");
        }

        synchronized(db) {
            count = db.update(getTableName(), values, selection, selectionArgs);
        }
        return count;
    }
}
