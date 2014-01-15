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
import android.text.format.Time;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanFileJava extends APRScanFile {

	static String TAG = "APRScanFileJava";
	
	static String ANR_TEXT = "ANR";
	
	// important to include the space
	static String JAVA_APP_NAME_START = "Cmd line: ";
	
	
	APRScanFileJava(APRManager apr_manager) {
		super(apr_manager);
		
	}

	String debug = null;
	
	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		long current_last_mod      = 0;
		String pid = null;
		String crash_string = null;
		String app_name = null;
		long crash_timestamp = 0; 
		
		String prev_crash_string = null;
		String prev_app_name = null;
		String prev_full_app_name = null;
		long prev_crash_timestamp = 0;
		
		try 
		{
			// APRDebug.APRDebugLogEnable(true);

			// can we read the file?
			if ( file_ptr.canRead() )
			{
				APRDebug.APRLog( TAG, "File Found:" + FileToScan );

				current_last_mod = file_ptr.lastModified(); 
				
				// keep track of the last time this file was modified.  if the modification
				//   date of the file is older than what we have stored, or we have nothing stored,
				//   then send the file and keep track of what was sent.
				if ( current_last_mod > APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) ) {
				
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
						BufferedReader inReader = new BufferedReader(new FileReader( FileToScan ));
						
						// read a line from the file, see if it has a new
						//   crash.
						String timestamp = null;
						
						while( ( timestamp = inReader.readLine() ) != null )
						{
							if ( timestamp.contains( "----- pid") )
							{	
								int start_pid= timestamp.lastIndexOf( "pid" ) + 3;
								int end_pid = timestamp.lastIndexOf( "at" ) - 1;
								pid = timestamp.substring( start_pid, end_pid );
							
								int start_time = timestamp.lastIndexOf( "at" ) + 2;
								String time_only = timestamp.substring( start_time );
								int end_time = time_only.lastIndexOf( "--" ) - 3;
								time_only = time_only.substring( 0, end_time );
							
								crash_string = new String( time_only + " " + pid );
														
								try {
									Time time = new Time();
									time.parse( time_only );
									crash_timestamp = time.normalize(false);
								} catch ( Exception e ) {
									crash_timestamp = HashTime(time_only);
								} catch ( UnsatisfiedLinkError e ) {
									crash_timestamp = HashTime(time_only);
								}
								
								String cmd_line = inReader.readLine();
								String new_app_name = null;
								String full_app_name = null;
								
								if ( cmd_line.contains( JAVA_APP_NAME_START ) ) 
								{
									int index = cmd_line.lastIndexOf( JAVA_APP_NAME_START );
							
									// skip the : and the space after it.
									app_name = cmd_line.substring( index + JAVA_APP_NAME_START.length() );
									
									full_app_name = new String( app_name );
									
									// Reverse the app name.
									app_name = ReverseName( app_name, '.' );
									
									// take the new app name, and force it to 24 chars.
									app_name = ForceStringLength( app_name, 24 );
									
									crash_string = new String( app_name + crash_string );
									
								} // end if second line contained "Cmd line";
								
								// if logging is on, log the new app name.
								APRDebug.APRLog( TAG, full_app_name );
								
								// at this point, crash_string, app_name, and crash_timestamp
								//   should ALL HAVE NEW VALUES for this loop iteration.
								
								// prev_* should have any old values, in distinct memory.

								// before we do anything else, check if there are multiple
								//  entries in this file with the same timestamp.
								if ( prev_crash_string != null ) 
								{
									long test_timestamp = 0;
									
									if ( prev_crash_timestamp > crash_timestamp )
									{
										test_timestamp = prev_crash_timestamp - crash_timestamp;
									}
									else 
									{
										test_timestamp = crash_timestamp - prev_crash_timestamp;
									}
									 
									APRDebug.APRLog( TAG, "Prev Timestamp: " + prev_crash_timestamp );
									APRDebug.APRLog( TAG, "This Timestamp: " + crash_timestamp );
									APRDebug.APRLog( TAG, "Delta         : " + test_timestamp );
									
									// if the crashes are within 10 seconds of each other, call it 
									//   an ANR.  But only if it's not the same app as before.  If it's a 
									//   cyclic crash of the same app, log it individually.
                                                                        if ( ( test_timestamp < 1000 ) && 
                                                                             ( prev_full_app_name != null ) && 
                                                                             ( full_app_name != null ) && 
                                                                             ( !prev_full_app_name.contains( full_app_name ) ) )
									{
										// this change the app name for the current thread
										//   we're looking at.  this will not get sent yet.
										app_name = ForceStringLength( ANR_TEXT, 8 );
										
                                                                                // we have an ANR.  Record the application name in the raw
                                                                                //   data, since we have a distinct panic type for ANR now.
                                                                                crash_string = new String( prev_app_name + time_only );

										APRDebug.APRLog( TAG, "APRScanFileJava     crash_string store    : " + crash_string );
										// this must be the only place we ever send an ANR Crash.
										if ( !prev_app_name.contains( app_name ) )
										{
											// only send ANR this time, if the previous one was not.
											JavaAppScanRecordAndSend( FileToScan, prev_full_app_name, PANIC_ID.JAVA_APP_CRASH_ANR, PANIC_ID.JAVA_APP_CRASH_ANR.id, app_name, crash_timestamp, crash_string );
											APRDebug.APRLog( TAG, "APRScanFileJava *******************    crash_string store    : " + ANR_TEXT + "from " + prev_app_name + " Detected @:" + crash_timestamp );											
										} // end if the previous entry was already noted as an ANR.
										else
										{
											APRDebug.APRLog( TAG, ANR_TEXT + " Detected, but previously sent:" + crash_timestamp );
										}
										
									} // end if this entry is not unique in the file.
									else
									{
										// we have a unique timestamp.  was the previous entry ANR?
										//   we had a very strange bug where prev_crash_string was null
										//   but we were allowed to get to this code.  
										if ( ( prev_app_name != null ) && ( !prev_app_name.contains( ANR_TEXT ) ) ) 
										{
											// the previous app name was not ANR, and
											//  it's timestamp is not equal to this entrie's
											//  timestamp.  Therefore, the previous entry was unique.  Send it.
											APRDebug.APRLog( TAG, "APRScanFileJava     2222222    : " + prev_crash_string );
											JavaAppScanRecordAndSend( null, prev_full_app_name, panic_id, panic_id.id, prev_app_name, prev_crash_timestamp, prev_crash_string );
										}
										else
										{
											// we'll get the "prev" values on the next go-around.  and
											//   we'll detect if it is an ANR, because the next timestamp
											//   will be equal to the prev_timestamp... which will be set
											//   below.
											// if this is the last entry in the traces.txt file, we'll get
											//   the prev values for that case too, after the while loop exits.
										}
									}
								} // end if prev crash string was not null.
								
								if ( ( crash_string != null ) &&
									 ( app_name != null ) &&
									 ( crash_timestamp != 0 ) )
								{
									// update the prev info, for the next loop.
									prev_crash_string = new String( crash_string );
									prev_app_name = new String( app_name );
									prev_crash_timestamp = crash_timestamp;
									prev_full_app_name = new String( full_app_name );
									
									// ensure we do not use these values again.
									crash_string = null;
									app_name = null;
									crash_timestamp = 0;
								}
								else
								{
									APRDebug.APRLog( TAG, "MAJOR ERROR.  We had a line with pid, but didn't get all the data!" );
								}
								
							} // end if line contains "pid"
							
						}  // end while lines left in traces.txt to scan.
						
						// there are no more entries in the file.
						
						// now, because the above loop delays sending of information, it becomes probable that 
						//  we have missed an entry.  in fact, the only exception is ANR, which sends the entry
						//  upon detection of the second ANR entry.  Therefore, we must check again, if the
						//  last thing we looked at was not an ANR, then it is an entry that should be sent.
						// we have a unique string.  was the previous entry ANR?
						if ( !prev_app_name.contains( ANR_TEXT ) ) 
						{
							// the previous app name was not ANR, and
							//  it's timestamp is not equal to this entrie's
							//  timestamp.  Therefore, the previous entry was unique.  Send it.
							JavaAppScanRecordAndSend( null, prev_full_app_name, panic_id, panic_id.id, prev_app_name, prev_crash_timestamp, prev_crash_string ); 
							
							// cleanup.  this is more for explanation than anything else.
							prev_crash_string = null;
							prev_app_name = null;
							prev_crash_timestamp = 0;
						}	
													
					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
						// log the crash anyways.  just because we cannot
						//  read the file, we know at this point that the
						//  timestamp has been updated, and we should be 
						//  indicating a failure.  But in this case the
						//  data sent will be blank.
					}			
											
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
					
				} // end if traces.txt file has been touched since the last time we looked at it.
				
			} // end if traces.txt file can be read.
			
		} catch ( Exception e ) 
		{
		    APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		} finally {
			
			//APRDebug.APRDebugLogEnable( false );
		}
		
		
	} // end APRScanFileJava.

	private void JavaAppScanRecordAndSend(
			String filename, 
			String full_app_name, 
			PANIC_ID panic_id,
			int id, 
			String app_name, 
			long crash_timestamp, String crash_string) {
		
		if ( APRInstalledPackages.IsFactoryInstalledApp(full_app_name) )
		{
			APRScanRecordAndSend( filename, panic_id, app_name, crash_timestamp, crash_string, "(app_name)" + full_app_name );
		}
	}

	
}
