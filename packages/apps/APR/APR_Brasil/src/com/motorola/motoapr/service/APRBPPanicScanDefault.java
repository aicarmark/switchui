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

public class APRBPPanicScanDefault {
	
	static String TAG = "APRBPPanicScanDefault";

	static private byte[] raw_data = null;
	
	APRBPPanicScanDefault( byte[] crash_log )
	{
		raw_data = crash_log.clone();
	}
	
	public int GetPanicID()
	{
		// by default, the panic id is in the 0th position,
		//   with byte reversal required.
		return( PanicIdFromRaw( 0, true ) );
	}
	
	/**
	 * in the default case this is actually a meaningless API.
	 *   however, it is defined here as a base for derivative
	 *   classes to use.
	 * @return
	 */
	public int GetVersionNumber( boolean reverse_bytes )
	{
		return( VersionFromRaw( reverse_bytes ));
	}
	
	/**
	 * In the default case, we do not know what the offset is
	 *   for the raw data. Therefore, just grab the first 50 
	 *   bytes.  
	 * @return
	 */	
	public byte[] GetRawData()
	{
		return( RawDataFromRaw( 0, 50, false));
	}
	
	public int GetDSPVersion()
	{
		// as as default, attempt to get the DSP verison.
		// byte[] raw_dsp_version = RawDataFromRaw( 8, 3, false);
		
		return( 0 );
	}
	
	/**
	 * Generic API to get the version number from
	 *   the raw data.  This API defines the version
	 *   number as the 4 bytes following the initial
	 *   5 bytes of marker string.
	 * @param byte_reverse
	 * @return
	 */
	protected int VersionFromRaw( boolean byte_reverse ) 
	{
		int APR_BP_PANIC_VERSION_OFFSET=5;
		
		return GetIntField( APR_BP_PANIC_VERSION_OFFSET, byte_reverse );
	}
	
	protected int GetIntField( int offset, boolean byte_reverse )
	{
		int return_field = 0;
		int FIELD_SIZE = 4;
						
		// Here panic_id is the first 32 bits of the bp_panic.binX file
		//   Byte reverse to get it right on the server. 
		for(int i =0; i < FIELD_SIZE; i++)
		{
			return_field <<= 8;
			
			if ( byte_reverse )
			{
				return_field ^= (long)raw_data[(offset+FIELD_SIZE-1)-i] & 0xFF;
			}
			else
			{
				return_field ^= (long)raw_data[offset+i] & 0xFF;
			}
		}
		
		return return_field;
	}
	
	/**
	 * Routine to extract some raw data from the byte array.
	 * @param offset - offset into the crash log
	 * @param length - length of the field to get.
	 * @param byte_reverse - indicates whether to byte reverse the data or not.
	 * @return
	 */
	protected byte[] RawDataFromRaw( int offset, int length, boolean byte_reverse )
	{
		// limit the length, in case we get the byte-reverse
		//   detail incorrect.
		if ( length > 50 ) length = 50;
		
		// just a clone to create memory.
		byte[] return_raw_data = new byte[ length ];
		
		// limit the length.  can never be more than what we have.
		if ( length + offset > raw_data.length )
		{
			length = ( raw_data.length - offset );
		}
		
		if ( ( length + offset ) > 0 )
		{
			for( int i = 0; i < length; i++ )
			{
				if ( !byte_reverse )
				{
					return_raw_data[i]= raw_data[ i + offset ];
				}
				else
				{
					return_raw_data[i]= raw_data[ ( offset + length ) - i ];
				}
			}
		}
		else
		{
			return_raw_data = null;
		}
		
		return( return_raw_data );
	}
	
	/**
	 * Get the panic id from raw data.  Which really means, get 
	 *   a 4 byte field from the raw data.
	 * @param offset
	 * @param byte_reverse
	 * @return
	 */
	protected int PanicIdFromRaw( int offset, boolean byte_reverse )
	{
		byte bp_panic_id[] = new byte[4];
		int bp_panic_id_l = 0;

		for(int i=0; i <4; i++)
		{	
			bp_panic_id[i] = raw_data[i+offset];
		}
		
		String s_debug = new String();
		
		for (int i=0;i<raw_data.length;i++)
		{
			s_debug = s_debug + String.format("%02x", raw_data[i]);
		}
		
		APRDebug.APRLog( TAG, "BP Panic ID: " + 
				String.format("%02x", bp_panic_id[3]) + 
				String.format("%02x", bp_panic_id[2]) + 
				String.format("%02x", bp_panic_id[1]) + 
				String.format("%02x", bp_panic_id[0]) );

		// Here panic_id is the first 32 bits of the bp_panic.binX file
		//   Byte reverse to get it right on the server. 
		for(int i =0; i < 4; i++)
		{
			bp_panic_id_l <<= 8;
			
			if ( byte_reverse )
			{
				bp_panic_id_l ^= (long)bp_panic_id[3-i] & 0xFF;
			}
			else
			{
				bp_panic_id_l ^= (long)bp_panic_id[i] & 0xFF;
			}
		}
		
		return( bp_panic_id_l );
	}
}
