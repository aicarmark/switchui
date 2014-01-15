/*
 * @(#)SmartProfileLocChangeReceiver.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/09/11 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile;

import com.motorola.contextual.smartprofile.locations.LocConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This class listens to the location change broadcast intent from location sensor
 * or RuleImporter and starts the SmartProfileLocService to process this intent.
 *
 *<code><pre>
 * CLASS:
 * 	extends BroadcastReceiver.
 *
 * 	implements
 *      Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	handles the location change broadcast intent from location sensor.
 *
 * COLABORATORS:
 * 	Uses LocationSensor provider to create an entry in the POI table of location sensor.
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class SmartProfileLocChangeReceiver extends BroadcastReceiver implements Constants, LocConstants {

    private static final String TAG = SmartProfileLocChangeReceiver.class.getSimpleName();
    
    /** onReceive()
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        if(LOG_DEBUG) Log.d(TAG, "In onReceive to handle "+intent.toURI());
        String action = intent.getAction();
        if(intent.getExtras() != null && action != null) {

            Bundle bundle = null;
            if(action.equals(LOCATION_CHANGE_ACTION)) {

                String status = intent.getStringExtra(STATUS);
                if(LOG_DEBUG)  Log.d(TAG, "status = "+status);

                if (status != null && status.equals(STATUS_LOCATION)){
                    bundle = intent.getBundleExtra(LOCDATA);
                }

            } else if(action.equals(LOCATION_IMPORT_ACTION)) {
                String st = intent.getStringExtra(EXTRA_LOCATION_DATA);
                bundle = new Bundle();
                bundle.putString(EXTRA_LOCATION_DATA, st);
            }

            if(bundle != null) {
                if(LOG_DEBUG) Log.d(TAG, "Starting up SmartProfileLocService");

                Intent serviceIntent = new Intent(context, SmartProfileLocChangeService.class);
                serviceIntent.putExtras(bundle);
                context.startService(serviceIntent);
            }
        }
    }
}