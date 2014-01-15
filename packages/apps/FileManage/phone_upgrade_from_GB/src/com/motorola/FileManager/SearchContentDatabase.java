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

package com.motorola.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.motorola.FileManager.utils.IconifiedText;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;


public class SearchContentDatabase {

    public static final String KEY_FOLDER = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_FULL_PATH = SearchManager.SUGGEST_COLUMN_TEXT_2;

    private static final String DATABASE_NAME = "searchcontent";
    private static final String FTS_VIRTUAL_TABLE = "FTSsearchcontent";
    private static final int DATABASE_VERSION = 2;
    private final DictionaryOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String, String> mColumnMap = buildColumnMap();

    public SearchContentDatabase(Context context) {
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
        return query(selection, selectionArgs, columns);

    }

	public void closeDB(){
    		mDatabaseOpenHelper.close();
    	}

    /**
     * Performs a database query.
     * @param selection The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
       
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor =
                builder.query(mDatabaseOpenHelper.getReadableDatabase(), columns, selection,
                        selectionArgs, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    /**
     * This creates/opens the database.
     */
    private static class DictionaryOpenHelper extends SQLiteOpenHelper {
        private static final String TAG = "DictionaryOpenHelper";

        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

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
            mHelperContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
	//delete by amt_xulei for SWITCHUITWOV-69 2012-9-12
	//reason:duplicate in method onOpen
//            loadAllPaths();
	//end delete
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            mDatabase = db;
            loadAllPaths();
        }

        /**
         * Starts a thread to load the database table with files
         */
        private List<IconifiedText> mFileList = new ArrayList<IconifiedText>();

        private void loadAllPaths() {
            File rootDirectory = null;
            Log.d("","loadAllPaths-FileManagerActivity.mCurrentContent = "+FileManagerActivity.mCurrentContent);
		//modify by amt_xulei for SWITCHUITWOV-69 2012-9-12
//            if (FileManagerActivity.mCurrentContent.equals(Util.ID_EXTSDCARD)){
//            	rootDirectory = new File(FileSystem.SDCARD_DIR);
//            } else {
//            	rootDirectory = new File(FileSystem.Internal_SDCARD_DIR);
//            }
//            scanDirectory(rootDirectory);
		//clear data first
		clearTableData();
		//write new data
		rootDirectory = new File(FileSystem.SDCARD_DIR);
		scanDirectory(rootDirectory);
		rootDirectory = new File(FileSystem.Internal_SDCARD_DIR);
		scanDirectory(rootDirectory);
		//end modify
        }

        private void scanDirectory(File directory) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File currentFile : files) {
                    IconifiedText item =
                            IconifiedText.buildIconItem(mHelperContext, currentFile);
                    long id = addWord(item.getText().trim(), item.getPathInfo().trim());
                    if (id < 0) {
                    }
                    /*
                     * FileManagerApp.log(TAG+"Inserted "+id+","+item.getText().trim
                     * ()+","+item.getPathInfo().trim());
                     */
                    mFileList.add(item);
                    if (currentFile.isDirectory()) {
                        scanDirectory(currentFile);
                    }
                }
            } else {
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
                
                ContentValues initialValues = new ContentValues();
                initialValues.put(KEY_FOLDER, word);
                initialValues.put(KEY_FULL_PATH, definition);
                if (cursor != null) {
                    cursor.close();
                }
                return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                return 0;
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

	public int clearTableData(){
        	return mDatabase.delete(FTS_VIRTUAL_TABLE, null, null);
        }
    }

}
