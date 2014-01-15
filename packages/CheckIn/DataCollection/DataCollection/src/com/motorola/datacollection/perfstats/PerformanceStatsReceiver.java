/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/01/20   IKCTXTAW-359    Initial version
 *
 */

package com.motorola.datacollection.perfstats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PerformanceStatsReceiver extends BroadcastReceiver {
    private static final String TAG = "PerfStatsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Received - " + action);
            }

            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                Intent serviceIntent = new Intent(PerformanceStatsWorker.INTENT_INIT_PERF_STATS);
                context.startService(serviceIntent);
            }
        }
    }
}
