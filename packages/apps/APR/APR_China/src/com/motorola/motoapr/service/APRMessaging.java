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
import android.content.Context;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.os.Handler;
import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import android.telephony.PhoneStateListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.lang.Integer;

/**
 * Class to perform APR Emailing
 * @author wlsd10
 */
public class APRMessaging {
	
	private String TAG = "APRMessaging";

	// While "messaging" does appear to be able to send messages
    //   to an email account, this SMS interface appears to not 
    //   function properly for mail.  The SMS interface is working
    //   for sending to telephone #s, however.    
    static final String address = "panicdt@motorola.com";
	static final String subject = "APR Message";
	static int apr_identifier = 0;
	static String apr_name = null;
	static String apr_version = null;
	
	static Context mContext = null;
	
	// this is for accessing the message queue from the 
	//   service context.
	APRMessageDatabase service_message_queue = null;
		
	// This handler will be used to handle messaging timeouts.
	private Handler mHandler = new Handler();
	long mStartTime = 0;

	TelephonyManager      telephony_mngr = null;
	APRPhoneStateListener phone_state_listener = null;
	
        // if this is the first time we have to delay more to prevent
        //   system problems.  It's not the first time in general that's 
        //   at issue, it's the first time immediately after powerup.
        static boolean apr_messaging_first_time = true;

	static boolean messaging_in_progress = false;
	
	static boolean APRMessagingSpeedFast = false;
	
	static SMS messaging_interface = null;
	
	static CARRIER APRMessagingCarrierType = CARRIER.CHINA_UMTS;
	
	/**
	 * Constructor for APR Messaging.
	 */
	APRMessaging(Context context, APRMessageDatabase uses_message_queue ) {
		mContext = context;	

		telephony_mngr = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		
		service_message_queue = uses_message_queue;
		
		messaging_interface = new SMS(mContext);
		try {
			// get the app name and version programmatically, so
			//   we don't have to update this file.
			Resources appR = mContext.getResources();
			apr_identifier = appR.getIdentifier( "app_name", "string", mContext.getPackageName());
			apr_name = appR.getString( apr_identifier );
		
			PackageManager pm = mContext.getPackageManager();		
			PackageInfo packageinfo = pm.getPackageInfo( apr_name, 0 );
			apr_version = packageinfo.versionName;
		
			APRDebug.APRLog( TAG, "AppName: " + apr_name + " v." + apr_version );
			
		} catch ( NameNotFoundException e ) {
			APRDebug.APRLog( TAG, "NameNotFound!" );			
		}
	}

	/**
	 * Queue a byte[] email message.
	 * @param raw_message
	 */
	public void APRQueueMail( String message_type, byte[] raw_message ) 
	{			
		APRDebug.APRLog(TAG, "-----------------------------------");
		APRDebug.APRLog(TAG, "APRMessaging Enqueue Type   :" + message_type );
		APRDebug.APRLog(TAG, "-----------------------------------");
		APRDebug.APRLog(TAG, "APRMessaging Enqueue Message:" + raw_message );

		String log_string = new String();
			for ( int i = 0; i < raw_message.length; i ++ ) {
				log_string = log_string + String.format("%02x",raw_message[i]);
			}
		
			APRDebug.APRLog ( TAG, "APRMessaging raw_message:" + log_string );

		String base64_raw_message;

		try
		{
			base64_raw_message = EncodingUtils.getAsciiString(Base64.encodeBase64(raw_message));

			// Open the Mailing Database.  Include Subject, Title, and Base64 Encoded Data.
			service_message_queue.StoreEntry( message_type, 
					address, 
					subject, 
					base64_raw_message );

			// update the package that will be uploaded via ftp.
			APRBuildFTPPackage.add_file( null, "Base64MsgBody: " + base64_raw_message );

			// Make string for the package information: Always put before adding queued messages.
			String package_from = "From: (SwVersion)" + APRMessage.CurrentBaselabel() + " (BuildCRC)" + 
				Integer.toHexString(APRMessage.BuildCRC()).toUpperCase() + " (Barcode)" + 
				APRMessage.CurrentBarcode() + " (PhoneNumber)" + CurrentPhoneNumber() + " (APR)" + apr_version +
				" (UpTime)" + APRMessage.ReportedUptime();

			APRDebug.APRLog(TAG, "APRQueueMail package_from = " + package_from );

			APRBuildFTPPackage.add_file( null, package_from );

			// update the package that will be uploaded via ftp.  NOTICE!  Getting a Message Index INCREMENTS it.
			// you cannot get a message index that you will not send!!!!
			APRBuildFTPPackage.add_file( null, "Index: " + APRPreferences.APRPrefsGetMessageIndex() + " Base64MsgBody: " + base64_raw_message );
		
		// Start the messaging timer, and start it immediately.
		APRStartMailing();
		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Error Queueing Mail." );
		} finally {
			APRDebug.APRLog( TAG, "APR Queue Mail Executed." );
		}
	}
	
	public void APRMessagingSetParameters( boolean run_fast, CARRIER carrier )
	{
		APRMessagingSpeedFast = run_fast;
		APRMessagingCarrierType = carrier;
		
		// now that we've set the speed to something different, the timeouts need
		//   to be recalculated.  But don't cause the timeout to happen immediately
		APRStartMessagingTimer( false, false );
	}
	
    /**
     * Send Messages Out from the SQL Messaging Database.  If there are no messages
     * to send, terminate sending of messages.
     * 
     * NOTICE!  This function must ONLY be invoked with the messaging timer.
     *          Likewise, this function must not itself start any timers.  
     *          The timer cycle itself must decide when/how to send messages.
     * 
     * This function returns whether or not a timeout should be setup on it's behalf.
     */
	private boolean APRMessagingSender() {
		
		boolean timeout_should_be_set = false;
		
		int column_index = 0;
		String message_type = null;
		String address = null;
		String subject = null;
		String body = null;
		
		int database_id = -1;
		
		Cursor TimerMessageCursor = null;
		
		APRDebug.APRLog( TAG, "Attempting APRMessagingSender... ");		
		
		// only attempt to send if in service
		if ( !APRPhoneStateListener.isPhoneInService() )
		{
			//To avoid APR from not receiving in-service message, we should re-listen the message
			phone_state_listener = APRPhoneStateListener.getInstance();
			telephony_mngr.listen( phone_state_listener, PhoneStateListener.LISTEN_NONE );
			telephony_mngr.listen( phone_state_listener, PhoneStateListener.LISTEN_SERVICE_STATE );
			// phone is not in service.  cycle the timer.
			// Start the messaging timer, but execute it delayed...
			APRDebug.APRLog(TAG,"Phone Not In Service... Waiting to Send..." );
			timeout_should_be_set = true;
		}
		else
		{
			APRDebug.APRLog( TAG, "Phone In Service... Attempting to Send...");
			
			CharArrayBuffer temp_buffer = new CharArrayBuffer( 20 );		
						
			TimerMessageCursor = service_message_queue.GetDBCursor(); 
		
			if ( TimerMessageCursor != null ) try {
				
				/* we're running the handler
				 */	
				if( TimerMessageCursor.moveToFirst())
				{			
					column_index = TimerMessageCursor.getColumnIndex( APRMessageDatabase.APR_TYPE_FIELD );
					TimerMessageCursor.copyStringToBuffer( column_index, temp_buffer );
					message_type = new String( temp_buffer.data );
					message_type = message_type.substring(0,temp_buffer.sizeCopied);
				
					column_index = TimerMessageCursor.getColumnIndex( APRMessageDatabase.APR_ADDRESS_FIELD );
					TimerMessageCursor.copyStringToBuffer( column_index, temp_buffer );
					address = new String( temp_buffer.data );
					address = address.substring(0,temp_buffer.sizeCopied);
				
					column_index = TimerMessageCursor.getColumnIndex( APRMessageDatabase.APR_SUBJECT_FIELD );
					TimerMessageCursor.copyStringToBuffer( column_index, temp_buffer );
					subject = new String( temp_buffer.data );
					subject = subject.substring(0,temp_buffer.sizeCopied);
				
					column_index = TimerMessageCursor.getColumnIndex( APRMessageDatabase.APR_BODY_FIELD );
					TimerMessageCursor.copyStringToBuffer( column_index, temp_buffer );
					body = new String( temp_buffer.data );
				
					// Get the column index for the id filed.
					column_index = TimerMessageCursor.
						getColumnIndex( APRPanicDatabase.APR_ID_FIELD );
			
					database_id = TimerMessageCursor.getInt(column_index);
				
					// 	Send the Email Now...
					if ( sendEmail( database_id, message_type, address, subject, body ) )
					{						
						// Delete the entry associated with this id.
						APRMessageDatabase.deleteEntry( database_id );
						
						APRDebug.APRLog(TAG,"In APRMessagingSender - finishing the sending of one APR message");
					}
					else
					{
						APRDebug.APRLog( TAG, "Mailing Attempt Failed. ");
					}
			
					// Start the messaging timer, but execute it delayed...					
					timeout_should_be_set = true;
				}	
				else
				{
					APRDebug.APRLog( TAG, "Done Sending Messages. ");

					// Halt the timer, done sending messages.
					timeout_should_be_set = false;
				}
				
			} catch ( Exception e ) {
				
				APRDebug.APRDebugStack(e);
				
				APRDebug.APRLog( TAG, "Exception... ");
				
				timeout_should_be_set = false;
				
			} finally {
				
				// save resources while we're not using the cursor.
				TimerMessageCursor.deactivate();
				TimerMessageCursor.close();
			}
			
		} // end if phone has service.
		
		return( timeout_should_be_set );
	}
	
	/**
	 * Public to start the mailing engine, in case
	 * it isn't started already.
	 * @return
	 */
	private boolean APRStartMailing()
	{
		APRStartMessagingTimer( true, false );
		return true;
	}
	
	/**
	 * Start the messaging timer.  The purpose of this routine is to start
	 * the messaging activity, which will send one message immediately, and
	 * send the rest with some delay (all will actually have a slight delay)
	 * @param immediate
	 * @return
	 */
	private boolean APRStartMessagingTimer( boolean immediate, boolean force ) {
		
		// interestingly, if a timeout is zero, a runtime exception
		//   will take place.  Better to have some delay.
		
		// 250 mSeconds.
		final int APR_MESSAGING_IMMEDIATE = 250;
		
		// 20 seconds.
		final int APR_MESSAGING_DELAYED = 20000;

		// This delay is for the first message we send.  If it's the first time
		//  we need this delay or we start to have problems with the BP.  It will
		//  crash and have other difficulties.  
		final int APR_MESSAGING_FIRST_TIME = 120000;

		int apr_messaging_delay = 0;
		
		if ( !messaging_in_progress || force )
		{
		
			messaging_in_progress = true;
				
			/* set the first timeout to be immediate
			 * so as to get the first message out.
			 */
			if ( apr_messaging_first_time == false )
			{
				// if we are running fast, or if the calling software
				//   wanted an immediate message, then make the timeout
				//   short.
				if ( immediate )
				{
					apr_messaging_delay = APR_MESSAGING_IMMEDIATE;
				}
				else
				{
					apr_messaging_delay = APR_MESSAGING_DELAYED;
				}
			}
			else 
			{
				apr_messaging_delay = APR_MESSAGING_FIRST_TIME;
			}

			APRDebug.APRLog(TAG,"APRStartMessagingTimer Delay: " + apr_messaging_delay + "mS");
			
			mStartTime = System.currentTimeMillis();
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, apr_messaging_delay );
			
		} // end if messaging already in progress.
		
		return( immediate );
	} // END APRStartMessagingTimer
	
	private Runnable mUpdateTimeTask = new Runnable() {
		   public void run() {
			   
		       // Send out the most recently queued message.
		       //   we're executing the result of a timeout, which
		       //   may generate more timeouts.  the messaging_in_progress
		       //   flag, will tell us whether we actually are allowed to 
		       //   or not.
		       if ( APRMessagingSender() )
		       {			   		       		
		       		// start the timer again, delayed.
		       		APRStartMessagingTimer( false, true );
		       }
		       else
		       {
		    	    // stop the timer.
		   			mHandler.removeCallbacks(mUpdateTimeTask);
		   			
		       		// now, allow new timeouts to be sent... since
		       		//   the process of recurring timers has been
		    	    //   halted.
		       		messaging_in_progress = false;
		   			
		   			APRDebug.APRLog(TAG, "Timer Cycle Stopped...");
		       }
		       
	    	   // now that the first time delay has actually happened,
	    	   //   then allow any adjustments to occur.  
	       		apr_messaging_first_time = false;
		   }
		};
		
	private boolean sendEmail( int database_id, String message_type, String address, String subject, String body ) throws Exception {
		
		
		// Log the data as it is being sent.
		APRDebug.APRLog(TAG, "-----------------------------------");
		APRDebug.APRLog(TAG, "Database ID    :" + database_id );
		APRDebug.APRLog(TAG, "Message Type   :" + message_type );
		APRDebug.APRLog(TAG, "Sending Address:" + address );
		APRDebug.APRLog(TAG, "Sending Subject:" + subject );
		APRDebug.APRLog(TAG, "Sending Body   :" + body );
		APRDebug.APRLog(TAG, "-----------------------------------");

		return messaging_interface.sendSMS( APRMessagingCarrierType, address, body, message_type );
	}

	private String CurrentPhoneNumber() {
		String number = TelephonyManager.getDefault().getLine1Number();
		APRDebug.APRLog(TAG, "CurrentPhoneNumber number =" + number);
		if(!TextUtils.isEmpty(number)){
			return number;
		}
		else{
			return "NoNumber";
		}
	}
}
