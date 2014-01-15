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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanFileBP extends APRScanFile {
	
	static String TAG = "APRScanFileBP";
	
	static int APR_SCAN_FOR_PANICS_READ_SIZE = 200;
		
	APRScanFileBP(APRManager apr_manager) {
		super(apr_manager);
	}

	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		FileInputStream file_ioptr = null;
		long current_last_mod      = 0;
		int bp_panic_id = 0;
		int dsp_version = 0;
		String panic_data_str = null;
		
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
//				if ( current_last_mod > APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) ) {
				if ( APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) == 0 ) {
							
					// get the io ptr
					file_ioptr = new FileInputStream( FileToScan );
				
					byte crash_log[] = new byte[ APR_SCAN_FOR_PANICS_READ_SIZE ];
					
					try 
					{	
						// read up to 
						int file_read = file_ioptr.read( crash_log, 0, APR_SCAN_FOR_PANICS_READ_SIZE );
						
						APRDebug.APRLog( TAG, "Read " + file_read + " bytes." );
						
						file_ioptr.close();
						
					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
						// log the crash anyways.  just because we cannot
						//  read the file, we know at this point that the
						//  timestamp has been updated, and we should be 
						//  indicating a failure.  But in this case the
						//  data sent will be blank.
					} catch ( Exception e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
					}
								
					// Determine the marker and therefore the technology type.
					String marker = MarkerFromRaw( crash_log );
					
					// Before adding a new technology type here, 
					//   you must create a new class and class file
					//   based upon the class APRScanFileBPPanicDefault.
					if ( marker.contains( "UMTS" ))
					{
						APRScanFileBPUMTS panic_scan = new APRScanFileBPUMTS( crash_log );
				//		bp_panic_id = panic_scan.GetPanicID();
						crash_log = panic_scan.GetRawData();
						dsp_version = panic_scan.GetDSPVersion();
					}
					else if ( marker.contains( "CDMA" ))
					{
						APRScanFileBPCDMA panic_scan = new APRScanFileBPCDMA( crash_log );
					//	bp_panic_id = panic_scan.GetPanicID();
						crash_log = panic_scan.GetRawData();
						dsp_version = panic_scan.GetDSPVersion();
					}
					else
					{
						// no technology type was identified.  just do some default
						//   scanning  and hope for the best.
						APRScanFileBPPanicDefault panic_scan = new APRScanFileBPPanicDefault( crash_log );
							bp_panic_id = panic_scan.GetPanicID();
						
						crash_log = panic_scan.GetRawData();
						dsp_version = panic_scan.GetDSPVersion();
					}
				
								
					if ( crash_log == null )
					{
						crash_log = "BPPanicScanFailed".getBytes();
						panic_data_str = "BPPanicScanFailed";
					}
					// the timestamp is the last time this file was modified.
					//   by default the id to be used is taken from the PANIC_ID 
					//   enumeration.
					system_apr_manager.APRMgrStoreEntry( FileToScan, panic_id, panic_id.id, dsp_version, crash_log, current_last_mod, panic_data_str ); 
					Log.d(TAG, "ModemCrash file: " + FileToScan + " Crash Text: " + panic_data_str + " " + current_last_mod );
			
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
				}
			}
			
		} catch ( Exception e ) 
		{
		    APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		}
	}
	
	private String MarkerFromRaw( byte[] crash_log ) 
	{
		int APR_BP_PANIC_MARKER_LENGTH=5;
		
		byte[] byte_marker = new byte[ APR_BP_PANIC_MARKER_LENGTH ];
		String return_marker = null; 
		
		for ( int i = 0; i < APR_BP_PANIC_MARKER_LENGTH - 1; i++ )
		{
			byte_marker[i]=crash_log[i];
		}
		byte_marker[ APR_BP_PANIC_MARKER_LENGTH-1 ]=0;
		
		return_marker = new String( byte_marker );
		
		return return_marker;
	}

	
	
}
