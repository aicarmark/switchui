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

import org.apache.commons.codec.binary.Base64;
import org.apache.http.util.EncodingUtils;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.DatabaseObjectNotClosedException;
import java.lang.Integer;

// Routine to create an SQL Database, if required,
//   or restore content from a previous instance of the
//   SQL Database.  This is required to ensure that we track
//   all possible messages sent to the APR Server, and only
//   eliminate them once their sending is complete.  "Sending"
//   a crash, as an external API, is reduced to adding an entry  
//   in the database, and internal timing of the APR Server
//   handles the rest.
public class APRPanicDatabase extends SQLiteOpenHelper {
		 
		 static String TAG = "APRPanicDatabase";
	
         static private SQLiteDatabase sqlite_database = null;
	
		 // SQLDatabase Related

		 static int APR_DATABASE_VERSION = 3;
		 static String APR_DATABASE_NAME = "APRPanicDatabase" + APR_DATABASE_VERSION;
		 static String APR_TABLE_NAME = "Panics" + APR_DATABASE_VERSION;
		 
		 // this is the DATABASE ID field.
		 static String APR_ID_FIELD = "_id";
		 
		 // this is a field that REFERENCES the panic id entry.  See PanicType.java 
		 static String APR_PANIC_ID_INDEX = "panic_id_index";
		 static String APR_TIMESTAMP = "timestamp";
		 static String APR_DSP_VERSION_FIELD = "dsp_version";
		 static String APR_DATA_FIELD = "data";
		 static String APR_CALL_TIME = "call_time";
		 
		 static Context mContext = null;
			
		// Constructor...
		APRPanicDatabase( Context context ) {
			
			super( context, APR_DATABASE_NAME, null, APR_DATABASE_VERSION );
			
			mContext = context;
			Cursor cursor = null;
			
			try {
				startDatabase();
				
				
				cursor = getGenericCursor( APR_TABLE_NAME );

			} catch ( Exception e ) {
				APRDebug.APRLog( TAG, "Failed To Get Cursor.  Creating New Table" + e );
			} finally {

				if ( cursor == null )
				{
					createTable( sqlite_database );
				} else {
					cursor.close();
				}

				stopDatabase();
			}
		}

		
		private static void startDatabase()
		{
			Context context = mContext;
			
			try {
				// don't open if it is already open.
				if ( sqlite_database == null )
				{
					sqlite_database = context.openOrCreateDatabase( APR_DATABASE_NAME, 
																	Context.MODE_PRIVATE, 
																	null);
				}				
			} catch( RuntimeException e) {
				
				APRDebug.APRLog ( TAG, "Exception during Panic DB Creation:" + APR_DATABASE_NAME );
				APRDebug.APRLog ( TAG, "DB May Not Have Been Closed." );
				APRDebug.APRDebugStack(e);
				
			} finally {
				APRDebug.APRLog( TAG, "Database Started");
			}
		}
		
		private static void stopDatabase()
		{
			try
			{
				if ( sqlite_database!= null ) 
				{
//					sqlite_database.close();		
//					sqlite_database = null;
				}
				
			} catch ( Exception e ) {
				APRDebug.APRDebugStack(e);
			} finally {
				APRDebug.APRLog( TAG, "Database Stopped" );
			}
		}
		
		public Cursor GetDBCursor() {

			startDatabase();
			
			sqlite_database.beginTransaction();
			
			Cursor return_cursor = getGenericCursor( APR_TABLE_NAME );
			
			sqlite_database.endTransaction();
			
			stopDatabase();
			
			return( return_cursor );
		}
		
		public static void deleteEntry( int id )
		{
			deleteRow( APR_TABLE_NAME, APR_ID_FIELD, id );
		} 
		
		public static void deleteAll()
		{
			deleteDB( APR_TABLE_NAME );
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
				
				if ( sqlite_database != null )
				{
					String deletion_str = "DELETE FROM " + table_name  + 
					 " WHERE " + matching_field_name + " = " + id + ";";
					
					APRDebug.APRLog( TAG, deletion_str );
					
					sqlite_database.execSQL( deletion_str );		
					
					sqlite_database.setTransactionSuccessful();
				}
				
			} catch ( Exception e ) {
				APRDebug.APRDebugStack(e);
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
	APRDebug.APRLog( TAG, "cm: getGenericCursor " + table_name );
			
			try {

				return_cursor = sqlite_database.rawQuery("SELECT * FROM " + table_name, null );
				
			} catch ( SQLiteException e ) {
				
				APRDebug.APRDebugStack(e);
				
				return_cursor = null;				
				
			} catch ( DatabaseObjectNotClosedException e ) {

				//APRDebug.APRDebugStack(e);

			} finally {
				
			}
			
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
		
			if ( db != null )
			
			try 
			{
				db.execSQL("CREATE TABLE " + APR_TABLE_NAME + " ("
	                + APR_ID_FIELD        + " INTEGER PRIMARY KEY,"
	                + APR_PANIC_ID_INDEX  + " INTEGER,"
	                + APR_DSP_VERSION_FIELD + " INTEGER,"
	                + APR_DATA_FIELD      + " TEXT,"
	                + APR_TIMESTAMP       + " TEXT,"
	                + APR_CALL_TIME       + " INTEGER"   
	                + ");");
				
			} catch ( SQLiteException e ) {
				APRDebug.APRDebugStack(e);
				APRDebug.APRLog( TAG, "Database Table Creation Issue." );
			} catch ( Exception e ) {
				APRDebug.APRLog( TAG, "Database Table Creation Issue." );
			} catch ( Throwable err ) {
				APRDebug.APRLog( TAG, "Database Table Creation Issue." );
			} finally {
				APRDebug.APRLog( TAG, "createTable Executed" );
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// do nothing.
		}
		
		// Store a Crash Entry to the Database
		// Accept the panic ID as an input.
		
		// This must be the ONLY method for inserting a panic.
		//   it is the choke point for determining if we have finished our
		//   initial scan.
		public void StoreEntry( String filename, PANIC_ID panic_id, int id, int dsp_version, byte[] crash_log, long timestamp, String panic_data_str ) {

			// if we have scanned the file system for panics once
			//   already, then allow new panic messages to be injected.
			//   Otherwise, do not allow.
			// 20100818 Let's allow panics happens before enable the APR
			//if ( APRPreferences.APRPrefsGetInitialScanComplete() )
			{
				// panic_id is expected to be correct at this time 

				String TransactString = BuildInsertionString( 
						id,             // the actual panic id.
						dsp_version,    // the dsp version.
						crash_log,      // raw byte crash log.
						timestamp		// the timestamp.
						);	
				String   hexString   =   Integer.toHexString(id); 
				APRDebug.APRLog( TAG, "APRPanicDatabase InsertionString: id=" + hexString );

				// build panic data using crash_log
				if ( panic_data_str == null )
				{
					StringBuffer crashHexString = new StringBuffer();
					for ( int i=0 ; i<crash_log.length ; i++ ) {
						String single_byte_hex = Integer.toHexString(0xFF & crash_log[i]);
						if (single_byte_hex.length() == 1) { 
							// append leading zero
							crashHexString.append('0'); 
						} 
						crashHexString.append(single_byte_hex); 
					}
					panic_data_str = crashHexString.toString();
				}

				String panic_content = "PanicData: (PanicID)" + Integer.toHexString(id).toUpperCase() + " " + panic_data_str;
				// update the package that will be uploaded via ftp.
				//APRBuildFTPPackage.add_file( filename, panic_content );
				
				try {	
					APRDebug.APRLog( TAG, "APRPanicDatabase InsertionString: " + TransactString );

					startDatabase();
					
					// begin the transaction
					sqlite_database.beginTransaction();			
					
					// Increment the Panic Count Stats.
					APRPreferences.APRPrefsPanicCount( panic_id.critical_panic );
								
					APRDebug.APRLog( TAG, "Inserting in panicDB: " + TransactString);
				
					sqlite_database.execSQL( TransactString );
				
					sqlite_database.setTransactionSuccessful();
				
					APRDebug.APRLog( TAG, "Insertion Successful." );
								
				} catch ( SQLiteException e ) {

					APRDebug.APRLog( TAG, e.getLocalizedMessage() );

					try {
						createTable( sqlite_database );
						
						sqlite_database.execSQL( TransactString );
						
						sqlite_database.setTransactionSuccessful();
						
						APRDebug.APRLog( TAG, "Insertion Successful." );
						
					} catch ( SQLiteException err ) {

						APRDebug.APRLog( TAG, err.getLocalizedMessage() );
					}
				} finally
				{
				
					// end the transaction
					sqlite_database.endTransaction();
				
					stopDatabase();
				}
				
			} // end if panic storage allowed
		}

		// Routine to build an insertion string
		private String BuildInsertionString( int id, int dsp_version, byte[] raw_data, long timestamp ) {
						
			String base_64_string = RawToString( raw_data );
			
			String tString = new String( "insert into " + APR_TABLE_NAME + " (" 
					+ APR_ID_FIELD + ","
					+ APR_PANIC_ID_INDEX + ","
					+ APR_DSP_VERSION_FIELD + ","
					+ APR_DATA_FIELD + ","  
					+ APR_TIMESTAMP + ","
					+ APR_CALL_TIME +
					") values ( " +
					"NULL" + ",'"              // APR_UID_FIELD (Database key, generate randomly)
					+ id + "','"  			   // APR_PANIC_ID_INDEX
					+ dsp_version + "','"      // APR_DSP_VERSION_FIELD
					+ base_64_string + "','"   // APR_DATA_FIELD
					+ timestamp + "','"        // APR_TIMESTAMP
					+ 0 + "');" );             // APR_CALL_TIME
			
			return tString;
		}
		
		public static String RawToString( byte[] raw )
		{
            String return_string = null;
            
            return_string = EncodingUtils.
              getAsciiString(Base64.
              	encodeBase64(raw));
            
            return( return_string );
		}
		
		public static byte[] StringToRaw( String str )
		{
			byte[] return_raw = null;
			
			return_raw = Base64.decodeBase64( str.getBytes());
			
			return( return_raw );
		}
				
	}  // End APRDatabase Helper...
