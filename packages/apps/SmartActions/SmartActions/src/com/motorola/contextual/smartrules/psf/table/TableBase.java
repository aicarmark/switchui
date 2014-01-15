/*
 * @(#)TableBase.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.table;

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.PsfProvider;
import com.motorola.contextual.smartrules.psf.mediator.MediatorProvider;

/** class TableBase - Defines constants used by all tables implements Constants
 *<code><pre>
 * CLASS:
 *     TableBase - Defines constants used by all tables implements Constants
 *                 Functions required by Content Providers are defined here (generalization)
 *
 * RESPONSIBILITIES:
 *     Defines constants used by all tables
 *     Base class for each table required in PSF
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public abstract class TableBase implements DbSyntax, PsfConstants, BaseColumns  {

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

    private String                  TAG                 = PsfConstants.PSF_PREFIX + TableBase.class.getSimpleName();
    private static String           EMPTY_STRING        = "";
    private static final String     CONTENT_TYPE_PREFIX         = "vnd.motorola.cursor.dir/vnd.motorola.";
    private static final String     CONTENT_ITEM_TYPE_PREFIX    = "vnd.motorola.cursor.item/vnd.motorola.";

    protected Context mContext  = null;

    private interface UriMatch {
        static final int TABLE_LOCAL_PUBLISHER              = 0x1;
        static final int TABLE_LOCAL_PUBLISHER_ROW          = 0x2;
        static final int TABLE_LOCAL_PUBLISHER_CONFIG       = 0x3;
        static final int TABLE_LOCAL_PUBLISHER_CONFIG_ROW   = 0x4;
        static final int TABLE_MEDIATOR               = 0x5;
        static final int TABLE_MEDIATOR_ROW           = 0x6;
    }

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(PsfProvider.AUTHORITY, LocalPublisherTable.TABLE_NAME, UriMatch.TABLE_LOCAL_PUBLISHER);
        sUriMatcher.addURI(PsfProvider.AUTHORITY, LocalPublisherTable.TABLE_NAME + "/#",        UriMatch.TABLE_LOCAL_PUBLISHER_ROW);
        sUriMatcher.addURI(PsfProvider.AUTHORITY, LocalPublisherConfigTable.TABLE_NAME,         UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG);
        sUriMatcher.addURI(PsfProvider.AUTHORITY, LocalPublisherConfigTable.TABLE_NAME + "/#",  UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG_ROW);
        sUriMatcher.addURI(MediatorProvider.AUTHORITY, MediatorTable.TABLE_NAME,                     UriMatch.TABLE_MEDIATOR);
        sUriMatcher.addURI(MediatorProvider.AUTHORITY, MediatorTable.TABLE_NAME + "/#",              UriMatch.TABLE_MEDIATOR_ROW);

    }

    /**
     *
     * @param context   -  Context to work with
     * @param tag       -  Tag for logging purposes
     * @param uri       -  uri that determines which table object to return
     * @param dbase     -  dbase instance to work with
     * @return          -  The right table object
     */
    public static TableBase getTable(Context context, String tag, Uri uri) {
        int match = sUriMatcher.match(uri);
        TableBase table = null;

        switch (match) {
        case UriMatch.TABLE_LOCAL_PUBLISHER:
        case UriMatch.TABLE_LOCAL_PUBLISHER_ROW: {
            table = new LocalPublisherTable(context, tag);
            break;
        }
        case UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG:
        case UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG_ROW: {
            table = new LocalPublisherConfigTable(context, tag);
            break;
        }
        case UriMatch.TABLE_MEDIATOR:
        case UriMatch.TABLE_MEDIATOR_ROW: {
            table = new MediatorTable(context, tag);
            break;
        }
        default: {
            break;
        }
        }

        return table;
    }

    /**
     * Returns the type of the URI
     * @param uri - URI for which type is required
     * @return - type
     */
    public static String getType(Uri uri) {
        String type = null;

        switch (sUriMatcher.match(uri)) {
        case UriMatch.TABLE_LOCAL_PUBLISHER:
            type = CONTENT_TYPE_PREFIX + LocalPublisherTable.TABLE_NAME;
            break;
        case UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG:
            type = CONTENT_ITEM_TYPE_PREFIX + LocalPublisherTable.TABLE_NAME;
            break;
        case UriMatch.TABLE_LOCAL_PUBLISHER_ROW:
            type = CONTENT_TYPE_PREFIX + LocalPublisherConfigTable.TABLE_NAME;
            break;
        case UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG_ROW:
            type = CONTENT_ITEM_TYPE_PREFIX + LocalPublisherConfigTable.TABLE_NAME;
            break;
        case UriMatch.TABLE_MEDIATOR:
            type = CONTENT_TYPE_PREFIX + MediatorTable.TABLE_NAME;
            break;
        case UriMatch.TABLE_MEDIATOR_ROW:
            type = CONTENT_ITEM_TYPE_PREFIX + MediatorTable.TABLE_NAME;
            break;
        }

        return type;
    }
    /**
     * Constructor
     * @param context - Context to work with
     * @param tag     - Tag used for logging
     * @param dbase   - database instance to operate on
     */
    public TableBase(Context context, String tag) {
        mContext = context;
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

        count = db.delete(getTableName(), extendWhere(uri, selection), selectionArgs);

        return count;
    }

    /**
     * Content Resolver's insert functionality with URI supplied by children
     * @param values - column values to be inserted
     * @return - Uri of the row just inserted
     */
    public Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
        long rowId;

        if (db == null) {
            throw new IllegalArgumentException("db cannot be null");
        }

        rowId = db.insert(getTableName(), EMPTY_STRING, values);

        Uri returnUri = null;

        if (rowId > 0) {
            returnUri = ContentUris.withAppendedId(getContentUri(), rowId);
        }

        return returnUri;
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
        Cursor cursor = qb.query(db, projection, extendWhere(uri, selection), selectionArgs, null, null,sortOrder);
        return cursor;
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

        count = db.update(getTableName(), values, extendWhere(uri, selection), selectionArgs);
        return count;
    }

    /**
     * Used to extend the where clause when the URI has a row_id qualifier
     * @param uri    - Uri passed by the content resolver
     * @param where  - actual where clause passed by the resolver
     * @return       - actual where clause + row_id clause(for item types)
     * @throws IllegalArgumentException
     */
    private String extendWhere(Uri uri, String where) throws IllegalArgumentException {
        switch (sUriMatcher.match(uri)) {
        case UriMatch.TABLE_LOCAL_PUBLISHER:
        case UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG:
        case UriMatch.TABLE_MEDIATOR:
        case UriMatch.TABLE_MEDIATOR_ROW:
            break;
        case UriMatch.TABLE_LOCAL_PUBLISHER_ROW:
        case UriMatch.TABLE_LOCAL_PUBLISHER_CONFIG_ROW:
            try {
                String id = uri.getPathSegments().get(1);
                String whereID = _ID + " = " + id;
                if (where != null && where.length() > 0) {
                    whereID = whereID + " AND ( " + where + " )";
                }
                where = whereID;
            } catch (Exception e) {
                Log.e(TAG, "Error in extendWhere " + uri.toString());
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return where;
    }
}
