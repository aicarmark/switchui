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

import android.content.Context;

// This class is for detecting whether a software version or 
//   flex change is required.  If it is required, this software
//   will construct the message, open the messages database, 
//   and cause the transmission of the message to occur.

public class APRSoftwareVersionFlexChange extends APRMessage { 

	APRMessaging mail = null;
	
	// ftp package list
	APRBuildFTPPackage    ftp_package_list = null;
	Context mcontext = null;

	// Constructor for Vector
	APRSoftwareVersionFlexChange( Context context, 
			                      APRMessageDatabase uses_message_queue ) throws BuildExceptionError {
		
		// Create the base message.
		super( context );
		
		try {
			// create the messaging interface for the first time
			mail = new APRMessaging( mContext, uses_message_queue );
		}
		catch ( VerifyError e )
		{
			APRDebug.APRDebugStack(e);
		}

		APRSoftwareVersionFlexChange_Restart();
	}

	void APRSoftwareVersionFlexChange_Restart() throws BuildExceptionError {
	
		APRDebug.APRLog( TAG, "NEW SoftwareVersionflexChange MESSAGE" );
		
		APRMessage_Restart();
		
		// initialize locals with the last preferences programmed.  NOT with the current
		//   build information.  
		sf_last_bldinfo_baselabel = APRPreferences.APRPrefsGetSwVers();
		sf_last_flex_version = APRPreferences.APRPrefsGetFlexVers();
		sf_last_blddetails_crc = APRPreferences.APRPrefsGetBuildCRC();
		
		// if the build info or the flex version has changed
		//   send the software version/flex change message.
		if ( ( sf_last_bldinfo_baselabel.compareTo(bldinfo_baselabel) != 0 ) ||
			 ( sf_last_flex_version.compareTo( flex_version ) != 0 )  ||
			 ( sf_last_blddetails_crc != blddetails_crc ) ) {
			
			APRDebug.APRLog( TAG, "Sending SwVersion Message..." );
			
			// make sure any subsequent actions have the latest data.
			APRPreferences.APRPrefsSetSwVers( bldinfo_baselabel );
			APRPreferences.APRPrefsSetFlexVers( flex_version );
			APRPreferences.APRPrefsSetBuildCRC( blddetails_crc );		

			// always create a new package list.  
			//   that way, the process of sending the package list
			//   is always unique as well.  
			ftp_package_list   = new APRBuildFTPPackage( mcontext );

			// delete the databases.  at this point we don't want any of the
			//   old messages going out any longer.
			APRPanicDatabase.deleteAll();
			APRMessageDatabase.deleteAll();
			
			// clean all the old preferences, that we longer want
			//  sticking around.  Subsequent code must address all these.
			APRPreferences.APRPrefsCleanPreferences();
			APRPreferences.APRPrefsSetTotalTimeElapsed(0);
			APRPreferences.APRPrefsSetTimeSinceLastReport(0);
			APRBuildFTPPackage.ClearBackupPackages();
			
			//Cleanup the BaseMessage
			APRCleanupBaseMessage(APRMessage.PANIC_APP_SW_FLEX_CHANGE_IND_MESSAGE_ID,
								  APRMessage.PANIC_APP_SW_FLEX_CHANGE_MSG_STRUCT_VERSION,
								  0 );

			// Append the extra data for this message type
			AppendStringToBase( bldinfo_baselabel, PANIC_APP_SW_VERSION_LENGTH );
			
			// Append the extra data for this message type
			AppendStringToBase( flex_version, PANIC_APP_FLEX_VERSION_LENGTH );

			// update the package that will be uploaded via ftp.
			APRBuildFTPPackage.add_file( null, "SwUpgradeMessage" );

			// Queue the message...
			mail.APRQueueMail( "SwVersion", BaseMessage );

			sf_last_bldinfo_baselabel = bldinfo_baselabel;
			sf_last_flex_version = flex_version;

			// send the report to the ftp server.
		//	ftp_package_list.report_send();
		}
	}
} // end APRSoftwareVersionFlexChange



