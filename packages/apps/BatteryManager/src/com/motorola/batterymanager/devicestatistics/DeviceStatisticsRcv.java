/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.motorola.batterymanager.Utils;
import com.motorola.batterymanager.wlmonitor.WakelockMonitorConstants;

/**
 * Receiver for Device Statistics intents
 */
public class DeviceStatisticsRcv extends BroadcastReceiver {

    private final static String TAG = "DeviceStatisticsRcv";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            /** Now wakelock monitor starts from here **/
            Utils.Log.v(TAG, "onBootCompleted: starting wakelock monitor");
            Intent wlIntent = new Intent(WakelockMonitorConstants.Intents.ACTION_START_MONITOR);
            wlIntent.setClassName("com.motorola.batterymanager",
                    "com.motorola.batterymanager.wlmonitor.WakelockMonitorService");
            context.startService(wlIntent);
        }
        // If Wifi connect, no work.
        boolean isWifiUp = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
        if (isWifiUp) return;

	intent.setClass(context, DeviceStatisticsSvc.class);
	context.startService(intent);
    }
}
