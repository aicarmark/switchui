// ********************************************************** //
// PROJECT:     APR (Automatic Panic Recording)
// DESCRIPTION: 
//   The purpose of APR is to gather the panics in the device
//   and record statics about those panics to a centralized
//   server, for automated tracking of the quality of a program.
//   To achieve this, several types of messages are required to
//   be sent at particular intervals.  This package is responsible
//   for sending the data in the correct format, at the right 
//   intervals.
// ********************************************************** //
// Change History
// ********************************************************** //
// Author         Date       Tracking  Description
// ************** ********** ********  ********************** //
// Stephen Dickey 03/01/2009 1.0       Initial Version
//
// ********************************************************** //
package com.motorola.motoapr.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.DatabaseObjectNotClosedException;

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
public class APRMessageDatabase extends SQLiteOpenHelper {
	
		static String TAG = "APRMessageDatabase";
	
        static private SQLiteDatabase sqlite_database = null;
        
        static Context mContext = null;

	
		public enum APRFields {
			 KEY, TITLE, RAW_DATA, TIMESTAMP
		 }
		 
		 // SQLDatabase Related

		 static int    APR_DATABASE_VERSION = 1;
		 static String APR_DATABASE_NAME = "APRMessageDatabase" + APR_DATABASE_VERSION;
		 static String APR_MESSAGE_TABLE_NAME = "Messages" + APR_DATABASE_VERSION;
		 static String APR_ID_FIELD = "_id";
		 static String APR_TYPE_FIELD = "type";
		 static String APR_ADDRESS_FIELD = "address";
		 static String APR_SUBJECT_FIELD = "subject";
		 static String APR_TIMESTAMP = "timestamp";
		 static String APR_BODY_FIELD = "body";
		 
		 
		// Constructor...
		APRMessageDatabase( Context context ) {
				
			super( context, APR_DATABASE_NAME, null, APR_DATABASE_VERSION );
				
			mContext = context;
			
			startDatabase();
			
        Cursor cursor = getGenericCursor(APR_MESSAGE_TABLE_NAME);

        if (cursor == null) {
		APRDebug.APRLog( TAG, "APRMessageDatabase cursor == null");
            createTable(sqlite_database);

        } else {
		APRDebug.APRLog( TAG, "APRMessageDatabase cursor.close()");
            cursor.close();
        }

			stopDatabase();
		}
		
		private static void startDatabase() {
			
			Context context = mContext;
		
			try {
				if ( sqlite_database == null )
				{
				
					sqlite_database = 
						context.openOrCreateDatabase( APR_DATABASE_NAME, 
								Context.MODE_PRIVATE, null);
			
					// confirmed is necessary, again on 3/23/09 
					// createTable( sqlite_database );
				}
							
			} catch( IllegalStateException e) {
				APRDebug.APRLog ( TAG, "Exception during Message DB Creation:" + APR_DATABASE_NAME );
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
			deleteRow( APR_MESSAGE_TABLE_NAME, APR_ID_FIELD, id );
		}
		
		protected static void deleteAll() {
			deleteDB( APR_MESSAGE_TABLE_NAME );
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
				
			} catch ( DatabaseObjectNotClosedException e ) {

				//APRDebug.APRDebugStack(e);				

			} finally {
				APRDebug.APRLog( TAG, "getGenericCursor 111" );
			}
			
			return( return_cursor );
		} 
			
		public Cursor GetDBCursor() {
			startDatabase();
			
			sqlite_database.beginTransaction();
			
			Cursor return_cursor = getGenericCursor( APR_MESSAGE_TABLE_NAME );
			
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
			
			String dbBuild_str = "CREATE TABLE " + APR_MESSAGE_TABLE_NAME + " ("
			 + APR_ID_FIELD      + " INTEGER PRIMARY KEY,"
			 + APR_TYPE_FIELD    + " TEXT,"
			 + APR_ADDRESS_FIELD + " TEXT,"
			 + APR_SUBJECT_FIELD + " TEXT,"
			 + APR_BODY_FIELD    + " TEXT,"
			 + APR_TIMESTAMP     + " TEXT"
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
		public void StoreEntry( String message_type, 
				                String address, 
				                String subject, 
				                String body) {
			
			// get the current time...
			long timestamp = System.currentTimeMillis();
			long db_key = timestamp;
			
			
			try {	
				startDatabase();
				
				// begin the transaction
				sqlite_database.beginTransaction();			

				String TransactString = BuildInsertionString( 
					db_key, 
					message_type,
					address,
					subject,						     
					body,
					timestamp );
				
				APRDebug.APRLog( TAG, "APRMessageDatabase Inserting in Message DB: " + TransactString);
			
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
				String message_type,
				String address,
				String subject,
				String message_body, 
				long timestamp ) 
		{
			
			String tString = new String( "insert into " + APR_MESSAGE_TABLE_NAME + " (" 
					+ APR_ID_FIELD + "," 
				    + APR_TYPE_FIELD + ","
					+ APR_ADDRESS_FIELD + ","
					+ APR_SUBJECT_FIELD + ","
					+ APR_BODY_FIELD + ","
					+ APR_TIMESTAMP  
					+ ") values ( " 
					+ "NULL" + ",'"
					+ message_type + "','"
					+ address + "','"
					+ subject + "','"
					+ message_body + "','"
					+ timestamp + "');" );
						
			// APRDebug.APRLog( TAG, tString );
			
			return tString;
		}
					
	}  // End APRDatabase Helper...
