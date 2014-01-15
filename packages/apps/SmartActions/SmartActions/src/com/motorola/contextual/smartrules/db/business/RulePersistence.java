/*
 * @(#)RulePersistence.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/20 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTuple;
import com.motorola.contextual.smartrules.db.table.TupleBase;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
import com.motorola.contextual.smartrules.list.ListRowInterface;
import com.motorola.contextual.smartrules.publishermanager.RulesValidatorInterface;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.util.Util;

/** This class holds the handlers that deal with query, insert, update and delete on
 * 	Rule Table Columns.
 *
 *<code><pre>
 * CLASS:
 * 	 extends RuleTable
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for the DB related constants
 *
 * RESPONSIBILITIES:
 *   None.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class RulePersistence extends RuleTable implements Constants, DbSyntax {


    protected static final String TAG = RulePersistence.class.getSimpleName();

    /** fetches the rule only for the rule ID passed in. Does not fetch the
     *  Actions and Conditions associated with the Rule.
     * 
     * @param context - context
     * @param _id - rule ID in the rule table
     * @return - a rule table level Rule type object for the rule ID passed in.
     */
    public static Rule fetchRuleOnly(Context context, long _id) {
    	Rule rule = null;
    	RuleTuple ruleTuple = (RuleTuple) (new RuleTable().fetch1(context, _id));
    	if(ruleTuple != null) {
    		rule = new Rule(ruleTuple);
    	}
    	return rule;
    }
    
    /** fetches the rule only for the rule Key passed in. Does not fetch the
     *  Actions and Conditions associated with the Rule.
     * 
     * @param context - context
     * @param ruleKey - rule Key in the rule table
     * @return - a rule table level Rule type object for the rule key passed in.
     */
    public static Rule fetchRuleOnly(Context context, String ruleKey) {
        Rule rule = null;
        String whereClause = Columns.KEY + EQUALS + Q + ruleKey + Q;
        RuleTuple ruleTuple = (RuleTuple) (new RuleTable().fetch1(context, whereClause));
        if(ruleTuple != null) {
            rule = new Rule(ruleTuple);
        }
        return rule;
    }
    
    /** fetches the complete rule (Rule, Action, Condition and Condition Sensor)
     *  table contents for the rule ID passed in.
     * 
     * @param context - context
     * @param _id - rule ID in the rule table
     * @return - a Rule type for the rule ID passed in.
     */
    public static Rule fetchFullRule(Context context, long _id) {
    	
    	Rule rule = null;
    	RuleTuple ruleTuple = (RuleTuple) (new RuleTable().fetch1(context, _id));
    	if(ruleTuple != null) {
    		rule = new Rule(ruleTuple,
    					new ActionPersistence().fetch(context, _id),
    					new ConditionPersistence().fetch(context, _id));
		rule.setIconBlob(IconPersistence.getIconBlob(context, rule.getRuleIconId(),
    				rule.getPublisherKey(), rule.getIcon()));
    		if(rule != null && rule.getConditionList() != null) {
    			if(rule.getConditionList().getEnabledConditionsCount() == 0)
    				rule.setRuleType(RuleType.MANUAL);
    			else
    				rule.setRuleType(RuleType.AUTOMATIC);
    		}
    	}
    	return rule;
    }
    
    /** inserts the rule into the database and also inserts the
     *  action and conditions associated with the copied/cloned
     *  rule.
     *  
     * @param context - context
     * @param tuple - subclass tuple
     * @return -1 if not added or primary key (_id) of added record    
     */
    @Override
	public <T extends TupleBase> long insert(Context context, T tuple) {
 
    	long id = -1;
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".1");
    	synchronized (db) {
	        try {
	        		db.beginTransaction();
	            	// insert the rule
	            	id = insertRule(db, tuple);      
	        		db.setTransactionSuccessful();
	
	        } catch (Exception e) {
	            Log.e(TAG, e.toString());
	            e.printStackTrace();
	
	        } finally {
	        	db.endTransaction();
	            if(db != null)
	            	db.close(TAG+".1");
	        }
    	}
		return id;
	}



    /**
     * Use this method to update the Validity column of rule publisher
     * @param context - Context
     * @param rulePublisherKey -  publisher key for the rule
     * @param ruleKey - Rule Key
     * @param ruleValidity - the new validity value for the rule
     */
    public static void updateRuleValidity(final Context context, final String rulePublisherKey,
                                          final String ruleKey, final String ruleValidity) {
        ContentValues cv = new ContentValues();
        cv.put(RuleTableColumns.VALIDITY, ruleValidity);
        String where = null;
        if(rulePublisherKey != null) {
            where = RuleTableColumns.PUBLISHER_KEY + EQUALS + Q +  rulePublisherKey + Q +
                    AND +RuleTableColumns.KEY + EQUALS + Q + ruleKey + Q;
        } else {
            where = RuleTableColumns.KEY + EQUALS + Q + ruleKey + Q;
        }
        updateRule(context, cv, where);
    }

    /** inserts the rule into the database and also inserts the
     *  action and conditions associated with the copied/cloned
     *  rule.
     *
     * @param db - DB instance
     * @param tuple - subclass tuple
     * @return -1 if not added or primary key (_id) of added record
     */
    public <T extends TupleBase> long insertRule(SQLiteManager db, T tuple) {
        // Write the rule
        long id = super.insert(db, tuple);

        Rule rule = (Rule) tuple;

        rule.getActionList().setParentFKey(id);
        rule.getConditionList().setParentFKey(id);

        // Write Actions
        new ActionPersistence().insertList(db, rule.getActionList());

        // Write Conditions
        new ConditionPersistence().insertList(db, rule.getConditionList());

        return id;
    }

    /** invokes the provider to clone the rule
     *
     * @param context - context
     * @param parentRuleId - rule that needs to be cloned
     * @param clonedRuleKey - rule key for the cloned rule
     * @param clonedRuleName - rule name for the cloned rule
     * @param isCloneForCopy - is the rule clone called for a 
     * 							"Copy from existing rule" case
     * @return the rule ID inserted or -1
     */
    public static long cloneRule(final Context context, final long parentRuleId,
                                 final String clonedRuleKey, final String clonedRuleName,
                                 final boolean isCloneForCopy) {
    	
        long clonedRuleId = DEFAULT_RULE_ID;

        ContentValues contentValues = new ContentValues();
        contentValues.put(RuleTable.Columns._ID, parentRuleId);
        contentValues.put(RuleTable.Columns.NAME, clonedRuleName);
        contentValues.put(RuleTable.Columns.KEY, clonedRuleKey);
        contentValues.put(IS_CLONE_FOR_COPY_RULE, isCloneForCopy);
        Uri uri = context.getContentResolver().insert(Schema.RULE_CLONE_URI, contentValues);

        if(uri != null) {
        	clonedRuleId = Long.parseLong(Uri.parse(uri.toString()).getLastPathSegment());
			Rule rule = fetchRuleOnly(context, clonedRuleId);
			if(rule != null) {
				String rulePub = rule.getPublisherKey();
				String pkgName = context.getPackageName();
				if(rulePub != null) {
					pkgName = Util.getPackageNameForRulePublisher(context, rulePub);
				}
				byte[] iconBlob = IconPersistence.insertIcon(context, rule.getIcon(),
						pkgName, clonedRuleId).getIconBlob();
				rule.setIconBlob(iconBlob);
			}
        }


        return clonedRuleId;
    }
   
    /** inserts a row into the rule table
     * 
     * @param context - context
     * @param ruleTuple - rule tuple
     * @return - the uri of the inserted row
     */
    public static Uri insertRule(Context context, RuleTuple ruleTuple){
    	Uri key = context.getContentResolver().insert(Schema.RULE_TABLE_CONTENT_URI, new RuleTable().toContentValues(ruleTuple));
    	return key;
    }
    
    /** updates the rule table entry
     * 
     * @param context - context
     * @param updateValues - content values used to update the row
     * @param whereClause - where clause for the update
     * @return number of rows updated
     */
    public static int updateRule(Context context, ContentValues updateValues, String whereClause){
        return context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, updateValues, whereClause, null);
    }
    
    /** updates this rule in the DB.
     * 
     * @param context - context
     * @param tuple - subclass tuple
     * @return true if only one rule was updated else false
     */
    public <T extends TupleBase> boolean updateRule(Context context, T tuple) {
    	int rows = 0;	
    	SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".1");
    	synchronized (db) {
	        try {
        		db.beginTransaction();
            	// Update the rule
            	rows = super.update(db, tuple);
	            	
            	if(rows == 1) {
	            	Rule rule = (Rule) tuple;
	            	new ActionPersistence().updateList(db, rule.getActionList());
	            	new ConditionPersistence().updateList(db, rule.getConditionList());
            	} else {
            		Log.e(TAG, "More than one row updated at rule " +
            						"level - should not enter this state");
            	}
        		db.setTransactionSuccessful();
	        } catch (Exception e) {
	            Log.e(TAG, e.toString());
	            e.printStackTrace();	
	        } finally {
	        	db.endTransaction();
	            if(db != null)
	            	db.close(TAG+".1");
	        }
    	}
    	return (rows == 1);
    }
    
    /**
	 * Returns the cloned rule Name
	 * 
	 * @param ruleName - Rule Name
	 * @return - Rule Name
	 */
	public static String createClonedRuleName(String ruleName, int childCount){
	
	    if(childCount > 0) ruleName += BLANK_SPC + (++childCount);
	    return ruleName;
	}

	/**
	 * Returns the cloned rule Key for Sample
	 *
	 * @param parentRuleKey
	 * @return - rule key
	 */

	public static String createClonedRuleKeyForSample(String parentRuleKey){
	    return parentRuleKey + DOT + new Date().getTime()+"";
	}

	/**
	 * Returns the cloned rule Key for Copy Existing Rule
	 *
	 * @param parentRuleKey
	 * @return - rule key
	 */
	public static String createClonedRuleKey(String parentRuleKey){
	    return parentRuleKey.substring(0, parentRuleKey.lastIndexOf(".")) + DOT + new Date().getTime()+"";
	}

	/** deletes a rule from the database by deleting the entry from Rule, Action and Condition
     * 	Condition Sensor tables.
     *
     * @param context - context
     * @param _id - _id for the rule to be deleted.
     * @param name - name of the rule deleted
	 * @param key - key of the rule deleted
	 * @param writeToDebug - true means log to debug viewer, false means do not
     * @return - the number of rows deleted
     */
    public static int deleteRule(Context context, long _id, String name,
    							String key, boolean writeToDebug) {
    	// Changes related to CR IKSTABLE6-16442
    	// Mark the rule as disabled so that the trigger publishers
    	// can be notified to stop listening before the rule is deleted.
    	RulePersistence.markRuleAsDisabled(context, _id, false);
    	
    	String whereClause = Columns._ID + EQUALS + Q + _id + Q;
    	int rowsDeleted = context.getContentResolver().delete(Schema.RULE_TABLE_CONTENT_URI, whereClause, null);
    	if(LOG_DEBUG) Log.d(TAG, "rowsDeleted = "+rowsDeleted);
    	if(rowsDeleted != 1)
    		Log.e(TAG, "More than one rule deleted for whereClause "+whereClause);

    	// Let the widget know that this rule has been deleted
    	Intent intent = new Intent(RULE_DELETED_MESSAGE);
    	intent.putExtra(Columns.KEY, key);
    	context.sendBroadcast(intent, SMART_RULES_PERMISSION);
    	
    	if(writeToDebug)
    		DebugTable.writeToDebugViewer(context, DebugTable.Direction.OUT, null, name, key,
    						SMARTRULES_INTERNAL_DBG_MSG, null, RULE_DELETED_STR,
    						Constants.PACKAGE, Constants.PACKAGE); 		
    	return rowsDeleted;
    }

    /** deletes the child rule associated with the child rule key.
     * 
     * @param context - context
     * @param childRuleKey child rule key
     * @return - the number of child rules deleted
     */
    public static int deleteChildRule(Context context, String childRuleKey) {
    	String whereClause = Columns.KEY + EQUALS + Q + childRuleKey + Q;
    	int rowsDeleted = 
    		context.getContentResolver().delete(Schema.RULE_TABLE_CONTENT_URI, 
    											whereClause, null);
    	
    	return rowsDeleted;
    }
    
    /** disables all the user visible rules in the DB.
     * 
     * @param context - context
     */
    public static void disableAllRules(Context context) {
		// Clear the notifications
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIF_ID);
        
        // Fire the default record action values
		ActionPersistence.fireDefaultRecordActions(context);
		
		// Fire all the stateful actions
        ActionPersistence.fireAllStatelessActionsDisabled(context);

        // Launch ModeAd with list of active rules
        unsubscribeFromConditionPublishers(context);
        
        // Mark all the user visible rules to disabled
        RulePersistence.markRulesInactiveInDb(context);

        // Mark all the actions of user visible rules to inactive
        ActionPersistence.markActionsInactiveInDb(context);


    }
    
    /**
     * Starts the Mode AD to send cancel request to conditions of all rule keys. 
     * This method is invoked when user selects Disable All option.
     * @param context - context
     */
    public static void unsubscribeFromConditionPublishers(Context context) {
		if(LOG_DEBUG) Log.d(TAG, " Launch ModeAd to send cancel to all enabled publisher keys");
	    ArrayList<String> configPubKeyList = new ArrayList<String>();

	    // Query all the config + pub keys from distinct condition view
	    ContentResolver cr = context.getContentResolver();
	    Cursor cursor = null;
	    try {

	        cursor = cr.query(Schema.DISTINCT_CONDITION_VIEW_CONTENT_URI, new String[] {
				          ConditionTable.Columns.CONDITION_CONFIG,
	                          ConditionTable.Columns.CONDITION_PUBLISHER_KEY}, null, null, null);
	        if(cursor != null) {
	            if(cursor.moveToFirst()) {
	                if(LOG_DEBUG) Log.d(TAG, "Extracting distinct config and pub keys ");
	                do {
	                    String config = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_CONFIG));
	                    String pubKey = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));
	                    configPubKeyList.add(config + COMMA + pubKey);
	                    if(LOG_DEBUG) Log.d(TAG,"Config: " + config + " and pubkey : " + pubKey);
	                } while (cursor.moveToNext());
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if(cursor != null) {
	            cursor.close();
	        }
	    }
        // Launch ModeAD to send 'Cancel' to Condition publishers for all config + pub key pairs
	    // in the list.
        Intent deleteIntent = new Intent(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR);
        deleteIntent.putExtra(EXTRA_LAUNCH_REASON, DISABLE_ALL);
        deleteIntent.putExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST, configPubKeyList);
        context.sendBroadcast(deleteIntent);
    }


    /** Updates the rule table and action table columns accordingly
     *
     * @param context - context
     * @param _id - rule table ID
     * @param isArriving - true if arriving into the rule and false if leaving the rule
     * @param disableRule - true then mark rule as disabled and false mean enable
     */
    public static void updateDatabaseTables(final Context context, final long _id, 
   										final boolean isArriving, final boolean disableRule) {
    	if(LOG_DEBUG) Log.d(TAG, "updateDatabaseTables for "+_id+" isArriving = "+isArriving+" disableRule "+disableRule);
    	updateRuleTable(context, _id, isArriving, disableRule);
    	ActionPersistence.updateActionTable(context, _id, isArriving);
   	}

   	/** Updates the rule table columns
   	 *
   	 * @param context - context
   	 * @param _id - rule table ID
   	 * @param isArriving - true if arriving into the rule and false if leaving the rule
   	 * @param disableRule - true then mark rule as disabled and false mean enable
   	 */
   	public static void updateRuleTable(final Context context, final long _id, 
   									final boolean isArriving, final boolean disableRule) {
   		String whereClause = Columns._ID + EQUALS + Q + _id + Q;
   		ContentValues contentValues = new ContentValues();

   		if(isArriving) {
   			contentValues.put(Columns.LAST_ACTIVE_DATE_TIME, new Date().getTime());
   			contentValues.put(Columns.ACTIVE, Active.ACTIVE);
   			contentValues.put(Columns.ENABLED, Enabled.ENABLED);
   		} else {
   			contentValues.put(Columns.LAST_INACTIVE_DATE_TIME, new Date().getTime());
   			contentValues.put(Columns.ACTIVE, Active.INACTIVE);
   			if(disableRule)
   				contentValues.put(Columns.ENABLED, Enabled.DISABLED);
   		}
       
   		try {
   			context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, contentValues, whereClause, null);
   		} catch (Exception e) {
   			Log.e(TAG, "Update to rule table failed");
   			e.printStackTrace();
   		}
	}

    /** Handles the click of rule icon or disable from the contextual menu based on rule type and state of the rule
     *  and sets the appropriate state in the DB.
     *
     * @param context - context
     * @param _id - rule ID in the Rule Table
     * @param callbackIntent
     */
    public static void toggleRuleStatus(final Context context, final long _id){
    	//call the new api version supporting a Intent that can be broadcast back
    	toggleRuleStatus(context, _id, null);
    }
    
    /** Handles the click of rule icon or disable from the contextual menu based on rule type and state of the rule
     *  and sets the appropriate state in the DB.
     *
     * @param context - context
     * @param _id - rule ID in the Rule Table
     * @param callbackIntent
     */
    public static void toggleRuleStatus(final Context context, final long _id, Intent callbackIntent) {
        if(LOG_DEBUG) Log.d(TAG, "toggleRuleStatus: Rule ID is "+_id);

        Cursor ruleCursor = null;
        try {
            String whereClause = Columns._ID + EQUALS + Q + _id + Q;
            ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, null);

            if(ruleCursor != null) {
                if(ruleCursor.moveToFirst()) {
                    int ruleType = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.RULE_TYPE));

                    if(ruleType == RuleTable.RuleType.MANUAL) {
                        toggleManualRuleStatus(context, _id, ruleCursor, callbackIntent);
                    } else if(ruleType == RuleTable.RuleType.AUTOMATIC) {
                        toggleAutomaticRuleStatus(context, _id, ruleCursor, callbackIntent);
                    }
                }
                else
                    Log.e(TAG, "ruleCursor.moveToFirst() for ruleID "+_id);
            }
            else
                Log.e(TAG, "ruleCursor is null for ruleID "+_id);
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH+" for ruleID"+_id);
            e.printStackTrace();

        } finally {
            // Close the cursor
            if(ruleCursor != null && !ruleCursor.isClosed())
                ruleCursor.close();
        }
    }

    /** handles the status changes for manual rules
     *	Newer version of the api available with added callback Intent param
     * @param context - context
     * @param _id - rule ID in the Rule Table
     * @param ruleCursor - rule cursor for the rule ID in the Rule Table
     */
    public static void toggleManualRuleStatus(final Context context, final long _id, final Cursor ruleCursor) {
    	toggleManualRuleStatus(context,  _id, ruleCursor, null);
    }
    
    /** handles the status changes for manual rules
     *
     * @param context - context
     * @param _id - rule ID in the Rule Table
     * @param ruleCursor - rule cursor for the rule ID in the Rule Table
     */
    public static void toggleManualRuleStatus(final Context context, final long _id, 
    				final Cursor ruleCursor, Intent callbackIntent) {
        boolean enabled = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.ENABLED)) == RuleTable.Enabled.ENABLED;
        String ruleKey = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.KEY));
        String ruleName = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.NAME));
        String ruleIcon = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.ICON));
        String debugString = "";

        if(enabled) { // rule is enabled - moving it to disabled state
            if(LOG_DEBUG) Log.d(TAG, "Moving a manual rule from active state to disabled state");
            debugString = MAN_ACTIVE_TO_DISABLED;
            handleRuleInactivation(context, _id, ruleIcon, ruleKey, ruleName, false, true, callbackIntent);

        } else { // rule is disabled - activate the rule and fire actions
            if(LOG_DEBUG) Log.d(TAG, "Moving a manual rule from disabled state to active state");
            debugString = MAN_DISABLED_TO_ACTIVE;
            markRuleAsEnabled(context, _id);
            ActionPersistence.fireManualRuleActions(context, _id, ruleKey, ruleName);
            updateDatabaseTables(context, _id, true, true);

            int ruleIconResId = 0;
            if(ruleIcon != null)
                ruleIconResId = context.getResources().getIdentifier(ruleIcon, "drawable", context.getPackageName());
            Util.sendMessageToNotificationManager(context, ruleIconResId);
        }
        DebugTable.writeToDebugViewer(context, DebugTable.Direction.OUT, null, ruleName, ruleKey,
        		SMARTRULES_INTERNAL_DBG_MSG, null, debugString,
        		Constants.PACKAGE, Constants.PACKAGE);
    }

    /** handles the status changes for automatic rules
     *	Newer version of the api available with added callback Intent param
     * @param context - context
     * @param _id - rule ID in the Rule Table
     * @param ruleCursor - rule cursor for the rule ID in the Rule Table
     */
    public static void toggleAutomaticRuleStatus(final Context context, final long _id, final Cursor ruleCursor){
    	//call the new api version supporting a Intent that can be broadcast back
    	toggleAutomaticRuleStatus(context, _id, ruleCursor, null);
    }

    /** handles the status changes for automatic rules
     *
     * @param context - context
     * @param _id - rule ID in the Rule Table
     * @param ruleCursor - rule cursor for the rule ID in the Rule Table
     * @param callbackIntent
     */
    public static void toggleAutomaticRuleStatus(final Context context, final long _id, final Cursor ruleCursor, Intent callbackIntent) {
        boolean enabled = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.ENABLED)) == RuleTable.Enabled.ENABLED;
        boolean active = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.ACTIVE)) == RuleTable.Active.ACTIVE;
        String ruleKey = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.KEY));
        String ruleName = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.NAME));
        String ruleIcon = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.ICON));
        String debugString = "";

        if(enabled) {
            if(active) { // rule is enabled and active - so disable after reverting actions
                if(LOG_DEBUG) Log.d(TAG, "Rule is active - moving it to disabled state");
                debugString = "Automatic Rule : Active to Disabled";
                handleRuleInactivation(context, _id, ruleIcon, ruleKey, ruleName, false, true, callbackIntent);
            }
            else { // rule is enabled and inactive - so disable only
                if(LOG_DEBUG) Log.d(TAG, "Rule is inactive - moving it to disabled state");
                debugString = "Automatic Rule : Ready to Disabled";
                ActionPersistence.updateActionTable(context, _id, false);
                markRuleAsDisabled(context, _id, false, callbackIntent);
            }
        }
        else {
        	if(LOG_DEBUG) Log.d(TAG, "Moving from disabled to ready state");
        	// rule was disabled so enable it
        	markRuleAsEnabled(context, _id);
        	debugString = "Automatic Rule : Disabled to Ready";
        }
        DebugTable.writeToDebugViewer(context, DebugTable.Direction.OUT, null, ruleName, ruleKey,
        		SMARTRULES_INTERNAL_DBG_MSG, null, debugString,
        		Constants.PACKAGE, Constants.PACKAGE);
    }
    
    /** Handles the rule inactivation of a manual or an automatic rule by performing
     * 	conflict resolution.
     * Newer version of the api available with added callback Intent param
     * @param context - context
     * @param _id - rule ID
     * @param ruleIcon - icon that is used for the rule
     * @param ruleKey - rule key
     * @param ruleName - rule name
     */
    public static void handleRuleInactivation(final Context context, final long _id,
            final String ruleIcon, final String ruleKey,
            final String ruleName, final boolean isRuleDeleted,
            final boolean isRuleActiveToDisabled) {
    	//call the new api version supporting a Intent that can be broadcast back
    	handleRuleInactivation( context,  _id,
                 ruleIcon,  ruleKey,
                 ruleName,  isRuleDeleted,
                 isRuleActiveToDisabled, null);
    }

    /** Handles the rule inactivation of a manual or an automatic rule by performing
     * 	conflict resolution.
     *
     * @param context - context
     * @param _id - rule ID
     * @param ruleIcon - icon that is used for the rule
     * @param ruleKey - rule key
     * @param ruleName - rule name
     * @param isRuleDeleted - is this handler invoked when rule is deleted
     * @param callbackIntent
     */
    public static void handleRuleInactivation(final Context context, final long _id,
            final String ruleIcon, final String ruleKey,
            final String ruleName, final boolean isRuleDeleted,
            final boolean isRuleActiveToDisabled, Intent callbackIntent) {
    	class ThreadWithParam extends Thread{
    		Intent callbackIntent = null;
    		ThreadWithParam(Intent callbackIntent){
    			this.callbackIntent = callbackIntent;
    		}
            public void run() {
                Cursor actionCursor = null;
                try {
                    String whereClause = ActionTable.Columns.PARENT_FKEY + EQUALS + Q + _id + Q;
                    actionCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);
                    if(actionCursor != null) {
                        if(actionCursor.moveToFirst() && actionCursor.getCount() > 0) {
                            if(LOG_DEBUG) Log.d(TAG, "Calling to check conflicts");
                            ActionPersistence.checkForConflictsAndFireActions(context, actionCursor, _id, ruleKey, ruleName);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, PROVIDER_CRASH);
                    e.printStackTrace();
                } finally {
                    if(actionCursor !=  null && !actionCursor.isClosed())
                        actionCursor.close();

                    if(LOG_DEBUG) Log.d(TAG, "isRuleDeleted = "+isRuleDeleted);
                    // Moved this block of code into the finally block from the try block after checkForConflictsAndFireActions() handler
                    // call. The reason being a rule can have no conditions or actions and could still be activated in the current design
                    // and in that case when a rule is deleted or inactivated then the further processing would not be done. Hence the
                    // code is moved to this place.
                    if(isRuleDeleted) {
                        RulePersistence.deleteRule(context, _id, ruleName, ruleKey, true);
                    } else {
                        ActionPersistence.updateActionTable(context, _id, false);
                        if(isRuleActiveToDisabled) // rule is moving from active state to disabled state
                            markRuleAsDisabled(context, _id, isRuleActiveToDisabled, callbackIntent);
                        else // rule is moving from active state to disabled state (user pressed on the rule icon)
                            updateRuleTable(context, _id, isRuleActiveToDisabled, true);
                        // Mark all actions for the rule inactive
                    }

                    int ruleIconResId = 0;
                    if(ruleIcon != null)
                        ruleIconResId = context.getResources().getIdentifier(ruleIcon, "drawable", context.getPackageName());
                    Util.sendMessageToNotificationManager(context, ruleIconResId);
                }
            }
        }
        
        ThreadWithParam thread = new ThreadWithParam(callbackIntent);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }
    
    /** Returns the default rule ID from the database
     *
     * @param context - context
     * @return - returns the default rule ID if it exists else -1
     */
    public static long getDefaultRuleId(final Context context) {
        long ruleId = DEFAULT_RULE_ID;
        Cursor defaultRuleCursor = null;
        try {
            String whereClause = Columns.KEY + EQUALS + Q + DEFAULT_RULE_KEY + Q;
            defaultRuleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, null);
            if(defaultRuleCursor != null) {
                if(defaultRuleCursor.moveToFirst()) {
                    ruleId = defaultRuleCursor.getLong(defaultRuleCursor.getColumnIndex(Columns._ID));
                    if(LOG_DEBUG) Log.d(TAG, "defaultRuleId = "+ruleId);
                } else {
                    Log.e(TAG, "getDefaultRuleId(): defaultRuleCursor.moveToFirst() failed");
                }
            } else {
                Log.e(TAG, "getDefaultRuleId(): defaultRuleCursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH+" query fetching the default rule");
            e.printStackTrace();
        } finally {
            if(defaultRuleCursor != null && !defaultRuleCursor.isClosed())
                defaultRuleCursor.close();
        }
        return ruleId;
    }

    /** Marks the rule as disabled in the rule table.
     * Newer version of api exists with added param a callback Intent
     * @param context - context
     * @param _id - rule ID
     * @param ruleWasActive - true indicates if the rule is being moved from active to disabled state else false.
     */   
    public static void markRuleAsDisabled(final Context context, final long _id, boolean ruleWasActive){
    	markRuleAsDisabled(context,  _id,  ruleWasActive, null);
    }
    
    /** Marks the rule as disabled in the rule table.
     *
     * @param context - context
     * @param _id - rule ID
     * @param ruleWasActive - true indicates if the rule is being moved from active to disabled state else false.
     * @param callbackIntent
     */
    public static void markRuleAsDisabled(final Context context, final long _id, 
    										boolean ruleWasActive, Intent callbackIntent) {
    	if(LOG_DEBUG) Log.d(TAG, "Entering markRuleAsDisabled for rule "+_id);
        String whereClause = Columns._ID + EQUALS + Q + _id + Q;
        ContentValues contentValues = new ContentValues();
        contentValues.put(Columns.ACTIVE, Active.INACTIVE);
        contentValues.put(Columns.ENABLED, Enabled.DISABLED);
        if(ruleWasActive)
            contentValues.put(Columns.LAST_INACTIVE_DATE_TIME, new Date().getTime());
        context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, contentValues, whereClause, null);

        int ruleType = RulePersistence.getColumnIntValue(context, _id, RuleTable.Columns.RULE_TYPE);
        if(ruleType != RuleTable.RuleType.MANUAL) {
	        // Launch ModeAD to send 'Cancel' to Condition publishers of this rule
	       ArrayList<String> configPubKeyList = getConfigPubKeyListForDeletion(context, _id);
	       Intent deleteIntent = new Intent(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR);
	       deleteIntent.putExtra(EXTRA_LAUNCH_REASON, RULE_DELETED);
	       deleteIntent.putExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST, configPubKeyList);
	       //Add an extra for the explicit return Intent we expect upon completion of Mode Ad - deletion process
	       if(callbackIntent != null) {
	            deleteIntent.putExtra(EXTRA_CALLBACK_INTENT, callbackIntent.toUri(0));
	       }
	       context.sendBroadcast(deleteIntent);
        } else if (callbackIntent != null){
		if(LOG_DEBUG) Log.d(TAG, " Special case where manual rule is changing to Automatic rule");
             context.sendBroadcast(callbackIntent);
        }

        ConditionPersistence.notifyConditionPublishers(context, _id, false);
    }
    
    /**
     * This method returns an array list of Enabled rule keys.
     * @param context Context
     * @param _id Rule id
     * @return List of condition config and publisher keys
     */
    public static ArrayList<String> getEnabledRuleKeyList(Context context){
        ArrayList<String> ruleKeyList = new ArrayList<String>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            String whereClause = Columns.ENABLED + EQUALS + Q + Enabled.ENABLED + Q
                    + AND + Columns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q;

            cursor = cr.query(Schema.RULE_TABLE_CONTENT_URI, new String[] { Columns.KEY }, 
                    whereClause, null, null);

            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        String ruleKey = cursor.getString(cursor.getColumnIndex(Columns.KEY));
                        ruleKeyList.add(ruleKey);
                        if(LOG_DEBUG) Log.d(TAG,"getEnabledRuleKeyList: Adding " + ruleKey + " to list");
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return ruleKeyList;
    }

    /**
     * This method queries all the conditions for a given rule id and extracts config +
     * publisher keys for those conditions and returns them as an array list.
     * @param context Context
     * @param _id Rule id
     * @return List of condition config and publisher keys
     */
    public static ArrayList<String> getConfigPubKeyListForDeletion(Context context, long _id) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        ArrayList<String> configPubKeyList = null;
        try {
            String whereClause = Columns._ID + EQUALS + Q + _id + Q;

            cursor = cr.query(Schema.RULE_CONDITION_VIEW_CONTENT_URI, new String[] { ConditionTable.Columns.CONDITION_CONFIG,
                              ConditionTable.Columns.CONDITION_PUBLISHER_KEY, RuleTable.Columns.KEY
                                                                                   }, whereClause, null, null);

            if(cursor != null) {
                if(cursor.moveToFirst()) {
			configPubKeyList = new ArrayList<String>(cursor.getCount());
                    do {
                        String config = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_CONFIG));
                        String pubKey = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));

                        configPubKeyList.add(config + COMMA + pubKey);
                        if(LOG_VERBOSE) Log.v(TAG,"Config: " + config + " and pubkey : " + pubKey);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return configPubKeyList;
    }
    /** Enables a disabled rule in the DB
     *
     * @param context - context
     * @param _id - rule ID
     */
    public static void markRuleAsEnabled(final Context context, final long _id) {
    	if(LOG_DEBUG) Log.d(TAG, "Entering markRuleAsEnabled for rule "+_id);
        String whereClause = Columns._ID + EQUALS + Q + _id + Q;
        ContentValues contentValues = new ContentValues();
        contentValues.put(Columns.ENABLED, Enabled.ENABLED);
        context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, contentValues, whereClause, null);

        String ruleKey = getRuleKeyForRuleId(context, _id);
        RulesValidatorInterface.launchModeAd(context, ruleKey, 
                ImportType.IGNORE, 
                RuleTable.Validity.VALID, 
                fetchRuleOnly(context, ruleKey),
                isRulePsuedoManualOrManual(context, ruleKey));
        ConditionPersistence.notifyConditionPublishers(context, _id, true);
    /*    // Launch RulesValidator
        Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
        ArrayList<String> ruleList = new ArrayList<String>();
        ruleList.add(getRuleKeyForRuleId(context, _id));
        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(_id));
        context.sendBroadcast(rvIntent);*/
    }

    /** Checks if the next rule in the conflicting actions is default record.
     *
     * @param context - context
     * @param nextRuleKey - rule key of the action which is next on the conflicting actions cursor
     * @return - true if the record is default else false
     */
    public static boolean isNextRuleDefaultRule(final Context context, final String nextRuleKey) {

        boolean result = false;
        if(nextRuleKey != null && nextRuleKey.equals(DEFAULT_RULE_KEY))
        	result = true;
        return result;
    }

    /** Marks the active column to inactive as the user has opted out off smart rules.
     *
     * @param context - context
     * 
     * @return - cursor with deactivated rules
     */
    public static Cursor markRulesInactiveInDb(final Context context) {
    	Cursor cursor = null;
        try {
            // Get all enabled rules to be returned
            String whereClause = Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + WILD + Q
                                 + AND + Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + WILD + Q
                                 + AND + Columns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q
                                 + AND + Columns.ENABLED + EQUALS + Enabled.ENABLED;

            cursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI,
                    null, whereClause, null, null);
            
         // Mark active rules as disabled and update the last inactive time column.
           whereClause = Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + WILD + Q
                                 + AND + Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + WILD + Q
                                 + AND + Columns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q
                                 + AND + Columns.ACTIVE + EQUALS + Active.ACTIVE;

            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.ACTIVE, Active.INACTIVE);
            contentValues.put(Columns.ENABLED, Enabled.DISABLED);
            contentValues.put(Columns.LAST_INACTIVE_DATE_TIME, new Date().getTime());
            context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, contentValues, whereClause, null);

            // Mark the inactive rules as disabled (Difference between the above request is that the
            // LAST_INACTIVE_DATE_TIME timestamp should not be updated for already inactive rules.
            whereClause = Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + WILD + Q
                          + AND + Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + WILD + Q
                          + AND + Columns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q
                          + AND + Columns.ACTIVE + EQUALS + Active.INACTIVE;

            contentValues = new ContentValues();
            contentValues.put(Columns.ENABLED, Enabled.DISABLED);
            context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, contentValues, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH + " update failed");
            e.printStackTrace();
        }
        return cursor;
    }

    /** Returns as string that contains comma separated rules names of all active rules.
     *
     * @param context - context
     * @return - string that contains comma separated rules names of all active rules
     */
    public static String getActiveRulesString(final Context context) {
        String content = null;
        Cursor ruleCursor = null;

        try {
            String whereClause = Columns.ACTIVE + EQUALS + Q + Active.ACTIVE + Q
                                 + AND + Columns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q
                                 + AND + Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + WILD + Q
                                 + AND + Columns.SILENT + EQUAL + Q + Silent.TELL_USER + Q;
            String sortOrder = Columns.LAST_ACTIVE_DATE_TIME + BLANK_SPC + DESC;

            ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, sortOrder);

            if(ruleCursor != null) {
                if(ruleCursor.moveToFirst()) {
                    if(LOG_DEBUG) Log.d(TAG, "rule cursor is not null and total active rules = "+ruleCursor.getCount());
                    content = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.NAME));
                    if(ruleCursor.moveToNext()) {
                        for(int i = 1; i < ruleCursor.getCount(); i++) {
                            content =  content.concat(COMMA_SPACE);
                            content = content.concat(ruleCursor.getString(ruleCursor.getColumnIndex(Columns.NAME)));
                            ruleCursor.moveToNext();
                        }
                    }
                } else {
                    Log.e(TAG, "getActiveRulesString(): move to first failed");
                }
            } else {
                Log.e(TAG, "getActiveRulesString(): rule cursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH+" query for rule table");
            e.printStackTrace();
        } finally {
            if(ruleCursor != null && !ruleCursor.isClosed())
                ruleCursor.close();
        }
        return content;
    }

    /** fetches and returns the rule ID from the rule table for the rule key passed in.
     *
     * @param context - context
     * @param ruleKey - rule key in the rule table
     * @return - the rule ID of the rule that matches the rule key in the table
     */
    public static long getRuleIdForRuleKey(final Context context, final String ruleKey) {
        long ruleId = DEFAULT_RULE_ID;
        String whereClause = Columns.KEY + EQUALS + Q + ruleKey + Q;
        Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, new String[] {RuleTable.Columns._ID},
                whereClause, null, null);
        if(ruleCursor != null) {
            try {
                if(ruleCursor.moveToFirst()) {
                    ruleId = ruleCursor.getLong(ruleCursor.getColumnIndex(Columns._ID));
                } else
                    Log.e(TAG, "ruleCursor.moveToFirst() failed for "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(!ruleCursor.isClosed())
                    ruleCursor.close();
            }
        } else
            Log.e(TAG, "rule cursor fetched is null for whereClause "+whereClause);

        if(LOG_DEBUG) Log.d(TAG, "returning ruleId as "+ruleId+" for ruleKey "+ruleKey);
        return ruleId;
    }

    /** fetches and returns the rule Icon from the rule table for the rule key passed in.
    *
    * @param context - context
    * @param ruleKey - rule key in the rule table
    * @return - the rule Icon of the rule that matches the rule key in the table
    */
   public static String getRuleIconForRuleKey(final Context context, final String ruleKey) {
       String ruleIcon = null;
       String whereClause = Columns.KEY + EQUALS + Q + ruleKey + Q;
       Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, new String[] {RuleTable.Columns._ID},
               whereClause, null, null);
       if(ruleCursor != null) {
           try {
               if(ruleCursor.moveToFirst()) {
            	   ruleIcon = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.ICON));
               } else
                   Log.e(TAG, "ruleCursor.moveToFirst() failed for "+whereClause);
           } catch (Exception e) {
               Log.e(TAG, PROVIDER_CRASH);
               e.printStackTrace();
           } finally {
               if(!ruleCursor.isClosed())
                   ruleCursor.close();
           }
       } else
           Log.e(TAG, "rule cursor fetched is null for whereClause "+whereClause);

       if(LOG_DEBUG) Log.d(TAG, "returning ruleIcon as "+ruleIcon+" for ruleKey "+ruleKey);
       return ruleIcon;
   }
   
    /** fetches and returns the rule key from the rule table for the rule ID passed in.
    *
    * @param context - context
    * @param ruleId - rule ID in the rule table
    * @return - the rule Key of the rule that matches the rule ID in the table
    */
    public static String getRuleKeyForRuleId(final Context context, final long ruleId) {
       String ruleKey = null;

       final String whereClause = RuleTable.Columns._ID + EQUALS + ruleId;
       Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, new String[] {RuleTable.Columns.KEY},
               whereClause, null, null);

       if(ruleCursor == null){
           Log.e(TAG, "rule cursor fetched is null for whereClause "+whereClause);
       } else {
           try{
               if(ruleCursor.moveToFirst()) {
                   ruleKey = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.KEY));
               } else
                   Log.e(TAG, "ruleCursor.moveToFirst() failed for "+whereClause);
           }catch(Exception e){
               Log.e(TAG, PROVIDER_CRASH);
               e.printStackTrace();
           }finally{
               ruleCursor.close();
           }
       }

        if(LOG_DEBUG) Log.d(TAG, "returning ruleKey as "+ruleKey+" for ruleId "+ruleId);
        return ruleKey;
    }

    /** fetches and returns the rule flags from the rule table for the rule key passed in.
    *
    * @param context - context
    * @param ruleKey - rule key in the rule table
    * @return - the rule flags of the rule that matches the rule key in the table
    */
    public static String getRuleFlagsForRuleKey(final Context context, final String ruleKey) {
        String ruleFlags = null;
        String whereClause = Columns.KEY + EQUALS + Q + ruleKey + Q;
        Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, new String[] {RuleTable.Columns.FLAGS},
                            whereClause, null, null);
        if(ruleCursor != null) {
            try {
                if(ruleCursor.moveToFirst()) {
                    ruleFlags = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.FLAGS));
                } else
                    Log.e(TAG, "ruleCursor.moveToFirst() failed for "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(!ruleCursor.isClosed())
                    ruleCursor.close();
            }
        } else
            Log.e(TAG, "rule cursor fetched is null for whereClause "+whereClause);

        if(LOG_DEBUG) Log.d(TAG, "returning ruleFlags as "+ruleFlags+" for ruleKey "+ruleKey);
        return ruleFlags;
    }
    
    /** fetches and returns the  Enabled value from the rule table for the rule key passed in.
    *
    * @param context - context
    * @param ruleKey - rule key in the rule table
    * @return - the rule Enabled of the rule that matches the rule key in the table
    */
    public static int getEnabledForRuleKey(final Context context, final String ruleKey) {
        int enabled = 0;
        String whereClause = Columns.KEY + EQUALS + Q + ruleKey + Q;
        Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, new String[] {RuleTable.Columns.ENABLED},
                            whereClause, null, null);
        if(ruleCursor != null) {
            try {
                if(ruleCursor.moveToFirst()) {
                    enabled = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.ENABLED));
                } else
                    Log.e(TAG, "ruleCursor.moveToFirst() failed for "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(!ruleCursor.isClosed())
                    ruleCursor.close();
            }
        } else
            Log.e(TAG, "rule cursor fetched is null for whereClause "+whereClause);

        if(LOG_DEBUG) Log.d(TAG, "returning Enabled as "+enabled+" for ruleKey "+ruleKey);
        return enabled;
    }

    /** returns if the rule being processed is active or not
     * 
     * @param context - application context
     * @param ruleId - rule ID in the rule table
     * @return true if the rule is active else false
     */
    public static boolean isRuleActive(final Context context, final long ruleId) {
    	boolean result = false;
    	String whereClause = Columns._ID + EQUALS + Q + ruleId + Q;
    	Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, null);
    	
    	if(ruleCursor == null) {
    		Log.e(TAG, "Rule being deleted does not exist in the DB");
    	} else {
    		try {
    			if(ruleCursor.moveToFirst()) {
    				result = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.ACTIVE)) == Active.ACTIVE;
    			}
    		} catch (Exception e) {
    			Log.e(TAG, "Exception processing rule cursor returned for whereClause "+whereClause);
    		} finally {
    			if(! ruleCursor.isClosed())
    				ruleCursor.close();
    		}
    	}   	
    	if(LOG_DEBUG) Log.d(TAG, "Returning from isRuleActive "+result);	
    	return result;
    }

    /** returns if the rule being processed is active or not
     *
     * @param context - application context
     * @param ruleKey - rule key in the rule table
     * @return true if the rule is active else false
     */
    public static boolean isRuleActive(final Context context, final String ruleKey) {
	boolean result = false;
	String whereClause = Columns.KEY + EQUALS + Q + ruleKey + Q;
	Cursor ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, null);

	if(ruleCursor == null) {
		Log.e(TAG, "Rule being deleted does not exist in the DB");
	} else {
		try {
			if(ruleCursor.moveToFirst()) {
				result = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.ACTIVE)) == Active.ACTIVE;
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception processing rule cursor returned for whereClause "+whereClause);
		} finally {
			if(! ruleCursor.isClosed())
				ruleCursor.close();
		}
	}
	if(LOG_DEBUG) Log.d(TAG, "Returning from isRuleActive "+result);
	return result;
    }

    // Column to hold the count of failures in the action table for a rule
    public static final String FAIL_COUNT = "FailCount";  
	// SQL statement to get if there are any errors in the Action Table for that rule
    // "(select count (*) from Action where FkRule_id=Rule._id and ActFailMsg like "%Failure%" 
    //   and EnabledAct=1) as FailCount";
	private static final String FAILURE_ACTIONS_SUB_QUERY = 
						LP 
							+ SELECT + COUNT 
								+ LP 
									+ ALL 
								+ RP 
							+ FROM + ActionTable.TABLE_NAME 
								+ WHERE + ActionTable.Columns.PARENT_FKEY 
									+ EQUALS + RuleTable.TABLE_NAME + "." + RuleTable.Columns._ID
								+ AND + ActionTable.Columns.FAILURE_MESSAGE + LIKE + Q + WILD + FAILURE + WILD + Q 
								+ AND + ActionTable.Columns.ENABLED + EQUALS + Q + ActionTable.Enabled.ENABLED + Q
						+ RP
						+ AS + FAIL_COUNT;
			
	// Column to hold the count of suggestions in the action table for a rule
	public static final String SUGGESTION_COUNT = "SuggestionCount";
	
	public static final String SUGGESTION_ACTION_COUNT = "SuggestionActionCount";
	public static final String SUGGESTION_CONDITION_COUNT = "SuggestionConditionCount";
	// SQL Statement to get the count of suggested actions for a rule
	// SELECT count (*)  FROM Action, Rule WHERE Rule._id = 117 AND SuggStateAct != 0;
	// "(select count (*) from Action where Rule._id = FkRule_id and SuggStateAct != 0) as SuggestionCount";
	private static final String SUGGESTED_ACTIONS_SUB_QUERY = 
						LP
							+ SELECT + COUNT 
								+ LP
									+ ALL
								+ RP
							+ FROM + ActionTable.TABLE_NAME 
							+ WHERE + RuleTable.TABLE_NAME + "." + RuleTable.Columns._ID 
							        + EQUALS + ActionTable.Columns.PARENT_FKEY 
									+ AND + ActionTable.Columns.SUGGESTED_STATE + NOT_EQUAL + RuleTable.SuggState.ACCEPTED
									+ AND
						  			+ LP + ActionTable.Columns.ACTION_VALIDITY
						  			+ NOT_EQUAL + Q + ActionTable.Validity.UNAVAILABLE + Q
						  			+ OR + ActionTable.Columns.MARKET_URL + IS_NOT_NULL + RP
						  			+ AND + ActionTable.Columns.ACTION_VALIDITY
						  			+ NOT_EQUAL + Q + ActionTable.Validity.BLACKLISTED + Q
						+ RP
						+ AS + SUGGESTION_ACTION_COUNT;	
	
	// SQL Statement to get the count of suggested conditions for a rule
	// SELECT count (*)  FROM Condition, Rule WHERE Rule._id = 117 AND SuggStateAct != 0;
	// "(select count (*) from Condition where Rule._id = FkRule_id and SuggStateAct != 0) as SuggestionCount";
	private static final String SUGGESTED_CONDITIONS_SUB_QUERY = 
						LP
							+ SELECT + COUNT 
								+ LP
									+ ALL
								+ RP
							+ FROM + ConditionTable.TABLE_NAME 
								+ WHERE + RuleTable.TABLE_NAME + "." + RuleTable.Columns._ID 
									+ EQUALS + ConditionTable.Columns.PARENT_FKEY 
									+ AND + ConditionTable.Columns.SUGGESTED_STATE + NOT_EQUAL + RuleTable.SuggState.ACCEPTED
									+ AND
						  			+ LP + ConditionTable.Columns.CONDITION_VALIDITY
						  			+ NOT_EQUAL + Q + ConditionTable.Validity.UNAVAILABLE + Q
						  			+ OR + ConditionTable.Columns.CONDITION_MARKET_URL + IS_NOT_NULL + RP
						  			+ AND + ConditionTable.Columns.CONDITION_VALIDITY
						  			+ NOT_EQUAL + Q + ConditionTable.Validity.BLACKLISTED + Q
						+ RP
						+ AS + SUGGESTION_CONDITION_COUNT;		
	
	// Column to hold if the location block is present (1) or not (0)
	public static final String LOCATION_BLOCK_COUNT = "LocationBlockCount";
	// SQL statement to query the condition table if location block is present or not
	// "(select count (*) from Condition where Rule._id = FkRule_id and 
	//	StatePubKey = 'com.motorola.contextual.smartprofile.location' and
	//  EnabledCond = '1' and 
	//  (Not Unvavailable Publisher or market url not null) and
	//   Not Blacklisted ) AS LocationBlockCount";
	private static final String LOCATION_BLOCK_COUNT_SUB_QUERY =
						  LP
						  	+ SELECT + COUNT
						  		+ LP
						  			+ ALL
						  		+ RP
						  	+ FROM + ConditionTable.TABLE_NAME
						  		+ WHERE + RuleTable.TABLE_NAME + "." + RuleTable.Columns._ID
						  			+ EQUALS + ConditionTable.Columns.PARENT_FKEY
								+ AND + ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS
						  			+ Q + LOCATION_TRIGGER_PUB_KEY + Q
						  		+ AND + ConditionTable.Columns.ENABLED + EQUALS
						  			+ Q + ConditionTable.Enabled.ENABLED + Q + AND
						  			+ LP + ConditionTable.Columns.CONDITION_VALIDITY
						  			+ NOT_EQUAL + Q + ConditionTable.Validity.UNAVAILABLE + Q
						  			+ OR + ConditionTable.Columns.CONDITION_MARKET_URL + IS_NOT_NULL + RP
						  			+ AND + ConditionTable.Columns.CONDITION_VALIDITY
						  			+ NOT_EQUAL + Q + ConditionTable.Validity.BLACKLISTED + Q
						  + RP
						  + AS + LOCATION_BLOCK_COUNT;

	// where clause to fetch all the rules that are visible and can be shown on the Landing Page
    private static final String VISIBLE_RULES_LIST_WHERE_CLAUSE = 
	        	Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + Q
			        + AND + Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + Q
			        + AND + Columns.SOURCE + NOT_EQUAL + Q + Source.DEFAULT + Q //Hide the default rule
			        ;
    
    // sort order for the visible rules
    private static final String VISIBLE_RULES_LIST_SORT_ORDER =
    			Columns.ENABLED + BLANK_SPC + DESC + CS
    				+ Columns.ACTIVE + BLANK_SPC + DESC + CS
					 	+ Columns.NAME;
    

	// SQL statement to query the icon table for IconBlob
	// "(select Icon from Icon where FkRule_id = Rule._id) AS Icon";
	private static final String ICON_BLOB_SUB_QUERY =
						  LP
							+ SELECT + IconTable.Columns.ICON
								+ FROM + IconTable.TABLE_NAME
								+ WHERE + IconTable.Columns.PARENT_FKEY
								+ EQUALS + RuleTable.TABLE_NAME + "." + RuleTable.Columns._ID
						  + RP
						  + AS + IconTable.Columns.ICON;
	
	// Column to hold if the location block is present (1) or not (0)
	public static final String VALID_CONDITION_PUBLISHER_COUNT = "ValidConditionPublisherCount";
	// SQL statement to get the count of valid condition publishers in a Rule
	// "(select count(*) from condition where Rule._id = FkRule_id and conditionValidity = valid)"
	private static final String VALID_CONDITION_PUBLISHER_COUNT_SUB_QUERY =
			  LP
			  	+ SELECT + COUNT
			  		+ LP
			  			+ ALL
			  		+ RP
			  	+ FROM + ConditionTable.TABLE_NAME
			  		+ WHERE + RuleTable.TABLE_NAME + "." + RuleTable.Columns._ID
			  			+ EQUALS + ConditionTable.Columns.PARENT_FKEY
					+ AND + LP + ConditionTable.Columns.CONDITION_VALIDITY
					+ NOT_EQUAL + Q + ConditionTable.Validity.UNAVAILABLE + Q
					+ OR + ConditionTable.Columns.CONDITION_MARKET_URL + IS_NOT_NULL + RP
					+ AND + ConditionTable.Columns.CONDITION_VALIDITY
					+ NOT_EQUAL + Q + ConditionTable.Validity.BLACKLISTED + Q
					+ AND + ConditionTable.Columns.ENABLED
					+ EQUALS + Q + ConditionTable.Enabled.ENABLED + Q
			+ RP
			+ AS + VALID_CONDITION_PUBLISHER_COUNT;
    /** fetches and returns the cursor of visible rules that can be shown to the user 
     * 	in the landing page.
     * 
     * @param context - Context
     * @return - cursor of user visible rules.
     */
    public static Cursor getVisibleRulesCursor(Context context) {
    	
    	ArrayList<String> cursorColumns = new ArrayList<String>();
    	cursorColumns.addAll(Arrays.asList(RuleTable.getColumnNames()));
    	cursorColumns.add(FAILURE_ACTIONS_SUB_QUERY);
    	cursorColumns.add(SUGGESTED_ACTIONS_SUB_QUERY);
    	cursorColumns.add(SUGGESTED_CONDITIONS_SUB_QUERY);
    	cursorColumns.add(LOCATION_BLOCK_COUNT_SUB_QUERY);
    	
    	return context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, cursorColumns.toArray(new String[0]), 
				VISIBLE_RULES_LIST_WHERE_CLAUSE, null, VISIBLE_RULES_LIST_SORT_ORDER);    	
    }

    /** returns the rule cursor base do the listRowType passed in.
     * 
     * @param context - context
     * @param listRowType - @see ListRowInterface class.
     * @return - cursor of user visible rules.
     */
    public static Cursor getVisibleRulesCursor(Context context, int listRowType) {
    	ArrayList<String> cursorColumns = new ArrayList<String>();
    	cursorColumns.addAll(Arrays.asList(RuleTable.getColumnNames()));
    	cursorColumns.add(FAILURE_ACTIONS_SUB_QUERY);
    	cursorColumns.add(SUGGESTED_ACTIONS_SUB_QUERY);
    	cursorColumns.add(SUGGESTED_CONDITIONS_SUB_QUERY);
    	cursorColumns.add(LOCATION_BLOCK_COUNT_SUB_QUERY);
    	cursorColumns.add(ICON_BLOB_SUB_QUERY);
    	cursorColumns.add(VALID_CONDITION_PUBLISHER_COUNT_SUB_QUERY);
    	    	
    	String whereClause = VISIBLE_RULES_LIST_WHERE_CLAUSE;
    	
    	switch(listRowType) {
    		case ListRowInterface.LIST_ROW_TYPE_AUTO:
    			whereClause = whereClause + AND + Columns.RULE_TYPE + EQUALS + Q + RuleType.AUTOMATIC + Q +
    			AND + VALID_CONDITION_PUBLISHER_COUNT + GREATER_THAN + 0;
    			break;
    		
    		case ListRowInterface.LIST_ROW_TYPE_MANUAL:
    			whereClause = whereClause + AND + LP + Columns.RULE_TYPE + EQUALS + Q + RuleType.MANUAL + Q +
    	    			OR + VALID_CONDITION_PUBLISHER_COUNT + EQUALS + 0 + RP;
    			break;
    	}
    	    	
    	return context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, cursorColumns.toArray(new String[0]), 
				whereClause, null, VISIBLE_RULES_LIST_SORT_ORDER);
    }
    
    // where clause to fetch the ids of all the visible rules on the landing page and
    // the list of rules in the suggestion in-box.
    private static final String DEBUG_DUMP_WHERE_CLAUSE = 
    		Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + Q
	        + AND + Columns.FLAGS + IS_NOT_LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + Q
	        + AND + Columns.SOURCE + NOT_EQUAL + Q + Source.DEFAULT + Q //Hide the default rule
	        + OR 
	        	+ LP 
	        		+ Columns.FLAGS + LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + Q
	        		+ AND + Columns.SUGGESTED_STATE + NOT_EQUAL + Q + SuggState.ACCEPTED + Q
	        	+ RP
	        ;
    		 
    /** fetches and returns a cursor of rule ID's of rules visible to the user
     *  on the landing page and in the suggestion in-box
     * 
     * @param context - context
     * @return - cursor of rule ID's of landing page and suggestion in-box rules.
     */
    public static Cursor getDebugDumpRulesCursor(Context context) {
    	String[] cursorColumns = {RuleTable.Columns._ID};
    	return context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, cursorColumns, 
    			DEBUG_DUMP_WHERE_CLAUSE, null, VISIBLE_RULES_LIST_SORT_ORDER);    
    }
    
    /** fetches the returns the count of visible enabled automatic rules rules.
     * 
     * @param context - context
     * @return - returns the count of the visible enabled automatic rules.
     */
    public static int getVisibleEnaAutoRulesCount(final Context context) {
    	int count = 0; 	
    	Cursor cursor = null;
    	try {
	    	cursor = context.getContentResolver().query(Schema.VISIBLE_ENA_AUTO_RULES_CNT_VIEW_CONTENT_URI, null, null, null, null);
	    	if(cursor != null) {
	    		if (cursor.moveToFirst()) {
	    			count = cursor.getInt(0);
	    		}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		if(cursor != null && !cursor.isClosed())
    			cursor.close();
    	}
    	if(LOG_DEBUG) Log.d(TAG, "Returning from getCountOfVisibleActiveRules "+count);
    	return count;
    }

    /** fetches and returns the cursor of samples rules.
     * 
     * @param context - context
     * @return cursor of sample rules
     */
    public static Cursor getSampleRules(Context context) {
		String whereClause = 
				WHERE + RuleTable.Columns.SOURCE + EQUALS + Q + RuleTable.Source.FACTORY + Q +
				AND + RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q + RuleTable.SuggState.ACCEPTED + Q +
				AND + RuleTable.Columns.FLAGS + EQUALS + Q + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q;
		return RulePersistence.getDisplayRulesCursor(context, whereClause);
    }
    
    /** fetches and returns a cursor of suggestions based on the suggFlags passed in.
     * 
     * @param context - context
     * @param suggFlags - @see RuleTable.SuggState
     * @return cursor of suggestions based on suggFlags passed in
     */
    public static Cursor getSuggestions(Context context, int suggFlags) { 	
		String whereClause = WHERE + RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q
				+ suggFlags + Q;
		
		return RulePersistence.getDisplayRulesCursor(context, whereClause);
    }
    
    // Variable to hold the count of adopted rules for each sample rule
	public static final String SAMPLE_RULE_ADOPTED_COUNT = "SampleRuleAdoptedCount";
	// Rule table name for the inner query
	private static final String INNER_RULE_TABLE_NAME = "InnerRule";
	// Inner Query to fetch the count of the adopted rules for each sample rule
	// (SELECT  COUNT(* ) FROM Rule AS InnerRule WHERE InnerRule.ParentRuleKey 
	// 	= Rule.Key AND InnerRule.Flags not like '%i%' AND 
	// InnerRule.Flags not like '%s%') AS SampleRuleCount 
	private static final String SAMPLE_FK_RULE_SUB_QUERY = 
						LP
							+ SELECT + COUNT 
								+ LP
									+ ALL
								+ RP
							+ FROM + RuleTable.TABLE_NAME + AS + INNER_RULE_TABLE_NAME
								+ WHERE + INNER_RULE_TABLE_NAME + "." + Columns.PARENT_RULE_KEY
									+ EQUALS + RuleTable.TABLE_NAME + "." + Columns.KEY
								+ AND + INNER_RULE_TABLE_NAME + "." + Columns.FLAGS 
									+ IS_NOT_LIKE + Q + WILD + Flags.INVISIBLE + Q
						        + AND + INNER_RULE_TABLE_NAME + "." + Columns.FLAGS 
					        		+ IS_NOT_LIKE + Q + WILD + Flags.SOURCE_LIST_VISIBLE + Q
						+ RP
						+ AS + SAMPLE_RULE_ADOPTED_COUNT;
	
	/** fetches and returns the cursor for the sample or suggested rules
     *  Note:  Caller is expected to close the cursor.
     *  
	 * @param context - context
	 * @param whereClause - whereClause for suggested or sample rules 
	 * @return rule table cursor for sample or suggested rules
	 */
    public static Cursor getDisplayRulesCursor(Context context, String whereClause) {
        return getDisplayRulesCursor(context, whereClause, RuleTable.getColumnNames());
    }

	/** fetches and returns the cursor for the sample or suggested rules
     *  Note:  Caller is expected to close the cursor.
     *
	 * @param context - context
	 * @param whereClause - whereClause for suggested or sample rules
	 * @param columns - columns to be fetched. Must not be null
	 * @return rule table cursor for sample or suggested rules
	 */
    public static Cursor getDisplayRulesCursor(Context context, String whereClause, String[] columns) {
		Cursor ruleCursor = null;
	
		if(whereClause != null && columns != null) {
			ArrayList<String> cursorColumns = new ArrayList<String>();
			cursorColumns.addAll(Arrays.asList(columns));
			cursorColumns.add(SAMPLE_FK_RULE_SUB_QUERY);
			cursorColumns.add(ICON_BLOB_SUB_QUERY);
	
	        ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI,
						cursorColumns.toArray(new String[0]), whereClause, null, null);
		}
		return ruleCursor;
    }

    /** Returns the rule cursor corresponding to a given rule key.
     *  Note:  Caller is expected to close the cursor.
    *
    * @param context
    * @param key - the rule key
    *
    * @return - cursor.
    */
    public static Cursor getRuleCursor(Context context, String key) {

        return getRuleCursor(context, key, null);
    }

    /** Returns the rule cursor corresponding to a given rule key.
     *  Note:  Caller is expected to close the cursor.
    *
    * @param context
    * @param key - the rule key
    * @param columns - Columns to read
    * @return - cursor.
    */
    public static Cursor getRuleCursor(Context context, String key, String[] columns) {

        Cursor cursor = null;

        if (context != null && key != null){
            String whereClause = RuleTable.Columns.KEY + DbSyntax.EQUALS + DbSyntax.Q + key + DbSyntax.Q;
            try {
                cursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI,
                                                            columns, whereClause, null, null);

            } catch(Exception e) {
                Log.e(TAG, "Query failed for " + whereClause);
            }
        }
        return cursor;
    }

    /** Returns the rule cursor corresponding to a given where clause.
     *  Note:  Caller is expected to close the cursor.
    *
    * @param context
    * @param columns - Columns to read
    * @param where clause
    * @return - cursor.
    */
    public static Cursor getRuleCursor(Context context, String[] columns, String whereClause) {

        Cursor cursor = null;

        if (context != null && !Util.isNull(whereClause)){
            try {
                cursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI,
                                                            columns, whereClause, null, null);

            } catch(Exception e) {
                Log.e(TAG, "Query failed for " + whereClause);
            }
        }
        return cursor;
    }

    /** Returns the Rule Table Cursor
     *  Note: The caller is expected to close the Cursor
     * @param context
     * @return - Cursor - Rule Table Cursor
     */
     public static Cursor getRuleCursor(Context context) {

         Cursor ruleCursor = null;

         if (context != null)
         {
             try {
                 ruleCursor = context.getContentResolver().query(
                         Schema.RULE_TABLE_CONTENT_URI, null, null, null,  null);
             } catch(Exception e) {
                 Log.e(TAG, "Query failed for Rule Table");
             }
         }else{
             Log.e(TAG,"context is null");
         }
         return ruleCursor;
     }

     /** Returns the Rule Table Cursor for a set of SOURCE
      *  Note: The caller is expected to close the Cursor
      * @param context
      * @return - Cursor - Rule Table Cursor
      */
      @SuppressWarnings("unused")
	public static Cursor getRuleCursorForSourceList(Context context,
                                              ArrayList<String> sourceList) {
          StringBuilder whereClause = new StringBuilder();

          Cursor ruleCursor = null;

          if (context != null){
              if (sourceList != null && sourceList.size() > 0){
                  for (int i = 0; i < sourceList.size(); i++) {
                      String source = sourceList.get(i);
                      if (LOG_DEBUG) Log.d(TAG, "Source is " + source);
                       if(source.equals(String.valueOf(RuleTable.Source.FACTORY))){
                           whereClause.append(LP + RuleTable.Columns.SOURCE + EQUALS
                                   + Q + source + Q + AND + LP + RuleTable.Columns.SUGGESTED_STATE + NOT_EQUAL
                                   + Q + RuleTable.SuggState.ACCEPTED + Q + OR + RuleTable.Columns.FLAGS + NOT_EQUAL
                                   + Q + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q + RP + RP);
                       } else if(source.equals(String.valueOf(RuleTable.Source.CHILD))){
                           whereClause.append(LP + RuleTable.Columns.SOURCE + EQUALS
                                   + Q + source + Q + AND + RuleTable.Columns.FLAGS + NOT_EQUAL
                                   + Q + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q + RP);
                       }else{
                           whereClause.append(RuleTable.Columns.SOURCE + EQUALS
                                          + Q + source + Q);
                       }
                       if (i < (sourceList.size()-1)){
                           whereClause.append(OR);
                       }
                  }

                  if (whereClause != null){
                      try {
                          ruleCursor = context.getContentResolver().query(
                                  Schema.RULE_TABLE_CONTENT_URI, null,
                                  whereClause.toString(), null,  null);
                      } catch(Exception e) {
                          Log.e(TAG, "Query failed for Rule Table");
                      }
                  }else{
                      Log.e(TAG,"whereCaluse is null");
                  }
              }else{
                  Log.e(TAG,"source List is empty");
              }
          }else{
              Log.e(TAG,"context is null");
          }
          return ruleCursor;
      }
      
    /**
     * Enables/adds the suggestion as a new Rule in MM DB set ENABLED == true set FLAG == none (not
     * as suggested or invisible)
     *
     * @param ruleId: ID of the suggestion to be converted to a Rule
     *
     */
    public static void addSuggestionAsRule(Context ct, long ruleId) {
        ContentValues cv = new ContentValues();

        cv.put(Columns.ENABLED, 1);
        cv.put(Columns.FLAGS, 0);

        // update the Tuple if rule table
        ct.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, cv,
                                       Columns._ID + EQUALS + ruleId, null);

        String ruleKey = getRuleKeyForRuleId(ct, ruleId);
        RulesValidatorInterface.launchModeAd(ct, ruleKey, 
                ImportType.IGNORE, 
                RuleTable.Validity.VALID, 
                fetchRuleOnly(ct, ruleKey),
                isRulePsuedoManualOrManual(ct, ruleKey));

    /*    // Launch RulesValidator
        Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
        ArrayList<String> ruleList = new ArrayList<String>();
        ruleList.add(getRuleKeyForRuleId(ct, ruleId));
        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(ruleId));
        ct.sendBroadcast(rvIntent);
*/
    }

    /**
     * Returns the field value for the column passed
     *
     * @param context - context
     * @param ruleId - ID of the Rule
     * @param whichColumn - column to read 
     * 
     * @return - Value of the column
     */
    public static int getColumnIntValue(Context context, long ruleId, String whichColumn) {

        int result = -1;
        if(whichColumn == null) return result;

        String whereClause = RuleTable.Columns._ID + EQUALS + Q + ruleId + Q;
        return getColumnIntValue(whichColumn, whereClause, context);
    }

    /**
     * Returns the field value for the column passed
     *
     * @param context - context
     * @param ruleKey - Key of the Rule
     * @param whichColumn - column to read 
     * 
     * @return - Value of the column
     */
    public static int getColumnIntValue(Context context, String ruleKey, String whichColumn) {

        int result = -1;
        if(whichColumn == null) return result;

        String whereClause = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q;
        return getColumnIntValue(whichColumn, whereClause, context);
    }

    /**
     * Returns the field value for the column passed
     * 
     * @param context - context
     * @param whichColumn - column to read 
     * @param whereClause - where clause
     * @return - Value of the column
     */
    public static int getColumnIntValue(String whichColumn, String whereClause, Context context){

        int result = -1;
        Cursor ruleCursor = getRuleCursor(context, new String[] {whichColumn}, whereClause);

        if (ruleCursor == null) return result;

        try {
            if(ruleCursor.moveToFirst()){
                result = ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow(whichColumn));
            }
        } catch(IllegalArgumentException e) {
            Log.e(TAG, "Query failed for Rule Table");
        } finally {
            ruleCursor.close();
        }

        return result;
    }

    /** This sets the count value in ADOPT_COUNT column
    *
    * @param context - context
    * @param sampleRuleId - Rule ID of the sample
    * @param adoptCount - adopted count value
    */
   public static void setAdoptCount(Context context, long sampleRuleId, long adoptCount){

       // update count in the sample rule
       ContentValues contentValues = new ContentValues();
       contentValues.put(RuleTable.Columns.ADOPT_COUNT, adoptCount);

       String whereClause = RuleTable.Columns._ID + EQUALS + Q + sampleRuleId + Q;
       context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, contentValues, whereClause, null);
   }

	/** fetches and returns the cursor for the sample child rules Note: Caller is
	 *  expected to close the cursor.
	 * 
	 * @param context - context
	 * @param sampleRuleId - rule Id of the sample rule
	 * @param whereClause - custom whereClause to use
	 * @param columns - columns to be fetched
	 * @return rule table cursor for sample child rules
	 */
	public static Cursor getSampleChildRuleCursor(Context context,
			final long sampleRuleId, String whereClause, String[] columns,
			String sampleRuleKey) {

		Cursor cursor = null;

		if (context != null && sampleRuleKey != null) {
			if (whereClause == null) {
				whereClause = RuleTable.Columns.PARENT_RULE_KEY + EQUALS + Q
						+ sampleRuleKey + Q;
			}
			try {
				cursor = context.getContentResolver().query(
						Schema.RULE_TABLE_CONTENT_URI, columns, whereClause,
						null, null);

			} catch (Exception e) {
				Log.e(TAG, "Query failed for " + whereClause);
			}
		}
		return cursor;
	}
    
    /**
     * Reads the string value of the column supplied
     * from Rule Table.
     * 
     * @param ct - context
     * @param ruleId - rule id
     * @param column - DB column to read
     * @return - DB column value
     */
    public static String getRuleStringValue(Context ct, long ruleId, String column) {
        
    	String columnValue = null;
    	
    	if (Util.isNull(column)) return columnValue;

        // what columns to query
        String[] columns = new String[] { column };
        // populate the where clause
        String whereClause = RuleTable.Columns._ID + EQUALS + Q
                + ruleId + Q;

        // query the db to fetch the rule tuple
        Cursor c = ct.getContentResolver().query(
                RuleTable.CONTENT_URI, columns, whereClause, null,
                null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    // read the rule xml from the column
                    columnValue = c.getString(c.getColumnIndexOrThrow(column));
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Fectching of ui intent failed for = " + ruleId);
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        return columnValue;
    }

    /** Returns the list of rule keys corresponding to a publisher Key.
    *
    * @param context
    * @param publisherKey - rule publisher key of the rule
    */
    public static List<String> getRuleKeys(Context context, String publisherKey) {
        List<String> ruleKeys = new ArrayList<String>();

        if (context != null) {

            Cursor cursor = null;
            String whereClause = RuleTable.Columns.PUBLISHER_KEY + EQUALS + Q +
                                 publisherKey + Q;
            try {

                cursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI,
                         new String[] { RuleTable.Columns.KEY},
                         whereClause, null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        do {
                            String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.PUBLISHER_KEY));
                            ruleKeys.add(ruleKey);
                        } while(cursor.moveToNext());
                    }
                }
            } catch(Exception e) {
                Log.e(TAG, "Query failed for " + whereClause);
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }else {
        	Log.e(TAG, " Input parameter null");
        }

        return ruleKeys;
    }
     /**
     * Provides the life cycle for a rule id.
     *
     * @param ct - context
     * @param ruleId - rule id
     * @return - Suggestion Type
     */
    public static int getSuggType(Context ct, long ruleId) {


        // This is initialized to -2 since suggestion type behavioral
        // is -1 (IMMIDEATE)
        int mSugType = INVALID;

        // get the rule cursor
        Cursor ruleCursor = SuggestionsPersistence.getSuggestedRuleCursor(ct, ruleId);

        if (ruleCursor == null)
            Log.e(TAG, "rule cursor is null in getSuggType");
        else {
            try {
                if (ruleCursor.moveToFirst()) {
                    // Get the suggestion type
                    mSugType = ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.LIFECYCLE));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // close the cursor
                ruleCursor.close();
            }
        }
        return mSugType;
    }

    /**
     * deletes the child rule and decrements the child count in the sample rule
     * 
     * @param context - context
     * @param childId - rule to be deleted
     * @param parentId - rule where child count needs to be decremented
     * @param count - child count
     */
    public static void deleteChildRule(Context context, long childId, 
    									long parentId, long count){

        deleteRule(context, childId, null, null, false);

        // Need to decrement the counter since the user selected cancel.
        // Update the ADOPT_COUNT
        setAdoptCount(context, parentId, count);
    }

    /** fetches the rule instance for the parent rule, resets the persistent fields
     *  if needed and launches rules builder.
     * 
     * @param context - context
     * @param source - @see RuleTable.Source
     * @param parentRuleId - rule id for the parent (sample of suggestion)
     * @param requestCode - RULE_EDIT or RULE_CREATE or RULE_PRESET or RULE_SUGGESTED
     * @return the ADOPT_COUNT column value for the parent rule
     */
    public static int launchRulesBuilder(Context context, int source, 
    		long parentRuleId, int requestCode) {
    	int adoptCount = 0;
        Rule rule = RulePersistence.fetchFullRule(context, parentRuleId);
        if(rule == null) {
        	Log.e(TAG, "fetchFullRule returned null for rule "+parentRuleId);
        } else {
            if(source == RuleTable.Source.FACTORY) {
            	adoptCount = (int) rule.getAdoptCount();
            	String newRuleName = RulePersistence.createClonedRuleName(rule.getName(), adoptCount);
		String newRuleKey = RulePersistence.createClonedRuleKeyForSample(rule.getKey());
            	rule.resetPersistentFields(newRuleKey, newRuleName, DEFAULT_RULE_ID, false);
            }
            Intent intent = rule.fetchRulesBuilderIntent(context, false);
            ((Activity) context).startActivityForResult(intent, requestCode);   
        } 	 	
        return adoptCount;
    }
    
    /**
     * This method helps to find out if there any blocks which are enabled and 
     * un configured.
     * @param context
     * @param ruleId
     * @return boolean - true - un configured enabled blocks exist
     */
    public static boolean anyUnconfiguredEnabledActionsOrConditions(final Context context, long ruleId) {
   	 
	    if (LOG_DEBUG) Log.d(TAG,"Rule: anyConnectedUnConfiguredBlocks");
	   
	   	//Check any condition or action unconfigured connected blocks
	   return (ConditionPersistence.anyUnconfiguredEnabledConditions(context, ruleId) ||
			 ActionPersistence.anyUnconfiguredEnabledActions(context, ruleId));
    }


    /**
     * Checks whether the rule is Manual or PsuedoManual
     * @param context
     * @param ruleKey
     * @return true if rule is manual else false
     */
    public static boolean isRulePsuedoManualOrManual(Context context, String ruleKey) {
        boolean isPsuedoManual = true;
        ContentResolver cr = context.getContentResolver();
        String where = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q
                       + AND + LP +
						ConditionTable.Columns.CONDITION_VALIDITY + NOT_EQUAL
						+ Q + ConditionTable.Validity.UNAVAILABLE + Q
						+ OR + ConditionTable.Columns.CONDITION_MARKET_URL + IS_NOT_NULL + RP
						+ AND + ConditionTable.Columns.CONDITION_VALIDITY + NOT_EQUAL
						+ Q + ConditionTable.Validity.BLACKLISTED + Q
                       + AND + ConditionTable.Columns.ENABLED + EQUALS + Q + ConditionTable.Enabled.ENABLED + Q;

//        String where = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q;
        String[] projection = {RuleTable.Columns.KEY};
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleConditionView.CONTENT_URI, projection, where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                isPsuedoManual = false;
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if(cursor != null) cursor.close();
        }
        return isPsuedoManual;
    }
}
