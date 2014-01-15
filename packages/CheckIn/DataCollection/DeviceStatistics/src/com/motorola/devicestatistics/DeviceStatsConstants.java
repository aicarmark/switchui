/**
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

public interface DeviceStatsConstants {
    public static final String DEVSTATS_CHECKIN_STATS_ALARM =
        "com.motorola.devicestatistics.CHECKIN_STATS_ALARM";
    public static final String DEVSTATS_CHECKIN_STATS_USB_EXTRA =
        "com.motorola.devicestatistics.stats.charger.extra";
    public final static String TIME_WARP_MONITOR_ACTION =
            "com.motorola.devicestatistics.timewarp.monitor";

    public static final boolean GLOBAL_DUMP = false;

    public static final long SECONDS_IN_MINUTE = 60L;
    public static final long SECONDS_IN_HOUR = 60L * 60L;
    public static final long SECONDS_IN_DAY = 24L * 60L * 60L;

    public static final long MS_IN_MINUTE = SECONDS_IN_MINUTE * 1000L;
    public static final long MS_IN_HOUR = SECONDS_IN_HOUR * 1000L;
    public static final long MS_IN_DAY = SECONDS_IN_DAY * 1000L;

    public static final long NANOSECS_IN_MSEC = 1000000000L/1000L;
    public static final long MS_IN_SEC = 1000L;
    public static final long MICROSEC_IN_MSEC = 1000L;

    public static final String SYSTEM_PROCESS_NAME = "system";
    public static final long INVALID_PID = -1;
}
