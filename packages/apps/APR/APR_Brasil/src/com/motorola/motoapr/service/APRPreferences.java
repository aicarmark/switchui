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
import java.io.IOException;
import java.io.InputStreamReader;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import com.motorola.motoapr.service.PanicType.PANIC_ID;


public class APRPreferences {
	
	static final String TAG = "APRPreferences";

	
	static SharedPreferences APRSharedPrefs = null;
	
	static String APR_PREFS_NAME                  = "APRPreferences";
	static String APR_PREFS_NAME_INITIALIZED      = "APRPrefsInitComplete";
	static String APR_PREFS_INITIAL_SCAN_COMPLETE = "APRPrefsInitialScanComplete";
	static String APR_PREFS_RUN_FAST              = "APRPrefsRunFast";
	static String APR_PREFS_SW_VERS_NUMBER        = "APRPrefsSoftwareVersion";
	static String APR_PREFS_FLEX_VERS_NUMBER      = "APRPrefsFlexVersion";
	static String APR_PREFS_CARRIER_TYPE          = "APRPrefsCarrierType";
	static String APR_PREFS_ENABLED_STATE         = "APRPrefsEnabledState";
	static String APR_PREFS_MESSAGE_INDEX         = "APRPrefsMessageIndex";
	static String APR_TIME_SINCE_LAST_PERIODIC_REPORT  = "APRPrefsTimeSinceLastReport";
	static String APR_PREFS_TOTAL_UPTIME_ELAPSED  = "APRPrefsTotlUptimeElapsed";
	static String APR_PREFS_DEFAULT_BARCODE       = "APRPrefsDefaultBarcode";
	static String APR_PREFS_DEFAULT_FLEX_VERSIONS = "APRPrefsDefaultFlexVersion";
	static String APR_PREFS_BUILD_CRC             = "APRPrefsBuildCRC";
	static String APR_PREFS_CRITICAL_PANIC_COUNT  = "APRPrefsCritPanicCount";
	static String APR_PREFS_APP_PANIC_COUNT       = "APRPrefsAppPanicCount";
	static String APR_PREFS_PHONE_RESTARTS        = "APRPrefsPhoneRestarts";
	static String APR_PREFS_APP_LIST_CREATED      = "APRPrefsAppListCreated1";
	
	static String APR_PREFS_SLIPPAGE_UPTIME       = "APRPrefsSlippageUptime";
	static String APR_PREFS_SLIPPAGE_REALTIME     = "APRPrefsSlippageRealtime";

	static String APR_PREFS_CARRIER_SMS_NUMBER      = "APRCarrierSMSNumber";

    //  until we have a method to switch this via UI, update this variable for your location.
    //        static CARRIER default_carrier             = CarrierInfo.CARRIER.UK_UK;
   static CARRIER default_carrier              = CARRIER.BRAZIL;
   static boolean APR_PREFS_RUN_FAST_DEFAULT   = false;
   
   static String UNINITIALIZED_RESULT             = "Uninitialized";
   static String SYSTEM_PROPERTIES_FILE           = "/data/data/com.motorola.motoapr.service/system_props";

   static Context mContext = null;
   static String APR_SERVERNUMBER = default_carrier.sms_number;

   // maintain a separate message index.  try to guarantee we don't have multiple
   //   threads getting the same index.
   static int message_index = 0;

   public APRPreferences( Context context ) {
	   
	   mContext = context;
	   	   
  	   // Initialize preferences.
	   if ( !APRServiceInitPrefs() )
  		{
  			APRDebug.APRLog( TAG, "Failed to Initialize Preferences" );
  		}
   }
	
   // provide this service to the system.  indicates if a property is valid or not.   
   public static boolean APRServiceCheckProp( String property )
   {
	   boolean return_result = false;
		
       try 
       {
   	      Runtime r = Runtime.getRuntime();
		
		  // use extended paths, and create a new string to pass into r.exec
		  String cmd = new String( "/system/bin/getprop " + property );
		
		  Process p = r.exec( cmd );
		
		  BufferedReader reader = new BufferedReader ( new InputStreamReader( p.getInputStream() ), 1024 );
			  
		  String textstring;
		  
		  while( (textstring = reader.readLine()) != null ) 
		  {
			 byte[] textbytes = textstring.getBytes();
			  
	         if ( (  textstring.length() > 5 ) && 
	              ( ( ( textbytes[0] > 'A' ) && ( textbytes[0] < 'Z' ) ) || 
	                ( ( textbytes[0] > 'a' ) && ( textbytes[0] < 'z' ) ) ) )
	         {
			    return_result = true;
		     }
          }
      } catch (IOException e ) 
      {
    		APRDebug.APRLog( TAG, "System Properties Discovery Failed" );
      } finally 
      {
    		APRDebug.APRLog( TAG, "APRPreferences.APRPrefCheckProp Exiting." );
      }
    	
      return( return_result );
   }
   
   
   // reset phone information only.
   public static boolean APRServiceResetPhoneInfo()
   {
	   boolean return_result = false;
	   
   	   Editor prefs_editor = APRSharedPrefs.edit();
    	   
   	   // remove the phone-specific preferences, so that 
   	   //   when we launch APR, they get initialized again.
   	   prefs_editor.remove( APR_PREFS_SW_VERS_NUMBER );
   	   prefs_editor.remove( APR_PREFS_DEFAULT_BARCODE );
  	   prefs_editor.remove( APR_PREFS_FLEX_VERS_NUMBER );    		
   	   prefs_editor.remove( APR_PREFS_DEFAULT_FLEX_VERSIONS );       
    	   
	   return ( return_result );
   }
   
	/**
	 * Initialize Preferences.  This can be run any time preferences
	 * are updated.  Its job is to distribute the new preferences
	 * throughout the software.
	 */
	private static boolean APRServiceInitPrefs(){
		boolean return_result = false;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE  );
   		
   		if ( !APRSharedPrefs.contains( APR_PREFS_NAME_INITIALIZED ) )
   		{
   			Editor prefs_editor = APRSharedPrefs.edit();
   			
   			// Defaults.  This is for when the APP is initialized for the very
   			//   first time.  This is NOT to set default values, most of the time.
   			//   Instead, it is for cleanliness only.
   			
   			prefs_editor.putBoolean( APR_PREFS_RUN_FAST, APR_PREFS_RUN_FAST_DEFAULT );
   			prefs_editor.putInt( APR_PREFS_CRITICAL_PANIC_COUNT, 0 );
   			prefs_editor.putInt( APR_PREFS_APP_PANIC_COUNT, 0 );
   			prefs_editor.putString( APR_PREFS_CARRIER_TYPE, default_carrier.label);
   			
   			// Put this at the end of our preferences.
   			prefs_editor.putString(APR_PREFS_NAME_INITIALIZED, "initialized" );

   			// Commit!
   			return_result = prefs_editor.commit();
 		}
   		else
   		{
   			return_result = true;
   		}
   		
   		return( return_result );
	}
	

	
	public static boolean APRPrefsGetRunFast() {
		
		return( APRSharedPrefs.getBoolean( APR_PREFS_RUN_FAST, APR_PREFS_RUN_FAST_DEFAULT ) );
	}
	
	public static void APRPrefsSetRunFast( boolean run_fast )
	{
		Editor prefs_editor = APRSharedPrefs.edit();
		
		// Put this at the end of our preferences.
		prefs_editor.putBoolean( APR_PREFS_RUN_FAST, run_fast );
		   			
		// Commit!
		prefs_editor.commit();
	}
		
	static void APRPrefsSetEntryLastModified( String file_path, long last_modified_timestmap ) {
		
		Editor prefs_editor = APRSharedPrefs.edit();
		
		// Put this at the end of our preferences.
		prefs_editor.putLong( file_path, last_modified_timestmap  );
		   			
		// Commit!
		prefs_editor.commit();
	}

	static long APRPrefsGetEntryLastModified( String file_path ) {
	
		long last_recorded_modified_val = 0;
		
		last_recorded_modified_val = APRSharedPrefs.getLong( file_path, 0 );
		
		return( last_recorded_modified_val );
	}

	static void APRPrefsCrashCount( PANIC_ID panic_id ) {
		
		long crash_count = 0;
		
		if ( panic_id != PANIC_ID.NORMAL_POWERDOWN ) {
		
			String crash_type = panic_id.label;		
			crash_count = APRPrefsGetCrashCount( panic_id ) + 1;
		
			Editor prefs_editor = APRSharedPrefs.edit();
		
			// Put this at the end of our preferences.
			prefs_editor.putLong( crash_type, crash_count );
		   			
			// Commit!
			prefs_editor.commit();
			/*APRPrefsPanicCount execute twice.
			if (( panic_id != PANIC_ID.MODEM_CRASH ) &&
				( panic_id != PANIC_ID.MODEM_CRASH1 ) &&
				( panic_id != PANIC_ID.MODEM_CRASH2 ) &&
				( panic_id != PANIC_ID.MODEM_CRASH3 ) )
			{
				// increment the totals for AP crashes only.
				// we will track and print modem crashes separately.
				APRPrefsPanicCount( panic_id.critical_panic );
			}*/
		}
	}

	static long APRPrefsGetCrashCount( PANIC_ID panic_id ) {
	
		long crash_count = 0;
		
		String crash_type = panic_id.label;
		
		crash_count = APRSharedPrefs.getLong( crash_type, 0 );
		
		return( crash_count );
	}

	static void APRPrefsPhoneRestart() {

		int crash_count = 0;

		crash_count = APRSharedPrefs.getInt( APR_PREFS_PHONE_RESTARTS, 0 ) + 1;

		Editor prefs_editor = APRSharedPrefs.edit();

		// For the carrier index specified, lookup the string and produce the label information.
		prefs_editor.putInt( APR_PREFS_PHONE_RESTARTS, crash_count );

		// Commit!
		prefs_editor.commit();	
	}
	
	static int APRPrefsGetPhoneRestart() {

		return APRSharedPrefs.getInt( APR_PREFS_PHONE_RESTARTS, 0 );
	}

	
	public static int APRPrefsGetMessageIndex() {

		// get the message index and increment it.
		int message_index = APRSharedPrefs.getInt( APR_PREFS_MESSAGE_INDEX, 0 );

		message_index = message_index + 1;

		Editor prefs_editor = APRSharedPrefs.edit();

		// For the carrier index specified, lookup the string and produce the label information.
		prefs_editor.putInt( APR_PREFS_MESSAGE_INDEX, message_index );

		// Commit!
		prefs_editor.commit();	

	        return( message_index );
	}
	
	public static void APRPrefsSetCarrierId( int carrier_index ) {

		Editor prefs_editor = APRSharedPrefs.edit();
	   			
		// For the carrier index specified, lookup the string and produce the label information.
		prefs_editor.putString( APR_PREFS_CARRIER_TYPE, ( CARRIER.fromId( carrier_index ) ).label );
			   			
		// Commit!
		prefs_editor.commit();		
	}
	
	public static int APRPrefsGetCarrierId() {

		String carrier_label = APRSharedPrefs.getString( APR_PREFS_CARRIER_TYPE, ( CARRIER.fromId( 0 ) ).label );
	
		int carrier_index = CARRIER.fromLabel( carrier_label ).id;

		return( carrier_index );
	}
	
	public static String APRPrefsGetCarrierName() 
	{
		return APRSharedPrefs.getString( APR_PREFS_CARRIER_TYPE, default_carrier.label );
	}
		
	public static boolean APRPrefsGetEnabledState() 
	{
		return APRSharedPrefs.getBoolean( APR_PREFS_ENABLED_STATE, true );
	}
	
	public static void APRPrefsSetEnabledState( boolean enabled_state )
	{
	
		Editor prefs_editor = APRSharedPrefs.edit();
		
		prefs_editor.putBoolean( APR_PREFS_ENABLED_STATE, enabled_state );
	   			
		// Commit!
		prefs_editor.commit();
	}
	
	public static boolean APRPrefsGetInitialScanComplete() 
	{
		return APRSharedPrefs.getBoolean( APR_PREFS_INITIAL_SCAN_COMPLETE, false );
	}
	
	public static void APRPrefsSetInitialScanComplete( boolean scan_complete_state )
	{
	
		Editor prefs_editor = APRSharedPrefs.edit();
		
		prefs_editor.putBoolean( APR_PREFS_INITIAL_SCAN_COMPLETE, scan_complete_state );
	   			
		// Commit!
		prefs_editor.commit();
	}
	

		
	public static boolean APRPrefsSetSwVers( String new_software_version ){
		
		boolean return_result = false;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putString( APR_PREFS_SW_VERS_NUMBER, new_software_version );
   			
   		// Commit!
   		return_result = prefs_editor.commit();
   		
   		return( return_result );
	}

	// the purpose of this routine is to get the software version from the PREFERENCES area.
	//   hence the NAME.
	public static String APRPrefsGetSwVers(){
		
		String software_version = null;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.  
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
   		software_version = APRSharedPrefs.getString( APR_PREFS_SW_VERS_NUMBER , "uninitialized" );
   		  
   		return( software_version );
	}

	
	public static boolean APRPrefsSetFlexVers( String new_flex_version ){
		
		boolean return_result = false;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putString( APR_PREFS_FLEX_VERS_NUMBER, new_flex_version );
   		
   			
   		// Commit!
   		return_result = prefs_editor.commit();
   		
   		return( return_result );
	}

	
	// the purpose of this function is to return the flex version from PREFERENCES!
	//   NOT to get the flex version from the phone code.  This tells us what
	//   flex version we have sent in the past... hence the name of the function.
	public static String APRPrefsGetFlexVers(){
		
		String flex_version = null;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
   		flex_version = APRSharedPrefs.getString( APR_PREFS_FLEX_VERS_NUMBER , UNINITIALIZED_RESULT );
   				
   		return( flex_version );
	}
	
	public static long APRPrefsGetTimeSinceLastReport() {
		
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		long last_recorded_uptime = APRSharedPrefs.getLong( APR_TIME_SINCE_LAST_PERIODIC_REPORT, 0);

		return( last_recorded_uptime );
	}
	
	public static void APRPrefsSetTimeSinceLastReport( long elapsed_uptime ) {
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putLong( APR_TIME_SINCE_LAST_PERIODIC_REPORT, elapsed_uptime );
   			
   		// Commit!
   		prefs_editor.commit();
	}
	
	public static long APRPrefsGetTotalUptimeElapsed() {
		
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		long total_uptime_elapsed = APRSharedPrefs.getLong( APR_PREFS_TOTAL_UPTIME_ELAPSED, 0);

		return( total_uptime_elapsed );
	}
	
	// set the total uptime elapsed.  
	public static void APRPrefsSetTotalTimeElapsed( long total_uptime_elapsed ) {
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putLong( APR_PREFS_TOTAL_UPTIME_ELAPSED, total_uptime_elapsed );
   			
   		// Commit!
   		prefs_editor.commit();
	}
	
	// this is the default barcode we keep around in the system when the real barcode
	//   isn't available, or isn't functioning.  
	public static String APRPrefsGetDefaultBarcode() {
		
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		String default_barcode = APRSharedPrefs.getString( APR_PREFS_DEFAULT_BARCODE, null );

		return( default_barcode );
	}
	
	public static void APRPrefsSetDefaultBarcode( String default_barcode ) {
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putString( APR_PREFS_DEFAULT_BARCODE, default_barcode );
   			
   		// Commit!
   		prefs_editor.commit();
	}
		
	/**
	 * Get the Build CRC Value
	 * @return
	 */
	public static int APRPrefsGetBuildCRC(){
		
		int build_crc = 0;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.  
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
   		build_crc = APRSharedPrefs.getInt( APR_PREFS_BUILD_CRC, 0 );

   		return( build_crc );
	}

	/**
	 * Store the Build CRC Value.
	 * @param build_crc
	 */
	public static void APRPrefsSetBuildCRC( int build_crc ) {
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putInt( APR_PREFS_BUILD_CRC, build_crc );
   			
   		// Commit!
   		prefs_editor.commit();
	}

	// this is the default flex version we keep around in the system when the real flex version
	//   hasn't been properly programmed.  We use this to ensure we don't continually crash when
	//   starting the application up.  If the flex isn't programmed, the app will crash upon reading.
	//   since we program this value first, subsequent attempts (by restarting the app) will not
	//   cause the app to crash, instead yielding a default value, as programmed here.
	//
	// this is distinct from the APR_PREFS_FLEX_VERS_NUMBER preference.  that preference is used to 
	//   determine when we've had a CHANGE in the flex version causing a software version flex change
	//   indication to be sent to the server.
	public static String APRPrefsGetDefaultFlexVersion() {
		
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		String default_flex_version = APRSharedPrefs.getString( APR_PREFS_DEFAULT_FLEX_VERSIONS, null );

		return( default_flex_version );
	}
	
	// Per the above, this is not for indicating the software version flex change indication.
	public static void APRPrefsSetDefaultFlexVersion( String default_flex_version ) {
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		prefs_editor.putString( APR_PREFS_DEFAULT_FLEX_VERSIONS, default_flex_version );
   			
   		// Commit!
   		prefs_editor.commit();
	}
	
	// API to increment the Panic Count.
	public static void APRPrefsPanicCount( boolean critical_panic )
	{		
		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
   		int critical_panic_count = APRSharedPrefs.getInt( APR_PREFS_CRITICAL_PANIC_COUNT, 0 );
   		int non_critical_panic_count = APRSharedPrefs.getInt( APR_PREFS_APP_PANIC_COUNT, 0 );
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   		
		// increment and store.
		if ( critical_panic ) 
		{	
			prefs_editor.putInt( APR_PREFS_CRITICAL_PANIC_COUNT, critical_panic_count+1 );
		}
		else
		{
			prefs_editor.putInt( APR_PREFS_APP_PANIC_COUNT, non_critical_panic_count+1 );
		}
   		
   		// Commit!
   		prefs_editor.commit();		
	}
	
	// API to increment the Panic Count.
	public static int APRPrefsGetPanicCount( boolean critical_panic )
	{		
		int return_val = 0; 
		int critical_panic_count = 0;
		int non_critical_panic_count = 0;

		try { 
		
			// Get the shared preferences to be managed by the 
			//   APR Service.
			APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
			critical_panic_count = APRSharedPrefs.getInt( APR_PREFS_CRITICAL_PANIC_COUNT, 0 );
			non_critical_panic_count = APRSharedPrefs.getInt( APR_PREFS_APP_PANIC_COUNT, 0 );
   		   		
			if ( critical_panic ) 
			{	
				return_val = critical_panic_count;
			}
			else
			{
				return_val = non_critical_panic_count;
			}
			
		} catch ( Exception e ) {
			APRDebug.APRLog ( TAG, "Failed to Get Panic Counts. ");
		}
		
		return( return_val );
	}
	
	public static boolean APRPrefsGetAppListCreated() {
		
		return( APRSharedPrefs.getBoolean( APR_PREFS_APP_LIST_CREATED, false ) );
	}
	
	public static void APRPrefsSetAppListCreated( boolean list_created )
	{
		Editor prefs_editor = APRSharedPrefs.edit();
		
		// Put this at the end of our preferences.
		prefs_editor.putBoolean( APR_PREFS_APP_LIST_CREATED, list_created );
		   			
		// Commit!
		prefs_editor.commit();
	}
	
	
	/**
	 * Clean Preferences.  Be very careful, this must be run only
	 * when there is a full reset of APR (i.e. when there is a software
	 * version change detected).
	 */
	public static void APRPrefsCleanPreferences() 
	{
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

		Editor prefs_editor = APRSharedPrefs.edit();

		// refresh the flex version.
		// prefs_editor.remove(APR_PREFS_DEFAULT_FLEX_VERSIONS);
		// prefs_editor.remove(APR_PREFS_SW_VERS_NUMBER);
		// prefs_editor.remove(APR_PREFS_NAME);
		// prefs_editor.remove(APR_PREFS_NAME_INITIALIZED);
		// prefs_editor.remove(APR_PREFS_RUN_FAST);
		// prefs_editor.remove(APR_PREFS_CARRIER_TYPE);
		// prefs_editor.remove(APR_PREFS_FLEX_VERS_NUMBER);
		// prefs_editor.remove(APR_PREFS_ENABLED_STATE);
		// prefs_editor.remove(APR_PREFS_DEFAULT_FLEX_VERSIONS);
		// prefs_editor.remove(APR_PREFS_DEFAULT_BARCODE);

		// Erase uptime, and start over.
		prefs_editor.remove(APR_PREFS_TOTAL_UPTIME_ELAPSED);
		prefs_editor.remove(APR_TIME_SINCE_LAST_PERIODIC_REPORT);
		prefs_editor.remove(APR_PREFS_CRITICAL_PANIC_COUNT);
		prefs_editor.remove(APR_PREFS_APP_PANIC_COUNT);
		prefs_editor.remove(APR_PREFS_SLIPPAGE_UPTIME);
		prefs_editor.remove(APR_PREFS_SLIPPAGE_REALTIME);
		prefs_editor.remove(APR_PREFS_PHONE_RESTARTS);
		
		// for each panic id in the panic id list,
		//   remove the preference associated with that entry.
		//   these were added by APRPrefsCrashCount.
		String[] panic_id_list = PANIC_ID.getAllLabels();
		for ( int i = 0; i < panic_id_list.length; i++ )
		{
			prefs_editor.remove( panic_id_list[i] );
		}

   		prefs_editor.commit();
	}
	
	public static long APRGetSlippageRealtimeValue() {
		
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		long slippage_realtime = APRSharedPrefs.getLong( APR_PREFS_SLIPPAGE_REALTIME, 0);

		return( slippage_realtime );
	}

	public static long APRGetSlippageUptime() {
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		long slippage_uptime = APRSharedPrefs.getLong( APR_PREFS_SLIPPAGE_UPTIME, 0);

		return( slippage_uptime );
	}

	public static void APRSetSlippageRealtimeValue(long current_realtime) {
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putLong( APR_PREFS_SLIPPAGE_REALTIME, current_realtime );
   			
   		// Commit!
   		prefs_editor.commit();
		
	}

	public static void APRSetSlippageUptime(long current_uptime) {
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putLong( APR_PREFS_SLIPPAGE_UPTIME, current_uptime );
   			
   		// Commit!
   		prefs_editor.commit();
		
	}

	public static boolean APRSetCarrierNumber( String carrier_number ){
		
		boolean return_result = false;
		
  		// Get the shared preferences to be managed by the 
   		//   APR Service.
   		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);
   		
		Editor prefs_editor = APRSharedPrefs.edit();
   			
   		// Initially, do not run fast.  Run at the normal speed.
   		prefs_editor.putString( APR_PREFS_CARRIER_SMS_NUMBER, carrier_number );
   		
   			
   		// Commit!
   		return_result = prefs_editor.commit();
   		
   		return( return_result );
	}

	public static String APRGetCarrierNumber() {
		
		APRSharedPrefs = mContext.getSharedPreferences( APR_PREFS_NAME, android.content.Context.MODE_PRIVATE);

   		String carrier_number = APRSharedPrefs.getString( APR_PREFS_CARRIER_SMS_NUMBER, APR_SERVERNUMBER );

		return( carrier_number );
	}

}
