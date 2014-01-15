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

import android.telephony.PhoneStateListener; 
import android.telephony.ServiceState;
import android.telephony.TelephonyManager; 

public class APRPhoneStateListener extends PhoneStateListener {
		
	static final String TAG = "APRPhoneStateListener";

	// temporarily set this to true.  the listener is not
	//   working right.
	static private boolean phone_in_service = false; 

	static private APRPhoneStateListener mInstance = null;

	static public APRPhoneStateListener getInstance() {
		if(mInstance == null) {
			mInstance = new APRPhoneStateListener();
		}

		return mInstance;
	}

	private APRPhoneStateListener() {;}
	
	static public boolean isPhoneInService()
	{
		return phone_in_service;
	}
	
	public final void onServiceStateChanged( ServiceState serviceState) {
		
		super.onServiceStateChanged( serviceState );
		
		switch ( serviceState.getState() ) 
		{
		case ServiceState.STATE_IN_SERVICE:
			phone_in_service = true;
			APRDebug.APRLog( TAG, "Phone Is In Service! ");
			break;
		default:
			phone_in_service = false;
			APRDebug.APRLog( TAG, "Phone is not in Service! " );
			break;
		}
	}
	
	public final void onCallStateChanged(int state, String incomingNumber)
	{                 
		super.onCallStateChanged(state, incomingNumber);
				
		switch(state)                 
		{                         
			case TelephonyManager.CALL_STATE_IDLE:                                 
				APRDebug.APRLog( TAG, "phone is idle");                                 
				break;                         
			case TelephonyManager.CALL_STATE_OFFHOOK:
				APRDebug.APRLog( TAG, "phone is off hook");
				break;                        
			case TelephonyManager.CALL_STATE_RINGING:
				APRDebug.APRLog( TAG, "phone is ringing");
				break;
			default:
				APRDebug.APRLog( TAG, "The state is " + state);
		}	
	}
}
