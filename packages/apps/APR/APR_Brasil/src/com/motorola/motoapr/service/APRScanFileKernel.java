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
import android.text.format.Time;

import com.motorola.motoapr.service.PanicType.PANIC_ID;
import com.motorola.motoapr.service.PuReasonType.PU_REASON_TYPE;

public class APRScanFileKernel extends APRScanFile{
	
	// this is the byte array that starts the beginning of each 4096 chunk
	//   of the kernel crash file.
	static byte[] kernel_crash_sync = { (byte)(0x00), (byte)(0x5D), (byte)(0x00), (byte)(0x5D) };
	
	// this is the byte array (non aligned) that ends each 4096 block.  if
	//   we find this before the end of the block is reached, we can safely abort
	//   scanning the block.  this is for performance.
	static byte[] kernel_crash_term = { (byte)(0x0A), (byte)(0xFF), (byte)(0xFF), (byte)(0xFF) };
	
	static byte[] kernel_crash_erased = { (byte)(0xFF), (byte)(0xFF), (byte)(0xFF), (byte)(0xFF) };
	
	// multiple of 4 to keep synchronized... so we can synchronize
	//   on a 4 byte boundary.
	static int KERNEL_CRASH_CHUNK_SIZE = 4096;
	
	// amount of data to try to parse, once we've found the area we 
	//   want to parse.
	static int KERNEL_CRASH_PARSE_SIZE = 60;
	
	// only allocate these once.
	byte crash_log[] = new byte[ KERNEL_CRASH_CHUNK_SIZE ];
	byte sync_buff[] = new byte[ kernel_crash_sync.length ];
	byte parse_buff[] = new byte[ KERNEL_CRASH_PARSE_SIZE ];
	byte reason_byte_array[] = null;
	
	// for scanning kernel files.
	private byte[] pc_bytes = "pc :".getBytes();
	static String current_time_str = "Current Time = ";
	private byte[] current_time_bytes = current_time_str.getBytes();
	private byte[] kernel_panic_bytes = "Kernel panic -".getBytes();
	
	static String KERNEL_APPLICATION_NAME = "LINUX_KERNEL";
	
	private int pc_value = 0;
	private int lr_value = 0;
	private long crash_timestamp = 0;
	
	// only do once per execution (should be once per powerup).
	private boolean kernel_panic_extracted = false;

	APRScanFileKernel(APRManager apr_manager) {
		super(apr_manager);
		
		if ( !kernel_panic_extracted )
		{
			// it is no longer necessary to extract kernel panics and
			//   process the kernel panics with this file.  
			// all of that work is done for us with /data/dontpanic
			//   and the new scanner APRScanFileApanic.java
			
			kernel_panic_extracted = true;
		}
	}

	// Scan 50 bytes from the input file, and store it as a crash.
	public void ScanFile( PANIC_ID panic_id, String FileToScan ) 
	{
		File            file_ptr   = new File( FileToScan );
		FileInputStream file_ioptr = null;
		long            current_last_mod = 0;
		int             dsp_version = 0; 
		boolean         kernel_crash_reported = false;
		
		// initial value non-zero, to prime the loop.
		int file_chunk_size_read = 1;
				
		// We need to delete the file after backing it up,
		//  if the file doesn't contain timestamps for individual
		//  kernel crash logs.  Otherwise, even when we advance
		//  the timestamp of the kernel crashes, we could be looking
		//  at old logs, and send them multiple times.
		
		byte[] kernel_crash_log = new byte[ APR_DEFAULT_LOG_SIZE ];
		
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

					// these are high level concepts, that extend up to the file, rather than
					//   down into the per-block scanning.
					boolean reason_found = false;
					boolean crash_time_found = false;
					boolean pc_lr_found = false;
					boolean kernel_panic_found = false;
					
					// start off assuming we need to delete the file.  if we find
					//   timestamps within the file, we won't delete it.
					boolean delete_file_upon_exit = false;
					
					// this is the method we'll use to ensure we only read good
					//   data from the file.  the first byte of each chunk represents
					//   the chunk count.  
					byte chunk_count = 0;

					// ensure these are cleared out for each file, 
					//   and after we have reported a crash ..
					pc_value = 0;
					lr_value = 0;
					crash_timestamp = 0;
										
					// get the io ptr
					file_ioptr = new FileInputStream( FileToScan );					
					
					if ( file_ioptr!= null ) try 
					{	
						/*****************************************************************************/
						/* beginning of file scanning/reading. */
						/*****************************************************************************/
						
						// continue until we fail to read more data from the file.
						while ( file_chunk_size_read > 0 )
						{						
							file_chunk_size_read = file_ioptr.read( crash_log, 0, KERNEL_CRASH_CHUNK_SIZE );
							
							chunk_count++;
							
							if ( file_chunk_size_read > 0 )
							{
								/*****************************************************************************/
								/* beginning of file chunk scanning/reading. */
								/*****************************************************************************/

								int read_chunk_offset = 0;
								
								// check to see if the chunk count has been written.
								//    if it has been written with a good value, then
								//    we will process it.
								if ( crash_log[0]==chunk_count )
								{									
									APRDebug.APRLog( TAG, "Good Data at Chunk: " + chunk_count );

									// ok... we're entering a section after a sync has been found.
									//   that in itself is not relevant to the panic data itself. 
									//   sync will only be an indication that this is a valid 4096 
									//   chunk of data to look at.
									
									// the first two words of the chunk are always sync or garbage data.
									read_chunk_offset += 8;
									
									// only scan for reason once per kernel panic.  there are multiple
									//   "reasons" we write to the kernel log, but the real reason is
									//   the first one found... after a previous kernel panic was found.
									if ( !reason_found )
									{
										reason_found = ScanForCrashReason( crash_log, read_chunk_offset, file_chunk_size_read );
									}
															
									// this code starts over at the beginning of the read chunk again.
									//   it doesn't care if a crash reason was found or not, as a result.
									int length_to_parse = file_chunk_size_read - read_chunk_offset;
							
									// scan the chunk for other items. the for loop can contain
									//   anything we want to scan for, but the logic (using break to
									//   abort the loop), may have to be updated if you scan for something
									//   else.
									if ( length_to_parse > 0 )
									{										
										// each byte in the read chunk could contain a start string.  
										//   we have to go through each one.
										for ( int i = read_chunk_offset; ( i < length_to_parse ); i ++ )
										{
											// if we've already found a kernel panic, we're heading
											//   through this loop on a different chunk.  pre-test
											//   the data we look at.  We may be looking for crash time
											//   and we may be looking for PC/LR.  If we find those then
											//   we have unique actions to take.
											if ( kernel_panic_found )
											{
												if ( match_PCLR( crash_log, i) || 
												     match_kernel_panic( crash_log, i ) )
												{
													APRDebug.APRLog( TAG, "Kernel Panic Found while processing previous.");

													// woops.  we had a kernel panic recorded, and
													//   we skipped onto the next chunk.  send whatever
													//   we have stored so far, and quickly.
													BuildAndStoreMessage( FileToScan, panic_id, panic_id.id, dsp_version, kernel_crash_log, crash_timestamp, reason_found );
													
													reason_found=false;
													pc_lr_found = false;
													kernel_panic_found = false;

													// indicate that we found a kernel crash.
													kernel_crash_reported = true;
													
													// this file format doesn't guarantee a timestamp per kernel panic.  
													//   so, the file must be deleted upon exit.
													delete_file_upon_exit = true;

													// now.  allow PC/LR, current time, and kernel_panic state
													//  to be rewritten.  on this iteration through the for loop.
													
													
												
												} 
												else 
												{
													// if we didn't match a kernel panic or a 
													//   pc/lr, see if we find the time.
													if (match_CurrentTime( crash_log, i ))
													{
														// ok!  we had a kernel crash, and we found the time.
														// that means it's time to skip out, after scanning for that time.
														if ( ScanForCurrentTime( crash_log, i ) )
														{
															crash_time_found = true;
															
															APRDebug.APRLog( TAG, "Kernel Crash and Timestamp Found!");
														
															// abort the loop and go send message.
															break;
														}
													}
												}
											}
											
											
											if ( ScanForPCLR( crash_log, i ) )
											{
												APRDebug.APRLog( TAG, "PC and LR FOUND!");

												pc_lr_found = true;
											}
											
											if ( ScanForCurrentTime( crash_log, i ) )
											{
												APRDebug.APRLog( TAG, "CRASH TIME FOUND!");
												
												crash_time_found = true;
											}
											
											// if we find a kernel panic in this loop,
											//   abort immediately.  this is the END of a 
											//   crash, in a sense.  We're still waiting for 
											//   timestamp.
											if ( ScanForKernelPanic( crash_log, i ) )
											{
												APRDebug.APRLog( TAG, "KERNEL PANIC FOUND!");
												
												kernel_panic_found = true;
												break;
											}
											
											// if we've come to the end of the chunk, also
											//   abort the for loop.
											if ( ScanForEndOfChunk( crash_log, i ) )
											{
												APRDebug.APRLog( TAG, "END OF CHUNK FOUND!");

												break;
											}
											
										} // end for each byte in the chunk we want to look at.
								
									} // end if the length of data to parse is reasonable.
							
									if ( kernel_panic_found && crash_time_found )
									{
										APRDebug.APRLog( TAG, "LOGGING CRASH ATTEMPT.  KERNEL AND TIMESTAMP OK!");
										
										// for the new format type, crash timestamp is included.
										//   for this type, we must determine if the crash has been
										//   stored in the past.  
										if ( crash_timestamp > APRPreferences.APRPrefsGetEntryLastModified( KERNEL_APPLICATION_NAME ) ) 
										{
											
											APRDebug.APRLog( TAG, "LOGGING CRASH!! KERNEL AND TIMESTAMP OK, and NEW!");

											BuildAndStoreMessage( null, panic_id, panic_id.id, dsp_version, kernel_crash_log, crash_timestamp, reason_found );
											
											// reset these values.  we're still going through the file
											//   so we will have to find a new reason, crash log or kernel panic.
											reason_found = false;
											pc_lr_found = false;
											kernel_panic_found = false;
											
											kernel_crash_reported = true;
											
											APRPreferences.APRPrefsSetEntryLastModified( KERNEL_APPLICATION_NAME, crash_timestamp );
											
											crash_time_found = false;
											
											// setting this to zero means we won't send another crash
											//   until we reset this value.  this is already true because
											//   we have to exceed the stored timestamp value.
											crash_timestamp = 0;
										}
										else
										{
											APRDebug.APRLog( TAG, "Crash Too Old... Not Being Sent!");

										}

									} // end if kernel_panic was found.
																
								} // end if sync was found.
								else
								{
									// APRDebug.APRLog( TAG, "No Sync Found." );
								}
														
							} // end if we were able to read more data.
							
						} // end while.

					} catch ( IOException e ) {
						APRDebug.APRLog( TAG, "Can't read data from" + FileToScan );
					} catch ( IndexOutOfBoundsException o )
					{
						APRDebug.APRLog( TAG, "Attempt to read beyond end of file." );
					} finally {
						
						// close the file.
						file_ioptr.close();
						
						// backup the file to the APR storage area.
						APRScanBackupFile( FileToScan );
						
						// if we couldn't find timestamps for the kernel crash logs
						//   delete the file upon exit.
						// well, we're done scanning the file.  
						if ( delete_file_upon_exit )
						{							
							// delete the original log file so it is not scanned more than once.
							APRDeleteLogFile( FileToScan );
						}
					}
				
					APRPreferences.APRPrefsSetEntryLastModified( FileToScan, current_last_mod );
					
				} // end if the file has a new timestamp
			} // end if we can read the file 
			
		} catch ( Exception e ) 
		{
		    APRDebug.APRLog( TAG, "Failed to Scan File " + FileToScan );
		} finally
		{
			// check up on our boot info, and compare what is there
			//   versus what was discovered in this function.  if we 
			//   didn't report in... report. but in either case, don't
			//   allow reporting based upon bootinfo again.
			if ( APRScanFileBootInfo.pu_reason_happened( PU_REASON_TYPE.AP_KERNEL_PANIC ) )
			{				
				if ( !kernel_crash_reported )
				{					
					pc_value = lr_value = 0;
								
					BuildAndStoreMessage( null, panic_id, panic_id.id, dsp_version, kernel_crash_log, crash_timestamp, false );
				}			
				
				// indicate that the kernel crash was reported, and should not be reported again
				//   (as the result of looking at bootinfo).
				APRScanFileBootInfo.pu_reason_report( PU_REASON_TYPE.AP_KERNEL_PANIC );
			}
		}	
	} // end ScanFile
	

	
	private boolean ScanForCrashReason( byte[] crash_log, int read_chunk_offset, int max_crash_log_scan )
	{
		boolean reason_found = false;
		
		// Copy the reason string here, after we've sunk.  we may never find the PC and LR.
		int reason_offset = read_chunk_offset;

		while ( ( reason_found == false ) && 
				( reason_offset < max_crash_log_scan ) )
		{
			if ( ( crash_log[ reason_offset ] == 0x0A ))
			{
				reason_found = true;
				reason_byte_array = new byte[ ( reason_offset - read_chunk_offset )];

				System.arraycopy( crash_log, read_chunk_offset, reason_byte_array, 0, reason_offset - read_chunk_offset );

				String crash_reason = new String( reason_byte_array );
			
				APRDebug.APRLog( TAG, "Crash Reason Found: " + crash_reason );
			}

			reason_offset++;
		}
		
		return( reason_found );
		
	} // end ScanForCrashReason
	
	
	boolean match_PCLR( byte[] crash_log, int index )
	{
		boolean return_value = false;
		
		if ( match_string( crash_log, index, pc_bytes ))
		{
			return_value = true;
		}
		
		return( return_value );
	}
	
	/**
	 * Scan for PC and LR.  We can scan both in one routine,
	 * because these two values are adjacent to each other in
	 * the kernel crash file. we can also grab psr in this code,
	 * though we are not currently doing so.  
	 * 
	 * @param crash_log
	 * @param index
	 * @return
	 */
	boolean ScanForPCLR( byte[] crash_log, int index ) {
				
		boolean crash_log_found = false;
		
		if ( match_string( crash_log, index, pc_bytes ))
		{
			// copy a chunk of the data for parsing.
			System.arraycopy( crash_log, index, parse_buff, 0, KERNEL_CRASH_PARSE_SIZE );
										
			try {
				// scanning for a section of the kpanic file that looks like this:
				// "pc : [<bf01e018>]    lr : [<c003e2d0>]    psr: 60000013"
	
				String parsed_data = new String( parse_buff );

				int pc_index = parsed_data.lastIndexOf( "pc");									

				if ( pc_index >= 0 ) 
				{
					parsed_data = parsed_data.substring( pc_index );
					
					// the end is the next field.
					int end_pc_index = parsed_data.lastIndexOf( "lr" );
					
					// ok we have narrowed it down enough, so we won't
					//   conflict with the lr field.  grab just the number.
					if ( end_pc_index >= 0 )
					{
						String pc = parsed_data.substring( pc_index + 1, end_pc_index );
						pc_index = pc.lastIndexOf( '<' );
						end_pc_index = pc.lastIndexOf( '>' );
						pc = pc.substring( pc_index + 1, end_pc_index );
						pc_value = HexStringToIntConversion( pc );
					}
				}
	
				int lr_index = parsed_data.lastIndexOf( "lr" );
				
				if ( lr_index >= 0 ) 
				{
					// the end is the next field
					int end_lr_index = parsed_data.lastIndexOf( "psr" );
					
					// as long as we have narrowed it down, continue.
					if ( end_lr_index >= 0 )
					{
						// grab just the number
						String lr = parsed_data.substring( lr_index + 1, end_lr_index );
						lr_index = lr.lastIndexOf( '<' );
						end_lr_index = lr.lastIndexOf( '>' );
						lr = lr.substring( lr_index + 1, end_lr_index );
						lr_value = HexStringToIntConversion( lr );
					}														
				}
				
				// this variable must be reset each entry in the for loop.
				//   this is used to determine if we need to continue the
				//   for loop.
				crash_log_found = true;
	
			} catch ( Exception e ) {
				APRDebug.APRLog( TAG, "Failed to Parse.");
			} finally {
				if ( crash_log_found == false )
				{
					APRDebug.APRLog( TAG, "Failed to Parse.");
				}
			}
		} // end if "pc : " found.
		
		return( crash_log_found );

	}
	
	private boolean match_CurrentTime( byte[] crash_log, int index )
	{
		boolean return_value = false;
				
		if ( match_string( crash_log, index, current_time_bytes ))
		{	
			return_value = true;
		}
		
		return( return_value );
	}
	
	private boolean ScanForCurrentTime( byte[] crash_log, int index )
	{
		boolean crash_time_found = false;
		
		long this_crash_timestamp = 0;
		
		if ( match_string( crash_log, index, current_time_bytes ))
		{	
			// copy a chunk of the data for parsing.
			System.arraycopy( crash_log, index, parse_buff, 0, KERNEL_CRASH_PARSE_SIZE );
										
			try {
				String parsed_data = new String( parse_buff );
				
				int time_start_index = parsed_data.lastIndexOf( current_time_str );									

				if ( time_start_index >= 0 ) 
				{
					parsed_data = parsed_data.substring( time_start_index );
					
					// the end is the next field
					int end_time_index = parsed_data.lastIndexOf( "," );
					
					if ( end_time_index >= 0 )
					{
						String time_str = parsed_data.substring( time_start_index + current_time_str.length(), end_time_index );
						
						// log the whole line.
						APRDebug.APRLog( TAG, "Kernel Crash Found @: " + new String( time_str ) );
						
						try {
							Time time = new Time();
							time.parse( time_str );
							this_crash_timestamp = time.normalize(false);
						} catch ( Exception e ) {
							this_crash_timestamp = HashTime(time_str);
						} catch ( UnsatisfiedLinkError e ) {
							this_crash_timestamp = HashTime(time_str);
						} 
						
						if ( this_crash_timestamp != 0 )
						{
							crash_timestamp = this_crash_timestamp;
							crash_time_found = true;
						}
					}
				}

			} catch ( Exception e ) {
				APRDebug.APRLog( TAG, "Failed to Parse.");
			} finally {
				if ( crash_time_found == false )
				{
					APRDebug.APRLog( TAG, "Failed to Parse.");
				}
			}
		
		} // end if "Current Time = " found.
		
		return( crash_time_found );
	}
	
	// routine to match for a kernel panic, but do nothing else
	private boolean match_kernel_panic( byte[] crash_log, int index ) {
		return( ScanForKernelPanic( crash_log, index  ) );
	}
	
	// routine to scan for kernel panic, and do any associated work.
	private boolean ScanForKernelPanic( byte[] crash_log, int index ) {
		
		boolean kernel_crash_log_found = false;
		
		if ( match_string( crash_log, index, kernel_panic_bytes ))
		{
			kernel_crash_log_found = true;
		}
		
		return ( kernel_crash_log_found );
	}

	// routine to scan for the end of a chunk of the kernel crash file.
	private boolean ScanForEndOfChunk( byte[] crash_log, int index ) {
		
		boolean chunk_end_found = false;
		
		if ( match_string( crash_log, index, kernel_crash_term ))
		{
			chunk_end_found = true;
		}
		
		return ( chunk_end_found );
	}	
	
	private boolean match_string( byte[] array_to_match, int index_to_match, byte[] byte_str_to_match )
	{
		boolean string_matched = true;
		byte[] test_data = byte_str_to_match;
	
		for ( int i = 0; ( i < test_data.length ) && ( string_matched ); i++ )
		{
			if ( array_to_match[ index_to_match + i ] != test_data[i] )
			{
				string_matched = false;
			}
		}
		
		return( string_matched );
	}
	
	private void BuildAndStoreMessage( String FileToScan, PANIC_ID panic_id, int id, int dsp_version, byte[] kernel_crash_log, long crash_timestamp, boolean reason_found ) {
		
		APRDebug.APRLog( TAG, "Building and Sending APR Message." );
		
		KernelCrash_BuildMessage( kernel_crash_log, reason_found );
		system_apr_manager.APRMgrStoreEntry( FileToScan, panic_id, panic_id.id, dsp_version, kernel_crash_log, crash_timestamp, null );
	}
	
	private void KernelCrash_BuildMessage( byte[] kernel_crash_log, boolean reason_found ) {
		
		// blank out the log as it exists so far.
		java.util.Arrays.fill( kernel_crash_log, (byte) 0 );
		
		// the values may be zero. always create the kernel crash
		//    log in exactly the same way.  even if we couldn't get
		//    a pc or lr value.
		byte[] pc_array = intToByteArray( pc_value, true );
		byte[] lr_array = intToByteArray( lr_value, true );

		System.arraycopy( pc_array, 0, kernel_crash_log, 0, 4);
		System.arraycopy( lr_array, 0, kernel_crash_log, 4, 4);
	
		// append the reason with whatever space is left.
		if ( reason_found ) 
		{
			System.arraycopy( reason_byte_array, 0, kernel_crash_log, 8, 
					( ( kernel_crash_log.length - 8 < reason_byte_array.length )?
					    kernel_crash_log.length - 8:reason_byte_array.length ) );
		}
	}
	
}
