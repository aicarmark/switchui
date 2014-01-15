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

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanFileTombstone extends APRScanFile {

	static String TAG = "APRScanFileTombstone";
	
	APRScanFileTombstone(APRManager apr_manager) {
		super(apr_manager);
	}

	byte[] tombstone_crash_log = new byte[ APR_DEFAULT_LOG_SIZE ];
		
	static String start_process_name = ">>> ";
	static String end_process_name = " <<<";
	static String slash = "/";
	static String start_fault_addr = "fault addr ";
	static String end_fault_addr = ",";
	static String start_pid_mark = "pid: ";
	static String end_pid_mark = ",";
	static String start_pc_marker = "pc ";
	static String end_pc_marker = "cpsr"; 

	
	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		long current_last_mod      = 0;
		
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
				
					if ( FileToScan.contains( "tombstone_" )) 
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
							String app_name_str = null;
							String full_app_name_str = null;
							String pid_str = null;
							String fault_addr_str = null;
							String pc_value_str = null;
							String panic_data_str = null;
						
							// set the array to null each time through.
							java.util.Arrays.fill( tombstone_crash_log, (byte) 0);
						
							while( ( process_info = inReader.readLine() ) != null )
							{
								if ( process_info.contains( start_pid_mark ) )
								{	
									int start_pid= process_info.lastIndexOf( start_pid_mark ) + start_pid_mark.length();
									int end_pid = process_info.lastIndexOf( end_pid_mark );
									pid_str = process_info.substring( start_pid, end_pid );
							
									int start_name = process_info.lastIndexOf( start_process_name ) + start_process_name.length();
									String process_name = process_info.substring( start_name );
									int end_time = process_name.lastIndexOf( end_process_name );
									app_name_str = process_name.substring( 0, end_time );
									
									// make a copy of the full app_name_str, before we mess around with it.
									full_app_name_str = new String( app_name_str );
									
									// truncate the name to the last characters
									if ( app_name_str.contains( slash )) 
									{
										int executable_index = app_name_str.lastIndexOf( slash ) + 1;
										app_name_str = app_name_str.substring( executable_index );
									}
									
									// reverse the name if it contains a .
									if ( app_name_str.contains( "." ))
									{
										app_name_str = ReverseName( app_name_str, '.' );
									}

									// force the string to 16 chars.
									app_name_str = ForceStringLength( app_name_str, 16 );																	
								} 
								else if ( process_info.contains( start_fault_addr ) ) 
								{
									int start_fault = process_info.lastIndexOf( start_fault_addr ) + start_fault_addr.length();
									fault_addr_str = process_info.substring( start_fault );
									
								} // end if line contains "fault addr"
								else if ( process_info.contains( end_pc_marker ) )
								{
									int start_pc = process_info.lastIndexOf( start_pc_marker ) + start_pc_marker.length();
									int end_pc = process_info.lastIndexOf( end_pc_marker );
									pc_value_str = process_info.substring( start_pc, end_pc );
								}
							
								if ( ( app_name_str != null ) &&
										( fault_addr_str != null ) &&
										( pc_value_str != null ) && 
										( pid_str != null ))
								{
									// abort the while loop, we have everything we need.
									break;
								}
							
							}  // end while lines left in the tombstone file to scan.
						
							// ok, well we know we have a tombstone, so we must send some data.  
							//   determine if app_name has been identified, and start building the
							//   byte array.
						
							// app name goes first.
							if ( app_name_str != null )
							{
								// 8 chars only.
								System.arraycopy( app_name_str.getBytes(), 0, tombstone_crash_log, 0, 16);
								panic_data_str = "(process_name)" + full_app_name_str;
							}
							else
							{
								System.arraycopy( "Unknown".getBytes(), 0, tombstone_crash_log, 0, 16);
								panic_data_str = "(process_name)unknown";
							}
						
							// program counter is second.
							int pc_value = 0;
						
							if ( pc_value_str != null )
							{
								pc_value = HexStringToIntConversion( pc_value_str );
								panic_data_str += " (pc)" + pc_value_str;
							}
							byte[] pc_array = intToByteArray( pc_value, true );
							System.arraycopy( pc_array, 0, tombstone_crash_log, 16, 4);
						
							// fault_address is third.
							int fault_address = 0;
						
							if ( fault_addr_str != null )
							{
								fault_address = HexStringToIntConversion( fault_addr_str );
								panic_data_str += " (fault_addr)" + fault_addr_str;
							}
						
							byte[] fault_array = intToByteArray( fault_address, true );
							System.arraycopy( fault_array, 0, tombstone_crash_log, 20, 4);

							// process id is fourth
							int process_id = 0;
						
							if ( pid_str != null )
							{
								process_id = HexStringToIntConversion( pid_str );
								panic_data_str += " (pid)" + pid_str;
							}
						
							byte[] pid_array = intToByteArray( process_id, true );
							System.arraycopy( pid_array, 0, tombstone_crash_log, 24, 4);
						
							// if this is not a user installed application (doesn't match
							//   the name of an entry installed after first time power-on)
							//   then store the crash.  Otherwise, filter it out.
							if ( !APRInstalledPackages.IsUserInstalledApp( full_app_name_str ) )
							{							
								system_apr_manager.APRMgrStoreEntry( FileToScan, panic_id, panic_id.id, 
										0, tombstone_crash_log, current_last_mod, panic_data_str);
							}
													
						} catch ( IOException e ) {
							APRDebug.APRLog( TAG, "Can't read data from: " + FileToScan );
							// log the crash anyways.  just because we cannot
							//  read the file, we know at this point that the
							//  timestamp has been updated, and we should be 
							//  indicating a failure.  But in this case the
							//  data sent will be blank.
						} catch ( Exception e ) {
							APRDebug.APRLog( TAG, "Failure Scanning: " + FileToScan );
						}
						finally {
							APRDebug.APRLog( TAG, "Tombstone Scanned:" + FileToScan );
						}
					
					} // end if this is actually a tombstone file.
					
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );

					} // end if the file has a newer timestamp than what was recorded.

			} // end if we can read the file.
				
		} catch ( Exception e ) 
		{
			APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
			
		} finally {
			APRDebug.APRLog( TAG, "APRScanFileTombstone. ScanFile Done." );
		}
		
	} // end ScanFile.
	
} // end APRScanFileTombstone class.


