/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        10/19/2012   NA                1.0
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.motorola.contextual.cache.SystemProperty;

/**
 * <code><pre>
 * CLASS:
 *  Receiver for Intent.ACTION_AIRPLANE_MODE_CHANGED
 * 
 * RESPONSIBILITIES:
 *  Sets the airplane mode.
 * USAGE:
 * 
 * </pre></code>
 */
public class AirplaneModeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if(action != null && Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            // I do not see any constant for the extra "state"
            boolean state = intent.getBooleanExtra("state", false);
            SystemProperty.setAirplaneMode(state);
        }
    }

}
