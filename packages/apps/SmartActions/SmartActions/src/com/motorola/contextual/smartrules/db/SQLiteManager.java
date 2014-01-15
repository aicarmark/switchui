/*
 * @(#)SQLiteManager.java
 * formerly OurDbAdapter.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 * ACD100        2011/05/09 KlocWorks cleanup Refactored OurDbAdapter to SQLiteManager
 */
package com.motorola.contextual.smartrules.db;


import java.io.File;
import java.util.Arrays;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.Build;
import android.util.Log;

import com.motorola.contextual.actions.BackgroundData;
import com.motorola.contextual.actions.Sync;
import com.motorola.contextual.rule.publisher.XmlUtils;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.view.ActiveSettingsView;
import com.motorola.contextual.smartrules.db.table.view.DistinctConditionView;
import com.motorola.contextual.smartrules.db.table.view.RuleActionView;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
import com.motorola.contextual.smartrules.db.table.view.RuleView;
import com.motorola.contextual.smartrules.db.table.view.RuleViewCnt;
import com.motorola.contextual.smartrules.db.table.view.TriggerStateCountView;
import com.motorola.contextual.smartrules.rulesimporter.RulesImporterService;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants;
import com.motorola.contextual.smartrules.util.Util;

/** This class handles opening/closing of the Db and helper, creation of the database tables,
 * and upgrades of the database (adding columns and such).
 *
 *  Most of the methods here are required (shadow the SQLiteDatabase method
 *  because I could not figure out how to successfully extend the Android SQLiteDatabase class 
 *  without problems (i.e. default constructor required, not visible, no other constructors visible, etc.).
 *  
 *  The main purpose here is to keep together the db and helper instance, and ensure both get closed.
 *  If the helper is closed before the db, it appears to close the db.
 *  
 *<code><pre>
 * CLASS:
 *  Implements DbSyntax to perform SQL operations.
 *
 * RESPONSIBILITIES:
 * 	Opening the database and the adapter
 * 	Closing the database and the adapter
 * 	Instantiating the adapter
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 * 	See FriendTable.java
 *
 *</pre></code>
 */
public class SQLiteManager implements DbSyntax, Constants, DbConstants {

    private static final String TAG = SQLiteManager.class.getSimpleName();
    // This interface is copying the Build.VERSION_CODES values from standard
    // Android documentation so that for pre-gingerbread buildsthe software 
    // compiles fine.
    @SuppressWarnings("unused")
	private interface BuildVersionCodes {
    	final int GINGERBREAD_MR1 = 10;
    }
    
    public static final int 	DATABASE_VERSION = 35;
    public static final int     INVALID_DATABASE_VERSION = -1;
    private static final String DB_NOT_INIT = "Db not initialzed";
    
    // database
    protected static final String DATABASE_NAME = Constants.DATABASE_NAME;

    /** database instance */
	private SQLiteDatabase mDb = null;
    /** related helper instance */
	@SuppressWarnings("unused")
	private OurDatabaseHelper mHelper = null;    	
       
    
	/** hide empty constructor */
	public SQLiteManager() {
		super();
	}

	/** convenience method, @see openForWrite(Context, CursorFactory, openFrom)
	 */
	public static SQLiteManager openForWrite(Context context,  String openFrom) {
		return openForWrite(context, false, openFrom);
	}

	private static SQLiteManager sqliteManager = null;
	
	/** opens the database for Write, and helper
	 * 
     * NOTE - YOU MUST CLOSE THIS WHEN FINISHED
     * 
	 * @param context - context
	 * @param createFactory - if true, will create the cursor factory
	 * @param openFrom - debug string
	 * @return SQLiteManager instance - contains both SQLiteDatabase instance and helper instance
	 */
	public static SQLiteManager openForWrite(Context context, boolean createFactory, String openFrom) {
		
		/* if(Build.VERSION.SDK_INT > BuildVersionCodes.GINGERBREAD_MR1) { // For Android Version 11 (Honeycomb i.e. 3.0) and above 
			SQLiteManager result = new SQLiteManager();

	        SQLiteManager.CursorFactoryManager factory = (createFactory? null: result.new CursorFactoryManager());
			result.mDb = (result.mHelper = new OurDatabaseHelper(context, factory, openFrom)).getWritableDatabase();
			
			if (result.mDb != null) {
				if (LOG_DEBUG) Log.d(TAG, "Normal: openForWrite From="+openFrom+" dbv="+DATABASE_VERSION+" ver="+ result.mDb.getVersion()+
	                " open="+result.mDb.isOpen());
			} else {
				Log.e(TAG, "Open failed for write, null db!");
			}
			return result;		
			
		} else { // For Android Versions below 11 i.e. pre-honeycomb (pre 3.0) */
			return openHack(context, createFactory, openFrom);
		//}
	}

	/** hacked instance of open database for Android Versions < 11 i.e. Gingerbread or 3.0 - this
	 *  is done so that the Database Locked issues are resolved and we have only one instance of
	 *  DB that is always open and not have an instance open for each database read/write call.
	 *  This should not be seen in versions from Honeycomb and the normal code in the if() of
	 *  openForWrite() above and openForRead() below should be executed.
	 * 
	 * @param context - context
	 * @param createFactory - if true, will create the cursor factory
	 * @param openFrom - debug string
	 * @return SQLiteManager instance - contains both SQLiteDatabase instance and helper instance
	 */
	private static synchronized SQLiteManager openHack(Context context, boolean createFactory, String openFrom) {
		
		if(LOG_DEBUG) Log.d(TAG, "Android Version = "+Build.VERSION.SDK_INT);
		if(sqliteManager == null) {
			if(LOG_DEBUG) Log.d(TAG, "openHack: sqliteManager is null - get the new instance of SQLiteManager");
			sqliteManager = new SQLiteManager();
		}
		
		if(sqliteManager.mDb == null) {
			if(LOG_DEBUG) Log.d(TAG, "openHack: sqliteManager.mDb is null - fetching the instance");
	        SQLiteManager.CursorFactoryManager factory = (createFactory? null: sqliteManager.new CursorFactoryManager());
	        sqliteManager.mDb = (sqliteManager.mHelper = new OurDatabaseHelper(context, factory, openFrom)).getWritableDatabase();
		}
		
		if (sqliteManager.mDb != null) {
			if(!sqliteManager.mDb.isOpen()) {
				Log.e(TAG, "openHack - sqliteManager.mDb instance exists but DB is closed");
				try {
					openForWrite(context, createFactory, openFrom);
				} catch (IllegalStateException ise) {
					Log.e(TAG, "Exception trying to refetch and open the DB instance");
					ise.printStackTrace();
					sqliteManager = new SQLiteManager();
					SQLiteManager.CursorFactoryManager factory = (createFactory? null: sqliteManager.new CursorFactoryManager());
				    sqliteManager.mDb = (sqliteManager.mHelper = new OurDatabaseHelper(context, factory, openFrom)).getWritableDatabase();
				}
			}
			if (LOG_DEBUG) Log.d(TAG, "openHack: openForWrite From="+openFrom+" dbv="+DATABASE_VERSION);
		} else {
			Log.e(TAG, "Open failed for write, null db!");
		}
		
		return sqliteManager;
	}
	
	/** convenience method, @see openForRead(Context, CursorFactory, openFrom)
	 */
	public static SQLiteManager openForRead(Context context, String openFrom) {
		return openForRead(context, false, openFrom);
	}
	
	/** opens the database for Read
	 * 
	 * @param context - context
	 * @param openFrom - debug string
	 * @param createFactory - if true, will create the factory instance to close the db.
	 * @return SQLiteManager instance - contains both SQLiteDatabase instance and helper instance
	 */
	public static SQLiteManager openForRead(Context context, boolean createFactory, String openFrom) {
		
		/* if(Build.VERSION.SDK_INT > BuildVersionCodes.GINGERBREAD_MR1) { // For Android Version 11 (Honeycomb i.e. 3.0) and above 

			SQLiteManager result = new SQLiteManager();
	        SQLiteManager.CursorFactoryManager factory = (createFactory? result.new CursorFactoryManager() : null);
			result.mDb = (result.mHelper = new OurDatabaseHelper(context, factory, openFrom)).getReadableDatabase();
			if (result.mDb != null) {
				if (LOG_DEBUG) Log.d(TAG, "Normal: openForRead From="+openFrom+" dbv="+DATABASE_VERSION+" ver="+ result.mDb.getVersion()+
	                " open="+result.mDb.isOpen());
			} else {
				Log.e(TAG, "Open failed for read, null db!");
			}
			return result;
		} else { // For Android Versions below 11 i.e. pre-honeycomb (pre 3.0) */
			return openHack(context, createFactory, openFrom);
		//}			
	}					
		
		
	/**
	 * @return the mSqlDb
	 */
	public SQLiteDatabase getDb() {
		return mDb;
	}
	
	/** shadow the SQLiteDatabase method */
    public boolean isOpen() {
    	if (mDb == null) {
    		return false;
    	} else {
    		return mDb.isOpen();
    	}
    }
    
    
	/** shadow the SQLiteDatabase method
	 * close both the db and the helper 
	 * @param closeFrom - debug
	 */
	public void close(String closeFrom) {
		/* if(Build.VERSION.SDK_INT > BuildVersionCodes.GINGERBREAD_MR1) { // For Android Version 11 (Honeycomb i.e. 3.0) and above 
			if (mDb != null && mDb.isOpen())
				mDb.close();
			if (mHelper != null)
				mHelper.close();
			if (LOG_DEBUG) Log.i(TAG, "Normal: close from:"+closeFrom);
		} else { // For Android Versions below 11 i.e. pre-honeycomb (pre 3.0) */
			closeHack(closeFrom);
		//}	
	}


	/** hacked instance of close - DO NOT CLOSE THE DB HERE. This is for 
	 *  Android Versions before 11 i.e. Gingerbread or 3.0 where in we will
	 *  always keep the instance of database open and not open a new instance
	 *  of the DB for every read/write.
	 * 
	 * @param closeFrom - debug
	 */
	public static synchronized void closeHack(String closeFrom) {
		// NOTE: DO NOT CLOSE THE DATABASE
		if (LOG_DEBUG) Log.d(TAG, "closeHack (Not Closed): close call from:"+closeFrom+" Android Version "+Build.VERSION.SDK_INT);
	}
	
	// * all these shadow methods are used to make coding conversion easier to support this class
	
	/** shadow the SQLiteDatabase method */
    public void beginTransaction() {
        mDb.beginTransaction();
    }
    
	/** shadow the SQLiteDatabase method */
    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {    	
    	if (mDb == null) throw new IllegalStateException ("DB_NOT_INIT");
        mDb.beginTransactionWithListener(transactionListener);    	
    }

    
	/** shadow the SQLiteDatabase method */
    public void setTransactionSuccessful() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	mDb.setTransactionSuccessful();
    }

	/** shadow the SQLiteDatabase method */
    public void endTransaction() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        mDb.endTransaction();
    }

	/** shadows the SQLiteDatabase method */
    public boolean isDbLockedByCurrentThread() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.isDbLockedByCurrentThread();
    }
    
	/** shadows the SQLiteDatabase method */
    public boolean isDbLockedByOtherThreads() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.isDbLockedByOtherThreads();
    }
    
    
	/** shadows the SQLiteDatabase method */
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.yieldIfContendedSafely(sleepAfterYieldDelay);
    }

	/** shadows the SQLiteDatabase method */
    public Map<String, String> getSyncedTables() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.getSyncedTables();
    }

	/** shadows the SQLiteDatabase method */
    public int getVersion() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.getVersion();
    }

    
	/** shadows the SQLiteDatabase method */
    public void setVersion(int version) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	mDb.setVersion(version);
    }
    
    
	/** shadows the SQLiteDatabase method */
    public long getMaximumSize() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.getMaximumSize();
    }
    
	/** shadows the SQLiteDatabase method */
    public long setMaximumSize(long numBytes) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.setMaximumSize(numBytes);
    }    
        
	/** shadows the SQLiteDatabase method */
    public long getPageSize() {
    	return mDb.getPageSize();
    }
    
	/** shadows the SQLiteDatabase method */
    public void markTableSyncable(String table, String deletedTable) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	mDb.markTableSyncable(table, deletedTable);
    }
    
	/** shadows the SQLiteDatabase method */
    public void markTableSyncable(String table, String foreignKey, String updateTable) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        mDb.markTableSyncable(table, foreignKey, updateTable);
    }

    public SQLiteStatement compileStatement(String sql) throws SQLException {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.compileStatement(sql);
    }
        
	/** shadow the SQLiteDatabase execSQL */
	public void execSQL(String sql) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
		mDb.execSQL(sql);
	}    

	/** shadow the SQLiteDatabase method */
    public Cursor query(boolean distinct, String table, String[] columns,
            String selection, String[] selectionArgs, String groupBy,
            String having, String orderBy, String limit) {
    	
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.query(distinct, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit);
    }
    
	/** shadow the SQLiteDatabase method */
    public Cursor queryWithFactory(CursorFactory cursorFactory,
            boolean distinct, String table, String[] columns,
            String selection, String[] selectionArgs, String groupBy,
            String having, String orderBy, String limit) {
    	
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.queryWithFactory(cursorFactory,
                distinct, table, columns, selection, selectionArgs, groupBy,
                having, orderBy, limit);
    }
    
	/** shadow the SQLiteDatabase method */
    public Cursor query(String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having,
            String orderBy) {
    	
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.query(table, columns, selection,
                selectionArgs, groupBy, having, orderBy);
     }
    
	/** shadow the SQLiteDatabase method */
    public Cursor query(String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having,
            String orderBy, String limit) {
    	
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.query(table, columns, selection,
                selectionArgs, groupBy, having,
                orderBy, limit);
    }
    
	/** shadow the SQLiteDatabase method */
    public Cursor rawQuery(String sql, String[] selectionArgs) {
    	
    	Cursor cursor = null;
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	try {
    		cursor = mDb.rawQuery(sql, selectionArgs);
    	} catch (SQLiteException sqe) {
    		Log.e(TAG, "sql = "+sql+" and selectionArgs = "+Arrays.toString(selectionArgs));
    		sqe.printStackTrace();
    	}
        return cursor;
    }
    
	/** shadow the SQLiteDatabase method */
    public Cursor rawQueryWithFactory(
            CursorFactory cursorFactory, String sql, String[] selectionArgs,
            String editTable) {
    	
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.rawQueryWithFactory(cursorFactory, sql, selectionArgs,
                editTable);
    }
    
            
	/** shadows the SQLiteDatabase method */
    public long insert(String table, ContentValues values) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.insert(table, null, values);
    }
    
	/** shadows the SQLiteDatabase method */
    public long insertOrThrow(String table, ContentValues values) throws SQLException {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.insertOrThrow(table, null, values);
    }

	/** shadows the SQLiteDatabase method */
    public long insertWithOnConflict(String table, 
            ContentValues initialValues, int conflictAlgorithm) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.insertWithOnConflict(table, null,
                initialValues, conflictAlgorithm); 
    }
    
	/** shadows the SQLiteDatabase method */
    public long replace(String table, ContentValues initialValues) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.replace(table, null, initialValues);
    }
        
	/** shadows the SQLiteDatabase method */
    public long replaceOrThrow(String table, String nullColumnHack,
            ContentValues initialValues) throws SQLException {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
        return mDb.replaceOrThrow(table, nullColumnHack, initialValues);
    }
    
	/** shadows the SQLiteDatabase method */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.update(table, values, whereClause, whereArgs);
    }
    
	/** shadows the SQLiteDatabase method */
    public int updateWithOnConflict(String table, ContentValues values,
            String whereClause, String[] whereArgs, int conflictAlgorithm) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.updateWithOnConflict(table, values,
                whereClause, whereArgs, conflictAlgorithm);
    }
    
	/** shadows the SQLiteDatabase method */
    public int delete(String table, String whereClause, String[] whereArgs) {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.delete(table, whereClause, whereArgs);
    }
    	
	/** shadow the SQLiteDatabase execSQL */
    public boolean inTransaction() {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	return mDb.inTransaction();
    }
    
	/** shadow the SQLiteDatabase execSQL */
    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
    	if (mDb == null) throw new IllegalStateException (DB_NOT_INIT);
    	mDb.execSQL(sql, bindArgs);
    }

    	
    /** This class extends the SQLiteOpenHelper.class.
     *
     *<pre>
     * CLASS:
     * 	Every class that abstracts a table or view uses this class to open the database.
     *
     * RESPONSIBILITIES:
     * 	Opening the database
     * 	Upgrading the database
     * 	Dropping and recreating the database
     *
     *
     * COLABORATORS:
     * 	This class extends the SQLiteOpenHelper.class. See that class for more info.
     *
     * USAGE:
     * 	Description of typical usage of class. Include code samples.
     *
     */
    public static class OurDatabaseHelper extends SQLiteOpenHelper {

        private static final String TAG = OurDatabaseHelper.class.getSimpleName();
        private Context mContext;
        
        /** Constructor
        *
        * android.database.sqlite.SQLiteOpenHelper(android.content.Context)
        *
        * This uses the DATABASE_NAME constant from the Constants.java field and
        * the DATABASE_VERSION constant from this file. The database lives in
        * the /data/data/com.motorola.contextual.location/databases folder of the device or emulator.
        *
        * @param context - context to be used to perform database operations.
        */
       public OurDatabaseHelper(Context context, CursorFactory factory, final String from) {

           super(context, DATABASE_NAME, factory, DATABASE_VERSION);
           mContext = context;
           if (LOG_DEBUG) Log.d(TAG, "OurDatabaseHelper from="+from);
       }


        /* (non-Javadoc)
         * @see java.lang.Object#finalize()
         */
        @Override
        protected void finalize() throws Throwable {
            if (LOG_DEBUG) Log.d(TAG, "OurDatabaseHelper.finalize");
            close();
			super.finalize();
			SQLiteManager.closeHack(TAG);
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#close()
         */
        @Override
        public synchronized void close() {
            super.close();
            if (LOG_DEBUG) Log.d(TAG, "OurDatabaseHelper.close");
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#getReadableDatabase()
         */
        @Override
        public synchronized SQLiteDatabase getReadableDatabase() {
            return super.getReadableDatabase();
        }


        /* (non-Javadoc)
         * @see android.database.sqlite.SQLiteOpenHelper#getWritableDatabase()
         */
        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            return super.getWritableDatabase();
        }


        /** handles the onCreate operation - which is only called when the
         * database does not exist.
         *
         * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
         *
         * @param db - SQLiteDatabase instance.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {

            // tables
            db.execSQL(RuleTable				.CREATE_TABLE_SQL);
            db.execSQL(IconTable                .CREATE_TABLE_SQL);
            db.execSQL(ConditionTable			.CREATE_TABLE_SQL);
            db.execSQL(ActionTable				.CREATE_TABLE_SQL);

            // indexes
            db.execSQL(RuleTable				.CREATE_RULE_KEY_INDEX);

            db.execSQL(ActionTable				.CREATE_PARENT_FKEY_INDEX);
            db.execSQL(ActionTable				.CREATE_PUBLISHER_KEY_COLUMN_INDEX);

            db.execSQL(ConditionTable			.CREATE_PARENT_FKEY_INDEX);
            db.execSQL(ConditionTable			.CREATE_SENSOR_NAME_COLUMN_INDEX);
            db.execSQL(ConditionTable			.CREATE_PUBLISHER_KEY_COLUMN_INDEX);

            // views
            db.execSQL(RuleView					.CREATE_VIEW_OUTER_JOIN);
            db.execSQL(RuleActionView			.CREATE_VIEW_OUTER_JOIN);
            db.execSQL(RuleConditionView		.CREATE_VIEW_OUTER_JOIN);
            db.execSQL(RuleViewCnt				.CREATE_VIEW);
            db.execSQL(TriggerStateCountView	.CREATE_VIEW);
            db.execSQL(ActiveSettingsView		.CREATE_VIEW);
            db.execSQL(DistinctConditionView		.CREATE_VIEW);

            new RuleTable().initialize(db, mContext);

        }

        private static final String SQLITE_AUTOINDEX = "sqlite_autoindex_Rule_1";
        
        /** drops all the database elements tables, views and indexes for this DB.
         * 
         * @param db - SQLiteDatabase instance
         */
        private static void dropDatabaseElements(SQLiteDatabase db) {
        	
    		Cursor cursor = DbUtil.getSqliteMasterTableCursor(db);
    		if(cursor == null)
    			Log.e(TAG, "Query to read from sqlite_master table failed");
    		else {
    			try {
    				if(cursor.moveToFirst()) {
	        			for(int i = 0; i < cursor.getCount(); i++) {
	        				String type = cursor.getString(cursor.getColumnIndex(ENTRY_TYPE));
	        				String name = cursor.getString(cursor.getColumnIndex(ENTRY_NAME));
	        				String sql = null;
	        				if(name.equals(ANDROID_METADATA_TABLENAME) 
	        						|| name.equals(SQLITE_SEQUENCE_TABLENAME)
	        						|| name.equals(SQLITE_AUTOINDEX)) {
	        					Log.e(TAG, "Cannot drop "+name);
	        				}
	        				else {
	        					if(type.equals(SQL_TABLE_TYPE))
	        						sql = DROP_IF_TABLE_EXISTS + name;
	        					else if (type.equals(SQL_INDEX_TYPE))
	        						sql = DROP_IF_INDEX_EXISTS + name;
	        					else if (type.equals(SQL_VIEW_TYPE))
	        						sql = DROP_IF_VIEW_EXISTS + name;
	        					
	        					if(sql != null) {
	        						if(LOG_INFO) Log.i(TAG, "Executing sql "+sql);
	        						db.execSQL(sql);
	        					}
	        				}
	        				cursor.moveToNext();
	        			}
	    			}
    			} catch (Exception e) {
    				e.printStackTrace();
    			} finally {
    				if(! cursor.isClosed())
    					cursor.close();
    			}
    		}    		
        }
        
        /** This method handles the database downgrade from SQL perspective. The handler
         * 	will drop all the tables, views and indexes in the DB and recreate it. All the
         * 	user data will be lost due to the downgrade of the DB.
         * 
         */
        /** This method handles the database downgrade from SQL perspective. The handler
         * 	will drop all the tables, views and indexes in the DB and recreate it. All the
         * 	user data will be lost due to the downgrade of the DB.
         *
         * @see android.database.sqlite.SQLiteOpenHelper#onDowngrade(android.database.sqlite.SQLiteDatabase, int, int)
         *
         * @param db - SQLiteDatabase instance being downgraded
         * @param oldVersion - version number being downgraded from
         * @param newVersion - version number being downgraded to
         *
         */
        @Override
        public void onDowngrade(final SQLiteDatabase db, int oldVersion, final int newVersion) {
            if (LOG_INFO) Log.i(TAG, "Downgrading database from oldVersion = "+oldVersion+
            							" newVersion = "+newVersion);
            if(newVersion < oldVersion) {
            	try {
	        		dropDatabaseElements(db);
	        		onCreate(db);      	
	        		// Invoke Rule Importer so that the samples can be re-imported.
	        		Intent serviceIntent = new Intent(mContext, RulesImporterService.class);
				serviceIntent.putExtra(IMPORT_TYPE, XmlConstants.ImportType.FACTORY);
	        		mContext.startService(serviceIntent);
            	} catch (Exception e) {
            		Log.e(TAG, "Exception while downgrading the Smart Actions DB");
            		e.printStackTrace();
            	}
        	}        
        }
        
        /** This method handles database upgrades from a SQL perspective. That is,
         * columns can be added to the database, columns can be initialized, etc.
         *
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         *
         * @param db - SQLiteDatabase instance being upgraded
         * @param oldVersion - version number being upgraded from
         * @param newVersion - version number being upgraded to
         *
         */
		@Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            int currentVersion = oldVersion;

            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                  + newVersion + ", which may destroy old data");

            boolean v26=false, v27=false, v28=false, v29=false, v30=false, v31=false;
            boolean v32=false, v33=false, v34=false;
            
            // NOTE: SUPPORT FOR PRIOR TO V26 REMOVED ON 21st MAY 2012.
            // DB upgrade from v26 to v27 on 15th Sep 2011 - targeted to Stable-6.
            if(currentVersion == 26 && newVersion > oldVersion) {
                db.beginTransaction();
                try {
                	String sql1 = TriggerStateCountView.CREATE_VIEW;
                	String sql2 = ConditionTable.CREATE_PUBLISHER_KEY_COLUMN_INDEX;
                	
                	db.execSQL(sql1);
                	db.execSQL(sql2);

                	db.setTransactionSuccessful();  
                	
                	currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "+(currentVersion-1)+" to "+currentVersion);
                    v26=true;                	
                }catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }    
            }
 
            // NOTE: This is the DB version from which all Stable-7 changes are added.
            // Any changes after this version that are related to Stable-6 would require
            // all the DB changes from this version on to be ported to Stable-6.
            if(currentVersion == 27 && newVersion > oldVersion) {
                db.beginTransaction();
                try {
                	
                	// add PRESET_FKEY to the rule Table
                	String sql1 = ALTER_TABLE + RuleTable.TABLE_NAME+
                				  ADD_COLUMN+RuleTable.Columns.SAMPLE_FKEY_OR_COUNT+KEY_TYPE+DEFAULT+RuleTable.DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE;
                	String sql2 = UPDATE + RuleTable.TABLE_NAME+
                					SET+RuleTable.Columns.SAMPLE_FKEY_OR_COUNT+EQUAL+RuleTable.DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE;
                	
                    db.execSQL(sql1);
                    db.execSQL(sql2);

                    // Few invisible inference rules have suggState set as UNREAD in stable6 and hence
                    // they pop up as suggestions after upgrade. We need to forcefully set them to ACCEPTED (0)
                    ContentValues cv = new ContentValues();
                    cv.put(RuleTable.Columns.SUGGESTED_STATE, RuleTable.SuggState.ACCEPTED);
                    String whereClause = RuleTable.Columns.SOURCE + EQUALS + Q + RuleTable.Source.INFERRED + Q;
                    int rows = db.update(RuleTable.TABLE_NAME, cv, whereClause, null);

                    db.setTransactionSuccessful();

                    // printing log after setting the transaction successful
                    if (LOG_INFO) Log.i(TAG,"No# of Inferred Rules Updated: " + rows);

                    // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "+(currentVersion-1)+" to "+currentVersion);
                    v27=true;

                    // VirtualSensorManager DB is copied in VSM while processing onBootComplete
                    
                    // Location Sensor DB
                    String absoluteInputFilePath = "/data/data/com.motorola.locationsensor/databases/locationsensor.db";
                    String absoluteOutputFilePath = "/data/data/com.motorola.contextual.smartrules/databases/locationsensor.db";
                    Util.copyFile(absoluteInputFilePath, absoluteOutputFilePath);

                    // Inference DB
                    absoluteInputFilePath = "/data/data/com.motorola.contextual.inferencemanager/databases/inference.db";
                    absoluteOutputFilePath = "/data/data/com.motorola.contextual.smartrules/databases/inference.db";
                    Util.copyFile(absoluteInputFilePath, absoluteOutputFilePath);
                    
                    // Debug/Analytics DB
                    //absoluteInputFilePath = "/data/data/com.motorola.contextual.debug/databases/debug.db";
                    //absoluteOutputFilePath = "/data/data/com.motorola.contextual.smartrules/databases/debug.db";
                    //Util.copyFile(absoluteInputFilePath, absoluteOutputFilePath);
                    
                    // Smart Profile DB (Time Frames)
                    absoluteInputFilePath = "/data/data/com.motorola.contextual.smartprofile/databases/time_frames.db";
                    absoluteOutputFilePath = "/data/data/com.motorola.contextual.smartrules/databases/time_frames.db";
                    Util.copyFile(absoluteInputFilePath, absoluteOutputFilePath);

                    // copy Blur updater shared prefs to check dupe import of rules
                    // create output dir first
                    String dirName = "/data/data/com.motorola.contextual.fw/shared_prefs";
                    new File(dirName).mkdirs();

                    absoluteInputFilePath = "/data/data/com.motorola.blur.contextual.rulesupdater.service/shared_prefs" +
                                                       "/com.motorola.contextual.rulesupdaterprefernce.xml";
                    absoluteOutputFilePath = dirName + "/com.motorola.contextual.rulesupdaterprefernce.xml";
                    Util.copyFile(absoluteInputFilePath, absoluteOutputFilePath);
                }catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }    
            }
            if(currentVersion == 28 && newVersion > oldVersion) {
                db.beginTransaction();
                try {
                	
                	String sql1 = ActiveSettingsView.CREATE_VIEW;
                    db.execSQL(sql1);
                    db.setTransactionSuccessful();

                    // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "+(currentVersion-1)+" to "+currentVersion);
                    v28=true;             	
                }catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }    
            }
            if(currentVersion == 29 && newVersion > oldVersion) {
            	db.beginTransaction();
                try {
                	
                    String sql1 = ALTER_TABLE + RuleTable.TABLE_NAME+
                            ADD_COLUMN+RuleTable.Columns.LAST_EDITED_DATE_TIME+INTEGER_TYPE+DEFAULT+0;
                    String sql2 = UPDATE + RuleTable.TABLE_NAME+
                          	SET+RuleTable.Columns.LAST_EDITED_DATE_TIME+EQUAL+0;

                    db.execSQL(sql1);
                    db.execSQL(sql2);
                    db.setTransactionSuccessful();

                    // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "+(currentVersion-1)+" to "+currentVersion);
                    v29=true;             	
                }catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }    
            }            

            if(currentVersion == 30 && newVersion > oldVersion) {
            	db.beginTransaction();
                try {
                	String sql1 = 
                		DROP_IF_VIEW_EXISTS + BLANK_SPC + TriggerStateCountView.VIEW_NAME;
                	String sql2 = TriggerStateCountView.CREATE_VIEW;

                    db.execSQL(sql1);
                    db.execSQL(sql2);
                    db.setTransactionSuccessful();

                    // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "
                    					+(currentVersion-1)+" to "+currentVersion);
                    v30=true;             	
                }catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }    
            } 
            
            if (currentVersion == 31 && newVersion > oldVersion) {
                db.beginTransaction();
                try {
                    // add Config, Validity and Market Url columns to ActionTable
                    String sql1 = ALTER_TABLE + ActionTable.TABLE_NAME+
                                  ADD_COLUMN+ActionTable.Columns.CONFIG+TEXT_TYPE;
                    String sql2 = ALTER_TABLE + ActionTable.TABLE_NAME+
                                  ADD_COLUMN+ActionTable.Columns.ACTION_VALIDITY+TEXT_TYPE
					+DEFAULT+TableBase.Validity.VALID;
                    String sql3 = ALTER_TABLE + ActionTable.TABLE_NAME+
                                  ADD_COLUMN+ActionTable.Columns.MARKET_URL+TEXT_TYPE;
                    String sql4 = UPDATE + ActionTable.TABLE_NAME+
                                  SET+ActionTable.Columns.CONFIG+EQUAL
                                  	+ActionTable.Columns.URI_TO_FIRE_ACTION;

                    // add Publisher key, Validity and Ui Intent columns to RuleTable
                    String sql5 = ALTER_TABLE + RuleTable.TABLE_NAME+
                                  ADD_COLUMN+RuleTable.Columns.PUBLISHER_KEY+TEXT_TYPE;
                    String sql6 = ALTER_TABLE + RuleTable.TABLE_NAME+
                                  ADD_COLUMN+RuleTable.Columns.VALIDITY+TEXT_TYPE
					+DEFAULT+TableBase.Validity.VALID;
                    String sql7 = ALTER_TABLE + RuleTable.TABLE_NAME+
                                  ADD_COLUMN+RuleTable.Columns.UI_INTENT+TEXT_TYPE;


                    db.execSQL(sql1);
                    db.execSQL(sql2);
                    db.execSQL(sql3);
                    db.execSQL(sql4);
                    db.execSQL(sql5);
                    db.execSQL(sql6);
                    db.execSQL(sql7);

                    db.setTransactionSuccessful();

                    // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "+(currentVersion-1)+" to "+currentVersion);
                    v31=true;

                } catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }
            }
            if (currentVersion == 32 && newVersion > oldVersion) {
                db.beginTransaction();
                try {
                    db.execSQL(IconTable.CREATE_TABLE_SQL);
                    db.setTransactionSuccessful();
                    // only increment oldVersion once!
                    currentVersion++;
                    v32=true;
                    if (LOG_INFO) Log.i(TAG,"update successful from "
        					+(currentVersion-1)+" to "+currentVersion);
                } catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
            }
            if(currentVersion == 33 && newVersion > oldVersion) {
            	db.beginTransaction();
                try {
                    String sql1 = ALTER_TABLE + RuleTable.TABLE_NAME+
                    		ADD_COLUMN+RuleTable.Columns.PARENT_RULE_KEY
                    		+TEXT_TYPE;
                    
                    // This column will replace the SampleFkOrCount column.
                    String sql2 = ALTER_TABLE + RuleTable.TABLE_NAME +
                    		ADD_COLUMN+RuleTable.Columns.ADOPT_COUNT
                    		+INTEGER_TYPE+DEFAULT+ZERO;
                    
                    String sql3 = ALTER_TABLE + ActionTable.TABLE_NAME+
                    		ADD_COLUMN+ActionTable.Columns.CHILD_RULE_KEY
                    		+TEXT_TYPE;            

                    db.execSQL(sql1);
                    db.execSQL(sql2);
                    db.execSQL(sql3);
                    
                    // Following are the steps that needs to be done to perform the file fix
                    // 1. Copy the SampleFkOrCount column value for source list visible
                    // 	  rules (Samples and Suggestions) to the new AdoptCount column. 
                    //	  These rules are only visible on the Add Rule List page and this
                    //	  is explicit to the AdoptCount column and we need not populate
                    //    the ParentRuleKey column for these rules.
                    // 2. Fill the ParentRuleKey column for the adopted sample and 
                    //     suggestions that are visible to the user on the Landing Page.
                    //	   The AdoptCount column for these rules should always be 0.
                    // 3. Completely drop the SampleFkOrCount column.
                    // 4. Change the Action publisher key com.motorola.contextual.actions.Sync to 
                    //    com.motorola.contextual.actions.BackgroundData
                    
                    // The objective of this whereClause is to fetch all (Parent) the sample 
                    // or suggested rules that have been adopted. These parent rules only 
                    // appear in the Add Rule List.
                    String SOURCE_LIST_WHERE_CLAUSE = 
                    		RuleTable.Columns.SAMPLE_FKEY_OR_COUNT 
                    			+ GT_OR_EQUAL + MIN_VALID_KEY
                    		+ AND 
                    		+ RuleTable.Columns.FLAGS + LIKE 
                    			+ Q + WILD + RuleTable.Flags.SOURCE_LIST_VISIBLE + WILD + Q;
                    
                    // Now that the AdoptCount is an added column to the Rule table, for the
                    // adopted samples need to copy the value in the SampleFkOrCount column
                    // to the AdoptCount column.
                    // update Rule set AdoptCount = SampleFkOrCount 
                    // where SampleFkOrCount >= 1 and Flags like '%s%';
                    String sql4 = UPDATE + RuleTable.TABLE_NAME 
                    				+ SET + RuleTable.Columns.ADOPT_COUNT
                    					+ EQUAL + RuleTable.Columns.SAMPLE_FKEY_OR_COUNT 
                    				+ WHERE + SOURCE_LIST_WHERE_CLAUSE;
                   
                    Log.i(TAG, "sql4 = "+sql4);
                    db.execSQL(sql4);
                    
                    // Next step is to update the ParentRuleKey for the adopted samples or 
                    // suggested rules that are visible to the user on the Landing Page.
                    //  update Rule set ParentRuleKey=
                    //	     (select Rule1.Key
                    //	     from Rule as Rule1
                    //	     join Rule as Rule2 
                    //	     where Rule._id = Rule2._id 
                    //				and Rule2.SampleFkOrCount = Rule1._id 
                    //				and Rule2.SampleFkOrCount >= 1 
                    //				and (Rule2.Flags not like '%s%' 
                    //						and Rule2.Flags not like '%i%') 
                    //				and Rule2.Source in (4,3))
                    //	where SampleFkOrCount >= 1 
                    // 		and (Flags not like '%s%' and Flags not like '%i%') 
                    //		and Source in (4,3);
                    
                    String RULE1 = RuleTable.TABLE_NAME + "1";
                    String RULE2 = RuleTable.TABLE_NAME + "2";
                    
                    String sql5 = 
                		UPDATE + RuleTable.TABLE_NAME 
                			+ SET + RuleTable.Columns.PARENT_RULE_KEY + EQUAL
                			+ LP
                				+ SELECT + RULE1 + DOT + RuleTable.Columns.KEY
                					+ FROM + RuleTable.TABLE_NAME + AS + RULE1
                					+ JOIN + RuleTable.TABLE_NAME + AS + RULE2
                				+ WHERE + RuleTable.TABLE_NAME + DOT + RuleTable.Columns._ID + EQUALS
                						+ RULE2 + DOT + RuleTable.Columns._ID
                					+ AND + RULE2 + DOT + RuleTable.Columns.SAMPLE_FKEY_OR_COUNT
                						+ EQUALS + RULE1 + DOT + RuleTable.Columns._ID
                					+ AND + RULE2 + DOT + RuleTable.Columns.SAMPLE_FKEY_OR_COUNT
                						+ GT_OR_EQUAL + MIN_VALID_KEY
                					+ AND + RULE2 + DOT + RuleTable.Columns.FLAGS 
                						+ IS_NOT_LIKE + Q + WILD 
                						+ RuleTable.Flags.SOURCE_LIST_VISIBLE + WILD + Q
                					+ AND + RULE2 + DOT + RuleTable.Columns.FLAGS 
                						+ IS_NOT_LIKE + Q + WILD 
                						+ RuleTable.Flags.INVISIBLE + WILD + Q      
                					+ AND + RULE2 + DOT + RuleTable.Columns.SOURCE + IN
                						+ LP 
                							+ RuleTable.Source.FACTORY + COMMA 
                							+ RuleTable.Source.SUGGESTED
                						+ RP                					
                			+ RP
                    	+ WHERE + RuleTable.Columns.SAMPLE_FKEY_OR_COUNT 
                    			+ GT_OR_EQUAL + MIN_VALID_KEY
                    		+ AND + RuleTable.Columns.FLAGS + IS_NOT_LIKE + Q + WILD 
        						+ RuleTable.Flags.SOURCE_LIST_VISIBLE + WILD + Q
        					+ AND + RuleTable.Columns.FLAGS  + IS_NOT_LIKE + Q + WILD 
        						+ RuleTable.Flags.INVISIBLE + WILD + Q      
        					+ AND + RuleTable.Columns.SOURCE + IN
        						+ LP 
        							+ RuleTable.Source.FACTORY + COMMA 
        							+ RuleTable.Source.SUGGESTED
        						+ RP
                    		;
                    			                    		
                    Log.i(TAG, "sql5 = "+sql5);
                    db.execSQL(sql5);
                                        
                    String sql6 = UPDATE + RuleTable.TABLE_NAME 
                    	+ SET + RuleTable.Columns.SAMPLE_FKEY_OR_COUNT + EQUAL + INVALID_KEY;
                    db.execSQL(sql6);
                    
                    // The objective of this whereClause is to fetch all Actions 
                    // with Action publisher key com.motorola.contextual.actions.Sync 
                    // to com.motorola.contextual.actions.BackgroundData
                    String SYNC_AP_WHERE_CLAUSE_ENABLED = 
                            ActionTable.Columns.ACTION_PUBLISHER_KEY 
                                + EQUALS + Q + Sync.SYNC_ACTION_KEY + Q
                                + AND + ActionTable.Columns.TARGET_STATE
                                + IN + LP + Q + BackgroundData.ENABLE + Q
                                + COMMA + Q + Sync.State.ON + Q + RP;
                    
                    String sql7 = UPDATE + ActionTable.TABLE_NAME 
                                    + SET + ActionTable.Columns.ACTION_PUBLISHER_KEY
                                    + EQUAL + Q + BackgroundData.BD_ACTION_KEY + Q 
                                    + COMMA  + ActionTable.Columns.CONFIG
                                    + EQUAL + Q + BackgroundData.CONFIG_ENABLE + Q
                                    + COMMA  + ActionTable.Columns.ACTION_DESCRIPTION
                                    + EQUAL + Q +  mContext.getString(R.string.bd_always) + Q
                                    + WHERE + SYNC_AP_WHERE_CLAUSE_ENABLED;
                   
                    Log.i(TAG, "sql7 = "+sql7);
                    db.execSQL(sql7);
                    
                    String SYNC_AP_WHERE_CLAUSE_DISABLED = 
                            ActionTable.Columns.ACTION_PUBLISHER_KEY 
                                + EQUALS + Q + Sync.SYNC_ACTION_KEY + Q
                                + AND + ActionTable.Columns.TARGET_STATE
                                + IN + LP + Q + BackgroundData.DISABLE + Q
                                + COMMA + Q + Sync.State.OFF + Q + RP;
                    
                    String sql8 = UPDATE + ActionTable.TABLE_NAME 
                                    + SET + ActionTable.Columns.ACTION_PUBLISHER_KEY
                                    + EQUAL + Q + BackgroundData.BD_ACTION_KEY + Q
                                    + COMMA  + ActionTable.Columns.CONFIG
                                    + EQUAL + Q + BackgroundData.CONFIG_DISABLE + Q
                                    + COMMA  + ActionTable.Columns.ACTION_DESCRIPTION
                                    + EQUAL + Q +  mContext.getString(R.string.bd_never) + Q
                                    + WHERE + SYNC_AP_WHERE_CLAUSE_DISABLED;
                   
                    Log.i(TAG, "sql8 = "+sql8);
                    db.execSQL(sql8);
                    
                    String SYNC_AP_WHERE_CLAUSE_OFF =
                            ActionTable.Columns.ACTION_PUBLISHER_KEY
                                + EQUALS + Q + Sync.SYNC_ACTION_KEY + Q
                                + AND + ActionTable.Columns.TARGET_STATE
                                + EQUALS + Q + BackgroundData.OFF + Q;

                    String sql9 = UPDATE + ActionTable.TABLE_NAME
                                    + SET + ActionTable.Columns.ACTION_PUBLISHER_KEY
                                    + EQUAL + Q + BackgroundData.BD_ACTION_KEY + Q
                                    + COMMA  + ActionTable.Columns.CONFIG
                                    + EQUAL + Q + BackgroundData.CONFIG_DISABLE + Q
                                    + COMMA  + ActionTable.Columns.ACTION_DESCRIPTION
                                    + EQUAL + Q +  mContext.getString(R.string.bd_never) + Q
                                    + WHERE + SYNC_AP_WHERE_CLAUSE_OFF;

                    Log.i(TAG, "sql9 = "+sql9);
                    db.execSQL(sql9);


                    db.setTransactionSuccessful();

                    // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "
                    					+(currentVersion-1)+" to "+currentVersion);
                    v33=true;             	
                }catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }    
            }
            
            if (currentVersion == 34 && newVersion > oldVersion) {
                db.beginTransaction();
                try {
                    // add Config, Validity and Market Url columns to Condition Table
                    String sql1 = ALTER_TABLE + ConditionTable.TABLE_NAME+
                                  ADD_COLUMN+ConditionTable.Columns.CONDITION_CONFIG+TEXT_TYPE;
                    String sql2 = ALTER_TABLE + ConditionTable.TABLE_NAME+
                                  ADD_COLUMN+ConditionTable.Columns.CONDITION_VALIDITY+TEXT_TYPE+DEFAULT+TableBase.Validity.VALID;
                    String sql3 = ALTER_TABLE + ConditionTable.TABLE_NAME+
                                  ADD_COLUMN+ConditionTable.Columns.CONDITION_MARKET_URL+TEXT_TYPE;
                    String sql4 = UPDATE + ConditionTable.TABLE_NAME+
                            SET+ConditionTable.Columns.CONDITION_CONFIG+EQUAL+ConditionTable.Columns.ACTIVITY_INTENT;

                    String sql5 = DROP_IF_TABLE_EXISTS + CONDITIONBUILDER_TABLE_NAME;
                    String sql6 = DROP_IF_TABLE_EXISTS + CONDITIONSENSOR_TABLE_NAME;
                    String sql7 = DistinctConditionView.CREATE_VIEW;
                    
                	String sql8 = 
                    		DROP_IF_VIEW_EXISTS + BLANK_SPC + ActiveSettingsView.VIEW_NAME;
                    String sql9 = ActiveSettingsView.CREATE_VIEW;
                   

                    db.execSQL(sql1);
                    db.execSQL(sql2);
                    db.execSQL(sql3);
                    db.execSQL(sql4);
                    db.execSQL(sql5);
                    db.execSQL(sql6);
                    db.execSQL(sql7);
                    db.execSQL(sql8);
                    db.execSQL(sql9); 
                    db.setTransactionSuccessful();

                      // only increment oldVersion once!
                    currentVersion++;
                    if (LOG_INFO) Log.i(TAG,"update successful from "+(currentVersion-1)+" to "+currentVersion);
                    v34=true;

                } catch (Exception e) {
                    if (LOG_INFO) Log.e(TAG,"update failed: to: "+currentVersion);
                    e.printStackTrace();

                } finally {
                    db.endTransaction();
                }
                /* 
                 * 2.1 to 2.2 upgrade; Force Rule Publisher to re-publish rules
                 */
                XmlUtils.resetXmlVersionSharedPref(mContext);
                
            }


            if (LOG_INFO) Log.i(TAG, "upgraded= v26="+v26+"v27="+v27+"v28="+v28+"v29="+v29+
			"v30="+v30+"v31="+v31+"v32="+v32+"v33="+v33+"v34="+v34+"ver="+db.getVersion());
        }

    }
    
    /** class to assign during query operations as CursorFactory, so that the provider can 
     * close the database when the cursor instance is closed.
     */
    public class CursorFactoryManager implements SQLiteDatabase.CursorFactory {

    	/** basic constructor */
		public CursorFactoryManager() {
			super();
		}
    	
    	/** method required by CursorFactory */
		public Cursor newCursor(SQLiteDatabase db,
				SQLiteCursorDriver masterQuery, String editTable,
				SQLiteQuery query) {
			
			return new MySQLiteCursor(db, masterQuery, editTable, query);
		}

    }
    
    
    /** allows us to close the db & helper after closing the cursor or during finalize() */
    public class MySQLiteCursor extends SQLiteCursor {
    	
		public MySQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
				String editTable, SQLiteQuery query) {
			super(db, driver, editTable, query);
			mDb = db;
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteCursor#close()
		 */
		@Override
		public void close() {
			super.close();
			SQLiteManager.this.close("MySQLiteCursor.close");
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteCursor#finalize()
		 */
		@Override
		protected void finalize() {
			if (!this.isClosed())
				this.close();
			SQLiteManager.this.close("MySQLiteCursor.finalize");
			super.finalize();
		}   	
		
		
    }
}
