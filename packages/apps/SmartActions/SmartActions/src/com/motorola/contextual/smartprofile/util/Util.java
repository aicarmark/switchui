/*
 * @(#)Util.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/09/11 NA		      Initial version
 * a18491        2011/2/18  NA                Incorporated first set of
 *                                            review comments
 *
 */
package com.motorola.contextual.smartprofile.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.motorola.contextual.smartprofile.Constants;


/** This class hosts a number of distinct and various utility routines.
 *
 *<code><pre>
 * CLASS:
 * 	implements Constants
 *
 * RESPONSIBILITIES:
 * 	This class is entirely utility routines, all static, nothing instance-based.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  See individual routines.
 *</pre></code>
 */
public class Util implements Constants {

    private static final String TAG = Util.class.getSimpleName();


    /** Constructs XML string from the rules with "String" currentMode, initial, possible values
    *
    * @param currentMode - current mode of the precondition chosen by the user
    * @param pcName - name of the precondition
    * @param pcDesc - description of the precondition
    * @param ruleSet - ruleset for the XML string construction
    * @param initialValue - initial value for the DVS
    * @param possibleValues - possible values for the DVS
    * @return - final XML string
    */
    public static final String generateDvsXmlString(String currentMode, String pcName,
            String pcDesc, String[] ruleSet, String initialValue,
            String[] possibleValues, String persistencyValue) {

        StringBuilder sBuilderPossibleValues = new StringBuilder();

        for(int index = 0; index < possibleValues.length; index++) {
            sBuilderPossibleValues.append(VALUE_START_TAG).append(possibleValues[index]).append(VALUE_END_TAG);
        }

        StringBuilder sRuleSet = new StringBuilder();

        for(int index = 0; index < ruleSet.length; index++) {
            sRuleSet.append(RULE_START_TAG).append(ruleSet[index]).append(RULE_END_TAG);
        }

        StringBuilder sBuilder = new StringBuilder();

        sBuilder.append(VSENSOR_START_TAG)
        .append(NAME_START_TAG)
        .append(pcName)
        .append(currentMode)
        .append(NAME_END_QUOTE_TAG)
        .append(PERSISTENCY_FORVEVER)
        .append(persistencyValue);

        sBuilder.append(VENDOR_TAG)
        .append(VERSION_TAG)
        .append(DESCRIPTION_START_TAG)
        .append(pcDesc)
        .append(DESCRIPTION_END_TAG)
        .append(INITIAL_VALUE_START_TAG)
        .append(initialValue)
        .append(INITIAL_VALUE_END_TAG)
        .append(PERMISSIONS_START_TAG)
        .append(POSSIBLE_VALUE_TRUE)
        .append(PERMISSIONS_ATTRIBUTE_END_TAG)
        .append(PERM_START_TAG)
        .append(PERM_NAME)
        .append(PERM_END_TAG)
        .append(PERMISSIONS_END_TAG)
        .append(POSSIBLE_VALUES_START_TAG)
        .append(sBuilderPossibleValues.toString())
        .append(POSSIBLE_VALUES_END_TAG).append(MECHANISM_TAG)
        .append(RULESET_START_TAG)
        .append(sRuleSet.toString())
        .append(RULESET_END_TAG)
        .append(NAME_END_TAG)
        .append(VSENSOR_END_TAG);

        return sBuilder.toString();
    }

    /** Hides the softkey pad popped open when the edit text area is selected.
     *
     * @param view - view
     * @param context - application context
     */
    public static void hideSoftKeyPad(final View view, final Context context) {
        InputMethodManager manager = (InputMethodManager)
                                     context.getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder binder = view.getApplicationWindowToken();
        if (binder != null) {
            manager.hideSoftInputFromWindow(binder, 0);
        }
    }

    /** replaces the spaces with %20 and remove all non-alphanumeric characters from the
     *  string passed in (except for '.' and '%') for VSM sensor creations.
     *
     *  Example: String like 600 U.S. 45, Libertyville would be returned as
     *  		600%20U.S.%2045%20Libertyville
     *
     * @param originalString - the string with spaces and special characters.
     * @return - string that has spaces replaced with %20 and special characters removed.
     */
    public static String getReplacedString(final String originalString) {

        String replacedString = null;
        if(originalString != null) {
            replacedString = originalString.replaceAll(" ", "%20");
            replacedString = replacedString.replaceAll("[^a-zA-Z0-9.%]", "");
        }

        return replacedString;
    }
    
    /** generates a location based Derived Virtual Sensor String
    *
    * @param poiTagName - tag for the location
    * @param rule - meaningful location sensor rule string
    * @return - a location based DVS string
    */
   public static String generateXMLStringForList(Context context, String configs[], String descriptions[]) {
	   StringWriter writer = new StringWriter();
	   
	   if (configs != null && descriptions != null) {
           int size = configs.length;
           XmlSerializer serializer = Xml.newSerializer();           
           try {
               serializer.setOutput(writer);
               serializer.startDocument("utf-8", null);
               serializer.startTag(null, TAG_CONFIG_ITEMS);
               for (int index = 0; index < size; index++) {
                   serializer.startTag(null, TAG_ITEM);
                   serializer.startTag(null, TAG_CONFIG);
                   serializer.text(configs[index]);
                   serializer.endTag(null, TAG_CONFIG);
                   serializer.startTag(null, TAG_DESCRIPTION);
                   serializer.text(descriptions[index]);
                   serializer.endTag(null, TAG_DESCRIPTION);
                   serializer.endTag(null, TAG_ITEM);
               }
               serializer.endTag(null, TAG_CONFIG_ITEMS);
               serializer.endDocument();
           } catch (Exception exp) {
               exp.printStackTrace();
               return null;
           }
       }
	   return writer.toString();
   }

    /** generates a location based Derived Virtual Sensor String
     *
     * @param poiTagName - tag for the location
     * @param rule - meaningful location sensor rule string
     * @return - a location based DVS string
     */
    public static String generateLocDVSString(final String poiTagName, final String rule) {

        StringBuilder builder = new StringBuilder();

        builder.append(NAME_START_TAG)
        .append(VIRTUAL_SENSOR_STRING)
        .append(poiTagName)
        .append(NAME_END_QUOTE_TAG)
        .append(PERSISTENCY_FORVEVER)
        .append(PERSISTENCY_VALUE_FOREVER)
        .append(VENDOR_TAG)
        .append(VERSION_TAG)
        .append(DESCRIPTION_START_TAG)
        .append(TEST_SENSOR)
        .append(DESCRIPTION_END_TAG)
        .append(INITIAL_VALUE_START_TAG)
        .append(INITIAL_VALUE_FALSE)
        .append(INITIAL_VALUE_END_TAG)
        .append(PERMISSIONS_START_TAG)
        .append(POSSIBLE_VALUE_TRUE)
        .append(PERMISSIONS_ATTRIBUTE_END_TAG)
        .append(PERM_START_TAG)
        .append(PERM_NAME)
        .append(PERM_END_TAG)
        .append(PERMISSIONS_END_TAG)
        .append(POSSIBLE_VALUES_START_TAG)
        .append(VALUE_START_TAG)
        .append(POSSIBLE_VALUE_TRUE)
        .append(VALUE_END_TAG)
        .append(VALUE_START_TAG)
        .append(POSSIBLE_VALUE_FALSE)
        .append(VALUE_END_TAG)
        .append(POSSIBLE_VALUES_END_TAG)
        .append(MECHANISM_TAG_DERIVED)
        .append(DERIVED_START_TAG)
        .append(CLAUSE_VALUE_TAG)
        .append(INITIAL_VALUE_TRUE)
        .append(QUOTE)
        .append(LOGIC_TAG_START)
        .append(rule)
        .append(LOGIC_TAG_END)
        .append(CLAUSE_VALUE_TAG)
        .append(INITIAL_VALUE_FALSE)
        .append(QUOTE)
        .append(LOGIC_TAG_START)
        .append(LOGIC_TAG_END)
        .append(DERIVED_END_TAG)
        .append(NAME_END_TAG);

        return builder.toString();
    }

    /**
     * Checks if length of the string parameter is 0.
     * @param name
     * @returns true if length of the string is 0
     */
    public static boolean isZeroLengthString(String name) {
        boolean status = false;
        if(name != null && name.length() == 0)
            status = true;

        return status;
    }

    /**
     * Replaces special characters from contact names selected
     * @param allNamesBuf
     * @return
     */
    public static final String replaceAllSpecialCharsFromNames(StringBuffer allNamesBuf) {

        String allNames = allNamesBuf.toString();
        allNames = allNames.replaceAll(REM_REGEX_TO_BE_REPLACED, REM_REGEX_TO_REPLACE);
        allNames = allNames.replaceAll(SEM_REGEX_TO_BE_REPLACED, SEM_REGEX_TO_REPLACE);
        allNames = allNames.replaceAll(HASH_REGEX_TO_BE_REPLACED, HASH_REGEX_TO_REPLACE);
        allNames = allNames.replaceAll(COLON_REGEX_TO_BE_REPLACED, COLON_REGEX_TO_REPLACE);
        allNames = allNames.replaceAll(HYPHEN_REGEX_TO_BE_REPLACED, HYPHEN_REGEX_TO_REPLACE);

        if (LOG_INFO) Log.i(TAG, "All Names : " + allNames);
        return allNames;
    }

    /**
     * Replaces special characters from numbers selected
     * @param allNumbersBuf
     * @return
     */
    public static final String replaceAllSpecialCharsFromNumbers(StringBuffer allNumbersBuf) {

        String allNumbers = allNumbersBuf.toString();
        allNumbers = allNumbers.replaceAll(SEM_REGEX_TO_BE_REPLACED, SEM_REGEX_TO_REPLACE);
        allNumbers = allNumbers.replaceAll(HASH_REGEX_TO_BE_REPLACED, HASH_REGEX_TO_REPLACE);
        allNumbers = allNumbers.replaceAll(HYPHEN_REGEX_TO_BE_REPLACED, HYPHEN_REGEX_TO_REPLACE);
        if (LOG_INFO) Log.i(TAG, "All Numbers" + allNumbers);
        return allNumbers;
    }

    /** handler to check if an app is installed to handle the intent action on the phone or not.
     *
     * @param context - context
     * @param intentAction - action that is set in intent when launching an activity/app
     * @return true if app is present to handle the intent else false
     */
    public static boolean isApplicationInstalled(final Context context, final String intentAction) {
        boolean result = true;
        Intent intent = new Intent();
        intent.setAction(intentAction);

        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                                 PackageManager.MATCH_DEFAULT_ONLY);

        if(list.size() > 0) {
            if(LOG_DEBUG) Log.d(TAG, "App to handle intent action "+intentAction+" is installed and list is "+list.toString());
        } else {
            if(LOG_DEBUG) Log.d(TAG, "App to handle intent action "+intentAction+" is not installed");
            result = false;
        }
        return result;
    }

    /**
     * Method to be used while creating description string from a list of names.
     * Duplication of names is not allowed
     * For every name to be added check if it is same as one of the previous entries
     *
     * @param namesList List of names
     * @param index Index of the name in the list. Indexes less than this value are already added
     * @return Name already present in the description or not
     */
    public static boolean isDuplicate(List<String> namesList, int index) {
        if (namesList != null && index < namesList.size()) {
            String currentName = namesList.get(index);
            if (namesList.indexOf(currentName) < index) {
                return true;
            } else {
                int indexOfTrimmedName = namesList.indexOf(currentName.trim());
                if ((indexOfTrimmedName != -1) && (indexOfTrimmedName < index)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /** Returns the list of activity names from package manager
	  * @param context
     * @return List of activity names
     */
   public static final List<String> getActivityNameListFromPackageManager(Context context) {
       
       List<String> list = new ArrayList<String>(); 	
       PackageManager pm = context.getPackageManager();
       try {
           PackageInfo pInfo = pm.getPackageInfo(SMART_RULES_PKG_NAME, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

           
           for (ActivityInfo aInfo : pInfo.activities) {
               if (aInfo.metaData != null) {
                   
                   String pubKey = aInfo.metaData.getString(PUB_KEY);
                   String conditionKey = aInfo.metaData.getString(CONDITION_KEY);
                   
                   if ((pubKey != null) && (pubKey.contains("smartprofile") || (pubKey.equals("com.motorola.contextual.Motion")))){
                   	list.add(aInfo.name + ":" + conditionKey);                           
                   } 
               }
           }
       } catch (NameNotFoundException e) {
           Log.e("InitCommandFactory", "package not found ? this code wouldn't execute if that was the case");
       }
       return list;
   }
    
    /**
     * Method to get activity name from package name
     * @param context
     * @param pubKey
     * @return activity name
     */
	public static final String getPublisherNameFromPublisherKey(Context context, String pubKey) {
	    PackageManager pm = context.getPackageManager();
	    try {
	    	
	        PackageInfo pInfo = pm.getPackageInfo(SMART_RULES_PKG_NAME, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
	        
	        for (ActivityInfo aInfo : pInfo.activities) {
	            if (aInfo.metaData != null) {
	                
	                String tmpPubKey = aInfo.metaData.getString(PUB_KEY);
	                String conditionKey = aInfo.metaData.getString(CONDITION_KEY);
	                
	                if ((tmpPubKey != null) && (tmpPubKey.equals(pubKey))){	                        
	                	if(LOG_INFO) Log.i(TAG, pubKey + " : " + aInfo.name);
	                    return (aInfo.name + ":" + conditionKey);    
	                } 
	            }
	        }
	    } catch (NameNotFoundException e) {
	        Log.e(TAG, "package not found ? this code wouldn't execute if that was the case");
	    }
	    return null;
	}
}
