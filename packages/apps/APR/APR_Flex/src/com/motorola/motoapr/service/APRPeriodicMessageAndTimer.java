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

import com.motorola.motoapr.service.CarrierInfo.CARRIER;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

// This class is for generating a periodic message upon its
//   creation.  This initial message will be added to the outgoing
//   queue.  Furthermore a timer shall be created, causing 
public class APRPeriodicMessageAndTimer extends APRMessage {

	static String TAG = "APRPeriodicMessageAndTimer";

	private static final int APR_PERIODIC_GENERIC_DATA_LENGTH = 20;

	APRMessaging mail = null;
	static Context mContext = null;

	Timer mTimer = null;

	private static final long ONE_SECOND = 1000;
	private static final long ONE_MINUTE = 60*ONE_SECOND;
	private static final long ONE_HOUR   = 60*ONE_MINUTE;
	
	APRMessageDatabase message_queue = null;

	enum SPEED_SET {

		// index, periodic, update
		NORMAL6_SPEED( 0, 6*ONE_HOUR, 10*ONE_MINUTE ),
		NORMAL12_SPEED( 0, 12*ONE_HOUR, 10*ONE_MINUTE ),
		NORMAL18_SPEED( 0, 18*ONE_HOUR, 10*ONE_MINUTE ),
		NORMAL24_SPEED( 0, 24*ONE_HOUR, 10*ONE_MINUTE ),
		MEDIUM_SLOW_SPEED( 1, 30*ONE_MINUTE, 10*ONE_MINUTE ),
		MEDIUM_SPEED( 2, 20*ONE_MINUTE, 10*ONE_MINUTE ),
		FAST_SPEED( 3, 5*ONE_MINUTE, 1*ONE_MINUTE);

		public final int index;
		public final long periodic_message_interval;
		public final long update_delay_interval;

		SPEED_SET( final int index,
				   final long periodic_message_interval,
				   final long update_delay_interval ) {

			this.index = index;
			this.periodic_message_interval = periodic_message_interval;
			this.update_delay_interval = update_delay_interval;
		}

		public int getIndex() {
			return index;
		}

		public long getMessageInterval () {
			return periodic_message_interval;
		}

		public long getUpdateDelay () {
			return update_delay_interval;
		}
	}

	static SPEED_SET this_speed_set = APRSetSpeedFromPrefs( APRPreferences.APRGetDurationValue() );//SPEED_SET.NORMAL12_SPEED;

	public static SPEED_SET APRSetSpeedFromPrefs( String speed ){
		
  		if (speed.equals("6")) {
			return SPEED_SET.NORMAL6_SPEED;
		}
		else if(speed.equals("12"))
		{
			return SPEED_SET.NORMAL12_SPEED;
		}	
		else if(speed.equals("18"))
		{
			return SPEED_SET.NORMAL18_SPEED;
		}		
		else if(speed.equals("24"))
		{
			return SPEED_SET.NORMAL24_SPEED;
		}
		else
		{
			return SPEED_SET.NORMAL12_SPEED;
		}
   		
	}

	// ftp package list
	APRBuildFTPPackage    ftp_package_list = null; 

	/**
	 * Constructor.
	 * @param context
	 * @param uses_message_queue
	 * @param run_fast
	 * @param carrier
	 * @throws BuildExceptionError 
	 */
	APRPeriodicMessageAndTimer( Context context, 
			                    APRMessageDatabase uses_message_queue ) throws BuildExceptionError {

		super( context );

		message_queue = uses_message_queue;

		mContext = context;

		// create the messaging interface for the first time
		mail = new APRMessaging( mContext, message_queue );

		// Queue a single message.
		APRPeriodicCheckTimeAndQueue();

		// start the only alarm timer we have in this app.
		APRStartPeriodicUpdateTimer( mContext );
	}
		
	// routine that we're allowed to call, when we want to determine if 
	//   enough time has gone by to report a message.  this enables us to write
	//   additional code that can check this more frequently than we otherwise
	//   would have been checking (and failing to report).
	public void APRPeriodicCheckTimeAndQueue() throws BuildExceptionError  {	
	
		long time_since_last_report = APRPreferences.APRPrefsGetTimeSinceLastReport();
		
		if ( time_since_last_report > APRGetPeriodicMessageDelay() )
			{
				APRDebug.APRLog( TAG, "Sending Periodic Message..." );

				// always create a new package list.  
				//   that way, the process of sending the package list
				//   is always unique as well.  
				ftp_package_list   = new APRBuildFTPPackage( mContext );

				// Update the message contents
				APRMessageUpdatedMessage(APRMessage.PANIC_APP_PERIODIC_REPORT_MESSAGE_ID,
						                 APRMessage.PANIC_APP_PERIODIC_MSG_STRUCT_VERSION,
					                     0 );
		
				// Append the call time field to the periodic message.
				AppendIntToBase( 0, true);
		
				// Append the timestamp field to the periodic message.
				AppendIntToBase( 0, true);
		
				// Append generic data to the periodic message.
				ArrayAppendToBase( null, APR_PERIODIC_GENERIC_DATA_LENGTH );

				APRDebug.APRLog( TAG, "Enqueing New Message!" );

				// update the package that will be uploaded via ftp.
				APRBuildFTPPackage.add_file( null, "PeriodicMessage" );
		
				// Send the Message
				mail.APRQueueMail( "Periodic", BaseMessage );
				
				APRPreferences.APRPrefsSetTimeSinceLastReport( 0 );

				// send the report to the ftp server.
				//ftp_package_list.report_send();
			}
		else
		{
			APRDebug.APRLog( TAG, "Attempted to Enqueue Message, but was denied." );
		}
	}

    private static long APRGetPeriodicMessageDelay()
    {
        return( this_speed_set.getMessageInterval() );
    }

    public static long APRGetUpdateDelay()
    {
        return( this_speed_set.getUpdateDelay() );
    }	
	
	/**
	 * This is the fundamental tick of APR.  It allows us to track uptime.
	 */ 
	static PendingIntent PeriodicUpdatePendingIntent = null;
	static AlarmManager  PeriodicUpdateAlarmManager = null;
	static Intent        PeriodicUpdateUpdateIntent = null;
	
	static public void APRStartPeriodicUpdateTimer( Context mContext ) {
		
		// ensure that we use the same clock that we set the alarm by!
		long nextUpdate = android.os.SystemClock.elapsedRealtime() + this_speed_set.getUpdateDelay();

        if ( PeriodicUpdateAlarmManager == null )
        {
        	PeriodicUpdateAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        }
        
        if ( PeriodicUpdatePendingIntent == null )
        {
        	PeriodicUpdateUpdateIntent = new Intent(APRService.ACTION_UPDATE_TIMER_EXPIRATION);
        	PeriodicUpdateUpdateIntent.setClass(mContext, APRService.class);
        	PeriodicUpdatePendingIntent = PendingIntent.getService(mContext, 0, PeriodicUpdateUpdateIntent, 0);
        }
        else
        {
        	PeriodicUpdateAlarmManager.cancel( PeriodicUpdatePendingIntent );
        	PeriodicUpdateUpdateIntent = new Intent(APRService.ACTION_UPDATE_TIMER_EXPIRATION);
        	PeriodicUpdateUpdateIntent.setClass(mContext, APRService.class);
        }
        
        // Schedule alarm, and force the device awake for this update with this intent
        PeriodicUpdateAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextUpdate, PeriodicUpdatePendingIntent);

		APRDebug.APRLog( TAG, "Next App Status Update in " + APRGetUpdateDelay()/(ONE_MINUTE) + " minutes" );

		return;
	}

	public static boolean APRSetSpeed( String speed ){
		
		boolean return_result = false;
		
  		if (speed.equals("6")) {

			this_speed_set = SPEED_SET.NORMAL6_SPEED;
			return_result = true;

		}
		else if(speed.equals("12"))
		 {

			this_speed_set = SPEED_SET.NORMAL12_SPEED;
			return_result = true;

		}	
		else if(speed.equals("18"))
		 {

			this_speed_set = SPEED_SET.NORMAL18_SPEED;
			return_result = true;

		}		
		else if(speed.equals("24"))
		 {

			this_speed_set = SPEED_SET.NORMAL24_SPEED;
			return_result = true;

		}
   		
   		return( return_result );
	}

	public static long APRGetSpeed( ){

			   return( this_speed_set.getMessageInterval() );


		}

	
} // end APRSoftwareVersionFlexChange

