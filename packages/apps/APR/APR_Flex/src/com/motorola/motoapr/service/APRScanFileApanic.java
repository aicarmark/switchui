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

public class APRScanFileApanic extends APRScanFile {

	static String TAG = "APRScanFileApanic";
	
	// important to include the space
	static String PROGRAM_COUNTER_STRING = "PC is at ";
	static String KERNEL_SYNC_STRING = "Kernel panic - not syncing: ";
	
	byte[] kernel_crash_log = new byte[ APR_DEFAULT_LOG_SIZE ];
		
	APRScanFileApanic (APRManager apr_manager) {
		super(apr_manager);
	}

	String debug = null;
	
	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		long current_last_mod      = 0;
		String crash_string = null;
		boolean         kernel_crash_reported = false;
		
		try 
		{
			// can we read the file?
			if ( file_ptr.canRead() )
			{
				APRDebug.APRLog( TAG, "File Found:" + FileToScan );

				current_last_mod = file_ptr.lastModified(); 
				
				if ( current_last_mod > APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) ) {
																						
					try 
					{							
						BufferedReader inReader = new BufferedReader(new FileReader( FileToScan ), 1024 );
						
						// read lines from the file until we find the first thread that's running.
						//   don't know if we need to look at all threads...
						String read_string = null;
						
						while( ( read_string = inReader.readLine() ) != null )
						{
							if ( read_string.contains( PROGRAM_COUNTER_STRING ) )
							{	
								
								int index = 0;
								String program_counter_text = null;
								
								try {
									
									index = read_string.lastIndexOf( PROGRAM_COUNTER_STRING ) + PROGRAM_COUNTER_STRING.length();
									program_counter_text = new String( read_string.substring( index ) );
																		
								} catch ( Exception e ) {
									
									APRDebug.APRLog( TAG, "Failed to Grab PC from Kernel Console." );
									
								} finally {
									
									if ( program_counter_text != null )
									{
										system_apr_manager.APRMgrStoreEntry( FileToScan, panic_id, panic_id.id, 0, program_counter_text.getBytes(), current_last_mod, program_counter_text );
										kernel_crash_reported = true;
									}
								}
								
								break;
								
							} // end if line contains "PC is at "
							else
							{
								if ( read_string.contains( KERNEL_SYNC_STRING) )
								{
									
									int index = 0;
									String kernel_sync_text = null;

									try {
										
										index = read_string.lastIndexOf( KERNEL_SYNC_STRING ) + KERNEL_SYNC_STRING.length();
										kernel_sync_text = new String( read_string.substring( index ) );
																			
									} catch ( Exception e ) {
										
										APRDebug.APRLog( TAG, "Failed to Grab PC from Kernel Console." );
										
									} finally {
										
										if ( kernel_sync_text != null )
										{
											system_apr_manager.APRMgrStoreEntry( FileToScan, panic_id, panic_id.id, 0, kernel_sync_text.getBytes(), current_last_mod, kernel_sync_text );
											kernel_crash_reported = true;
										}
									}
									
									break;

								}
							}
							
						}  // end while lines left in apainic_threads to scan.
						
					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
					} finally {	

						APRDebug.APRLog( TAG, "APRScanFileApanic kernel_crash_reported " + kernel_crash_reported );
					
						if ( !kernel_crash_reported )
						{					
							// because we are in this file, we know that we had a log for this panic.
							//   don't call it a kernel panic with no data.  
							APRScanRecordAndSend( FileToScan, panic_id, "LOGAVAIL", current_last_mod, "LOGAVAIL", "LOGAVAIL" );
						
							kernel_crash_reported = true;
						}			
											
					// record the timestamp for this file, so we don't send again.
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
					}
					
				} // end if timestamp for the file is valid.  this means we have a log, and it is valid.
				
			} // end if file can be read.
			
		} catch ( Exception e ) 
		{
		    APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		} finally {
			
            // check up on our boot info, and compare what is there
            //   versus what was discovered in this function.  if we 
            //   didn't report in... report. but in either case, don't
            //   allow reporting based upon bootinfo again.
            if ( APRScanFileBootInfo.pu_reason_happened( PU_REASON_TYPE.AP_KERNEL_PANIC ) )
            {                               
                    if ( kernel_crash_reported )
                    {                                       
                    	// indicate that the kernel crash was reported, and should not be reported again
                    	//   (as the result of looking at bootinfo).
                    	APRScanFileBootInfo.pu_reason_report( PU_REASON_TYPE.AP_KERNEL_PANIC );
                    }                       
            }
	 }
	} // end APRScanFileApanic.
}
