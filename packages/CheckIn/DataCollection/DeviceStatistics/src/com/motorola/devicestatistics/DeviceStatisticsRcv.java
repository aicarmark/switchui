/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.motorola.devicestatistics.eventlogs.EventNote;
import com.motorola.devicestatistics.wlmonitor.WakelockMonitorConstants;

/**
 * Receiver for Device Statistics intents
 */
public class DeviceStatisticsRcv extends BroadcastReceiver {

    private final static String TAG = "DeviceStatisticsRcv";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Utils.RunInBackgroundThread() {
            public void run() {
                onReceiveImpl(context, intent);
            }
        };
    }

    private void onReceiveImpl(Context context, Intent intent) {
        if (Watchdog.isDisabled()) return;

        if(intent == null) return;
        String action = intent.getAction();
        if(action == null) return;

        if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            /** Now wakelock monitor starts from here **/
            Utils.Log.v(TAG, "onBootCompleted: starting wakelock monitor");
            EventNote.noteEvent(context, intent, System.currentTimeMillis());
            Intent wlIntent = new Intent(WakelockMonitorConstants.Intents.ACTION_START_MONITOR);
            wlIntent.setClassName("com.motorola.devicestatistics",
                    "com.motorola.devicestatistics.wlmonitor.WakelockMonitorService");
            context.startService(wlIntent);
        }
        // If Wifi connect, no work.

        boolean isWifiUp = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);

        if (isWifiUp) return;

	    intent.setClass(context, DeviceStatisticsSvc.class);
	    context.startService(intent);
    }
}

