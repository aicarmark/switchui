/*
 * @(#)MediatorProvider.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/05/17                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.mediator;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;
import com.motorola.contextual.smartrules.psf.table.TableBase;

/** Content provider for publisher provider
 *<code><pre>
 * CLASS:
 *     MediatorProvider Extends ContentProvider
 *
 * RESPONSIBILITIES:
 *     Implements abstract functions of ContentProvider
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class MediatorProvider extends ContentProvider implements PsfConstants {

    public  static final    String          AUTHORITY           = MediatorProvider.class.getPackage().getName();

    private static final    String          DATABASE_NAME       = "mediator.db";
    private static final    int             DATABASE_VERSION    = 1;
    private static final    String          TAG                 = MediatorProvider.class.getSimpleName();
    private                 DatabaseHelper  mDbHelper;


    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            Log.i(TAG, "Constructor");
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(MediatorTable.CREATE_TABLE_SQL);
            Log.i(TAG, "onCreate");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOG_INFO) Log.i(TAG, "Upgrading database from version " + oldVersion
                                    + " to " + newVersion);

            if (LOG_WARN) Log.w(TAG, "Database upgrade called but unimplemented");
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOG_WARN) Log.w(TAG, "Database downgrade called but unimplemented");
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (LOG_DEBUG) Log.d(TAG, "Deleting " + uri.toString() + " for " + selection);
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        int count = -1;

        TableBase table = TableBase.getTable(getContext(), TAG, uri);
        if (table != null) {
            count = table.delete(db, uri, selection, selectionArgs);
            if (count > 0) {
                notifyChange(uri);
            }
        } else {
            if (LOG_ERROR) Log.e(TAG, "Unable to get a table object");
        }

        return count;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        return TableBase.getType(uri);
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (LOG_DEBUG) Log.d(TAG, "Inserting into " + uri.toString());

        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        Uri returnUri = null;
        TableBase table = TableBase.getTable(getContext(), TAG, uri);
        if (table != null) {
            returnUri = table.insert(db, uri, values);
            if (returnUri != null) {
                notifyChange(uri);
            }
        } else {
            if (LOG_ERROR) Log.e(TAG, "Unable to get a table object");
        }

        return returnUri;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        if (LOG_DEBUG) Log.d(TAG, "Mediator db initialized");
        return true;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (LOG_DEBUG) Log.d(TAG, "Querying " + uri.toString()
                                 + " for " + selection
                                 + " Params " + (selectionArgs != null ? TextUtils.join(",", selectionArgs) : "null"));

        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        TableBase table = TableBase.getTable(getContext(), TAG, uri);
        Cursor cursor = null;
        if (table != null) {
            cursor = table.query(db, uri, projection, selection, selectionArgs, sortOrder, null);
        } else {
            if (LOG_ERROR) Log.e(TAG, "Unable to get a table object");
        }

        return cursor;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (LOG_DEBUG) Log.d(TAG, "Updating " + uri.toString() + " for " + selection);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = -1;

        TableBase table = TableBase.getTable(getContext(), TAG, uri);
        if (table != null) {
            count = table.update(db, uri, values, selection, selectionArgs);
            if (count > 0) {
                notifyChange(uri);
            }
        } else {
            if (LOG_ERROR) Log.e(TAG, "Unable to get a table object");
        }

        return count;
    }

    /**
     * Notifies the observers on the change in object pointed by uri
     * @param uri - Uri that has changed
     */
    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null,
                true);
    }

    /**
     * Returns database name;
     * @return
     */
    public static String getDbName() {
        return DATABASE_NAME;
    }

    private DatabaseHelper getDbHelper() {

        if (mDbHelper == null) {
            mDbHelper = new DatabaseHelper(getContext());
        }

        return mDbHelper;
    }

}
