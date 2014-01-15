/*
 * @(#)TimeFrameConstants.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -------------------
 * a15776       2011/02/21   NA               Review comments incorporated
 * a15776       2010/12/01   NA               Initial version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.smartprofile.Constants;
/**
 * The interface TimeFrameConstants defines various constants used in the time frames. All the
 * constants used in time frames should be defined here
 *
 * <code><pre>
 *
 * CLASS:
 *  Extends the Constants interface and defines additional constants
 *
 * RESPONSIBILITIES:
 *  Defines the frequently used constants in the time sensor
 *
 * </pre></code>
 *
 */
public interface TimeFrameConstants extends Constants {
    int MONDAY    = 0x01;
    int TUESDAY   = 0x02;
    int WEDNESDAY = 0x04;
    int THURSDAY  = 0x08;
    int FRIDAY    = 0x10;
    int SATURDAY  = 0x20;
    int SUNDAY    = 0x40;
    int NODAY     = 0x00;
    int ALLDAY    = 0x7f;

    String MORNING_TIMEFRAME = "Morning";
    String EVENING_TIMEFRAME = "Evening";
    String WEEKEND_TIMEFRAME = "Weekend";
    String NIGHT_TIMEFRAME = "Night";
    String WORK_TIMEFRAME = "Work";

    // Debugging related constants
    boolean SHOW_TOASTS   = true;
    // multiple days are separated by comma
    String SHORT_DAY_SEPARATOR = ", ";
    //not used
    String LONG_DAY_SEPERATOR = " and ";
    //hour minute separator hh:mm
    String TIME_SEPERATOR = ":";
    String DAY_START_TIME = "00:00";
    String DAY_END_TIME   = "23:59";
    String EMPTY_SUMMARY = "";
    String ALL_DAY_FLAG_TRUE  = "true";
    String ALL_DAY_FLAG_FALSE = "false";

    // constants to identify special day selections - all days & no days
    int TIME_FRAME_ON_ALL_DAYS   = 0x12345;
    int TIME_FRAME_DAY_NONE      = 0x54321;

    int NUM_OF_MILLIS_PER_DAY    = (24*60*60*1000);
    int NUM_OF_MILLIS_PER_WEEK   = (7 * NUM_OF_MILLIS_PER_DAY);

    // time sensor related constants
    String VSENSOR_CONSTANT        = "#vsensor;name=";
    String TIME_END_TRUE_STRING    = ";p0=true;end";
    String TIME_END_FALSE_STRING   = ";p0=false;end";
    String TRIGGERS                = " TRIGGERS ";

    // time frame intent related constants
    String TIME_FRAME_INTENT_PREFIX = "com.motorola.contextual.smartprofile.sensors.timesensor.";
    String INTENT_ACION_PREFIX    = "#Intent;action=";
    String INTENT_END_STRING      =  ";.*end ";
    String INTENT_WILD_CHAR       =  ";.*S.";
    String EXTRA_EQUALS           = "=";
    String EXTRA_FRAME_NAME       = "MODE_NAME";
    String EXTRA_FRIENDLY_NAME    = "FRIENDLY_NAME";
    String EXTRA_ACTIVE_FLAG      = "ACTIVE_FLAG";
    String ACTIVE_TRUE            = "TRUE";
    String ACTIVE_FALSE           = "FALSE";

    // Extras related to SETALARM Intent
    String EXTRA_SET_TIME         = "ALARM_SET_TIME";
    String EXTRA_TYPE             = "ALARM_TYPE";

    //Extras related to TF_MODIFY Intent
    String EXTRA_NAME             = "TF_NAME";
    String EXTRA_START            = "TF_START";
    String EXTRA_END              = "TF_END";
    String EXTRA_ALLDAYFLAG       = "TF_ALLDAYFLAG";
    String EXTRA_DAYS             = "TF_DAYS";


    String TIME_FRAME_INTENT_MIME = "com.motorola.contextual.smartprofile.sensors.timesensor/";
    String TIMEFRAME_INTENT_ACTION = TIME_FRAME_INTENT_PREFIX + "ALARM";
    String SETALARM_INTENT_ACTION = TIME_FRAME_INTENT_PREFIX + "SETALARM";
    String TF_MODIFY_INTENT_ACTION = TIME_FRAME_INTENT_PREFIX + "TF_MODIFY";
    String TIMEFRAME_IMPORT_ACTION = TIME_FRAME_INTENT_PREFIX + "IMPORT";
    String UPDATE_SMARTRULES_INTENT_ACTION = "com.motorola.contextual.RULES_IMPORTED";
    String TIMEFRAME_UPDATE_SMARTRULES_ACTION = TIME_FRAME_INTENT_PREFIX + "UPDATE_SMARTRULES";
    String EXTRA_INTERNAL_NAME    = "com.motorola.contextual.internalName";
    String UNREGISTER_ALL_TIME_FRAMES = "com.motorola.contextual.deleteAll";

    String PERIOD                  = ".";
    String START                   = ".start";
    String END                     = ".end";

    String SPACE                   = " ";
    String EMPTY_STRING            = "";

    // constants used in returning back information to puzzle/rule builder
    String CURRENT_SELECTION     = "CURRENT_SELECTION";
    String INT_CURRENT_SELECTION = "INT_CURRENT_SELECTION";
    String TIME_FRAME_URI_TO_FIRE_STRING =
        "#Intent;action=android.intent.action.MAIN;component=com.motorola.contextual.smartrules/com.motorola.contextual.smartprofile" +
        ".sensors.timesensor.TimeFramesCheckListActivity;";

    //String EXTRA_CURRENT_SELECTION_EXT = "S.CURRENT_SELECTION=";
    String EXTRA_CURRENT_SELECTION_INT = "S.INT_CURRENT_SELECTION=";

    String TIME_FRAME_EDIT_INTENT = "android.intent.action.TIME_MODE_EDIT";
    String TIME_ZERO_ROWS         = "No time frames configured yet";

    // operation type
    int DB_TASK_READ  = 0;
    int DB_TASK_WRITE = 1;

    int READ_SUCCESS  = 0;
    int WRITE_SUCCESS = 1;
    int FAILED        = -1;

    // constant to differentiate named time frames and un-named time frames
    String FLAG_VISIBLE   = "v";
    String FLAG_INVISIBLE = "i";

    // list of intents that time frames module is interested in
    String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    String TIME_ZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    String DATE_CHANGED      = "android.intent.action.DATE_CHANGED";

    // intent to show the list of time frames
    String TIMEFRAME_LIST_INTENT = "com.motorola.contextual.timeframeslist";
    // constant for the publisher key
    String TIMEFRAME_PUBLISHER_KEY = "com.motorola.contextual.smartprofile.timeframes";
    //extra to return for the debug provider to track the states
    String TIME_EVENT_TARGET_STATE = "com.motorola.rulescomposer.EventTargetState";

    String REG_EX_TO_IGNORE = "[!@#$%^&*;:\"\', ?/\\()~`|{}<>+=-]";

    //intent to launch Rules Exporter to backup the data to server
    String LAUNCH_RULES_EXPORTER = "com.motorola.contextual.launch.rulesexporter";

    String EXTRA_TIMEFRAME_DATA = TIME_FRAME_INTENT_PREFIX + "intent.extra.TIMEFRAME_DATA";

    boolean EXPORT_TIMEFRAMES = true;
    int TIMEFRAME_REGISTERED = 1;
    int TIMEFRAME_UNREGISTERED = 0;
    
    int TIMEFRAME_ACTIVE = 1;
    int TIMEFRAME_INACTIVE = 0;


    String TIMEFRAME_OLD_CONFIG_PREFIX = "S.INT_CURRENT_SELECTION=";
    // Phoenix :: Shared Preference to share data with Rules Exporter
    public static final String TIMEFRAME_XML_CONTENT = "com.motorola.contextual.timeframexml";
    public static final String TIMEFRAME_SHARED_PREFERENCE = "com.motorola.contextual.timeframepreference";

    public static final String TIMEFRAME_CONFIG_PERSISTENCE = "TimeFrameConfigs";
    public static final String TIMEFRAME_CONFIG_STRING = "TimeFrame=";
    public static final String TIMEFRAME_NAME = "TimeFrame";

    public static final String TIMEFRAME_CONFIG_VERSION = "1.0";


    public static final String OPEN_B = "(";
    public static final String CLOSE_B = ")";
}
