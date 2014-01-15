/*
 * @(#)RulesImporter.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18984        2011/03/28 IKINTNETAPP-153   Initial version
 * A18172        2011/04/06 IKINTNETAPP-157   Rules Importer to send an intent
 * 					  com.motorola.contextual.RULES_IMPORTED
 *                                            once the rules are imported with
 *                                            source & key information.,
 * A18172,       2011/04/10 IKINTNETAPP-171   Added few functionalities
 * A18984
 *
 * a21034        2011/06/08                   Changes in RTI import logic
 *
 */
package com.motorola.contextual.smartrules.rulesimporter;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.actions.BackgroundData;
import com.motorola.contextual.actions.Sync;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.ActionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.Source;
import com.motorola.contextual.smartrules.db.table.RuleTuple;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.util.RuleFilterList;
import com.motorola.contextual.smartrules.util.Util;

/** This class handles the parsing of Smart Rules stored in xml format. The parsed
 * rule data is written to Smart Rules DB.
 *
 *<code><pre>
 * CLASS:
 *
 * RESPONSIBILITIES:
 *  a. Parses the xml from either
 *      1) a specific location in the file system
 *      2) or passed in as a String
 *  b. Writes the parsed rule data to Smart Rules DB
 *
 * COLLABORATORS:
 *  Smart Rules / Inference Manager.
 *
 * USAGE:
 *  see each method
 *
 *</pre></code>
 */

public class RulesImporter implements Constants, DbSyntax, XmlConstants {

    private static final String TAG = RulesImporter.class.getSimpleName();
    // Array List to store the SOURCE information of rules which are
    // inserted successfully
    private ArrayList<String> mRuleSourceInfo = new ArrayList<String>();
    // Array List to store the KEY information of rules which are
    // inserted successfully
    private ArrayList<String> mRuleKeyInfo = new ArrayList<String>();

    private ArrayList<String> mRuleStatus = new ArrayList<String>();

    private Map<String, Rule> mToBeImportedRulesMap = new HashMap<String, Rule>();
    private Set<String> mKeySet = new HashSet<String>();

    // maintain Parent rule key for all inserted or updated rules
    private ArrayList<String> mParentRuleKeyInfoAll = new ArrayList<String>();

    // Whether the respective rule key is a silent update or not
    private ArrayList<String> mRuleIsSilentUpdate = new ArrayList<String>();

    // maintain Child rule key for all inserted or updated rules
    private ArrayList<String> mChildRuleKeyInfoAll = new ArrayList<String>();
    
    // maintain Parent & Child rule key for all inserted or updated rules
    private ArrayList<String> mParentChildRuleKeyInfoAll = new ArrayList<String>();
    
    // Whether the respective rule key is a silent update or not
    private ArrayList<String> mParentChildKeyIsSilentUpdate = new ArrayList<String>();
    
    private HashMap<String, String> mRulePublisherResponseMap = new HashMap<String, String>();

    // Invoke Condition Builder only if there is No Rules to Infer
    boolean mRTIPresent = false;

    boolean mRuleUpdated = false;

    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_SMS_TEXT = "sms_text";
   
    private Context mContext = null;
    private String mRequestId = null;
    private String mParentRuleKey = null;
    private String mPubKey = null;
    private static int mXmlDbVersion = SQLiteManager.INVALID_DATABASE_VERSION;
    
    
    /** This class holds the XML tag <SMARTRULE> 
     *<code><pre>
     * CLASS:
     *
     * RESPONSIBILITIES:
     *  It holds holds the XML tag <SMARTRULE
     *
     * COLLABORATORS:
     *  Smart Rules 
     *
     * USAGE:
     *  see each method
     *
     *</pre></code>
     */
    private static class RuleXmlTag {
        private static String getStartTag() {
            return  XmlTag.B + SMARTRULE + XmlTag.E;
        }
    }

    // What launched the RulesImporter?
    private static int mImportType = ImportType.IGNORE;

    // Constructor
    RulesImporter(Context context, int importType, String requestID, String pubKey, String parentKey) {
        mContext = context;
        mImportType = importType;
        mRequestId = requestID;
        mParentRuleKey = parentKey;
        mPubKey = pubKey;

    }
   
    /**
     * Entry method for RulesImporter. Rules are imported depending on
     * the ImportType
     *
     * @param xmlString - Rules Xml
     */
    public void startRulesImporter(String xmlString){

        if(LOG_INFO) Log.i(TAG, "Starting RI with importType=" + mImportType + "; mRequestId=" + mRequestId);

        switch(mImportType){

        case ImportType.TEST_IMPORT:{
            // For Automation Usecase
            if(LOG_DEBUG) Log.d(TAG,"Test Import New Test");
            
            // Parse the xml from the sdcard
            String testXml = FileUtil.readRulesImporterFile(mContext, true);

            // and write the data into Smart Rules Database
            parseXmlAndInsertRules(testXml);
            break;
        }

        case ImportType.FACTORY:{

             if(LOG_DEBUG) Log.d(TAG,"Importing Factory rules");

             // Parse the xml from the asset directory
             String factoryXml = FileUtil.readRulesImporterFile(mContext, false);
             // and write the data into Smart Rules Database
             parseXmlAndInsertRules(factoryXml);
             break;
        }

        case ImportType.RESTORE_MYRULES:
        case ImportType.SERVER_RULES:{

            if(LOG_DEBUG) Log.d(TAG,"Importing for : "+mImportType);

            String updaterXml = null;
            
            if (mImportType == ImportType.RESTORE_MYRULES){
            	updaterXml = FileUtil.readFromInternalStorage(mContext, 
            										MYRULES_RESTORED_FILENAME);
            }else{
            	updaterXml = readXmlFromUpdaterSharedPref();
            }

            if( ! Util.isNull(updaterXml)){
            	
            	boolean importRule = true;
            	
                if (mImportType == ImportType.RESTORE_MYRULES){
                	importRule = FileUtil.isXmlDBVersionCompatible(updaterXml);
                }
                
                if (importRule){
	                // Extract and return SmartRules, send intents for other blocks
	                String xml = FileUtil.processXmlAndSendIntents(mContext, updaterXml);
	                // and write the data into Smart Rules Database
	                parseXmlAndInsertRules(xml);
                }
            }
            
            break;
        }

        case ImportType.RP_RULE:
        case ImportType.INFERRED : {
            if(xmlString !=null){

                // Extract and return SmartRules, send intents for other blocks
                String inferredXml = FileUtil.processXmlAndSendIntents(mContext, xmlString);
                // and write the data into Smart Rules Database
                parseXmlAndInsertRules(inferredXml);
            }
            break;
        }
        
        case ImportType.INFERRED_ACCEPTED:{
        	if (LOG_DEBUG) Log.d(TAG, "Importing accepted rule");
        	 if(xmlString !=null){
                // Extract and return SmartRules, send intents for other blocks
                String xml = FileUtil.processXmlAndSendIntents(mContext, xmlString);
                // and write the data into Smart Rules Database
                parseXmlAndInsertRules(xml);
            }
        	 break;
        }
        default:
            Log.e(TAG,"Unknown State: Rules Import Aborted. importType=" +
                     mImportType + " Xml= " + xmlString);
        }
    }

    /**
     * Reads XML from the shared pref of Updater module
     *
     * @return - Xml String
     */
    private String readXmlFromUpdaterSharedPref(){

        String updaterXmlString = null;
        
        if(LOG_DEBUG) Log.d(TAG,"Reading Xml from RulesUpdater Shared pref");
        
        try {
        	// Get the context of BlurRulesUpdater package
            Context ruContext = mContext.createPackageContext(SMARTACTIONFW_PACKAGE,
                                     							Context.CONTEXT_RESTRICTED);
            // Read SharedPreferences stored in BlurRulesUpdater using its context
            SharedPreferences prefs = ruContext.getSharedPreferences(NEWRULES_SHARED_PREFERENCE, 
            															Context.MODE_WORLD_READABLE);
	        updaterXmlString = prefs.getString(XML_CONTENT, null);
	        
	        if(LOG_DEBUG) Log.e(TAG,"updater xmlString is "
						+(updaterXmlString==null ? "null" : updaterXmlString));

        } catch (NameNotFoundException e) {
            Log.e(TAG,"readXmlFromUpdaterSharedPref: NameNotFoundException");
            e.printStackTrace();
        }

        return updaterXmlString;
    }

    /** This method parses the passed in XML string and writes
     *  the parsed data into the Smart Rules DB.
     *
     * @param xmlString - contains list of rules in XML format
     */
    private void parseXmlAndInsertRules(final String xmlString) {

        if (xmlString == null || xmlString.length() == 0){
            Log.e(TAG, "Aborting Import - Empty XML file");
        	mRulePublisherResponseMap.put(mPubKey, XML_FORMAT_ERROR);
        	sendResponseToRulePublisher();
        	mRulePublisherResponseMap.clear();
            return;
        }

        // parse XML and populate Rule Array
        parseXmlString(xmlString);

        // Insert the rules in DB
        insertOrUpdateRulesInDb();

        // Lets clean the db in the end, after .rules have been read/inserted
        if(mImportType == ImportType.FACTORY){
            new RulesDeleter(mContext).startRulesCleaner(null);

        }

        // send out the broadcast
        sendInsertionBroadcast();

        // reset all
        mRuleKeyInfo.clear();
        mParentRuleKeyInfoAll.clear();
        mChildRuleKeyInfoAll.clear();
        mParentChildRuleKeyInfoAll.clear();
        mParentChildKeyIsSilentUpdate.clear();
        mRuleSourceInfo.clear();
        mRuleStatus.clear();
        mRuleIsSilentUpdate.clear();
        mRuleUpdated = false;
        mRTIPresent = false;
        mToBeImportedRulesMap.clear();
        mRulePublisherResponseMap.clear();;
        mKeySet.clear();
        if(LOG_INFO) Log.i(TAG, "---------Finished Import Type= " + mImportType + "--------");
    }

    /**
     * Parse XML and populate Rule Array
     *
     * @param s - contains list of rules in XML format
     */
    private void parseXmlString(String s){

        String ruleContainerStartTag = RuleXmlTag.getStartTag();

        // Separate each rule
        String[] ruleBlockArray = s.split(ruleContainerStartTag);

        if (ruleBlockArray == null) {
            Log.e(TAG, "ruleArray is null");
            mRulePublisherResponseMap.put(mPubKey, XML_FORMAT_ERROR);
            return;
        }

        if (LOG_DEBUG)  Log.d(TAG, "Number of rules " + ruleBlockArray.length);

        boolean logOnlyOnce = true;

        for (String ruleBlock : ruleBlockArray) {

            if ( ruleBlock.length() > 0) {
                if (LOG_DEBUG) Log.d(TAG, "Rule is " + ruleBlock);
                // put back the rule start tag removed by split and
                // make the parse request
                Document ruleDoc = FileUtil.getParsedDoc(ruleContainerStartTag + ruleBlock);

                if (ruleDoc == null) {
                    Log.e(TAG, "ruleDoc is null, skip to the next rule");
                    mRulePublisherResponseMap.put(mPubKey, XML_FORMAT_ERROR);
                } else {
                    Rule rule = new Rule(ruleDoc.getFirstChild());

                    if(!rule.isValidRule()) continue;

                    // Check if the rule is compatible with the current DB
                    mXmlDbVersion = rule.mRuleInfo.getIntValue(DB_VERSION, SQLiteManager.INVALID_DATABASE_VERSION);
                    if(logOnlyOnce) {
                    	if (LOG_INFO) Log.i(TAG,"xmlDbVersion "+mXmlDbVersion + 
            			"SQLite DB Version" + SQLiteManager.DATABASE_VERSION); 
                        logOnlyOnce = false;
                    }
                    if(mXmlDbVersion > SQLiteManager.DATABASE_VERSION)
                        continue; // skip this rule, go to next
                    
                    switch(rule.mRuleInfo.getRuleSource()){
                        case RuleTable.Source.USER:
                        case RuleTable.Source.FACTORY:
                        case RuleTable.Source.CHILD:
                        case RuleTable.Source.SUGGESTED:
                        case RuleTable.Source.INFERRED:
                        	
                            mToBeImportedRulesMap.put(rule.mRuleInfo.getRuleKey(), rule);
                            mKeySet.add(rule.mRuleInfo.getRuleKey());
                            break;

                        default:{
                            Log.e(TAG, "Ignoring unsupported Source=" + rule.mRuleInfo.getRuleSource() +
                                    " Key=" + rule.mRuleInfo.getRuleKey());
                            mRulePublisherResponseMap.put(rule.mRuleInfo.getRuleKey(), XML_INVALID_PARAM);
                        }
                    }
                }
            }else{
            	// This not an error case hence don't add 
            	// XML format error
                Log.e(TAG, "No rule blocks !!");
            }
        }
    }

    /**
     * Send out the broadcast
     *
     * @param isMyRules
     */
    private void sendInsertionBroadcast(){

        if((mRuleKeyInfo != null) &&
                (mRuleSourceInfo != null) &&
                (mRuleKeyInfo.size() == mRuleSourceInfo.size()) &&
                (mRuleKeyInfo.size() > 0) &&
                (mRuleSourceInfo.size() > 0)) {

		if (mImportType == ImportType.INFERRED_ACCEPTED){
        		//This intent is required by the widgets listening to Smart Rules
            	Intent intent = new Intent(INFERRED_RULES_ADDED);
                com.motorola.contextual.smartrules.db.business.Rule addedRule = RulePersistence.fetchRuleOnly(mContext, mRuleKeyInfo.get(0));
                if (addedRule != null){
                	Long ruleId = addedRule.get_id();
                	String ruleIcon = addedRule.getIcon();

            		// Following two lines are duplicates and can be removed once 
            		// Widget changes to for the extras are in
            		intent.putExtra(RULE_KEY, mRuleKeyInfo.get(0));
            		intent.putExtra(PUZZLE_BUILDER_RULE_ID, ruleId);
            	
            		intent.putExtra(RuleTable.Columns.KEY, mRuleKeyInfo.get(0));
            		intent.putExtra(RuleTable.Columns._ID, ruleId);
            		intent.putExtra(RuleTable.Columns.ICON, ruleIcon);
            		intent.putExtra(RULE_KEY, mRuleKeyInfo.get(0));
            		intent.putExtra(PUZZLE_BUILDER_RULE_ID, ruleId);
            		intent.putExtra(EXTRA_RESPONSE_ID, Integer.parseInt(mRequestId));
            		if (LOG_DEBUG) Log.d(TAG,"Widget Rule Added; sending broadcast " + intent.toUri(0));
            		mContext.sendBroadcast(intent);
                }else{
			Log.e(TAG,"Widget Rule Added; broadcast not send");
                }
            } else if (mImportType != ImportType.RESTORE_MYRULES){

                if (LOG_DEBUG) Log.d(TAG,"No: of Rules being sent in the broadcast :"
                        + mRuleKeyInfo.size() + "," + mRuleSourceInfo.size());

                //Send intent with the KEY & SOURCE information of rules
                // which are inserted successfully
                HashMap<String, ArrayList<String>> ruleKeySourceMap =
                    new HashMap<String, ArrayList<String>>();

                ruleKeySourceMap.put(KEY_RULE_KEY, mRuleKeyInfo);
                ruleKeySourceMap.put(KEY_RULE_SOURCE, mRuleSourceInfo);
                ruleKeySourceMap.put(KEY_RULE_STATUS, mRuleStatus);

                Intent intent = new Intent(INTENT_RULES_IMPORTED);
                intent.putExtra(EXTRA_RULE_INFO,ruleKeySourceMap);
                mContext.sendBroadcast(intent);
  
                if (LOG_DEBUG) Log.d(TAG,"Rules Imported intent sent!");
            }


        }
        sendResponseToRulePublisher();
        sendPublisherUpdaterIntent();


    }

    /**This method helps to invoke RV with
     * the rules which needs to be validated.
     * 
     */
    private void sendPublisherUpdaterIntent(){
    	
        if ((mParentRuleKeyInfoAll != null &&
			mParentRuleKeyInfoAll.size() > 0)  ||
			(mChildRuleKeyInfoAll != null &&
			 mChildRuleKeyInfoAll.size() > 0)){

			  if (LOG_DEBUG) Log.i(TAG,"Publisher Updater Intent sent! ");
			  
			  if(mParentRuleKeyInfoAll != null)
				  if(LOG_DEBUG) Log.d(TAG," ParentRuleKeySize : " + mParentRuleKeyInfoAll.size());
					 
			  if(mChildRuleKeyInfoAll != null)
				  if(LOG_DEBUG) Log.d(TAG," ChildRuleKeySize : " + mChildRuleKeyInfoAll.size());
			  
			  if (mParentRuleKeyInfoAll != null &&
		    			mParentRuleKeyInfoAll.size() > 0 ){
		      		for (String key : mParentRuleKeyInfoAll) {
		      			mParentChildRuleKeyInfoAll.add(key);
		      			if(LOG_DEBUG) Log.d(TAG,"Parent Keys Inserted or Updated : "+key);
		            }	
		      }
			  
			  if (mRuleIsSilentUpdate != null &&
					  mRuleIsSilentUpdate.size() > 0 ){
		      		for (String flag : mRuleIsSilentUpdate) {
		      			mParentChildKeyIsSilentUpdate.add(flag);
		      			if(LOG_DEBUG) Log.d(TAG,"Parent flags : "+flag);
		            }	
		      }
			  
			  if (mChildRuleKeyInfoAll != null &&
					  mChildRuleKeyInfoAll.size() > 0 ){
		      		for (String key : mChildRuleKeyInfoAll) {
		      			mParentChildRuleKeyInfoAll.add(key);
		      			mParentChildKeyIsSilentUpdate.add(Boolean.toString(true));
		      			if(LOG_DEBUG) Log.d(TAG,"Child Keys : "+key);
		            }
		      }
			  
		   
		      //Send intent with the KEY information of rules
		      // which are inserted successfully
		      HashMap<String, ArrayList<String>> ruleKeyMap =
		          new HashMap<String, ArrayList<String>>();
		
		      ruleKeyMap.put(KEY_RULE_KEY, mParentChildRuleKeyInfoAll);
		      ruleKeyMap.put(KEY_RULE_SILENT, mParentChildKeyIsSilentUpdate);
		
		      Intent intent = new Intent(ACTION_PUBLISHER_UPDATER);
		      intent.putExtra(EXTRA_RULE_INFO,ruleKeyMap);
		      intent.putExtra(EXTRA_IMPORT_TYPE, mImportType);
		      intent.putExtra(EXTRA_RESPONSE_ID, mRequestId);
		      mContext.sendBroadcast(intent);

        }
    }

    /** This method helps to send the response to 
     * the Rule Publisher with respect to the rules
     * it has published.
     * 
     */
    private void sendResponseToRulePublisher(){
    	if(LOG_DEBUG) Log.d(TAG,"sendResponseToRulePublisher");
    	if (mImportType == ImportType.RP_RULE &&
    			!Util.isNull(mPubKey)){
	    	if (mParentRuleKeyInfoAll != null &&
	    			mParentRuleKeyInfoAll.size() > 0 ){
	      		for (String key : mParentRuleKeyInfoAll) {
	      			mRulePublisherResponseMap.put(key, XML_INSERT_SUCCESS);
	      			if(LOG_DEBUG) Log.d(TAG,"Keys Inserted or Updated"+key);
	            }	
	      	}
	      	
	      	if (LOG_INFO) Log.i(TAG,"Send Response to Rule Publisher Size of Rule Keys : "
					+ mRulePublisherResponseMap.size());
	      	
	      	if(LOG_DEBUG) Log.d(TAG,"Data send to RP with pub Key "+mPubKey +
	      									" \n Data Send :"+ mRulePublisherResponseMap);
	
	      	Intent intent = new Intent(mPubKey);
	      	intent.putExtra(EXTRA_VERSION, VERSION);
	      	intent.putExtra(EXTRA_RULE_STATUS,mRulePublisherResponseMap);
	        intent.putExtra(EXTRA_COMMAND,PUBLISH_RULE_RESPONSE);
	      	intent.putExtra(EXTRA_RESPONSE_ID, mRequestId);
		mContext.sendBroadcast(intent, PERM_RULE_PUBLISHER);
    	}
    }
    /**
     * Inserts or updates Rules into smartrules Db
     * Logic:
     *     1. Query all the rules
     *     2. insert the new rules
     *     3. Update existing rules
     *     4. Delete and re-insert actions, conditions & CondSensor
     *
     */
    private void insertOrUpdateRulesInDb(){
    	
    	if (LOG_DEBUG) Log.d(TAG,"insertOrUpdateRulesInDb");

        /*
         *  Insert the new rules first
         *  This method will insert entries in all the tables
         */

        int existingRuleCount  = 0;
        String whereClause = getWhereClause(RuleTable.Columns.KEY, mKeySet);
        // This returns valid cursor with rows already present.
        Cursor ruleCursor = RulePersistence.getDisplayRulesCursor(mContext,
                whereClause,
                new String[] {RuleTable.Columns.KEY});

        if(ruleCursor == null)
            Log.e(TAG, "Null cursor in insertOrUpdateRulesInDb!");
        else{
            try{
                if(ruleCursor.moveToFirst()){
                    int keyCol = ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.KEY);
                    do{
                        String ruleKey = ruleCursor.getString(keyCol);
                        if(mKeySet.contains(ruleKey)){
                            Collection<Rule> rules = mToBeImportedRulesMap.values();
                            Set<String> delKeys = new HashSet<String>();
                            for( Rule rule : rules){
                                if(rule.mRuleInfo.getRuleKey().contains(ruleKey)){
                                    // Update one rule at a time
                                    updateExistingRuleInAllTables(rule);
                                    existingRuleCount++;
                                    delKeys.add(rule.mRuleInfo.getRuleKey());
                                }
                            }
                            // remove the added rules
                            for(String key: delKeys) mToBeImportedRulesMap.remove(key);
                        }
                    } while(ruleCursor.moveToNext());
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                ruleCursor.close();
            }
        }

        // import New rules one by one
        Set<String> newKeySet = mToBeImportedRulesMap.keySet();
        for(String ruleKey : newKeySet){
            insertNewRule(mToBeImportedRulesMap.get(ruleKey));
        }

        if (LOG_INFO) Log.i(TAG, newKeySet.size() +
               " New Rules imported.\n" + existingRuleCount + " Existing Rules Updated");
    }

    /**
     * This method helps to insert a new rule
     * 
     * @param rule - complete rule information
     */
    private void insertNewRule(Rule rule){

        String ruleKey = rule.mRuleInfo.getRuleKey();
        if (rule.mRuleInfo.getRuleSource() == RuleTable.Source.SUGGESTED && 
            ruleKey.equals(RULE_KEY_IN_MEETING_CHANGE_RINGER) &&
            doesMeetingRingerChangeRuleExist()) {
		    mRulePublisherResponseMap.put(rule.mRuleInfo.getRuleKey(), XML_INVALID_PARAM);
            if (LOG_DEBUG) Log.d(TAG,"Rule Insertion skipped for " + ruleKey + " as user has a similar rule");
        } else {
            // insert in Rule table
            ContentValues cv = rule.mRuleInfo.getContentValues(mContext);
            String parentRuleKey = cv.getAsString(RuleTable.Columns.PARENT_RULE_KEY);
            if (parentRuleKey != null) {
                long adoptCount = updateAdoptCountForSample(parentRuleKey);
                if (mImportType == ImportType.INFERRED_ACCEPTED && adoptCount > 0){
                    cv = updateWidgetRuleName(cv, adoptCount);
                }
            }
            Uri uri = mContext.getContentResolver().insert(Schema.RULE_TABLE_CONTENT_URI,
                   cv );

            if(uri != null){
                mRuleUpdated = true;


                mParentRuleKeyInfoAll.add(ruleKey);
                if(LOG_DEBUG) Log.d(TAG, "Rule Inserted: " + ruleKey);
                int source = rule.mRuleInfo.getRuleSource();
                if( (source == RuleTable.Source.INFERRED) ||
                    (source == RuleTable.Source.SUGGESTED)){
                    if(mImportType != ImportType.RESTORE_MYRULES){
                        // Update the broadcast values
                        rule.updateResultInfo(true);
                        mRuleIsSilentUpdate.add(Boolean.toString(false));
                    }else{
                    	mRuleIsSilentUpdate.add(Boolean.toString(true));
                    }
                }else if(mImportType == ImportType.INFERRED_ACCEPTED){
                    // Update the broadcast values
                    rule.updateResultInfo(true);
			        mRuleIsSilentUpdate.add(Boolean.toString(true));
                }else{
                	mRuleIsSilentUpdate.add(Boolean.toString(true));
                }

                // Insert Actions, Conditions and ConditionSensors for the New entries
                long ruleId = Long.parseLong(Uri.parse(uri.toString()).getLastPathSegment());
                insertConditionsActionsInARule(rule, ruleId, false);
                insertIconInRule(rule, ruleId, false);
                if(rule.mRuleInfo.isEnabled()){
                    //Send broadcast to the CP
                    if(LOG_DEBUG) Log.d(TAG, "calling notifyConditionPublishers for "+ ruleId);
                    ConditionPersistence.notifyConditionPublishers(mContext, ruleId, true);

            }
        }
    }
    }

    private ContentValues updateWidgetRuleName(ContentValues cv, long adoptCount) {
      //Rule is pushed from widget need to update the rule name
        String ruleName = cv.getAsString(RuleTable.Columns.NAME);
        if (LOG_DEBUG) Log.d(TAG,"Change the rule name "+ ruleName);
        ruleName = ruleName + " " + Long.toString(adoptCount+1);
        if (LOG_DEBUG) Log.d(TAG,"rule name changed to "+ ruleName);
        cv.put(RuleTable.Columns.NAME, ruleName);    
        return cv;
    }

    private long updateAdoptCountForSample(String parentRuleKey) {
        com.motorola.contextual.smartrules.db.business.Rule parentRule = RulePersistence.fetchRuleOnly(mContext, parentRuleKey);
        
        if (parentRule != null) { 
            long childCount = parentRule.getAdoptCount();
            RulePersistence.setAdoptCount(mContext, parentRule.get_id(), childCount + 1);
            return childCount;
        }
        return 0;
    }

    /**
     * For each rule, insert entries in icon table
     *
     * @param rule - rule read from xml file
     * @param ruleId - Rule id
     * @param replace - Old entries to be deleted or not
     */
    private void insertIconInRule(Rule rule, long ruleId, boolean replace) {
        if (LOG_DEBUG) Log.d(TAG,"insertIconInRule : pubKey = " + rule.mRuleInfo.get(XmlConstants.PUBLISHER_KEY) +
                " ruleId = " + ruleId + " replace = " + replace);
		if(replace)
			IconPersistence.deleteIcon(mContext, (int)ruleId);
        String pkgName = Util.getPackageNameForRulePublisher(mContext, rule.mRuleInfo.get(XmlConstants.PUBLISHER_KEY));
		if(pkgName == null) pkgName = mContext.getPackageName();
		IconPersistence.insertIcon(mContext, rule.mRuleInfo.getRuleIcon(), pkgName, (int)ruleId);
	}

	/**
     * Checks if user composed rule with Calendar as precondition
     * and ringer change as action exists, enabled/disabled state
     * @return true - if found.  false otherwise
     */
    private boolean doesMeetingRingerChangeRuleExist() {
        boolean result = false;
        Cursor cursor = null;

        try {
            String whereClause = ConditionTable.Columns.CONDITION_PUBLISHER_KEY
                                 + EQUALS
                                 + Q + CALENDAR_SENSOR_PUBLISHER_KEY + Q
                                 + AND
                                 + ActionTable.Columns.ACTION_PUBLISHER_KEY
                                 + EQUALS
                                 + Q + RINGER_ACTION_PUBLISHER_KEY + Q
                                 + AND
                                 + RuleTable.Columns.SOURCE
                                 + EQUALS
                                 + RuleTable.Source.USER;


            cursor = mContext.getContentResolver().query(Schema.RULE_VIEW_CONTENT_URI,
                    new String[] {RuleTable.Columns.SOURCE},
                    whereClause, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                result = true;
            } else {
                if (LOG_DEBUG) Log.d(TAG, "User composed rule for calendar-ringer does not exist");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in meetingRingerChangeRuleExists");
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }



    /**
     * Create Key based where clause
     *
     * @param list - list of keys
     * @return - where clause
     */
    private String getWhereClause(String column, Set<String> list){

    	Iterator<String> iter = list.iterator();
    	if(iter == null ||
    			(iter != null && ! (iter.hasNext()))) return null;
	
        // create the whereClause
        String condition = column + LIKE + Q;
        String or = Q + OR;
        StringBuilder where = new StringBuilder();
        int indx = 0;
        while(iter.hasNext()){
            if(indx > 0)
                where.append(or);
            where.append(condition);
            where.append(iter.next());
            where.append(LIKE_WILD);
            indx++;
        }
        where.append(Q);
        
        if (LOG_DEBUG) Log.d(TAG,"WhereClause : "+  where.toString());

        return where.toString();
    }

    /**
     * For each rule, insert entries in action, condition and condition sensor tables
     *
     * @param rule - rule read from xml file
     * @param ruleId - Rule id
     * @param replace - Old entries to be deleted or not
     */
    private void insertConditionsActionsInARule(Rule rule, Long ruleId, boolean replace){

	if (LOG_DEBUG) Log.d(TAG,"insertConditionsActionsInARule");
        /*
         * We delete the entries for existing rules, not for New rules
         */
        if(replace)
            deleteAllCondActionsInARule(rule, ruleId);

        // Insert actions in bulk
        int indx = 0;
        if(rule.mActions.size() > 0){
                /*
                 * we need to ensure that if a rule has just one action and if it gets
                 * blacklisted, then the rule itself should have been blacklisted in the
                 * xml
                 */
                ContentValues[] values = new ContentValues[rule.mActions.size()];
                for (ActionContainer act : rule.mActions) {
                    values[indx] = act.getContentValues(mContext);
                    values[indx++].put(ActionTableColumns.PARENT_FKEY, ruleId);
                }
                mContext.getContentResolver().bulkInsert(Schema.ACTION_TABLE_CONTENT_URI, values);
        }

        // insert conditions
        for (ConditionContainer cont: rule.mConditions) {
                ContentValues cv = cont.getContentValues(mContext);
                cv.put(ConditionTableColumns.PARENT_FKEY, ruleId);
                mContext.getContentResolver().insert(Schema.CONDITION_TABLE_CONTENT_URI, cv);
        }
    }


    /**
     * Delete all the entries in action, condition and condition sensor table
     * for a rule
     *
     * @param fKeyList - list of FKEYs
     */
    private void deleteAllCondActionsInARule( Rule rule, Long ruleId){

    	if (LOG_DEBUG) Log.d (TAG,"deleteAllCondActionsInARule");

        String whereClause = null;

        if(rule.mActions.size() > 0){
             // Delete Action table entries
            whereClause = ActionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
            ActionPersistence.deleteAction(mContext, whereClause);
        }

        if(rule.mConditions.size() == 0) return;

        // Query Condition table to fetch existing entries' ID
        whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
        // delete ConditionSensorTable entries
        ConditionPersistence.deleteCondition(mContext, whereClause);
    }

    /** This method helps to get the cursor for a rule
     * 
     * @param ruleKey - Rule Key
     * @return - Cursor for that Rule Key
     */
    private Cursor getRuleCursorEquals(String ruleKey){

        // get cursor with only the required columns
        String[] columns = new String[] { RuleTable.Columns._ID, RuleTable.Columns.SOURCE,
                RuleTable.Columns.INFERENCE_LOGIC,
                RuleTable.Columns.ENABLED,RuleTable.Columns.SUGGESTED_STATE,
                RuleTable.Columns.LIFECYCLE, RuleTable.Columns.KEY};

        String whereClause = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q;
        return RulePersistence.getDisplayRulesCursor(mContext, whereClause, columns);
    }

    /** This method helps to get the rule cursor for a sample rule
     * 
     * @param ruleKey - Sample rule rule Key
     * @return Cursor - Sample rule cursor
     */
    private Cursor getRuleCursorForSampleEquals(String ruleKey){

        // get cursor with only the required columns
        String[] columns = new String[] { RuleTable.Columns._ID, RuleTable.Columns.SOURCE,
                RuleTable.Columns.INFERENCE_LOGIC,
                RuleTable.Columns.ENABLED,RuleTable.Columns.SUGGESTED_STATE,
                RuleTable.Columns.LIFECYCLE, RuleTable.Columns.KEY};

        String whereClause = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q + AND +
						 RuleTable.Columns.SOURCE + EQUALS + Q + RuleTable.Source.FACTORY + Q;
        return RulePersistence.getDisplayRulesCursor(mContext, whereClause, columns);
    }

    /**
     * Update each rule one by one.
     *
     * @param rule - rule to be updated
     */
    private void updateExistingRuleInAllTables(Rule rule){

        int importType = ImportType.IGNORE;
        String ruleKey = rule.mRuleInfo.getRuleKey();

        if (LOG_INFO) Log.i(TAG,"updateExistingRuleInAllTables = "  + ruleKey);

         int inSource = rule.mRuleInfo.getRuleSource();
         switch(inSource){
             case RuleTable.Source.SUGGESTED:{
                 importType = importIncomingSuggestion(rule);
                 break;
             }

             case RuleTable.Source.INFERRED:{
                 importType = updateExistingRti(rule);
                 break;
             }

            case RuleTable.Source.FACTORY:
                 importType = importIncomingSample(rule);
                 break;

            case RuleTable.Source.CHILD:
                importType = importIncomingChild(rule);
                break;

            case RuleTable.Source.USER:{
            	 importType = importIncomingUser(rule);
                break;
            }

            default:
                Log.w(TAG, "update not supported for rules of type " + inSource);
        }

        if(importType == ImportType.IGNORE){
            Log.w(TAG,"Rule Insertion skipped for " + rule.mRuleInfo.getRuleKey());
            mRulePublisherResponseMap.put(rule.mRuleInfo.getRuleKey(), XML_INVALID_PARAM);
        } else {

		mParentRuleKeyInfoAll.add(ruleKey);

            // Update info to send broadcast later
            boolean status = importType != ImportType.SILENT_UPDATE;
            if(status || inSource == RuleTable.Source.INFERRED ){
                rule.updateResultInfo(status);
                mRuleIsSilentUpdate.add(Boolean.toString(false));
            } else if(mImportType == ImportType.INFERRED_ACCEPTED){
                rule.updateResultInfo(status);
                mRuleIsSilentUpdate.add(Boolean.toString(true));
            } else {
                mRuleIsSilentUpdate.add(Boolean.toString(true));
            }
        }
    }

    /**
     * Update user rule.
     *
     * @param rule -  user rule
     */
    private int importIncomingUser(Rule rule){
    	
    	
        if (LOG_DEBUG) Log.d(TAG,"User comming in");
        
	    int importType = ImportType.SILENT_UPDATE;

	    boolean incomingRuleInserted = false;
    	
	    String whereClause = RuleTableColumns.KEY + LIKE + Q + LIKE_WILD + rule.mRuleInfo.getRuleKey() + LIKE_WILD + Q;
	    
	    Cursor ruleCursor = RulePersistence.getRuleCursor(mContext,
                new String[]{RuleTable.Columns._ID, RuleTable.Columns.SOURCE,
	    							RuleTable.Columns.SUGGESTED_STATE, RuleTable.Columns.KEY }, whereClause);
	    
        if(ruleCursor == null)
            Log.e(TAG, "Null cursor in importIncomingUser!");
        else{
            try{
                if(ruleCursor.moveToFirst()){
                 
                    do{
                        long dbRuleId = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
                        int dbSource = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SOURCE));
                        int dbSuggState = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SUGGESTED_STATE));
                        String fullRuleKey = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.KEY));

                    	if (LOG_DEBUG) Log.d(TAG,"Comparing against rule Key" +fullRuleKey);
                        
                    	String parentRuleKey = rule.mRuleInfo.get(PARENT_RULE_KEY);

                        if(dbSource == RuleTable.Source.USER && 
                        		fullRuleKey.equals(rule.mRuleInfo.getRuleKey())){
                	            if (LOG_DEBUG) Log.d(TAG,"User exist");
                	            // we should not replace the existing condition and actions
                	            boolean state = updateRule(rule, dbRuleId, dbSource);
                	            importType = getImportType(dbSuggState, dbSource, state);
                	                        		            
                	            incomingRuleInserted = true;
                	            
                        }else if(dbSource == RuleTable.Source.FACTORY &&
                        		((fullRuleKey.equals(rule.mRuleInfo.getRuleKey())) || 
                            			(!Util.isNull(parentRuleKey) && fullRuleKey.equals(parentRuleKey))
                                   				 || (mXmlDbVersion != SQLiteManager.INVALID_DATABASE_VERSION && 
                                               		 		mXmlDbVersion < 34))) {                       				
            		            if (LOG_DEBUG) Log.d(TAG,"Factory exist");
            		            /*
            		             * Special case for stable 6 to 7 upgrade
            		             * if user rule key is same as sample, then it must be an adopted sample
            		             * in stable6. So just make it an adopted rule in s7
            		             * or
            		             * This is an adopted sample case in hss7 which is backed up and restore. 
            		             */
            		            long childRuleId = linkSampleAndChild(mContext, dbRuleId, rule, fullRuleKey);
            		            if(childRuleId != RuleTable.RuleType.DEFAULT){
            		            	insertConditionsActionsInARule(rule, childRuleId, false);
                                    insertIconInRule(rule, childRuleId, false);
                                    incomingRuleInserted = true;
            		            }else{
            		            	Log.e(TAG,"Rule Insert failed for Sample Child"+rule.mRuleInfo.getRuleKey());
            		            }
            		            
            		            importType = ImportType.SILENT_UPDATE;
                        }
                    } while(ruleCursor.moveToNext());
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                ruleCursor.close();
            }
        }
        
        if (!incomingRuleInserted) {
            if(LOG_INFO) Log.i(TAG,"Rule Copied from an Adopted Sample or " +
            							"Accepted Suggestion inserted"+rule.mRuleInfo.getRuleKey());
            insertNewRule(rule);
        }
             
        return importType;
    }
    
	/**
	 *  To import adopted Sample or accepted suggestion
	 *     
	 * @param rule -  Adopted Sample or Accepted Suggestion
	 * @return int - import type
	 */
	 private int importIncomingAdoptedSampleOrAcceptedSugg(Rule rule){
    		
        if (LOG_DEBUG) Log.d(TAG,"importIncomingAdoptedSampleOrAcceptedSugg");
        
	    int importType = ImportType.SILENT_UPDATE;

	    String whereClause = RuleTableColumns.KEY + LIKE + Q + LIKE_WILD + rule.mRuleInfo.getRuleKey() + LIKE_WILD + Q;
	    
	    Cursor ruleCursor = RulePersistence.getRuleCursor(mContext,
                new String[]{RuleTable.Columns._ID, RuleTable.Columns.SOURCE,
	    							RuleTable.Columns.SUGGESTED_STATE, RuleTable.Columns.KEY,
	    							RuleTable.Columns.FLAGS}, whereClause);
	    
        if(ruleCursor == null)
            Log.e(TAG, "Null cursor in importIncomingAdoptedSampleOrAcceptedSugg!");
        else{
            try{
                if(ruleCursor.moveToFirst()){
                 
                    do{
                        long dbRuleId = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
                        int dbSource = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SOURCE));
                        int dbSuggState = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SUGGESTED_STATE));
                        String dbFullRuleKey = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.KEY));
                        String dbFlags = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.FLAGS));
                        
                        String parentRuleKey = rule.mRuleInfo.get(PARENT_RULE_KEY);

                    	if (LOG_DEBUG) Log.d(TAG,"Comparing against rule Key" +dbFullRuleKey + " dbFlags : "+dbFlags +
                    			" isnull operaton :"+Util.isNull(dbFlags));
                        
                    	// Adopted Sample already exist and it's an update
                        if((dbSource == RuleTable.Source.FACTORY || 
                        		dbSource == RuleTable.Source.SUGGESTED) &&
                        		Util.isNull(dbFlags)  &&
                        		dbFullRuleKey.equals(rule.mRuleInfo.getRuleKey())){
            	            if (LOG_DEBUG) Log.d(TAG,"Adopted Sample exist");
            	            // we should not replace the existing condition and actions
            	            boolean state = updateRule(rule, dbRuleId, dbSource);
            	            importType = getImportType(dbSuggState, dbSource, state);                	            
                        } else if(dbSource == RuleTable.Source.FACTORY &&
                       		 (!Util.isNull(dbFlags)  && dbFlags.equals(RuleTable.Flags.SOURCE_LIST_VISIBLE)) &&
                       		 ((!Util.isNull(parentRuleKey) && dbFullRuleKey.equals(parentRuleKey))
                       				 || (mXmlDbVersion != SQLiteManager.INVALID_DATABASE_VERSION && 
                                   		 		mXmlDbVersion < 34))) {                        	
	                       	//Adopted Sample needs to be linked to the Factory Sample
	       		            if (LOG_DEBUG) Log.d(TAG,"Factory exist or restoring a rule from older DB version");
	       		            long childRuleId = linkSampleAndChild(mContext, dbRuleId, rule, dbFullRuleKey);
	       		            if(childRuleId != RuleTable.RuleType.DEFAULT){
	       		            	insertConditionsActionsInARule(rule, childRuleId, false);
	                               insertIconInRule(rule, childRuleId, false);
	       		            }else{
	       		            	Log.e(TAG,"Rule Insert failed for Sample Child"+rule.mRuleInfo.getRuleKey());
	       		            }
	       		            
	       		            importType = ImportType.SILENT_UPDATE;  
                        }
                    } while(ruleCursor.moveToNext());
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                ruleCursor.close();
            }
        }
             
        return importType;
    }

	/**This method helps to import the adopted or 
	 * parent sample to smartrules DB
	 * 
	 * @param rule - Rule related information
	 * @return int - whether a notification should be shown or not.
	 */
    private int importIncomingSample(Rule rule){

        String fullRuleKey = rule.mRuleInfo.getRuleKey();
        
        RuleInfoContainer ruleInfo = rule.mRuleInfo;
        
        int result = ImportType.SILENT_UPDATE;
        
        if (LOG_INFO) Log.i(TAG,"Factory coming in");
        
        // Sample , Not adopted Sample
        if(!Util.isNull(ruleInfo.getFlags())){
	        // Handle stable 6 upgrade - adopted sample or accepted suggestion or unaccepted suggestion
	        String whereCl = RuleTable.Columns.SOURCE + NOT_EQUAL + Q + RuleTable.Source.FACTORY + Q +
	                             AND + RuleTable.Columns.KEY + EQUALS + Q + fullRuleKey + Q ;
	
	        Cursor c = RulePersistence.getRuleCursor(mContext,
	                                    new String[]{RuleTable.Columns._ID, RuleTable.Columns.SUGGESTED_STATE}, whereCl);
			try{
	            if (c != null && c.moveToFirst()) {
				    long dbRuleId = c.getLong(c.getColumnIndex(RuleTable.Columns._ID));
					int dbSuggState = c.getInt(c.getColumnIndex(RuleTableColumns.SUGGESTED_STATE));
					//stable 6 rule unaccepted suggestion
					if(dbSuggState != RuleTable.SuggState.ACCEPTED){
						if (LOG_DEBUG) Log.d(TAG,"Suggestion Read or Unread");
					    replaceRule(rule, dbRuleId);

	                	// If the suggestion existing in the DB and it is not in accepted state
	                    // then it should be a SILENT UPDATE, however the state of the 
	                    // suggestion needs to be moved to UNREAD state. In case of
	                    // rules restore  the suggested state is preserved in the 
	                    // replaceRuleInRuleTable method.
					    if (mImportType != ImportType.RESTORE_MYRULES){
		                	 SuggestionsPersistence.setSuggestionState(mContext, dbRuleId, 
		                			 							RuleTable.SuggState.UNREAD);
		                }
					    /* Make it a suggested sample
					     * and return
					     */
					    return ImportType.SILENT_UPDATE;
					} else {
						// stable 6 landing page rule - adopted sample or accepted suggestion
						adoptAsSampleChild(fullRuleKey);
					}
				}
			} catch (IllegalArgumentException e){
				e.printStackTrace();
	        } finally {
	            if(c != null) c.close();
	        }
	
	        boolean sampleExists = false;
	        // fetch stable7+ sample
	        Cursor ruleCursor = getRuleCursorForSampleEquals(fullRuleKey);

	
	        try{
	        	if (ruleCursor != null && ruleCursor.moveToFirst()) {
				if(LOG_DEBUG) Log.d(TAG,"Sample exist");
		        	long dbRuleId = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
		        	String dbRuleKey = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.KEY));

		            	// Propagate updates to the child rules
		            int childCount = ruleCursor.getInt(ruleCursor.getColumnIndex(RulePersistence.SAMPLE_RULE_ADOPTED_COUNT));
		            if(childCount > 0){
		
		                // Traverse thru each child and propagate new actions/conditions/suggestion text
		                updateChildRules(rule, dbRuleId, null, dbRuleKey);
		            }
		
		            // Samples should always be of NEVER_EXPIRE type
		            rule.mRuleInfo.put(LIFECYCLE, String.valueOf(RuleTable.Lifecycle.NEVER_EXPIRES));
	                    // Update the ADOPT_COUNT to get unique name during adoption	
                            rule.mRuleInfo.put(ADOPT_COUNT,String.valueOf(childCount));

		            replaceRule(rule, dbRuleId);
		            sampleExists = true;
	       		} 
	       }catch (IllegalArgumentException e){
	                e.printStackTrace();
	       } finally {
	       	if(ruleCursor != null) ruleCursor.close();
	       }
	
	        if(!sampleExists){

			if(LOG_DEBUG) Log.d(TAG,"Sample does not exist");
	
	            // insert a new sample rule in Rule table
	            Uri uri = mContext.getContentResolver().insert(Schema.RULE_TABLE_CONTENT_URI,
	                    rule.mRuleInfo.getContentValues(mContext));
	
	
	            if(uri != null){
	            	 long ruleId = Long.parseLong(Uri.parse(uri.toString()).getLastPathSegment());
	            		
		                // add conditions/actions for the sample
		                insertConditionsActionsInARule(rule, ruleId, false);
		                insertIconInRule(rule, ruleId, false);
		
		                //Update the PARENT_RULE_KEY for the child
		                ContentValues cv = new ContentValues();
		                cv.put(RuleTableColumns.PARENT_RULE_KEY, rule.mRuleInfo.getRuleKey());
		
		                String whereClause = RuleTableColumns.KEY + LIKE + Q + LIKE_WILD 
							+ fullRuleKey + LIKE_WILD + Q + AND
		                            + RuleTable.Columns.PARENT_RULE_KEY + EQUALS 
		                            	+ Q + rule.mRuleInfo.getRuleKey() + Q;
		                int childCount = RulePersistence.updateRule(mContext, cv, whereClause);
		
		                cv.clear();
		                cv.put(RuleTableColumns.ADOPT_COUNT, childCount);
		                String where = RuleTableColumns._ID + EQUALS + Q + ruleId + Q;
		                RulePersistence.updateRule(mContext, cv, where);
		                // Propagate updates to the child rules
		                // Traverse thru each child and propagate new actions/conditions/suggestion text
		                updateChildRules(rule, ruleId, whereClause, rule.mRuleInfo.getRuleKey());
	            }else{
	                Log.e(TAG,"null uri");
	            }
	        }
        } else{
        	// Adopted Sample
        	if (LOG_INFO) Log.i(TAG,"Adopted Sample");
        	result = importIncomingAdoptedSampleOrAcceptedSugg(rule);
        }
   
        return result;
}
    /**This method imports the child rule
     * 
     * 
     * @param rule - Rule related information
     * @return int - clarifies whether the notification 
     *               should be thrown or not
     */
    private int importIncomingChild(Rule rule){

        String fullRuleKey = rule.mRuleInfo.getRuleKey();

        int result = ImportType.SILENT_UPDATE;

        if (LOG_DEBUG) Log.d(TAG,"Factory Child coming in");

        boolean ruleExists = false;
        // fetch stable7+ sample
        Cursor ruleCursor = getRuleCursorEquals(fullRuleKey);

        try{
            if (ruleCursor != null && ruleCursor.moveToFirst()) {
                long dbRuleId = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
                // Samples should always be of NEVER_EXPIRE type
                rule.mRuleInfo.put(LIFECYCLE, String.valueOf(RuleTable.Lifecycle.NEVER_EXPIRES));

                replaceRule(rule, dbRuleId);
                ruleExists = true;
            }
       }catch (IllegalArgumentException e){
                e.printStackTrace();
       } finally {
        if(ruleCursor != null) ruleCursor.close();
       }

        if(!ruleExists){

            // insert a new sample rule in Rule table
            Uri uri = mContext.getContentResolver().insert(Schema.RULE_TABLE_CONTENT_URI,
                    rule.mRuleInfo.getContentValues(mContext));


            if(uri != null){
                long ruleId = Long.parseLong(Uri.parse(uri.toString()).getLastPathSegment());

                // add conditions/actions for the sample
                insertConditionsActionsInARule(rule, ruleId, false);

            }else{
                Log.e(TAG,"null uri");
            }
        }


        return result;
    }

    /**This method will help to link the adopted sample to 
     * the parent sample
     * 
     * @param fullRuleKey - Sample Rule Key
     */
    private void adoptAsSampleChild(String fullRuleKey){
	if (LOG_DEBUG) Log.d(TAG,"adoptAsSampleChild");
		// rename the key of the existing rule to make it a child rule
        ContentValues cv = new ContentValues();

        String childKey = RulePersistence.createClonedRuleKeyForSample(fullRuleKey);
        cv.put(RuleTableColumns.KEY, childKey);
        cv.put(RuleTableColumns.TAGS,childKey);
        cv.put(RuleTableColumns.ADOPT_COUNT, 0);
        cv.put(RuleTableColumns.PARENT_RULE_KEY, fullRuleKey);
        
        String whereClause = RuleTableColumns.KEY + EQUALS + Q + fullRuleKey + Q;
        RulePersistence.updateRule(mContext, cv, whereClause);

        if (LOG_DEBUG) Log.d(TAG,"Child Rule Key Changed: "+childKey);
    }

    /**
     * decide if broadcast has to be sent or not
     *
     * @param dbSuggState - suggestion state in db
     * @param dbSource - source in db
     * @param state - operation passed?
     * @return - to broadcast or not
     */
    private int getImportType(int dbSuggState, int dbSource, boolean state){

        int importType = ImportType.IGNORE;

        if( ! state ) return importType;

        if(dbSuggState != RuleTable.SuggState.ACCEPTED){

            // For read or unread, its always silent
            importType = ImportType.SILENT_UPDATE;
        } else {

            // For accepted, always sent notification
            importType = ImportType.UPDATE;
        }

        return importType;
    }

    /**
     * Updates new actions/conditions and critical info in RuleTable
     *
     * @param rule - incoming rule
     * @param dbRuleId - rule ID in db
     * @param dbSource - rule source in db
     * @return - status
     */
    private boolean updateRule(Rule rule, final long dbRuleId, final int dbSource){

        if (LOG_DEBUG) Log.d(TAG,"Update Rule: " + rule.mRuleInfo.getRuleKey());

        boolean result = false;

        // we should not replace the existing condition and actions
        if( dropDuplicateConditionsActions(rule, dbRuleId, dbSource) ){

            result = replaceOrUpdateRule(rule, dbRuleId, false);
        } else {
            if (LOG_DEBUG) Log.d(TAG,"Nothing to Update!");
        }

        return result;
    }

    /** Replaces RuleTable content, adds new Conditions/Actions/ConditionsSensors
     *
     * @param rule - incoming rule
     * @param dbRuleId - rule If in db
     * @return - pass or fail
     */
    private boolean replaceRule(Rule rule, final long dbRuleId){

        if (LOG_DEBUG) Log.d(TAG,"Replace Rule: " + rule.mRuleInfo.getRuleKey());
        return replaceOrUpdateRule(rule, dbRuleId, true);
    }

    /**
     * Replace RuleTable/Conditions/Actions/ConditionsSensors
     *
     * @param rule - incoming rule
     * @param dbRuleId - rule id in db
     * @param replace - replace or update
     * @return status
     */
    private boolean replaceOrUpdateRule(Rule rule, final long dbRuleId, boolean replace){

        if (LOG_DEBUG) Log.d(TAG,"replaceOrUpdateRule");

        boolean result = false;

        if(replace)
            // Replace all column values in Rule Table
            result = replaceRuleInRuleTable(dbRuleId, rule.mRuleInfo.getContentValues(mContext));
        else
            // Replace only the suggestions/critical column values in Rule Table
            result = updateSuggestionContent(rule, dbRuleId);

        if(result){
            // Replace/Update conditions and actions
            insertConditionsActionsInARule(rule, dbRuleId, replace);
            insertIconInRule(rule, dbRuleId, replace);
        }

        if (LOG_DEBUG) Log.d(TAG,"Result: "+result);
        return result;
    }


    /**
     * import the Rule depending on what it is replacing
     *
     *
     * @param ruleCursor - rule cursor
     * @param rule - rule object
     * @param childCount - count of child rules
     * @return - import type
     */
    private int importIncomingSuggestion(Rule rule){

    	if (LOG_INFO) Log.i(TAG,"importIncomingSuggestion");

        int result = ImportType.IGNORE;
        int inSuggType = (int) rule.mRuleInfo.getLifeCycle();
        
        RuleInfoContainer ruleInfo = rule.mRuleInfo;
        
        // Suggestion , Not accepted
        if(!Util.isNull(ruleInfo.getFlags())){

	        Cursor ruleCursor = getRuleCursorEquals(rule.mRuleInfo.getRuleKey());
	        if (ruleCursor == null || !ruleCursor.moveToFirst()) {
	            Log.e(TAG, "in source - suggestion: critical error, rule update ignored");
	
	            if(ruleCursor != null) ruleCursor.close();
	            return result;
	        }
	
	
	        int dbSource = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SOURCE));
	        long dbRuleId = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
	        int dbSuggState = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SUGGESTED_STATE));
	
	        switch(dbSource){
	            case RuleTable.Source.FACTORY:
	                result = updateSuggestedSample(rule, ruleCursor);
	                break;
	
	            case RuleTable.Source.SUGGESTED:
	
	                if(inSuggType == RuleTable.Lifecycle.SWAP_ONE){
	                    result = swapCondition(rule, dbSuggState, dbSource, dbRuleId);
	
	                } else if (dbSuggState == RuleTable.SuggState.ACCEPTED) {
	                    // for accepted rule, just add new actions/conditions
	                    updateRule(rule, dbRuleId, dbSource);
	                    result = ImportType.SILENT_UPDATE;
	
	                } else {	                		                	
	                    // blindly replace
	                    replaceRule(rule, dbRuleId);
	                    
	                	 // If the suggestion existing in the DB and it is not in accepted state
	                     // then it should be a SILENT UPDATE, however the state of the 
	                     // suggestion needs to be moved to UNREAD state. In case of
	                     // rules restore  the suggested state is preserved in the 
	                     // replaceRuleInRuleTable method.
		                 result = ImportType.SILENT_UPDATE;
		                 if (mImportType != ImportType.RESTORE_MYRULES){
		                	 SuggestionsPersistence.setSuggestionState(mContext, dbRuleId, 
		                			 							RuleTable.SuggState.UNREAD);
		                 }
	                }
	                break;
	
	            case RuleTable.Source.USER:
	
	                if(inSuggType == RuleTable.Lifecycle.UPDATE_RULE ||
	                   inSuggType == RuleTable.Lifecycle.SWAP_ONE) {
	
	                    boolean state = updateRule(rule, dbRuleId, dbSource);
	                    result = getImportType(dbSuggState, dbSource, state);
	                }
	                break;
	
	            case RuleTable.Source.COMMUNITY:
	            case RuleTable.Source.DEFAULT:
	            case RuleTable.Source.INFERRED:
	            default:
	                Log.e(TAG, "importIncomingSuggestion: Invalid usecase: dbSource=" + dbSource);
	        }
	
	        ruleCursor.close();
        }else{
        	// Accepted Suggestion 
        	if (LOG_INFO) Log.i(TAG,"Accepted Suggestion ");
        	result = importIncomingAdoptedSampleOrAcceptedSugg(rule);
        	
        }
        return result;
    }




    /**
     * Swap the existing condition - delete old if its a sample or unaccepted suggestion
     * else just insert the new condition in disabled state
     *
     * @param rule - incoming rule object
     * @param dbSuggState - read/unread/accepted in db
     * @param dbSource - source in db
     * @param dbRuleId - rule ID in db
     * @return - import type
     */
    private int swapCondition(Rule rule, int dbSuggState, int dbSource, long dbRuleId){

    	if (LOG_DEBUG) Log.d(TAG,"swapCondition");

        int result = ImportType.IGNORE;

        String dbSwapPubKey = rule.mConditions.get(0).get(SWAP);
        if(dbSwapPubKey == null) return result;

        if(dbSource != RuleTable.Source.USER){

            String whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + dbRuleId + AND
			+ ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + Q + dbSwapPubKey + Q;

            // delete the old/existing condition
            ConditionPersistence.deleteCondition(mContext, whereClause);

            // set the new condition as accepted/enabled
            rule.mConditions.get(0).put(ENABLED, String.valueOf(RuleTable.Enabled.ENABLED));
            rule.mConditions.get(0).put(SUGGESTED_STATE, String.valueOf(RuleTable.SuggState.ACCEPTED));

            // insert new condition
            insertConditionsActionsInARule(rule, dbRuleId, false);

            result = (dbSuggState == RuleTable.SuggState.READ)?
                    ImportType.UPDATE:ImportType.SILENT_UPDATE;
        } else {

            boolean state = updateRule(rule, dbRuleId, RuleTable.Source.USER);
            result = getImportType(dbSuggState, dbSource, state);

        }

        return result;
    }

    /**
     * Replace/update existing Sample by the incoming Suggestion
     *
     * @param rule - incoming rule
     * @param ruleCursor - rule cursor of the sample
     * @return - import type
     */
    private int updateSuggestedSample(Rule rule, Cursor ruleCursor){

    	if (LOG_DEBUG) Log.d(TAG,"updateSuggestedSample");

        int result = ImportType.IGNORE;

        RuleInfoContainer ruleInfo = rule.mRuleInfo;
        String ruleKey = rule.mRuleInfo.getRuleKey();
        int inSuggType = (int)rule.mRuleInfo.getLifeCycle();
        	

        long dbRuleId = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
        int dbSuggState = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SUGGESTED_STATE));
        String dbRuleKey = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.KEY));
        int adoptCount = ruleCursor.getInt(ruleCursor.getColumnIndex(RulePersistence.SAMPLE_RULE_ADOPTED_COUNT));

        switch(inSuggType){
        case RuleTable.Lifecycle.NEVER_EXPIRES:
            // does it have child rules?
            if(adoptCount > 0){
            	if(LOG_DEBUG) Log.d(TAG, "Update adopted rules" + ruleKey);

				//Traverse thru each child and propagate new actions/conditions/suggestion text
                updateChildRules(rule, dbRuleId, null, dbRuleKey);

                //Blindly replace sample
                // update the incoming source to sample = 4
                ruleInfo.put(SOURCE, String.valueOf(RuleTable.Source.FACTORY));
                
                // Update the ADOPT_COUNT to get unique name during adoption
                rule.mRuleInfo.put(ADOPT_COUNT,String.valueOf(adoptCount));

                replaceRule(rule, dbRuleId);
                result = ImportType.SILENT_UPDATE;
                
                // If the suggestion existing in the DB and it is not in accepted state
                // then it should be a SILENT UPDATE, however the state of the 
                // suggestion needs to be moved to UNREAD state. In case of
                // rules restore  the suggested state is preserved in the 
                // replaceRuleInRuleTable method.
                if(dbSuggState != RuleTable.SuggState.ACCEPTED &&
                		mImportType != ImportType.RESTORE_MYRULES){
                	SuggestionsPersistence.setSuggestionState(mContext, dbRuleId, 
                											RuleTable.SuggState.UNREAD);
                } 
            } else if (ruleKey.equals(RULE_KEY_IN_MEETING_CHANGE_RINGER) &&
                    doesMeetingRingerChangeRuleExist()) {

                if(LOG_DEBUG) Log.d(TAG, "user has a similar rule " + ruleKey);
                result = ImportType.IGNORE;

            } else { // no child rules

                // update the incoming source to sample = 4
                ruleInfo.put(SOURCE, String.valueOf(RuleTable.Source.FACTORY));
                boolean state = replaceRule(rule, dbRuleId);
                result = getImportType(dbSuggState, RuleTable.Source.FACTORY, state);
                
                // If the suggestion existing in the DB and it is not in accepted state
                // then it should be a SILENT UPDATE, however the state of the 
                // suggestion needs to be moved to UNREAD state. In case of
                // rules restore  the suggested state is preserved in the 
                // replaceRuleInRuleTable method.
                if(dbSuggState != RuleTable.SuggState.ACCEPTED &&
                		mImportType != ImportType.RESTORE_MYRULES){
                	SuggestionsPersistence.setSuggestionState(mContext, dbRuleId, 
                											RuleTable.SuggState.UNREAD);
                } 
            }
            break;

        case RuleTable.Lifecycle.UPDATE_RULE:{

            if(adoptCount > 0){

                //Traverse thru each child and propagate new actions/conditions/suggestion text
                updateChildRules(rule, dbRuleId, null, dbRuleKey);
            }

            // Samples are always of NEVER_EXP type
            ruleInfo.put(LIFECYCLE, String.valueOf(RuleTable.Lifecycle.NEVER_EXPIRES));

            updateRule(rule, dbRuleId, RuleTable.Source.FACTORY);
            result = ImportType.SILENT_UPDATE;

            break;
        }

        case RuleTable.Lifecycle.SWAP_ONE:{
            swapCondition(rule, dbSuggState, RuleTable.Source.FACTORY, dbRuleId);

            if(adoptCount > 0){

                //Traverse thru each child and propagate conditions/suggestion text
                updateChildRules(rule, dbRuleId, null, dbRuleKey);
            }
            break;
        }

        default:
            Log.e(TAG, "importIncomingSuggestion: Invalid usecase: inSuggType=" + inSuggType);
            result = ImportType.IGNORE;
        }

        return result;
    }


    /**
     * Propagate the new actions/conditions to the child rules
     *
     * @param sampleRule - Sample rule xml object
     * @param sampleRuleId - Sample rule id
     */
    private void updateChildRules(Rule sampleRule, long sampleRuleId, 
    		String whereClause, String sampleRuleKey){

    	if (LOG_DEBUG)  Log.d(TAG,"updateChildRules");

        String[] columns = new String[] {RuleTable.Columns._ID, RuleTable.Columns.KEY, RuleTable.Columns.SOURCE};
        Cursor childCursor = 
        		RulePersistence.getSampleChildRuleCursor(mContext, sampleRuleId, 
        				whereClause, columns, sampleRuleKey);

        if(childCursor == null){
            Log.e(TAG, "updateChildRules: Null cursor");
            return;
        }

        try{
            if(childCursor.moveToFirst()){

                do{
                    Rule childRule = new Rule(sampleRule);
                    long childRuleId = childCursor.getLong(childCursor.getColumnIndexOrThrow(RuleTable.Columns._ID));
                    String childRuleKey = childCursor.getString(childCursor.getColumnIndexOrThrow(RuleTable.Columns.KEY));
                    int suggType = (int)childRule.mRuleInfo.getLifeCycle();
                    

                    suggType = (suggType == RuleTable.Lifecycle.SWAP_ONE)?
                                    RuleTable.Lifecycle.SWAP_ONE:RuleTable.Lifecycle.UPDATE_RULE;

                    childRule.mRuleInfo.put(LIFECYCLE, String.valueOf(suggType));

                    if(updateRule(childRule, childRuleId, RuleTable.Source.USER)){
                    	
                    	if(LOG_DEBUG) Log.d(TAG,"Added Child Rule Key to update RV : "+childRuleKey);
                    	
                    	mChildRuleKeyInfoAll.add(childRuleKey);

                        childRule.mRuleInfo.put(KEY, String.valueOf(childRuleKey));
                        childRule.mRuleInfo.put(SOURCE, String.valueOf(RuleTable.Source.USER));

                        // IKINTNETAPP-509 - Do not send update broadcast

                    }
                }while (childCursor.moveToNext());
            }
        } catch (IllegalArgumentException e){
            Log.e(TAG, "updateChildRules: IllegalArgumentException");
        } finally {
            childCursor.close();
        }
    }

    /**
     * Replaces Rule in rule table
     *
     * @param ruleId - Rule ID
     * @param cv - ContentValues
     * @return - Import status
     */
    private boolean replaceRuleInRuleTable(long ruleId, ContentValues cv){

        if (LOG_DEBUG) Log.d(TAG,"replaceRuleInRuleTable");

        boolean result = true;

        if(mImportType != ImportType.RESTORE_MYRULES) {
             if(LOG_DEBUG) Log.d(TAG,"Ignore Suggested State value");
             cv.remove(RuleTableColumns.SUGGESTED_STATE);

             /*
              * Do not import Suggestion free flow text from sample if
              * it equals to null
              */
             if(cv.getAsInteger(RuleTable.Columns.SOURCE) == RuleTable.Source.FACTORY){
                  String free = cv.getAsString(RuleTable.Columns.RULE_SYNTAX);
                  if(Util.isNull(free))
                      cv.remove(RuleTable.Columns.RULE_SYNTAX);
             }
        }

        // We should never overwrite ACTIVE column!
        cv.remove(RuleTableColumns.ACTIVE);

        String whereClause = RuleTable.Columns._ID + EQUALS + ruleId;
        int row = RulePersistence.updateRule(mContext, cv, whereClause);

        if (row == 0) {
            Log.e(TAG, "Rule update failed for " + cv.getAsString(KEY));
            result = false;
        }

        return result;
    }

    /**
     * Selectively update the Suggestion related fields in RuleTable
     *
     * @param rule - Rule object
     * @param dbRuleId - rule id in DB
     *
     * @return - success or failed
     */
    private boolean updateSuggestionContent(Rule rule, long dbRuleId){

    	if (LOG_DEBUG) Log.d(TAG,"updateSuggestionContent");

        boolean result = false;
        ContentValues cv = new ContentValues();

        // Update FreeFlow text
        String sugXml = rule.mRuleInfo.get(SUGGESTION_FREEFLOW);
        String ruleKey = RulePersistence.getRuleKeyForRuleId(mContext, dbRuleId);
        if( !Util.isNull(sugXml) ){
        	
        	if (LOG_DEBUG) Log.d(TAG,"Text to translate " + sugXml);
            if(LOG_DEBUG) Log.d(TAG, "Free Flow Suggestion for Key= "+ ruleKey);

            String translatedSuggestedText = FileUtil.translateSuggestionText(mContext, sugXml);
            if (LOG_DEBUG) Log.d(TAG,"translatedSuggestedText returned " + translatedSuggestedText);
            cv.put(RuleTable.Columns.RULE_SYNTAX, translatedSuggestedText);
        }

        // Update suggested reason text for boiler plate UI
        String suggReason = rule.mRuleInfo.get(SUGGESTED_REASON);
        if( !Util.isNull(suggReason) ){
            cv.put(RuleTable.Columns.SUGGESTED_REASON, suggReason);
        }

        // change the life cycle field
        cv.put(LIFECYCLE, rule.mRuleInfo.getLifeCycle());

        // Update the values in RuleTable
        String where = RuleTableColumns.KEY + EQUALS + Q + ruleKey + Q;
        int numRows = RulePersistence.updateRule(mContext, cv, where);
        if (numRows > 0) {
            result = true;
        } else {
            Log.e(TAG,"updateSuggestionContent: Update failed");
        }

        return result;
    }

    /**
     * Updated RTI in Rule Table
     *
     * @param ruleInfo - Rule table xml block
     * @return - import type
     */
    private int updateExistingRti(Rule rule){

        if (LOG_DEBUG) Log.d(TAG,"updateExistingRti");

        boolean updated = false;
        int result = ImportType.IGNORE;

        Cursor cursor = getRuleCursorEquals(rule.mRuleInfo.getRuleKey());
        if (cursor == null || !cursor.moveToFirst()) {
            Log.e(TAG, "in source - inferred: critical error, rule update ignored");

            if(cursor != null) cursor.close();
            return result;
        }

        // ignore if DB source is not of INFERRED type
        int dbSource = cursor.getInt(cursor.getColumnIndex((RuleTable.Columns.SOURCE)));
        if(dbSource != RuleTable.Source.INFERRED) return result;

        String infLogic = cursor.getString(cursor.getColumnIndex
                (RuleTable.Columns.INFERENCE_LOGIC));
        int enabled = cursor.getInt(cursor.getColumnIndex((RuleTable.Columns.ENABLED)));

        int suggState = cursor.getInt(cursor.getColumnIndex((RuleTable.Columns.SUGGESTED_STATE)));

        cursor.close();

        ContentValues cv = new ContentValues();
        if (infLogic != null && !infLogic.equals(rule.mRuleInfo.get(INFERENCE_LOGIC))) {
            cv.put(RuleTable.Columns.INFERENCE_LOGIC, rule.mRuleInfo.get(INFERENCE_LOGIC));
            updated = true;
        }

        if (enabled != rule.mRuleInfo.getIntValue(ENABLED, 0)) {
           cv.put(RuleTable.Columns.ENABLED, rule.mRuleInfo.getIntValue(ENABLED, 0));
           updated = true;
        }

        if (suggState != RuleTable.SuggState.ACCEPTED) {
            cv.put(RuleTable.Columns.SUGGESTED_STATE, RuleTable.SuggState.ACCEPTED);
            updated = true;
        }

        if (updated) {

            String where = RuleTable.Columns.KEY + DbSyntax.EQUALS + DbSyntax.Q + rule.mRuleInfo.getRuleKey() + DbSyntax.Q;
            int numRows = RulePersistence.updateRule(mContext, cv, where);

            if (numRows > 0) {
                result = ImportType.SILENT_UPDATE;
            }
        }

        return result;
    }

   
    /**
     * Compares Conditions and Actions present in the incoming XML
     * with the ones in the DB and then removes the common from the input XML
     *
     * @param rule - Rule object
     * @param ruleId - _ID of the rule
     * @return
     */
    private boolean dropDuplicateConditionsActions(Rule rule, long ruleId, int source){

    	if (LOG_DEBUG) Log.d(TAG,"dropDuplicateConditionsActions");

        boolean result = false;

        // get the list of action publisher keys already present for this rule
        List<String> pubKeys = ActionPersistence.getPublisherKeys(mContext, ruleId);

        if (pubKeys.size() > 0) {
            // trim the actions to only the new ones
            ListIterator<ActionContainer> li = rule.mActions.listIterator();

            while(li.hasNext()) {
                ActionContainer action = li.next();

                if (pubKeys.contains(action.get(XmlConstants.PUBLISHER_KEY))) {
                    li.remove();
                } else {

                    int suggState = RulePersistence.getColumnIntValue(mContext,
                    		rule.mRuleInfo.getRuleKey(), RuleTableColumns.SUGGESTED_STATE);

                    if( source == RuleTable.Source.FACTORY   ||
                       (source == RuleTable.Source.SUGGESTED &&
                        suggState == RuleTable.SuggState.UNREAD)){

                            // Ensure that new actions are connected and in accepted state
                            action.put(ENABLED, String.valueOf(RuleTable.Enabled.ENABLED));
                            action.put(SUGGESTED_STATE, String.valueOf(RuleTable.SuggState.ACCEPTED));

                    } else {
                            // Ensure that new actions are disconnected and in unread state
                            action.put(ENABLED, String.valueOf(RuleTable.Enabled.DISABLED));
                            action.put(SUGGESTED_STATE, String.valueOf(RuleTable.SuggState.UNREAD));
                    }
                }
            }

            if (rule.mActions.size() > 0) {
                result = true;
            }
        }

        // get the list of action publisher keys already present for this rule
        pubKeys = ConditionPersistence.getPublisherKeys(mContext, ruleId);

        if (pubKeys.size() > 0) {
            // trim the actions to only the new ones
            ListIterator<ConditionContainer> li = rule.mConditions.listIterator();

            while(li.hasNext()) {
                ConditionContainer condition = li.next();

                if (pubKeys.contains(condition.get(XmlConstants.PUBLISHER_KEY))) {
                    li.remove();
                } else {

                    int suggState = RulePersistence.getColumnIntValue(mContext,
                    		rule.mRuleInfo.getRuleKey(), RuleTableColumns.SUGGESTED_STATE);

                    if( source == RuleTable.Source.FACTORY   ||
                       (source == RuleTable.Source.SUGGESTED &&
                        suggState == RuleTable.SuggState.UNREAD)){

                            // Ensure that new conditions are connected and in accepted state
                        condition.put(ENABLED, String.valueOf(RuleTable.Enabled.ENABLED));
                        condition.put(SUGGESTED_STATE, String.valueOf(RuleTable.SuggState.ACCEPTED));

                    } else {
                            // Ensure that new condition are disconnected and in unread state
                        condition.put(ENABLED, String.valueOf(RuleTable.Enabled.DISABLED));
                        condition.put(SUGGESTED_STATE, String.valueOf(RuleTable.SuggState.UNREAD));
                    }
                }
            }

            if (rule.mConditions.size() > 0) {
                result = true;
            }
        }

        return result;
    }
    
    /** Class representing a Smart Rule
     *
     * XML Format:
     *     <SMARTRULE>
     *
     *     <RULEINFO>
     *     --- snipped ---  ---> Rule Header, written to RuleTable of SmartRules DB ( Mandatory )
     *     </RULEINFO>
     *
     *     <CONDITIONS>
     *     --- snipped ---   ---> written to ConditionTable of SmartRules DB
     *     </CONDITIONS>
     *
     *     <ACTIONS>
     *     --- snipped ---  ---> written to ActionTable of SmartRules DB
     *     </ACTIONS>
     *
     *     </SMARTULE>
     *
     */
    private class Rule {

        private Node                    mRuleNode;
        private RuleInfoContainer       mRuleInfo;
        private ActionsContainer        mActions;
        private ConditionsContainer     mConditions;
        private boolean isRuleInfoPresent;

        Rule(Node ruleNode) {
            mRuleNode = ruleNode;
            mActions = new ActionsContainer();
            mConditions = new ConditionsContainer();

            isRuleInfoPresent = parseRuleValues();
        }

        /**
         * Copy constructor
         *
         * @param rule - Rule object
         */
        Rule(Rule rule) {
            mRuleNode = rule.mRuleNode;
            mActions = new ActionsContainer();
            mConditions = new ConditionsContainer();

            parseRuleValues();
        }

        /** Returns true if the node is a Top level node i.e. one of
         *  RULEINFO, CONDITIONS or ACTIONS
         *
         * @param node
         * @return
         */
        private boolean isValidTopLevelNode(Node node) {
            String name = node.getNodeName();
            if (name.equals(RULEINFO) || name.equals(CONDITIONS) ||
                    name.equals(ACTIONS)) {
                return true;
            } else {
                if(LOG_DEBUG) Log.w(TAG, "isValidTopLevelNode returning false; Nodename is " + name);
                return false;
            }
        }

        /** Parses the rule
         *
         * @return parse status
         */
        private boolean parseRuleValues() {

            NodeList nl = mRuleNode.getChildNodes();
            HashMap<String, Node> children = new HashMap<String, Node>();
            boolean ruleInfoPresent = false;
            //validate the rule
            for (int index = 0; index < nl.getLength(); index++) {
                Node n = nl.item(index);
                if (isValidTopLevelNode(n)) {
                    if (n.getNodeName().equals(RULEINFO)) {
                        ruleInfoPresent = true;
                    }
                    children.put(n.getNodeName(), n);
                }
            }

            if ( ruleInfoPresent ) {
                //valid rule
                NodeList list = null;
                Iterator<Entry<String, Node>> iter = children.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<String, Node> entry = iter.next();
                    String name = entry.getKey();
                    if (name.equals(RULEINFO)) {
                        if (LOG_DEBUG) Log.d(TAG, "mParentRuleKey is " + mParentRuleKey);
                        mRuleInfo = new RuleInfoContainer(entry.getValue(), mParentRuleKey);
                    } else {
                        // ACTIONS or CONDITIONS node
                        list = entry.getValue().getChildNodes();
                        int length = list.getLength();
                        for ( int i = 0; i < length; ++i) {
                            String childName = list.item(i).getNodeName();
                            if (childName.equals(ACTION)) {
                                mActions.add(new ActionContainer(list.item(i)));
                            } else if (childName.equals(CONDITION)) {
                                mConditions.add(new ConditionContainer(list.item(i)));
                            } else {
                                if(LOG_DEBUG) Log.w(TAG, "unrecognized child node");
                            }
                        }
                    }
                }
            }

            return ruleInfoPresent;
        }


        /** Updates the result info
         *
         * @param status - rule import succeeded or failed
         */
        private void updateResultInfo(boolean status) {

            if(LOG_DEBUG) Log.d(TAG, "Adding to Broadcast List= " + mRuleInfo.getRuleKey());

            // Updating the KEY & SOURCE information of a rule which
            // is successfully inserted.
            // Note: Even if a SOURCE or KEY node is not available
            // in the XML it will add a null string value to the
            // respective SOURCE or KEY.
            mRuleKeyInfo.add(mRuleInfo.getRuleKey());
            String source =  Integer.toString(mRuleInfo.getRuleSource());
            mRuleSourceInfo.add(source);
            mRuleStatus.add(Boolean.toString(status));

            // Check if any Rule to Infer Rule is imported
            if(source != null && source.equals(String.valueOf(Source.INFERRED))) {
                if (LOG_DEBUG) Log.d(TAG,"Inferred Rule Key: "+mRuleInfo.getRuleKey());
                mRTIPresent = true;
            }
        }

        /** This method helps to check the validity of the rule by 
         *  checking whether the mandatory tags are available or not 
         *  at the RULEINFO level
         * 
         * @return boolean - Mandatory tags are available or not
         */
        private boolean isValidRule(){
            boolean status = false;
            boolean isBlacklisted = false;
        	        
         	//RULEINFO Level
            if (isRuleInfoPresent && 
         	    mRuleInfo != null &&
         	    mRuleInfo.areMandatoryTagsAvailable()){

                // check if the rule has been blacklisted or not
                RuleFilterList instFilterList = RuleFilterList.getInstance();
                if(! instFilterList.isBlacklisted(mContext, mRuleInfo.getRuleKey())){
                    status = true;
                }else{
                    Log.e(TAG,"Blacklisted Rule ignored: " + mRuleInfo.getRuleKey());
                    isBlacklisted = true;
                }

            }else{
		        if (mRuleInfo !=null) {
		     	   Log.e(TAG,"Invalid Rule : Mandatory tags not " +
		    		   		"available : "+ mRuleInfo.getRuleKey());
		        }else{
		     	   Log.e(TAG,"Invalid Rule & mRuleInfo is null");
		        }
            }
           
            // Applicable only for RP rules for now
            // considering backward compatibility
            if( mImportType == ImportType.RP_RULE && status){
               //Look for All Actions Mandatory tags
               if (mActions.isEmpty()) {
            	   Log.e(TAG,"No actions for RuleKey :"+mRuleInfo.getRuleKey());
            	   status =  false;
               }else{
	               for (ActionContainer action : mActions) {
	            	   if(!action.areMandatoryTagsAvailable()){
	            		   status = false;
	            		   Log.e(TAG,"Mandatory tag not available for Action : "
	            				+action.get(PUBLISHER_KEY) +
	            		   		" for rule Key :"+mRuleInfo.getRuleKey());
	            		   break;
	            	   } 
	               }
	               //Look for All Conditions Mandatory tags
	        	   if(status){
	        		   if (mConditions.isEmpty()) {
	                	   Log.w(TAG,"Manual Rule : "+mRuleInfo.getRuleKey());
	                   }else{
		                   for (ConditionContainer condition : mConditions) {
		                	   if(!condition.areMandatoryTagsAvailable()){
		                		   status = false;
		                		   Log.e(TAG,"Mandatory tag not available for Condition : "
		                				+condition.get(PUBLISHER_KEY) +
		                		   		" for rule Key :"+mRuleInfo.getRuleKey());
		                		   break;
		                	   } 
		                   }
	                   }
	               }   
               }
           }

            if(! status){
                // add entry to the response map
                String errString = isBlacklisted? XML_BLACKLISTED_RULE:XML_MAND_TAGS_MISSING;
                if(mRuleInfo !=null){
                        Log.e(TAG,"Not valid : "+mRuleInfo.getRuleKey());
                        mRulePublisherResponseMap.put(mRuleInfo.getRuleKey(), errString);
                }else{
                        Log.e(TAG,"Not valid : "+mPubKey);
                        mRulePublisherResponseMap.put(mPubKey, errString);
                }
            }

           return status;
        }    
    }

    /** This class holds the actions related data with respect to a rule. 
     *<code><pre>
     * CLASS:
     *
     * RESPONSIBILITIES:
     *  It holds the actions related data with respet to rule
     *
     * COLLABORATORS:
     *  Smart Rules 
     *
     * USAGE:
     *  see each method
     *
     *</pre></code>
     */
    private static class ActionsContainer extends ArrayList<ActionContainer> {

        private static final long serialVersionUID = -6992410221926708205L;

    }

    /** This class holds the conditions related data with respect to a rule. 
     *<code><pre>
     * CLASS:
     *
     * RESPONSIBILITIES:
     *  It holds the conditions related data with respect to rule
     *
     * COLLABORATORS:
     *  Smart Rules 
     *
     * USAGE:
     *  see each method
     *
     *</pre></code>
     */
    private static class ConditionsContainer extends ArrayList<ConditionContainer> {

        private static final long serialVersionUID = -4001710925549283013L;

    }

    /** This class holds the RULEINFO related data with respect to a rule. 
     *<code><pre>
     * CLASS:
     *
     * RESPONSIBILITIES:
     *  It holds the RULEINFO related data with respect to rule
     *
     * COLLABORATORS:
     *  Smart Rules 
     *
     * USAGE:
     *  see each method
     *
     *</pre></code>
     */
    private static class RuleInfoContainer extends BaseContainer {

        private static final long serialVersionUID = -3481668012298156643L;
        
        private String parentRuleKey = null;

        RuleInfoContainer(Node ruleInfoNode) {
            super(ruleInfoNode);
        }

        RuleInfoContainer(Node ruleInfoNode, String parentKey) {
            super(ruleInfoNode);
            parentRuleKey = parentKey;
        }

        /** Converts the RuleInfo data into ContentValues format.
         *
         *
         * @return
         */
		ContentValues getContentValues(Context context) {
            RuleTuple tuple;

            // Get the Strings that needs to be translated
            // We need to replace all translatable strings in Name, Description and Suggested Reason
            // For example "Send Text to 12345" will be represented as "rulesxml_descSendText,12345"
            String ruleName = get(NAME);
            if (ruleName != null) {
                String[] nameArray = ruleName.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : nameArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                ruleName = buf.toString();
            }
            
            
            String ruleDesc  = getRuleDesc();

            if (ruleDesc != null) {
                String[] descArray = ruleDesc.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : descArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                ruleDesc = buf.toString();
            }

            String suggestedReason = get(SUGGESTED_REASON);
            if (suggestedReason != null) {
                String[] suggArray = suggestedReason.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : suggArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                suggestedReason = buf.toString();
            }

            String sugXml = get(SUGGESTION_FREEFLOW);
            if (LOG_DEBUG) Log.d(TAG,"Text to translate " + sugXml);
            String translatedSuggestedText = FileUtil.translateSuggestionText(context, sugXml);
            if (LOG_DEBUG) Log.d(TAG,"translatedSuggestedText returned " + translatedSuggestedText);
                        
            int type = getRuleSource();

            String parentKeyfromXml = get(PARENT_RULE_KEY);
            if (parentKeyfromXml != null) parentRuleKey = parentKeyfromXml;
            
            tuple = new RuleTuple(DEFAULT_ID,
                                  getIntValue(ENABLED, DEFAULT_ENABLED),
                                  getFloatValue(RANK, DEFAULT_RANK),
                                  getRuleKey(),
                                  getIntValue(ACTIVE, DEFAULT_ACTIVE),
                                  getIntValue(RULE_TYPE, DEFAULT_RULE_TYPE),
                                  type,
                                  getIntValue(COMMUNITY_RATING, DEFAULT_COMMUNITY_RATING),
                                  get(COMMUNITY_AUTHOR),
                                  getFlags(),
                                  ruleName,
                                  ruleDesc,
                                  getRuleKey(),
                                  get(CONDITIONS_LOGIC),
                                  get(INFERENCE_LOGIC),
                                  null, //Inference status - unused field
                                  getIntValue(SUGGESTED_STATE, DEFAULT_SUGGESTED_STATE),
                                  suggestedReason,
                                  getLifeCycle(),
                                  getIntValue(SILENT, DEFAULT_SILENT),
                                  null,
                                  translatedSuggestedText,
                                  getLongValue(LAST_ACTIVE_DATETIME, DEFAULT_TIME),
                                  getLongValue(LAST_INACTIVE_DATETIME, DEFAULT_TIME),
                                  getLongValue(CREATED_DATETIME, new Date().getTime()),
                                  getLongValue(LAST_EDITED_DATE_TIME, 0),
                                  getRuleIcon(),
                                  RuleTable.DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE,
                                  get(PUBLISHER_KEY),
                                  TableBase.Validity.VALID,
                                  getSuggestionUiIntent(),
                                  parentRuleKey,
                                  getLongValue(ADOPT_COUNT, RuleTable.DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE)
                                 );


            tuple.set_idToGenerateNewKeyOnInsert();
            return new RuleTable().toContentValues(tuple);
        }
        
        /**This methods helps to maps text value with   
         * the integer value for different types of rules
         * 
         * @return int - the rule source
         */
        private int getRuleSource(){
	        // Older tag name is SOURCE
	        int type = INVALID;
	
            String stringType = null;
            
            if ((type = getIntValue(SOURCE,INVALID)) == INVALID){
            	// New tag name is TYPE
            	if ( !Util.isNull(stringType = get(TYPE)) ){
            		//String comparison 
            		if (stringType.equalsIgnoreCase("suggestion")){
            			type = Source.SUGGESTED;
            		}else if (stringType.equalsIgnoreCase("inference")){
            			type = Source.INFERRED;
            		}else if (stringType.equalsIgnoreCase("sample")){
            			type = Source.FACTORY;
            		}else if (stringType.equalsIgnoreCase("user")){
            			type = Source.USER;
            		}else if (stringType.equalsIgnoreCase("child")){
                        type = Source.CHILD;
                    }
            	}
            }
            
            if (LOG_DEBUG) Log.d(TAG,"Source : " +type);

			return type;
        }
        

        /** This method helps to get the
         *  tag value from the KEY or
         *  IDENTIFIER tag
         *
         * @return - the rule key value
         */
        private String getRuleKey(){
    	
           String key = null;
    	   if (Util.isNull(key = get(KEY))){
    		   if (Util.isNull(key = get(IDENTIFIER))){
    			   Log.e(TAG,"Both tags for RuleKey is not there"); 
    		   }
           }
           
           if (LOG_DEBUG) Log.d(TAG,"KEY : " +key);
        	
            return key;
        }
        
        /** This method returns if a rule is enabled or not
         *
         * @return - true if the rule is enabled; false otherwise
         */
        private boolean isEnabled(){
            return getIntValue(ENABLED, DEFAULT_ENABLED) == 0 ? false : true;
        }
        
        
        /** This method helps to get the
         *  tag value from the DESCRIPTION or
         *  DESC tag
         * 
         * @return - rule description
         */
        private String getRuleDesc(){
	        String ruleDesc = null;
	        if (Util.isNull(ruleDesc = get(DESCRIPTION))){
	        	if (Util.isNull(ruleDesc = get(DESCRIPTION_NEW))){
	        		Log.e(TAG,"Both tags for Rule Desc is not there"); 
	        	}
	        }       
	        if (LOG_DEBUG) Log.d(TAG,"ruleDesc : "+ruleDesc);
	        return ruleDesc;
        }
        
        /** This method helps to get the
         *  tag value from the UI_INTENT or
         *  SUGGESTION_UI_INTENT tag
         * 
         * @return - rule description
         */
        private String getSuggestionUiIntent(){
	        // New tag for UI_INTENT is SUGGESTION_UI_INTENT
	        String uiIntent  = null;
	        if (Util.isNull(uiIntent = get(UI_INTENT))){
	        	if (Util.isNull(uiIntent = get(SUGGESTION_UI_INTENT))){
	        		if (LOG_DEBUG) Log.d(TAG,"Suggestion UI Intent is not present");
	        	}
	        }
	        if (LOG_DEBUG) Log.d(TAG,"UI_INTENT : " +uiIntent);
	        return uiIntent;
        }
        
        /** This method helps to get the
         *  tag value from the LIFECYCLE or
         *  SUGGESTION_TYPE tag
         * 
         * @return - rule description
         */
        private long getLifeCycle(){
	        // Older tag name is LIFECYCLE
	        long suggType = DEFAULT_LIFECYCLE;

	        if ((suggType = getLongValue(LIFECYCLE,INVALID)) == INVALID){
	        	// New tag name is SUGESTION_TYPE
	        	if ((suggType = getLongValue(SUGGESTION_TYPE,INVALID)) == INVALID){
	        		if (LOG_DEBUG) Log.d(TAG,"Both Lifecycle tags are not present");
	        		// Baseline behavior default LifeCycle is Never Expires
	        		suggType = 	DEFAULT_LIFECYCLE;
	        	}
	        }
	        return suggType;
        }
        
        /**
         * This method helps to parse the FLAGS xml tag and
         * in case if the Rule is published from RP then the
         * default value is SOURCE_LIST_VISIBLE - "s"
         *
         * @return String - FLAGS tag value
         */
        private String getFlags(){
	        String flags = null;
	        if (Util.isNull((flags = get(FLAGS)))){
			// This change is only needed if the XML is from RP
			// because it can throw only suggestions or samples
			if(mImportType == ImportType.RP_RULE){
			if (LOG_DEBUG) Log.d(TAG,"FLAGS is null hence assign default Value");
			flags = RuleTable.Flags.SOURCE_LIST_VISIBLE;
			}
	        }
	        if (LOG_DEBUG) Log.d(TAG,"FLAGS value :" +flags);
	        return flags;
        }

        /**
         * This method helps to parse the ICON tag and
         * if there is no ICON tag available then the default
         * ICON is considered. This is basically for Rule Level
         * icon.
         *
         * @return String - ICON tag value
         */

        private String getRuleIcon(){
	        String icon = null;
	        if (Util.isNull((icon = get(ICON)))){
			if (LOG_DEBUG) Log.d(TAG,"Rule ICON is null hence assign Default Icon");
			icon = DEFAULT_RULE_ICON;
	        }
	        return icon;
        }
        
        private boolean areMandatoryTagsAvailable(){
        	boolean status = false; 
        	
        	// For backward compatibility, till the entire
        	// rulesimporter.rules are moved to RP
        	if(mImportType != ImportType.RP_RULE){
        		
        		return true;
        	}
        	 //RULEINFO Level
            if (!Util.isNull(get(NAME)) &&
		        !Util.isNull(get(XmlConstants.PUBLISHER_KEY)) &&
         	    !Util.isNull(getRuleKey()) &&
         	    getRuleSource() != INVALID &&
		    !Util.isNull(getRuleDesc())){
			   if(getRuleSource() == Source.SUGGESTED &&
				  Util.isNull(getSuggestionUiIntent())){
				   Log.e(TAG,"Invalid Rule : Mandatory tags not " +
						"available for Suggestion : "+getRuleKey());
			   }else{
				   status = true;
			   }
            }else{
		   Log.e(TAG,"isManRuleInfoTagsAvail: Invalid Rule : Mandatory tags not " +
            		   		"available : "+getRuleKey());
            }
            
            return status;
        }


    }

    
    /** This class holds the condition related data with respect to a rule. 
     *<code><pre>
     * CLASS:
     *
     * RESPONSIBILITIES:
     *  It holds the condition related data with respect to rule
     *
     * COLLABORATORS:
     *  Smart Rules 
     *
     * USAGE:
     *  see each method
     *
     *</pre></code>
     */
    private static class ConditionContainer extends BaseContainer {

        private static final long serialVersionUID = 3397229440474328571L;

        ConditionContainer(Node conditionNode) {
            super(conditionNode);
        }

        /** Parses the Condition Node
         *
         */
        @Override
        protected void parse() {
            List<Node> children =  loadValues();
            // For nodes which have children
            for (Node child : children) {
                String name = child.getNodeName();
                put(name, getCdataValues(child).get(0));
            }
        }

        /** Converts the Condition data into ContentValues format.
         *
         * @return
         */
        ContentValues getContentValues(Context context) {
            ConditionTuple tuple;

            // Get the Strings that needs to be translated
            // We need to replace all translatable strings in Description and Suggested Reason
            // For example "Send Text to 12345" will be represented as "rulesxml_descSendText,12345"
            String ruleName = get(NAME);
            if (ruleName != null) {
                String[] nameArray = ruleName.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : nameArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                ruleName = buf.toString();
            }

            String ruleDesc = get(DESCRIPTION);
            if (ruleDesc != null) {
                String[] descArray = ruleDesc.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : descArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                ruleDesc = buf.toString();
            }

            String suggestedReason = get(SUGGESTED_REASON);
            if (suggestedReason != null) {
                String[] suggArray = suggestedReason.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : suggArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                suggestedReason = buf.toString();
            }

            String targetState = get(TARGET_STATE);
            if (targetState != null) {
                String[] stateArray = targetState.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : stateArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                targetState = buf.toString();
            }
            
            String config = null;
            
            if (Util.isNull(config = get(ACTIVITY_INTENT))){
            	if (Util.isNull(config = get(CONFIG))){  
            		if (LOG_DEBUG) Log.d(TAG,"CONFIG tag is not present");
            	}
            }
        	if (LOG_DEBUG) Log.d(TAG,"CONFIG is "+config);
        	
        	// New tag for MARKET_URL is DOWNLOAD_URL
            String url  = null;
            if (Util.isNull(url = get(MARKET_URL))){
            	if (Util.isNull(url = get(DOWNLOAD_URL))){
            		if (LOG_DEBUG) Log.d(TAG,"DOWNLOAD_URL tag is not present");
            	}
            }
            
            if (LOG_DEBUG) Log.d(TAG,"Url : "+url);
            
            // New tag for SYNTAX is VSENSOR_LOGIC
            String syntax  = null;
            if (Util.isNull(syntax = get(SYNTAX))){
            		if (LOG_DEBUG) Log.d(TAG,"SYNTAX tag is not present");
            }
            
            if (LOG_DEBUG) Log.d(TAG,"vsensor logic : "+syntax);
            
            String validity = get(VALIDITY);
            if(validity == null){
                if(LOG_DEBUG) Log.d(TAG, " Condition validity invalid");
                validity = DEFAULT_VALIDITY;
            }
            // This is for Backward compatibility where in the
            // ENABLED value was numerical value like 1 or 0 which 
            // is now changed to true or false.
            int enabled = DEFAULT_CONDITION_ENABLED;
            if((enabled = getIntValue(ENABLED,INVALID)) == INVALID){
                String stringEnabled = null;
                if (!Util.isNull(stringEnabled = get(ENABLED))){
                	enabled = (stringEnabled.equalsIgnoreCase("false")) ? 
                			 ConditionTable.Enabled.DISABLED : ConditionTable.Enabled.ENABLED;
                }else{
                    Log.w(TAG,"Default value identified for ENABLED ta for Condition *");
                    enabled = DEFAULT_CONDITION_ENABLED;
                }
            }
            
            if (LOG_DEBUG) Log.d(TAG,"Enabled tag value for condition "+enabled);
            
            tuple = new ConditionTuple(DEFAULT_ID,
                                       DEFAULT_FK,
                                       enabled,
                                       getIntValue(SUGGESTED_STATE, DEFAULT_SUGGESTED_STATE),
                                       suggestedReason,
                                       getIntValue(CONDITION_MET, DEFAULT_CONDITION_MET),
                                       get(PUBLISHER_KEY),
                                       getIntValue(MODAL, DEFAULT_MODAL),
                                       ruleName,
                                       config,
                                       targetState,
                                       ruleDesc,
                                       syntax,
                                       getLongValue(CREATED_DATETIME, new Date().getTime()),
                                       getLongValue(LAST_FAIL_DATETIME, DEFAULT_TIME),
                                       get(SWAP),
                                       get(ICON),
                                       config,
                                       validity,
                                       get(MARKET_URL)
                                      );

            // _id should be generated on insert
            tuple.set_idToGenerateNewKeyOnInsert();
            return new ConditionTable().toContentValues(tuple);
        }

        /**This method helps to find out whether the
         * mandatory tag for a Condition Publisher is 
         * available or not
         * 
         * @return boolean Mandatory tag available or not
         */
        private boolean areMandatoryTagsAvailable(){
		return (!Util.isNull(get(XmlConstants.PUBLISHER_KEY)) &&
				!Util.isNull(get(XmlConstants.NAME)));
        }


    }

    /** This class holds the action related data with respect to a rule. 
     *<code><pre>
     * CLASS:
     *
     * RESPONSIBILITIES:
     *  It holds the action related data with respet to rule
     *
     * COLLABORATORS:
     *  Smart Rules 
     *
     * USAGE:
     *  see each method
     *
     *</pre></code>
     */
    private static class ActionContainer extends BaseContainer {

        private static final long serialVersionUID = 5473440014924933655L;

        ActionContainer(Node actionNode) {
            super(actionNode);
        }

        /** Converts the Action data into ContentValues format.
         *
         * @return
         */
        ContentValues getContentValues(Context context) {
            ActionTuple tuple;


            // Get the Strings that nees to be translated
            // Name doesnt need to be translated since it is not used to display to the user
            // We need to replace all translatable strings in Description and Suggested Reason
            // For example "Send Text to 12345" will be represented as "rulesxml_descSendText,12345"
            String ruleName = get(NAME);

            if (LOG_DEBUG) Log.d(TAG, "ActionContainer getContentValues name is : " + ruleName);

            if (ruleName != null) {
                String[] nameArray = ruleName.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : nameArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                ruleName = buf.toString();
            }
            String ruleDesc = get(DESCRIPTION);
            if (ruleDesc != null) {
                String[] descArray = ruleDesc.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : descArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                ruleDesc = buf.toString();
            }

            String suggestedReason = get(SUGGESTED_REASON);
            if (suggestedReason != null) {
                String[] suggArray = suggestedReason.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : suggArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                suggestedReason = buf.toString();
            }


            String targetState = get(TARGET_STATE);
            if (targetState != null) {
                String[] stateArray = targetState.split(",");
                StringBuffer buf = new StringBuffer();
                for (String elem : stateArray) {
                    if (elem.startsWith(context.getString(R.string.rulesxml_prefix)))
                        buf.append(context.getString(context.getResources().getIdentifier(elem, "string", context.getPackageName())));
                    else
                        buf.append(elem);
                }
                targetState = buf.toString();
            }
            
            String config = null;

            if (!Util.isNull(config = get(URI_TO_FIRE_ACTION))) {
               if (LOG_DEBUG) Log.d(TAG, "getContentValues uriToFireAction is : " + config);
	           config = getTranslatedConfig(context,config);
            }else if (!Util.isNull(config = get(CONFIG))){
	           if (LOG_DEBUG) Log.d(TAG, "getContentValues config is : " + config);
	           config = getTranslatedConfig(context,config);
            } 

            if (LOG_DEBUG) Log.d(TAG, "Final config : " + config);
            
            String validity = get(VALIDITY);
            if(validity == null){
		        if(LOG_DEBUG) Log.d(TAG, " Action validity invalid");
            	validity = DEFAULT_VALIDITY;
            }

            // For not configured blocks config is null 
            String activityIntent = get(ACTIVITY_INTENT);
            
            if(Util.isNull(activityIntent)){
            	activityIntent = config;
            }
            
            if (LOG_DEBUG) Log.d(TAG," Action activityIntent : "+activityIntent);
            
            // New tag for MARKET_URL is DOWNLOAD_URL
            String url  = null;
            if (Util.isNull(url = get(MARKET_URL))){
            	if (Util.isNull(url = get(DOWNLOAD_URL))){
            		if (LOG_DEBUG) Log.d(TAG,"DOWNLOAD_URL is not present");
            	}
            }
            
            if (LOG_DEBUG) Log.d(TAG,"Url : "+url);
            
            // This is for Backward compatibility where in the
            // ENABLED value was numerical value like 1 or 0 which
            // is now changed to true or false.
            int enabled = DEFAULT_ACTION_ENABLED;
            if((enabled = getIntValue(ENABLED,INVALID)) == INVALID){
                String stringEnabled = null;
                if (!Util.isNull(stringEnabled = get(ENABLED))){
                	    enabled = (stringEnabled.equalsIgnoreCase("false")) ? 
                	    	ActionTable.Enabled.DISABLED : ActionTable.Enabled.ENABLED;
                }else{
                        Log.w(TAG,"Default value identified for ENABLED ta for Action *");
                                enabled = DEFAULT_ACTION_ENABLED;
                }
            }

            String publisherKey = get(PUBLISHER_KEY);
            if (mXmlDbVersion != SQLiteManager.INVALID_DATABASE_VERSION && 
                    mXmlDbVersion < 34) {
                // We need to convert Old Background sync actions to new Background data action
                if (LOG_DEBUG) Log.d(TAG, "Converting Sync Action to BGData Action");
                publisherKey = get(PUBLISHER_KEY);
                if (publisherKey.equals(Sync.SYNC_ACTION_KEY)) {
                    publisherKey = BackgroundData.BD_ACTION_KEY;
                    if (!Util.isNull(targetState) && 
                            (targetState.equalsIgnoreCase(BackgroundData.ENABLE) ||
                                    targetState.equalsIgnoreCase(Sync.State.ON))) {
                        config = BackgroundData.CONFIG_ENABLE;
                        ruleDesc = context.getString(R.string.bd_always);
                    } else if (!Util.isNull(targetState) && 
                            (targetState.equalsIgnoreCase(BackgroundData.DISABLE) ||
                                    targetState.equalsIgnoreCase(Sync.State.OFF))) {
                            config = BackgroundData.CONFIG_DISABLE;
                            ruleDesc = context.getString(R.string.bd_never);
                    }
                }
            }
                      
            tuple = new ActionTuple(DEFAULT_ID,
                                    DEFAULT_FK,
                                    enabled,
                                    getIntValue(ACTIVE, DEFAULT_ACTIVE),
                                    getIntValue(CONFLICT_WINNER, DEFAULT_CONFLICT_WINNER),
                                    getIntValue(EXIT_MODE, DEFAULT_EXIT_MODE),
                                    getIntValue(SUGGESTED_STATE, DEFAULT_SUGGESTED_STATE),
                                    suggestedReason,
                                    ruleDesc,
                                    publisherKey,
                                    FileUtil.getModalForPublisher(context,get(PUBLISHER_KEY)),
                                    ruleName,
                                    targetState,
                                    null, // Not used any more
                                    activityIntent,
                                    get(SYNTAX),
                                    getLongValue(LAST_FIRED_DATETIME, DEFAULT_TIME),
                                    get(FAILURE_MESSAGE),
                                    get(ICON),
                                    config,
                                    validity,
                                    url,
                                    get(CHILD_RULE_KEY)
                                   );
            tuple.set_idToGenerateNewKeyOnInsert();
            return new ActionTable().toContentValues(tuple);
        }
        
        /**This method helps to find out whether the
         * mandatory tag for an Action Publisher is 
         * available or not
         * 
         * @return boolean Mandatory tag available or not
         */
        private boolean areMandatoryTagsAvailable(){
		return (!Util.isNull(get(XmlConstants.PUBLISHER_KEY)) &&
				!Util.isNull(get(XmlConstants.NAME)));
        }
    }
    /**
     * This method gives the translated config
     * 
     * @param context
     * @param config
     * @return translated config
     */
    
    public static String getTranslatedConfig(Context context, String config){
    	
        if (LOG_DEBUG) Log.d(TAG,"getTranslatedConfig");
        
	    if (!Util.isNull(config)) {
	        try {
				Intent uri = Intent.parseUri(config, 0);
				if (uri != null) {
					String message = uri.getStringExtra(EXTRA_MESSAGE);
					if (LOG_DEBUG) Log.d(TAG, "getContentValues message is : " + message);
					if (message != null && message.startsWith(context.getString(R.string.rulesxml_prefix))) {
						message = context.getString(context.getResources().getIdentifier(message, "string", context.getPackageName()));
						if (LOG_DEBUG) Log.d(TAG, "getContentValues translated message is : " + message);
						uri.putExtra(EXTRA_MESSAGE, message);
						config = uri.toUri(0);
					}
					
					String smsText = uri.getStringExtra(EXTRA_SMS_TEXT);
					if (LOG_DEBUG) Log.d(TAG, "getContentValues smsText is : " + smsText);
					if (smsText != null && smsText.startsWith(context.getString(R.string.rulesxml_prefix))) {
						smsText = context.getString(context.getResources().getIdentifier(smsText, "string", context.getPackageName()));
						if (LOG_DEBUG) Log.d(TAG, "getContentValues translated message is : " + smsText);
						uri.putExtra(EXTRA_SMS_TEXT, smsText);
						config = uri.toUri(0);
					}
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
				Log.e(TAG, "Invalid URI? Should we quit?");
			}
	    }

		return config;
    }
    
    /**
     * This links the child (restored) with a Sample (Not Clone), 
     *
     *
     * @param context - Context
     * @param sampleRuleId - Rule ID of the Child sample rule
     * @param rule - Rule info of a rule
     * @return - new rule ID
     */
	public static long linkSampleAndChild(Context context, final long sampleRuleId, 
    						Rule rule, String sampleRuleKey){
    	
    	if (LOG_DEBUG) Log.i(TAG,"linkSampleAndChild");

        long newId = RuleTable.RuleType.DEFAULT;
        if(sampleRuleKey == null) {
        	// only fetch of the caller has passed in a null value for sampleRuleKey.
        	sampleRuleKey = RulePersistence.getRuleKeyForRuleId(context, sampleRuleId);
        }
        String[] columns = new String[] {RuleTableColumns.ADOPT_COUNT};
        Cursor ruleCursor = RulePersistence.getRuleCursor(context, sampleRuleKey, columns);

        if(ruleCursor == null) return newId;

        try{
            if(ruleCursor.moveToFirst()){
                int childCount = ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow(
                										RuleTableColumns.ADOPT_COUNT)); 
                
                ContentValues cv = rule.mRuleInfo.getContentValues(context);
                cv.remove(RuleTableColumns.ACTIVE);
                cv.remove(FLAGS);
                cv.put(RuleTable.Columns.ADOPT_COUNT, 0);
                cv.put(RuleTable.Columns.PARENT_RULE_KEY, sampleRuleKey);
                
                if (mImportType == ImportType.INFERRED_ACCEPTED){
                	//Rule is pushed from widget need to update the rule name
                	String ruleName = cv.getAsString(RuleTable.Columns.NAME);
                	if (LOG_DEBUG) Log.d(TAG,"Change the rule name "+ ruleName);
                	if(childCount > 0)
                		ruleName = ruleName + " " + Integer.toString(childCount+1);
                	if (LOG_DEBUG) Log.d(TAG,"rule name changed to "+ ruleName);
                	cv.put(RuleTable.Columns.NAME, ruleName);             	
                }
                
            	//Stable 6 upgraded to Stable 7 case
                if(sampleRuleKey.equals(rule.mRuleInfo.getRuleKey())){
                	
			String newRuleKey = RulePersistence.createClonedRuleKeyForSample(sampleRuleKey);
                	if (LOG_DEBUG) Log.i(TAG,"Change the child Key"+rule.mRuleInfo.getRuleKey() +
                									" to NewRuleKey :"+newRuleKey);
                	cv.put(RuleTable.Columns.KEY, newRuleKey);               	
                }
                
                // insert in Rule table
                Uri uri = context.getContentResolver().insert(Schema.RULE_TABLE_CONTENT_URI, cv);

                if(uri != null)
                	newId = Long.parseLong(Uri.parse(uri.toString()).getLastPathSegment());
                
                if(newId == RuleTable.RuleType.DEFAULT)
                    Log.e(TAG, "Sample Child Rule insertion failed for ruleKey: " + rule.mRuleInfo.getRuleKey());
                else{
                	RulePersistence.setAdoptCount(context, sampleRuleId, childCount + 1);
                } 
            }
        }catch(IllegalArgumentException e){
            Log.e(TAG, "addtheChildRule: Null cursor");
        } finally {
            ruleCursor.close();
        }

        return newId;
    }

}
