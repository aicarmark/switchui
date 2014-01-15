/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: WlMonitorPreferences.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       FJM225       Created file
 *                -Jatin
 **********************************************************
 */

package com.motorola.batterymanager.wlmonitor;

public final class WakelockMonitorConstants {

    // Shared Preferences
    public final static String OPTIONS = "com.motorola.batterymanager.wlmonitor.options";

    // Options keys 
    public final static String KEY_WAKELOCK_TIMEOUT = "wakelock.timeout";
    public final static String KEY_SYSTEM_MONITOR = "wakelock.monitorsystem";
    public final static String KEY_WAKELOCK_CHECKIN_INTERVAL = "wakelock.checkininterval";

    // Whitelist Options 
    public final static String WHITELIST_ACTION_FORCECLOSE = "forceclose";
    public final static String WHITELIST_ACTION_IGNORE = "ignore";
    public final static String WHITELIST_ACITON_ERROR = "readerror";
    public final static String WHITELIST_ACTION_REMOVE = "remove";

    // Defaults
    public final static boolean DEF_SYSTEM_MONITOR = true;
    public final static int DEF_TIMEOUT_INDEX = 3; /* should be 45 mins */
    public final static long DEF_CHECKIN_INTERVAL = 24L * 3600L * 1000L; /* 24 Hours */
    //public final static long DEF_CHECKIN_INTERVAL = 1800L * 1000L; /* 24 Hours */

    // Startup Action
    public final static class Intents {
        public final static String ACTION_START_MONITOR = "com.motorola.batterymanager.action.START_MONITOR";
    }
}

