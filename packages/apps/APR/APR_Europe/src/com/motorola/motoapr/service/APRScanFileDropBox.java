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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.util.Log;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanFileDropBox extends APRScanFile {

	static String TAG = "APRScanFileDropBox";
	static String FORCE_CLOSE = "system_app_crash";
	
	APRScanFileDropBox(APRManager apr_manager) {
		super(apr_manager);
	}

	static String Process_name         = "Process: ";
	static String Package_name         = "Package: ";
	static String Exception_name       = "Exception: ";
	static String Unknown_name         = "com.unknown.Unknown";
	
	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		long current_last_mod      = 0;
		
		try 
		{
			// Is a Dropbox force close file?
			if(!FileToScan.contains(FORCE_CLOSE)) {
			//	APRDebug.APRLog( TAG, FileToScan + " is not a force close, pass!");
				return;
			}
			
			// can we read the file?
			if ( file_ptr.canRead() )
			{
				APRDebug.APRLog( TAG, "File Found:" + FileToScan );
				
				current_last_mod = file_ptr.lastModified(); 
				
				// keep track of the last time this file was modified.  if the modification
				//   date of the file is older than what we have stored, or we have nothing stored,
				//   then send the file and keep track of what was sent.
				if ( current_last_mod > APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) ) {
				
					if ( FileToScan.contains( FORCE_CLOSE )) 
					{
						// the file has been modified.  We don't know if it is a new instance of the file
						//   (destroyed and rewritten) or an existing instance that's been appended.  
						//   therefore we must back it up in its entirety.
						APRScanBackupFile( FileToScan );
					
						// Rules for finding crashes.
					
						// If two entries in the log have the same timestamp then neither entry
						//   is a valid crash in-itself.  those entries are part of an ANR, and we
						//   must continue to scan all entries that are part of the ANR, until we 
						//   find a unique timestamp.  For that ANR, we must record a single crash.
						
						// Otherwise, if the entry has its own unique timestamp, we do not have
						//   to record an ANR.  We can simply record a crash, and note the app name
						//   and timestamp.
																		
						try 
						{							
							BufferedReader inReader = new BufferedReader(new FileReader( FileToScan ), 1024);
						
							// read a line from the file, see if it has a new
							//   crash.
							String process_info = null;
							String process_name = null;
							String package_name = null;
							String exception_name = null;
							String app_name_str = null;
							String crash_string = null;
						
							// Retrieve creation timestamp from filename
							String filename = file_ptr.getName();
							int time_created_start = filename.lastIndexOf("@") + 1;
							int time_created_end = filename.lastIndexOf(".");
							String time_created = filename.substring(time_created_start, time_created_end);
							APRDebug.APRLog( TAG, "time_created:" + time_created );
							
							// Calculate timestamp
	

							long inTimeInMillis = Long.parseLong(time_created);
							SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
							String time_only = formatter.format(new Date(inTimeInMillis));
							APRDebug.APRLog( TAG, "time_only:" + time_only );
							
							crash_string = new String(time_only);
						
							while( ( process_info = inReader.readLine() ) != null )
							{
								if ( process_info.contains( Process_name ) )
								{										
									int process_name_start = process_info.lastIndexOf( Process_name ) + Process_name.length();
									process_name = process_info.substring(process_name_start);
									APRDebug.APRLog( TAG, "process_name:" + process_name );
								} 
								else if ( process_info.contains( Package_name ) ) 
								{
									int package_name_start = process_info.lastIndexOf( Package_name ) + Package_name.length();
									package_name = process_info.substring( package_name_start );									
									APRDebug.APRLog( TAG, "package_name:" + package_name );
								}
								else if ( process_info.contains( Exception_name ) )
								{
									int exception_name_end = process_info.indexOf(":");
									exception_name = process_info.substring(0, exception_name_end);
									APRDebug.APRLog( TAG, "exception_name:" + exception_name );
									
									/**
									 * 
									int package_name_start = process_info.lastIndexOf( "/" );
									int package_name_end  = process_info.indexOf(")");
									if(package_name_start != -1 && package_name_end != -1) {										
										full_app_name_str = process_info.substring(package_name_start, package_name_end);
									}
									APRDebug.APRLog( TAG, "full_app_name_str:" + full_app_name_str );
									*/
									
									break;
								}
							}
							
							/**
							 * Our DropBox report policy:
							 * Package name sometimes contains extra characters,
							 * So when process name is available, we use process name
							 * instead of package name. 
							 * Use package name when process name is absent
							 */
							
							if(process_name == null) {
								APRDebug.APRLog( TAG, "process_name is null, use package name");
								app_name_str = package_name;
							} else {
								app_name_str = process_name;
							}
							// If we still don't know which process crashed, we need to 
							// give it a dummy name
							if(app_name_str == null) {
								Log.e(TAG, "app_name_str still has no name, use unknown");
								app_name_str = Unknown_name;
							}
							APRDebug.APRLog( TAG, "app_name_str:" + app_name_str);
							
							// Reverse the app name.
							app_name_str = ReverseName( app_name_str, '.' );
							
							// take the new app name, and force it to 24 chars.
							app_name_str = ForceStringLength( app_name_str, 24 );
							APRDebug.APRLog( TAG, "ReverseName and ForceLength:" + app_name_str);
							
							crash_string = new String( app_name_str + " " + crash_string );
						
							// ok, well we know we have a tombstone, so we must send some data.  
							//   determine if app_name has been identified, and start building the
							//   byte array.
						
							APRScanRecordAndSend( filename, panic_id, app_name_str, inTimeInMillis, crash_string, "(app_name)" + app_name_str );
													
						} catch ( IOException e ) {
							APRDebug.APRLog( TAG, "Can't read data from: " + FileToScan );
							// log the crash anyways.  just because we cannot
							//  read the file, we know at this point that the
							//  timestamp has been updated, and we should be 
							//  indicating a failure.  But in this case the
							//  data sent will be blank.
						} catch ( Exception e ) {
							APRDebug.APRLog( TAG, "Failure Scanning: " + FileToScan );
							Log.e( TAG, "Failure Scanning: " + FileToScan );
							APRDebug.APRDebugStack(e);
						}
						finally {
							APRDebug.APRLog( TAG, "DropBox Scanned:" + FileToScan );
						}
					
					} // end if this is actually a tombstone file.
					
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );

					} // end if the file has a newer timestamp than what was recorded.

			} // end if we can read the file.
				
		} catch ( Exception e ) 
		{
			APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
			
		} finally {
			//APRDebug.APRLog( TAG, "APRScanFileDropBox. ScanFile Done." );
		}
		
	} // end ScanFile.
} // end APRScanFileDropBox class.


