/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: EventConstants.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date               Author            Comments
 * Nov 02, 2010       bluremployee      Created file
 * Sep 12, 2011       w04917            IKCTXAW-357 Add parsing for Performance Stats
 **********************************************************
 */
package com.motorola.devicestatistics.eventlogs;

/**
 * @author bluremployee
 *
 */
public class EventConstants {

    public final static class Intents {
        public final static String LOG_ACTION =
                "com.motorola.eventlogger.START_LOG";
    }

    // Defaults
    public final static long LOG_INTERVAL = 2L * 60L * 60L * 1000L;
    //public final static long LOG_INTERVAL = 5L * 60L * 1000L;
    public final static String LOG_BUFFER = "/dev/log/events";
    public final static int LOG_CONFIG = 7; // L1 + L2 + L3 + KBD + PERF_STATS + CAM

    // Date Low limit
    public final static long DATE_LOWLIMIT = 1262304000; // Fri, 01 Jan 2010 00:00:00 GMT
    
    public final static class Source {
        public final static int EVENTLOG = 0;
        public final static int RECEIVER = 1;
        public final static int HELPER = 2;
        public final static int ADDNL_LOG = 3;
    }

    public final static class Events {
        // These are based off event logs
        public final static String SCREEN_STATE = "power_screen_state";
        public final static String AM_RESUME_ACTIVITY = "am_resume_activity";
        public final static String AM_PAUSE_ACTIVITY = "am_pause_activity";
        public final static String AM_RELAUNCH_RESUME_ACTIVITY = "am_relaunch_resume_activity";
        public final static String AM_RESTART_ACTIVITY = "am_restart_activity";
        //public final static String BT_STATUS = "bluetooth_status";
        public final static String DC_KEYBOARD = "dc_kbd";
        public final static String DC_MM = "dc_mm";
        public final static String PERF_STATS = "perf_stats";
        public final static String DC_VIB = "dc_vib";
        public final static String DC_SND = "dc_snd";
        public final static String DC_AUTOCMPL = "dc_autocmpl";
        public final static String DSBT_STATUS = "dsbt_status";
        public final static String DC_MMLOG = "dc_mmlog";

        // NOTE: The ordering here is used in event state, config, DO NOT CHANGE THIS
        public final static String[] EVENT_FILTER = new String[] {
            DC_KEYBOARD,
            DC_MM,
            PERF_STATS,
            DC_VIB,
            DC_SND,
            DC_AUTOCMPL,
            DSBT_STATUS,
            DC_MMLOG,
        };
        
        // These are based off dynamic receiver classes
        public final static String CHARGE = "chrg";
        public final static String DISCHARGE = "dchrg";
        public final static String BOOTUP = "btup";
        public final static String SHUTDOWN = "shtdn";
        public final static String BATTLOW = "btlow";
        public final static String BATTSTS = "bsts";
        public final static String BATTLVL = "blvl";
        public final static String CHARGEFULL = "bfc";
        public final static String SETTINGSTAT = "settingstat";
    }

    // TODO: This class is no longer used, and should be removed
    public final static class RawEvents {

        public final static String[] EVENT_FILTER = new String[] {
        };

        public final static boolean isRawEvent(String event) {
            for(int i = 0; i < EVENT_FILTER.length; ++i) {
                if(EVENT_FILTER[i].equals(event))
                    return true;
            }
            return false;
        }
    }

    public final static class SizeFilters {
        // NOTE: These need to be in-sync with Config.java levels
        public final static String FILTER = "0,25600,35328,60928,9216,9216,-1,9216,-1";
        public final static String DEFAULT_SIZES = "0,0,0,0,0,0,0,0,0";
        public final static int DEFAULT_LEVELS = 9;
    }

    public final static String CHECKIN_ID = "EventLogs";
    public final static String SETTING_CHK_ID = "SettingLogs";
    public final static String DC_KEYBOARD_ID = "DC_KBD";
    public final static String DC_MULTIMEDIA_ID = "DC_MM";
    public final static String PERF_STATS_ID = "PERF_STATS";
    public final static String DC_MULTIMEDIALOG_ID = "DC_MMLOG";
    public final static String DC_MMLOG_VERSION = "4.8";

    public final static String OPTIONS = "com.motorola.eventlogger.options";
    // Scheduling options - sample rate doesnt work as yet
    public final static String SAMPLE_RATE = "log.samplerate";
    public final static String TIME_REFERENCE = "log.timereference";
    // Boot time corrections
    public final static String BOOTTIME_REFERENCE = "log.bootreference";
    public final static String BOOTTIME_SEQNUM = "log.bootseqnum";
    public final static String KERNEL_BOOTTIME_REFERENCE = "log.kernelbootreference";
    // Size filters
    public final static String SIZEFILTER = "log.sizefilter";
    public final static String SIZECOUNTER = "log.sizecounter";
    public final static String LOGFULL = "log.sizefull";
    public final static String SIZETIME_REFERENCE = "log.sizetime";
    public final static String OFLOWSIZECOUNTER = "log.oflow.counter";
    public final static String MISSEDEVENTCOUNTER = "log.missev.counter";
    public final static String LEVELCOUNTER = "log.levelcounter";

    // Event options - temp persistent stores
    public final static String SHUTDOWN_TIME = "events.shutdown";
    public final static String BOOTUP_TIME = "events.bootup2";
    public final static String BATTLOW_TIME = "events.battlow";
    public final static String CHARGE_TIME = "events.chgtime";
    public final static String DISCHARGE_TIME = "events.dchgtime";
    public final static String LEVEL_TIME = "events.lvltime";
    public final static String STATUS_TIME = "events.ststime";
    public final static String CHARGE_FULL_TIME = "events.chgfulltime";
    public final static String SETTING_DATA_KEY = "events.phsettingslog2";
    public final static String BUILD_ID = "events.settings.buildid";
    public final static String CUMULATIVE_DISCHARGE = "events.dchrg.full";
    
    // Config options
    // Some explanation needed here - input log levels
    // 0 - Nothing will be logged
    // 1 - L1 logs
    // 2 - L1 + L2 logs
    // 3 - L1 + L2 + L3 logs
    public final static String CONFIG_LEVEL = "config.level";

    public final static class Debug {
        public final static String RUNTIME = "debug.runtime";
        public final static String RUNS = "debug.runs";
    }

    // Bakground event thread parameters
    public final static String EVENTBGTHREAD_REAL = "bgevtthrd.lastreal";
    public final static String EVENTBGTHREAD_ELAPSED = "bgevtthrd.lastelapsed";
    public final static String EVENTBGTHREAD_STATS = "bgevtthrd.stats2";
    public final static String EVENTBGTHREAD_SANITYERRORS = "bgevtthrd.sanityerror";
}

