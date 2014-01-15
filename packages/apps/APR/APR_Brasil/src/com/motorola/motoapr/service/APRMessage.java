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

import org.apache.http.util.EncodingUtils;

import android.content.Context;
import android.database.Cursor;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.AndroidRuntimeException;
import android.util.Log;


// /* IMPORTANT - update structure_version with any changes (for parsing) */
//typedef struct
//{
// PANIC_APP_MESSAGE_ID_T      id;
// UINT8                       structure_version;
// UINT8                       bldinfo_baselabel[PANIC_APP_SW_VERSION_LENGTH];
// UINT8                       flex_version[PANIC_APP_FLEX_VERSION_LENGTH];
// UINT8                       dsp_version[PANIC_APP_DSP_VERSION_LENGTH];
// UINT8                       kb_remain; 
// UINT8                       totalPanics; 
// SU_TIME                     up_time;
// UINT16                      blddetails_crc;
// UINT8                       barcode[NVM_FACTORY_SERIAL_NUMBER_LENGTH];
// /* 64 bytes, that leaves up to 48 bytes for the sub structures */
// }  MESSAGE_CONTENT_T;

/**
 * Class to define the base message type.
 */
public class APRMessage {

	static String TAG = "APRMessage";
		
	public APRPanicDatabase PanicDB;
	
	static TelephonyManager telephony_manager = null; 

	/* APR Message Type */
	static final byte PANIC_APP_PANIC_REPORT_MESSAGE_ID            = 0;
	static final byte PANIC_APP_SW_FLEX_CHANGE_IND_MESSAGE_ID      = 1;
	static final byte PANIC_APP_PERIODIC_REPORT_MESSAGE_ID         = 2;
	static final byte PANIC_APP_PERIODIC_CALL_DROP_MESSAGE_ID      = 10;
	static final byte PANIC_APP_PERIODIC_3G_CALL_DROP_MESSAGE_ID   = 11;

    // these values must be used for 3GSM.
	static final byte PANIC_APP_PANIC_REPORT_MSG_STRUCT_VERSION    = 2;
	static final byte PANIC_APP_SW_FLEX_CHANGE_MSG_STRUCT_VERSION  = 2;
	static final byte PANIC_APP_PERIODIC_MSG_STRUCT_VERSION        = 3;

    // these values must be used for CDMA.
    //	static final byte PANIC_APP_PANIC_REPORT_MSG_STRUCT_VERSION    = 1;
    //	static final byte PANIC_APP_SW_FLEX_CHANGE_MSG_STRUCT_VERSION  = 1;
    //	static final byte PANIC_APP_PERIODIC_MSG_STRUCT_VERSION        = 1;

	static final byte  PANIC_APP_SW_VERSION_LENGTH                 = 20;
	static final byte  PANIC_APP_HW_VERSION_LENGTH                 = 5; 
	static final byte  PANIC_APP_FLEX_VERSION_LENGTH               = 20; 
	static final byte  PANIC_APP_DSP_VERSION_LENGTH                = 4;
	static final byte  PANIC_APP_GENERIC_PANIC_DATA_LENGTH         = 36;
	static final byte  PANIC_APP_PERIODIC_REPORT_DATA_LENGTH       = 24;
	
	static final byte  NVM_FACTORY_SERIAL_NUMBER_LENGTH            = 10;
	static final int APR_NUM_MESSAGE_CONTENT_ITEMS = 10;
	
	static byte             id;
	
	static final String APR_DEFAULT_FLEX_VALUE = "FLEX_N_21.xx.xxI_GIT";
	static final String APR_DEFAULT_BARCODE_VALUE = "LF11111111";
	
	byte             structure_version = 0;
	
	String           bldinfo_baselabel = "Sholes1.0"; // [PANIC_APP_SW_VERSION_LENGTH];
	
	String           flex_version = "SholesFlex1.0"; //[PANIC_APP_FLEX_VERSION_LENGTH];
		
	byte             kb_remain = 10;

	byte             totalPanics = 12;
	
	static long      reported_uptime = 0;
	
	// int, to hold unsigned short.
	int              blddetails_crc = 0; //0x0182
	
	String           barcode = "UniquePhoneID"; // [NVM_FACTORY_SERIAL_NUMBER_LENGTH];
	
	String sf_last_bldinfo_baselabel = "Sholes1.0";
	
	String sf_last_flex_version = "SholesFlex1.0";
	
	int              sf_last_blddetails_crc = 0;

	byte[] BaseMessage = null;

	static Context mContext = null;
	
	Cursor APRPanicCursor = null;
	
	/**
	 * Constructor for base APR Message
	 * @param context current context from which we're executing
	 * @param message_id message id for the message type we're creating.
	 */
	APRMessage( Context context ) {
		
		mContext = context;
		
		// Create the Panic Database
		PanicDB = new APRPanicDatabase( context );
		
		APRMessage_Restart();
	}
		
	public void APRMessage_Restart() {
		
		// NOTICE: APR must function even if the phone isn't programmed
		//   correctly, and the server doesn't accept bad values.  
		//   Therefore, good, default values must be here
		//   for each and every one of these entries.  Blindly getting
		//   a value from the system settings is invalid therefore, because
		//   the system settings may not be programmed correctly.  Instead
		//   each value must be verified, and if invalid, a default, corrected
		//   value, returned.
		
		// get a valid software version, even if it doesn't match the phone.
		bldinfo_baselabel = APRMessageGetCurrentBaselabel();
		
		flex_version = APRMessageGetAndSetCurrentFlexVersion();
				
		barcode = APRMessageGetAndSetCurrentBarcode();
		
		// Compute the CRC!
		blddetails_crc = APRMessageGetBuildCRC();
	APRDebug.APRLog( TAG, "cm: APRMessage_Restart: " + bldinfo_baselabel + ", " + flex_version + ", " + barcode + ", " + blddetails_crc);
	}
	
	public byte[] AppendByteToBase( byte append_byte ) {
		return AppendByte( BaseMessage, append_byte );
	}
		
	/**
	 * get the software version of THIS release, or 
	 *   return a fake value if it fails.
	 * added support for debug versus official releases.
	 *   add a "D" to the version string for a debug version
	 *   so we can track this separately.  official versions
	 *   are not debuggable.
	 * @return String
	 */
	static String APRMessageGetCurrentBaselabel()
	{
		String return_baselabel = null;
		String debuggable = null;
		
		try {
			// get the base-label from the currently loaded baseline.
			return_baselabel = SystemProperties.get( "ro.build.display.id", "OLYEV_U4_00.0");

			if ( return_baselabel.contains( "VZW" ) ) {
				if ( return_baselabel.length() < 4 ) {
					// we have a whacked out verizon build version.

					String incremental_version = SystemProperties.get( "ro.build.version.incremental", "0.0.0" );
					String product_model = SystemProperties.get ( "ro.product.model", "DROID2" );

					return_baselabel = new String( product_model + "_" + incremental_version );
				}
			}
		
			// get the debugggable state of the build.
			debuggable = SystemProperties.get( "ro.build.type", "user" );
		
			// if it's debuggable, append a D to it.  Not the same as an official release.
			if ( debuggable.contains( "userdebug") )
			{
		//		return_baselabel = return_baselabel + "D";
			}
			
		} catch ( Exception e ) {
			
			if ( return_baselabel != null )
			{
				// must have failed to get debuggable information.
				APRDebug.APRLog( TAG, "Postfix Not Determined Properly." );
			}
			
		} finally {

			// create a safe new string.
			return_baselabel = new String( return_baselabel );
		}
		
	//	APRDebug.APRLog( TAG, "SW Version:" + return_baselabel );
		
		return( return_baselabel );
	}
	
	public static String CurrentBaselabel()
	{
		return( APRMessageGetCurrentBaselabel() );
	}
	
	public static Long ReportedUptime()
	{
		return( reported_uptime );
	}

	/**
	 * get the flex version programmed
	 * @return String
	 */	
	static String APRMessageGetCurrentFlexVersion()
	{
		// get the default flex version.
		String return_flex_version = APRPreferences.APRPrefsGetDefaultFlexVersion();

		String hardware_type = SystemProperties.get( "ro.hardware", "mapphone_cg" );
		if(hardware_type.contains("cdma")) {//single cdma phone
			return_flex_version = SystemProperties.get( "gsm.version.baseband", "CDMA_BP_03.00.00RSCP" ); 
			return_flex_version += "SCP";
			return( return_flex_version );
		}
				
		try
		{		
			if ( APRPreferences.APRServiceCheckProp( "ro.gsm.flexversion" ) )
			{
				return_flex_version = SystemProperties.get( "ro.gsm.flexversion", APR_DEFAULT_FLEX_VALUE );
			} 
			else if ( APRPreferences.APRServiceCheckProp( "ro.flexversion" ) )
			{
				return_flex_version = SystemProperties.get( "ro.flexversion", APR_DEFAULT_FLEX_VALUE );
			}
			else if ( APRPreferences.APRServiceCheckProp( "ro.cdma.flexversion" ) )
			{
				return_flex_version = SystemProperties.get( "ro.cdma.flexversion", APR_DEFAULT_FLEX_VALUE );
			}
			else if ( APRPreferences.APRServiceCheckProp( "ro.build.version.full" ) )
			{
				return_flex_version = SystemProperties.get( "ro.build.version.full", APR_DEFAULT_FLEX_VALUE );
				
				// if the build version is a blur version, use that for flex.
				//   otherwise, null, and it's back to the default flex.
				if ( !return_flex_version.contains( "Blur" ) )
				{
					return_flex_version = null;
				}
			}
			
				
			// check the flex version...
			if ( ( return_flex_version == null ) || ( return_flex_version.length() < 5 ) )
			{
				return_flex_version = APR_DEFAULT_FLEX_VALUE;
			}

		} catch ( AndroidRuntimeException e ) {
			return_flex_version = APR_DEFAULT_FLEX_VALUE;
		}

	APRDebug.APRLog( TAG, "cm: return_flex_version finally: " + return_flex_version );
		return( return_flex_version );
	}
	
	private static String APRMessageGetAndSetCurrentFlexVersion()
	{
		String flex_version = null;
		
		// get a valid flex version, even if it doesn't match the phone.
		flex_version = APRMessageGetCurrentFlexVersion();
		
		if ( !flex_version.contains( APR_DEFAULT_FLEX_VALUE ))
		{
			// getting the current flex version should never be the same as setting it.
			// but now we're setting it each time we call this.
			APRPreferences.APRPrefsSetDefaultFlexVersion( flex_version );
		}

		return( flex_version );
	}
	
	public static Boolean APRMessageGoodFlexVersion()
	{
		Boolean return_result = false;
		
		String return_flex_version = null;
		
		return_flex_version = APRMessageGetCurrentFlexVersion();
		
		if ( !return_flex_version.contentEquals( APR_DEFAULT_FLEX_VALUE ))
		{
			return_result = true;
		}
		
		return( return_result );
	}
	
	/**
	 * get the barcode of THIS phone or return a default value.
	 *   It is EXTREMELY important to get the barcode field right
	 *   or all the messages to the server will get rejected.  It 
	 *   must be alpha numeric, it must be unique to this phone, and
	 *   it must be all upper case
	 */

	static String APRMessageGetCurrentBarcode()
	{
		String return_barcode = APRPreferences.APRPrefsGetDefaultBarcode();

		try {
            // Get barcode from device ID
            if ( !CheckBarcode( return_barcode ) ) {
                if (mContext != null) {
                    telephony_manager = (TelephonyManager) mContext.getSystemService( Context.TELEPHONY_SERVICE);
                    return_barcode = telephony_manager.getDeviceId();
                }
                int deviceID_length = 0;
                if (return_barcode != null) {
                    deviceID_length = return_barcode.length();
                }

                if ( deviceID_length > 10 ) {
                    return_barcode = return_barcode.substring(deviceID_length-10);
                }
            }



			// keep trying to get a good barcode, if this routine is called again.
			if ( !CheckBarcode( return_barcode ) ) {
         //       APRDebug.APRLog( TAG, "Get Barcode...");

				// attempt to override with the real barcode from the phone.
				return_barcode = SystemProperties.get( "ro.ril.barcode", APR_DEFAULT_BARCODE_VALUE );

				if ( return_barcode.contentEquals( APR_DEFAULT_BARCODE_VALUE ) ) {

					return_barcode = SystemProperties.get( "ro.cdma.barcode", APR_DEFAULT_BARCODE_VALUE );

					if ( return_barcode.contentEquals( APR_DEFAULT_BARCODE_VALUE ) )
					{
						return_barcode = SystemProperties.get( "ro.gsm.barcode", APR_DEFAULT_BARCODE_VALUE );

                        // some products use serial number for the same information.
						if ( return_barcode.contentEquals( APR_DEFAULT_BARCODE_VALUE ) )
						{
                      //      APRDebug.APRLog( TAG, "Get Serial No as Barcode...");
							return_barcode = SystemProperties.get( "ro.serialno", APR_DEFAULT_BARCODE_VALUE );

							if ( return_barcode.contentEquals( APR_DEFAULT_BARCODE_VALUE ) )
							{
								return_barcode = SystemProperties.get( "ro.barcode", APR_DEFAULT_BARCODE_VALUE );
							}
						}
					}
				}

				// check the flex version...
				if ( return_barcode.length() < 5 )
				{
					return_barcode = APR_DEFAULT_BARCODE_VALUE;
				}
			}
			
		} catch ( AndroidRuntimeException e ) {
			return_barcode = APR_DEFAULT_BARCODE_VALUE;
		}
		
            APRDebug.APRLog( TAG, "APRMessageGetCurrentBarcode .return_barcode = " + return_barcode);
		return( return_barcode );
	}
	
	
	public static String CurrentBarcode()
	{
		return( APRMessageGetAndSetCurrentBarcode() );
	}
	
	private static String APRMessageGetAndSetCurrentBarcode()
	{
		String barcode = null;
		
		barcode = APRMessageGetCurrentBarcode();
		
		if ( !barcode.contains( APR_DEFAULT_BARCODE_VALUE ))
			{
				APRPreferences.APRPrefsSetDefaultBarcode( barcode );
			}
		
		return( barcode );
	}
	
	private static Boolean CheckBarcode( String barcode ) {

		Boolean return_result = false;

		if ( (barcode != null) && (barcode.length() >=5) ) {

			if ( !barcode.contentEquals( APR_DEFAULT_BARCODE_VALUE ) &&
				 !barcode.contentEquals( "L000000000") ) {
					return_result = true;
				}
		}

		return ( return_result );
	}

	public static Boolean APRMessageGoodBarCode()
	{
		String return_barcode = null;

		return_barcode = APRMessageGetCurrentBarcode();

		return( CheckBarcode( return_barcode ) );
	}


	/**
	 * Get the CRC for this version of this build.
	 *   It will change every time the code is built.
	 * @return
	 */
	
	// for debug
	static String crc_generation_string = null;
	static String user_name = null;
	static String build_date = null;
	static String crc_software_version = null;
	
	static int APRMessageGetBuildCRC() {
		int return_crc = 0;
		
		crc_generation_string = new String();
		
		user_name = SystemProperties.get( "ro.build.user", "000000");
		
		if ( user_name.length() < 2 )
		{
			user_name = "000000";
		}
		
		build_date = SystemProperties.get( "ro.build.date.utc", "1242867772");
		
		if ( build_date.length() < 2 )
		{
			build_date = "1242867772"; 
		}
		
		crc_software_version = APRMessageGetCurrentBaselabel();
		
	//	APRDebug.APRLog( TAG, "Building CRC:" + user_name + " " + build_date + " " + crc_software_version);
		
		crc_generation_string = user_name + " " + build_date + " " + crc_software_version;
		
		return_crc = APRCrc16( crc_generation_string );
		
		//APRDebug.APRLog( TAG, "CRC Result:" + Integer.toHexString( return_crc ) );
		
		return( return_crc );
	}


	public static int BuildCRC()
	{
		return( APRMessageGetBuildCRC() );
	}
	
	static int fax_util_gentab[] =
	{
	    0x0000, 0x1081, 0x2102, 0x3183,
	    0x4204, 0x5285, 0x6306, 0x7387,
	    0x8408, 0x9489, 0xa50a, 0xb58b,
	    0xc60c, 0xd68d, 0xe70e, 0xf78f
	};

	/**
	 * Generate a CRC.
	 * @param crc_generation_string
	 * @return
	 */
	static int APRCrc16( String crc_generation_string )
	{   
	    int crc,temp;
	    char[] crc_input_array = crc_generation_string.toCharArray();
	    char c;
	    
	    crc = 0xffff;   /* preset all ones      */
	    for ( int i = 0; i < crc_generation_string.length(); i ++ )
	    {
	    	// c = (WORD)*ptr++;
	    	
	    	c = crc_input_array[i];
	  
	        temp = (((crc & 0xff) ^ c) & 0x0f);
	        
	        temp = fax_util_gentab[temp];
	        
	        crc = (crc >> 4) ^ temp;
	        c >>= 4;
	        temp = ((crc & 0xff) ^ c) & 0x0f;
	        temp = fax_util_gentab[temp];
	        crc = (crc >> 4) ^ temp;
	        crc &= 0xffff;
	    }
	    
	    return((~crc) & 0xffff);
	}

	
	
	/**
	 * Function to reset the BaseMessage in order to create a new one
	 * 
	 * @param nothing
	 * @throws BuildExceptionError 
	 */
	void APRCleanupBaseMessage(byte message_id, byte structure_version, int dsp_version ) throws BuildExceptionError
	{
		BaseMessage = null;
		
		//mContext should not be null
		if(mContext != null)
		{
			try
			{	
				// try to move this up, so we only attempt to get the 
				//  telephony manager once.  this works for now, here.
				telephony_manager = (TelephonyManager) 
					mContext.getSystemService( Context.TELEPHONY_SERVICE);
				
				// Update All the message contents
				APRUpdateAllData( mContext );
				APRDebug.APRLog(TAG, "APRCleanupBaseMessage before buildbasemessage");
			
				// Build the base message into a Container.
				APRBuildBaseMessage( message_id, structure_version, dsp_version );
				
			} catch ( RuntimeException e )
			{
				APRDebug.APRDebugStack(e);
			}
		}
		else
		{
			APRDebug.APRLog(TAG, "mContext should not be null in APRCleanupBaseMessage function");
		}
	}
	
/**
 * Build the base APR Message
 * @param id
 * @param structure_version
 * @throws BuildExceptionError 
 */
	
	void APRBuildBaseMessage( byte id, byte structure_version, int dsp_version ) throws BuildExceptionError {

		APRDebug.APRLog( TAG, "APRBuildBaseMessage Constructor barcode = " + barcode);
		
		if ( structure_version != 0 ) {
			
			// always start with a clean base message.
			BaseMessage = null;		
					
			BaseMessage = AppendByte   ( BaseMessage, id );  // size = 1
			BaseMessage = AppendByte   ( BaseMessage, structure_version ); // size = 2 (1 byte)
			BaseMessage = AppendString ( BaseMessage, bldinfo_baselabel, PANIC_APP_SW_VERSION_LENGTH ); // size == 22 (20 bytes) 
			BaseMessage = AppendString ( BaseMessage, flex_version, PANIC_APP_FLEX_VERSION_LENGTH ); // size == 42 (20 bytes) 
			BaseMessage = AppendInt    ( BaseMessage, dsp_version, true ); // size == 46 (4 bytes)
			BaseMessage = AppendByte   ( BaseMessage, kb_remain ); // size = 47 (1 bytes)
			BaseMessage = AppendByte   ( BaseMessage, totalPanics ); // size = 48 ( 1 bytes)
			BaseMessage = AppendInt    ( BaseMessage, reported_uptime, true ); // size = 52 ( 4 bytes ) // uptime, bytes in reverse order.
			BaseMessage = AppendShort  ( BaseMessage, blddetails_crc, true ); // size = 54 ( 2 bytes )  //value not reported
			BaseMessage = AppendString ( BaseMessage, barcode, NVM_FACTORY_SERIAL_NUMBER_LENGTH ); // size = 64 ( 10 bytes ) Warning about the ascii to decimal conversion
		
			//String log_string = new String(BaseMessage,0,BaseMessage.length);
			String log_string = new String();
			for ( int i = 0; i < BaseMessage.length; i ++ ) {
				log_string = log_string + String.format("%02x",BaseMessage[i]);
			}
		
			APRDebug.APRLog ( TAG, "BaseMessage:" + log_string );
		}
		else
		{
			APRDebug.APRLog ( TAG, "Failure to build base message.");
		}
		
	} // end BuildBaseMessage.
	
	/**
	 *  APR Build up an updated message.
	 * @throws BuildExceptionError 
	 */
	void APRMessageUpdatedMessage( byte message_id, byte structure_version, int dsp_version ) throws BuildExceptionError {
		
		// Update the uptime calculation we're reporting to the server.
		reported_uptime = APRGetUptimeCalculation();
		APRDebug.APRLog ( TAG, "APRMessageUpdatedMessage before APRBuildBaseMessage");		
		// Build the Base Message.
		APRBuildBaseMessage( message_id, structure_version, dsp_version );
	}

	/**
	 * Update all of the data in an APR base message
	 * @param message_id data to update the id field.
	 */
	void APRUpdateAllData( Context context ) {
	
		// Update the uptime calculation we're reporting to the server.
		reported_uptime = APRGetUptimeCalculation();

		// Get New cursor to access the data.
		APRPanicCursor = PanicDB.GetDBCursor();
	
		if ( APRPanicCursor != null  ) 
		{	
			//totalPanics is not having the right value as it's taking only the 
			//pending panic reports waiting to be sent
			totalPanics = (byte) APRPanicCursor.getCount();
			APRPanicCursor.deactivate();
			APRPanicCursor.close();
		}
		else
		{
			totalPanics = 0;
		}
	}
	
	
	static long previous_realtime = 0;
	static long current_realtime = 0;

	/**
	 * Update the uptime... only.
	 */
	public static void APRUpdateUptime()
	{
		// recalculate and store uptime calc.
		long last_recorded_uptime = 0;
		long current_uptime = 0;
		long slippage_since_last_record = 0;
		
		current_realtime = android.os.SystemClock.elapsedRealtime();
		
		// limit update calculations, since we are about to add UpdateDelay, regardless.
		//    This limits the effect of spurious calls to APRUpdateUptime(), which may be
		//    happening due to extra timer expirations.  we don't want to adjust APRService
		//    though: we need to ensure this timer goes off consistently.
		if ( ( current_realtime - previous_realtime ) > APRPeriodicMessageAndTimer.APRGetUpdateDelay() ) {
			previous_realtime = current_realtime;
			
			APRDebug.APRLog( TAG, "Adjusting Uptime." );

			// get the current time based upon the last recorded uptime,
			//   and the uptime delay (the alarm clock going off).
			last_recorded_uptime = APRPreferences.APRPrefsGetTotalUptimeElapsed();
			current_uptime = last_recorded_uptime + APRPeriodicMessageAndTimer.APRGetUpdateDelay();
			
			// get the slippage based upon the current computed uptime value,
			//   and a hard look at the amount of time since the last record.
			slippage_since_last_record = getSlippage( current_uptime );
			current_uptime += slippage_since_last_record;
			
			APRPreferences.APRPrefsSetTotalTimeElapsed( current_uptime );
			
			// Recalculate and store time since last periodic message.
			long time_since_last_report = APRPreferences.APRPrefsGetTimeSinceLastReport();
			time_since_last_report += APRPeriodicMessageAndTimer.APRGetUpdateDelay() + slippage_since_last_record;
			APRPreferences.APRPrefsSetTimeSinceLastReport( time_since_last_report );		
		} else {
			APRDebug.APRLog( TAG, "Spurious Uptime Update Request!");
		}
	}
	
	// get the uptime calculated.  Only.
	private static long APRGetUptimeCalculation()
	{
		long last_recorded_uptime = 0;
		last_recorded_uptime = APRPreferences.APRPrefsGetTotalUptimeElapsed();		
		return ( last_recorded_uptime );
	}
	
	// the purpose of this routine, is, between calls, to measure slippage of realtime
	//  against uptime.  It will take in the current uptime value, and measure whether
	//  the uptime has increased more, or less, than realtime.  Then, to ensure our 
	//  adjustments are small, it will return a fixed +/or zero value that should be added
	//  to the current uptime.  It will not dynamically compute that value, since the
	//  concern is that we might adjust too much.  After the conclusion of the routine,
	//  all variables will be reset and restarted, so that the next time the routine is called,
	//  a fresh start, and new decision will be made against the next block of slippage.
	static private long getSlippage( long current_uptime ) {
		
		long return_slippage = 0;
		long prev_realtime = 0;
		long prev_uptime = 0;
		long current_realtime = 0;
		
		long uptime_delta = 0;
		long realtime_delta = 0;
		
		// get the previous realtime measurement.
		prev_realtime = APRPreferences.APRGetSlippageRealtimeValue();
		
		// get the previous uptime measurement.
		prev_uptime = APRPreferences.APRGetSlippageUptime();
		
		// get the current realtime measurement.
		current_realtime = android.os.SystemClock.elapsedRealtime();
		
		// get the current uptime measurement.
		
		// compute the delta.
		uptime_delta = current_uptime - prev_uptime;
		
		if ( uptime_delta < 0 ) uptime_delta = 0;
		
		realtime_delta = current_realtime - prev_realtime;

		// realtime cannot go negative
		if ( realtime_delta < 0 ) realtime_delta = 0;
		
		// if the delta of uptime, exceeds the delta of realtime
		// indicate a zero return_slippage.  Since we exceed, 
		//    the APR Server can handle the excess. 
		
		// increment by two delay delta.  If we have slippage, we need to be able 
		//   to catch-up eventually.  At the moment, we assume no more than one
		//   cycle has been lost per intended cycle.  
		if ( realtime_delta > uptime_delta )
		{
			return_slippage = ( realtime_delta - uptime_delta );
				
			// compute slippage to return, but limit it to the uptime delay.
			if ( return_slippage > APRPeriodicMessageAndTimer.APRGetUpdateDelay() )
			{
				return_slippage = APRPeriodicMessageAndTimer.APRGetUpdateDelay();
			}
		} else {
			
			// allow negative slippage, for when uptime_delta is skipping ahead too fast.
			//   this should NEVER happen.
			if ( realtime_delta > uptime_delta ) {
				
				// compute slippage to return, but limit it to the uptime delay.
				if ( return_slippage < (-APRPeriodicMessageAndTimer.APRGetUpdateDelay()) )
				{
					return_slippage = -APRPeriodicMessageAndTimer.APRGetUpdateDelay();
				}				
			}
		}
		
		APRDebug.APRLog( TAG, "Slippage Added: " + return_slippage );
		
		// store the current realtime measurement.
		APRPreferences.APRSetSlippageRealtimeValue( current_realtime );
		APRPreferences.APRSetSlippageUptime( current_uptime );
		
		return( return_slippage );
	}

	
	/**
	 * AppendLong to base
	 * @param append_long
	 * @return
	 */
	public byte[] AppendLongToBase( long append_long ) {
		BaseMessage = AppendLong( BaseMessage, append_long, false );
		return BaseMessage;
	}
	public byte[] AppendShortToBase(short append_short, boolean reverse ) {
		BaseMessage = AppendShort( BaseMessage, append_short, reverse );
		return BaseMessage;
	}
	public byte[] AppendIntToBase(int append_int, boolean reverse ) throws BuildExceptionError {
		BaseMessage = AppendInt( BaseMessage, append_int, reverse );
		return BaseMessage;
	}
	
	public byte[] AppendToBase( long append_long, boolean reverse ) {
		BaseMessage = AppendLong( BaseMessage, append_long, reverse );
		return BaseMessage;
	}
	public byte[] AppendToBase(short append_short, boolean reverse ) {
		BaseMessage = AppendShort( BaseMessage, append_short, reverse );
		return BaseMessage;
	}
	public byte[] AppendToBase(int append_int, boolean reverse ) throws BuildExceptionError {
		BaseMessage = AppendInt( BaseMessage, append_int, reverse );
		return BaseMessage;
	}
	
	/**
	 * Append string of max length, to the base.
	 * @param append_string
	 * @param append_length the precise number of digits to append.
	 * @return
	 */
	public byte[] AppendStringToBase( String append_string, int append_length )
	{
		BaseMessage = AppendString( BaseMessage, append_string, append_length );
		
		return( BaseMessage );
	}
	public byte[] ArrayAppendToBase ( byte[] data_to_append, int max_append_length ){
		BaseMessage = ArrayAppend( BaseMessage, data_to_append, max_append_length );
		return( BaseMessage );
	}

	/**
	 * Append a byte (1 byte) to a byte array.
	 * @param baseMessage
	 * @param append_byte
	 * @return
	 */
	private static byte[] AppendByte(byte[] baseMessage, byte append_byte ) {
		
		byte[] new_array = null;
		int l1;
		int l2 = 1;
		
		if ( baseMessage == null )
		{
			l1 = 0;
			new_array = new byte[ l2 ];
			new_array[ l1 ] = append_byte;
		}
		else
		{
			l1 = baseMessage.length;
			new_array = new byte[ l1 + l2 ];
			System.arraycopy( baseMessage, 0, new_array, 0, l1 );
			new_array[ l1 ] = append_byte;
		}
			
		return( new_array );		
	}
	private static byte[] AppendInt(byte[] baseMessage, long append_int, boolean reverse ) throws BuildExceptionError {
		
		byte[] new_array = null;
		int l1 = baseMessage.length;
		int l2 = 4;
		
		int i;
		
		new_array = new byte[ l1 + l2 ];
		
		if ( new_array.length != l1 + l2 ) {
			Log.e( TAG, "Failure To Allocate Byte Array" );
			
			// try again.
			new_array = new byte[ l1 + l2 ];
		}
		
		System.arraycopy( baseMessage, 0, new_array, 0, l1 );
		
		// append the four bytes
		for ( i=0; i<l2; i++ )
		{
			if ( reverse ) {
				new_array[ (l1+l2-1)-i ] = (byte) ( append_int & 0xff );
			}
			else
			{
				new_array[ l1+i ] = (byte) ( append_int & 0xff );
			}
			
			append_int = append_int >>> 8;
		}
		
		baseMessage = new_array;
		
		if ( new_array.length != l1 + l2 ) {
			Log.e( TAG, "Failure To Append Int!!" );
			Log.e( TAG, "MessageCreationFailure!!" );
			throw new BuildExceptionError( "Failure To Append Int" );
		}
		
		return( new_array );				
	}
	/** 
	 * Append a long (64 bits) to a byte array.
	 * @param baseMessage
	 * @param append_long
	 * @return
	 */
	private static byte[] AppendLong(byte[] baseMessage, long append_data, boolean reverse ) {
				
		byte[] new_array = null;
		int l1 = baseMessage.length;
		int l2 = 8;
		int i;
		
		new_array = new byte[ l1 + l2 ];
		
		System.arraycopy( baseMessage, 0, new_array, 0, l1 );
		
		for ( i=0; i<l2; i++ )
		{

			if ( reverse ) {
				new_array[ (l1+l2-1)-i ] = (byte) ( append_data & 0xff );
			}
			else
			{
				new_array[ l1+i ] = (byte) ( append_data & 0xff );
			}
			
			append_data = append_data >>> 8;
		}
		
		baseMessage = new_array;
		
		return( new_array );				
	}

	/**
	 * Append a short (2 bytes) to a byte array.
	 * @param baseMessage
	 * @param append_short
	 * @return
	 */
	private static byte[] AppendShort(byte[] baseMessage, long append_data, boolean reverse ) {
				
		byte[] new_array = null;
		int l1 = baseMessage.length;
		int l2 = 2;
		
		int i;
		
		new_array = new byte[ l1 + l2 ];
		
		System.arraycopy( baseMessage, 0, new_array, 0, l1 );
		
		for ( i=0; i<l2; i++ )
		{

			if ( reverse ) {
				new_array[ (l1+l2-1)-i ] = (byte) ( append_data & 0xff );
			}
			else
			{
				new_array[ l1+i ] = (byte) ( append_data & 0xff );
			}
			
			append_data = append_data >>> 8;
		}
		
		baseMessage = new_array;
		
		return( new_array );				
	}
	/**
	 * Append a string to the BaseMessage.
	 * @param append_string the string to append
	 * @param append_length indicates the exact number of chars to append.
	 * @return
	 */
	private static byte[] AppendString( byte[] BaseMessage, String append_string, int append_length )
	{
		byte[] bytes_encoded = EncodingUtils.getBytes(append_string, "ASCII");
		
		BaseMessage = ArrayAppend( BaseMessage, bytes_encoded, append_length );
		
		return( BaseMessage );
	}
	/** 
	 * Append at most at_most bytes to an array.  also, truncate the incoming string if exceeds at_most.
	 * @param current existing string to append to
	 * @param data_to_append data to be appended
	 * @param append_length exact number of characters to append
	 * @return
	 */
	private static byte[] ArrayAppend ( byte[] current, byte[] data_to_append, int append_length )
	{
		byte[] new_array = null;
		int l1 = 0, l2 = 0;
		
		l1 = current.length;
		
		if ( data_to_append != null )
		{
			l2 = data_to_append.length;
		}
		else
		{
			l2 = 0;
		}
		
		if ( l2 > append_length )
		{
			l2 = append_length;
		}
		
		new_array = new byte[ l1 + l2 ];
		
		System.arraycopy( current, 0, new_array, 0, l1 );
		
		if ( ( l2 > 0 ) && ( data_to_append != null ) )
		{
			System.arraycopy( data_to_append, 0, new_array, l1, l2 );
		}
		
		if ( l2 < append_length )
		{
			byte temp = 0;
			
			// Append the remaining characters, but append
			//   it to the new array we've been creating.
			for ( ; l2 < append_length; l2 ++ )
			{
				new_array = AppendByte( new_array, temp );
			}
		}
		
		return( new_array );
	}
	
}
