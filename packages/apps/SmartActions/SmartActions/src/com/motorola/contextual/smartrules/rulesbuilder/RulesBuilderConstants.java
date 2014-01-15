/*
 * @(#)RulesBuilderConstants.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18385        2011/04/28  NA				  Initial version
 *   
 *
 */

package com.motorola.contextual.smartrules.rulesbuilder;

/** This is the constants interface for the RulesBuilder class
 *
 *<code><pre>
 * CLASS:
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
 * 	 None.
 *
 * USAGE:
 * 	All constants here should be static, almost or all should be final.
 *
 * </pre></code>
 **/
public interface RulesBuilderConstants {
	
	public static final int DEFAULT_VALUE = -1;

	public static final int CONFIGURE_ACTION_BLOCK = 0;
	public static final int CONFIGURE_CONDITION_BLOCK = CONFIGURE_ACTION_BLOCK + 1;
    public static final int PROCESS_CONDITION_LIST_ITEM = 100;
    public static final int PROCESS_ACTION_LIST_ITEM = 200;
    public static final float DISCONNECT_POS = 50;
    
    public static final String DESCRIPTION_CONFIG_PARM = "DESCRIPTION";
    public static final String DEFAULT_NEW_RULE_NAME = "New Rule";
    public static final String NO_TARGET_STATE = "No target state";
    
    
    public static final String UNNAMED_RULE = "Unnamed Rule";
    public static final String ACTION_DESCRIPTION = "com.motorola.smartactions.description";
    public static final String SAVE_INTENT_CALLBACK = "com.motorola.contextual.SmartActions.EditRuleActivity.Save";


   
    public static final String WIFI_TRIGGER = "com.motorola.contextual.smartprofile.wificonnectionwithaddresssensor";
    public static final String BT_TRIGGER = "com.motorola.contextual.smartprofile.btconnectionwithaddresssensor";
    public static final String MISSED_CALL_TRIGGER = "com.motorola.contextual.smartprofile.missedcallsensor";
    public static final String WIFI_ACTION = "com.motorola.contextual.actions.Wifi"; 
    public static final String AIRPLANE_ACTION = "com.motorola.contextual.actions.Airplane";
    public static final String BT_ACTION = "com.motorola.contextual.actions.Bluetooth";
    public static final String LOCATION_TRIGGER = "com.motorola.contextual.smartprofile.location";
    public static final String GPS_ACTION = "com.motorola.contextual.actions.gps";
    public static final String AUTO_REPLY_ACTION = "com.motorola.contextual.actions.autosms";

    //DEBUG TABLE STRINGS
    public static final String ACTION_ON_CREATE = "Action:OnCreate";
    public static final String ACTION_ON_EDIT = "Action:OnEdit";
    public static final String ACTION_ON_DELETE = "Action:OnDelete";
    public static final String CONDITION_ON_CREATE = "Condition:OnCreate";
    public static final String CONDITION_ON_EDIT = "Condition:OnEdit";
    public static final String CONDITION_ON_DELETE = "Condition:OnDelete";
    public static final String VSM_OUT_MESSAGE = "To VSM";
}
