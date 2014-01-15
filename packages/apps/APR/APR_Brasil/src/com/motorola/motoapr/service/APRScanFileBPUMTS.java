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

public class APRScanFileBPUMTS extends APRScanFileBPPanicDefault {

	static String TAG = "APRScanFileBPUMTS";
	
	int version_number = 0;
	
	// Version Based Defines
	 
	// to create a new header version, you must first copy everything here.
	//   subsequently, you must never modify the tech type nor header version
	//   fields, or the behavior of this architecture will fail.
	
	// Begin Version 1.0
	static boolean VERSION_1_REVERSE_BYTES = false;
	static int TECH_TYPE_OFFSET_1 = 0;
	static int TECH_TYPE_LENGTH_1= 5;
	static int HEADER_VERSION_OFFSET_1 = TECH_TYPE_LENGTH_1+TECH_TYPE_OFFSET_1;
	static int HEADER_VERSION_LENGTH_1 = 4;
	static int BP_PANIC_OFFSET_1 = HEADER_VERSION_OFFSET_1+HEADER_VERSION_LENGTH_1;
	static int BP_PANIC_LENGTH_1 = 4;
	static int DSP_VERISON_OFFSET_1 = BP_PANIC_OFFSET_1+BP_PANIC_LENGTH_1;
	static int DSP_VERSION_LENGTH_1 = 23;
	static int BP_RAW_DATA_LEN_OFFSET_1 = DSP_VERISON_OFFSET_1 + DSP_VERSION_LENGTH_1;
	static int BP_RAW_DATA_LEN_LENGTH = 4;
	static int BP_RAW_DATA_OFFSET = BP_RAW_DATA_LEN_OFFSET_1 + BP_RAW_DATA_LEN_LENGTH;
	// End Version 1.0 
	
	APRScanFileBPUMTS(byte[] crash_log) {
		super(crash_log);
		
		version_number = GetVersionNumber();
		
		APRDebug.APRLog(TAG, "UMTS Panic File Version:" + version_number );
	}

	/**
	 * in the default case this is actually a meaningless API.
	 *   however, it is defined here as a base for derivative
	 *   classes to use.
	 * @return
	 */
	public int GetVersionNumber()
	{
		return super.GetVersionNumber( VERSION_1_REVERSE_BYTES );
	}
	
	public int GetPanicID()
	{
		int return_panic_id = 0;
		
		switch( version_number )
		{
		case 1: 
			return_panic_id=PanicIdFromRaw( BP_PANIC_OFFSET_1, VERSION_1_REVERSE_BYTES );
			break;
		default:
			return_panic_id=super.GetPanicID();
		}
		
		// by default, the panic id is in the 0th position,
		//   with byte reversal required.
		return( return_panic_id );
	}
	
	public int GetDSPVersion()
	{
		String dsp_version_string = null;
		int return_dsp_version = 0;
		int major_digits_int = 0;
		int first_digits_int = 0; 
		int mid_digits_int = 0;
		int last_digits_int = 0;
		int last_digit_index = 0; 
		
		switch( version_number )
		{
		case 1:
			byte[] raw_dsp_version = RawDataFromRaw( DSP_VERISON_OFFSET_1, DSP_VERSION_LENGTH_1, VERSION_1_REVERSE_BYTES);
			
			int trunk_length = raw_dsp_version.length;
			
			dsp_version_string = new String( raw_dsp_version );
			
			for ( int i = raw_dsp_version.length - 1; i > 0; i-- )
			{
				if ( raw_dsp_version[i]==0 )
				{
					trunk_length = i;
				}
			}
			
			try {
				dsp_version_string = dsp_version_string.substring(0,trunk_length);

				if ( dsp_version_string != null )
				{
					last_digit_index = dsp_version_string.lastIndexOf('.');

					if ( last_digit_index > 0 )
					{
						String last_digits = dsp_version_string.substring(last_digit_index+1);
						dsp_version_string = dsp_version_string.substring(0,last_digit_index);
						last_digits_int = Integer.parseInt( last_digits.trim(), 16 );
					}
					else
					{
						// this is the last digits available.
						last_digits_int = Integer.parseInt( dsp_version_string.trim(), 16 );
						dsp_version_string = null;
					}
					
				}
				
				if ( dsp_version_string != null )
				{
					last_digit_index = dsp_version_string.lastIndexOf('.');

					if ( last_digit_index > 0 )
					{
						String mid_digits = dsp_version_string.substring(last_digit_index+1);
						dsp_version_string = dsp_version_string.substring(0,last_digit_index);
						mid_digits_int = Integer.parseInt( mid_digits.trim(), 16 );
					}
					else
					{
						// this is the last digits available.
						mid_digits_int = Integer.parseInt( dsp_version_string.trim(), 16 );
						dsp_version_string = null;
					}
				}
			
				if ( dsp_version_string != null )
				{
					last_digit_index = dsp_version_string.lastIndexOf('.');
					
					if ( last_digit_index > 0 )
					{
						String first_digits = dsp_version_string.substring(last_digit_index+1);
						dsp_version_string = dsp_version_string.substring(0,last_digit_index);
						first_digits_int = Integer.parseInt( first_digits.trim(), 16 );
					}
					else
					{
						// this is the last digits available.
						first_digits_int = Integer.parseInt( dsp_version_string.trim(), 16 );
						dsp_version_string = null;
					}
				}
				
				if ( dsp_version_string != null )
				{
					last_digit_index = dsp_version_string.lastIndexOf('.');

					if ( last_digit_index > 0 )
					{
						String major_digits = dsp_version_string.substring(last_digit_index);
						dsp_version_string = dsp_version_string.substring(0,last_digit_index-1);
						major_digits_int = Integer.parseInt( major_digits.trim(), 16 );
					}					
					else
					{
						// this is the last digits available.
						major_digits_int = Integer.parseInt( dsp_version_string.trim(), 16 );
						dsp_version_string = null;
					}
				}
			
			} catch ( Exception e )
			{
				APRDebug.APRLog( TAG, "Failed some portion of DSP Version capture." );
				return_dsp_version = 0;
			}
			
			return_dsp_version = ( ( ( major_digits_int & 0xFF ) << 24 ) |
					               ( ( first_digits_int & 0xFF ) << 16 ) | 
		               			   ( ( mid_digits_int & 0xFF ) << 8 )    | 
		               			   (   last_digits_int & 0xFF ) );
			break;
		default:
			super.GetDSPVersion();
		}
		
		return ( return_dsp_version );
	}

		
	/**
	 * In the default case, we do not know what the offset is
	 *   for the raw data. Therefore, just grab the first 50 
	 *   bytes.  
	 * @return
	 */	
	public byte[] GetRawData()
	{
		byte[] return_raw_data = null;

		
		switch( version_number )
		{
		case 1:		
			int raw_data_length = GetIntField( BP_RAW_DATA_LEN_OFFSET_1, VERSION_1_REVERSE_BYTES );
			
			// it doesn't make sense to reverse the bytes for raw data.
			return_raw_data = RawDataFromRaw( BP_RAW_DATA_OFFSET, raw_data_length, false );
			break;
		default:
			return_raw_data = super.GetRawData();
			break;
		}
		
		return( return_raw_data );
	}

	
}
