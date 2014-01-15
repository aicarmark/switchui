/*
 * @(#)BluetoothToggle.java
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


import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothToggle extends BaseTogglerWidget {

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
	 * Gets the state of Bluetooth
	 * 
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED, STATE_INTERMEDIATE
	 */
	protected State getServiceState(Context context) {
		
		myLog(LOG_TAG, "getServiceState");
		State bluetoothState;
		BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mAdapter != null) {
			
			switch (mAdapter.getState()) {
				case BluetoothAdapter.STATE_ON:
					bluetoothState =  State.ENABLED_STATE;
					break;	
				
				case BluetoothAdapter.STATE_OFF:
					bluetoothState = State.DISABLED_STATE;
					break;
					
				default: 
					bluetoothState = State.INTERMEDIATE_STATE;
			}
		}
		else {
			myLog(LOG_TAG,"Service Not Available");
			bluetoothState = State.DISABLED_STATE;
		}
		myLog(LOG_TAG,"getServiceState.".concat(bluetoothState.toString()));
		return bluetoothState;
	}

	
	/**
	 * Toggles the state of Bluetooth
	 * 
	 * @param context
	 * @param serviceState
	 *            Indicates the present Service State
	 * @return Indicates the state after the toggling (STATE_ENABLED,
	 *         STATE_DISABLED, STATE_INTERMEDIATE)
	 */
	protected void toggleService(Context context, State serviceState) {
		
		myLog(LOG_TAG, "Widget ToggleService");
		BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mAdapter != null)
			switch (serviceState) {
				
				case ENABLED_STATE:
					mAdapter.disable();
					break;
					
				case DISABLED_STATE:
					mAdapter.enable();
					break;
			}
	}

	/**
	 * @return the availability of the Service 
	 * For Airplane Mode its always ON.
	 */
	protected Availability getServiceAvailability(Context context) {
		
		BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mAdapter == null) {
			return Availability.DISABLED_SERVICE;
		} else {
			return Availability.ENABLED_SERVICE;
		}
	}

	
	private static boolean sleeveState = false;
	
	protected boolean getSleeveState() {
		
		return sleeveState;
		
	}
	
	/**
	 * @returns the Sleeve Image to be used. 
	 * It also depends upon the previousState of the widget
	 */
	protected int getSleeveImageName(State currentState) {
		
		switch (currentState) {
			
			case ENABLED_STATE:
				sleeveState = true;
				return R.drawable.sleeve_bt_on;
				
			case DISABLED_STATE:
				sleeveState = false;
				return R.drawable.sleeve_toggle_off;
				
			default:	
				return sleeveState?
						R.drawable.sleeve_bt_on:
						R.drawable.sleeve_toggle_off;
				
		}
	}
	
	
	/**
	 * @return the Bluetooth Image 
	 */
	protected int getTogglerImageName() {
		return R.drawable.ic_toggle_bluetooth;
	}
	
	/**
	 * @return the Text color to be used based on the State 
	 */
	protected int getStatusTextColor(State currentState) {
		
		return (currentState == State.ENABLED_STATE?
				R.color.bluetoothOnColor:
				R.color.offColor);
	}
	
	/**
	 * @return the Toast Text to be displayed when the service is not available 
	 */
	protected int getToastText() {
		return R.string.BluetoothtoastText;
	}
}
