/*
 * @(#)PsfConstants.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf;

import com.motorola.contextual.smartrules.Constants;

/**
 * Declares constants used across PSF (Publisher Sharing Framework)
 * 
 * CLASS:
 *     PsfConstants
 *
 * RESPONSIBILITIES:
 *     Constants used across PSF
 *
 */
public interface PsfConstants {

    // Prefix used for logging by components inside PSF
    public static final String  PSF_PREFIX = "Psf";

    // Logging related constants
    public static final boolean PRODUCTION_MODE     = Constants.PRODUCTION_MODE;
    public static final boolean LOG_INFO            = Constants.LOG_INFO;
    public static final boolean LOG_WARN            = true;
    public static final boolean LOG_ERROR           = true;
    public static final boolean LOG_DEBUG           = Constants.LOG_DEBUG;
    public static final boolean LOG_VERBOSE         = Constants.LOG_VERBOSE;

    // Intent actions
    public static final String ACTION_PSF_INIT              = "com.motorola.contextual.smartrules.intent.action.PSF_INIT";
    public static final String ACTION_PSR_INIT              = "com.motorola.contextual.smartrules.intent.action.PSR_INIT";
    public static final String ACTION_AP_RESPONSE           = "com.motorola.smartactions.intent.action.ACTION_PUBLISHER_EVENT";
    public static final String ACTION_SA_CORE_INIT_COMPLETE = "com.motorola.smartactions.intent.action.SA_CORE_INIT_COMPLETE";
    public static final String ACTION_MEDIATOR_INIT         = "com.motorola.contextual.smartrules.intent.action.MEDIATOR_INIT";
    public static final String ACTION_PUBLISHER_META_DATA_SETTINGS_ACTION   = "com.motorola.smartactions.settings_intent";

    public static final String ACTION_PUBLISHER_ACTION      = "com.motorola.smartactions.intent.action.GET_CONFIG";
    public static final String CONDITION_PUBLISHER_ACTION   = "com.motorola.smartactions.intent.action.GET_CONFIG";
    public static final String RULE_PUBLISHER_ACTION   		= "com.motorola.smartactions.intent.action.GET_INFO";

    // Intent categories
    public static final String ACTION_PUBLISHER_CATEGORY    = "com.motorola.smartactions.intent.category.ACTION_PUBLISHER";
    public static final String CONDITION_PUBLISHER_CATEGORY = "com.motorola.smartactions.intent.category.CONDITION_PUBLISHER";
    public static final String RULE_PUBLISHER_CATEGORY = "com.motorola.smartactions.intent.category.RULE_PUBLISHER";

    // Intent extra value constants
    public static final String ACTION_COMMAND_LIST      = "list_request";
    public static final String CONDITION_COMMAND_LIST   = "list_request";
    public static final float  LIST_VERSION             = 1.0f;
    public static final String LIST_RESPONSE            = "list_response";
    public static final String LIST_REQUEST_ID          = "1";

    // Intent extras
    public static final String EXTRA_DATA_CLEARED    = "com.motorola.smartactions.intent.extra.DATA_CLEARED";
    public static final String EXTRA_COMMAND         = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_VERSION         = "com.motorola.smartactions.intent.extra.VERSION";
    public static final String EXTRA_REQUEST_ID      = "com.motorola.smartactions.intent.extra.REQUEST_ID";
    public static final String EXTRA_CONFIG_ITEMS    = "com.motorola.smartactions.intent.extra.CONFIG_ITEMS";
    public static final String EXTRA_PUBLISHER_KEY   = "com.motorola.smartactions.intent.extra.PUBLISHER_KEY";
    public static final String EXTRA_EVENT_TYPE      = "com.motorola.smartactions.intent.extra.EVENT";
    public static final String EXTRA_NEW_STATE_TITLE = "com.motorola.smartactions.intent.extra.NEW_STATE_TITLE";
    public static final String EXTRA_PACKAGE_NAME    = "com.motorola.contextual.smartrules.intent.extra.PACKAGE_NAME";
    public static final String EXTRA_PSR_LAUNCH_COMMAND = "com.motorola.contextual.smartrules.intent.extra.PSR_LAUNCH_COMMAND";

    // Publisher related meta data
    public static final String ACTION_PUBLISHER_META_DATA_PUBKEY = "com.motorola.smartactions.publisher_key";
    public static final String ACTION_PUBLISHER_META_DATA_ACTION_TYPE = "com.motorola.smartactions.action_type";
    public static final String ACTION_PUBLISHER_META_DATA_DOWNLOAD_LINK = "com.motorola.smartactions.download_link";
    public static final String ACTION_PUBLISHER_META_DATA_BATTERY_DRAIN = "com.motorola.smartactions.battery_drain";
    public static final String ACTION_PUBLISHER_META_DATA_DATA_USAGE = "com.motorola.smartactions.data_use";
    public static final String ACTION_PUBLISHER_META_DATA_RESPONSE_LATENCY = "com.motorola.smartactions.latency";
    public static final String ACTION_PUBLISHER_META_DATA_NEW_STATE = "com.motorola.smartactions.new_state_mode";
    public static final String ACTION_PUBLISHER_META_DATA_PUBLISHER_VERSION = "com.motorola.smartactions.publisher_version";
    public static final String ACTION_PUBLISHER_META_DATA_INTERFACE_VERSION = "com.motorola.smartactions.interface_version";

    public static final String CONDITION_PUBLISHER_META_DATA_PUBKEY = ACTION_PUBLISHER_META_DATA_PUBKEY;
    public static final String CONDITION_PUBLISHER_META_DATA_DOWNLOAD_LINK = ACTION_PUBLISHER_META_DATA_DOWNLOAD_LINK;
    public static final String CONDITION_PUBLISHER_META_DATA_BATTERY_DRAIN = ACTION_PUBLISHER_META_DATA_BATTERY_DRAIN;
    public static final String CONDITION_PUBLISHER_META_DATA_DATA_USAGE = ACTION_PUBLISHER_META_DATA_DATA_USAGE;
    public static final String CONDITION_PUBLISHER_META_DATA_RESPONSE_LATENCY = ACTION_PUBLISHER_META_DATA_RESPONSE_LATENCY;
    public static final String CONDITION_PUBLISHER_META_DATA_NEW_STATE = ACTION_PUBLISHER_META_DATA_NEW_STATE;
    public static final String CONDITION_PUBLISHER_META_DATA_PUBLISHER_VERSION = "com.motorola.smartactions.publisher_version";
    public static final String CONDITION_PUBLISHER_META_DATA_INTERFACE_VERSION = "com.motorola.smartactions.interface_version";


    public static final String RULE_PUBLISHER_META_DATA_PUBKEY = "com.motorola.smartactions.publisher_key";
    public static final String RULE_PUBLISHER_META_DATA_DOWNLOAD_LINK = ACTION_PUBLISHER_META_DATA_DOWNLOAD_LINK;
    public static final String RULE_PUBLISHER_META_DATA_BATTERY_DRAIN = ACTION_PUBLISHER_META_DATA_BATTERY_DRAIN;
    public static final String RULE_PUBLISHER_META_DATA_DATA_USAGE = ACTION_PUBLISHER_META_DATA_DATA_USAGE;
    public static final String RULE_PUBLISHER_META_DATA_RESPONSE_LATENCY = ACTION_PUBLISHER_META_DATA_RESPONSE_LATENCY;
    public static final String RULE_PUBLISHER_META_DATA_NEW_STATE = ACTION_PUBLISHER_META_DATA_NEW_STATE;
    public static final String RULE_PUBLISHER_META_DATA_PUBLISHER_VERSION = "com.motorola.smartactions.publisher_version";
    public static final String RULE_PUBLISHER_META_DATA_INTERFACE_VERSION = "com.motorola.smartactions.interface_version";

    // To enable list command, make this constant value to true
    public static final boolean ENABLE_LIST_COMMAND = false;

    // Other constants
    public static final String  PSF_APP_PACKAGE     = Constants.PACKAGE;

}
