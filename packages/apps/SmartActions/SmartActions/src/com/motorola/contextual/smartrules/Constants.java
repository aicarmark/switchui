/*
 * @(#)Constants.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/11/01 NA				  Initial version
 * A18172        2011/03/08 NA                Added IS_AUTOMATION,
 *                                    		  RULESIMPORTER_XML_CONTENT and
 *                                            changed LAUNCH_CANNED_RULES
 *
 */

package com.motorola.contextual.smartrules;

/** This is the system-wide constants class
 *
 *<code><pre>
 * CLASS:
 *   This class simply represents system-wide constants, that are not visible to
 *   the user or otherwise dependent upon being changed if the language is translated
 *   from English to some other language.
 *
 *   This should not be used for constants within a class.
 *   Keep all constants as static
 *
 * RESPONSIBILITIES:
 *   None - static constants only
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	All constants here should be static, almost or all should be final.
 *
 * </pre></code>
 **/
public interface Constants {

    public static final boolean PRODUCTION_MODE = true;
    public static final boolean LOG_INFO = true; // should stay true even in production mode
    public static final boolean LOG_DEBUG = ! PRODUCTION_MODE;
    public static final boolean LOG_WARN = ! PRODUCTION_MODE;
    public static final boolean LOG_VERBOSE = false;

    public static final String COMMA_SPACE = ", ";
    public static final String HYPHEN = " - ";
    public static final String EMPTY_STRING = "";
    public static final String SPACE = " ";
    public static final String SEMICOLON = "; ";
    public static final String KEY_TAG_S = "<IDENTIFIER>";
    public static final String KEY_TAG_E = "</IDENTIFIER>";

    public static final String 	PACKAGE = Constants.class.getPackage().getName();

    // Phoenix :: Adding permissions to share data
    public static final String SMART_RULES_PERMISSION = "com.motorola.contextual.permission.SMARTRULES";

    public static final int MAX_VISIBLE_ENABLED_AUTOMATIC_RULES = 30;

    public static final float OPAQUE_APLHA_VALUE = 1.0f;
    public static final float FIFTY_PERECENT_ALPHA_VALUE = 0.5f;

    public static final int EIGHTY_PERCENT_ALPHA_VALUE = 204;
    public static final int TWENTY_FIVE_PERCENT_ALPHA_VALUE = 64;
    
    public static final String IS_CLONE_FOR_COPY_RULE = PACKAGE + ".IS_CLONE_FOR_COPY_RULE";
    public static final String RULE_OBJECT = PACKAGE + ".RULE_OBJECT";

    public static final String INTENT_ACTION = PACKAGE + ".INTENT_ACTION";

    public static final String CLONED_CHILD_RULE_ID = PACKAGE + ".CLONED_CHILD_RULE_ID";
    public static final String CLONED_CHILD_RULE_KEY = PACKAGE + ".CLONED_CHILD_RULE_KEY";

    // Default Rulekey in the Smart Rules DB.
    public static final String DEFAULT_RULE_KEY = "com.motorola.contextual.default";
    public static final int DEFAULT_RULE_ID = -1;
    public static final long DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE = 0;

    // Rulekey for In meeting/change ringer suggestion
    public static final String RULE_KEY_IN_MEETING_CHANGE_RINGER = "com.motorola.contextual.Meeting.1300451788675";

    // Publisher keys
    public static final String CALENDAR_SENSOR_PUBLISHER_KEY     = "com.motorola.contextual.smartprofile.calendareventsensor";
    public static final String TIMEFRAME_PUBLISHER_KEY           = "com.motorola.contextual.smartprofile.timeframes";
    public static final String RINGER_ACTION_PUBLISHER_KEY       = "com.motorola.contextual.actions.Ringer";

    // HomeScreen Widget
	public static final String WIDGET_UPDATE_INTENT = PACKAGE + ".WIDGET_UPDATE";
	public static final String WIDGET_UPDATE_RESPONSE = PACKAGE + ".WIDGET_UPDATE_RESPONSE";
	public static final String RULE_MODIFIED_MESSAGE = PACKAGE + ".RULE_MODIFIED_MESSAGE";
	public static final String RULE_DELETED_MESSAGE = PACKAGE + ".RULE_DELETED_MESSAGE";
	public static final String SMARTRULES_INIT_EVENT = PACKAGE + ".INIT_COMPLETE";
	public static final String RULE_PROCESSED_RESPONSE = PACKAGE + ".RULE_PROCESSED_RESPONSE";
	public static final String RULE_ADDED_ACTION = PACKAGE + ".RULE_ADDED_ACTION";
	public static final String RULE_ATTACHED_ACTION = PACKAGE + ".RULE_ATTACHED_ACTION";
	public static final String AUTO_RULE_ENA_DISABLE = PACKAGE + ".AUTO_RULE_ENA_DISABLE";
	public static final String DATA_CLEAR = PACKAGE + ".DATA_CLEAR";

	//Jarfile related intents
	public static final String ONLOAD_INTENT = "com.motorola.contextual.fw.CLASS_LOADED";
	public static final String EXTRA_ACTIONNAME = "actionname";
	public static final String EXTRA_ACTION_ONLOAD = "onload";
	public static final String EXTRA_ACTION_LOADFILE = "loadfile";
	public static final String EXTRA_CLASSNAME = "classname";

    // Following strings are introduced to maintain backward compatibility with old Success
    // failure strings
    public static final String QA_SUCCESS = "Success";
    public static final String QA_FAILURE = "Failure";

    // Quick Actions related strings
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String IN_PROGRESS = "in_progress";
    public static final String MODE_NAME = PACKAGE + ".modename";
    public static final String FROM_TO = PACKAGE + ".fromto";
    public static final String STATEFUL = "stateful";
    public static final String STATELESS = "stateless";
    public static final String FIRE_URI = "com.motorola.intent.extra.FIRE_URI";

    public static final int REQ_SAVE_DEFAULT = 0;
    public static final int REQ_RESTORE_DEFAULT = REQ_SAVE_DEFAULT + 1;
    public static final int REQ_NORMAL_VALUE_SET = REQ_RESTORE_DEFAULT + 1;

    public static final String EXEC_STATUS = "com.motorola.intent.action.EXEC_STATUS";
    public static final String SETTING_CHANGE = "com.motorola.intent.action.SETTING_CHANGE";
    public static final String RULE_STATE_CHANGED = "com.motorola.intent.action.RULE_STATE_CHANGED";
    public static final String QA_EXEC_STATUS_PROCESSED = "com.motorola.intent.action.EXEC_STATUS_PROCESSED";

    // Shared Preference Values
    public static final String RULE_STATUS_NOTIFICATIONS_PREF = PACKAGE + ".RULE_STATUS_NOTIFICATIONS_PREF";
    public static final String NOTIFY_SUGGESTIONS_PREF = PACKAGE + ".NOTIFY_SUGGESTIONS_PREF";
    public static final String POPUP_SUGGESTIONS_PREF = PACKAGE + ".PROMPT_IMP_SUGGESTIONS_PREF";
    public static final String SUGGESTIONS_LAUNCH_PREF = PACKAGE + ".SUGGESTIONS_LAUNCH_PREF";
    public static final String SUGGESTIONS_FIRST_NO_PREF = PACKAGE + ".SUGGESTIONS_FIRST_NO_PREF";
    public static final String SUGGESTIONS_NOTIFICATION_PREF = PACKAGE + ".SUGGESTIONS_NOTIFICATION_PREF";
    public static final String WIFI_LOCATION_WARNING_PREF = PACKAGE + ".WIFI_LOCATION_WARNING_PREF";

    public static final String LOC_SENSOR_SHARED_PREF_FILE_NAME = "com.motorola.contextual.virtualsensor.locationsensor";

    public static final String RULE_STATE_CHANGE = "com.motorola.contextual.smartrules.rulestate";
    public static final String MM_CHANGED_RULE = "com.motorola.contextual.smartrules.changedrule";
    public static final String MM_RULE_KEY = PACKAGE + ".rulekey";
    public static final String MM_RULE_STATUS = PACKAGE + ".rulestatus";
    public static final String MM_ENABLE_RULE = PACKAGE + ".enablerule";
    public static final String MM_DISABLE_RULE = PACKAGE + ".disablerule";
    public static final String MM_DISABLE_ALL = PACKAGE + ".disableall";
    public static final String MM_DELETE_RULE = PACKAGE + ".deleterule";
    public static final String VSM_EVENT_ARRAY = "com.motorola.virtualsensor.event";
    public static final String VSM_SENSOR_NAME = "com.motorola.virtualsensor.sensor";
    public static final String IS_FROM_VSM = PACKAGE + ".isfromvsm";

    // INIT, power up, upgrade related shared preferences
    public static final String IS_DURING_POWERUP                  = PACKAGE + ".cb.isduringpowerup";
    public static final String RI_INIT_COMPLETE_SHARED_PREFERENCE = PACKAGE + ".ri.initcomplete";
    public static final String RI_INIT_COMPLETE_STRING            = PACKAGE + ".ri.initcomplete.string";
    public static final String INIT_RI_IMPORTER_INIT_COMPLETE_SHARED_PREFERENCE = PACKAGE + ".initriimporter.initcomplete";
    public static final String INIT_RI_IMPORTER_INIT_COMPLETE_STRING            = PACKAGE + ".initriimporter.initcomplete.string";

    // Export or Import rules
    public static final  boolean EXPORT_RULES                     = true;
    public static final  boolean IMPORT_RULES                     = false;

    public static final String NEW_IMPORT_TYPE                        =  "com.motorola.smartactions.importtype";
    public static final String NEW_XML_CONTENT                        =  "com.motorola.smartactions.xmlcontent";

    // Do not change from com.motorola.contextual.smartrules to com.motorola.smartactions
    public static final String IMPORT_TYPE                        =  "com.motorola.contextual.smartrules.importtype";
    public static final String XML_CONTENT                        =  "com.motorola.contextual.smartrules.xmlcontent";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final String	DATABASE_NAME = "smartrules.db";

    /** authority for this provider */
    public static final String  AUTHORITY = PACKAGE;

    public static final String BLANK_SPC = " ";
    public static final String NL = "\n";
    public static final String COLON = ":";
    public static final String DOT = ".";
    public static final String NULL_STRING = "null";

    public static final int CANCEL_RESP = 0;
    public static final int OK_RESP = CANCEL_RESP + 1;

    // Notification ID's used in Notification Manager
    public static final int NOTIF_ID = 1;

    public static final int RULE_EDIT = 1;
    public static final int RULE_CREATE = RULE_EDIT + 1;
    public static final int RULE_PRESET = RULE_CREATE + 1;
    public static final int RULE_SUGGESTED = RULE_PRESET + 1;

    //Suggestion related
    public static final String RP_RESPONSE_ACCEPT = "accepted";
    public static final String RP_RESPONSE_CUSTOMIZE = "customize";
    public static final String RP_RESPONSE_REJECT = "rejected";

    // Rules Composer related strings
    public static final String RULE_KEY = "com.motorola.rulescomposer.RuleKey";
    public static final String RULE_NAME = "com.motorola.rulescomposer.RuleName";
    public static final String RULE_DESC = "com.motorola.rulescomposer.RuleDescription";
    public static final String DISALLOW_EDIT_RULE_NAME = "com.motorola.rulescomposer.NoEditRuleName";
    public static final String DISALLOW_EDIT_RULE_DESC = "com.motorola.rulescomposer.NoEditRuleDesc";
    public static final String DEFAULT_RULE_ICON = "ic_default_w";
    public static final String RULE_NAME_EXTRA = "RuleNameTitle";

    public static final String CONDITION_PUB_SEL = "com.motorola.contextual.smartrules.rulesbuilder.ConditionPubSelected";
    public static final String ACTION_PUB_SEL = "com.motorola.contextual.smartrules.rulesbuilder.ActionPubSelected";
    public static final int    DEFAULT_BLOCK_POSITION = -1;
    public static final String LAUNCH_APP_PUBLISHER_KEY = "com.motorola.contextual.actions.launchapp";

    // Puzzle Builder Strings
    public static final String PUZZLE_BUILDER_RULE_ID = PACKAGE + ".RuleId";
    public static final String PUZZLE_BUILDER_RULE_INSTANCE = PACKAGE + ".RuleInst";
    public static final String PUZZLE_BUILDER_RULE_COPY = PACKAGE + ".CopyRule";
    public static final String PUZZLE_BUILDER_RULE_NAME = PACKAGE + ".RuleName";

    // Strings used in Log.x to log a message.
    public static final String NULL_INTENT = "Intent is null";
    public static final String NULL_CURSOR = "Cursor is null";
    public static final String PROVIDER_CRASH = "Fail: Provider crashed = ";

    public static final String CONTENT_TYPE_PREFIX = "vnd.motorola.cursor.dir/vnd.motorola.";
    public static final String CONTENT_ITEM_TYPE_PREFIX = "vnd.motorola.cursor.item/vnd.motorola.";

    public static final String IS_MY_RULES_RETRIEVED   = "ismyrulesretrieved";
    public static final String XML_VERSION_SHARED_PREFERENCE = "com.motorola.contextual.xmlversion";

    public static final String DELETE_RULES_SHARED_PREFERENCE = PACKAGE + ".DELETE_RULES_PREF";
    public static final String DELETE_RULES_XML = PACKAGE + ".DELETE_RULES_XML";

    public static final String XML_VERSION   = "xmlversion";
    public static final String XML_HEADER    = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    public static final String QA_SETTINGS_INTENT_METADATA = "com.motorola.smartactions.settings_intent";
    public static final String XML_UPGRADE   = "XmlUpgrade";
    /*
     * Note: Name and the key of SharedPreference should be always in
     * sync with the values in BlurRulesUpdater module. Any change made here should be repeated
     * in the BlurRulesUpdater module
     */
    // Sync START
    public static final String SMARTACTIONFW_PACKAGE = "com.motorola.contextual.fw";
    public static final String RULES_UPDATER_PACKAGE = SMARTACTIONFW_PACKAGE + ".importer";
    public static final String NEWRULES_SHARED_PREFERENCE = RULES_UPDATER_PACKAGE + ".newrules";

    //File name where "my rules" restored are stored
    public static final String MYRULES_RESTORED_FILENAME = "myrules_restored.xml";
    // Sync END


    public static final String TIMEFRAME_SHARED_PREFERENCE = "com.motorola.contextual.timeframepreference";
    public static final String TIMEFRAME_XML_CONTENT = "com.motorola.contextual.timeframexml";

    public static final String LOCATION_SHARED_PREFERENCE = "com.motorola.contextual.locationpreference";
    public static final String LOCATION_XML_CONTENT = "com.motorola.contextual.locationxml";

    public static final String CALENDAR_EVENTS_XML_CONTENT = "com.motorola.contextual.calendarevents";
    public static final String CALENDAR_EVENTS_SHARED_PREFERENCE = "com.motorola.contextual.calendareventssharedpreference";

    //Strings used in RulesImporter
    public static final String XML_TAG = "RULE";

    public static final String XML_TAG_STATE = "STATE";

    public static final String XML_TAG_ACTION = "ACTION";

    /** The value of this constant is {@value}.*/
    public static final String RULES_FOLDER_NAME 	= "rules.d";

    /** The value of this constant is {@value}.*/
    public static final String RULES_IMPORTER_FILE_NAME = "rulesimporter.rules";

    public static final String LAUNCH_CONDITION_BUILDER =
        "com.motorola.rules.launch.conditionbuilder";

    public static final String LAUNCH_CANNED_RULES =
        "com.motorola.rules.launch.CANNED_RULES";

    public static final String LAUNCH_POWERUP_CANNED_RULES =
        "com.motorola.rules.launch.powerup.cannedrules";

    public static final String LAUNCH_IMPORTER_FROM_RP =
        "com.motorola.smartactions.PUBLISH_RULE";

    public static final String LAUNCH_RULES_EXPORTER =
        "com.motorola.contextual.launch.rulesexporter";

    // To Notify Backup Manager about data change
    public static final String NOTIFY_DATA_CHANGE =
        								"com.motorola.contextual.notify.DATA_CHANGE";


    public static final String LAUNCH_INFERENCE_MANAGER_CLEAR_DATA = "com.motorola.ca.ruleinferer.CLEAR_INFERENCE_DATA";

    public static final String KEY_RULE_KEY  = "com.motorola.contextual.rulekey";

    public static final String KEY_RULE_SOURCE  = "com.motorola.contextual.rulesource";

    public static final String KEY_RULE_STATUS = "com.motorola.contextual.status";
    
    public static final String INTENT_RULES_DISABLED =  "com.motorola.contextual.RULES_DISABLED";

    public static final String KEY_RULE_SILENT = "com.motorola.contextual.issilent";

    public static final String INTENT_RULES_IMPORTED =  "com.motorola.contextual.RULES_IMPORTED";


    public static final String INTENT_RULES_VALIDATED =  "com.motorola.contextual.RULES_VALIDATED";

    public static final String INFERRED_RULES_ADDED =  "com.motorola.contextual.INFERRED_RULES_ADDED";

    public static final String EXTRA_RULE_INFO =     "com.motorola.intent.extra.RULE_INFO";

    public static final String EXTRA_SOURCE_LIST =     "com.motorola.intent.extra.SOURCE_LIST";

    public static final String EXTRA_TIMEFRAME_DATA = "com.motorola.contextual.smartprofile.sensors.timesensor.intent.extra.TIMEFRAME_DATA";

    public static final String EXTRA_LOCATION_DATA = "com.motorola.contextual.smartprofile.locations.intent.extra.LOCATION_DATA";

    public static final String EXTRA_CALENDAR_EVENTS_DATA = "com.motorola.contextual.smartprofile.sensors.calendareventsensor.intent.extra.CALENDAR_EVENTS_DATA";

    public static final String RULE_KEY_PREFIX = "com.motorola.contextual.";

    public static final String RULE_KEY_PREFIX_SEARCH = "com.motorola.contextual.%";

    public static final String EXTRA_IS_EXPORT_TO_SDCARD = "com.motorola.intent.extra.EXPORT_TO_SDCARD";

    public static final String MISSED_CALL_PUB_KEY      = "com.motorola.contextual.smartprofile.missedcallsensor";
    public static final String LOCATION_TRIGGER_PUB_KEY = "com.motorola.contextual.smartprofile.location";
    public static final String MOTION_SENSOR_PUB_KEY = "com.motorola.contextual.Motion";
    public static final String PROCESSOR_SPD_PUB_KEY = "com.motorola.contextual.actions.ProcessorSpeed";
    public static final String SYNC_PUB_KEY = "com.motorola.contextual.actions.Sync";

    //Strings used for the Debug provider
    public static final String SMARTRULES_INTERNAL_DBG_MSG = "Smartrules Internal";
    public static final String TO_QUICKACTIONS_OUT_DBG_MSG = "SmartRules to QA";
    public static final String MODEAD_DBG_MSG = "ModeAD Internal";
    public static final String FROM_MODEAD_DBG_MSG = "From ModeAD to SmartRules";
    public static final String SUGG_ACCEPTED_DBG_MSG = "Suggested rule accepted";
    public static final String SUGG_REJECTED_DBG_MSG = "Suggested rule rejected";
    public static final String SUGG_CUSTOMIZE_DBG_MSG = "Suggested rule customized";
    public static final String SAMPLE_RULE_ACCEPTED_DBG_MSG = "Sample rule accepted";
    public static final String RULE_DELETED_STR = "Rule Deleted";
    public static final String RULE_RENAMED_STR = "Rule Renamed";
    public static final String NO_RULES = "No Rules";
    public static final String DISABLE_ALL_RULES = "All Rules Disabled";
    public static final String AUTO_ACTIVE_TO_DISABLED = "Automatic Rule : Active to Disabled";
    public static final String AUTO_READY_TO_DISABLED = "Automatic Rule : Ready to Disabled";
    public static final String AUTO_DISABLED_TO_READY = "Automatic Rule : Disabled to Ready";
    public static final String MAN_DISABLED_TO_ACTIVE = "Manual rule: Disabled to Active";
    public static final String MAN_ACTIVE_TO_DISABLED = "Manual rule: Active to Disabled";
    public static final String AUTO_MAX_ENABLED_RULES = "Automatic Rule: Ignore Enable as max visible auto enabled rules";
    public static final String CONFLICT_ACTION_NOT_FIRED = "Conflict: Action not fired";
    public static final String ACTION_REQ_NORMAL_SET = "Action: Set rule value";
    public static final String ACTION_RESTORE_DEFAULT = "Action: Restore default";
    public static final String ACTION_STATELESS = "Action: Stateless";
    public static final String VISIBLE_RULE = "Visible Rule";
    public static final String RULEKEY_UPDATED = "RuleKey Updated";
    public static final String USER_CREATED_RULE_DBG_MSG = "User created rule";
    public static final String EDIT_DBG_MSG = ":Edit";
    public static final String SUGG_INBOX_DBG_MSG = "Suggested rule in inbox";
    public static final String ABOUT_VERSION = "About Version";
    public static final String HELP_LAUNCHED = "Help Page Lanuched";
    public static final String CHECK_STATUS = "Check Status";
    public static final String MYPROFILE_LOC = "My Profile Location";
    public static final String MYPROFILE_TIMEFRAME = "My Profile TimeFrame";
    public static final String SETTINGS_RULE = "Settings Rule Status Notifications";
    public static final String SETTINGS_SUGG = "Settings Suggestion Notifications";
    public static final String SETTINGS_LOC = "Settings Motorola Location Services";
    public static final String RULE_ICON_MODIFIED = "Rule Icon Modified";
    public static final String NEW_RULE_CREATED = "New rule created";
    public static final String CHILD_RULE = "Child Rule";

    // Intent to Launch Inference Manager
    public static final String LAUNCH_INFERENCE_MANAGER_FOR_DOTNEXT =
        "com.motorola.contextual.dotnext.PUBLISHED";
    public static final String EXTRA_DOT_NEXT_CONTENT =
        "com.motorola.intent.extra.DOT_NEXT_CONTENT";

    public static final String LAUNCH_INFERENCE_MANAGER_TO_DELETE_RULE = "com.motorola.ca.ruleinferer.DELETE_RULE";
    public static final String EXTRA_DELETE_RULE_INFERENCE = "com.motorola.intent.extra.DELETE_INFERENCE";
    public static final String EXTRA_DELETE_RULE_NEXT_CONTENT = "com.motorola.intent.extra.DELETE_NEXT_CONTENT";

    // Intent to send data to TimeFrame module
    public static final String TIMEFRAME_IMPORT_ACTION =
        "com.motorola.contextual.smartprofile.sensors.timesensor.IMPORT";

    public static final String LOCATION_IMPORT_ACTION =
        "com.motorola.contextual.smartprofile.sensors.locationsensor.IMPORT";
    public static final String LAUNCH_LOC_SVC_UPON_CONSENT = "com.motorola.contextual.locationconsent";
    public static final String LOC_CONSENT_SET ="1";
    public static final String LOC_CONSENT_NOTSET ="0";
    public static final String CALENDAR_EVENTS_IMPORT_ACTION = "com.motorola.contextual.smartprofile.sensors.calendareventsensor.IMPORT";

    // Intent to Launch Rules Updater
    public static final String LAUNCH_RULES_UPDATER =
        "com.motorola.contextual.launch.RULES_UPDATER";
    public static final String EXTRA_RULE_TYPE =
        "com.motorola.contextual.intent.extra.RULE_TYPE";
    public static final String EXTRA_IS_DOWNLOAD =
        "com.motorola.contextual.intent.extra.IS_DOWNLOAD";
    public static final String EXTRA_XML_CONTENT =
        "com.motorola.contextual.intent.extra.XML_CONTENT";

    public static final String SMARTRULE_NODE =  "SMARTRULE";
    public static final String NEXT_CONTENT =  "NEXT_CONTENT";
    public static final String RULES  = "RULES";
    public static final String TIMEFRAMES_NODE = "TIMEFRAMES";
    public static final String LOCATION_NODE = "LOCATION";
    public static final String CALENDAR_EVENTS_NODE = "CALENDAR_EVENTS";
    public static final String DELETE_RULES_NODE = "DELETE_RULES";
    public static final String ROOT_START_TAG =  "<ROOT>";
    public static final String ROOT_END_TAG =  "</ROOT>";
    public static final String XML_VERSION_NODE =  "VERSION";
    public static final float  DEFAULT_XML_VERSION = 0.0f;

    public static final int SHIFT_LOCPROVIDER = 0;
    public static final int SHIFT_ADA = 1;
    public static final int SHIFT_AIRPLANE_ON = 2;
    public static final int SHIFT_WIFI_AUTOSCAN = 3;
    public static final int SHIFT_WIFISLEEP = 4;
    public static final int MASK_LOCPROVIDER = 1;
    public static final int MASK_ADA = 2;

    public static final int MASK_AIRPLANE_ON = 4;
    public static final int MASK_WIFI_AUTOSCAN = 8;
    public static final int MASK_WIFISLEEP = 16;

    public static final int INVALID_KEY = -1;
    public static final int INVALID_REQUEST_ID = -1;
    // This is used to distinguish from other source types
    // as well as for LifeCycle, Since LifeCycle has Behavioral
    // type as IMMEDIATE (-1), hence the default value is kept as -2.
    public static final int INVALID = -2;

    public static final String ACTION_SA_CORE_INIT_COMPLETE = "com.motorola.smartactions.intent.action.SA_CORE_INIT_COMPLETE";
    public static final String EXTRA_SA_CORE_INIT_COMPLETE_SENT =
		"com.motorola.smartactions.intent.extra.SA_CORE_INIT_COMPLETE_SENT";

    /** TODO These constants should preferably be stored in the class which owns them.
     *  By putting them in here, ownership or specialization is dangling. We should really
     *  find an appropriate class for each.
     */
    public static final String ACTION_PUBLISHER_UPDATER =  "com.motorola.contextual.smartrules.intent.action.PUBLISHER_UPDATER";
    public static final String ACTION_RULESTATE_CHANGED = "com.motorola.contextual.smartrules.intent.action.RULE_STATE_CHANGED";
    public static final String ACTION_GET_CONFIG = "com.motorola.smartactions.intent.action.GET_CONFIG";
    public static final String ACTION_PUBLISHER_EVENT = "com.motorola.smartactions.intent.action.ACTION_PUBLISHER_EVENT";
    public static final String ACTION_CONDITION_PUBLISHER_EVENT =
        "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_EVENT";
    public static final String ACTION_LAUNCH_RULES_VALIDATOR_ONUPGRADE = "com.motorola.contextual.smartrules.intent.action.LAUNCH_RULES_VALIDATOR_ONUPGRADE";
    public static final String ACTION_LAUNCH_MODE_ACT_DEACTIVATOR =
        "com.motorola.contextual.smartrules.intent.action.LAUNCH_MODEAD";
    public static final String LAUNCH_CP_INTERFACE =
        "com.motorola.contextual.smartrules.intent.action.LAUNCH_CPINTERFACE";
    public static final String LAUNCH_RB_INTERFACE =
        "com.motorola.contextual.smartrules.intent.action.LAUNCH_RBINTERFACE";

    public static final String ACTION_RULES_VALIDATE_REQUEST = "com.motorola.smartactions.intent.action.RULES_VALIDATE_REQUEST";
    public static final String ACTION_RULES_VALIDATE_RESPONSE = "com.motorola.smartactions.intent.action.RULES_VALIDATE_RESPONSE";

    public static final String LAUNCH_REASON_COMMAND_RESPONSE = "CommandResponse";
    public static final String ACTION_CONDITION_PUBLISHER_NOTIFY = "com.motorola.contextual.smartrules.intent.action.NOTIFY";
    public static final String ACTION_COMMAND_RESPONSE = "com.motorola.contextual.smartrules.intent.action.COMMAND_RESPONSE";
    public static final String EXTRA_SUBSCRIBER_ACTION = "com.motorola.contextual.smartrules.intent.extra.SUBSCRIBER_ACTION";

    public static final String CONDITION_PUBLISHER_EVENT = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_EVENT";
    public static final String RULE_PUBLISHER_EVENT = "com.motorola.contextual.smartrules.intent.action.RULE_PUBLISHER_EVENT";

    public static final String EXTRA_RULE_KEY = "com.motorola.contextual.smartrules.intent.extra.RULEKEY";
    public static final String EXTRA_STATE = "com.motorola.smartactions.intent.extra.STATE";
    public static final String EXTRA_CONFIG = "com.motorola.smartactions.intent.extra.CONFIG";
    public static final String EXTRA_EVENT_TYPE = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_PUBLISHER_KEY = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY";
    public static final String EXTRA_STATUS = "com.motorola.smartactions.intent.extra.STATUS";
    public static final String EXTRA_RESPONSE_ID = "com.motorola.smartactions.intent.extra.RESPONSE_ID";
    public static final String EXTRA_REQUEST_ID = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String EXTRA_VERSION = "com.motorola.smartactions.intent.extra.VERSION";
    public static final String EXTRA_SAVE_DEFAULT = "com.motorola.smartactions.intent.extra.SAVE_DEFAULT";
    public static final String EXTRA_COMMAND = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_FAILURE_DESCRIPTION = "com.motorola.smartactions.intent.extra.FAILURE_DESCRIPTION";
    public static final String EXTRA_DESCRIPTION = "com.motorola.smartactions.intent.extra.DESCRIPTION";
    public static final String EXTRA_STATE_DESCRIPTION = "com.motorola.smartactions.intent.extra.STATE_DESCRIPTION";
    public static final String EXTRA_RULE_ENDS = "com.motorola.smartactions.intent.extra.WHEN_RULE_ENDS";
    public static final String EXTRA_NAME = "com.motorola.contextual.smartrules.intent.extra.NAME";
    public static final String EXTRA_DATA_CLEARED    = "com.motorola.smartactions.intent.extra.DATA_CLEARED";

    public static final String EXTRA_DEFAULT_URI = "com.motorola.smartactions.intent.extra.DEFAULT_URI";
    public static final String EXTRA_LAUNCH_REASON = "com.motorola.contextual.smartrules.intent.extra.LAUNCH_REASON";
    public static final String EXTRA_DISABLE_ALL = "com.motorola.contextual.smartrules.intent.extra.DISABLE_ALL";
    public static final String EXTRA_CONFIG_PUBKEY_LIST = "com.motorola.contextual.smartrules.intent.extra.CONFIG_PUBKEY_LIST";
    public static final String EXTRA_PREV_CONFIG_PUBKEY_LIST =
        "com.motorola.contextual.smartrules.intent.extra.PREV_CONFIG_PUBKEY_LIST";
    public static final String EXTRA_STATE_LIST = "com.motorola.contextual.smartrules.intent.extra.STATE_LIST";
    public static final String EXTRA_CONFIG_LIST = "com.motorola.contextual.smartrules.intent.extra.CONFIG_LIST";
    public static final String EXTRA_CONFIG_STATE_MAP = "com.motorola.smartactions.intent.extra.STATES";
    public static final String EXTRA_CALLBACK_INTENT = "com.motorola.contextual.smartrules.intent.extra.CALLBACK_INTENT";
    public static final String EXTRA_IMPORT_TYPE = "com.motorola.contextual.smartrules.intent.extra.IMPORT_TYPE";
    public static final String EXTRA_RULE_LIST = "com.motorola.smartactions.intent.extra.RULE_LIST";
    public static final String EXTRA_RULE_STATUS = "com.motorola.smartactions.intent.extra.STATUS";
    public static final String EXTRA_RP_RESPONSE = "com.motorola.smartactions.intent.extra.RESULT_DETAIL";

    public static final String CONDITION_PUBLISHER_VERSION = "1.0";
    public static final String ACTION_PUBLISHER_VERSION = "1.0";
    public static final float VERSION = 1.0f;
    public static final float INVALID_VERSION = -1.0f;

    /** TODO These constants (Commands) should preferable be stored in the class which owns them. We should
     *	have a class or interface which abstracts the commands. That class should be probably an internal
     * class to a class named Publisher. So the constant reference would be Publisher.Command.COMMAND_FIRE.
     */
    public static final String COMMAND_FIRE = "fire_request";
    public static final String COMMAND_REVERT = "revert_request";
    public static final String COMMAND_REFRESH = "refresh_request";
    public static final String COMMAND_SUBSCRIBE = "subscribe_request";
    public static final String COMMAND_CANCEL = "cancel_request";
    public static final String FIRE_RESPONSE = "fire_response";
    public static final String REVERT_RESPONSE = "revert_response";
    public static final String REFRESH_RESPONSE = "refresh_response";
    public static final String INITIATE_REFRESH_REQUEST = "initiate_refresh_request";
    public static final String SUBSCRIBE_RESPONSE = "subscribe_response";
    public static final String CANCEL_RESPONSE = "cancel_response";
    public static final String PUBLISH_RULE_RESPONSE = "publish_rule_response";
    public static final String STATE_CHANGE = "state_change";
    public static final String NOTIFY = "notify";

    public static final String RULE_CREATED = "RuleCreated";
    public static final String RULE_EDITED = "RuleEdited";
    public static final String RULE_DELETED = "RuleDeleted";
    public static final String DISABLE_ALL= "DisableAllRules";

    public static final String CONDITIONBUILDER_TABLE_NAME 			= " ConditionBuilder";
    public static final String CONDITIONSENSOR_TABLE_NAME 			= " ConditionSensor";

    public static final String ACTION_PACKAGE_MANAGER_STRING = "com.motorola.smartactions.intent.category.ACTION_PUBLISHER";
    public static final String ACTION_TYPE = "com.motorola.smartactions.action_type";
    public static final String MARKET_URL = "com.motorola.smartactions.download_link";
    public static final String CONDITION_PACKAGE_MANAGER_STRING = "com.motorola.smartactions.intent.category.CONDITION_PUBLISHER";
    public static final String GENERIC_PUBLISHER_KEY = "com.motorola.smartactions.publisher_key";
    public static final String CONDITION_TYPE = "com.motorola.contextual.smartrules.preconditiontype";

    public static final boolean CONFLICT_RESOLUTION = true;
    public static final String ADA_ACCEPTED_KEY = "com.motorola.analytics.ada_accepted";

    public static final String SA_CORE_KEY = "com.motorola.contextual.smartrules.sacore";
    public static final String ACTION_CONDITION_PUBLISHER_REQUEST = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_REQUEST";
    public static final String EXTRA_CONSUMER = "com.motorola.smartactions.intent.extra.CONSUMER";
    public static final String EXTRA_CONSUMER_PACKAGE  = "com.motorola.smartactions.intent.extra.CONSUMER_PACKAGE";

    public static final String CONDITION_PUBLISHER_DATA_RESET = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_DATA_RESET";
    public static final String EXTRA_PUBLISHER_KEY_LIST ="com.motorola.smartactions.intent.extra.PUBLISHER_KEY_LIST";

    public interface XmlTag {
        /** The value of this constant is {@value}.*/
        public String B = "<";
        /** The value of this constant is {@value}.*/
        public String E = ">";
        /** The value of this constant is {@value}.*/
        public String F = "</";
    }

    public static final String PERM_CONDITION_PUBLISHER         = "com.motorola.smartactions.permission.CONDITION_PUBLISHER";
    public static final String PERM_CONDITION_PUBLISHER_USER    = "com.motorola.smartactions.permission.CONDITION_PUBLISHER_USER";
    public static final String PERM_CONDITION_PUBLISHER_ADMIN   = "com.motorola.smartactions.permission.CONDITION_PUBLISHER_ADMIN";
    public static final String PERM_ACTION_PUBLISHER            = "com.motorola.smartactions.permission.ACTION_PUBLISHER";
    public static final String PERM_ACTION_PUBLISHER_ADMIN      = "com.motorola.smartactions.permission.ACTION_PUBLISHER_ADMIN";
    public static final String PERM_RULE_PUBLISHER              = "com.motorola.smartactions.permission.RULE_PUBLISHER";
    public static final String PERM_RULE_PUBLISHER_ADMIN        = "com.motorola.smartactions.permission.RULE_PUBLISHER_ADMIN";
}
