/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

/**
 *<code><pre>
 * CLASS:
 *  Boot complete recevier
 *
 * RESPONSIBILITIES:
 * 	Start the service upon boot
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "LSAPP_Boot";
    private static final String LSS = "Location Sensor Services";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent myIntent = new Intent(context, LocationSensorManager.class);
        //Intent myIntent = new Intent(context, NoCellLocationManager.class);
        if (BOOT_COMPLETE.equals(action)) {
            myIntent.putExtra(INTENT_PARM_STARTED_FROM_BOOT, true);
            LSAppLog.pd(TAG, LSS+" started: from boot complete");
        } else if (VSM_INIT_COMPLETE.equals(action)) {
            myIntent.putExtra(INTENT_PARM_STARTED_FROM_VSM, true);
            LSAppLog.pd(TAG, LSS+" started: from boot VSM init complete");
        } else if (ADA_ACCEPTED_KEY.equals(action)) {
            myIntent.putExtra(INTENT_PARM_STARTED_FROM_ADA, intent.getIntExtra(ADA_ACCEPTED_KEY, 0));
            LSAppLog.d(TAG, LSS+" started: from analytics.ada_accepted");
        } else if (LOCATION_CONSENT_KEY.equals(action)) {
            myIntent.putExtra(INTENT_PARM_STARTED_FROM_CONSENT, true); // intent.getStringExtra("has_user_loc_consent"));
            LSAppLog.pd(TAG, LSS+" started: from LOCATION_CONSENT_KEY consent accepted !");
        }

        // we are virtual sensor and we need to notify VSM we are up and running once VSM comes up.
        ComponentName component = context.startService(myIntent);
        if (component != null) {
            LSAppLog.i(TAG, LSS+" started: " + component.toShortString());
        } else {
            LSAppLog.i(TAG, LSS+" start failed.");
        }
    }
}
