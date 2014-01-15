/*
 * @(#)LocationConsent.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18385        2011/06/27    NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.rulesbuilder;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.util.PublisherFilterlist;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.virtualsensor.locationsensor.AppPreferences;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

/** This class has the logic for displaying the Location Consent Dialog to the user
 *  and the actions that need to be taken based on certain settings.
 * 
* CLASS:
* 
* RESPONSIBILITIES:
* 
* COLABORATORS:
*  None.
*
* USAGE:
* 	see methods for usage instructions
*
*
*/
public class LocationConsent implements Constants, RulesBuilderConstants{
	
	private static final String TAG = LocationConsent.class.getSimpleName();
	private static final String LOCATION_SETTING_INTENT = Settings.ACTION_LOCATION_SOURCE_SETTINGS;			
	private static Intent mIntent;
	private static boolean mLocationPickerLaunched = false;  // True if location picker has been launched
	private static boolean mAirplaneModeSettingLaunched = false;  // True if Airplane Mode settings has been launched
	private static boolean isAirplaneModeLocCorrelationDialogShownOnSave = false;
	private static boolean mSecuritySettingsScreenLaunched = false; //True if the location and security settings screen is launched
    private static final String RULESBUILDER_SHARED_PREFERENCE = "com.motorola.contextual.rulesbuilder";
    private static final String LOCATION_CONSENT_ACCEPTED = "locationconsentaccepted";
    static final String AIRPLANE_SETTING_INTENT = Settings.ACTION_AIRPLANE_MODE_SETTINGS;
	
    /**
     *  AIRPLANE_MODE_ON 1
     *	AIRPLANE_MODE_OFF 0
     *
     */
    public static enum AIRPLANE_MODE_STATUS{
    	AIRPLANE_MODE_ON (1),
    	AIRPLANE_MODE_OFF (0);
    	
		private int status;
		AIRPLANE_MODE_STATUS(int stat){
			this.status = stat;
		}
		
		public int getStat(){
			return this.status;
		} 
    }
    
	/** Getter - mLocationPickerLaunched*/
	public static boolean getLocationPickerLaunchedFlag(){
	   return mLocationPickerLaunched;
	}
   
	/** Setter - mLocationPickerLaunched*/
	public static void setLocationPickerLaunchedFlag(boolean pickerLaunched){
	   mLocationPickerLaunched = pickerLaunched;
	}
   
	/** Getter - mSecuritySettingsScreenLaunched*/
	public static boolean getSecuritySettingsScreenLaunchedFlag(){
	   return mSecuritySettingsScreenLaunched;
	}
   
	/** Setter - mSecuritySettingsScreenLaunched*/
	public static void setSecuritySettingsScreenLaunchedFlag(boolean securityScreenLaunched){
		mSecuritySettingsScreenLaunched = securityScreenLaunched;
	}
	
	public static boolean getAirplaneModeSettingLaunched() {
		return mAirplaneModeSettingLaunched;
	}

	public static void setAirplaneModeSettingLaunched(
			boolean mAirplaneModeSettingLaunched) {
		LocationConsent.mAirplaneModeSettingLaunched = mAirplaneModeSettingLaunched;
	}
	
	public static boolean isAirplaneModeLocCorrelationDialogShownOnSave() {
		return isAirplaneModeLocCorrelationDialogShownOnSave;
	}

	public static void setAirplaneModeLocCorrelationDialogShownOnSave(
			boolean isAirplaneModeLocCorrelationDialogShownOnSave) {
			LocationConsent.isAirplaneModeLocCorrelationDialogShownOnSave = isAirplaneModeLocCorrelationDialogShownOnSave;
	}
    
	/** Update the intent to be used later*/
	public static void setIntent(Intent intent){
		mIntent = intent;
	}
	   
	/** Update the intent to be used later*/
	public static Intent getIntent(){
		return mIntent;
	}
	
	/** Invokes the Location and Security Screen*/
	public static void invokeLocationAndSecurityScreen(Context context){
	   setSecuritySettingsScreenLaunchedFlag(true);
	   Intent intent = new Intent(LOCATION_SETTING_INTENT);
	   context.startActivity(intent);
	}
	   
	/** After Back key pressed in Security Settings*/
	public static void onBackPressInSecuritySettings (Context context, View blockView){
		if (LocationConsent.isGoogleLocationProviderAvailable(context)){
			//add also the clause to check for wifi enabled
			if (!LocationConsent.isWifiEnabled(context)){
         	   showWifiTurnOnDialog(context, blockView);
            }
			gotoLocationPicker(context);
		}
	   	   LocationConsent.setSecuritySettingsScreenLaunchedFlag(false);
	}
	 
	/** After Back key pressed in Location picker*/
	public static void onBackPressInLocationPicker (Context context, View view, ConditionInteractionModel event){
		Publisher block = (Publisher)view.getTag();
		if (block.getPublisherKey().equals(LOCATION_TRIGGER)){
		   Blocks.refreshLocationBlock(context, view, event);
		}
		LocationConsent.setLocationPickerLaunchedFlag(false);
	}
	 
	/** Start an activity depending on the current settings*/
	public static void startRelevantActivity(Context context, Intent intent){
		setIntent(intent);

		if (!isGoogleLocationProviderAvailable(context) ||
				!LocationConsent.isWifiSleepSetToNever(context)){ 
				showLocationDialog(context);
		}else if(isAirplaneModeOn(context)){
				showAirplaneDialog(context);
		}else if(!LocationConsent.isWifiEnabled(context)){
			showWiFiEnableDialog(context);
		}else{
			gotoLocationPicker(context);
		}

	}
	
	/** Display the location consent dialog*/
	  public static void showAirplaneDialog(Context context){
		  if (context instanceof EditRuleActivity)
			  ((EditRuleActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID);
		  else if (context instanceof DisplayConditionsActivity)
 	   		  ((DisplayConditionsActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID);
	  }
	
	  /** Display the location consent dialog*/
	  public static void showLocationDialog(Context context){
		  if (context instanceof EditRuleActivity)
			  ((EditRuleActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_LOCATION_CONSENT_ID);
   	   	  else if (context instanceof DisplayConditionsActivity)
   	   		  ((DisplayConditionsActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_LOCATION_CONSENT_ID);
	  }
	  
	  /** Display the WiFi dialog*/
	  public static void showWiFiEnableDialog(Context context){
		  if (context instanceof EditRuleActivity)
			  ((EditRuleActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_LOCATION_WIFI_ENABLE_CONSENT_ID);
   	   	  else if (context instanceof DisplayConditionsActivity)
   	   		  ((DisplayConditionsActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_LOCATION_WIFI_ENABLE_CONSENT_ID);
	  }
	   
	   /** Go to the location picker screen*/
	   public static void gotoLocationPicker(Context context){
		   if (context instanceof EditRuleActivity)
    		   ((EditRuleActivity) context).invokeLocationPicker();
    	   else if (context instanceof DisplayConditionsActivity)
    		   ((DisplayConditionsActivity) context).invokeLocationPicker();
	   }
	   
	   /** Display the location WiFi auto-scan consent dialog*/
       public static void showLocationWiFiAutoscanDialog(Context context){
           if (context instanceof EditRuleActivity)
               ((EditRuleActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_LOC_WIFI_AUTOSCAN_CONSENT_ID);
           else if (context instanceof DisplayConditionsActivity)
               ((DisplayConditionsActivity) context).showDialog(EditRuleActivity.DialogId.DIALOG_LOC_WIFI_AUTOSCAN_CONSENT_ID);
       }
	 
	   /** Interface to be implemented to invoke the location picker from respective activities*/
	   public interface ILocationPicker{
		   public void invokeLocationPicker();
	   }
	   
	   /** Sets the User consent as a shared preference
	    * 
	    * @param context - Context
	    * @param consent - True if Agree, else False
	    */
	   @SuppressWarnings("unused")
	   private static void setConsent(Context context, boolean consent){
		   SharedPreferences prefs = context.getSharedPreferences(
				   RULESBUILDER_SHARED_PREFERENCE, Context.MODE_PRIVATE);
	       SharedPreferences.Editor editor = prefs.edit();
		   editor.putBoolean(LOCATION_CONSENT_ACCEPTED, consent);
		   editor.commit();
	   }
	   
	  /** Gets the user consent value
	   * 
	   * @param context
	   * @return True if the user has agreed to the location consent
	   */
	  public static boolean getConsent(Context context){
		   SharedPreferences prefs = context.getSharedPreferences(
				   RULESBUILDER_SHARED_PREFERENCE, Context.MODE_PRIVATE);
		  return (prefs.getBoolean(LOCATION_CONSENT_ACCEPTED, false));
	  }
	
	  /** Shows the location consent dialog
	   * 
	   * @param context
	   * @return
	   */
	  public static Dialog showLocationConsentDialog(final Context context, final View blockView) {
			 
		   AlertDialog.Builder builder = new AlertDialog.Builder(context);
	       builder.setMessage(R.string.location_consent)
	       .setTitle(R.string.location_title)
	       .setIcon(R.drawable.ic_pin_w)
	       .setInverseBackgroundForced(true)
	       .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               
	        	   if (!isWifiEnabled(context)){
                       WifiManager wifiMan = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                       wifiMan.setWifiEnabled(true); 
                   }

	               if (!isWifiSleepSetToNever(context)){
	            	   Settings.System.putInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
	               }
	               if ( !isGoogleLocationProviderAvailable(context)){
	            	  LocationConsent.setSecuritySettingsScreenLaunchedFlag(true);
	         		  Intent intent = new Intent(LOCATION_SETTING_INTENT);
	         		  context.startActivity(intent);
	               }
	               else {
	            	  LocationConsent.gotoLocationPicker(context);
	            	  LocationConsent.setLocationPickerLaunchedFlag(true);
	               }
	          }
	       })
	       .setNegativeButton(R.string.disagree, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   if(null!=blockView){
		        	   Publisher savedBlockInfo = (Publisher) blockView.getTag();
		               if (Util.isNull(savedBlockInfo.getConfig()) || savedBlockInfo.getError()){
		    		        savedBlockInfo.getInstBlockGestureListener().setConnect(false);
		    		        savedBlockInfo.getInstBlockGestureListener().animFromDialogView();
		               }
	        	   }
	               dialog.cancel();
	           }
	       });
	       AlertDialog consentDialog = builder.create();
	       return consentDialog;
	}
	
	  /**
       * Creates and shows a consent dialog to the user when the user tries to add location trigger
       * @param id - Id for Location-Wi-Fi autoscan  consent
       * @return
       */
      public static Dialog showWiFiLocationAutoscanConsentDialog(final Context context, final Intent intent, final View blockView) {

              AlertDialog.Builder builder = new AlertDialog.Builder(context);
              builder.setMessage(R.string.wifi_loc_wifi_autoscan_desc)
              .setTitle(R.string.location_title)
              .setIcon(R.drawable.ic_pin_w)
              .setInverseBackgroundForced(true)
              .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                      dialog.cancel();
                      Util.setLocSharedPrefStateValue(context, AppPreferences.HAS_USER_LOC_CONSENT, TAG, LOC_CONSENT_SET);
                      LocationConsent.startRelevantActivity(context, intent);

                  }
              })
              .setNegativeButton(R.string.disagree, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                      dialog.cancel();
                      if(null!=blockView){
                              //we suppress/disable multiple touch events to avoid launching multiple dialogs
                              //once we are done from dialog,the touch events need to be re-enabled
                    	  Publisher savedBlockInfo = (Publisher) blockView.getTag();
                              savedBlockInfo.getInstBlockGestureListener().setTouchSuppressed(false);
                      }
                  }
              })
              .setOnKeyListener(new DialogInterface.OnKeyListener() {
                  public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                         if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
                         dialog.cancel();

                      }
                      if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                         dialog.cancel();

                      }
                      if(null!=blockView){
                    	  Publisher savedBlockInfo = (Publisher) blockView.getTag();
                              savedBlockInfo.getInstBlockGestureListener().setTouchSuppressed(false);
                      }
                      return false; // Any other keys are still processed as normal
                  }
              });
              if (LOG_DEBUG) Log.d(TAG, "WiFi Dialog shud be shown by now!!! contextual : ");

              AlertDialog wifiOnDialog = builder.create();
              return wifiOnDialog;
      }
	  
	  /** checks is WiFi is enabled or not in settings
		 * 
		 * @param context
		 * @return true if WiFi enabled from settings, 
		 * 			returns true if AutoScan supported irrespective of WiFi settings
		 */
		public static boolean isWifiEnabled(Context context) {
			boolean enabled = false;
			if(isWifiAutoscanSupported(context)) return true; 
			// return true in above case as scan part of wifi required for location will be enabled
			
			WifiManager wifiMan = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		    int state = wifiMan.getWifiState();
		    if (state == WifiManager.WIFI_STATE_ENABLED || 
		   		  state == WifiManager.WIFI_STATE_ENABLING) {
		   	   enabled = true;
		    }
		    return enabled;
		}
		
		/** Checks if WiFi sleep is set to Never
		 * 
		 * @param context
		 * @return true if set to Never, returns true if AutoScan supported.
		 */
		public static boolean isWifiSleepSetToNever(Context context) {
			boolean wifiSetToNever = true;
			if(isWifiAutoscanSupported(context)) return true; 
			// return true in above case as scan part of wifi required for location will be enabled
			
			int wifiSleepStatus = locationFuncAvailable(context);
			if ((wifiSleepStatus & MASK_WIFISLEEP) == MASK_WIFISLEEP ) wifiSetToNever = false;
			return wifiSetToNever;
		}
		
		/**check whether location functionality available, i.e, should be enabled based on 3 conditions:
		 * 1. location network provider needs to be enabled.
		 * 2. user has accepted Analytical permission.
		 * 3. Wifi sleep policy is never sleep.
		 * @param ctx, the context to get all the system services.
		 * @return a int with following bit being set to indicate what extra settings need to be done.
		 * 0, YES, Nothing extra needs to be done. location functionality is fully available.
		 * 1: No, location network provider needs to be enabled.
		 * 2: No, ADA accept permission needs to be on
		 * 4. No, Wifi sleep policy needs to be set
		 */
	    public static int locationFuncAvailable(Context ctx){
	    		int status = 0;
	    		if(((LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE)).getProvider(LocationManager.NETWORK_PROVIDER) == null ||
	    				false == ((LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
	    			status |= (1<<SHIFT_LOCPROVIDER);
	    			if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: Location Network Provider Not Enabled!");
	    		}
	    		
	    		// ADA_ACCEPTED = 1 means user granted the permission.
	    		int ada_accept = Settings.System.getInt(ctx.getContentResolver(), ADA_ACCEPTED_KEY, 0);
	    		if(ada_accept != 1){
	    			status |= (1<<SHIFT_ADA);
	    			if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: ADA ACCEPTED key Not Enabled!");
	    		}

	    		// WIFI sleep policy: WIFI_SLEEP_POLICY_DEFAULT = 0;WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED=1;WIFI_SLEEP_POLICY_NEVER = 2;
	    		int wifi_sleep = Settings.System.getInt(ctx.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, 0);
	    		if (wifi_sleep != Settings.System.WIFI_SLEEP_POLICY_NEVER){
	    			status |= (1 << SHIFT_WIFISLEEP);
	    			if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: Wifi Sleep policy not set to Never sleep!");
	    		}
	    		
	    		//check for Airplane Mode on, which will disable Location sensor, should show error if turned on
	    		if(isAirplaneModeOn(ctx)){
	    			status |= (1<<SHIFT_AIRPLANE_ON);
	    			if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: AIRPLANE MODE ON !!");
	    		}else{
	    		       if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: AIRPLANE MODE OFF !!");
	    		}
             
	    		if(isWifiAutoscanSupported(ctx)){
	    			status |= (1<<SHIFT_WIFI_AUTOSCAN);
	    			if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: WiFi Autoscan supported !!");
	    		}else{
	    		      if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: WiFi Autoscan NOT supported !!");
	    		}

	    		if (LOG_DEBUG) Log.d(TAG, "locationFuncAvailable: Status code: "+ status);
	    		return status;
	    }
	    
	    /**
	      * Check to see if Airplane mode is on from System settings
	      * @param context
	      * @return
	      */
	     public static boolean isAirplaneModeOn(Context context)
	     {
	             return (Settings.System.getInt(context.getContentResolver(),
	            		 			Settings.System.AIRPLANE_MODE_ON, 0) == AIRPLANE_MODE_STATUS.AIRPLANE_MODE_ON.getStat() );
	     }
	     
	     /**
	      * Is WiFi Auto scan supported to decouple Location triggers from WiFi settings 
	      * @param context
	      * @return true if WiFi Auto scan is supported
	      */
	     public static boolean isWifiAutoscanSupported(Context context){
	    	 boolean ret = false;
	    	 AppPreferences chkWifiAutoScan = new AppPreferences(new LocationSensorApp(context));
	    	 String pref_string =  chkWifiAutoScan.getString(AppPreferences.BACKGROUND_SCAN);
	    	 if(LOG_DEBUG) Log.d(TAG, "isWifiAutoscanSupported: pref_string = "+pref_string);
	    	 if(pref_string != null){
	    		 if(pref_string.equalsIgnoreCase("1")){
	    			 ret = true;
	    			 if(LOG_DEBUG) Log.d(TAG, "isWifiAutoscanSupported: true");
	    		 }
	    	 }
	    	 return ret;
	     }
	     
	     /** checks and returns true if the location error is required else false
	      * 
	      * @param context - Context
	      * @return true if location error block has to show error else false
	      */
	     public static boolean isLocationErrorRequired(final Context context) {
	     	return (!isGoogleLocationProviderAvailable(context) ||
	     				!isWifiEnabled(context) ||
	     				!isWifiSleepSetToNever(context) ||
	     				!Util.isMotLocConsentAvailable(context) ||
	     				 isAirplaneModeOn(context)	);
	     }
	     
	     /** checks if the Google Location provider is setup
	      * 
	 	 * @param context
	 	 * @return true if the Location provider has not been set up.
	 	 */
	 	public static boolean isGoogleLocationProviderAvailable(Context context){
	 		boolean available = true;
	 	    int locationStatus = locationFuncAvailable(context);
	 		if ((locationStatus & MASK_LOCPROVIDER) == MASK_LOCPROVIDER) available = false;
	 		return available;
	 	}
	 	
	 	public static Dialog showWifiTurnOnDialog(final Context context, final View blockView) {
			 
			   AlertDialog.Builder builder = new AlertDialog.Builder(context);
		       builder.setMessage(R.string.wifi_loc_consent_desc)
		       .setTitle(R.string.wifi_loc_consent_title)
		       .setIcon(R.drawable.ic_pin_w)
		       .setInverseBackgroundForced(true)
		       .setPositiveButton(R.string.turn_on, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               
		               WifiManager wifiMan = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		               wifiMan.setWifiEnabled(true); 

		               if (!LocationConsent.isWifiSleepSetToNever(context)){
		            	   Settings.System.putInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
		               }
		               //Not sure if this check is needed, seems might not be true, keeping it in as safe change for St6, to be verified/tested later
		               if ( !LocationConsent.isGoogleLocationProviderAvailable(context)){
		            	  LocationConsent.setSecuritySettingsScreenLaunchedFlag(true);
		         		  Intent intent = new Intent(LOCATION_SETTING_INTENT);
		         		  context.startActivity(intent);
		               }
		               else {
		            	  LocationConsent.gotoLocationPicker(context);
		            	  LocationConsent.setLocationPickerLaunchedFlag(true);
		               }
		          }
		       })
		       .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   if(null!=blockView){
			        	   Publisher savedBlockInfo = (Publisher) blockView.getTag();
			               if (Util.isNull(savedBlockInfo.getConfig()) || savedBlockInfo.getError()){
			    		        savedBlockInfo.getInstBlockGestureListener().setConnect(false);
			    		        savedBlockInfo.getInstBlockGestureListener().animFromDialogView();
			               }
		        	   }
		               dialog.cancel();
		           }
		       });
		       AlertDialog wifiConsentDialog = builder.create();
		       return wifiConsentDialog;
		}
	 	
	 	/**
	     * Creates and shows a dialog to the user when the user tries to save a Loc based rule
	     * and Airplane mode is on
	     * @param id - Id for Location-Airplane mode correlation
	     * @return
	     */
	 	protected static Dialog showAirplaneModeLocationCorrelationDialog(final Context context) {

	        AlertDialog.Builder builder = new AlertDialog.Builder(context);
	        builder.setMessage(R.string.airplanemode_loc_correlation_desc)
	        .setTitle(R.string.airplanemode_loc_correlation_title)
	        .setIcon(R.drawable.ic_pin_w)
	        .setInverseBackgroundForced(true)
	        .setPositiveButton(R.string.turn_off_airplanemode, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	             // toggle airplane mode off
	                Settings.System.putInt(
	                      context.getContentResolver(),
	                      Settings.System.AIRPLANE_MODE_ON, AIRPLANE_MODE_STATUS.AIRPLANE_MODE_OFF.getStat());

	                // Broadcast intent so that notification bar and other subscribers can know that its turned off 
	                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
	                intent.putExtra("state", AIRPLANE_MODE_STATUS.AIRPLANE_MODE_OFF.getStat());
	                context.sendBroadcast(intent);
	            }
	        })
	        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	                dialog.cancel();
	            }
	        })
	        .setOnKeyListener(new DialogInterface.OnKeyListener() {
	            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
	         	   if ( (keyCode == KeyEvent.KEYCODE_SEARCH) || (keyCode == KeyEvent.KEYCODE_BACK)
	         			   							&& event.getRepeatCount() == 0) {
	             	   dialog.cancel();
	                }
	                return false; // Any other keys are still processed as normal
	            }
	        });
	        if (LOG_DEBUG) Log.d(TAG, "Airplane Mode on Correlation Dialog shud be shown by now!!! contextual : ");
	        
	        AlertDialog wifiOnDialog = builder.create();
	        return wifiOnDialog;
	    }
	 	
	 	
	 	
	 	/**
	 	* This method checks if Wifi Action Consent dialog needs to be shown
	 	*
	 	* @param ct - context
	 	* @return true if the consent dialog needs to be shown
	 	*/
	 	public static boolean checkForWifiActionConsent(Context ct){
	 	
		 	boolean result = false;
		 	
		 	// read the shared preference
		 	if( Util.getSharedPrefStateValue(ct, WIFI_LOCATION_WARNING_PREF, TAG)){	 	
			 	// set the shared pref so that the dialog is not shown again
			 	Util.setSharedPrefStateValue(ct, WIFI_LOCATION_WARNING_PREF, TAG, false);		 	
			 	// show dialog only if location trigger is NOT blacklisted.
			 	result = !(PublisherFilterlist.getPublisherFilterlistInst()
			 	                                             .isBlacklisted(ct, LOCATION_TRIGGER_PUB_KEY));
		 	}
		 	
		 		return result;
	 	}
	  
}
