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


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import com.motorola.motoapr.service.PanicType.PANIC_ID;
import java.io.FileInputStream;

// SQLite Related

public class APRService extends Service {
	
	static String TAG = "APRService";
	
    // Intent action that will cause the service to restart
    public static final String ACTION_RELAUNCH_DELAYED = "com.motorola.motoapr.service.RELAUNCH_DELAYED";
    public static final String ACTION_UPDATE_TIMER_EXPIRATION = "com.motorola.motoapr.service.UPDATE_TIMER_EXPIRATION";
    public static final String ACTION_FILE_OBSERVER_HIT = "com.motorola.motoapr.service.FILE_OBSERVER_HIT";
    public static final String ACTION_SEND_BUG_REPORT = "com.motorola.motoapr.service.SEND_BUG_REPORT";
    
	String  carrier  = null;
	CARRIER carrier_type = null;
		
	private static APRManager AprManagerInstance = null;
	
	static Context mContext = null;
   
    String[] PanicSearchList = null;
   
    boolean debug_exceptions = false;
   
    APRPreferences preferences = null;
   
    APRScanForPanics panic_scanner = null;
   
    TelephonyManager      telephony_mngr = null;
   
    APRPhoneStateListener phone_state_listener = null;
   
    APRInstalledPackages installed_packages = null;
   
    static String prev_postfix = null; 
	
	// This is the interface as defined in IAPRService.java
	//  which must be supported by APRService.
    private final IAPRService.Stub binder = new IAPRService.Stub() {
    	
   		// PUBLIC APIs used for communicating with this service.
		public boolean APRSendEmail(String address, String subject, String message) {
			APRServiceLog( "SendAPREmail" );
			if ( AprManagerInstance == null ) return false;
			AprManagerInstance.APRMgrQueueEmail( "Generic", address, subject, message );			
			return true;
		} 

		public boolean APRStoreTestCrash( String crash ) {
			
			APRServiceLog( "StoreCrash" );
			
			if ( AprManagerInstance == null ) return false;
			
			// get the current time...
			long timestamp = System.currentTimeMillis();
			
			AprManagerInstance.APRMgrStoreEntry( null,
												 PANIC_ID.fromLabel("TEST_CRASH"),
												 PANIC_ID.fromLabel("TEST_CRASH").id,
												 0, 
												 crash.getBytes(), timestamp, crash );
			
			return true;
		}

		public boolean APRSendStoredEmail() {
			if ( AprManagerInstance == null ) return false;
			AprManagerInstance.APRMgrSendStoredEmail();
			return true;
		}

		public boolean APRToggleRunSpeed() throws RemoteException {
   			
   			boolean run_fast = APRPreferences.APRPrefsGetRunFast();
   			
   			if ( run_fast )
   			{
   				run_fast = false;
   			}
   			else 
   			{
   				run_fast = true;
   			}
   			
   			APRPreferences.APRPrefsSetRunFast( run_fast );
   			
  			return true;
		}

		public boolean APRGetRunSpeed() throws RemoteException {
   			
   			boolean run_fast = APRPreferences.APRPrefsGetRunFast();
  			
			return run_fast;
		}

		// This routine takes an index which is expected to be the correct index corresponding
		//   to the entries in the CARRIER enumeration.
		public boolean APRSetCarrierId(int carrier_index) throws RemoteException {

			APRPreferences.APRPrefsSetCarrierId( carrier_index );
			
			// set the preferences throughout the software.
			APRServiceSetPrefs();

			return false;
		}

		public int APRGetCarrierId() throws RemoteException {
			
			return APRPreferences.APRPrefsGetCarrierId();
		}
		
		// this is where APR gets turned on, as a request from
		//   the activity.
		public int APRSetEnabledState(boolean enabled_state)
				throws RemoteException {

			// Check if we really need to change the state.
			if ( APRGetEnabledState() != enabled_state )
			{				
				// restart APR Service upon change in state.
				APRServiceRestart( enabled_state );
			}
						
   			return 0;
		}
		
		public boolean APRGetEnabledState() throws RemoteException {
			return APRPreferences.APRPrefsGetEnabledState();
		}
		
		public boolean APRRescanPhoneInfo() throws RemoteException {
			
			boolean success = APRPreferences.APRServiceResetPhoneInfo();
			
			// re-launch... if we're enabled.
			APRServiceRelaunchDelayed( ACTION_RELAUNCH_DELAYED );
			
			return success;
		}
		
	};  // End APR Interface Description

    @Override
    public IBinder onBind(Intent intent) {
    	return this.binder;
    }
   			
	@Override
   	public void onCreate() {
   		super.onCreate();
   		
   		try {
   			// Get the base context for usage in this class.
   			mContext = getBaseContext();
   		   		
   			preferences = new APRPreferences( mContext );
   		
   			boolean run_fast = APRPreferences.APRPrefsGetRunFast();
   		
   			if ( run_fast )
   			{
   				APRPreferences.APRPrefsSetRunFast( false );
   			}
   		
   			carrier  = APRPreferences.APRPrefsGetCarrierName();
   			carrier_type = CARRIER.fromLabel(carrier);
   		   		   		
   			APRServiceRelaunchDelayed( ACTION_RELAUNCH_DELAYED );
   		
   		} catch( Exception e ) {
   			APRDebug.APRLog(TAG, "APRService OnCreate FAILED!!!" );
   		}
   		
   	} // end onCreate Creation of Service

	@Override
	public void onStart( Intent i, int j)
	{
		super.onStart(i,j);
		
		// one of the most important jobs of APR is to keep track of
		//   uptime.  It has to run, even if the service is not enabled.
		//   otherwise, someone could turn on APR, record a failure immediately
		//   and not be held accountable for all the previous uptime they had
		//   gathered to cause that failure to happen.
		APRPeriodicMessageAndTimer.APRStartPeriodicUpdateTimer( mContext );
		
		try {
			APRDebug.APRLog( TAG, "APRService onStart!!!");

			// Full blown restarting of the service.
			if (ACTION_RELAUNCH_DELAYED.equals(i.getAction())) {
				// The only real goal here is actually to send out a periodic message.
				LaunchAPRService();
			}
			
			// this is a short timeout, not to be used for sending Periodic Messages.
			if (ACTION_UPDATE_TIMER_EXPIRATION.equals(i.getAction())) {
				APRDebug.APRLog( TAG, "APRService onStart!! ACTION_UPDATE_TIMER_EXPIRATION!");
				
				// Update Uptime, even if APR is not enabled.
				APRMessage.APRUpdateUptime();
				
				if ( APRPreferences.APRPrefsGetEnabledState() ) 
					{
						UpdateAppList();
					
						// Do a quick check to verify if we need to send a message.
						//   it appears there are times when messages don't go out.
						APRManager.periodic.APRPeriodicCheckTimeAndQueue();
						
						// Scan the file system for panics.  If they've happened
						//   create messages and send them out.
						if ( panic_scanner != null ) panic_scanner.APRScanFileSystem();
					}
			}

			// panic file is changed, scan now.
            if (ACTION_FILE_OBSERVER_HIT.equals(i.getAction())) {

        		APRDebug.APRLog( TAG, "ACTION_FILE_OBSERVER_HIT is called" );
			
		// Check Failure Rate... but only in the act of seeing new
		//   failures in the file system.  
	    	CheckFailureRate();
		

                // Update Uptime, even if APR is not enabled.
                APRMessage.APRUpdateUptime();

                if ( APRPreferences.APRPrefsGetEnabledState() ) {
                    UpdateAppList();

                    // Do a quick check to verify if we need to send a message.
                    //   it appears there are times when messages don't go out.
                    APRManager.periodic.APRPeriodicCheckTimeAndQueue();

                    // Scan the file system for panics.  If they've happened
                    //   create messages and send them out.
                    if ( panic_scanner != null ) panic_scanner.APRScanFileSystem();
                }
            }
            
		} catch( Exception e )
		{
			APRDebug.APRLog(TAG, "Failed OnStart!!!");
		} finally {
			// stop this thread.
			stopSelf();
		}
	}
	
	@Override
	public void onDestroy( )
	{
		super.onDestroy();
		try {
		
			telephony_mngr.listen( phone_state_listener, PhoneStateListener.LISTEN_NONE );
			telephony_mngr = null;
		
			APRDebug.APRLog( TAG, "APRService onDestroy!!!");
		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "APRService onDestroy FAILED!!!");
		}
	}

	 // #####################################################################
	 // #####################################################################
	 // #####################################################################
	   		   	
	 // Use this to log common information.
	 static public boolean APRServiceLog( String message ) 
	 {		
		 APRDebug.APRLog( TAG, message );
	   	 return true;
	   	 
	 } // end APRServiceLog

		
	private void LaunchAPRService()
	{
		APRDebug.APRLog( TAG, "LaunchAPRServce!!!!");
		
		try 
		{	
			// Update the app list... even if we're not enabled yet.
	    	UpdateAppList();
	    	
			if ( APRPreferences.APRPrefsGetEnabledState() ) {
				
				if ( telephony_mngr == null ) 
				{
					telephony_mngr = 
						(TelephonyManager)mContext.
						getSystemService(Context.TELEPHONY_SERVICE);
				}
				
		    	if ( phone_state_listener == null ) 
		    		{
		    			phone_state_listener = APRPhoneStateListener.getInstance();
		    		}
		    	
		    	telephony_mngr.listen( phone_state_listener, 
		    			PhoneStateListener.LISTEN_SERVICE_STATE );
								
				boolean run_fast = APRPreferences.APRPrefsGetRunFast();

				if ( AprManagerInstance == null ) {
					// Create the APR Management Instance, but don't
					//   destroy a previous instance!
					AprManagerInstance = new APRManager( mContext, run_fast, carrier_type );
				} else {
					AprManagerInstance.APRManagerRestart();
				}

				// set the preferences throughout the software.
				APRServiceSetPrefs();
														
		   		try {
		   			
		   			if ( panic_scanner == null ) {
		   				panic_scanner = new APRScanForPanics( AprManagerInstance );
		   			}
					
					// Scan File System For Content
					panic_scanner.APRScanFileSystem();  
					
					// until we've finished our first scan of the file
					//  system, never allow those panics to go out.
					// from this moment forward, new panics are allowed.
					APRPreferences.APRPrefsSetInitialScanComplete( true );

				} catch (Exception e) {
					APRDebug.APRLog( TAG, "Failed Panic Scanner Init.");
					
					APRPreferences.APRPrefsSetEnabledState( false );

					stopSelf();
				}
				
				APRDebug.APRLog( TAG, "APR Launched ");
				
	  	   	}
	  	   	else
	  	   	{
	  	   		stopSelf();
	  	   	}

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, e.getLocalizedMessage() );
			APRDebug.APRLog( TAG, e.getMessage() );
			APRDebug.APRLog( TAG, "Service Aborted with Otherwise Uncaught Exception." );
		} finally {
			
			APRDebug.APRLog( TAG, "Service Stopped." );
			
			stopSelf();
		}
 	}
	
	private void UpdateAppList() 
	{
    	// instantiate the installed_packages class if not done already.
		if ( installed_packages == null )
		{
			installed_packages = new APRInstalledPackages( mContext );
		}
		
		// CreateAppList is intended to be run every time we launch
		//   the APR Service.  
		//installed_packages.CreateAppList();
	}

	// propagate some preferences values throughout the phone.
	private boolean APRServiceSetPrefs() {
   		boolean return_result = false;
   		
   		try {
   			// enforce ordering.  if AprManagerInstance hasn't been
   			//   initialized yet, we will fail.
   			if ( AprManagerInstance != null )
   			{
   				boolean run_fast = APRPreferences.APRPrefsGetRunFast();
   				String carrier_name = APRPreferences.APRPrefsGetCarrierName();
   				   				   				
   				CARRIER carrier_type = CARRIER.fromLabel(carrier_name);
				APRDebug.APRLog( TAG, "APRServiceSetPrefs carrier_name : " + carrier_name);
   				
   				// ensure that we're running at the right speed.
   				AprManagerInstance.periodic.mail.APRMessagingSetParameters( run_fast, carrier_type );
   				AprManagerInstance.panics.mail.APRMessagingSetParameters(run_fast, carrier_type );
   				AprManagerInstance.swver.mail.APRMessagingSetParameters(run_fast, carrier_type );
   			
   				return_result = true;
   			}
   		} catch ( Exception e ) {
   			APRDebug.APRDebugStack(e);
   		}
   		
   		return( return_result );
	}
	
	// Check the failure rate, and disable APR if it is failing too much.
	private void CheckFailureRate() {
		
		long panic_count = APRPreferences.APRPrefsGetPanicCount( true ) + APRPreferences.APRPrefsGetPanicCount( false );
		long uptime_count = APRPreferences.APRPrefsGetTotalUptimeElapsed()/(1000 * 60 * 60 );
		long panic_rate = 0;
		
		if ( uptime_count > 0 ) {
			panic_rate = panic_count/uptime_count;
		} else {
			panic_rate = panic_count;
		}
		
		if ( panic_rate > 100 ) {
			APRPreferences.APRPrefsSetEnabledState(false);
			Log.e( TAG, "Panic Rate Too High, Disabling APR.");
		}
	}
	
	// restart the service while changing the enabled state.
	public static void APRServiceRestart( boolean enabled_state ) {

		APRDebug.APRLog( TAG, "APRServiceRestart Executed" );
		
		// We're enabling (or disabling) the APR application.
		//   indicate that initial scanning is NOT complete.
		//   It will not be complete until we do a full scan
		//   THAT happens in the APRManager class.
		APRPreferences.APRPrefsSetInitialScanComplete( false );

		APRPreferences.APRPrefsSetEnabledState( enabled_state );
						
		// Do the heavy work in the service thread.
		APRServiceRelaunchDelayed( ACTION_RELAUNCH_DELAYED );
	}
	
	// restart the service, without changing the enabled state.
	public static void APRServiceRestart() {
		
		APRDebug.APRLog( TAG, "APRServiceRestart Executed" );

		// We're enabling (or disabling) the APR application.
		//   indicate that initial scanning is NOT complete.
		//   It will not be complete until we do a full scan
		//   THAT happens in the APRManager class.
		APRPreferences.APRPrefsSetInitialScanComplete( false );
						
		// Do the heavy work in the service thread.
		APRServiceRelaunchDelayed( ACTION_RELAUNCH_DELAYED );
	}
   	
	/**
	 * This is a critical API for divorcing the APP layer
	 *   from the service layer.
	 */
	static public void APRServiceRelaunchDelayed( String IntentString ) 
	{
        long nextUpdate = android.os.SystemClock.elapsedRealtime() + 10;

        // Create an intent that will cause the service to re-launch
        Intent updateIntent = new Intent( IntentString );
        updateIntent.setClass(mContext, APRService.class);

        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, updateIntent, 0);

        // Schedule alarm, and force the device awake for this update with this intent
        AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextUpdate, pendingIntent);
	}

	/**
	 *  Class Destruction routine (sortof).  There is no 
   	 *   destructor routine for JAVA, but finalize will be
   	 *   called at the exit of this service. 
	 */
	@Override
   	protected void finalize() throws Throwable {
   		try {
   			
   		} finally {
   			super.finalize();
   		}
   	} // end finalize attempt.
	

}
