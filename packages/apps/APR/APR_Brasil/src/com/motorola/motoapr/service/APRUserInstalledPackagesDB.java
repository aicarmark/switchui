//********************************************************** //
//PROJECT:     APR (Automatic Panic Recording)
//DESCRIPTION: 
//The purpose of APR is to gather the panics in the device
//and record statics about those panics to a centralized
//server, for automated tracking of the quality of a program.
//To achieve this, several types of messages are required to
//be sent at particular intervals.  This package is responsible
//for sending the data in the correct format, at the right 
//intervals.
//********************************************************** //
//Change History
//********************************************************** //
//Author         Date       Tracking  Description
//************** ********** ********  ********************** //
//Stephen Dickey 03/01/2009 1.0       Initial Version
//
//********************************************************** //
package com.motorola.motoapr.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
* Class to create an SQL Database, if required,
*   or restore content from a previous instance of the
*   SQL Database.  This is required to ensure that we track
*   all possible messages sent to the APR Server, and only
*   eliminate them once their sending is complete.  "Sending"
*   a crash, as an external API, is reduced to adding an entry  
*   in the database, and internal timing of the APR Server
*   handles the rest.
* @author wlsd10
*/
public class APRUserInstalledPackagesDB extends SQLiteOpenHelper {
	
	 static String TAG = "APRUserInstalledPackagesDB";
	
   static private SQLiteDatabase sqlite_database = null;
   
   static Context mContext = null;

	
		public enum APRFields {
			 KEY, TITLE, RAW_DATA, TIMESTAMP
		 }
		 
		 // SQLDatabase Related

		 static int    APR_UIP_DB_VERSION = 1;
		 static String AP_UIP_DB_NAME = "APRUserInstalledPackagesDB" + APR_UIP_DB_VERSION;
		 static String APR_UIP_TABLE_NAME = "UserInstalledPackages" + APR_UIP_DB_VERSION;
		 static String APR_UID_FIELD = "_id";
		 static String APR_PACKAGE_NAME_FIELD = "package_name";
		 static String APR_PACKAGE_UNUSED1 = "unused1";
		 static String APR_PACKAGE_UNUSED2 = "unused2";
		 
		// Constructor...
		 APRUserInstalledPackagesDB( Context context ) {
				
			super( context, AP_UIP_DB_NAME, null, APR_UIP_DB_VERSION );
				
			mContext = context;
			
			startDatabase();
			
			if ( getGenericCursor( APR_UIP_TABLE_NAME ) == null )
			{
				createTable( sqlite_database );
			}
			
			stopDatabase();
		}
		
		private static void startDatabase() {
			
			Context context = mContext;
		
			try {
				if ( sqlite_database == null )
				{
				
					sqlite_database = 
						context.openOrCreateDatabase( AP_UIP_DB_NAME, 
								Context.MODE_PRIVATE, null);
			
					// confirmed is necessary, again on 3/23/09 
					// createTable( sqlite_database );
				}
							
			} catch( IllegalStateException e) {
				APRDebug.APRLog ( TAG, "Exception during Package DB Creation:" + AP_UIP_DB_NAME );
				APRDebug.APRLog ( TAG, "DB May Not Have Been Closed." );

				APRDebug.APRDebugStack(e);
			} finally {
				APRDebug.APRLog( TAG, "Database Started");
			}
		}
		
		private static void stopDatabase() {
			
			try {
				if ( sqlite_database != null ) {
				
//					sqlite_database.close();
//					sqlite_database = null;
				}
			} catch ( Exception e ) {
				APRDebug.APRDebugStack(e);
			} finally {
				APRDebug.APRLog( TAG, "Database Stopped" );
			}
		}
		
		// delete the entry based upon the id passed in.
		//   this should delete at most one entry, because
		//   it is expected that all ids are unique.
		protected static void deleteEntry(int id) {
			deleteRow( APR_UIP_TABLE_NAME, APR_UID_FIELD, id );
		}
		
		protected static void deleteAll() {
			deleteDB( APR_UIP_TABLE_NAME );
		}

		// delete the entry based upon the id passed in.
		//   this should delete at most one entry, because
		//   it is expected that all ids are unique.
		protected static void deleteRow( String table_name,
				                         String matching_field_name,
				                         int id) {
					
			try
			{
				startDatabase();
				
				sqlite_database.beginTransaction();
				
				// the open helper delete command   
				//   just sucks. do it manually.
				if ( sqlite_database != null )
				{
					String deletion_str = "DELETE FROM " + table_name  + 
					 " WHERE " + matching_field_name + " = " + id + ";";
					
					// APRDebug.APRLog( TAG, deletion_str );
					
					sqlite_database.execSQL( deletion_str );		
					
					sqlite_database.setTransactionSuccessful();
				}
				
			} catch ( Exception e ) {
				e.printStackTrace();
			} finally {
			
				sqlite_database.endTransaction();
			
				stopDatabase();
			}
		}

		/**
		 * Delete all entries from the database.
		 */
		protected static void deleteDB( String table_name ) {
					
			try
			{
				startDatabase();
				
				sqlite_database.beginTransaction();

				// the open helper delete command   
				//   just sucks. do it manually.
				if ( sqlite_database != null )
				{
					sqlite_database.delete( table_name, "1", null);
										
					sqlite_database.setTransactionSuccessful();
				}
				
			} catch ( Exception e ) {
				
				APRDebug.APRDebugStack(e);
				
			} finally {
				
				sqlite_database.endTransaction();
			
				stopDatabase();
			}
		}

		private Cursor getGenericCursor( String table_name ) {			
			
			Cursor return_cursor = null;
			
			try {

				return_cursor = sqlite_database.rawQuery("SELECT * FROM " + table_name, null );
				
			} catch ( SQLiteException e ) {
				APRDebug.APRDebugStack(e);
				
				return_cursor = null;
				
			} finally {
				
			}
			
			return( return_cursor );
		} 
			
		public Cursor GetDBCursor() {
			startDatabase();
			
			sqlite_database.beginTransaction();
			
			Cursor return_cursor = getGenericCursor( APR_UIP_TABLE_NAME );
			
			sqlite_database.endTransaction();
			
			stopDatabase();
			
			return( return_cursor );
		}

		public Cursor GetMatchingPackageName( String package_name ) {
			
			Cursor return_cursor = null;
			
			startDatabase();
			
			sqlite_database.beginTransaction();
			
			try {
				
				String myQuery = "SELECT * FROM " + APR_UIP_TABLE_NAME + 
				" WHERE " + APR_PACKAGE_NAME_FIELD  + " = '" + package_name + "';";
				
				APRDebug.APRLog( TAG, "Package Query: " + myQuery );

				return_cursor = sqlite_database.rawQuery( myQuery, null );
				
			} catch ( SQLiteException e ) {
				APRDebug.APRDebugStack(e);
				
				return_cursor = null;
				
			} finally {
				
			}
			
			sqlite_database.endTransaction();
			
			stopDatabase();
			
			return( return_cursor );
		}

		
		// ******************************************************************* //
		// Required, Overridden functions from superclass.
		//  Don't need to call the super version of these.
		// ******************************************************************* //
		@Override
		public void onCreate(SQLiteDatabase db) {
			
			createTable( db );
		}
		
		private static void createTable( SQLiteDatabase db) {
			
			String dbBuild_str = "CREATE TABLE " + APR_UIP_TABLE_NAME + " ("
			 + APR_UID_FIELD      + " INTEGER PRIMARY KEY,"
			 + APR_PACKAGE_NAME_FIELD    + " TEXT,"
			 + APR_PACKAGE_UNUSED1 + " TEXT,"
			 + APR_PACKAGE_UNUSED2 + " TEXT"
			 + ");";

			APRDebug.APRLog( TAG, dbBuild_str );
	
			try 
			{
				db.execSQL( dbBuild_str );

			} catch ( SQLiteException e )
			{
				APRDebug.APRDebugStack(e);
				APRDebug.APRLog( TAG, "Database Table Creation Issue." );
			} catch ( Exception e ) {
				APRDebug.APRLog( TAG, "Database Table Creation Issue." );
			} catch ( Throwable err ) {
				APRDebug.APRLog( TAG, "Database Table Creation Issue." );
			} finally {
				APRDebug.APRLog( TAG, "createTable Executed." );
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// do nothing.
		}
		
		// Store a Crash Entry to the Database
		public void StoreEntry( String package_name, 
				                String unused1, 
				                String unused2 ) {
			
			// get the current time...
			long timestamp = System.currentTimeMillis();
			long db_key = timestamp;
			
			try {	
				startDatabase();
				
				// begin the transaction
				sqlite_database.beginTransaction();			

				String TransactString = BuildInsertionString( 
					db_key, 
					package_name,
					unused1,
					unused2						     
					);
				
				APRDebug.APRLog( TAG, "Inserting in Message DB: " + TransactString);
			
				sqlite_database.execSQL( TransactString );
			
				sqlite_database.setTransactionSuccessful();
										
			} catch ( SQLiteException e ) {
				APRDebug.APRLog( TAG, "Insertion FAILED." );
			} catch ( Exception e ) {
				APRDebug.APRLog( TAG, "Insertion FAILED." );
			} finally {
				
				sqlite_database.endTransaction();
				
				stopDatabase();				
			}
		}

		// Routine to build an insertion string
		private String BuildInsertionString(
				long prim_key, 
				String package_name,
				String unused1,
				String unused2
				 ) 
		{
			
			String tString = new String( "insert into " + APR_UIP_TABLE_NAME + " (" 
					+ APR_UID_FIELD + "," 
				    + APR_PACKAGE_NAME_FIELD + ","
					+ APR_PACKAGE_UNUSED1 + ","
					+ APR_PACKAGE_UNUSED2 
					+ ") values ( " 
					+ "NULL" + ",'"
					+ package_name + "','"
					+ unused1 + "','"
					+ unused2 + "');" );
						
			// APRDebug.APRLog( TAG, tString );
			
			return tString;
		}
					
	}  // End APRDatabase Helper...
