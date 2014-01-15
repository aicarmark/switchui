package com.motorola.contextual.rule;


public interface CoreConstants {

    /*************** START: SA Core Constants ********************/
    // Protocol Version supported with respect to Core
    public static final float VERSION = com.motorola.contextual.smartrules.Constants.VERSION;
    public static final float INVALID_VERSION = com.motorola.contextual.smartrules.Constants.INVALID_VERSION;


    public static final String SA_CORE_INIT_COMPLETE = com.motorola.contextual.smartrules.Constants.ACTION_SA_CORE_INIT_COMPLETE;
    public static final String PUBLISH_TO_SA_CORE = com.motorola.contextual.smartrules.Constants.LAUNCH_IMPORTER_FROM_RP;
    public static final String XML_CONTENT         = com.motorola.contextual.smartrules.Constants.NEW_XML_CONTENT;
    public static final String PUB_KEY  = com.motorola.contextual.smartrules.Constants.EXTRA_PUBLISHER_KEY;
    public static final String REQUEST_ID = com.motorola.contextual.smartrules.Constants.EXTRA_REQUEST_ID;
    public static final String EXTRA_RESPONSE_ID = com.motorola.contextual.smartrules.Constants.EXTRA_RESPONSE_ID;
    public static final String EXTRA_COMMAND = com.motorola.contextual.smartrules.Constants.EXTRA_COMMAND;
    public static final String EXTRA_RULE_LIST = com.motorola.contextual.smartrules.Constants.EXTRA_RULE_LIST;
    public static final String REFRESH_REQUEST = com.motorola.contextual.smartrules.Constants.COMMAND_REFRESH;
    public static final String PUBLISH_RULE_RESPONSE = com.motorola.contextual.smartrules.Constants.PUBLISH_RULE_RESPONSE;
    public static final String EXTRA_RULE_STATUS =
                com.motorola.contextual.smartrules.Constants.EXTRA_RULE_STATUS;
    public static final String EXTRA_VERSION =
        com.motorola.contextual.smartrules.Constants.EXTRA_VERSION;
    public static final String EXTRA_RP_RESPONSE = com.motorola.contextual.smartrules.Constants.EXTRA_RP_RESPONSE;
    public static final String EXTRA_DATA_CLEARED    = com.motorola.contextual.smartrules.Constants.EXTRA_DATA_CLEARED;

     public static final String RP_RESPONSE_ACCEPT = com.motorola.contextual.smartrules.Constants.RP_RESPONSE_ACCEPT;
     public static final String RP_RESPONSE_CUSTOMIZE = com.motorola.contextual.smartrules.Constants.RP_RESPONSE_CUSTOMIZE;
     public static final String RP_RESPONSE_REJECT = com.motorola.contextual.smartrules.Constants.RP_RESPONSE_REJECT;

     public static final String LOCATION_INFERRED_ACTION =
         com.motorola.contextual.smartprofile.locations.LocConstants.LOCATION_INFERRED_ACTION;
     public static final String LOCATION_TAG   = "LOCATION_TAG";
     public static final String HOME_POI       = "Home";

    public static interface RulesImporterImportType{
        int INFERRED        = com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType.INFERRED;
    }

    public static final String OUTBOUND_SMS_ACTION  = "com.motorola.trigger.sentsms";

    // Possible values for TYPE column
    public interface RuleType {
        final String SUGGESTED     = "suggestion";
        final String FACTORY       = "sample";
        final String DELETE        = "delete";
        final String CHILD         = "child";
    }
    /*************** END: SA Core Constants ********************/

    /*************** START: Publisher Constants ********************/
    // publicly accessed by PublisherService
    public static final String CONFIG_EXTRA     = "com.motorola.smartactions.intent.extra.CONFIG";
    public static final String REQUEST_ID_EXTRA = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String REFRESH_COMMAND  = "refresh_request";
    public static final String SUBSCRIBE_REQUEST  = "subscribe_request";
    public static final String SUBSCRIBE_CANCEL  = "cancel_request";

    // local constants
    public static final String RESPONSE_ID_EXTRA = "com.motorola.smartactions.intent.extra.RESPONSE_ID";
    public static final String RESPONSE_EXTRA    = com.motorola.contextual.smartrules.Constants.EXTRA_RESPONSE_ID;
    public static final String DESCRIPTION_EXTRA = com.motorola.contextual.smartrules.Constants.EXTRA_DESCRIPTION;
    public static final String NAME_EXTRA        = com.motorola.contextual.smartrules.Constants.EXTRA_NAME;
    public static final String EXTRA_STATE       = com.motorola.contextual.smartrules.Constants.EXTRA_STATE;


    public static final String REFRESH_RESPONSE  = com.motorola.contextual.smartrules.Constants.RULE_PUBLISHER_EVENT;
    public static final String SUBSCRIBE_RESPONSE = com.motorola.contextual.smartrules.Constants.SUBSCRIBE_RESPONSE;
    public static final String CANCEL_RESPONSE = com.motorola.contextual.smartrules.Constants.CANCEL_RESPONSE;
    public static final String EXTRA_EVENT_TYPE = com.motorola.contextual.smartrules.Constants.EXTRA_EVENT_TYPE;
    public static final String NOTIFY = com.motorola.contextual.smartrules.Constants.NOTIFY;
    public static final String CANCEL_RESP = com.motorola.contextual.smartrules.Constants.CANCEL_RESPONSE;
    public static final String EXTRA_CONFIG_LIST = com.motorola.contextual.smartrules.Constants.EXTRA_CONFIG_LIST;
    public static final String EXTRA_STATE_LIST = com.motorola.contextual.smartrules.Constants.EXTRA_STATE_LIST;
    public static final String EXTRA_CONFIG_STATE_MAP = com.motorola.contextual.smartrules.Constants.EXTRA_CONFIG_STATE_MAP;
    public static final String EXTRA_PUB_KEY     = com.motorola.contextual.smartrules.Constants.EXTRA_PUBLISHER_KEY;
    public static final String EXTRA_CONSUMER = com.motorola.contextual.smartrules.Constants.EXTRA_CONSUMER;
    public static final String EXTRA_CONSUMER_PACKAGE = com.motorola.contextual.smartrules.Constants.EXTRA_CONSUMER_PACKAGE;

    public static final String OBSERVER = "Observer";
    public static final String XML_INSERT_SUCCESS ="success";

    public static final String CONFIG_TF_NIGHT = "TimeFrame=(260.Night);Version=1.0";
    public static final String CONFIG_CALENDAR = "Version 1.0;events_from_calendars null;exclude_all_day_events true;include_events_with_multiple_people_only false;include_accepted_events_only false;notify_strictly_on_time false";
    public static final String CONFIG_MISSED_CALL = "MissedCall=(.*)(1);Version=1.0";
    public static final String CONFIG_TF_DATE_CHANGE = "TimeFrame=(tf_day_change);Version=1.0";

    public static final String PUBLISHER_KEY_TIMEFRAME  = com.motorola.contextual.smartrules.Constants.TIMEFRAME_PUBLISHER_KEY;
    public static final String PUBLISHER_KEY_CALENDAR   = com.motorola.contextual.smartrules.Constants.CALENDAR_SENSOR_PUBLISHER_KEY;
    public static final String PUBLISHER_KEY_LOCATION   = com.motorola.contextual.smartrules.Constants.LOCATION_TRIGGER_PUB_KEY;
    public static final String PUBLISHER_KEY_RINGER_ACTION   = com.motorola.contextual.smartrules.Constants.RINGER_ACTION_PUBLISHER_KEY;
    public static final String PUBLISHER_KEY_MISSED_CALL= "com.motorola.contextual.smartprofile.missedcallsensor";

    public static final String PERM_CONDITION_PUBLISHER_USER = com.motorola.contextual.smartrules.Constants.PERM_CONDITION_PUBLISHER;
    public static final String PERM_CONDITION_PUBLISHER_ADMIN = com.motorola.contextual.smartrules.Constants.PERM_CONDITION_PUBLISHER_ADMIN;
    public static final String PERM_RULE_PUBLISHER_ADMIN = com.motorola.contextual.smartrules.Constants.PERM_RULE_PUBLISHER_ADMIN;

    // for sleep rule
    public static final String KEY_IGNORE_VOLUME_CHANGE = com.motorola.contextual.actions.Constants.KEY_IGNORE_VOLUME_CHANGE;
    public static final String ACTIONS_PACKAGE = com.motorola.contextual.actions.Constants.PACKAGE;
    /*************** END: Publisher Constants ********************/
}
