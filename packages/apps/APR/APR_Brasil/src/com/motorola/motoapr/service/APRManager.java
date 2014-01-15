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

import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import com.motorola.motoapr.service.PanicType.PANIC_ID;

import android.content.Context;
import android.content.Intent;
import com.motorola.motoapr.service.APRDebug;

/**
 * This is the overall APR Manager.  
 * @author wlsd10
 *
 */
public class APRManager	{

    static final String TAG="APRManager";

	APRMessageDatabase MessageDB;
	APRSoftwareVersionFlexChange swver;
	APRBuildPanicMessages panics;
	
	static APRPeriodicMessageAndTimer periodic;
	
	// needs to stick around permanently.  static!
	static APRFileObserver file_observer = null;

	Context mContext = null;
			
	// Constructor
	public APRManager( Context basecontext, boolean run_fast, CARRIER carrier ){	

		mContext = basecontext;
		
		// Initialize APR
		APRInit( mContext, run_fast, carrier );
		APRManagerRestart();

		return;
	}
	
	public void APRManagerRestart() {
		
		// generate a new software version flex change message.
		//   and update all the preferences.
		try {
			swver.APRSoftwareVersionFlexChange_Restart();
		} catch (BuildExceptionError e) {
			
			// failed to build our message somehow.  Restart the service.
			APRService.APRServiceRestart();
		}
	}

  	// This is the APR Initialization Software.  It will 
	//   create whatever databases are needed, instantiate the
	//   classes that use them, etc.
	public void APRInit( Context context, boolean run_fast, CARRIER carrier ) {
				
		// Create the APR Message Database
		MessageDB = new APRMessageDatabase(context);

		APRDebug.APRLog( TAG, "APRMessageDatabase Instantiated." );
		
		// Instantiate Software Version Change Messages		
		try {
			swver = new APRSoftwareVersionFlexChange( context, MessageDB);
		} catch (BuildExceptionError e1) {
			
			// failed to build our message somehow.  Restart the service.
			APRService.APRServiceRestart();
		}

		APRDebug.APRLog( TAG, "APRSoftwareVersionFlexChange Instantiated." );
	
		try {
			panics = new APRBuildPanicMessages( context, MessageDB, run_fast, null );

		} catch ( RuntimeException e) {
			APRDebug.APRLog( TAG, "APRBuildPanicMessages Failed." );
		    APRDebug.APRDebugStack(e);
		}

		APRDebug.APRLog( TAG, "APRBuildPanicMessages Instantiated." );
		
		// Instantiate Periodic Messages and Timer for Future Msgs.
		try {
			periodic = new APRPeriodicMessageAndTimer( context, MessageDB );
		} catch (BuildExceptionError e) {
			// failed to build our message somehow.  Restart the service.
			APRService.APRServiceRestart();
		}

		APRDebug.APRLog( TAG, "APRPeriodicMessageAndTimer Instantiated." );
		
		if ( file_observer == null ) {
			file_observer = new APRFileObserver(context);
			APRDebug.APRLog( TAG, "APRFileObserver Instantiated." );
		} else {
			APRDebug.APRLog( TAG, "Using Existing FileObserver" );
		}
	} // end APRInit
	
	/**
	 * Since the APR Manager has taken over all responsibilities
	 * related to managing APR, we must provide these APIs as needed
	 * by the Service layer.
	 * @param filename TODO
	 * @param panic_id this is the crash log we intend to store.
	 * @param id
	 * @param dsp_version
	 * @param crash_log
	 * @param timestamp
     * @return
	 */
	public boolean APRMgrStoreEntry( String filename, PANIC_ID panic_id, int id, int dsp_version, byte[] crash_log, long timestamp, String panic_data_str )
	{
		try {

			if ( filename == null ) {
				filename = "UNKNOWN";
			}

			if ( panic_data_str == null ) {
				panic_data_str = "UNKNOWN";
			}

			// temporary fix to suppress app_process failures in APR recording.
			if ( filename.contains( "tombstone") && panic_data_str.contains( "app_process" ) ) {
				// reject this panic
				return false;
			}
			APRDebug.APRLog( TAG, "APRMgrStoreEntry ........." );

			// Store the entry
			panics.PanicDB.StoreEntry( filename, panic_id, id, dsp_version, crash_log, timestamp, panic_data_str );

			APRPreferences.APRPrefsCrashCount( panic_id );

			// Scan the database and cause messages to be built for sending.
			panics.APR_BuildSomeMessages();

            // IKCBSMMCPPRC-422 start
            // Send broadcast to support save the log for BYD
            Intent Savelog_intent = new Intent("Intent.action.panic_savelog");
            if ( panic_id == PANIC_ID.MODEM_CRASH ) {
                Savelog_intent.putExtra("type", "1");
            }
            else {
                Savelog_intent.putExtra("type", "2");
            }
            mContext.sendBroadcast(Savelog_intent);
            // IKCBSMMCPPRC-422 end


		} catch ( NullPointerException e ) {
			APRDebug.APRLog( TAG, "Panic Recording Failure." );
		}

		return true;
	}	
	
	/**
	 * Send out whatever email is stored in the database... now.
	 */
	public boolean APRMgrSendStoredEmail()
	{
		return true;
	}
	
	/**
	 * Queue an email for eventual sending.  Generic API for us
	 * to use for testing, or for really sending an email.
	 * @param message_type
	 * @param address
	 * @param subject
	 * @param body
	 * @return
	 */
	public boolean APRMgrQueueEmail( String message_type, 
			                         String address,
			                         String subject,
			                         String body ) {
		
		MessageDB.StoreEntry(message_type, address, subject, body);
		
		return true;
	}
}
