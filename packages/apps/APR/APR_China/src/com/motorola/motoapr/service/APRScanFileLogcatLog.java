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

public class APRScanFileLogcatLog extends APRScanFile {

	static String TAG = "APRScanFileLogcatLog";
	
	APRScanFileLogcatLog(APRManager apr_manager) {
		super(apr_manager);
	}
	
	static String ANY_STRING = null;
	static String ANR_STRING = "ANR in";
	static String END_ANR = ",";
	static String APP_DEATH_STRING = "has died";
	static String EXCEPTION_STRING = "FATAL EXCEPTION";
	static String AT_LINE_STRING = "at ";
	static int no_match = 0;
	
	enum SEARCH_STATES {
		
		// index, free_scan, label, string, next state, start_text, end_text
		DEFAULT( 0, true, "DEFAULT", ANY_STRING, "", "", 0 ),
		ANR( 1, true, "ANR", ANR_STRING, ANR_STRING, END_ANR, 0 ),
		EXCEPTION( 2, true, "EXCEPTION", EXCEPTION_STRING, "", "", 3 ),
		AT_LINE_EXCEPTION( 3, false, "AT_LINE", AT_LINE_STRING, "(", ")", 0 );
		
		// index for the state table
		public final int index;
		
		// indicate that any line can match this state
		public final boolean free_scan;
		
		public final String label;
		
		// search string to match the line
		public final String search_string;
		
		// search string within the line, prior to what we want to grab
		public final String start_string;
		
		// search string within the line, after what we want to grab
		public final String end_string;
		
		// next entry state table, to be used after this entry
		public final int next_index;
		
		SEARCH_STATES( final int index, 
				       final boolean free_scan,
				       final String label,
				       final String search_string,
				       final String start_string,
				       final String end_string,
				       final int next_index ) {
			
			this.index = index;
			this.free_scan = free_scan;
			this.label = label;
			this.search_string = search_string;
			this.start_string = start_string;
			this.end_string = end_string;
			this.next_index = next_index;
		}
		
		public int getIndex() {
			return index;
		}
		
		public boolean getFreeScan() {
			return free_scan;
		}
		
		public String getLabel() {
			return label;
		}
		
		public String getString() {
			return search_string;
		}
		
		public String getStartString() {
			return start_string;
		}

		
		public String getEndString() {
			return end_string;
		}
		
		public int getNext() {
			return next_index;
		}
		
        public static SEARCH_STATES fromIndex(final int index) {
            for (SEARCH_STATES state : SEARCH_STATES.values()) {
                if (state.index == index) {
                    return state;
                }
            }
            return ( DEFAULT );
        }

	}
	
	SEARCH_STATES current_search_state = SEARCH_STATES.DEFAULT;
	
	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		long    current_last_mod   = 0;
		boolean       lines_read   = false;
		
		try 
		{
			// can we read the file?
			if ( file_ptr.canRead() )
			{

				current_last_mod = file_ptr.lastModified(); 
				
				if ( current_last_mod > APRPreferences.APRPrefsGetEntryLastModified( FileToScan ) ) {
				
					APRScanBackupFile( FileToScan );
					
					APRDebug.APRLog( TAG, "New File Found: " + FileToScan );
					
					try 
					{							
						BufferedReader inReader = new BufferedReader(new FileReader( FileToScan ), 1024 );
						
						// read a line from the file, see if it has a new
						//   crash.
						String this_line = inReader.readLine();
						
						// scan for 3 types of failures, simultaneously.  
						//   and record all, equally.
						while( this_line != null )
						{
							lines_read = true;
							
							current_search_state = APRLogcatScan( current_search_state, this_line, FileToScan, panic_id );
							
							this_line = inReader.readLine();
						}
					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
					} finally {
						if ( !lines_read ) {
							APRDebug.APRLog( TAG, "No Lines Read!!! " );
						}
					}

					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
					
				} // end if the file has been touched since the last time we looked at it.
				
			} // end if the file can be read.
			
		} catch ( Exception e ) 
		{
			e.printStackTrace();
		    // APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		    // APRDebug.APRDebugStack(e);
		} finally {

		}		
	} // end APRScanFileJava.

	private SEARCH_STATES APRLogcatScan( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id ) {
		
		SEARCH_STATES new_state = this_state;
		
		boolean matching_line = false;
		
		if ( this_state == SEARCH_STATES.DEFAULT ) {
			
			// scan all string types, switch states if something found
			for ( SEARCH_STATES temp_state: SEARCH_STATES.values() ) {
				
				// if we're allowed to scan this state on any line, and this state
				//    has a matching string, scan for it here.
				if ( temp_state.getFreeScan() && ( temp_state.getString() != null ) ) {
					if ( this_line.contains( temp_state.getString() )) {
						
						new_state = temp_state;

						APRDebug.APRLog( TAG, "SearchState:  " + this_state.getLabel() );
						APRDebug.APRLog( TAG, "NewState:     " + new_state.getLabel() );
						
						matching_line = true;
						break;
					}
				}
			}
				
			// end DEFAULT search state.	
		} else {
			// match the string for this state!
			if ( this_state.getString() != null ) {
				if ( this_line.contains( this_state.getString() )) {
					APRDebug.APRLog( TAG, "SearchState:  " + this_state.getLabel() );
					APRDebug.APRLog( TAG, "NewState:     " + new_state.getLabel() );
					
					matching_line = true;
				}
			}
		}

		if ( matching_line == true ) {
			
			// we matched, so reset.
			no_match = 0;
			
			// process new_state
			if ( new_state == SEARCH_STATES.ANR ) return doAnrProcessing( new_state, this_line, FileToScan, panic_id ) ;
			if ( new_state == SEARCH_STATES.AT_LINE_EXCEPTION ) return doAtLineExceptionProcessing( new_state, this_line, FileToScan, panic_id );
			if ( new_state == SEARCH_STATES.EXCEPTION ) return doExceptionProcessing( new_state, this_line, FileToScan, panic_id );
			if ( new_state == SEARCH_STATES.DEFAULT ) return( new_state );
		} else {
			if ( ( new_state != SEARCH_STATES.DEFAULT ) && ( no_match ++ > 10 ) ) {
				// reverting our state.  reset no_match
				no_match = 0;				
				APRDebug.APRLog( TAG, "Never Found Matching State: Resetting to Default" );				
				return ( SEARCH_STATES.DEFAULT );
			}
		}
		
		return new_state;
	}
	
	private String getLogcatTime( String this_line ) {
		return( this_line.substring( 0, 19 ));
	}
	
	// do processing needed when we find the ANR string on the line
	private SEARCH_STATES doAnrProcessing( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id ) {
		
		APRDebug.APRLog( TAG, "doAnrProcessing" );
		
		try {
			// get the string to pull
			int start_task = this_line.lastIndexOf( this_state.getStartString() ) + this_state.getStartString().length();
			int end_task = this_line.lastIndexOf( this_state.getEndString() );
			String task_name = this_line.substring( start_task, end_task );
			
			String time = getLogcatTime( this_line );
			
			// get a unique crash timestamp
			long crash_timestamp = HashTime( time );
			
			// record the ANR crash
			JavaAppScanRecordAndSend( FileToScan, PANIC_ID.JAVA_APP_CRASH_ANR, task_name, task_name, crash_timestamp, task_name );

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed ANR Processing " );
		}
		
		// return the next state
		return SEARCH_STATES.fromIndex( this_state.getNext());
	}

	// NOT USED

	// do processing when we find the DEATH string on the line
	private SEARCH_STATES doAppDeathProcessing( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id ) {
		
		APRDebug.APRLog( TAG, "doAppDeathProcessing" );
		
		try {
			// get the string to pull
			int start_task = this_line.lastIndexOf( this_state.getStartString() ) + this_state.getStartString().length();
			int end_task = this_line.lastIndexOf( this_state.getEndString());
			String task_name = this_line.substring( start_task, end_task );
			
			String time = getLogcatTime( this_line );
			
			// get a unique crash timestamp
			long crash_timestamp = HashTime( time );
			
			// record the ANR crash
			JavaAppScanRecordAndSend( FileToScan, PANIC_ID.JAVA_APP_CRASH, task_name, task_name, crash_timestamp, task_name );

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed AppDeath Processing " );
		}
		// return the next state
		return SEARCH_STATES.fromIndex( this_state.getNext());
	}
	
	// do processing when we find the AT LINE string on the line
	private SEARCH_STATES doAtLineExceptionProcessing( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id ) {
		
		APRDebug.APRLog( TAG, "doAtLineExceptionProcessing" );
		
		try {
			// get the string to pull
			int start_task = this_line.lastIndexOf( this_state.getStartString() ) + this_state.getStartString().length();
			int end_task = this_line.lastIndexOf( this_state.getEndString() );
			String task_name = this_line.substring( start_task, end_task );
			
			String time = getLogcatTime( this_line );
			
			// get a unique crash timestamp
			long crash_timestamp = HashTime( time );
			
			APRScanRecordAndSend( FileToScan, PANIC_ID.JAVA_APP_EXCEPTION, task_name, crash_timestamp, task_name, "(app_name)" + task_name );

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed AtLine Processing " );
		}
		// return the next state
		return SEARCH_STATES.fromIndex( this_state.getNext());
	}
		
	// do processing when we find the fatal exception string on the line
	private SEARCH_STATES doExceptionProcessing( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id ) {
		
		APRDebug.APRLog( TAG, "doExceptionProcessing" );
		
		try {
			
			APRDebug.APRLog( TAG, "No Work.  Find Next State..." );
			
		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed Exception Processing " );
		}
		
		return SEARCH_STATES.fromIndex( this_state.getNext());
	}

	private void JavaAppScanRecordAndSend(
			String filename, 
			PANIC_ID panic_id,
			String full_crash_text, 
			String app_name, 
			long crash_timestamp, 
			String crash_string ) {
		
		if ( APRInstalledPackages.IsFactoryInstalledApp(full_crash_text) )
		{
			APRScanRecordAndSend( filename, panic_id, app_name, crash_timestamp, crash_string, "(app_name)" + full_crash_text );
		}
	}

	
}
