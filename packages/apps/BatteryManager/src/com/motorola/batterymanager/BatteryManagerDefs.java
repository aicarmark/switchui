/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager;

/**
 * All BatteryManager interface fields are here
 */

public class BatteryManagerDefs {

    // Start Intent for BatteryManager
    public static final String ACTION_START_BATTERY_MANAGER =
            "com.motorola.batterymanager.START_BATTERY_MANAGER";

    // Broadcast intent on BatteryManager state change
    public static final String ACTION_BM_STATE_CHANGED =
            "android.intent.action.batterymanager.BM_STATE_CHANGED";
    public static final String KEY_BM_MODE = "bmMode";
    public static final String KEY_DATA_CONNECTION = "dataConnection";

    public static final long NO_DURATION = 0L;

    // enableApnType returns
    public static final int APN_ENABLE_REQUEST_ACCEPTED = 0;
    public static final int APN_TYPE_NOT_SUPPORTED = 1;

    public static final class DataSettings {
        public static final int DATA_ON_ALWAYS = 0;
        public static final int DATA_OFF_ALWAYS = 1;
        public static final int RESET_DATA_ON_ALWAYS = 2;
        public static final int RESET_DATA_OFF_ALWAYS = 3;
    }

    // Battery Manager working modes
    public static final class Mode {
        public static final int PERFORMANCE = 0;
        public static final int NIGHT_SAVER = 1;
        public static final int BATTERY_SAVER = 2;
        public static final int CUSTOM = 3;
    }

    public static final class DataConnection {
        public static final int ON = 0;
        public static final int OFF = 1;
    }

}
