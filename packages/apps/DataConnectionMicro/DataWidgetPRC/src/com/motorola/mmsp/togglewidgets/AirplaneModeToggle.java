/*
 * @(#)AirplaneModeToggle.java
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
import android.content.Intent;
import android.provider.Settings;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties ;

public class AirplaneModeToggle extends BaseTogglerWidget {

	private static final String AIRPLANE_STATE_SET_KEYWORD = "state";
	
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

	/** Gets the state of AirplaneMode
	 * 
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED
	 */
	@Override
	protected State getServiceState(Context context) {
        if( this.service_state_intent_recved_this_time == false )
        {
            myLog(LOG_TAG, "getServiceState.".concat(State.INTERMEDIATE_STATE.toString()));
            return State.INTERMEDIATE_STATE;
        }

		State result = State.convert(
				Settings.System.getInt(context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON, State.DISABLED_STATE.ordinal()
				)
		);
		myLog(LOG_TAG, "getServiceState.".concat(result.toString()));
		return result;
	}

	/**
	 * Toggles the state of Airplane Mode
	 * 
	 * @param context
	 * @param serviceState
	 *            Indicates the present Service State
	 * @return Indicates the state after the toggling (STATE_ENABLED,
	 *         STATE_DISABLED, STATE_INTERMEDIATE)
	 */
	protected void toggleService(Context context, State serviceState) {
				
		final String PREFIX = "ToggleService.";
		myLog(LOG_TAG, PREFIX);
		Intent intent = null;
		//boolean inEmergencyCall; //SystemProperties.getBoolean(TelephonyProperties.PROPERTY_IN_EMERGENCY_CALL, false);
		
		switch (serviceState) {
			case DISABLED_STATE:
				if (SystemProperties.getBoolean(TelephonyProperties.PROPERTY_INECM_MODE, false) //|| inEmergencyCall
				        ){
					Intent ecmDialogIntent = new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null);
                                   ecmDialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                   context.startActivity(ecmDialogIntent);
				}
				else{
				       Settings.System.putInt(context.getContentResolver(),
				       Settings.System.AIRPLANE_MODE_ON, 1);
				       intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				       intent.putExtra(AIRPLANE_STATE_SET_KEYWORD, true);
				       context.sendBroadcast(intent);
				       myLog(LOG_TAG, PREFIX.concat(State.ENABLED_STATE.toString()));
				}
				break;

			case ENABLED_STATE:
						
				Settings.System.putInt(context.getContentResolver(),
				Settings.System.AIRPLANE_MODE_ON, 0);
				intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				intent.putExtra(AIRPLANE_STATE_SET_KEYWORD, false);
				context.sendBroadcast(intent);
				myLog(LOG_TAG, PREFIX.concat(State.DISABLED_STATE.toString()));
				break;

			default:	
				
				myLog(LOG_TAG, PREFIX.concat(State.INTERMEDIATE_STATE.toString()));
				break;
		}
	}


	/**
	 * @return the availability of the Service 
	 */
	protected Availability getServiceAvailability(Context context) {
		try {
			Settings.System.getInt(context.getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON);
			return Availability.ENABLED_SERVICE;
		} catch(Exception e) {
			return Availability.DISABLED_SERVICE;
		}
		
	}
	
	/**
	 * @return the Sleeve Image to be used. It also depends upon the previous State
	 */
	private static boolean sleeveState = false;
	
	protected boolean getSleeveState() {
		
		return sleeveState;
		
	}
	
	protected int getSleeveImageName(State currentState) {
		
		switch (currentState) {
			case ENABLED_STATE:
				sleeveState = true;
				return R.drawable.sleeve_airplane_on;
				
			case DISABLED_STATE:
				sleeveState = false;
				return R.drawable.sleeve_toggle_off;

			default:
				return sleeveState?
					R.drawable.sleeve_airplane_on:
					R.drawable.sleeve_toggle_off;	
				
		}
	}
	
	
	/**
	 * @return the Airplane Mode Image 
	 */
	protected int getTogglerImageName() {
		return R.drawable.ic_toggle_airplane;
	}
	
	/**
	 * @return the Text color to be used based on the State 
	 */
	protected int getStatusTextColor(State currentState) {
		
		return (currentState == State.ENABLED_STATE?
				R.color.airplaneOnColor:
				R.color.offColor);					
	}
	
	/**
	 * @return the Toast Text to be displayed when the service is not available 
	 * For Airplane mode this will never be used.
	 */
	protected int getToastText() {
		return R.string.AirplanetoastText;
	}
}
