/*
 * @(#)XmlConstants.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18984        2011/03/28 IKINTNETAPP-153   Initial version
 * A18984        2011/04/10 IKINTNETAPP-171   Added few constants
 *
 */
package com.motorola.contextual.smartrules.rulesimporter;

import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;

/**
 * This is the Constants interface for the XML module.
 * <code><pre>
 * INTERFACE:
 *   This interface represents constants, that are not visible to
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

public interface XmlConstants {

    /**
	 * Interface used to define import type and if
	 * Rule/Action/Conditions/etc
	 * needs to be replaced or updated/added
	 *
	 * NOTE: This should be in sync with the one in
	 *       Rules Updater module
	 *
	 * @author a21693
	 *
	 */
	public static interface ImportType{
	    int IGNORE          = 0;
	    int UPDATE          = 1;
	    int SILENT_UPDATE   = 2;

	    int TEST_IMPORT     = 3;
	    int RESTORE_MYRULES = 4;
	    int SERVER_RULES    = 5;
	    int FACTORY         = 6;
	    int INFERRED        = 8;
	    int INFERRED_ACCEPTED = 9;
	    int RP_RULE           = 10; // Rule published by RP
	}
	// XML Node names
    public static final String SMARTRULE = "SMARTRULE";
    public static final String RULEINFO = "RULEINFO";
    public static final String CONDITIONS = "CONDITIONS";
    public static final String CONDITION = "CONDITION";
    public static final String ACTIONS = "ACTIONS";
    public static final String ACTION = "ACTION";
    public static final String DB_VERSION = "PrereqSmartActionsDbVersion";
    
    /* Older tag is KEY, New tag is IDENTIFIER */
    public static final String KEY = "KEY";
    public static final String IDENTIFIER = "IDENTIFIER";
    
    public static final String ENABLED = "ENABLED";
    public static final String CONDITIONS_LOGIC = "CONDITIONS_LOGIC";
    public static final String MODAL = "MODAL";
    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESC";
    public static final String DESCRIPTION_NEW = "DESCRIPTION";
    
    /* Older Value is SOURCE New value is TYPE */
    public static final String SOURCE = "SOURCE";	
    public static final String TYPE = "TYPE";
    
    public static final String FLAGS= "FLAGS";
    public static final String INFERENCE_LOGIC = "INFERENCE_LOGIC";
    public static final String COMMUNITY_RATING = "COMMUNITY_RATING";
    public static final String COMMUNITY_AUTHOR = "COMMUNITY_AUTHOR";
    public static final String SUGGESTION_FREEFLOW= "SUGGESTION_FREEFLOW";
    public static final String SILENT  = "SILENT";
    public static final String ACTIVE  = "ACTIVE";
    public static final String RULE_TYPE  = "RULE_TYPE";
    
    /* Old tag SYNTAX New tag VSENSOR_LOGIC */
    public static final String SYNTAX  = "SYNTAX";
    @Deprecated
    public static final String VSENSOR_LOGIC  = "VSENSOR_LOGIC";
    
    @Deprecated
    public static final String VSENSOR  = "VSENSOR";
    
    public static final String PUBLISHER_KEY  = "PUBLISHER_KEY";
    public static final String TARGET_STATE  = "TARGET_STATE";
    public static final String CHILD_RULE_KEY = "CHILD_RULE_KEY";
    public static final String ACTIVITY_INTENT  = "ACTIVITY_INTENT";
    public static final String URI_TO_FIRE_ACTION  = "URI_TO_FIRE_ACTION";
    public static final String ICON = "ICON";
    @Deprecated
    public static final String SAMPLE_FKEY_OR_COUNT = "SAMPLE_FKEY_OR_COUNT";
    public static final String ADOPT_COUNT = "ADOPT_COUNT";
    public static final String PARENT_RULE_KEY = "PARENT_RULE_KEY";
    public static final String RATING = "RATING";
    public static final String EXIT_MODE = "EXIT_MODE";
    public static final String SUGGESTED_REASON = "SUGGESTED_REASON";
    public static final String RANK = "RANK";
    public static final String SUGGESTED_STATE = "SUGGESTED_STATE";
    
    //Older tag for different types of suggestions
    public static final String LIFECYCLE = "LIFECYCLE";	
    //New tag for different types of suggestions
    public static final String SUGGESTION_TYPE = "SUGGESTION_TYPE";

    public static final String LAST_ACTIVE_DATETIME = "LAST_ACTIVE_DATETIME";
    public static final String LAST_INACTIVE_DATETIME = "LAST_INACTIVE_DATETIME";
    public static final String CREATED_DATETIME = "CREATED_DATETIME";
    public static final String LAST_EDITED_DATE_TIME = "LAST_EDITED_DATE_TIME";
    public static final String CONDITION_MET = "CONDITION_MET";
    public static final String LAST_FAIL_DATETIME = "LAST_FAIL_DATETIME";
    public static final String FAILURE_MESSAGE = "FAILURE_MESSAGE";
    public static final String SWAP = "SWAP";
    public static final String LAST_FIRED_DATETIME = "LAST_FIRED_DATETIME";
    public static final String CONFLICT_WINNER = "CONFLICT_WINNER";
    public static final String SUGGESTION_ICON = "SUGGESTION_ICON";
    public static final String SUGGESTION_DESC = "SUGGESTION_DESC";
    public static final String SHORT = "SHORT";
    public static final String LONG = "LONG";
    public static final String EPILOGUE = "EPILOGUE";
    public static final String PROLOGUE = "PROLOGUE";
    public static final String BODY = "BODY";
    public static final String ITEM = "ITEM";
    public static final String BULLET_ITEM = "BULLET_ITEM";
    public static final String DESC = "DESC";
    public static final String SUGGESTION_CONTENT = "SUGGESTION_CONTENT";
    public static final String CONFIG = "CONFIG";
    public static final String VALIDITY = "VALIDITY";
    
    /* Old tag MARKET_URL New tag DOWNLOAD_URL */
    public static final String MARKET_URL = "MARKET_URL";
    public static final String DOWNLOAD_URL = "DOWNLOAD_URL";
    
    /* Older tag UI_INTENT, New tag SUGGESTION_UI_INTENT */
    public static final String UI_INTENT = "UI_INTENT";
    public static final String SUGGESTION_UI_INTENT = "SUGGESTION_UI_INTENT";
    
    public static final String S_SUGGESTION_ICON = "<SUGGESTION_ICON>";
    public static final String S_SUGGESTION_DESC = "<SUGGESTION_DESC>";
    public static final String S_EPILOGUE = "<EPILOGUE>";
    public static final String S_PROLOGUE = "<PROLOGUE>";
    public static final String S_BODY = "<BODY>";
    public static final String S_ITEM = "<ITEM>";
    public static final String S_BULLET_ITEM = "<BULLET_ITEM>";
    public static final String S_ICON = "<ICON>";
    public static final String S_DESC = "<DESC>";
    public static final String S_SUGGESTION_CONTENT = "<SUGGESTION_CONTENT>";
    
    public static final String E_SUGGESTION_ICON = "</SUGGESTION_ICON>";
    public static final String E_SUGGESTION_DESC = "</SUGGESTION_DESC>";
    public static final String E_EPILOGUE = "</EPILOGUE>";
    public static final String E_PROLOGUE = "</PROLOGUE>";
    public static final String E_BODY = "</BODY>";
    public static final String E_ITEM = "</ITEM>";
    public static final String E_BULLET_ITEM = "</BULLET_ITEM>";
    public static final String E_ICON = "</ICON>";
    public static final String E_DESC = "</DESC>";
    public static final String E_SUGGESTION_CONTENT = "</SUGGESTION_CONTENT>";

    // Default values for db columns which are not of type String
    public static final long DEFAULT_ID = -1;
    public static final long DEFAULT_FK = -1;

    public static final float DEFAULT_RANK = 0;
    public static final int DEFAULT_ENABLED = RuleTable.Enabled.DISABLED;
    public static final int DEFAULT_ACTIVE = RuleTable.Active.INACTIVE;
    public static final int DEFAULT_RULE_TYPE = RuleTable.RuleType.AUTOMATIC;
    public static final int DEFAULT_SOURCE = RuleTable.Source.SUGGESTED;
    public static final int DEFAULT_SILENT = RuleTable.Silent.TELL_USER;
    public static final int DEFAULT_COMMUNITY_RATING = 0;
    public static final int DEFAULT_SUGGESTED_STATE = RuleTable.SuggState.ACCEPTED;
    public static final int DEFAULT_LIFECYCLE = RuleTable.Lifecycle.NEVER_EXPIRES;
    public static final long DEFAULT_TIME = 0;
    public static final String DEFAULT_VALIDITY = TableBase.Validity.VALID;

    public static final int  DEFAULT_CONDITION_MET = ConditionTable.CondMet.COND_NOT_MET;
    // default should be enabled, so that the condition blocks stick to the pipe
    public static final int  DEFAULT_CONDITION_ENABLED = ConditionTable.Enabled.ENABLED;

    public static final int DEFAULT_MODAL = -1; //unknown

    public static final int DEFAULT_EXIT_MODE = ActionTable.OnModeExit.ON_ENTER;
    public static final int DEFAULT_CONFLICT_WINNER = ActionTable.ConflictWinner.LOSER;
    // default should be enabled, so that the action blocks stick to the pipe
    public static final int DEFAULT_ACTION_ENABLED = ActionTable.Enabled.ENABLED;
    
    // XML Parse errors to be send to RP as part of the response
    public static final String XML_BLACKLISTED_RULE = "failure:blacklisted_rule";
    public static final String XML_MAND_TAGS_MISSING = "failure:tags_missing";
    public static final String XML_FORMAT_ERROR = "failure:xml_format_error";
    public static final String XML_INVALID_PARAM = "failure:invalid_parameter";
    public static final String XML_INSERT_SUCCESS ="success";

}
