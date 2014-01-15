/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/16   IKCTXTAW-492    Use new intent from LocationSensor
 */

package com.motorola.contextual.smartnetwork;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.INTENT_EXTRA_POI;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.INTENT_POI_EVENT;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PoiReceiver extends BroadcastReceiver {
    private static final String TAG = PoiReceiver.class.getSimpleName();
    private static final boolean LOGD = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmartNetwork.isEnabled()) {
            String poi = getPoi(intent);
            if (poi != null) {
                Intent poiIntent = new Intent();
                poiIntent.setAction(PoiHandler.INTENT_POI_EVENT);
                poiIntent.setClass(context, PoiHandler.class);
                poiIntent.putExtra(PoiHandler.EXTRA_POI, poi);
                context.startService(poiIntent);
            }
        }
    }

    private String getPoi(Intent intent) {
        String poi = null;
        if (intent != null) {
            String action = intent.getAction();
            if (INTENT_POI_EVENT.equals(action)) {
                poi = intent.getStringExtra(INTENT_EXTRA_POI);
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Received poi event: " + poi);
                }
            }
        }
        return poi;
    }
}
