/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * w21782                    12/28/2009                  added new column for Widget ID.
 * w21782                    12/21/2009                  added new column for MIME type.
 * w21782                    11/16/2009                  Initial creation.
 *****************************************************************************************/

package com.motorola.quicknote;

import java.util.HashMap;
import java.lang.StringBuffer;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.motorola.quicknote.QuickNotesDB.QNColumn;

public class QuickNotesProvider extends ContentProvider {

	private static final String TAG = "QuickNotesProvider";

	private static final String DATABASE_NAME = "quick_notes.db";

	// Note: if you update the version number, you must also update the code
	// in updateDatabase() to modify the database (gracefully, if possible).
	// BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call
	// quicknote demo.
	private static final int DATABASE_VERSION = 7;
	// END IKCNDEVICS-504
	private static final String QNOTES_TABLE_NAME = "qnotes";

	private static HashMap<String, String> sQNotesProjectionMap;

	private static final int QNOTES = 1;
	private static final int QNOTE_ID = 2;

	private static final UriMatcher sUriMatcher;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(QuickNotesDB.AUTHORITY, "qnotes", QNOTES);
		sUriMatcher.addURI(QuickNotesDB.AUTHORITY, "qnotes/#", QNOTE_ID);

		sQNotesProjectionMap = new HashMap<String, String>();
		for (QNColumn c : QNColumn.values()) {
			sQNotesProjectionMap.put(c.column(), c.column());
		}
	}

	private static class QNDatabaseHelper extends SQLiteOpenHelper {

		QNDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			StringBuffer query = new StringBuffer();
            query.append("CREATE TABLE ").append(QNOTES_TABLE_NAME).append(" (");
            QNColumn[] columns = QNColumn.values(); 
            for(int i=0; i<columns.length -1; i++) {
                query.append(columns[i].column())
                    .append(" ")
                    .append(columns[i].type())
                    .append(",");
            }
            query.append(columns[columns.length -1].column())
                .append(" ")
                .append(columns[columns.length -1].type())
                .append(");");
            db.execSQL(query.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            updateDatabase(db, oldVersion, newVersion);
        }

    }

    private QNDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new QNDatabaseHelper(getContext());
        return true;
    }


    /**
     * This method takes care of updating all the tables in the database to the
     * current version, creating them if necessary.
     * This method can only update databases at 5 or higher.
     * Older database will be cleared and recreated.
     * @param db Database
     */
    private static void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
       QNDev.log(TAG+ "Upgrading database from version " + oldVersion + " to "
                    + newVersion);
        
       if ((oldVersion < 5) || (oldVersion > newVersion)) {
           QNDev.log(TAG+ "oldVersion < 4 or oldVersion > newVersion, Drop the old table and create a new one!");
           db.execSQL("DROP TABLE IF EXISTS " + QNOTES_TABLE_NAME);
           //create a new table
           StringBuffer query = new StringBuffer();
           query.append("CREATE TABLE ").append(QNOTES_TABLE_NAME).append(" (");
           QNColumn[] columns = QNColumn.values(); 
           for(int i=0; i<columns.length -1; i++) {
              query.append(columns[i].column())
                  .append(" ").append(columns[i].type())
                  .append(",");
           }
           query.append(columns[columns.length -1].column())
               .append(" ").append(columns[columns.length -1].type())
               .append(");");
           db.execSQL(query.toString());
           return; // this was lossy
       } 


       if (oldVersion == 5) {
           QNDev.log(TAG+ "Upgrading " + QNOTES_TABLE_NAME + " table from version " + oldVersion + " to " + oldVersion + 1);
           db.execSQL("ALTER TABLE " + QNOTES_TABLE_NAME +" ADD COLUMN reminder TEXT DEFAULT 0;");
           oldVersion += 1;
       }

       // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-504/Quicknote: in-call quicknote demo.
       if (oldVersion == 6) {
           QNDev.log(TAG+ "Upgrading " + QNOTES_TABLE_NAME + " table from version " + oldVersion + " to " + oldVersion + 1);
           db.execSQL("ALTER TABLE " + QNOTES_TABLE_NAME +" ADD COLUMN call_number TEXT;");
           db.execSQL("ALTER TABLE " + QNOTES_TABLE_NAME +" ADD COLUMN call_date_begin LONG;");
           db.execSQL("ALTER TABLE " + QNOTES_TABLE_NAME +" ADD COLUMN call_date_end LONG;");
           db.execSQL("ALTER TABLE " + QNOTES_TABLE_NAME +" ADD COLUMN call_type INTEGER;");
           db.execSQL("ALTER TABLE " + QNOTES_TABLE_NAME +" ADD COLUMN summary TEXT;");
           oldVersion += 1;
       }
       // END IKCNDEVICS-504
    }



    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(QNOTES_TABLE_NAME);
        qb.setProjectionMap(sQNotesProjectionMap);

        QNDev.log("DB query: uri (" + uri.toString() + ")");
        switch (sUriMatcher.match(uri)) {
            case QNOTES:
                break;
            case QNOTE_ID:
                qb.appendWhere(QNColumn._ID + "=" + uri.getPathSegments().get(1));
                break;
    
            default:
                QNDev.qnAssert(false);
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (c == null) { return null;}

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        QNDev.log("DB query: return cursor");
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case QNOTES:
                return QuickNotesDB.CONTENT_TYPE;
            case QNOTE_ID:
                return QuickNotesDB.CONTENT_ITEM_TYPE;
    
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested URI
        if (sUriMatcher.match(uri) != QNOTES) { QNDev.qnAssert(false); }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        QNDev.log("DB insert: uri (" + uri.toString() + ")");
        for(QNColumn c : QNColumn.values()) {
            if (!values.containsKey(c.column())) {
                if(null != c.init_value()) {
                    if("TEXT".equals(c.type())) {
                        values.put(c.column(), (String)c.init_value());
                    } else if ("INTEGER".equals(c.type())) {
                        values.put(c.column(), (Integer)c.init_value());
                    } else {
                        QNDev.qnAssert(false);
                    }
                }
            }
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(QNOTES_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri qnoteUri = ContentUris.withAppendedId(QuickNotesDB.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(qnoteUri, null);
            return qnoteUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;

        QNDev.log("DB update: uri (" + uri.toString() + ")");
        switch (sUriMatcher.match(uri)) {
            case QNOTES:
                count = db.update(QNOTES_TABLE_NAME, values, where, whereArgs);
                break;
            case QNOTE_ID:
                String noteId = uri.getPathSegments().get(1);
                count = db.update(QNOTES_TABLE_NAME, values, QNColumn._ID.column() + "=" + noteId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
    
            default:
                QNDev.qnAssert(false);
        }


        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        QNDev.log("DB delete: uri (" + uri.toString() + ")");
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case QNOTES:
                count = db.delete(QNOTES_TABLE_NAME, where, whereArgs);
                break;
            case QNOTE_ID:
                String noteId = uri.getPathSegments().get(1);
                count = db.delete(QNOTES_TABLE_NAME, QNColumn._ID.column() + "=" + noteId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
    
            default:
                QNDev.qnAssert(false);
        }


        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
