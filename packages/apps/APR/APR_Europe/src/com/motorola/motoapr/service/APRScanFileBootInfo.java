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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.motorola.motoapr.service.PanicType.PANIC_ID;
import com.motorola.motoapr.service.PuReasonType.PU_REASON_TYPE;

public class APRScanFileBootInfo extends APRScanFile {

	static final String TAG="APRScanFileBootInfo";
	static int BOOT_INFO_READ_SIZE=100;
	static final String POWERUP_REASON_STRING="POWERUPREASON : 0x" ;
	
	static long powerup_reason = 0;
	static boolean startup_received = false;
	static boolean reported_failure_on_boot = false;
	static boolean reported_poweron = true;

	static long current_last_mod = 0;
	static String file_scanned = "BOOTINFO";

	static long INVALID_BOOTINFO=0xFFFFFFFF;
	
	APRScanFileBootInfo(APRManager apr_manager) {
		super(apr_manager);

		// scan boot info upon creation of this class.
		// store the information for use in the accessor APIs
	}

	// this function must only be called once per phone powerup
	//   therefore it is limited by the API As called from the
	//   StartupIntentReceiver.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		// only do once per powerup... 		
		if ( startup_received )
		{
			File    file_ptr = new File( FileToScan );
			file_scanned = new String( FileToScan );
			
			try 
			{
				// can we read the file?
				if ( file_ptr.canRead() )
				{
					APRDebug.APRLog( TAG, "File Found:" + FileToScan );

					current_last_mod = file_ptr.lastModified(); 
					
					// keep track of the last time this file was modified.  if the modification
					//   date of the file is older than what we have stored, or we have nothing stored,
					//   then send the file and keep track of what was sent.
					if ( current_last_mod > APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) ) {
								
						try 
						{							
							BufferedReader inReader = new BufferedReader(new FileReader( FileToScan ), 1024);
						
							String powerup_reason_line = null;

							while( ( powerup_reason_line = inReader.readLine() ) != null )
							{
								if ( powerup_reason_line.contains( POWERUP_REASON_STRING ) )
								{
									int index = powerup_reason_line.lastIndexOf( POWERUP_REASON_STRING ) + POWERUP_REASON_STRING.length();

									String powerup_reason_str = powerup_reason_line.substring( index );

									powerup_reason = Long.parseLong(powerup_reason_str,16);
									APRDebug.APRLog( TAG, "cm: Found boot flag:" + POWERUP_REASON_STRING + " " + powerup_reason);
		

									if ( powerup_reason == INVALID_BOOTINFO )
									{
										powerup_reason = 0;
									}

									// we're done.
									break;
								}
							}
						} catch ( IOException e ) {
							APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
						} catch ( Exception e ) {
							APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
						}

						APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
					}
				}
				
			} catch ( Exception e ) 
			{
			    APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
			}
			
			startup_received = false;
			
			APRPreferences.APRPrefsPhoneRestart();

		} // end if valid startup signal received.
	}
	
	// routine to send panic messages for anything that is critical, 
	//   that has been uncaught by any other panic scanner.
	public void BootInfoSendUncaughtPanics()
	{
		PANIC_ID panic_id = null;
		int dsp_version = 0;
		byte[] crash_log = PU_REASON_TYPE.NO_PU_REASON_TYPE1.label.getBytes();
		boolean local_reported_failure_on_boot = reported_failure_on_boot;
		
		// loop through each of the pu reason types.
		for ( PU_REASON_TYPE pu_reason : PU_REASON_TYPE.values() ) {
			crash_log = pu_reason.label.getBytes();
			panic_id = PANIC_ID.KERNEL_APANIC_ND;
			
			// determine whether this reason happened
			if ( pu_reason_happened( pu_reason ) ) {
				
				// determine if we are/should report this reason as a panic.
				if ( pu_reason_report( pu_reason ) )
				{
					if ( !local_reported_failure_on_boot ) {
						// if we haven't reported a crash for this powerup already,
						//   report one now.
						system_apr_manager.APRMgrStoreEntry( file_scanned, panic_id, panic_id.id, dsp_version, crash_log, current_last_mod, pu_reason.label );
						local_reported_failure_on_boot = true;
						
						APRDebug.APRLog( TAG, "PANIC Recorded: " + panic_id.label + ":" + pu_reason.label ); 
					}
				} else {

					// if we haven't yet reported a normal powerdown
					//    check if we should report this as a normal powerdown
					if ( !reported_poweron ) {

						// we had a poweron reason, but it's not something we report
						//   about.  indicate a normal poweron to the APR Server.
						//   change the poweron reason.
						panic_id = PANIC_ID.NORMAL_POWERDOWN;

						system_apr_manager.APRMgrStoreEntry( file_scanned, panic_id, panic_id.id, dsp_version, crash_log, current_last_mod, pu_reason.label );					
						reported_poweron = true;
						
						APRDebug.APRLog( TAG, "POWERDOWN Recorded: " + panic_id.label + ":" + pu_reason.label ); 
					}
				}
			}
		}
		
		if ( ( !local_reported_failure_on_boot ) && ( !reported_poweron ) ) {
			// we had a restart, but we didn't report a crash.
			// indicate that to APR.
			
			APRDebug.APRLog( TAG, "NO BOOT REASON: PANIC Recorded: " + PANIC_ID.KERNEL_APANIC_ND.label ); 
			system_apr_manager.APRMgrStoreEntry( file_scanned, PANIC_ID.KERNEL_APANIC_ND, PANIC_ID.KERNEL_APANIC_ND.id, dsp_version, crash_log, current_last_mod, "CPCAP_WATCH_DOG" );
		}

		local_reported_failure_on_boot = true;
		reported_failure_on_boot = true;
		reported_poweron = true;
	}

	static public void StarupReceived()
	{
		startup_received = true;
	}
	
	/*
	 * Get the raw powerup reason.
	 */
	static long GetRawPowerupReason()
	{
		return( powerup_reason );
	}
	
    static boolean pu_reason_happened( PU_REASON_TYPE this_reason_type ) 
    {
    	return PuReasonType.matches( this_reason_type, powerup_reason );
    }
    
    // clears out the bit, and returns whether this can/should be reported.
    static boolean pu_reason_report( PU_REASON_TYPE this_reason_type ) 
    {
		powerup_reason &= (~this_reason_type.value );
		
		// if we've had a reportable failure, don't report again.
		if ( this_reason_type.reportable ) {
			reported_failure_on_boot = true;
		}
		
		return( this_reason_type.reportable );
    }	
}
