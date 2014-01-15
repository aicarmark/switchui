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
//Stephen Dickey 08/09/2010 1.0       Initial Version
//
//********************************************************** //
package com.motorola.motoapr.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanFileMDM6600 extends APRScanFile {

	static String TAG = "APRScanFileLogcatLog";
	
	APRScanFileMDM6600(APRManager apr_manager) {
		super(apr_manager);
	}
	
	static String ANY_STRING = null;
	static String CRASH_STRING = "Error in file ";
	static String TIME_STRING = "Time of crash (m-d-y h:m:s): ";
	static int no_match = 0;
	
	enum SEARCH_STATES {
		
		// index, free_scan, label, string, next state, start_text, end_text
		DEFAULT( 0, true, "DEFAULT", ANY_STRING, "", "", 0 ),
		MODEM_CRASH( 1, true, "CRASH", CRASH_STRING, CRASH_STRING, "", 2 ),
		CRASH_TIMESTAMP( 2, false, "TIMESTAMP", TIME_STRING, TIME_STRING, "", 0 );

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
		BufferedReader  inReader   = null;
		FileReader    filereader   = null;
		
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
						filereader = new FileReader( FileToScan );

						if ( filereader != null ) {

							inReader = new BufferedReader( filereader, 1024 );

							if ( inReader != null ) {
							
								// read a line from the file, see if it has a new
								//   crash.
								String this_line = inReader.readLine();
								
								// scan for 3 types of failures, simultaneously.  
								//   and record all, equally.
								while( this_line != null )
								{
									lines_read = true;
									
									current_search_state = APRLogcatScan( current_search_state, this_line, FileToScan, panic_id, current_last_mod );
									
									this_line = inReader.readLine();
								}
								
								inReader.close();
							}
							
							filereader.close();
						}
					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
					} finally {
						if ( !lines_read ) {
							APRDebug.APRLog( TAG, "No Lines Read!!! " );
						}
					}

					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
				} else {
					APRDebug.APRLog( TAG, "file found: already scanned or older than latest." );
				}
			} // end if the file can be read.
		} catch ( Exception e ) 
		{
			e.printStackTrace();
		    // APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		    // APRDebug.APRDebugStack(e);
		} finally {
	
		}		
	} // end APRScanFileJava.

	private SEARCH_STATES APRLogcatScan( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id, long timestamp ) {
		
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
			if ( new_state == SEARCH_STATES.MODEM_CRASH ) return doModemProcessing( new_state, this_line, FileToScan, panic_id, timestamp ) ;
			if ( new_state == SEARCH_STATES.CRASH_TIMESTAMP ) return doTimestampProcessing( new_state, this_line, FileToScan, panic_id, timestamp ) ;
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
	
	String filename_lineno = null;
	
	// do processing when we find the AT LINE string on the line
	private SEARCH_STATES doModemProcessing( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id, long timestamp ) {

		APRDebug.APRLog( TAG, "doModemProcessing" );

		try {
			// get the string to pull
			int start_string = this_line.lastIndexOf( this_state.getStartString() ) + this_state.getStartString().length();
			int end_string = this_line.lastIndexOf( this_state.getEndString() );
			String this_substring = this_line.substring( start_string, end_string );
			
			filename_lineno = new String( this_substring );

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed doModemProcessing" );
		}
		// return the next state
		return SEARCH_STATES.fromIndex( this_state.getNext());
	}

	// do processing when we find the AT LINE string on the line
	private SEARCH_STATES doTimestampProcessing( SEARCH_STATES this_state, String this_line, String FileToScan, PANIC_ID panic_id, long timestamp ) {

		APRDebug.APRLog( TAG, "doTimestampProcessing" );

		try {
			// get the string to pull
			int start_string = this_line.lastIndexOf( this_state.getStartString() ) + this_state.getStartString().length();
			int end_string = this_line.lastIndexOf( this_state.getEndString() );
			String this_substring = this_line.substring( start_string, end_string );
			
			long this_timestamp = HashTime( this_substring );

			// Filter Modem Indication of a Modem Powerdown
			if ( !filename_lineno.contains( "PWRDWN_REQ" )) {
				APRScanRecordAndSend( FileToScan, PANIC_ID.MODEM_CRASH3, filename_lineno, this_timestamp, filename_lineno, filename_lineno );
			} else {
				APRDebug.APRLog( TAG, "PWRDWN_REQ: Not Recording Normal Modem Powerdown." );
			}

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed doModemProcessing" );
		}
		// return the next state
		return SEARCH_STATES.fromIndex( this_state.getNext());
	}

	
}
