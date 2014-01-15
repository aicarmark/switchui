/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19       Initial version
 * w04917        2012/02/17 IKHSS7-8262       Use the new Checkin API.
 * w04917        2012/07/16 IKCTXTAW-492      Add INTENT_POI_EVENT and move TRANSIENT
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import android.net.Uri;
//import android.os.SystemProperties;
import android.text.format.DateUtils;

/**
 *<code><pre>
 * CLASS:
 *  All the constant referred in this package
 *
 * RESPONSIBILITIES:
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public final class Constants {

    private Constants() {}  // prevent instantiation

    public static final String TAG_PREFIX = "LSAPP_";
    public static final String PACKAGE_NAME = Constants.class.getPackage().getName();
    public static final String MOVEMENTSENSOR_PACKAGE_NAME = "com.motorola.movementsensor";

    public static final boolean PRODUCTION_MODE = com.motorola.contextual.smartrules.Constants.PRODUCTION_MODE;
    public static final boolean LOG_INFO = true;
    public static final boolean LOG_DEBUG = !PRODUCTION_MODE;
    //public static final boolean LOG_VERBOSE = "1".equals(SystemProperties.get("debug.mot.lslog","0"));
    public static final boolean LOG_VERBOSE = !PRODUCTION_MODE;

    public static final String DATABASE_NAME = "locationsensor.db";
    public static final long   DATABASE_SIZE = (5*1024*1024);   // avg 10 record per day, 5000 records ~ 800k, set to 5M should be sufficient enough.

    // common messages, use 1010 prefix
    public static final int LOCATION_UPDATE			= 1010001;
    public static final int BEACONSCAN_RESULT		= 1010002;
    public static final int WIFI_CONNECTED			= 1010003;
    public static final int WIFI_STATE_CHANGED		= 1010004;

    public static final String TRANSIENT = "Transient";

    public static final String CONTEXT_NULL 		= "Context cannot be null";
    public static final String UNKNOWN_MESSAGE 		= " unknown message:";
    public static final String TYPE_OUT_OF_RANGE 	= "type is out of range:";
    public static final String INDEX_OUT_OF_RANGE 	= "Index out of range:";
    public static final String CANNOT_BE_NULL 		= "cannot be null";

    /* set to one to disable no detection inside poi */
    public static final int DET_BOUNCING_CELL_SIZE = 1;
    public static final String INVALID_CELL = "-1";

    public static final int FUZZY_MATCH_MIN = 2;   // the min match to fuzzy match be positive.
    public static final int WIFI_MIN_SS = -120;


    public static final long TIMER_HEARTBEAT_INTERVAL = 15 * DateUtils.MINUTE_IN_MILLIS; // heart beat 15min
    public static final long TIMER_ALIVE_INTERVAL = 2*DateUtils.HOUR_IN_MILLIS;  // 2 hours

    public static final int MONITOR_RADIUS = 1500; // meters
    public static final int TARGET_RADIUS = 50;
    public static final int LOCATION_DETECTING_DIST_RADIUS = 200;

    public static final int LOCATION_DETECTING_UPDATE_MAX_DIST_METERS = 2*TARGET_RADIUS;
    public static final long LOCATION_DETECTING_UPDATE_INTERVAL_MILLIS = 5 * DateUtils.MINUTE_IN_MILLIS;

    public static final String INTENT_PARM_STARTED_FROM_BOOT = PACKAGE_NAME+".StartedFromBoot";
    public static final String INTENT_PARM_STARTED_FROM_VSM = PACKAGE_NAME+".StartedFromVSM";
    public static final String INTENT_PARM_STARTED_FROM_ADA = PACKAGE_NAME+".StartedFromADA";
    public static final String INTENT_PARM_STARTED_FROM_CONSENT = PACKAGE_NAME+".StartedFromConsent";
    public static final String ALARM_TIMER_LOCATION_EXPIRED =  PACKAGE_NAME + ".loctimer";
    public static final String ALARM_TIMER_METRIC_EXPIRED = PACKAGE_NAME + ".metrictimer";
    public static final String ALARM_TIMER_SELF_HEALING_EXPIRED = PACKAGE_NAME + ".healingtimer";
    public static final String ALARM_TIMER_SET_TIME = PACKAGE_NAME + ".AlarmScheduledTime";
    public static final String ALARM_TIMER_ALIVE_EXPIRED = PACKAGE_NAME + ".alivetimer";
    public static final String LOCATION_DETECTION_POI =  PACKAGE_NAME + ".poidetected";
    public static final String LOCATION_UPDATE_AVAILABLE =  PACKAGE_NAME + ".locationupdate";

    // poi change broadcast outside SmartRules for components independent of ConditionPublisher
    public static final String INTENT_POI_EVENT = PACKAGE_NAME + ".POI_EVENT";
    public static final String INTENT_EXTRA_POI = "poi";

    public static final int HEAL_HOUR = 4;   // heal at 4am in the morning
    public static final int DAY_TIME_START = 6;    // 6am
    public static final int DAY_TIME_END = 22; // 10pm

    public static final String LOCATION_CONSENT_KEY = "com.motorola.contextual.locationconsent"; // user accept moto location consent
    public static final String ADA_ACCEPTED_KEY = "com.motorola.analytics.ada_accepted"; // ada = Advanced Data Analytics
    public static final String CHECKIN_TAG = "MOT_CA_LOC";
    public static final int CHECKIN_BLOCK_SIZE = 1024;  /*dsp.getMaxLogSize()*/
    //public static final String CHECKIN_ID_HEAD = "[ID=Location;pkg=com.motorola.contextual.virtualsensor.locationsensor;ver=0.9;time=";

    public static final String CHECKIN_EVENT_DATA = "DATA";
    public static final String CHECKIN_EVENT_DATA_VERSION = "1.1";
    public static final String CHECKIN_FIELD_ENC = "ENC";

    public static final String PERMISSION = "com.motorola.virtualsensor.MEANINGFUL_LOCATION";

    public static final String BOOT_COMPLETE = "com.motorola.intent.action.BOOT_COMPLETE_RECEIVED";
    public static final String VSM_LOCATION_CHANGE = "com.motorola.intent.action.LOCATION_CHANGE";
    public static final String VSM_PROXY = "com.motorola.situation.VSENSOR";
    public static final String VSM_INIT_COMPLETE = "com.motorola.contextual.virtualsensor.manager.INIT_COMPLETE";
    public static final String VSM_CREATE_LOCATION_SENSOR = "com.motorola.intent.action.VS_ACTION_CREATE_LS";
    public static final String VSM_UPDATE_LOCATION_SENSOR = "com.motorola.intent.action.VS_ACTION_UPDATE_LS";
    public static final String VSM_LS_PARAM_SETVALUE = "name=com.motorola.virtualsensor.Meaningfullocation;";
    public static final String VSM_LS_PARAM_PLACES = "p0=unknown;rule0=TestRule;";
    public static final String VSM_LS_PARAM_TRIGGER = ";triggers ARRIVEDHOME";
    public static final String VSM_LS_PARAM_END = "end";

    public static final String LOCATION_RULE = "com.motorola.contextual.smartprofile.location";
    public static final String HIDDEN_LOCATION_RULE = "com.motorola.contextual.smartprofile.hidden_location";

    // for beacon sensor json string
    public static final String LS_JSON_TYPE = "type";  // wifi, bt, ...
    public static final String LS_JSON_TIME = "time";
    public static final String LS_JSON_ARRAY = "valueset";
    public static final String LS_JSON_NAME = "name";
    public static final String LS_JSON_ID =   "id";
    public static final String LS_JSON_VALUE = "value";
    public static final String LS_JSON_WIFISSID = "wifissid";
    public static final String LS_JSON_WIFIBSSID = "wifibssid";
    public static final String LS_JSON_WIFISS = "ss";

    // debug provider
    public static final Uri CHECKIN_DATA_URI = Uri.parse("content://com.motorola.contextual.analytics/checkin");
    public static final String DEBUG_CHECKIN_TAG = "CHECKIN_TAG";
    public static final String DEBUG_CHECKIN_DATA = "CHECKIN_DATA";
    public static final Uri DEBUG_DATA_URI = Uri.parse("content://com.motorola.contextual.analytics/debug");
    public static final String DEBUG_COMPKEY = "compkey";
    public static final String DEBUG_COMPINSTKEY = "compinstkey";
    public static final String DEBUG_DIRECTION = "direction";
    public static final String DEBUG_DATA = "data";
    public static final String DEBUG_STATE = "state";
    public static final String VSM_PKGNAME = "com.motorola.contextual.virtualsensor.manager";
    public static final String DEBUG_OUT = "out";
    public static final String DEBUG_IN = "in";
    public static final String DEBUG_INTERNAL = "internal";

    // for PNO intent
    public static final int PNO_SLOTS = 4;   // only
    public static final String PNO_UPDATE = "android.net.wifi.WIFI_PNO_APP_LIST_UPDATE";
    public static final String PNO_LIST = "android.net.wifi.WIFI_PNO_APP_LIST_CONFIRM";
    public static final String PNO_SSID = "ssid";

    // for background scan when wifi is off
    public static final String AUTONOMOUS_SCAN_PERMISSION = "com.motorola.permission.ACCESS_WIFI_AUTONOMOUS";
    public static final String AUTONOMOUS_SCAN_RESULTS = "android.net.wifi.AUTONOMOUS_MODE_SCAN_RESULTS";
    public static final String LS_BACKGROUND_SCAN_CHECK = PACKAGE_NAME+".bgscan_check";
    public static final String LS_BACKGROUND_SCAN_EXIST = PACKAGE_NAME+".bgscan_exist";
    public static final String LS_BACKGROUND_SCAN_START = PACKAGE_NAME+".bgscan_start";
}
