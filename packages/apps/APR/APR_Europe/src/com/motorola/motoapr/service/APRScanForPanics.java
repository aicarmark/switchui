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

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanForPanics {
	
	static final String TAG="APRScanForPanics";

	private static final Exception APRInitError = new Exception();
	
	static APRManager system_apr_manager = null;
	static boolean scanner_running = false;
	
	APRScanFile      default_scanner       = null;
	APRScanFileJava     java_scanner       = null;
	APRScanFileBP         bp_scanner       = null;
	APRScanFileKernel kernel_scanner       = null;
	APRScanFileTombstone tombstone_scanner = null;
	APRScanFileBootInfo  boot_info_scanner = null;
	APRScanFileApanic apanic_scanner       = null;
	APRScanFileDropBox dropbox_scanner     = null;
//	APRScanFileLogcatLog log_scanner       = null;
//	APRScanFileMDM6600 mdm6600_scanner     = null;
	
	// ftp package list
	APRBuildFTPPackage    ftp_package_list = null; 
	   	
	APRScanForPanics( APRManager apr_manager ) throws Exception {
		
		if ( apr_manager != null ) 
		{
			system_apr_manager = apr_manager;
			default_scanner    = new APRScanFile( apr_manager );
			boot_info_scanner  = new APRScanFileBootInfo( apr_manager );
			kernel_scanner     = new APRScanFileKernel( apr_manager );
			apanic_scanner     = new APRScanFileApanic( apr_manager );
			bp_scanner         = new APRScanFileBP( apr_manager );
			tombstone_scanner  = new APRScanFileTombstone( apr_manager );
			java_scanner       = new APRScanFileJava( apr_manager );
			dropbox_scanner    = new APRScanFileDropBox( apr_manager ); 
//			log_scanner        = new APRScanFileLogcatLog( apr_manager );
//			mdm6600_scanner    = new APRScanFileMDM6600( apr_manager );
		}
		else
		{
			throw APRInitError;
		}
				
		return;
	}

	// Scan each directory where content is supposed to be 
	//   and scan it for content.  This content will be the
	//   crash logs in the system (e.g. BP Panic Crash Logs).
	public void APRScanFileSystem() {
		// takes too long on some products.  dislocate with the main thread.
		
		if ( scanner_running == false ) {
			scanner_running = true;
		
			APRDebug.APRLog(TAG, "Scan File System Requested, spawning thread.");

			new Thread( new Runnable() {
				
				public void run () {
					
					try {
						
						APRDebug.APRLog(TAG, "Scan File System Thread Running....");
						
						// always create a new package list.  
						//   that way, the process of sending the package list
						//   is always unique as well.  
						ftp_package_list   = new APRBuildFTPPackage( system_apr_manager.mContext );
						
						for ( PANIC_ID panic_id : PANIC_ID.values() ) {
							APRScanDirectory( panic_id, null );
						}
						
						// After scanning the file system, check to ensure we've
						//   handled any otherwise uncaught panics 
						//  ( powercut, hardware watchdog, kernel).
						boot_info_scanner.BootInfoSendUncaughtPanics();
						
						// send the report to the ftp server.
						//ftp_package_list.report_send();

					} finally {
						APRDebug.APRLog(TAG, "Scan File System thread completed.");
						scanner_running = false;
					}
				}
			}).start();

		} else {
			APRDebug.APRLog(TAG, "Scan File System in progress, additional panics will be caught in subsequent scans.");
		}
	}
	
	// Check if entry is a directory, and scan each of its entries.
	//   otherwise, treat the incoming entry as a file.
	private void APRScanDirectory( PANIC_ID panic_id, String this_dir_name )
	{
		try 
		{				
			String directory_to_scan = null;
			
			if ( this_dir_name == null ) 
			{
				directory_to_scan = new String( panic_id.dir_location );	
			}
			else
			{
				directory_to_scan = this_dir_name;
			}
			
			File log_area = new File( directory_to_scan );
			
			if ( log_area.isDirectory() ) 
			{
				// this is a directory.  find the sub entries.
				String dir_entries[] = log_area.list();
				
				if ( dir_entries != null ) {
					for ( int j=0; j< dir_entries.length; j++ )
					{
						String this_entry_name = directory_to_scan + 
												"/" + 
												dir_entries[j]; 
						
						File this_entry = new File( this_entry_name );
						
						if ( this_entry.isDirectory() )
						{
							APRScanDirectory( panic_id, this_entry_name );
						}
						else
						{
							APRScanFile( panic_id, this_entry_name );
						}
					}
				}
			}
			else if ( log_area.exists() )
			{
				// this is already a file.  scan it.
				APRScanFile( panic_id, panic_id.dir_location);
			}
			else
			{
				APRDebug.APRLog( TAG, "Nothing for " + panic_id.dir_location );
			}
				
		} catch ( Exception e ) {
		    APRDebug.APRLog( TAG, "Error during APR File System Scan:" + 
			   panic_id.dir_location );
		}
	}
	
	private void APRScanFile( PANIC_ID panic_id, String FileToScan ) 
	{	
		APRDebug.APRLog( TAG, "Scanning: " + FileToScan + " For Panic ID: " + panic_id.toString() );
		
		if ( panic_id == PANIC_ID.BOOT_INFO ) {
			boot_info_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.KERNEL_CRASH ) {
			kernel_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.KERNEL_APANIC ) {
			apanic_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.LINUX_USER_SPACE_CRASH ) {
			tombstone_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.JAVA_APP_CRASH ) {
			java_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.DROPBOX_CRASH ) {
			dropbox_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.MODEM_CRASH ) {
			bp_scanner.ScanFile( panic_id, FileToScan );	
			return;
		}
		if ( panic_id == PANIC_ID.MODEM_CRASH1 ) {
			bp_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
		if ( panic_id == PANIC_ID.MODEM_CRASH2 ) {
			bp_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
/*		if ( panic_id == PANIC_ID.MODEM_CRASH3 ) {
			mdm6600_scanner.ScanFile( panic_id, FileToScan );
			return;
		}
*/
		// by default, do this.  Requires each of the
		//  above statements to return prior to coming
		//  here.
		default_scanner.ScanFile( panic_id, FileToScan );

		return;
	}
}
