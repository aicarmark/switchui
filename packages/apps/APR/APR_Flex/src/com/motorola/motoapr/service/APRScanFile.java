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
import android.os.FileUtils;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

public class APRScanFile {
	
	static final String TAG="APRScanFile";
	static final String APR_FILE_BACKUP_AREA = "/data/data/com.motorola.motoapr.service/";

	static final int APR_MAX_NUM_BACKUP_FILES = 20;
	static final int APR_DEFAULT_LOG_SIZE = 50;
	
	APRManager system_apr_manager = null;
	
	APRScanFile( APRManager apr_manager )
	{
		system_apr_manager = apr_manager;
		return;
	}
	
	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		FileInputStream file_ioptr = null;
		long current_last_mod      = 0;
		int dsp_version = 0;
		
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
				
					byte crash_log[] = new byte[ APR_DEFAULT_LOG_SIZE ];
			
					// get the io ptr
					file_ioptr = new FileInputStream( FileToScan );
										
					try 
					{	
						int file_read = file_ioptr.read( crash_log, 0, APR_DEFAULT_LOG_SIZE );
						
						APRDebug.APRLog( TAG, "Read " + file_read + " bytes." );
						
						file_ioptr.close();
						
					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
						// log the crash anyways.  just because we cannot
						//  read the file, we know at this point that the
						//  timestamp has been updated, and we should be 
						//  indicating a failure.  But in this case the
						//  data sent will be blank.
					}			
										
					// the timestamp is the last time this file was modified.
					//   by default the id to be used is taken from the PANIC_ID 
					//   enumeration.
					system_apr_manager.APRMgrStoreEntry( FileToScan, panic_id, panic_id.id, dsp_version, crash_log, current_last_mod, null ); 
			
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
				}
			}
			
		} catch ( Exception e ) 
		{
		    APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		}
	}
	
	void APRScanRecordAndSend( String filename, 
						       PANIC_ID panic_id, 
						       String app_name, 
						       long timestamp, 
						       String crash_text, 
						       String full_crash_text )  {
		
		byte crash_log[] = new byte[ APR_DEFAULT_LOG_SIZE ];
		int dsp_version = 0;
		
		if ( timestamp > APRPreferences.APRPrefsGetEntryLastModified( app_name ) )
		{
			crash_log = crash_text.getBytes();

			try {
				// the timestamp is the last time this file was modified.
				//   by default the id to be used is taken from the PANIC_ID 
				//   enumeration.
				system_apr_manager.APRMgrStoreEntry( filename, panic_id, panic_id.id, dsp_version, crash_log, timestamp, full_crash_text );
				APRDebug.APRLog( TAG, "Crashed App: " + app_name + " Crash Text: " + full_crash_text + " " + timestamp );
				Log.d(TAG, "Crashed App: " + app_name + " Crash Text: " + full_crash_text + " " + timestamp );
			} catch ( Exception e )	{
				APRDebug.APRLog( TAG, "Initialization of APRScanForPanics is wrong.");
			}
				
			APRPreferences.APRPrefsSetEntryLastModified( app_name, timestamp );
		}
		else
		{
			APRDebug.APRLog( TAG, "Crash Already Logged: " + crash_text + " " + timestamp );
		}
	}
	
	/**
	 * Function to Backup a /data/anr/traces.txt file
	 *   to a uniquely named file in /data/anr_backup
	 *   
	 * @param fileToCopy
	 */
	void APRScanBackupFile(String fileToCopy ) {
					
		// dst_name starts as the full path to the name
		//   of the file.
		String dst_name = new String( fileToCopy );
		
		// attempt to cut out the preceding path.
		int index = dst_name.lastIndexOf('/') + 1;
		
		// if successful...
		if ( index > 0 )
		{
			// cut it out.
			dst_name = dst_name.substring(index);
			
			// attempt to cut out the trailing
			//   extension on the file.
			index = dst_name.lastIndexOf('.');
		
			// if successful
			if ( index > 0 )
			{
				// cut it out.
				dst_name = dst_name.substring( 0, index );
			}
		}

		// dst_name now contains no path, and no extension.
		
		File src_file = new File( fileToCopy );
			
		// pre-pend new path, "bck", and a number.
		File dst_file = new File( APR_FILE_BACKUP_AREA + dst_name + "bck" + APRScanGetNextLargestBackupNumber() + ".txt" );
			
		FileUtils.copyFile( src_file, dst_file );
		
		// limit the number of backup files indirectly.
		//   limit the total number of entries allowed in
		//   the directory, including other files.  But
		//   only delete files including the name bck
		APRScanLimitBackups( APR_MAX_NUM_BACKUP_FILES );
							
    } // end APRScanBackupFile

	// method to delete a log file and catch any errors.
	void APRDeleteLogFile( String fileToDelete )
	{
		try {
			File to_del = new File( fileToDelete );			
			to_del.delete();			
		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Delete File Failed:" + fileToDelete );
		} finally {
			APRDebug.APRLog( TAG, "Delete File :" + fileToDelete );
		}
	}
	
	private void APRScanLimitBackups( int limit) 
	{
		File log_area = new File( APR_FILE_BACKUP_AREA );

		if ( ( log_area != null ) && ( log_area.isDirectory() ) ) 
		{
			// this is a directory.  find the sub entries.
			String dir_entries[] = log_area.list();

			if ( dir_entries != null ) {

				try {

					APRDebug.APRLog( TAG, "Limiting Total Num Backups..." );

					// if too many files exist, find and delete the oldest.
					while ( dir_entries.length > limit )
					{
						boolean first_file_found = false;
						long oldest_timestamp = 0;
						String oldest_filename = null;
						int current_list_length = dir_entries.length;
				
						for ( int j=0; j< dir_entries.length; j++ )
						{
							if ( dir_entries[j].contains("bck"))
							{
								String this_entry_name = new String( APR_FILE_BACKUP_AREA + dir_entries[j] ); 
						
								File this_entry = new File( this_entry_name );
								
								long current_last_mod = this_entry.lastModified(); 
								
								if ( !first_file_found ) 
								{
									oldest_timestamp = current_last_mod;
									oldest_filename = this_entry_name;
									first_file_found = true;
								}
								else if ( current_last_mod < oldest_timestamp )
								{
									oldest_timestamp = current_last_mod;
									oldest_filename = this_entry_name;
								}
							}
						} // end loop through each file name.

						if ( oldest_filename != null )
						{
							// 	delete the oldest entry.
							File oldest_entry = new File( oldest_filename );			
							oldest_entry.delete();
						}
						else
						{
							APRDebug.APRLog(TAG, "Failed To Limit Files ");
						}

						// rescan dir_entries.  
						dir_entries = log_area.list();

						if ( log_area.length() == current_list_length )
						{
							// detect a possible infinite loop.  if we 
							//   were not able to delete an entry, do not
							//   allow the loop to continue.
							limit = dir_entries.length;
							
							// try to break out of the while loop early 
							//   the manual way as well.  
							break;
						}
	
					} // end while we need to search for and delete the oldest entry.
				} catch ( Exception e ) {
					APRDebug.APRLog(TAG, "Failed To Limit Files ");
				}
			}
		}
	} // end APRScanLimitBackups

	/**
	 * Routine to determine the largest backup # 
	 *   that exists in the backup area, add 1 to
	 *   it and return it.  This is to ensure we
	 *   do not have backup file names that conflict.
	 * @return
	 */
	private int APRScanGetNextLargestBackupNumber()
	{
		File log_area = new File( APR_FILE_BACKUP_AREA );
		
		int max_count = 0;
		
		if ( log_area.isDirectory() ) 
		{
			// this is a directory.  find the sub entries.
			String dir_entries[] = log_area.list();

			for ( int j=0; j< dir_entries.length; j++ )
			{
				if ( dir_entries[j].contains("bck"))
				{
					int index = dir_entries[j].lastIndexOf("bck") + 3;
					int index2 = dir_entries[j].lastIndexOf('.'); 
					String substring = dir_entries[j].substring( index, index2 );
					
					// get the # out of the substring.
					Integer count = new Integer( substring );
					
					if ( count > max_count )
					{
						max_count = count;
					}
				}
			}
		}	
		
		return( max_count + 1 );
		
	} // end APRScanLimitBackups
	
	
	long HashTime(String time_only) {		
		byte[] array = time_only.getBytes();
		long hash_result = 0;
		int digit_count = 0;
		
		// so that larger dates are always bigger hash values,
		//  weight the contributions by position in the array,
		//  left to right, heavier to the left.
		for ( int i=0; i < array.length; i++ ) 
		{	
			char c = (char)(array[i] & 0xFF);
		
			// find the different fields.  for each
			//   time that there are consecutive digits
			//   multiply by 10.  When there's a separation,
			//   push to the result.  But you also have to
			//   weigh each of the pushes.  2009 is more important
			//   than 06.  and 06 is more important than the day.
			if ( Character.isDigit(c) ) {

				int value = Character.digit(c, 10 );
				
			    // APRDebug.APRLog( TAG, "Index:" + i + " Value:" + value );
				
			    // make room for the new value.
				hash_result = hash_result * 10;
		
				// add in the base 10 value.
				hash_result = hash_result + value;

				digit_count++;
			}
		}

		// make sure the "magnitude" of each
		//  number out of this algo, is consistent
		while( digit_count < 16 ) {
			hash_result = hash_result * 10;
			digit_count++;
		}

		// APRDebug.APRLog( TAG, "Hash of " + time_only + ":" + hash_result );
				
		return hash_result;
	}
	
	// very fast routine to convert an int to a byte array.
	//   this is used to copy integers into the raw data (or other data
	//   streams).  We have similar routines in the Message Creation code,
	//   but not precisely like this, nor as simple.
	public static final byte[] intToByteArray(int value, boolean little_endian)  
	{

		if ( little_endian )
		{
				return new byte[]{
					(byte)(value >>> 24), 
					(byte)(value >> 16 & 0xff), 
					(byte)(value >> 8 & 0xff), 
					(byte)(value & 0xff) };
		}
		else
		{
			return new byte[]{
					(byte)(value & 0xff),
					(byte)(value >> 8 & 0xff),
					(byte)(value >> 16 & 0xff),
					(byte)(value >>> 24) };				
		}
	}
	
	// very fast routine to convert an int to a byte array.
	//   this is used to copy integers into the raw data (or other data
	//   streams).  We have similar routines in the Message Creation code,
	//   but not precisely like this, nor as simple.
	public static final byte[] longToByteArray(long value, boolean little_endian)  
	{
		
		if ( little_endian )
		{
			return new byte[]{
					(byte)(value >>> 56), 
					(byte)(value >> 48 & 0xff), 
					(byte)(value >> 40 & 0xff), 
					(byte)(value >> 32 & 0xff),
					(byte)(value >> 24 & 0xff), 
					(byte)(value >> 16 & 0xff), 
					(byte)(value >>  8 & 0xff), 
					(byte)(value       & 0xff) };

		}
		else
		{
			return new byte[]{
					(byte)(value       & 0xff),
					(byte)(value >>  8 & 0xff), 
					(byte)(value >> 16 & 0xff), 
					(byte)(value >> 24 & 0xff), 
					(byte)(value >> 32 & 0xff),
					(byte)(value >> 40 & 0xff), 
					(byte)(value >> 48 & 0xff), 
					(byte)(value >>> 56) };
		}
	}
	
	// worst case hex string to int conversion.
	public static int HexStringToIntConversion( String hex_string ) 
	{
		int return_int = 0;
		
		if ( hex_string.length() > 8 )
		{
			hex_string = new String( hex_string.substring( 0, 8 ) );
		}
		
		try
		{
			// convert radix 16.
			return_int = Integer.parseInt( hex_string, 16 );
			
		} catch ( NumberFormatException e ) {
			// Something wrong with the format.  Either not hex, or something
			//   else.  Do a byte by byte conversion, doing the best we can.
			byte[] hex_bytes = hex_string.getBytes();
			
			// just to be careful
			return_int = 0;
			
			for ( int i = 0; i < hex_bytes.length; i++ )
			{
				int val1 = 0;
				
				if ( ( hex_bytes[i] >= '0' ) && ( hex_bytes[i] <= '9' ) )
				{
					val1 = hex_bytes[i] - '0';
				}
				else if ( ( hex_bytes[i] >= 'A' ) && ( hex_bytes[i] <= 'F' ) )
				{
					val1 = hex_bytes[i] - 'A' + 0xA;
				} else if ( ( hex_bytes[i] >= 'a' ) && ( hex_bytes[i] <= 'f' ) )
				{
					val1 = hex_bytes[i] - 'a'+ 0xA;
				}
								
				return_int = return_int << 4;
				return_int |= val1;
			}
		}
		
		return return_int;
		
	}
	
	
	/**
	 * force the incoming string to a particular length
	 * then dup it off to a new string to be returned.
	 * @param string_to_force
	 * @param length_to_force
	 * @return
	 */
	final String ForceStringLength( String string_to_force, int length_to_force )
	{
		String local_string = null;
		
		try 
		{
			local_string = new String( string_to_force );
			
			if ( local_string.length() < length_to_force ) 
			{
				while ( local_string.length() < length_to_force )
				{
					local_string = local_string + " ";
				}
			}
			
			if ( local_string.length() > length_to_force )
			{
				local_string = local_string.substring( 0, length_to_force );
			}
								
		} catch ( Exception e ) {
			
			APRDebug.APRLog( TAG, "Error Adjusting String Size.  Bad String?" );
			
		} finally {
			string_to_force = new String( local_string );
		}
		
		return( string_to_force );
	}
	
	String ReverseName( String app_name, char seperator )
	{
		String new_app_name = null;
		int index = 0;
		
		while ( ( index = app_name.lastIndexOf( seperator ) ) > -1 )
		{																				
			if ( new_app_name != null )
			{
				new_app_name = new_app_name + seperator + app_name.substring(index + 1);
			}
			else
			{
				new_app_name = new String( app_name.substring(index + 1) );
			}
			
			// Truncate out we have put in so far. 
			app_name = app_name.substring( 0, index );
		}
		
		// do some checks to make sure this is clean.
		if  ( ( app_name != null ) && ( new_app_name != null ) )
		{
			new_app_name = new_app_name + seperator + app_name;
		}
		else
		{
			if ( app_name != null ) 
			{
				new_app_name = app_name;
			}
			else
			{
				new_app_name = new String( "UNKNOWN" );
			}
		}
		
		return( new_app_name );

	}
	
	
}
