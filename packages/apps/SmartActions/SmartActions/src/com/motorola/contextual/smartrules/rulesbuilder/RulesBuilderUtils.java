/*
 * @(#)RulesBuilderUtils.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18385        2011/06/22 NA				  Initial version
 * 
 */
package com.motorola.contextual.smartrules.rulesbuilder;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.uipublisher.Publisher;

/** This class contains the utilities used by the RulesBuilder module.
 *
 * CLASS:
 * Implements Constants to reuse constants.
 *
 * RESPONSIBILITIES:
 * 	Writes debug info to DebugTable
 *  Queries package manager for action and condition activities
 *  XML parse operations
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 * 	see methods for usage instructions
 *
 *</pre></code>
 */
public class RulesBuilderUtils implements  Constants, RulesBuilderConstants {

    public static final String TAG = RulesBuilderUtils.class.getSimpleName();

    
   /** Writes debug data to debug table
    * 
    * @param context   - Context
    * @param direction - IN/OUT/INTERNAL
    * @param fromTo    - From which module/ to which module
    * @param ruleName  - Rule name
    * @param ruleKey   - Rule key
    * @param targetState - target state
    * @param data1 - debug data
    * @param data2 - debug data
    */
    public static void writeInfoToDebugTable(Context context, String direction, String fromTo,
    										 String ruleName, String ruleKey,
    										 String targetState, String data1, String data2){
    	
       DebugTable.writeToDebugViewer(context, direction, targetState,
       		 ruleName, ruleKey, fromTo, data1, data2,
       		Constants.PACKAGE, Constants.PACKAGE);
    }
    


    /** Parses the string into a Document.
    *
    * @param ruleXml - string form of XML read from the rules file
    * @return - Document tree.
    * @see Document#
    */
    public static Document getParsedDoc(final String ruleXml) {

    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	DocumentBuilder db = null;
    	try {
    		db = dbf.newDocumentBuilder();
    	} catch (ParserConfigurationException e) {
    		e.printStackTrace();
    	}
	    Document doc = null;
	    try {
	    	if (db != null)
	    		doc = db.parse(new InputSource( new StringReader( ruleXml )));
	    } catch (SAXException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    return doc;
  }
  
    /**Checks if any error needs to be displayed on the block. For now we have a requirement
	  * for Location only
	  * 
	  * @param blockInfo
	  * @return Error message to be displayed
	  */
    public static String checkForErrorStatus(Context context, Publisher blockInfo){
    	String errorMsg = EMPTY_STRING; 
		StringBuffer buf = new StringBuffer();
		buf.append(errorMsg);
		if (blockInfo.getPublisherKey().equals(LOCATION_TRIGGER)){
			int locationStatus = LocationConsent.locationFuncAvailable(context);
			if ((locationStatus & MASK_ADA) == MASK_ADA) buf.append(context.getString(R.string.no_ada_accepted_key) + SEMICOLON);
			if ((locationStatus & MASK_LOCPROVIDER) == MASK_LOCPROVIDER) buf.append(context.getString(R.string.no_nw_provider) + SEMICOLON);
			if ((locationStatus & MASK_WIFISLEEP) == MASK_WIFISLEEP) buf.append(context.getString(R.string.wifi_sleep) + SEMICOLON);
		}
		errorMsg = buf.toString();
	   //Remove the semicolon at the end
	   if (errorMsg.endsWith(SEMICOLON)){
		   int position = errorMsg.lastIndexOf(SEMICOLON);
		   errorMsg = errorMsg.substring(0, position);
	   }
	   return errorMsg;
    }

	/** Logs the action and condition blocks that have been created, based on the Package manager query
	 * 
	 * @param actionList - List of Action Blocks in the system
	 * @param conditionList - List of condition blocks in the system
	 */
    public static void logActionAndConditionBlockList(Publisher[] actionList, Publisher[] conditionList)
    {
    	for (int i = 0; i < actionList.length; i++) {
    		if (LOG_DEBUG) Log.d(TAG,"Action Block List: " + actionList[i].toString());
    	}
        for (int i = 0; i < conditionList.length; i++) {
        	if (LOG_DEBUG) Log.d(TAG,"Condition Block List: " + conditionList[i].toString());
        }
    }

    /** Makes an intent with the package and component name
	 * 
	 * @param pkg - package name
	 * @param componentName - component name
	 * @return intent
	 */
    public static Intent activityIntent(String pkg, String componentName) {
       Intent result = new Intent(ACTION_GET_CONFIG);
       result.setClassName(pkg, componentName);
       return result;
    }

}
