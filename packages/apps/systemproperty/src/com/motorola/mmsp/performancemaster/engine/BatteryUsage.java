/**
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 */

package com.motorola.mmsp.performancemaster.engine;

public class BatteryUsage {
    private final static String TAG = "batteryusage";

    public float collectDisplayUsage() {
        /*
         * long batteryrealtimems =
         * mBattStats.computeBatteryRealtime(SystemClock.elapsedRealtime() *
         * 1000, mStatsType)/1000; long screenontimems =
         * mBattStats.getScreenOnTime(batteryRealtime, mStatsType)/1000; float
         * screenontimeS=(float)(screenontimems/1000); float
         * batteryrealtimeS=(float)(batteryrealtimems/1000); return
         * screenontimeS/batteryrealtimeS;
         */
        return 0.08f;
    }

    public float collectWifiUsage() {
        return 0.04f;

    }

    public float collectBluetoothUsage() {
        return 0.01f;

    }

    public float collectCellStandbyUsage() {
        return 0.99f;

    }

    public float collectVoiceCallsUsage() {
        return 0.01f;

    }

    public float collectAndroidSystemUsage() {
        return 0.1f;

    }

    public float collectPhoneIdleUsage() {
        return 0.8f;

    }

    public float collectDataCallsUsage() {
        return 0.01f;

    }
}
