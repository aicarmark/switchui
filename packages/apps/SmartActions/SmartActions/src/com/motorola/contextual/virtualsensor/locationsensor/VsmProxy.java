/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19       Initial version
 * w04917        2012/07/16 IKCTXTAW-492      Broadcast INTENT_POI_EVENT
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;
import android.content.Intent;

import com.motorola.contextual.smartprofile.locations.LocationUtils;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

public final class VsmProxy {
    public static final String TAG = "LSAPP_VSM";

    private LocationSensorApp mLSApp;

    // ok, checkin module should be in lsapp, for now, it lives inside lsman.
    @SuppressWarnings("unused")
    private VsmProxy() { }
    public VsmProxy(LocationSensorApp lsapp) {
        mLSApp = lsapp;
    }

    /**
     * register vsm listeners and unregister upon start and stop
     */
    public void start() {
        //mReceiver.start();
    }
    public void stop() {
        //mReceiver.stop();
    }

    /**
     * broadcast entering into poi intent from location manager. LOCATION_MATCHED_EVENT
     * @param poi
     */
    public void sendVSMLocationUpdate(String poi) {
        LocationUtils.sendNotifyIntent(mLSApp.mContext, poi);
        broadcastPoiEvent(poi);
    }

    /**
     * Broadcast POI entry/exit event to components outside ConditionPublisher.
     * Used by SmartNetwork.
     * @param poi
     */
    public void broadcastPoiEvent(String poi) {
        if (poi != null && !poi.isEmpty()) {
            LSAppLog.d(TAG, "Broadcasting poi event: " + poi);
            Intent intent = new Intent(INTENT_POI_EVENT);
            intent.putExtra(INTENT_EXTRA_POI, poi);
            mLSApp.mContext.sendBroadcast(intent, PERMISSION);
        }
    }
}
