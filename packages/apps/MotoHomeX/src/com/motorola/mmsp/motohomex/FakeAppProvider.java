/*
 * Copyright (C) 2008 The Android Open Source Project
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
/**
*Create by ncqp34 at Mar-20-2012 for fake app 
*
*/

package com.motorola.mmsp.motohomex;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.provider.BaseColumns;

public class FakeAppProvider extends ContentProvider {

    private static final String TAG = "Launcher.AppProvider";
    private static final boolean LOGD = true;

    public static final String DATABASE_NAME = "app.db";

    private static final int DATABASE_VERSION = 9;

    static final String AUTHORITY = "com.motorola.mmsp.motohomex.fake";

    static final String TABLE_FAKE_APPLICATION = "fakeApp";

    static final String PARAMETER_NOTIFY = "notify"; 
    private SQLiteDatabase mDatabase;
    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
	Log.d(TAG,"FakeAppProvider---onCreate---ENTER");
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    private static long dbInsertAndCheck(DatabaseHelper helper,
            SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        return db.insert(table, nullColumnHack, values);
    }

    private static void deleteId(SQLiteDatabase db, long id) {
        Uri uri = LauncherSettings.Favorites.getContentUri(id, false);
        SqlArguments args = new SqlArguments(uri, null, null);
        db.delete(args.table, args.where, args.args);
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
	Log.d(TAG,"FakeAppProvider---insert--uri==" + uri  + ",initialValues=" + initialValues);

        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
	Log.d(TAG,"FakeAppProvider---insert-rowId ==" + rowId);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
	Log.d(TAG,"FakeAppProvider---bulkInsert--uri==" + uri  + ",values,=" + values);

        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }


 private class DatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG_FAKE_APP = "fakeApp";

        private final Context mContext;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            Log.d(TAG, "DatabaseHelper constructor");
            mContext = context;
          }

        @Override
        public void onCreate(SQLiteDatabase db) {
           Log.d(TAG, "creating new fake database");

            db.execSQL("CREATE TABLE fakeApp (" +
                    "_id INTEGER PRIMARY KEY," +
                    "componentName TEXT," +
                    "title TEXT," +
                    "icon BLOB," +
                    "uri TEXT," +
        	    "hidden INTEGER" +
                       ");");

	    Flex.getFakeAppFromCDA(db, mContext);
        }

    @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG,"onUpgrade triggered");

            int version = oldVersion;

            if (version < 9) {
                version = 9;
            }

            if (version != DATABASE_VERSION) {
                Log.w(TAG, "Destroying all old data.");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAKE_APPLICATION);
                onCreate(db);
            }
	}

    }

   private static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);                
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }


  /**
     * Favorites.
     */
    static final class FakeApplication implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        static final Uri CONTENT_URI = Uri.parse("content://" +
                FakeAppProvider.AUTHORITY + "/" + FakeAppProvider.TABLE_FAKE_APPLICATION);

        /**
         * The content:// style URL for this table. When this Uri is used, no notification is
         * sent if the content changes.
         */
        static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" +
                FakeAppProvider.AUTHORITY + "/" + FakeAppProvider.TABLE_FAKE_APPLICATION +
                "?" + FakeAppProvider.PARAMETER_NOTIFY + "=false");

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + FakeAppProvider.AUTHORITY +
                    "/" + FakeAppProvider.TABLE_FAKE_APPLICATION + "/" + id);
        }

        /**
         * The componentName for the to-be-install application.
         * <P>Type: TEXT</P>
         */
        static final String COMPONENT_NAME = "componentName";
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>Type: TEXT</P>
         */
        static final String TITLE = "title";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>Type: BLOB</P>
         */
        static final String ICON = "icon";
        /**
         * The URI to launche the google play.
         * <P>Type: TEXT</P>
         */
        static final String URI = "uri";

        /**
         * The state to indicate show/hide.
         * <P>Type: INTEGER</P>
         */
        static final String HIDDEN = "hidden";
        public static final int INDEX_ID = 0;
        public static final int INDEX_COMPONENT = 1;
        public static final int INDEX_TITLE = 2;
        public static final int INDEX_ICON = 3;
        public static final int INDEX_URI = 4;
        public static final String [] PROJECTION = {
            _ID, COMPONENT_NAME, TITLE, ICON, URI,
        };
     	}
}
