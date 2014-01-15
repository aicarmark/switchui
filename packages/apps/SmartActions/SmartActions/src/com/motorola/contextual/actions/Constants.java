/*
 * @(#)Constants.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 * a21383       2011/03/17  NA                  Changes for 'Play a song' picker
 * rdq478       2011/05/24  IKMAIN-17917        Added Wallpaer directory name
 * rdq478       2011/07/13  IKINTNETAPP-386     Removed wallpaper stateless action
 * rdq478       2011/08/16  IKSTABLE6-6826      Add intent action for wallpaper service
 * rdq478       2011/09/22  IKSTABLE6-10141     Add wallpaper related
 * rdq478       2011/09/28  IKMAIN-28588        Add Voice Announce related
 * rdq478       2011/10/18  IKSTABLE6-18450     Changed to use PNG compression
 * rdq478       2011/10/21  IKMAIN-31135        Remove wallpaper related constants
 * rdq478       2011/10/26  IKMAIN-31519        Changed to SMS_RECEIVED
 */

package com.motorola.contextual.actions;

/**
 * This is the package-wide Constants interface <code><pre>
 * INTERFACE:
 *   This interface represents package-wide constants, that are not visible to
 *   the user or otherwise dependent upon being changed if the language is translated
 *   from English to some other language.
 *
 *   This should not be used for constants within a class.
 *   All constants are static
 *
 * RESPONSIBILITIES:
 *   None - static constants only
 *
 * COLABORATORS:
 *       None.
 *
 * USAGE:
 *      All constants here should be static, almost or all should be final.
 *
 * </pre></code>
 **/

public interface Constants {

    public static final String PACKAGE = (Constants.class.getPackage() != null) ? Constants.class
            .getPackage().getName() : null;

    public static final String APP_PACKAGE = "com.motorola.contextual.smartrules";

    /* helpful for managing logging */
    public static final boolean PRODUCTION_MODE = true;
    public static final boolean LOG_INFO = true;
    public static final boolean LOG_DEBUG = !PRODUCTION_MODE;
    public static final String TAG_PREFIX = "QA";

    // Action key suffixes
    public static final String RINGER = "Ringer";
    public static final String WIFI = "Wifi";
    public static final String BLUETOOTH = "Bluetooth";
    public static final String GPS = "GPS";
    public static final String AIRPLANE = "AirPlane";
    public static final String SCR_TIMEOUT = "ScreenTimeout";

    // User mode
    public static final String USER = "User";

    // Storage related
    public static final String SEPARATOR = "::";
    public static final String WP_DIR = "Wallpapers";

    // Wallpaper related
    public static final String WP_DEFAULT = "DEFAULT_WP.png";
    public static final String EXTRA_WP_DIR_NAME = "WP_DIR_NAME";
    public static final String WP_CURRENT = "CURRENT_WP.png";
    public static final String EXTRA_WP_SAVE_CURRENT = "WP_SAVE_CURRENT";
    public static final String EXTRA_WP_PERFORM_CLEANUP = "WP_PERFORM_CLEANUP";

    // Extras related to voice announce actions
    public static final String EXTRA_VA_READ_TEXT = "Voice_Announce_Read_Text";
    public static final String EXTRA_VA_READ_CALL = "Voice_Announce_Read_Caller";
    public static final String EXTRA_VA_CALL_ACTIVE = "Voice_Announce_Call_Active";
    public static final String EXTRA_VA_CALL_IDLE = "Voice_Announce_Call_Idle";

    // common extras
    public static final String EXTRA_DESCRIPTION = "com.motorola.smartactions.intent.extra.DESCRIPTION";
    public static final String EXTRA_STATE_DESCRIPTION = "com.motorola.smartactions.intent.extra.STATE_DESCRIPTION";
    public static final String EXTRA_RULE_ENDS = "com.motorola.smartactions.intent.extra.WHEN_RULE_ENDS";
    public static final String OLD_EXTRA_RULE_ENDS = "com.motorola.intent.extra.WHEN_RULE_ENDS";
    public static final String EXTRA_RULE_KEY = "com.motorola.intent.extra.RULE_KEY";

    // Extras related to binary actions ( On/Off )
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_DELAY = "delay";

    // Extras related to Ringer
    public static final String EXTRA_VIBE_STATUS = "VIB_CHECK_STATUS";
    public static final String EXTRA_RINGER_MODE = "RINGER_MODE";
    public static final String EXTRA_MODE_STRING = "MODE_STRING";
    public static final String EXTRA_RINGER_VOLUME = "RINGER_VOLUME";
    public static final String EXTRA_MAX_RINGER_VOLUME = "MAX_RINGER_VOLUME";
    public static final String EXTRA_VIBE_SETTING = "VIB_SETTING";
    public static final String EXTRA_VIB_IN_SILENT = "VIB_IN_SILENT_MODE";
    public static final String RINGER_MODE_PREVIOUS_SET_STATE = "ringer_mode_current_state";
    public static final String RINGER_VIBRATE_PREVIOUS_SET_STATE = "ringer_vibrate_current_state";
    public static final String RINGER_VOLUME_PREVIOUS_SET_STATE = "ringer_volume_current_state";
    public static final String VIP_RINGER_ACTIVE = "vip_active";

    // Constants related to Volumes action publisher
    public static final String EXTRA_VOL_MEDIA_VOLUME = "MEDIA_VOLUME_PERCENT";
    public static final String EXTRA_VOL_RINGER_VOLUME = "RINGER_VOLUME_PERCENT";
    public static final String EXTRA_VOL_NOTIFICATION_VOLUME = "NOTIFICATION_VOLUME_PERCENT";
    public static final String EXTRA_VOL_ALARM_VOLUME = "ALARM_VOLUME_PERCENT";
    public static final String EXTRA_VOL_RINGER_MODE = EXTRA_RINGER_MODE;
    public static final String EXTRA_VOL_VIBRATE_SETTING = "VIBRATE_SETTING";
    public static final double VOLUMES_ACTION_VERSION = 1.1;
    public static final int VOL_INVALID_VALUE = -1;
    public static final String KEY_IGNORE_VOLUME_CHANGE = "ignore_volume_change";

    // Extras related to Launch app
    public static final String EXTRA_COMPONENT = "comp";
    public static final String EXTRA_APP_NAME = "app_name";
    public static final String EXTRA_APP_URI = "app_uri";

    // Extras related to messaging
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NUMBER = "number";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_STATUS_INTENT = "status_intent";

    // Extras related to Notification
    public static final String EXTRA_VIBRATE = "vibrate";
    public static final String EXTRA_SOUND = "sound";

    // Extras related to Screen Timeout
    public static final String EXTRA_TIMEOUT = "timeout";

    // Extras related to Ringtone / Wallpaper
    public static final String EXTRA_URI = "Uri";
    public static final String EXTRA_TITLE = "title";

    // Website related
    public static final String EXTRA_URL = "url";

    // Settings which can be just enabled/disabled
    public static final String ENABLE = "Enable";
    public static final String DISABLE = "Disable";
    public static final String FAILED = "Failed";

    public static final String EXTRA_DEBUG_REQRESP = PACKAGE + ".debugReqResp";
    public static final boolean MONITOR_SETTINGS_ALWAYS = false;

    // Mode Related
    public static final String RULEKEY = "com.motorola.contextual.smartrules.rulekey";
    public static final String EXTRA_OLD_SAVE_DEFAULT = "com.motorola.intent.action.SAVE_DEFAULT";
    public static final String EXTRA_ACTION_KEY = "com.motorola.intent.extra.ACTION_KEY";
    public static final String EXTRA_SETTING_DISPLAY_STRING = "com.motorola.intent.extra.SETTING_STRING";
    public static final String EXTRA_DEFAULT_URI = "com.motorola.smartactions.intent.extra.DEFAULT_URI";
    public static final String EXTRA_OLD_DEFAULT_URI = "com.motorola.contextual.smartrules.RESTORE_DEFAULT_URI";
    public static final String EXTRA_DEFAULT_SETTING_STRING = "com.motorola.intent.action.DEFAULT_SETTING_STRING";
    public static final String ACTION_TYPE = "com.motorola.smartactions.action_type";
    public static final String PUBLISHER_KEY = "com.motorola.smartactions.publisher_key";
    public static final String ACTION_KEY = "com.motorola.smartactions.action_key";
    public static final String EXTRA_SONG_ID = "com.motorola.intent.extra.songid";
    public static final String EXTRA_PLAYLIST_ID = "com.motorola.intent.extra.playlistid";
    public static final String EXTRA_PLAYER_COMPONENT = "com.motorola.intent.extra.playercomponent";
    public static final String EXTRA_SMART_RULE_INITIATED = "com.motorola.intent.extra.SMART_RULE_INITIATED";
    public static final String STATEFUL = "stateful";

    // Mode related - due to new AP
    public static final String COMMAND_FIRE = "fire_request";
    public static final String COMMAND_REVERT = "revert_request";
    public static final String COMMAND_REFRESH = "refresh_request";
    public static final String COMMAND_LIST = "list_request";
    public static final String EXTRA_REQUEST_ID = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String EXTRA_CONFIG = "com.motorola.smartactions.intent.extra.CONFIG";
    public static final String EXTRA_PUBLISHER_KEY = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY";
    public static final String EXTRA_RESPONSE_ID = "com.motorola.smartactions.intent.extra.RESPONSE_ID";
    public static final String EXTRA_CONFIG_ITEMS = "com.motorola.smartactions.intent.extra.CONFIG_ITEMS";
    public static final String EXTRA_NEW_STATE_TITLE = "com.motorola.smartactions.intent.extra.NEW_STATE_TITLE";
    public static final String EXTRA_FAILURE_DESCRIPTION = "com.motorola.smartactions.intent.extra.FAILURE_DESCRIPTION";
    public static final String EXTRA_CONFIG_VERSION = "com.motorola.smartactions.intent.extra.CONFIG_VERSION";
    public static final String TAG_CONFIG_ITEMS = "CONFIG_ITEMS";
    public static final String TAG_ITEM = "ITEM";
    public static final String TAG_CONFIG = "CONFIG";
    public static final String TAG_DESCRIPTION = "DESCRIPTION";
    public static final String ACTION_PUBLISHER_EVENT = "com.motorola.smartactions.intent.action.ACTION_PUBLISHER_EVENT";
    public static final String EXTRA_EVENT_TYPE = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_FIRE_RESPONSE = "fire_response";
    public static final String EXTRA_REVERT_RESPONSE = "revert_response";
    public static final String EXTRA_REFRESH_RESPONSE = "refresh_response";
    public static final String EXTRA_LIST_RESPONSE = "list_response";
    public static final String EXTRA_STATE_CHANGE = "state_change";
    public static final double INITIAL_VERSION = 1.0;

    // Action keys for stateless actions
    public static final String ACTION_KEY_PREFIX = PACKAGE + ".";
    public static final String LAUNCH_APP_ACTION_KEY = ACTION_KEY_PREFIX
            + "launchapp";
    public static final String SMS_ACTION_KEY = ACTION_KEY_PREFIX
            + "sendmessage";
    public static final String NOTIFICATION_ACTION_KEY = ACTION_KEY_PREFIX
            + "notification";
    public static final String WEBSITE_ACTION_KEY = ACTION_KEY_PREFIX
            + "launchwebsite";
    public static final String PLAYLIST_ACTION_KEY = ACTION_KEY_PREFIX
            + "playlist";
    public static final String AUTO_SMS_ACTION_KEY = ACTION_KEY_PREFIX
            + "autosms";
    public static final String BLUETOOTH_ACTION_KEY = ACTION_KEY_PREFIX
            + "Bluetooth";

    // Intent actions
    public static final String BOOT_COMPLETE_INTENT = "com.motorola.intent.action.BOOT_COMPLETE_RECEIVED";
    public static final String SETTING_WP_ACTION = "com.motorola.intent.action.WP_SAVE_COMPLETED";
    public static final String SETTING_CHANGE_ACTION = "com.motorola.intent.action.QUICK_SETTING_CHANGE";
    public static final String STATELESS_ACTION = "com.motorola.intent.action.QUICK_ONESHOT";
    public static final String ACTION_SMS_SENT = "QUICK_ACTION_SMS_SENT";
    public static final String MISSED_CALL_INTENT_ACTION = "com.motorola.dummy.MissedCall";
    public static final String POWER_SETTING_INTENT = "com.motorola.contextual.powersetting";

    // Action execution status
    public static final String ACTION_EXEC_STATUS = "com.motorola.intent.action.EXEC_STATUS";
    public static final String EXTRA_STATUS = "com.motorola.smartactions.intent.extra.STATUS";
    public static final String EXTRA_OLD_STATUS = "com.motorola.intent.extra.ACTION_STATUS";
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String EXTRA_REQ_TYPE = "com.motorola.contextual.smartrules.REQUEST_TYPE";
    public static final String EXTRA_EXCEPTION_STRING = "com.motorola.intent.extra.EXCEPTION_STRING";

    // Settings change
    public static final String ACTION_SETTING_CHANGE = "com.motorola.intent.action.SETTING_CHANGE";
    public static final String EXTRA_REGISTER = "com.motorola.intent.exta.REGISTER";
    public static final String EXTRA_DEREGISTER = "com.motorola.intent.extra.DEREGISTER";

    // Save / Restore Settings
    public static final String EXTRA_SAVE_DEFAULT = "com.motorola.smartactions.intent.extra.SAVE_DEFAULT";
    public static final String DEFAULT_SUFFIX = "-default";
    public static final String DEFAULT_FW_SUFFIX = "-defaultfw";
    public static final String EXTRA_RESTORE_DEFAULT = "com.motorola.intent.action.RESTORE_DEFAULT";
    public static final String MONITOR_SUFFIX = "-monitor";

    // Debug Provider related
    public static final String QA_TO_MM = "QA to MM";
    public static final String MM_TO_QA = "MM to QA";
    public static final String QA_TO_FW = "QA to FW";
    public static final String COLON = ":";
    // Action related debug info
    public static final String NORMAL = "Normal";
    public static final String VIBRATE = "Vibrate";
    public static final String SILENT = "Silent";

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    // Binary states
    public static final String STATE_TRUE = "true";
    public static final String STATE_FALSE = "false";

    // Extras related to brightness
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_BRIGHTNESS = "brightness";

    // Brightness
    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck
    public static final int MAXIMUM_BACKLIGHT = 255;
    public static final int MINIMUM_BACKLIGHT = 20;
    public static final int BACKLIGHT_RANGE = MAXIMUM_BACKLIGHT
            - MINIMUM_BACKLIGHT;
    public static final int AUTOMATIC_NOT_SUPPORTED = -1;
    public static final String DISPLAY_CURVE_SUPPORT_KEY = "display_curve_supported";

    // Cpu Power Saver
    public static final String CPU_POWERSAVER_SUPPORT_KEY = "cpu_powersaver_supported";

    //Ringtone constants
    public static final String RINGTONE_ACTION_KEY = ACTION_KEY_PREFIX + Ringtone.class.getSimpleName();
    public static final String RINGTONE_SILENT = "ringtone_silent";
    public static final String RINGTONE_STATE_KEY = "ringtone_current_state";

    // Auto SMS related
    public static final String AUTO_SMS_TABLE = "auto_sms";
    public static final String EXTRA_INTENT_ACTION = "intent_action";
    public static final String EXTRA_REGISTER_RECEIVER = "reg_receiver_flag";
    public static final String EXTRA_INTERNAL_NAME = "internal_name";
    public static final String EXTRA_NUMBERS = "numbers";
    public static final String EXTRA_RESPOND_TO = "respond_to_flag";
    public static final String EXTRA_SMS_TEXT = "sms_text";
    public static final String ALL_CONTACTS = "*all*";
    public static final String KNOWN_CONTACTS = "*known*";
    public static final int RESPOND_TO_CALLS_AND_TEXTS = 0;
    public static final int RESPOND_TO_CALLS = 1;
    public static final int RESPOND_TO_TEXTS = 2;

    // VIP Caller related
    public static final String VIP_CALLER_TABLE = "vip_caller";
    public static final String EQUALS = " = ";
    public static final String EXTRA_PHONE_RINGING = "com.motorola.RINGING";
    public static final String EXTRA_PHONE_IDLE = "com.motorola.IDLE";
    public static final String ACTION_CALLS_ADD_ENTRY = "com.android.phone.intent.action.CALLS_ADD_ENTRY";
    public static final String VIP_RINGER_NO_REVERT_FLAG = "vip_no_revert";
    public static final String ACTION_EXT_RINGER_CHANGE = "ext_ringer_change";
    public static final String EXTRA_KNOWN_FLAG = "KNOWN_FLAG";
    public static final String NUMBER_TO_END = "vip_number_to_end";
    public static final String EXTRA_VOLUME_LEVEL = "VOLUME_LEVEL";

    public static String SPACE = " ";
    public static final String QUOTE = "'";
    public static final String BROADCAST_ACTION_DELIMITER = ",";

    public static final String BLUETOOTH_STATE_AFTER_CALL = "bt_state_after_call";

    // General constants
    public static final String NEW_LINE = "\n";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String PLUS = "+";
    public static final String PERCENT = "%";
    public static final String DASH = "-";

    public static final int TYPE_ACTION = 0;
    public static final int TYPE_STATEFUL_ACTION = 1;
    public static final int TYPE_STATELESS_ACTION = 2;

    public static final String GOOGLE_MUSIC_PKG = "com.google.android.music";

    public static final String PERM_ACTION_PUBLISHER       = "com.motorola.smartactions.permission.ACTION_PUBLISHER";
    public static final String PERM_ACTION_PUBLISHER_ADMIN = "com.motorola.smartactions.permission.ACTION_PUBLISHER_ADMIN";

}
