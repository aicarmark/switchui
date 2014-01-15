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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

public class APRStartupIntentReceiver extends BroadcastReceiver {
	static final String TAG = "APRStartupIntentReceiver";

	static boolean debug_on_startup = false;
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	if ( debug_on_startup ) {
    		android.os.Debug.waitForDebugger();
    	}
    	
    	APRScanFileBootInfo.StarupReceived();
    	
    	// Create the intent which will launch the service        
        Intent serviceIntent = new Intent(context, APRService.class);
        
        // Start our service
        context.startService(serviceIntent);
        
        // Start to run ftmipcd and bp_panic to get CDMA panic
        Log.e(TAG, "Start to run ftmipcd and bp_panic to get CDMA panic");
        //SystemProperties.set("sys.usb.attach.state", "1");
    }
}
