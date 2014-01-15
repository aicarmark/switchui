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

import java.util.Timer;
import android.content.Context;
import android.database.Cursor;
import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import android.util.Log;


/**
 * This class will scan the SQL database of stored panics
 *   for things it needs to send.  
 * @author wlsd10
 */
public class APRBuildPanicMessages extends APRMessage {
	
	private static final int APR_PANIC_SCAN_MAX_ITERATIONS = 5;
	private static final int APR_PANIC_MAX_DATA_LENGTH = 36;
	
	Cursor APRPanicCursor = null;
	APRMessage PanicMessage = null;
	APRMessaging mail = null;	
	
	APRMessageDatabase message_queue = null;
	
	Timer mTimer = null;
	
	int loop_count = 0;
	
	public APRBuildPanicMessages( Context context, 
			              APRMessageDatabase uses_message_queue, 
			              boolean run_fast,
			              CARRIER carrier ) {
		
		// Updates the mContext variable.
		super( context );
		APRDebug.APRLog( TAG, "APRBuildPanicMessages start");
		
		message_queue = uses_message_queue;

		// create the messaging interface for the first time
		mail = new APRMessaging( mContext, message_queue );

		// set the initial running speed.
		mail.APRMessagingSetParameters(run_fast, carrier );
				
		//Cleanup the BaseMessage
		try {
		APRCleanupBaseMessage(APRMessage.PANIC_APP_PANIC_REPORT_MESSAGE_ID,
					          APRMessage.PANIC_APP_PANIC_REPORT_MSG_STRUCT_VERSION, 
					          0 );
		} catch (BuildExceptionError e) {
			APRDebug.APRLog( TAG, "Failed APRCleanupBaseMessage Failed");
			return;
		}
		
		// Scan and Send what panics we have in the database.
		APR_BuildSomeMessages();
		
	} // APRBuildPanicMessages constructor
	
	public void APR_BuildSomeMessages() {
		
		int column_index = 0;
		APRDebug.APRLog( TAG, "APR_BuildSomeMessages Making the APR message before sending it to the message DB");
		
		// Get New cursor to access the data.
		APRPanicCursor = PanicDB.GetDBCursor();
		
		if ( APRPanicCursor != null ) try {

			loop_count = APRPanicCursor.getCount();

			if ( loop_count > APR_PANIC_SCAN_MAX_ITERATIONS )
			{
				loop_count = APR_PANIC_SCAN_MAX_ITERATIONS;
			}

			// loop through the database a few times.  
			//   gather the data up, and append it to our message.
			//   
			if ( APRPanicCursor.moveToFirst() ) {

				for ( int i = 0; i < loop_count; i++ )
				{	
					APRDebug.APRLog( TAG, "APR_BuildSomeMessages Sending Panic Message..." );

					// Get the call time				
					column_index = APRPanicCursor.getColumnIndex( APRPanicDatabase.APR_DSP_VERSION_FIELD );
					int dsp_version = APRPanicCursor.getInt( column_index );	
					
					//Cleanup the BaseMessage for each possible message we're going to send.
					APRCleanupBaseMessage(APRMessage.PANIC_APP_PANIC_REPORT_MESSAGE_ID, 
										  APRMessage.PANIC_APP_PANIC_REPORT_MSG_STRUCT_VERSION,
										  dsp_version );
					
					// Get the call time				
					column_index = APRPanicCursor.getColumnIndex( APRPanicDatabase.APR_CALL_TIME );
					int call_time = APRPanicCursor.getInt( column_index );	
					String   hexString2   =   Integer.toHexString(call_time); 
					APRDebug.APRLog( TAG, "APR_BuildSomeMessages Sending Panic Message call_time..." + hexString2);
					AppendIntToBase( call_time, true );
					
					column_index = APRPanicCursor.getColumnIndex( APRPanicDatabase.APR_TIMESTAMP );
					int timestamp = APRPanicCursor.getInt( column_index );
					 String   hexString1   =   Integer.toHexString(timestamp); 
					APRDebug.APRLog( TAG, "APR_BuildSomeMessages Sending Panic Message timestamp..." + hexString1);
					AppendIntToBase( timestamp, true );
					
					if ( BaseMessage.length != 72 ) {
						throw new BuildExceptionError( "Size Not Correct" );
					}
					
					column_index = APRPanicCursor.getColumnIndex( APRPanicDatabase.APR_PANIC_ID_INDEX );
					int panic_id_index = APRPanicCursor.getInt( column_index );
					int append_panic_id = panic_id_index;
				       String   hexString   =   Integer.toHexString(append_panic_id); 

					
					
					// Get the raw data
					column_index = APRPanicCursor.getColumnIndex( APRPanicDatabase.APR_DATA_FIELD );
					String buffer2 = APRPanicCursor.getString(column_index);
					byte[] raw_data = APRPanicDatabase.StringToRaw( buffer2 );
	
		
					// code was updated to actually keep the real panic id in the panic database.
					//   this enables us to move the entire creation of the panic information into
					//   the scanner software, now APRScanForPanics.java
					
					// PANIC ID is UINT32 on All other Motorola Programs.  This is Int, 4 bytes.
					APRDebug.APRLog( TAG, "APR_BuildSomeMessages Sending Panic Message panicid = " + hexString);	
					AppendIntToBase( append_panic_id, true );

					// Note... when we append raw_data of APR_PANIC_MAX_DATA_LENGTH, we're appending
					//   precisely that many characters.  
					 
					//read_array is containing the BP panic binary
					ArrayAppendToBase(raw_data, APR_PANIC_MAX_DATA_LENGTH );
							
					// Queue the outgoing message.
					mail.APRQueueMail( "Panic", BaseMessage );
					
				
					// Get the column index for the id filed.
					column_index = APRPanicCursor.
						getColumnIndex( APRPanicDatabase.APR_ID_FIELD );
				
					int EntryID = 0; 	
					EntryID = APRPanicCursor.getInt(column_index);
						
					// Delete the entry associated with this id.
					APRPanicDatabase.deleteEntry( EntryID );

						
					// Move to the next entry in the cursor.
					APRPanicCursor.moveToNext();	
					
				} // end Loop to send panics, and delete the existing ones from the 
			      // database.
			}
		} catch ( BuildExceptionError e ) {
			APRDebug.APRLog( TAG, e.getLocalizedMessage() );
			APRDebug.APRLog( TAG, e.getMessage() );
			APRDebug.APRLog( TAG, "Failure During Message Building.... retrying later.");

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, e.getLocalizedMessage() );
			APRDebug.APRLog( TAG, e.getMessage() );
			APRDebug.APRLog( TAG, "Failure During Message Building.");
		} finally {
			APRPanicCursor.deactivate();
			APRPanicCursor.close();
		}
		
	} // END APR_ScanAndSend
		
} // end APRBuildPanicMessages
