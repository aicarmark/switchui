/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    DTW768      initial
 */

package com.motorola.filemanager.local.search.database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.ui.IconifiedText;

public class SearchContentDatabase {
    private static final String TAG = "SearchContentDatabase";

    public static final String KEY_FOLDER = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_FULL_PATH = SearchManager.SUGGEST_COLUMN_TEXT_2;

    private static final String DATABASE_NAME = "searchcontent";
    private static final String FTS_VIRTUAL_TABLE = "FTSsearchcontent";
    private static final int DATABASE_VERSION = 2;
    private final DictionaryOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String, String> mColumnMap = buildColumnMap();

    public SearchContentDatabase(Context context) {
        FileManagerApp.log(TAG + " SearchContentDatabase() ", false);
        mDatabaseOpenHelper = new DictionaryOpenHelper(context);
    }

    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(KEY_FOLDER, KEY_FOLDER);
        map.put(KEY_FULL_PATH, KEY_FULL_PATH);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        return map;
    }

    public Cursor getPath(String rowId, String[] columns) {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[]{rowId};

        return query(selection, selectionArgs, columns);

        /*
         * This builds a query that looks like: SELECT &lt;columns&gt; FROM
         * &lt;table&gt; WHERE rowid = &lt;rowId&gt;
         */
    }

    /**
     * Returns a Cursor over all files or folders that match the given query
     *
     * @param query The string to search for
     * @param columns The columns to include, if null then all are included
     * @return Cursor over all files that match, or null if none found.
     */
    public Cursor getPathMatches(String query, String[] columns) {
        String selection = KEY_FOLDER + " MATCH ?";
        String[] selectionArgs = new String[]{"*" + query + "*"};
        FileManagerApp.log(TAG + " getPathMatches() " + "query=" + query, false);
        return query(selection, selectionArgs, columns);

        /*
         * This builds a query that looks like: SELECT &lt;columns&gt; FROM
         * &lt;table&gt; WHERE &lt;KEY_WORD&gt; MATCH '*query*' which is an FTS3
         * search for the query text (plus a wildcard) inside the word column. -
         * "rowid" is the unique id for all rows but we need this value for the
         * "_id" column in order for the Adapters to work, so the columns need
         * to make "_id" an alias for "rowid" - "rowid" also needs to be used by
         * the SUGGEST_COLUMN_INTENT_DATA alias in order for suggestions to
         * carry the proper intent data. These aliases are defined in the
         * DictionaryProvider when queries are made. - This can be revised to
         * also search the definition text with FTS3 by changing the selection
         * clause to use FTS_VIRTUAL_TABLE instead of KEY_WORD (to search across
         * the entire table, but sorting the relevance could be difficult.
         */
    }

    /**
     * Performs a database query.
     * @param selection The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        /*
         * The SQLiteBuilder provides a map for all possible columns requested
         * to actual columns in the database, creating a simple column alias
         * mechanism by which the ContentProvider does not need to know the real
         * column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor =
                builder.query(mDatabaseOpenHelper.getReadableDatabase(), columns, selection,
                        selectionArgs, null, null, null);

        if (cursor == null) {
            FileManagerApp.log(TAG + " query() cursor is null", true);
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            FileManagerApp.log(TAG + " query() cursor is empty", true);
            return null;
        }
        FileManagerApp.log(TAG + " query() returned cursor with size=" + cursor.getCount(), false);
        return cursor;
    }

    /**
     * This creates/opens the database.
     */
    private static class DictionaryOpenHelper extends SQLiteOpenHelper {
        private static final String TAG = "DictionaryOpenHelper";

        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;
        private FileManagerApp mFileManagerApp;

        /*
         * Note that FTS3 does not support column constraints and thus, you
         * cannot declare a primary key. However, "rowid" is automatically used
         * as a unique identifier, so when making requests, we will use "_id" as
         * an alias for "rowid"
         */
        private static final String FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                " USING fts3 (" + KEY_FOLDER + ", " + KEY_FULL_PATH + ");";

        DictionaryOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            FileManagerApp.log(TAG + " DictionaryOpenHelper() ", false);
            mHelperContext = context;
            mFileManagerApp = ((FileManagerApp) mHelperContext.getApplicationContext());
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            FileManagerApp.log(TAG + " onCreate() ", false);
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
            loadAllPaths();
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            FileManagerApp.log(TAG + " onOpen() ", false);
            mDatabase = db;
            // need to add code to delete all rows from table
            //mDatabase.execSQL(FTS_TABLE_DELETE);
            // comment out this line after 1 run
            loadAllPaths();
        }

        /**
         * Starts a thread to load the database table with files
         */
        private List<IconifiedText> mFileList = new ArrayList<IconifiedText>();

        private void loadAllPaths() {
            File rootDirectory = new File(mFileManagerApp.SD_CARD_DIR);
            // need to add for other storage
            FileManagerApp.log(TAG + ":loadAllPaths() " + rootDirectory, false);
            scanDirectory(rootDirectory, FileManagerApp.INDEX_INTERNAL_MEMORY);
            FileManagerApp.log(
                    TAG + ":loadAllPaths(), after scan mFileList.size= " + mFileList.size(), false);
            /*
             * for (IconifiedText currentFile : mFileList) { long id =
             * addWord(currentFile.getText().trim(),
             * currentFile.getPathInfo().trim()); if (id < 0) {
             * FileManagerApp.log(TAG +":unable to add path: " +
             * currentFile.getText().trim()); }
             * FileManagerApp.log(TAG+"Inserted "
             * +id+","+currentFile.getText().trim
             * ()+","+currentFile.getPathInfo().trim()); }
             */
        }

        private void scanDirectory(File directory, int mode) {
            File[] files = directory.listFiles();
            FileManagerApp.log(TAG + ":scanDirectory() scaning" + directory, false);
            if (files != null) {
                for (File currentFile : files) {
                    IconifiedText item =
                            IconifiedText.buildIconItem(mHelperContext, currentFile, mode);
                    long id = addWord(item.getText().trim(), item.getPathInfo().trim());
                    if (id < 0) {
                        FileManagerApp.log(TAG + ":unable to add path: " + item.getText().trim(),
                                true);
                    }
                    /*
                     * FileManagerApp.log(TAG+"Inserted "+id+","+item.getText().trim
                     * ()+","+item.getPathInfo().trim());
                     */
                    mFileList.add(item);
                    if (currentFile.isDirectory()) {
                        scanDirectory(currentFile, mode);
                    }
                }
            } else {
                FileManagerApp.log(TAG + ":scanDirectory(),files is null, " + directory +
                        "is empty", true);
            }
        }

        public long addWord(String word, String definition) {
            String selection = KEY_FULL_PATH + " MATCH ?";
            String[] selectionArgs = new String[]{definition};
            String[] columns = new String[]{KEY_FULL_PATH};
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(FTS_VIRTUAL_TABLE);
            builder.setProjectionMap(mColumnMap);

            Cursor cursor =
                    builder.query(mDatabase, columns, selection, selectionArgs, null, null, null);

            if (cursor == null || cursor.moveToFirst() == false) {
                FileManagerApp
                        .log(TAG + " query() cursor is null or empty, row doesnt exist", true);
                ContentValues initialValues = new ContentValues();
                initialValues.put(KEY_FOLDER, word);
                initialValues.put(KEY_FULL_PATH, definition);
                if (cursor != null) {
                    cursor.close();
                }
                return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
            } else {
                FileManagerApp.log(TAG + "row exists, skip", false);
                if (cursor != null) {
                    cursor.close();
                }
                return 0;
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            FileManagerApp.log(TAG + ":Upgrading database from version " + oldVersion + " to " +
                    newVersion + ", which will destroy all old data", false);
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }
    }

}
