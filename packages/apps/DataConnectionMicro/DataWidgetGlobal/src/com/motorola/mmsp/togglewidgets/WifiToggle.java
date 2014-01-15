/*
 * @(#)WifiToggle.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * Ankur Agarwal 2009/11/06  IKAPP-616		  Initial version 
 *
 */
package com.motorola.mmsp.togglewidgets;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

public class WifiToggle extends BaseTogglerWidget {

    /**
     * This method returns the state the widget should be shown based on mobile
     * data state and availability.
     *
     * @param context the context.
     *
     * @return the widget state
     */
    protected State getWidgetState(Context context)
    {
        return (getServiceState(context));
    }

	/**
	 * Gets the state of WiFi
	 * 
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED
	 */
	protected State getServiceState(Context context) {

		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		State wifiState;
		if(wifiManager != null) {
			switch (wifiManager.getWifiState()){
				
				case WifiManager.WIFI_STATE_DISABLED:
					wifiState =  State.DISABLED_STATE;
					break;
					
				case WifiManager.WIFI_STATE_ENABLED:	
					wifiState = State.ENABLED_STATE;
					break;
					
				default:
					wifiState = State.INTERMEDIATE_STATE;
			}			
		}
		else {
			
			wifiState = State.DISABLED_STATE;
			myLog(LOG_TAG,"Wifi Service Not Available");
		}
		myLog(LOG_TAG,"getServiceState.".concat(wifiState.toString()));
		return wifiState;
	}

	
	/**
	 * Toggles the state of WiFi
	 * 
	 * @param context
	 * @param serviceState
	 *            Indicates the present Service State
	 * @return Indicates the state after the toggling (STATE_ENABLED,
	 *         STATE_DISABLED, STATE_INTERMEDIATE)
	 */	
	protected void toggleService(Context context, State serviceState) {
		
		if(serviceState != State.INTERMEDIATE_STATE)
		{
			WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);		
			if(wifiManager != null)
			{
                             int wifiApState = wifiManager.getWifiApState();
                             if ((serviceState != State.ENABLED_STATE)
                                  && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                                   (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                                       wifiManager.setWifiApEnabled(null, false);
                                 }
			     wifiManager.setWifiEnabled(!(serviceState == State.ENABLED_STATE));
			}
		}
		myLog(LOG_TAG, "ToggleService");
	}


	/**
	 * @return the availability of the Service 
	 */

	protected Availability getServiceAvailability(Context context) {
		
		if (ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_WIFI))
		{
			myLog(LOG_TAG,"Wifi ServiceAvailability Available");
			return Availability.ENABLED_SERVICE;
		}
		else 
		{
			myLog(LOG_TAG,"Wifi ServiceAvailability unavailable");
			return Availability.DISABLED_SERVICE;
		}
	}
	
	private static boolean sleeveState = false;
	
	protected boolean getSleeveState() {
		
		return sleeveState;
		
	}
	
	/**
	 * @return the Sleeve Image to be used. 
	 * It also depends upon the previousState of the widget
	 */
	protected int getSleeveImageName(State currentState) {
	
		switch (currentState) {
			case ENABLED_STATE: 
				sleeveState = true;
				return R.drawable.sleeve_wifi_on;
				
			case DISABLED_STATE:
				sleeveState = false;
				return R.drawable.sleeve_toggle_off;
				
			default:
				return (sleeveState?
							R.drawable.sleeve_wifi_on:
							R.drawable.sleeve_toggle_off);
		}
	}
	
	
	/**
	 * @return the WiFi Image 
	 */
	protected int getTogglerImageName() {
		return R.drawable.ic_toggle_wifi;
	}
	
	/**
	 * @return the Text color to be used based on the State 
	 */
	protected int getStatusTextColor(State currentState) {
		
		return (currentState == State.ENABLED_STATE?
					R.color.wifiOnColor:
					R.color.offColor);
	}
	
	/**
	 * @return the Toast Text to be displayed when the service is not available 
	 */
	protected int getToastText() {
		return R.string.WifitoastText;
	}
}
