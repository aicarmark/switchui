/*
 * @(#)Constants.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * csd053        2010/11/26 NA                Initial version
 * a18491        2011/02/18 NA                Incorporated first set of
 *                                            review comments
 * csd053        2011/03/08 NA				  Added additional constants for
 * 											  Location.
 *
 */
package com.motorola.contextual.smartprofile;



public interface Constants {

    public static final boolean PRODUCTION_MODE = true;
    public static final boolean LOG_INFO = true; // should stay true even in production mode
    public static final boolean LOG_DEBUG = !PRODUCTION_MODE;
    public static final boolean LOG_VERBOSE = !PRODUCTION_MODE;

    public static final boolean DEBUG_COMPONENTS = true; //for easy debugging

    // Used in Missed Call and Incoming Call Sensors
    public static final String COLON_SEPARATOR = ":";
    public static final String HYPHEN_SEPARATOR = "-";

    // Rules Composer related intent strings
    /** The value of this constant is {@value}.*/
    public static final String EVENT_NAME = "com.motorola.rulescomposer.EventName";
    /** The value of this constant is {@value}.*/
    public static final String EVENT_DESC = "com.motorola.rulescomposer.EventDescription";
    /** The value of this constant is {@value}.*/
    public static final String EDIT_URI = "com.motorola.rulescomposer.EditUri";
    /** The value of this constant is {@value}.*/
    public static final String EVENT_TARGET_STATE = "com.motorola.rulescomposer.EventTargetState";
    /** The value of this constant is {@value}.*/
    public static final String STATE_PUBLISHER_KEY = "com.motorola.smartactions.publisher_key";
    /** The value of this constant is {@value}.*/
    public static final String VSENSOR = "VSENSOR";
    /** The config's version */
    public static final String CONFIG_VERSION = "Version";

    /** Meaningful location sensor related strings */
    /** String sequence that is used to split the incoming string from Rules Composer. */
    public static final String URI_TO_FIRE_STRING = "#Intent;action=android.intent.action.MAIN;component=com.motorola.contextual.smartrules/com.motorola.contextual.smartprofile.locations.PoiLocationsListActivity;S.CURRENT_POITAG=";
    public static final String CURRENT_POINAME_STRING = ";S.CURRENT_POINAME=";
    public static final String END_STRING = ";end";
    public static final String SENSOR_NAME_START_STRING = "(#sensor;name=";
    public static final String SENSOR_NAME_END_STRING = ";p0=true;end)";
    public static final String LEFT_PAREN = "(";
    public static final String RIGHT_PAREN = ")";
    public static final String HOME_RULE_SENSOR_STRING = "#sensor;vsensor=setvalue;name=com.motorola.virtualsensor.Meaningfullocation;p0=Home;end";
    public static final String MEANINGFUL_LOC_SENSOR_STRING = "#sensor;name=com.motorola.virtualsensor.Meaningfullocation;p0=";
    public static final String VIRTUAL_SENSOR_STRING = "com.motorola.contextual.";

    /** Preconditions related common strings */
    public static final String VSENSOR_START_TAG = "<VSENSOR>";
    public static final String VSENSOR_END_TAG = "</VSENSOR>";
    public static final String NAME_START_TAG = "<VirtualSensor name =\"";
    public static final String NAME_END_QUOTE_TAG = "\" ";
    public static final String NAME_END_TAG = "</VirtualSensor>";
    public static final String PERSISTENCY_FORVEVER = "persistenceSensor =\"persist_forever\" ";
    public static final String PERSISTENCY_VALUE_FOREVER = "persistenceValue =\"persist_forever\" ";
    public static final String PERSISTENCY_VALUE_REBOOT = "persistenceValue =\"persist_reboot\" ";
    public static final String VENDOR_TAG = "vendor =\"Motorola\" ";
    public static final String VERSION_TAG = "version =\"1\" ";
    public static final String DESCRIPTION_START_TAG = "description =\"";
    public static final String DESCRIPTION_END_TAG = "\"  >";
    public static final String PERMISSIONS_START_TAG = "<permissions clean=\"";
    public static final String PERMISSIONS_ATTRIBUTE_END_TAG = "\">";
    public static final String PERMISSIONS_END_TAG   = "</permissions>";
    public static final String PERM_START_TAG = "<perm name=\"";
    public static final String PERM_END_TAG = "\"/>";
    public static final String INITIAL_VALUE_START_TAG = "<initialValue value=\"";
    public static final String INITIAL_VALUE_END_TAG = "\"/>";
    public static final String POSSIBLE_VALUES_START_TAG = "<possibleValues0> ";
    public static final String VALUE_START_TAG = "<value>";
    public static final String VALUE_END_TAG = "</value>";
    public static final String POSSIBLE_VALUES_END_TAG = "</possibleValues0>";
    public static final String MECHANISM_TAG = "<mechanism value=\"rules\"/>";
    public static final String RULESET_START_TAG = "<ruleset>";
    public static final String RULESET_END_TAG = "</ruleset>";
    public static final String RULE_START_TAG = "<rule>";
    public static final String RULE_END_TAG = "</rule>";
    public static final String MECHANISM_TAG_DERIVED = "<mechanism value=\"derived\"/>";
    public static final String DERIVED_START_TAG = "<derived>";
    public static final String DERIVED_END_TAG = "</derived>";
    public static final String CLAUSE_VALUE_TAG = "<clause value=\"";
    public static final String LOGIC_TAG_START = " logic=\"";
    public static final String LOGIC_TAG_END = "\"/>";
    public static final String TEST_SENSOR = "Test Sensor1";
    public static final char QUOTE = '"';
    public static final String OR_STRING = " OR ";

    public static final String KEY_RULE_SOURCE  = "com.motorola.contextual.rulesource";
    public static final String EXTRA_RULE_INFO =     "com.motorola.intent.extra.RULE_INFO";
    public static final String KEY_RULE_KEY  = "com.motorola.contextual.rulekey";
    public static final String KEY_RULE_STATUS = "com.motorola.contextual.status";

    public static final String INITIAL_VALUE_FALSE = "{false}";
    public static final String INITIAL_VALUE_TRUE = "{true}";

    public static final String POSSIBLE_VALUE_TRUE = "true";
    public static final String POSSIBLE_VALUE_FALSE = "false";

    public static final String END_FALSE_STRING = "%3Bp0%3Dfalse%3Bend;end";
    public static final String END_TRUE_STRING = "%3Bp0%3Dtrue%3Bend;end";
    public static final String END_TRUE = ";p0=true;end";
    public static final String END_FALSE = ";p0=false;end";
    public static final String INTERNAL = "Internal";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    public static final String NO_RESPONSE = "NO_RESPONSE";

    public static final String VERSION = "1.0";
    public static final String VIRTUAL_SENSOR = " Virtual Sensor ";
    public static final String CURRENT_MODE = "CURRENT_MODE";
    public static final String PERM_NAME = "com.motorola.contextual.permission.ACCESS_DESTROY_SMARTRULES_SENSORS";

    // ConditionSensor related strings
    public static final String STATE_VSENSOR = "StateVSensor";
    public static final String CONDITION_SENSOR_URI = "content://com.motorola.contextual.smartrules/ConditionSensor";

    // Condition Table in Smart Rule DB related strings
    public static final String CONDITION_TABLE_URI = "content://com.motorola.contextual.smartrules/Condition";
	public static final String CONDITON_TARGET_STATE = "TargetState";
	public static final String CONDITON_PUBLISHER_KEY = "StatePubKey";
    public static final String CONDITON_SENSOR_NAME = "SensorName";
    public static final String CONDITON_DESCRIPTION = "ConditionDesc";
    public static final String CONDITION_TABLE_RULE_ID = "FkRule_id";

    // Rules Table in Smart Rule DB related strings
    public static final String RULE_TABLE_URI = "content://com.motorola.contextual.smartrules/Rule";
    public static final String RULE_ID = "_id";
    public static final String RULE_KEY = "Key";

    public static final String ID				= "_id";

    // Strings used in Log.x to log a message.
    public static final String NULL_CURSOR = "Fail: Cursor is null";

    // Remove after moved to Smart Rules
    public static final String BLANK_SPC 		= " ";
    public static final String QUOTE_REPLACE    = "''";

    public static final String REM_REGEX_TO_BE_REPLACED = "%";
    public static final String REM_REGEX_TO_REPLACE = "reminderreplaced25";
    public static final String SEM_REGEX_TO_BE_REPLACED = ";";
    public static final String SEM_REGEX_TO_REPLACE = "semicolonreplaced3B";
    public static final String HASH_REGEX_TO_BE_REPLACED = "#";
    public static final String HASH_REGEX_TO_REPLACE = "hashreplaced35";
    public static final String COLON_REGEX_TO_BE_REPLACED = ":";
    public static final String COLON_REGEX_TO_REPLACE = "colonreplaced3A";
    public static final String HYPHEN_REGEX_TO_BE_REPLACED = "-";
    public static final String HYPHEN_REGEX_TO_REPLACE = "hyphenreplaced2D";
    public static final String SLASH_REGEX_TO_BE_REPLACED = "/";
    public static final String SLASH_REGEX_TO_REPLACE = "slashreplaced2F";
    
    public static final String BOOT_COMPLETE_INTENT = "com.motorola.intent.action.BOOT_COMPLETE_RECEIVED";
    
    // To Notify Backup Manager about a data change
    public static final String NOTIFY_DATA_CHANGE =
									"com.motorola.contextual.notify.DATA_CHANGE";

    public static final String EXTRA_SUBSCRIBER_ACTION = "com.motorola.contextual.smartrules.intent.extra.SUBSCRIBER_ACTION";
    public static final String EXTRA_RESPONSE = "com.motorola.contextual.smartrules.intent.extra.RESPONSE";
    public static final String EXTRA_CONFIG = "com.motorola.smartactions.intent.extra.CONFIG";
    public static final String EXTRA_CONFIG_LIST = "com.motorola.contextual.smartrules.intent.extra.CONFIG_LIST";
    public static final String EXTRA_CONFIG_STATE_MAP = "com.motorola.smartactions.intent.extra.STATES";
    public static final String COMMAND = "com.motorola.contextual.smartrules.intent.action.COMMAND";
    public static final String COMMAND_RESPONSE = "com.motorola.contextual.smartrules.intent.action.COMMAND_RESPONSE";
    public static final String EXTRA_RESPONSE_ID = "com.motorola.smartactions.intent.extra.RESPONSE_ID";
    public static final String EXTRA_STATE = "com.motorola.smartactions.intent.extra.STATE";
    public static final String EXTRA_STATE_LIST = "com.motorola.contextual.smartrules.intent.extra.STATE_LIST";
    public static final String EXTRA_DESCRIPTION = "com.motorola.smartactions.intent.extra.DESCRIPTION";
    public static final String EXTRA_NAME = "com.motorola.contextual.smartrules.intent.extra.NAME";
    public static final String EXTRA_REQUEST_ID = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String EXTRA_NOTIFY = "com.motorola.contextual.smartrules.intent.action.NOTIFY";
    public static final String EXTRA_PUB_KEY = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY";
    public static final String EXTRA_COMMAND = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_CONFIG_ITEMS = "com.motorola.smartactions.intent.extra.CONFIG_ITEMS";
    public static final String EXTRA_DATA_CLEARED    = com.motorola.contextual.smartrules.Constants.EXTRA_DATA_CLEARED;
    public static final String SMART_RULES_PKG_NAME = "com.motorola.contextual.smartrules";
    public static final String PUB_KEY =   "com.motorola.smartactions.publisher_key";
    public static final String EXTRA_EVENT_TYPE = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String SUBSCRIBE_RESPONSE = "subscribe_response";
    public static final String CANCEL_RESPONSE = "cancel_response";
    public static final String CANCEL_REQUEST = "cancel_request";
    public static final String NOTIFY_REQUEST = "notify_request";
    public static final String ALL_CONFIGS = "*";
    public static final String NOTIFY = "notify";
    public static final String ACTION_CONDITION_PUBLISHER_EVENT = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_EVENT";
    public static final String SA_CORE_INIT_COMPLETE = com.motorola.contextual.smartrules.Constants.ACTION_SA_CORE_INIT_COMPLETE;
    public static final String REFRESH_RESPONSE = "refresh_response";
    public static final String ASYNC_REFRESH = "initiate_refresh_request";
    public static final String LIST_RESPONSE = "list_response";
    public static final String EXTRA_NEW_STATE_TITLE = "com.motorola.smartactions.intent.extra.NEW_STATE_TITLE";
    public static final String TAG_CONFIG_ITEMS = "CONFIG_ITEMS";
    public static final String TAG_ITEM = "ITEM";
    public static final String TAG_CONFIG = "CONFIG";
    public static final String TAG_DESCRIPTION = "DESCRIPTION";
    public static final String EXTRA_STATUS = "com.motorola.smartactions.intent.extra.STATUS";
    public static final String EXTRA_FAILURE_DESCRIPTION = "com.motorola.smartactions.intent.extra.FAILURE_DESCRIPTION";
    public static final double CURRENT_VERSION = 1.0;

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    public static final String CONFIG_ITEMS_START = "<CONFIG_ITEMS>";
    public static final String CONFIG_ITEMS_END = "</CONFIG_ITEMS>";
    public static final String ITEM_START = "<ITEM>";
    public static final String ITEM_END = "</ITEM>";
    public static final String CONFIG_START = "<CONFIG>";
    public static final String CONFIG_END = "</CONFIG>";
    public static final String DESCRIPTION_START = "<DESCRIPTION>";
    public static final String DESCRIPTION_END = "</DESCRIPTION>";
    public static final String EXTRA_STATE_MON_CLASS ="STATE_MONITOR_CLASS";
    public static final String EXTRA_REGISTER ="REGISTER";
    public static final String ACTION_NAME = "ACTION_NAME";
    public static final String CLASS_NAME = "CLASS_NAME";
    public static final String RECEIVER = "Receiver";
    public static final String OBSERVER = "Observer";
    public static final String LESS_THAN            = "<";
    public static final String CONDITION_KEY = "com.motorola.contextual.smartrules.conditionkey";
    public static final String OLD_CONFIG_PREFIX = "S.CURRENT_MODE=";
    public static final String INTENT_PREFIX = "#Intent";
    public static final String EXTRA_LIST_RESPONSE = "list_response";

    //permissions
    public static final String PERM_CONDITION_PUBLISHER_ADMIN = "com.motorola.smartactions.permission.CONDITION_PUBLISHER_ADMIN";

}
