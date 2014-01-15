/**
 * Copyright (C) 2009, Motorola, Inc,
 *
 * This file is derived in part from code issued under the following license.
 * This class was initially trimmed from Android's SyncableContentProvider.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.mmsp.motohomex.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
public abstract class DatabaseContentProvider extends ContentProvider {

    //Constants
    private static final String TAG = "DbContentProvider"; // tag must be < 23 chars

    //Fields
    protected SQLiteOpenHelper mDbHelper;
    /*package*/final int mDbVersion;
    private final String mDbName;

    /**
     * Initializes the DatabaseContentProvider
     * @param dbName the filename of the database
     * @param dbVersion the current version of the database schema
     * @param contentUri The base Uri of the syncable content in this provider
     */
    public DatabaseContentProvider(String dbName, int dbVersion) {
        super();
        mDbName = dbName;
        mDbVersion = dbVersion;
    }

    /**
     * bootstrapDatabase() allows the implementer to set up their database
     * after it is opened for the first time.  this is a perfect place
     * to create tables and triggers :)
     * @param db
     */
    protected void bootstrapDatabase(SQLiteDatabase db) {
    }

    /**
     * updgradeDatabase() allows the user to do whatever they like
     * when the database is upgraded between versions.
     * @param db - the SQLiteDatabase that will be upgraded
     * @param oldVersion - the old version number as an int
     * @param newVersion - the new version number as an int
     * @return
     */
    protected abstract boolean upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion);

    /**
     * Safely wraps an ALTER TABLE table ADD COLUMN columnName columnType
     * If columnType == null then it's set to INTEGER DEFAULT 0
     * @param db - db to alter
     * @param table - table to alter
     * @param columnDef
     * @return
     */
    protected static boolean addColumn(SQLiteDatabase db, String table, String columnName,
            String columnType) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(table).append(" ADD COLUMN ").append(columnName).append(
                ' ').append(columnType == null ? "INTEGER DEFAULT 0" : columnType).append(';');
        try {
            db.execSQL(sb.toString());
        } catch (SQLiteException e) {
            Log.d(TAG, "Alter table failed : " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * onDatabaseOpened() allows the user to do whatever they might
     * need to do whenever the database is opened
     * @param db - SQLiteDatabase that was just opened
     */
    protected void onDatabaseOpened(SQLiteDatabase db) {
    }

    private class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context, String name) {
            // Note: context and name may be null for temp providers
            super(context, name, null, mDbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            bootstrapDatabase(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            upgradeDatabase(db, oldVersion, newVersion);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            onDatabaseOpened(db);
        }
    }

    /**
     * deleteInternal allows getContentResolver().delete() to occur atomically
     * via transactions and notify the uri automatically upon completion (provided
     * rows were deleted) - otherwise, it functions exactly as getContentResolver.delete()
     * would on a regular ContentProvider
     * @param uri - uri to delete from
     * @param selection - selection used for the uri
     * @param selectionArgs - selection args replacing ?'s in the selection
     * @return returns the number of rows deleted
     */
    protected abstract int deleteInternal(final SQLiteDatabase db, Uri uri, String selection,
            String[] selectionArgs);

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int result = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if (isClosed(db)) {
            return result;
        }
        try {
            //acquire reference to prevent from garbage collection
            db.acquireReference();
            //beginTransaction can throw a runtime exception 
            //so it needs to be moved into the try
            db.beginTransaction();
            result = deleteInternal(db, uri, selection, selectionArgs);
            db.setTransactionSuccessful();
        } catch (SQLiteFullException fullEx) {
            Log.e(TAG, "exception :" + fullEx);
        } catch (Exception e) {
            Log.e(TAG, "exception :" + e);
        } finally {
            try {
                db.endTransaction();
            } catch (SQLiteFullException fullEx) {
                Log.e(TAG, "exception :" + fullEx);
            } catch (Exception e) {
                Log.e(TAG, "exception :" + e);
            }
            //release reference
            db.releaseReference();
        }
        // don't check return value because it may be 0 if all rows deleted
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    /**
     * insertInternal allows getContentResolver().insert() to occur atomically
     * via transactions and notify the uri automatically upon completion (provided
     * rows were added to the db) - otherwise, it functions exactly as getContentResolver().insert()
     * would on a regular ContentProvider
     * @param uri - uri on which to insert
     * @param values - values to insert
     * @return returns the uri of the row added
     */
    protected abstract Uri insertInternal(final SQLiteDatabase db, Uri uri, ContentValues values);

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri result = null;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if (isClosed(db)) {
            return result;
        }
        try {
            db.acquireReference();
            //beginTransaction can throw a runtime exception 
            //so it needs to be moved into the try
            db.beginTransaction();
            result = insertInternal(db, uri, values);
            db.setTransactionSuccessful();
        } catch (SQLiteFullException fullEx) {
            Log.w(TAG, "exception :" + fullEx);
        } catch (Exception e) {
            Log.w(TAG, "exception :" + e);
        } finally {
            try {
                db.endTransaction();
            } catch (SQLiteFullException fullEx) {
                Log.w(TAG, "exception :" + fullEx);
            } catch (Exception e) {
                Log.w(TAG, "exception :" + e);
            }
            db.releaseReference();
        }
        if (result != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return result;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext(), mDbName);
        return onCreateInternal();
    }

    /**
     * Called by onCreate.  Should be overridden by any subclasses
     * to handle the onCreate lifecycle event.
     * 
     * @return
     */
    protected boolean onCreateInternal() {
        return true;
    }

    /**
     * queryInternal allows getContentResolver().query() to occur
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return Cursor holding the contents of the requested query
     */
    protected abstract Cursor queryInternal(final SQLiteDatabase db, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder);

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        if (isClosed(db)) {
            return null;
        }

        try {
            db.acquireReference();
            return queryInternal(db, uri, projection, selection, selectionArgs, sortOrder);
        } finally {
            db.releaseReference();
        }
    }

    protected abstract int updateInternal(final SQLiteDatabase db, Uri uri, ContentValues values,
            String selection, String[] selectionArgs);

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int result = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if (isClosed(db)) {
            return result;
        }
        try {
            db.acquireReference();
            //beginTransaction can throw a runtime exception 
            //so it needs to be moved into the try
            db.beginTransaction();
            result = updateInternal(db, uri, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } catch (SQLiteFullException fullEx) {
            Log.e(TAG, "exception :" + fullEx);
        } catch (Exception e) {
            Log.e(TAG, "exception :" + e);
        } finally {
            try {
                db.endTransaction();
            } catch (SQLiteFullException fullEx) {
                Log.e(TAG, "exception :" + fullEx);
            } catch (Exception e) {
                Log.e(TAG, "exception :" + e);
            }
            db.releaseReference();
        }
        if (result > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return result;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int added = 0;
        if (values != null) {
            int numRows = values.length;
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            if (isClosed(db)) {
                return added;
            }
            try {
                db.acquireReference();
                //beginTransaction can throw a runtime exception 
                //so it needs to be moved into the try
                db.beginTransaction();

                for (int i = 0; i < numRows; i++) {
                    if (insertInternal(db, uri, values[i]) != null) {
                        added++;
                    }
                }
                db.setTransactionSuccessful();
                if (added > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            } catch (SQLiteFullException fullEx) {
                Log.e(TAG, "exception :" + fullEx);
            } catch (Exception e) {
                Log.e(TAG, "exception :" + e);
            } finally {
                try {
                    db.endTransaction();
                } catch (SQLiteFullException fullEx) {
                    Log.e(TAG, "exception :" + fullEx);
                } catch (Exception e) {
                    Log.e(TAG, "exception :" + e);
                }
                db.releaseReference();
            }
        }
        return added;
    }

    private boolean isClosed(SQLiteDatabase db) {
        if (db == null || !db.isOpen()) {
            Log.w(TAG, "Null DB returned from DBHelper for a writable/readable database.");
            return true;
        }
        return false;
    }
}
