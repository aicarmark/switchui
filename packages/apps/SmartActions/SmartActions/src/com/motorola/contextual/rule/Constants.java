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

package com.motorola.contextual.rule;

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

    /*************** START: Common ********************/
    public static final boolean PRODUCTION_MODE = com.motorola.contextual.smartrules.Constants.PRODUCTION_MODE;
    public static final boolean LOG_INFO        = true; // should stay true even in production mode
    public static final boolean LOG_DEBUG       = ! PRODUCTION_MODE;
    public static final boolean LOG_VERBOSE     = false;

    public static final String  PUBLISHER_KEY   = "com.motorola.smartactions.publisher.rule";

    // log tag for Rule Publisher - debug convenience tag
    public static final String  RP_TAG      = "RP-";
    public static final String  NEW_LINE    = "\n";
    public static final String EMPTY_STRING = "";
    public static final int INVALID_KEY     = -1;

    public static final String DBG_MSG_RULE_INFERRED   = " Rule Inferred";
    public static final String DBG_RP_REPUBLISH_RULES  = " Republish Rules";
    public static final String DBG_MSG_RULE_PUB_FAILED = " Rule publish failed";
    public static final String DBG_RP_TO_CORE         = "RP to Core";
    public static final String DBG_CORE_TO_RP         = "Core to RP";
    
    public static final String XML_UPGRADE         = com.motorola.contextual.smartrules.Constants.XML_UPGRADE;

    /*************** END: Common ********************/

    /*************** START: Inferencing Constants ********************/
    public static final String IS_STICKY        = "is_sticky";
    public static final String INFERENCE_INTENT = "inference_intent";
    public static final String RECEIVER = "Receiver";
    public static final String OBSERVER = "Observer";

    public static final String INFERENCE_STATE_MEETING_RINGER       = "meeting_ringer_state";
    public static final String INFERENCE_STATE_MEETING_MISSEDCALL   = "meeting_missedcall_state";
    public static final String INFERENCE_STATE_BE_LT_20 = "be_lt_20_state";
    public static final String INFERENCE_STATE_BE_GT_20 = "be_gt_20_state";
    public static final String INFERENCE_BE_LT_20 = "be_lt_20";
    public static final String INFERENCE_BE_GT_20 = "be_gt_20";
    public static final String INFERENCE_NBS_BATT_LEVEL = "nbs_batt_level";

    /* Possible values for PUBLISHED_STATE and INFERRED_STATE columns. */
    public static interface RuleState {

        // Rule state after has been accepted by the Core
        final String PUBLISHED      = "published";

        // Rule is inferred but not yet published (accepted by core)
        final String UNPUBLISHED    = "unpublished";

        // Rule state after a rule has been inferred
        final String INFERRED       = "inferred";
    }

    /*************** END: Inferencing Constants ********************/

    /*************** START: Xml Constants********************/
    public static final String RULES                = "RULES";
    public static final String SMARTRULE            = "SMARTRULE";
    public static final String DELETE_RULES         = "DELETE_RULES";
    public static final String RULEINFO             = "RULEINFO";
    public static final String ACTION               = "ACTION";
    public static final String ACTIONS              = "ACTIONS";
    public static final String CONDITION            = "CONDITION";
    public static final String CONDITIONS           = "CONDITIONS";
    public static final String IDENTIFIER           = "IDENTIFIER";
    public static final String TYPE                 = "TYPE";
    public static final String NAME                 = "NAME";
    public static final String DESCRIPTION          = "DESCRIPTION";
    public static final String XML_VERSION_NODE     = "VERSION";
    public static final String SUGGESTION_FREEFLOW  = "SUGGESTION_FREEFLOW";
    public static final String CONFIG               = "CONFIG";
    public static final String PUB_KEY              = "PUBLISHER_KEY";
    /*************** END: Xml Constants********************/

    /*************** START: Xml Version ********************/
    public static final float  DEFAULT_XML_VERSION = 0.0f;
    public static final String RP_XML_VERSION_SHARED_PREF   = PUBLISHER_KEY + ".XML_VERSION";
    public static final String RP_XML_VERSION               = PUBLISHER_KEY + ".XML_VERSION";
    /*************** END: Xml Version **********************/

    /*************** START: Suggestion XML ********************/
    public static final String SUGGESTION_CONTENT   = "SUGGESTION_CONTENT";
    public static final String SUGGESTION_ICON      = "SUGGESTION_ICON";
    public static final String SUGGESTION_DESC      = "SUGGESTION_DESC";
    public static final String SUG_PROLOGUE         = "PROLOGUE";
    public static final String SUG_BODY             = "BODY";
    public static final String SUG_ICON             = "ICON";
    public static final String SUG_ITEM             = "ITEM";
    public static final String SUG_DESC             = "DESC";
    public static final String BULLET_ITEM          = "BULLET_ITEM";
    public static final String EPILOGUE             = "EPILOGUE";
    /*************** END: Suggestion XML ********************/

    public static final String RULE_KEY_MEETING = "com.motorola.contextual.Meeting.1300451788675";
    public static final String RULE_KEY_SLEEP   = "com.motorola.contextual.Sleep%20Rule.1299861775030";
    public static final String RULE_KEY_LBS     = "com.motorola.contextual.suggested_battery_optimizer_level1";
    public static final String RULE_KEY_NBS     = "com.motorola.contextual.night%20time%20battery%20saver.1300814433701";
    public static final String RULE_KEY_BS      = "com.motorola.contextual.Max%20Battery%20Saver.1300811788675";
    public static final String RULE_KEY_HOME    = "com.motorola.contextual.Home%20Rule.1300077007930";

    public static final String HOME_LOCATION_CONFIG = "Version 1.0;selected_locations_tags Home";

}
