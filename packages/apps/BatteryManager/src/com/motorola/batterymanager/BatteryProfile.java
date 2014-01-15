/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: BatteryProfile.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Feb 17, 2010	  A24178       Created file
 * Jul 13, 2010   A16462       IKSTABLETWO-2784: Dynamic Data Mode change
 **********************************************************
 */

package com.motorola.batterymanager;

/**
 * @author A24178
 *
 */
// package-scope
class BatteryProfile {
    // Core Preferences name
    public final static String OPTIONS_STORE = "com.motorola.batterymanager.powerpreferences";

    // Custom mode - Temp Preferences name
    public final static String TEMP_OPTIONS_STORE = "com.motorola.batterymanager.temp.powerpreferences";

    // Top Level Options - Presets(true)/Customs - boolean
    public final static String KEY_OPTION_IS_PRESET = "options.isPreset";

    // Preset Options - Max/All-Day/Perf - int
    public final static String KEY_OPTION_PRESET_MODE = "options.presets.mode";

    // Preset Options - Max Saver/Display Reduced Value (0-255) - int
    public final static String KEY_OPTION_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS =
            "options.presets.maxsaver.dispbright";

    // Preset Options - Max Saver / Data Timeout
    public final static String KEY_OPTION_PRESET_MAXSAVER_DATATIMEOUT =
            "options.presets.maxsaver.datatimeout";

    // Custom Options - Off-peak Start time (24 Hour, in mins) - int
    public final static String KEY_OPTION_CUSTOM_OFFPEAK_START = "options.customs.opstart";

    // Custom Options - Off-peak End time (24 Hour, in mins)- int
    public final static String KEY_OPTION_CUSTOM_OFFPEAK_END = "options.customs.opend";

    // Custom Options - Off-peak Data Timeout (mins) - int
    public final static String KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT = "options.customs.opdatatimeout";

    // Custom Options - Peak Data Timeout (mins) - int
    public final static String KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT = "options.customs.pdatatimeout";

    // Custom Options - Display Reduced Value (0-255) - int
    public final static String KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS = "options.customs.dispbright";

    // Custom Backup Store - User brightness (0-255) - int
    public final static String KEY_BACKUP_DISPLAY_BRIGHTNESS = "backups.customs.dispbright";

    // Legacy - For BC, dummy Wi-Fi option
    public final static String KEY_OPTION_PEAK_ALLOW_WIFI = "options.customs.pallowwifi";

    // Legacy - For BC, dummy Wi-Fi option
    public final static String KEY_OPTION_OFFPEAK_ALLOW_WIFI = "options.customs.opallowwifi";

    // Legacy - For User Notification
    public final static String KEY_NOTIFY_USER_PROFILE_SELECT = "options.notify.profileselect";

    // Constants
    // Type - Top Level options
    public final static boolean OPTION_PRESETS = true;
    public final static boolean OPTION_CUSTOMS = false;
    // Type - Preset options
    public final static int OPTION_PRESET_PERFORMANCE_MODE = 0;
    public final static int OPTION_PRESET_NTSAVER_MODE = 1;
    public final static int OPTION_PRESET_MAXSAVER_MODE = 2;

    // Presets/Defaults
    // Top Level default
    public final static boolean DEFAULT_OPTION_SELECT = OPTION_PRESETS;
    // Preset Mode default
    public final static int DEFAULT_PRESET_MODE = OPTION_PRESET_NTSAVER_MODE;

    // Preset Max - Values - None - move to legacy Max Battery mode
    public final static int DEFAULT_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS = 102; // 40% of 255
    public final static int DEFAULT_PRESET_MAXSAVER_DATATIMEOUT = 15; // 15 mins

    // Preset All Day - Values - Same as Customs defaults - recorded here for
    //                           clarity and future customization
    public final static int DEFAULT_PRESET_ALLDAY_OFFPEAK_START = 22 * 60; // 10pm (sooo early)
    public final static int DEFAULT_PRESET_ALLDAY_OFFPEAK_END = 5 * 60; // 5am (still early)
    public final static int DEFAULT_PRESET_ALLDAY_OFFPEAK_DATATIMEOUT = 15; // 15 mins
    public final static int DEFAULT_PRESET_ALLDAY_PEAK_DATATIMEOUT = -1; // Never timeout
    public final static int DEFAULT_PRESET_ALLDAY_DISPLAY_BRIGHTNESS = 102; // 40% of 255

    // Customs Off-peak
    public final static int DEFAULT_OFFPEAK_START = 22 * 60; // 12am (sooo early)
    public final static int DEFAULT_OFFPEAK_END = 5 * 60; // 5am (still early)
    public final static int DEFAULT_OFFPEAK_DATATIMEOUT = 15; // 15 mins

    // Customs peak
    public final static int DEFAULT_PEAK_DATATIMEOUT = -1; // Never timeout

    // Constant - Invalid data timeout
    public final static int INVALID_DATA_TIMEOUT = -1;

    // Customs display red
    public final static int DEFAULT_DISPLAY_BRIGHTNESS = 102; // 40% of 255
    public final static int DEFAULT_BACKUP_DISPLAY_BRIGHTNESS = -1; // invalid

    // Legacy - For BC, keeps Wi-Fi control a separate entity
    public final static boolean DEFAULT_OFFPEAK_ALLOW_WIFI = false;
    public final static boolean DEFAULT_PEAK_ALLOW_WIFI = false;

    // Legacy - Smart Batt Charge Thresholds for display
    public final static int SMARTBATT_CAPACITY_START_TRIGGER = 30;
    public final static int SMARTBATT_CAPACITY_STOP_TRIGGER = 40;

    // Helper methods
    public final static int getDayHour(int daymins) {
        return (int)daymins/60;
    }

    public final static int getHourMins(int daymins) {
        return daymins % 60;
    }

    // Debug - Logging
    // Preferences and keys
    public final static String LOG_STORE = "com.motorola.batterymanager.datalog";
    public final static String KEY_LOG_INDEX = "log.data.index";
    public final static String LOG_KEYS_ARRAY[] =
        {"Key1", "Key2", "Key3", "Key4", "Key5",
        "Key6", "Key7", "Key8", "Key9", "Key10",
        "Key11", "Key12", "Key13", "Key14", "Key15",
        "Key16", "Key17", "Key18", "Key19", "Key20"};

    // Log levels
    public final static int VERBOSE = 0; // additional - omit log itself in prod
    public final static int DEBUG = 1;
    public final static int INFO = 2;
    public final static int ERROR = 3;

    // Checkin level
    public final static int DISABLE_CHECKIN = ERROR + 1; // use if you want to disable checkin
    public final static int DEFAULT_CHECKIN_LEVEL = INFO;
    public final static String KEY_LOG_LEVEL = "log.level";

    // Start: Data Mode Settings
    public final static String KEY_REQ_DATA_MODE = "RequestedDataMode";
    public final static int NO_ACTIVE_MODE = 0;
    public final static int KEEP_DATA_OFF = 1;
    public final static int KEEP_DATA_ON = 2;
    public final static int DEF_REQ_DATA_MODE = NO_ACTIVE_MODE;

    public final static String KEY_KEEP_DATA_ON = "KeepDataOn";
    public final static String KEY_KEEP_DATA_ON_WITH_TIMER = "KeepDataOnWithTimer";
    public final static String KEY_KEEP_DATA_ON_TIMER_START = "KeepDataOnTimerStart";
    public final static String KEY_KEEP_DATA_ON_TIMER_END = "KeepDataOnTimerEnd";
    public final static String KEY_KEEP_DATA_OFF = "KeepDataOff";
    public final static String KEY_KEEP_DATA_OFF_PERSISTENT = "KeepDataOffPersistent";
    public final static String KEY_KEEP_DATA_ON_PERSISTENT = "KeepDataOnPersistent";

    public final static String KEY_DATA_CONNECTION_STATE = "DataConnectionState";
    public final static int DATA_CONN_ON = 0;
    public final static int DATA_CONN_OFF = 1;
    public final static int DEF_DATA_CONN_STATE = DATA_CONN_ON;
    // End: Data Mode Settings

}

