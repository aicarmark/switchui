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
package com.motorola.contextual.smartrules.drivingmode;


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

    
	//This is the sample rule key and must match the rule key in rulesimporter.rules
	public static final String DRIVEMODE_RULE_KEY = "com.motorola.contextual.Car%20Rule.1299861367137";
    
	public static final String DRIVEMODE_RULEINFO_PREFIX = "<RULES>" +
			    "<SMARTRULE>" +
				"<RULEINFO>" +
				"<NAME>l10nStart_nameDriveMode_l10nEnd</NAME>" +
				"<LIFECYCLE>0</LIFECYCLE>" +
				"<SUGGESTED_STATE>0</SUGGESTED_STATE>" +
				"<INFERENCE_LOGIC><![CDATA[null]]></INFERENCE_LOGIC>" +
				"<FLAGS>null</FLAGS>" +
				"<ICON>ic_driving_w</ICON>" +
				"<SOURCE>4</SOURCE>" +
				"<PARENT_RULE_KEY>"+DRIVEMODE_RULE_KEY+"</PARENT_RULE_KEY>" +
				"<DESC>l10nStart_DescDriveMode_l10nEnd</DESC>" +
				"<ACTIVE>0</ACTIVE>";
	    
    public static final String DRIVEMODE_RULEINFO_SUFFIX = "</RULEINFO>";

    public static final String DRIVEMODE_RULETYPE_AUTO = "<RULE_TYPE>0</RULE_TYPE>" + 
	    		"<ENABLED>1</ENABLED>";
	    
    public static final String DRIVEMODE_RULETYPE_MANUAL = "<RULE_TYPE>1</RULE_TYPE> +" +
	    		"<ENABLED>0</ENABLED>";
							
	    public static final String DRIVEMODE_KEY_PREFIX = "<KEY>";
	    public static final String DRIVEMODE_KEY_SUFFIX = "</KEY>";
	    public static final String DRIVEMODE_SYNTAX_PREFIX = "<SYNTAX>";
	    public static final String DRIVEMODE_SYNTAX_SUFFIX = "</SYNTAX>";
	    public static final String DRIVEMODE_DESC_PREFIX = "<DESC>";
	    public static final String DRIVEMODE_DESC_SUFFIX = "</DESC>";
	    public static final String DRIVEMODE_ACTIVITY_INTENT_PREFIX = "<ACTIVITY_INTENT>";
	    public static final String DRIVEMODE_ACTIVITY_INTENT_SUFFIX = "</ACTIVITY_INTENT>";
	    public static final String DRIVEMODE_CONFIG_PREFIX = "<CONFIG>";
	    public static final String DRIVEMODE_CONFIG_SUFFIX = "</CONFIG>";
	    public static final String DRIVEMODE_DOCK_PUB_KEY = "com.motorola.contextual.smartprofile.dock";

	    public static final String DRIVEMODE_DOCK_TRIGGER = "<CONDITIONS>" +
					"<CONDITION>" +
					"<ENABLED>true</ENABLED>" +
					"<CONFIG>Dock=CarDock;Version=1.0</CONFIG>" +
					"<PUBLISHER_KEY>" + DRIVEMODE_DOCK_PUB_KEY + "</PUBLISHER_KEY>" +
					"</CONDITION>" +
					"</CONDITIONS>";
	    
	    public static final String DRIVEMODE_BT_TRIGGER_PREFIX = "<CONDITIONS><CONDITION>" +
	    			"<NAME>l10nStart_nameBT_l10nEnd</NAME>" +
				    "<ENABLED>true</ENABLED>" +
	    			"<PUBLISHER_KEY>com.motorola.contextual.smartprofile.btconnectionwithaddresssensor</PUBLISHER_KEY>";
	    
	    public static final String DRIVEMODE_BT_TRIGGER_SUFFIX = "</CONDITION></CONDITIONS>";
	    
	    public static final String DRIVEMODE_ACTIONS = "<ACTIONS>" +
    	            "<ACTION>" +
                    "<ENABLED>true</ENABLED>" +
                    "<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;S.app_uri=%23Intent%3Baction%3Dcom.motorola.smartcardock.LAUNCHER%3Bcategory%3Dandroid.intent.category.DEFAULT%3Bend;S.com.motorola.intent.extra.ACTION_KEY=com.motorola.contextual.actions.launchapp;end</CONFIG>" +
                    "<PUBLISHER_KEY>com.motorola.contextual.actions.launchapp</PUBLISHER_KEY>" +
                    "<ACTIVE>0</ACTIVE>" +
                    "</ACTION>" +
                
	                "<ACTION>" +
                    "<NAME>l10nStart_nameGPS_l10nEnd</NAME>" +
                    "<ENABLED>true</ENABLED>" +
                    "<PUBLISHER_KEY>com.motorola.contextual.actions.Gps</PUBLISHER_KEY>" +
                    "<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;B.state=true;S.com.motorola.intent.extra.ACTION_KEY=com.motorola.contextual.actions.Gps;S.com.motorola.intent.extra.SETTING_STRING=On;i.com.motorola.contextual.smartrules.rulesbuilder.ActionPosition=10;S.com.motorola.intent.extra.ACTION_STRING=GPS;end</CONFIG>" +
                    "</ACTION>" +
                
					"<ACTION>" +
					"<ENABLED>true</ENABLED>" +
					"<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;i.RINGER_VOLUME=12;i.RINGER_MODE=2;i.MAX_RINGER_VOLUME=15;S.MODE_STRING=Normal;B.VIB_CHECK_STATUS=false;end</CONFIG>" +
					"<PUBLISHER_KEY>com.motorola.contextual.actions.Ringer</PUBLISHER_KEY>" +
					"</ACTION>" +
					
					"<ACTION>" +
					"<ENABLED>true</ENABLED>" +
					"<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;S.KNOWN_FLAG=;S.sms_text=Driving%2C%20I'll%20get%20back%20to%20you%20when%20I%20can%20safely%20reply%3A%20Sent%20by%20Smart%20Actions;S.internal_name=690790;i.respond_to_flag=0;S.numbers=*all*;end</CONFIG>" +
			        "<PUBLISHER_KEY>com.motorola.contextual.actions.autosms</PUBLISHER_KEY>" +
					"</ACTION>" +
			        
                    "<ACTION>" +
                    "<NAME>l10nStart_nameVoiceAnnounce_l10nEnd</NAME>" +
                    "<ENABLED>true</ENABLED>" +
                    "<PUBLISHER_KEY>com.motorola.contextual.actions.SetVoiceAnnounce</PUBLISHER_KEY>" +
                    "<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;B.Voice_Announce_Read_Caller=true;B.Voice_Announce_Read_Text=false;end</CONFIG>" +
                    "</ACTION>" +
                    
					"</ACTIONS>" +
					
					"</SMARTRULE>" +
				"</RULES>" ;
	    
	    public static final String DRIVEMODE_ACTIONS_DOCK = "<ACTIONS>" +
	            "<ACTION>" +
                "<NAME>l10nStart_nameGPS_l10nEnd</NAME>" +
                "<ENABLED>true</ENABLED>" +
                "<PUBLISHER_KEY>com.motorola.contextual.actions.Gps</PUBLISHER_KEY>" +
                "<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;B.state=true;S.com.motorola.intent.extra.ACTION_KEY=com.motorola.contextual.actions.Gps;S.com.motorola.intent.extra.SETTING_STRING=On;i.com.motorola.contextual.smartrules.rulesbuilder.ActionPosition=10;S.com.motorola.intent.extra.ACTION_STRING=GPS;end</CONFIG>" +
                "</ACTION>" +
                
	            "<ACTION>" +
				"<ENABLED>true</ENABLED>" +
				"<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;i.RINGER_VOLUME=0;i.RINGER_MODE=1;i.MAX_RINGER_VOLUME=15;S.MODE_STRING=Vibrate;B.VIB_CHECK_STATUS=true;end</CONFIG>" +
				"<PUBLISHER_KEY>com.motorola.contextual.actions.Ringer</PUBLISHER_KEY>" +
				"</ACTION>" +

				"<ACTION>" +
				"<ENABLED>true</ENABLED>" +
				"<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;S.KNOWN_FLAG=;S.sms_text=Driving%2C%20I'll%20get%20back%20to%20you%20when%20I%20can%20safely%20reply%3A%20Sent%20by%20Smart%20Actions;S.internal_name=690790;i.respond_to_flag=0;S.numbers=*all*;end</CONFIG>" +
		        "<PUBLISHER_KEY>com.motorola.contextual.actions.autosms</PUBLISHER_KEY>" +
				"</ACTION>" +
				
                "<ACTION>" +
                "<NAME>l10nStart_nameVoiceAnnounce_l10nEnd</NAME>" +
                "<ENABLED>true</ENABLED>" +
                "<PUBLISHER_KEY>com.motorola.contextual.actions.SetVoiceAnnounce</PUBLISHER_KEY>" +
                "<CONFIG>#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;B.Voice_Announce_Read_Caller=true;B.Voice_Announce_Read_Text=false;end</CONFIG>" +
                "</ACTION>" +

				"</ACTIONS>" +
				
				"</SMARTRULE>" +
			"</RULES>" ;	    	   
}