/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.filemanager.local;

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
import android.os.FileObserver;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.utils.IconifiedText;

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
	FileManagerApp.log(TAG + " SearchContentDatabase() ");
	mDatabaseOpenHelper = new DictionaryOpenHelper(context);
    }

    private static HashMap<String, String> buildColumnMap() {
	HashMap<String, String> map = new HashMap<String, String>();
	map.put(KEY_FOLDER, KEY_FOLDER);
	map.put(KEY_FULL_PATH, KEY_FULL_PATH);
	map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
	map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS "
		+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
	return map;
    }

    public Cursor getPath(String rowId, String[] columns) {
	String selection = "rowid = ?";
	String[] selectionArgs = new String[] { rowId };

	return query(selection, selectionArgs, columns);

	/*
	 * This builds a query that looks like: SELECT &lt;columns&gt; FROM
	 * &lt;table&gt; WHERE rowid = &lt;rowId&gt;
	 */
    }

    /**
     * Returns a Cursor over all files or folders that match the given query
     * 
     * @param query
     *            The string to search for
     * @param columns
     *            The columns to include, if null then all are included
     * @return Cursor over all files that match, or null if none found.
     */
    public Cursor getPathMatches(String query, String[] columns) {
	String selection = KEY_FOLDER + " MATCH ?";
	String[] selectionArgs = new String[] { "*" + query + "*" };
	FileManagerApp.log(TAG + " getPathMatches() " + "query=" + query);
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
     * 
     * @param selection
     *            The selection clause
     * @param selectionArgs
     *            Selection arguments for "?" components in the selection
     * @param columns
     *            The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs,
	    String[] columns) {
	/*
	 * The SQLiteBuilder provides a map for all possible columns requested
	 * to actual columns in the database, creating a simple column alias
	 * mechanism by which the ContentProvider does not need to know the real
	 * column names
	 */
	SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
	builder.setTables(FTS_VIRTUAL_TABLE);
	builder.setProjectionMap(mColumnMap);

	Cursor cursor = builder.query(
		mDatabaseOpenHelper.getReadableDatabase(), columns, selection,
		selectionArgs, null, null, null);

	if (cursor == null) {
	    FileManagerApp.log(TAG + " query() cursor is null");
	    return null;
	} else if (!cursor.moveToFirst()) {
	    cursor.close();
	    FileManagerApp.log(TAG + " query() cursor is empty");
	    return null;
	}
	FileManagerApp.log(TAG + " query() returned cursor with size="
		+ cursor.getCount());
	return cursor;
    }

    /**
     * This creates/opens the database.
     */
    private/* static */class DictionaryOpenHelper extends SQLiteOpenHelper {
	private static final String TAG = "DictionaryOpenHelper";

	private final Context mHelperContext;
	private SQLiteDatabase mDatabase;
	private HashMap<String, SearchContentDatabase.DirectoryObserver> m_directoryObservers = new HashMap<String, SearchContentDatabase.DirectoryObserver>();

	/*
	 * Note that FTS3 does not support column constraints and thus, you
	 * cannot declare a primary key. However, "rowid" is automatically used
	 * as a unique identifier, so when making requests, we will use "_id" as
	 * an alias for "rowid"
	 */
	private static final String FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE "
		+ FTS_VIRTUAL_TABLE + " USING fts3 (" + KEY_FOLDER + ", "
		+ KEY_FULL_PATH + ");";
	private static final String FTS_TABLE_DELETE = "DELETE FROM "
		+ FTS_VIRTUAL_TABLE;

	DictionaryOpenHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    FileManagerApp.log(TAG + " DictionaryOpenHelper() ");
	    mHelperContext = context;
	}

	Context getContext() {
	    return mHelperContext;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	    FileManagerApp.log(TAG + " onCreate() ");
	    mDatabase = db;
	    mDatabase.execSQL(FTS_TABLE_CREATE);
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
	    FileManagerApp.log(TAG + " onOpen() ");
	    mDatabase = db;
	    // need to add code to delete all rows from table
	    // mDatabase.execSQL(FTS_TABLE_DELETE);
	    // comment out this line after 1 run
	    loadAllPaths();
	}

	/**
	 * Starts a thread to load the database table with files
	 */
	private List<IconifiedText> mFileList = new ArrayList<IconifiedText>();

	private void loadAllPaths() {
	    File rootDirectory = new File(FileManagerApp.SD_CARD_DIR);
	    FileManagerApp.log(TAG + ":loadAllPaths() " + rootDirectory);
	    addObserver(FileManagerApp.SD_CARD_DIR);
	    scanDirectory(rootDirectory);
	    FileManagerApp
		    .log(TAG
			    + ":loadAllPaths(), after scan rootDirectory mFileList.size= "
			    + mFileList.size());
	    if (FileManagerApp.getEMMCEnabled()) {
		File rootDirectory_emmc = new File(
			FileManagerApp.SD_CARD_EXT_DIR);
		FileManagerApp.log(TAG + ":loadAllPaths() /mnt/sdcard-ext "
			+ rootDirectory_emmc);
		addObserver(FileManagerApp.SD_CARD_EXT_DIR);
		scanDirectory(rootDirectory_emmc);
		FileManagerApp
			.log(TAG
				+ ":loadAllPaths(), after scan rootDirectory_emmc mFileList.size= "
				+ mFileList.size());
	    }
	    /*
	     * for (IconifiedText currentFile : mFileList) { long id =
	     * addRecord(currentFile.getText().trim(),
	     * currentFile.getPathInfo().trim()); if (id < 0) {
	     * FileManagerApp.log(TAG +":unable to add path: " +
	     * currentFile.getText().trim()); } FileManagerApp
	     * .log(TAG+"Inserted "+id+","+currentFile.getText().trim()+","
	     * +currentFile.getPathInfo().trim()); }
	     */
	}

	private void scanDirectory(File directory) {
	    File[] files = directory.listFiles();
	    FileManagerApp.log(TAG + ":scanDirectory() scaning" + directory);
	    if (files != null) {
		for (File currentFile : files) {
		    IconifiedText item = IconifiedText.buildIconItem(
			    mHelperContext, currentFile);
		    long id = addRecord(item.getText().trim(), item
			    .getPathInfo().trim());
		    if (id < 0) {
			FileManagerApp.log(TAG + ":unable to add path: "
				+ item.getText().trim());
		    }
		    /*
		     * FileManagerApp.log(TAG+"Inserted "+id+","+item.getText().trim
		     * ()+","+ item.getPathInfo().trim());
		     */
		    mFileList.add(item);
		    if (currentFile.isDirectory()) {
			addObserver(currentFile.getAbsolutePath());
			scanDirectory(currentFile);
		    }
		}
	    } else {
		FileManagerApp.log(TAG + ":scanDirectory(),files is null, "
			+ directory + "is empty");
	    }
	}

	public long addRecord(String folder, String full_path) {
	    if (mDatabase == null) {
		FileManagerApp.log(TAG + "DB is not created or opened yet.");
		return -1;
	    }

	    String selection = KEY_FULL_PATH + "=?";
	    String[] selectionArgs = new String[] { full_path };
	    String[] columns = new String[] { KEY_FULL_PATH };
	    SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
	    builder.setTables(FTS_VIRTUAL_TABLE);
	    builder.setProjectionMap(mColumnMap);

	    Cursor cursor = builder.query(mDatabase, columns, selection,
		    selectionArgs, null, null, null);

	    if (cursor == null || cursor.moveToFirst() == false) {
		// Row doesnt exist,insert
		if (cursor != null) {
		    cursor.close();
		}
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_FOLDER, folder);
		initialValues.put(KEY_FULL_PATH, full_path);
		return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
	    } else {
		cursor.close();
		return 0;
	    }
	}

	public void deleteRecord(String full_path) {
	    if (mDatabase == null) {
		FileManagerApp.log(TAG
			+ "database is not created or opened yet.");
		return;
	    }
	    boolean isDirectory = false;
	    synchronized (this) {
		if (m_directoryObservers.containsKey(full_path)) {
		    isDirectory = true;
		}
	    }
	    if (isDirectory == true) {
		String[] deleteArgs = new String[] { full_path + "/" };
		int numDeleted = mDatabase.delete(FTS_VIRTUAL_TABLE,
			KEY_FULL_PATH + "=?", deleteArgs);
		FileManagerApp.log(TAG + "called delete to delete directory="
			+ full_path + ", number of deleted=" + numDeleted);
	    } else {
		String[] deleteArgs = new String[] { full_path };
		int numDeleted = mDatabase.delete(FTS_VIRTUAL_TABLE,
			KEY_FULL_PATH + "=?", deleteArgs);
		FileManagerApp.log(TAG + "called delete to delete file="
			+ full_path + ", number of deleted=" + numDeleted);
	    }
	}

	public void addObserver(String parent_path) {
	    synchronized (this) {
		if (!m_directoryObservers.containsKey(parent_path)) {
		    int eventMask = FileObserver.CREATE | FileObserver.DELETE
			    | FileObserver.MOVED_FROM | FileObserver.MOVED_TO
			    | FileObserver.DELETE_SELF | FileObserver.MOVE_SELF;
		    SearchContentDatabase.DirectoryObserver directoryObserver = new SearchContentDatabase.DirectoryObserver(
			    this, parent_path, eventMask);
		    m_directoryObservers.put(parent_path, directoryObserver);
		    directoryObserver.startWatching();
		    FileManagerApp.log(TAG + " Added FileObserver for "
			    + parent_path);
		}
	    }
	}

	public void removeObserver(String parent_path) {
	    synchronized (this) {
		if (m_directoryObservers.containsKey(parent_path)) {
		    SearchContentDatabase.DirectoryObserver directoryObserver = m_directoryObservers
			    .remove(parent_path);
		    if (directoryObserver != null) {
			directoryObserver.stopWatching();
		    }
		    FileManagerApp.log(TAG + " Removed FileObserver for "
			    + parent_path);
		}
	    }
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    FileManagerApp.log(TAG + ":Upgrading database from version "
		    + oldVersion + " to " + newVersion
		    + ", which will destroy all old data");
	    db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
	    onCreate(db);
	}
    }

    private static class DirectoryObserver extends FileObserver {
	private static final String TAG = "DirectoryObserver: ";
	private DictionaryOpenHelper m_database = null;
	private String m_parentPath = null;

	public DirectoryObserver(DictionaryOpenHelper _database, String path) {
	    super(path);
	    m_database = _database;
	    m_parentPath = path;
	    // FileManagerApp.log(TAG + " " +
	    // "created observer for parent="+path);
	}

	public DirectoryObserver(DictionaryOpenHelper _database, String path,
		int mask) {
	    super(path, mask);
	    m_database = _database;
	    m_parentPath = path;
	    // FileManagerApp.log(TAG + " " +
	    // "created observer for parent="+path);
	}

	@Override
	public void onEvent(int event, String path) {
	    if (path == null) {
		return;
	    }
	    /*
	     * FileManagerApp.log(TAG + " " + this + " onEvent(): event="+event
	     * +", path="+path);
	     */
	    int createEvent = event & FileObserver.CREATE;
	    int moved_toEvent = event & FileObserver.MOVED_TO;
	    int deleteEvent = event & FileObserver.DELETE;
	    int moved_fromEvent = event & FileObserver.MOVED_FROM;
	    int delete_selfEvent = event & FileObserver.DELETE_SELF;
	    int move_selfEvent = event & FileObserver.MOVE_SELF;
	    if ((createEvent != 0) || (moved_toEvent != 0)) {
		// file added
		if ((createEvent != 0)) {
		    FileManagerApp.log(TAG + " on " + m_parentPath
			    + " onEvent(): event=CREATE" + ", path=" + path);
		} else {
		    FileManagerApp.log(TAG + " on " + m_parentPath
			    + " onEvent(): event=MOVED_TO" + ", path=" + path);
		}
		File currentFile = new File(m_parentPath + "/" + path);
		if (currentFile.exists() == false) {
		    // FileManagerApp.log(TAG +
		    // " onEvent(): cant get file from path="+m_parentPath+"/"+path);
		    return;
		}
		IconifiedText item = IconifiedText.buildIconItem(
			m_database.getContext(), currentFile);
		m_database.addRecord(item.getText().trim(), item.getPathInfo()
			.trim());
		if (currentFile.isDirectory()) {
		    m_database.addObserver(currentFile.getAbsolutePath());
		    if (moved_toEvent != 0) {
			m_database.scanDirectory(currentFile);
		    }
		}
	    }
	    if ((deleteEvent != 0) || (moved_fromEvent != 0)) {
		// file deleted
		if (deleteEvent != 0) {
		    FileManagerApp.log(TAG + " on " + m_parentPath
			    + " onEvent(): event=DELETE" + ", path=" + path);
		} else {
		    FileManagerApp
			    .log(TAG + " on " + m_parentPath
				    + " onEvent(): event=MOVED_FROM"
				    + ", path=" + path);
		}
		m_database.deleteRecord(m_parentPath + "/" + path);
		m_database.removeObserver(m_parentPath + "/" + path);
	    }
	    if (delete_selfEvent != 0) {
		FileManagerApp.log(TAG + " on " + m_parentPath
			+ " onEvent(): event=DELETE_SELF" + ", path=" + path);
	    }
	    if (move_selfEvent != 0) {
		FileManagerApp.log(TAG + " on " + m_parentPath
			+ " onEvent(): event=MOVE_SELF" + ", path=" + path);
	    }
	}
    }
}