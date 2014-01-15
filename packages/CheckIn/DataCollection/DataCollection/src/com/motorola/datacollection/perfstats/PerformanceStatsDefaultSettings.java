/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/02/07   IKCTXTAW-359    Initial version
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.HashMap;
import java.util.Map;

import com.motorola.kpi.perfstats.LogSetting;

/**
 * @author w04917 Brian Lee
 * Defines default PerfStats settings.
 * Stand-alone file to enable overlays for different products.
 */
public class PerformanceStatsDefaultSettings {

    public static final Map<String, String> DEFAULT_SETTINGS = new HashMap<String,String>();

    static {
        DEFAULT_SETTINGS.put(LogSetting.KEY_ENABLED, "1");
        DEFAULT_SETTINGS.put(LogSetting.KEY_LOG_CPU_METRIC, "1");
        DEFAULT_SETTINGS.put(LogSetting.KEY_LOG_MEMORY_METRIC, "1");
    }
}
