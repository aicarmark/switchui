/*
 * @(#)ActionPersistence.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/20 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.ActionTableColumns;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.util.Util;

/** This class holds the handlers that deal with query, insert, update and delete on
 * 	Action Table Columns.
 *
 *<code><pre>
 * CLASS:
 * 	 extends ActionTable
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
public class ActionPersistence extends ActionTable implements Constants, DbSyntax {

    protected static final String TAG = ActionPersistence.class.getSimpleName();

    /** Fetches the list of actions associated with the rule ID passed in. Returns
     *  a list of type Action.
     * 
     * @param context - context
     * @param ruleId - rule ID in the rule table
     * @return - list of actions associated with the rule.
     */
    @SuppressWarnings("unchecked")
	public <E extends Action> ActionList<Action> fetch(Context context, long ruleId) {

    	String whereClause = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
    	ArrayList<ActionTuple> tupleList = this.fetchList(context, whereClause);
    	
    	ActionList<Action> actionList = null;
    	if(tupleList != null) {
    		actionList = new ActionList<Action>();
    		for (ActionTuple t: tupleList) {
    			Rule childRule = null;
    			if(t.getChildRuleKey() != null) {
    				if(LOG_DEBUG) Log.d(TAG, "Fetch the child rule with key "
    					+t.getChildRuleKey()+" associated with the action "+t.get_id());
    				long childRuleId = 
    					RulePersistence.getRuleIdForRuleKey(context, t.getChildRuleKey());
    				childRule = RulePersistence.fetchFullRule(context, childRuleId);
    			}
    			Action a = new Action(t);
    			if(childRule != null) {
    				Log.d(TAG, "Appending the child rule "+childRule.toString());
    				a.setChildRule(childRule);
    			}
    			actionList.add((E) a);
    		}    		
    	}
    	
    	return actionList;
    }

    /** inserts the list of actions in to the DB.
     * 
     * @param db - database instance
     * @param list - list of actions to be inserted
     */
	public void insertList(SQLiteManager db, ActionList<Action> list) {  	
    	for(Action action: list) {
    		if(action.getChildRule() != null) {
    			if(LOG_DEBUG) Log.d(TAG, "Need to insert the child rule "
    									+action.getChildRule().toString());
    			new RulePersistence().insertRule(db, action.getChildRule());
    		}
    		super.insert(db, (ActionTuple) action);
    	}
	}
	
    /** Inserts an action tuple into the Action table
     * 
     * @param context - context
     * @param actionTuple - action tuple
     */
    public static void insertAction(Context context, ActionTuple actionTuple){
    	try {
    		context.getContentResolver().insert(Schema.ACTION_TABLE_CONTENT_URI, 
    									new ActionTable().toContentValues(actionTuple));
    	} catch (Exception e) {
    		Log.e(TAG, "Insert to Action Table failed");
    		e.printStackTrace();
    	}
    }
    
    /** updates the list of actions in the DB.
     * 
     * @param db - database instance
     * @param list - list of actions to be updated
     */
    public void updateList(SQLiteManager db, ActionList<Action> list) {  	
    	for(ActionTuple action: list) {
    		if(action.isNew()) {
    			super.insert(db, action);
    		}
    		else if(action.isLogicalDelete()) {
    			String whereClause = 
    					WHERE + Columns._ID + EQUALS + Q + action.get_id()  + Q;
    			super.deleteWhere(db, whereClause);
    		} else if (action.isDirtyFlag()) {
    			super.update(db, action);
    		} 
    	}
    }
    
    /** Deletes an action tuple from the Action table
     * 
     * @param context - context
     * @param whereClause - where clause
     */
    public static void deleteAction(Context context, String whereClause){
    	try {
    		context.getContentResolver().delete(Schema.ACTION_TABLE_CONTENT_URI, whereClause, null);
    	} catch (Exception e) {
    		Log.e(TAG, "Delete from Action Table failed");
    		e.printStackTrace();
    	}
    }

    /** updates the default record for the passed in action publisher key with the default setting
     * 	and also the URI to fire when we need to revert to the default setting for the action
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key of the action for which the default record
     * 								is being stored
     */
    public static void updateDefaultRecordForActionKey(final Context context, final String actionPublisherKey) {

        if(LOG_DEBUG) Log.d(TAG, "Entering updateDefaultRecordForActionKey");

        long defaultRuleId = RulePersistence.getDefaultRuleId(context);
        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q + AND +
                             Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q;

        Cursor actionCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

        if(actionCursor != null) {
            try {
                if(actionCursor.moveToFirst()) {
                    if(actionCursor.getInt(actionCursor.getColumnIndex(Columns.ACTIVE)) == ActionTable.Active.INACTIVE) {
                        if(LOG_DEBUG) Log.d(TAG, "Request was made to store the default value - store this value in the default record");
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(Columns.ACTIVE, ActionTable.Active.ACTIVE);
                        contentValues.put(Columns.LAST_FIRED_ATTEMPT_TIME, new Date().getTime());
                        int rowsUpdated = context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
                        // Only one default action record should exist for this action publisher key and be updated.
                        if(rowsUpdated != 1)
                        	Log.e(TAG, "Update to default action record failed - for actionPublisherKey "+actionPublisherKey
									+"; defaultRuleId "+defaultRuleId+" rowsUpdated "+rowsUpdated);
                    }
                }
                else
                    Log.e(TAG, "actionCursor.moveToFirst() failed and whereClause = "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(! actionCursor.isClosed())
                    actionCursor.close();
            }
        }
        else
            Log.e(TAG, "Action Cursor fetched is null for whereClause "+whereClause);
    }

    /**
     *  This method updates the Validity column of Action publisher
     * @param context - Context
     * @param actionPublisherKey - action publisher key for the action
     * @param config - configuration of the action
     * @param ruleKey - Rule Key
     * @param actionValidity - the new validity value for the action
     */
    public static void updateActionValidity(final Context context, final String actionPublisherKey,
            final String config, String ruleKey, final String actionValidity) {
	    if(LOG_DEBUG) Log.d(TAG, "updateActionValidity: " + actionPublisherKey +
			"\n config :" + config + "ruleKey: " + ruleKey + " actionValidity: " + actionValidity);
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(ActionTableColumns.ACTION_VALIDITY, actionValidity);
        String where = null;
        String[] whereArgs = null;
        //If ruleKey is null, then update 'Action Validity' column for all entries in action table
        // with this publisher key, else form the query based on the available config
        // ruleKey and update accordingly. 
        try {
            if(ruleKey == null){
		    where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q;
			    cr.update(ActionTable.CONTENT_URI, cv, where, null);
            }else {
                long ruleId = RulePersistence.getRuleIdForRuleKey(context, ruleKey);
                // If rulekey is not null, check if config is provided. If yes, add config also
                // to the query arguments
                if(config != null) {
			// If ruleId is default rule id, do not add rule id to query arguments
			// Else add rule id to query arguments
                    if(ruleId == DEFAULT_RULE_ID) {
                       where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + ANY +
	                            AND + ActionTableColumns.CONFIG + EQUALS + ANY;
                       whereArgs = new String[]{actionPublisherKey, config};
                    } else {
                       where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + ANY +
	                           AND + ActionTableColumns.PARENT_FKEY + EQUALS + ANY +
	                                   AND + ActionTableColumns.CONFIG + EQUALS + ANY;
                       whereArgs = new String[]{actionPublisherKey, Long.toString(ruleId), config};
                    }
                } else {
			// If config is not null, check if ruleId is default rule id, If so
			// do not add rule key to query arguments, else add rule id to query arguments
                    if(ruleId == DEFAULT_RULE_ID) {
                        where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + ANY;
                        whereArgs = new String[]{actionPublisherKey};
                    } else {
                         where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + ANY +
	                        AND + ActionTableColumns.PARENT_FKEY + EQUALS + ANY ;
                         whereArgs = new String[]{actionPublisherKey, Long.toString(ruleId)};
			        }
                }
	            cr.update(ActionTable.CONTENT_URI, cv, where, whereArgs);
	        }
        }catch(Exception e) {
		e.printStackTrace();
        }

    }
    /** this handler is invoked to mark inactive all the actions that match the action
     *  publisher key passed as the user has changed the value from the settings menu.
     *  Also updates the default record with this new default setting.
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key for the action that was changed by the user
     * @param newDefaultValue - the new default value for the action that user changed from settings menu
     */
    public static void markActionsInactiveForActionKey(final Context context, final String actionPublisherKey,
            final String newDefaultValue) {

        if(LOG_DEBUG) Log.d(TAG, "Entering markActionsInactiveForActionKey");

        long defaultRuleId = RulePersistence.getDefaultRuleId(context);
        
        // Reset the default record
        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q + AND +
                             Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q;
        ContentValues contentValues = new ContentValues();
        contentValues.put(Columns.ACTION_DESCRIPTION, newDefaultValue);
        context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);

        // Mark all action records with this action publisher key to inactive and also clear
        // the conflict winner flag
        whereClause = Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q;
        contentValues = new ContentValues();
        contentValues.put(Columns.ACTIVE, ActionTable.Active.INACTIVE);
        contentValues.put(Columns.CONFLICT_WINNER_FLAG, ConflictWinner.LOSER);
        context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
    }

    /** Updates the action table columns
     *
     * @param context - context
     * @param _id - rule table ID
     * @param isArriving - true if arriving into the rule and false if leaving the rule
     */
    public static void updateActionTable(final Context context, final long _id,
                                         final boolean isArriving) {

        if(LOG_DEBUG) Log.d(TAG, "Entering updateActionTable");

        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + _id + Q + AND
                             + Columns.ENABLED + EQUALS + Q + Enabled.ENABLED + Q;

        ContentValues contentValues = new ContentValues();
        // Clear the error message when a rule fires successfully.
        contentValues.put(Columns.FAILURE_MESSAGE, NULL_STRING);
        if(isArriving) {
            contentValues.put(Columns.LAST_FIRED_ATTEMPT_TIME, new Date().getTime());
            contentValues.put(Columns.ACTIVE, Active.ACTIVE);
        } else {
            contentValues.put(Columns.ACTIVE, Active.INACTIVE);
        }
        try {
            context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, "Update to action table failed");
            e.printStackTrace();
        }
    }

    /** updates the FAILURE_MESSAGE column for the action pub key with the failure message and also
     *  marks the action as inactive.
     *
     * @param context - context
     * @param ruleId - ruleId i.e. PARENT_FKEY for the action in the action table
     * @param actionId - action Id for the action
     * @param failureMessage - message that is entered into FAILURE_MESSAGE column
     */
    public static void updateActionFailureStatus(final Context context, final long ruleId,
            final String failureMessage, final long actionId) {

        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q + AND +
                             Columns._ID + EQUALS + Q + actionId + Q;
        ContentValues contentValues = new ContentValues();
        contentValues.put(Columns.FAILURE_MESSAGE, failureMessage);
        contentValues.put(Columns.ACTIVE, Active.INACTIVE);
        try {
            context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, "Update to action table failed");
            e.printStackTrace();
        }
    }

    /** Fires the actions for a manual rule
     *
     * @param context - context
     * @param _id - rule ID
     * @param ruleKey - rule key
     * @param ruleName - rule name
     */
    public static void fireManualRuleActions(final Context context, final long _id,
            final String ruleKey, final String ruleName) {

        if(LOG_DEBUG) Log.d(TAG, "Entering fireManualRuleActions");

        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + _id + Q;
        Cursor actionCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

        if(actionCursor != null) {
            try {
                if(actionCursor.moveToFirst())
                    handleFiringManualActions(context, actionCursor, _id, ruleKey, ruleName);
                else
                    Log.e(TAG, "actionCursor.moveToFirst failed and whereClause = "+whereClause);

            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH+" fetching action cursor");
                e.printStackTrace();
            } finally {
                if(! actionCursor.isClosed())
                    actionCursor.close();
            }
        } else {
            Log.e(TAG, "action cursor returned is null for whereClause "+whereClause);
        }
    }

    /** loops through the action cursor and fires actions accordingly.
     *
     * @param context - context
     * @param actionCursor - action table cursor for the rule
     * @param _id - rule ID
     * @param ruleKey - rule key
     * @param ruleName - rule name
     */
    public static void handleFiringManualActions(final Context context, final Cursor actionCursor, final long _id,
            final String ruleKey, final String ruleName) {

        if(LOG_DEBUG) Log.d(TAG, "Entering handleFiringManualActions");

        boolean saveDefault = false;
        ActionTable tbl = new ActionTable();
        ActionTuple t = null;
        for(int i = 0; i < actionCursor.getCount(); i ++) {
        	t = tbl.toTuple(actionCursor);

            if(t.getEnabled() == ActionTable.Enabled.ENABLED) {
                if(t.getModality() == ModalTable.Modality.STATEFUL) {
                    if(LOG_DEBUG) Log.d(TAG, "Stateful action - check for default record before firing action");

                   if(LOG_DEBUG) Log.d(TAG, "for actionId "+ t.get_id() +"; action is "+ t.getPublisherKey());
                    saveDefault = isDefaultValueNeeded(context, t.getPublisherKey(), t.getDescription(), 
                    		               t.getStateMachineName(), t.getActivityIntent());
                    if(LOG_DEBUG) Log.d(TAG, "saveDefault = "+saveDefault);
                    updateConflictWinnerFlag(context, t.get_id(), t.getPublisherKey(), ActionTable.ConflictWinner.WINNER);
                    Action.sendBroadcastIntentForAction(context, t.getPublisherKey(),
						ruleKey, ruleName, saveDefault,
						t.getPublisherKey(), COMMAND_FIRE, t.getConfig(), t.get_id());
                } else if(t.getModality() == ModalTable.Modality.STATELESS && 
                		t.getOnExitModeFlag() == ActionTable.OnModeExit.ON_ENTER) {
                    saveDefault = false;
                    if(LOG_DEBUG) Log.d(TAG, "Modal action for ON_ENTER: for actionId "+ t.get_id() 
                    		+"; action is "+ t.getPublisherKey());
                    Action.sendBroadcastIntentForAction(context, t.getPublisherKey(),
					ruleKey, ruleName, saveDefault,
					t.getPublisherKey(), COMMAND_FIRE, t.getConfig(), t.get_id());
                } else {
                    Log.e(TAG, "No Modal Value was set for the actionId "+ t.get_id());
                }
            } else {
                Log.e(TAG, "action "+ t.get_id() +" is not enabled");
            }
            actionCursor.moveToNext();
        }
    }

    /** mark all the actions that are active in the DB as inactive including the default record actions.
     *
     * @param context - context
     */
    public static void markActionsInactiveInDb(final Context context) {

        if(LOG_DEBUG) Log.d(TAG, "Entering markActionsInactiveInDb");

        try {
            String whereClause = Columns.ACTIVE + EQUALS + Q + Active.ACTIVE + Q;
            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.ACTIVE, Active.INACTIVE);
            contentValues.put(Columns.LAST_FIRED_ATTEMPT_TIME, new Date().getTime());
            int numRows = context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
            if(LOG_DEBUG) Log.d(TAG, "updated total rows = "+numRows);
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH);
            e.printStackTrace();
        }

    }

    /** fires all the actions associated with the default record.
     *
     * @param context - context
     */
    public static void fireDefaultRecordActions(final Context context) {

        if(LOG_DEBUG) Log.d(TAG, "Entering fireDefaultRecordActions");

        long defaultRuleId = RulePersistence.getDefaultRuleId(context);
        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q;
        Cursor defaultActionsCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

        if(defaultActionsCursor != null) {
            try {
                if(defaultActionsCursor.moveToFirst()) {
                    if(LOG_DEBUG) Log.d(TAG, "Default record has "+defaultActionsCursor.getCount()+" active actions to be fired");
                    for (int i = 0; i < defaultActionsCursor.getCount(); i++) {
                        if(defaultActionsCursor.getInt(defaultActionsCursor.getColumnIndexOrThrow(Columns.ACTIVE)) == Active.ACTIVE) {
                            String config = defaultActionsCursor.getString(defaultActionsCursor.getColumnIndexOrThrow(ActionTable.Columns.CONFIG));
                            String actionPublisherKey = defaultActionsCursor.getString(defaultActionsCursor.getColumnIndexOrThrow(Columns.ACTION_PUBLISHER_KEY));
                            long id = defaultActionsCursor.getLong(defaultActionsCursor.getColumnIndex(Columns._ID));
                            if(LOG_DEBUG) Log.d(TAG, "Firing "+actionPublisherKey);
                            Action.sendBroadcastIntentForAction(context, actionPublisherKey,
									DEFAULT_RULE_KEY,
									context.getString(R.string.default_rule), false,
									actionPublisherKey, COMMAND_REVERT, config, id);
                        }
                        defaultActionsCursor.moveToNext();
                    }
                }
                else
                    Log.e(TAG, "defaultActionsCursor.moveToFirst() and whereClause = "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(! defaultActionsCursor.isClosed())
                    defaultActionsCursor.close();
            }
        }
        else
            Log.e(TAG, "Default Actions Cursor fetched is null for whereClause "+whereClause);
    }

    /**
    * fires an intent to notify all the active stateless actions that user has disabled all the rules
    *
    * @param context - context
    */
   public static void fireAllStatelessActionsDisabled (final Context context) {

       if (LOG_DEBUG) Log.d(TAG, "Entering disableAllStatelessActions");

       String whereClause = Columns.MODAL + EQUALS + Q + ModalTable.Modality.STATELESS + Q + AND +
               Columns.ACTIVE + EQUALS + Q + Active.ACTIVE + Q;
       Cursor statelessActionsCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

       if(statelessActionsCursor != null) {
           try {
               if(statelessActionsCursor.moveToFirst()) {
                   if (LOG_DEBUG) Log.d(TAG, "Stateless record has "+statelessActionsCursor.getCount()+" active actions to be fired");
                   for (int i = 0; i < statelessActionsCursor.getCount(); i++) {
                       String ruleForeignKey = statelessActionsCursor.getString(statelessActionsCursor.getColumnIndexOrThrow(Columns.PARENT_FKEY));
                       String ruleWhereClause = RuleTable.Columns._ID + EQUALS + Q + ruleForeignKey + Q;
                       String ruleKey = "";
                       String ruleName = "";
                       Cursor ruleCursor = null;
                       try {
                           ruleCursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, ruleWhereClause, null, null);
                           if (ruleCursor != null) {
                               if (ruleCursor.moveToFirst()) {
                                    ruleKey = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.KEY));
                                    ruleName = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.NAME));
                                    if (LOG_DEBUG) Log.d(TAG, "ruleKey: "+ruleKey+", ruleName: "+ruleName);
                               }
                           }
                       } finally {
                           if (ruleCursor != null)
                               ruleCursor.close();
                       }
                       String actionName = statelessActionsCursor.getString(statelessActionsCursor.getColumnIndexOrThrow(Columns.STATE_MACHINE_NAME));
                       String config = statelessActionsCursor.getString(statelessActionsCursor.getColumnIndexOrThrow(Columns.CONFIG));
                       String actionPublisherKey = statelessActionsCursor.getString(statelessActionsCursor.getColumnIndex(Columns.ACTION_PUBLISHER_KEY));
                       int onExitMode = statelessActionsCursor.getInt(statelessActionsCursor.getColumnIndex(Columns.ON_MODE_EXIT));
                       long id = statelessActionsCursor.getLong(statelessActionsCursor.getColumnIndex(Columns._ID));
                       if (LOG_DEBUG) Log.d(TAG, "Firing "+actionPublisherKey);

                       if(onExitMode == OnModeExit.ON_ENTER) {
                           Action.sendBroadcastIntentForAction(context, actionPublisherKey, ruleKey, ruleName, false,
                               actionName, COMMAND_REVERT, config, id);
                       } else if(onExitMode == OnModeExit.ON_EXIT){
                           Action.sendBroadcastIntentForAction(context, actionPublisherKey, ruleKey, ruleName, false,
                               actionName, COMMAND_FIRE, config, id);
                       }
                       statelessActionsCursor.moveToNext();
                   }
               }
               else
                   Log.e(TAG, "statelessActionsCursor.moveToFirst() and whereClause = "+whereClause);
           } catch (Exception e) {
               Log.e(TAG, PROVIDER_CRASH);
               e.printStackTrace();
           } finally {
               if(! statelessActionsCursor.isClosed())
                   statelessActionsCursor.close();
           }
       }
       else
           Log.e(TAG, "Stateless Actions Cursor fetched is null for whereClause "+whereClause);
   }

    /** Sets the default action record to inactive in the action table
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key for which the record is marked inactive
     */
    public static void setDefaultRecordForActionInactive(final Context context, final String actionPublisherKey) {

        if(LOG_DEBUG) Log.d(TAG, "Entering setDefaultRecordForActionInactive");

        long defaultRuleId = RulePersistence.getDefaultRuleId(context);
        try {
            String whereClause = Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q + AND +
                                 Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q;
            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.ACTIVE, Active.INACTIVE);
            context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH);
            e.printStackTrace();
        }
    }

    /** Inserts the default record for the action in the db.
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key for the action that we look up in the database
     * @param actionDesc - action description to be saved in the db if no default action record exists
     * @param stateMachine - state machine to be saved in the db if no default action record exists
     * @param activityIntent - activity intent to be saved in the db if no default action record exists
     * @param defaultRuleId - rule id of the default record in the rule table
     * @return - true if the default record is inserted for the action to the Action table else false
     */
    public static boolean insertDefaultRecordForAction(final Context context, final String actionPublisherKey,
            final String actionDesc, final String stateMachine,
            final String activityIntent, final long defaultRuleId) {

        if(LOG_DEBUG) Log.d(TAG, "Entering insertDefaultRecordForAction");

        boolean result = false;
        try {
            if(LOG_DEBUG) Log.d(TAG, "inserting the default action record for "+actionPublisherKey);
            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.PARENT_FKEY, defaultRuleId);
            contentValues.put(Columns.ENABLED, Enabled.ENABLED);
            contentValues.put(Columns.ON_MODE_EXIT, OnModeExit.ON_ENTER);
            contentValues.put(Columns.ACTIVE, Active.INACTIVE);
            contentValues.put(Columns.ACTION_DESCRIPTION, actionDesc);
            contentValues.put(Columns.ACTION_PUBLISHER_KEY, actionPublisherKey);
            contentValues.put(Columns.MODAL, ModalTable.Modality.STATEFUL);
            contentValues.put(Columns.STATE_MACHINE_NAME, stateMachine);
            contentValues.put(Columns.ACTIVITY_INTENT, activityIntent);
            context.getContentResolver().insert(Schema.ACTION_TABLE_CONTENT_URI, contentValues);
            result = true;
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH);
            e.printStackTrace();
        }
        return result;
    }

    /** Checks and returns if the default value needs to be saved by quick actions. If no default
     * 	record exists for this action one will be created.
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key for the action that we look up in the database
     * @param actionDesc - action description to be saved in the db if no default action record exists
     * @param stateMachine - state machine to be saved in the db if no default action record exists
     * @param activityIntent - activity intent to be saved in the db if no default action record exists
     * @return - true if the default value needs to be stored by quick actions else false
     */
    public static boolean isDefaultValueNeeded(final Context context, final String actionPublisherKey,
            final String actionDesc, final String stateMachine,
            final String activityIntent) {

        if(LOG_DEBUG) Log.d(TAG, "Entering isDefaultValueNeeded");

        boolean result = false;
        long defaultRuleId = RulePersistence.getDefaultRuleId(context);

        if(defaultRuleId < 1) {
            RuleTable.initialize(context);
            defaultRuleId = RulePersistence.getDefaultRuleId(context);
        }

        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q + AND +
                             Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q;
        Cursor defaultActionCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

        if(defaultActionCursor != null) {
            try {
                if(defaultActionCursor.moveToFirst()) {
                    if(defaultActionCursor.getCount() == 1) {
                        if(defaultActionCursor.getInt(defaultActionCursor.getColumnIndex(Columns.ACTIVE)) == Active.INACTIVE)
                            result = true;
                    } else {
                        Log.e(TAG, "Error case - cannot have more than 1 record for the publisher key "+actionPublisherKey+" in the default record. Delete and reinsert");
                        context.getContentResolver().delete(Schema.ACTION_TABLE_CONTENT_URI, whereClause, null);
                        result = insertDefaultRecordForAction(context, actionPublisherKey, actionDesc, stateMachine, activityIntent, defaultRuleId);
                        result = true;
                    }
                }
                else {
                    Log.e(TAG, "Default record does not exist for this action - so create a default action record");
                    result = insertDefaultRecordForAction(context, actionPublisherKey, actionDesc, stateMachine, activityIntent, defaultRuleId);
                }

            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(! defaultActionCursor.isClosed())
                    defaultActionCursor.close();
            }
        }
        else
            Log.e(TAG, "Default Action Cursor fetched is null for whereClause "+whereClause);

        return result;
    }

    /** updates the CONFLICT_WINNER_FLAG column
     *
     * @param context - context
     * @param actionId - action ID in the table
     * @param conflictValue - ConflictWinner.WINNER or ConflictWinner.LOSER
     */
    public static void updateConflictWinnerFlag(final Context context, final long actionId,
            final String actionPubKey, final long conflictValue) {

        if(LOG_DEBUG) Log.d(TAG, "Entering updateConflictWinnerFlag");
        try {
            String whereClause = Columns._ID + EQUALS + Q + actionId + Q;

            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.CONFLICT_WINNER_FLAG, conflictValue);

            context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);

            if(conflictValue == ConflictWinner.WINNER) {
                whereClause = Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPubKey + Q
                              + AND + Columns._ID + NOT_EQUAL + Q + actionId + Q;

                contentValues = new ContentValues();
                contentValues.put(Columns.CONFLICT_WINNER_FLAG, ConflictWinner.LOSER);

                context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, contentValues, whereClause, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Update to action table failed");
            e.printStackTrace();
        }
    }

    /** Fetches the action for the action publisher key passed in for the default rule and
     *  if fired.
     *
     * @param context - context
     * @param actionPublisherKey - action publisher key of the action that is currently being processed
     * @param restoreDefault - boolean to indicate if the default needs to be restored or not
     */
    public static void fetchAndFireActionForDefaultRule(final Context context, final String actionPublisherKey,
            final boolean restoreDefault) {

        if(LOG_DEBUG) Log.d(TAG, "Entering fetchAndFireActionForDefaultRule");
        long defaultRuleId = RulePersistence.getDefaultRuleId(context);
        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q + AND +
                             Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPublisherKey + Q + AND +
                             Columns.ACTIVE + EQUALS + Q + Active.ACTIVE + Q;
        Cursor actionCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

        if(actionCursor != null) {
            try {
                if(actionCursor.moveToFirst()) {
                    String config = actionCursor.getString(actionCursor.getColumnIndex(Columns.CONFIG));
                    long id = actionCursor.getLong(actionCursor.getColumnIndex(Columns._ID));
                    Action.sendBroadcastIntentForAction(context, actionPublisherKey,
									Constants.DEFAULT_RULE_KEY,
									context.getString(R.string.default_rule), false,
									actionPublisherKey, COMMAND_REVERT, config, id);
                    if(restoreDefault)
                        ActionPersistence.setDefaultRecordForActionInactive(context, actionPublisherKey);
                }
                else
                    Log.e(TAG, "actionCursor.moveToFirst failed and whereClause = "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(! actionCursor.isClosed())
                    actionCursor.close();
            }
        }
        else
            Log.e(TAG, "Action Cursor fetched for Default Rule is null for whereClause "+whereClause);
    }

    /** checks for conflicts and fires the actions accordingly.
     *
     * @param context - context
     * @param actionCursor - action table cursor
     * @param _id - rule ID
     * @param ruleKey - rule key
     * @param ruleName - rule name
     */
    public static void checkForConflictsAndFireActions(final Context context, final Cursor actionCursor,
            final long _id, final String ruleKey, final String ruleName) {

        if(LOG_DEBUG) Log.d(TAG, "Entering checkForConflictsAndFireActions");

        ActionTable tbl = new ActionTable();
        ActionTuple t = null;
        for(int i = 0; i < actionCursor.getCount(); i++) {
        	t = tbl.toTuple(actionCursor);
            if(t.getEnabled() == Enabled.ENABLED && t.getActive() == Active.ACTIVE) {
                if(t.getModality() == ModalTable.Modality.STATELESS)
                    revertStatelessAction(context, t, t.get_id(), _id, ruleKey, ruleName);
                else
                    revertStatefulAction(context, t, ruleKey, ruleName);
            } else {
                Log.e(TAG, "Action "+ t.get_id() +" is not currently active ignoring the firing");
            }
            actionCursor.moveToNext();
        }
    }

    /** handles the firing of stateless actions
     *
     * @param context - context
     * @param actionCursor - action table cursor
     * @param actionId - action ID
     * @param _id - rule ID
     * @param ruleKey - rule key
     * @param ruleName - rule name
     */
    public static void revertStatelessAction(final Context context, final ActionTuple t, final long actionId,
                                           final long _id, final String ruleKey, final String ruleName) {

        if(LOG_DEBUG) Log.d(TAG, "Action is Stateless");
        if(LOG_DEBUG) Log.d(TAG, "for actionId "+actionId+"; action is "+ t.getPublisherKey());
        if(t.getOnExitModeFlag() == OnModeExit.ON_ENTER) {
		Action.sendBroadcastIntentForAction(context, t.getPublisherKey(), ruleKey,
									ruleName, false,
										t.getPublisherKey(), COMMAND_REVERT, t.getConfig(), t.get_id());
        } else if(t.getOnExitModeFlag() == OnModeExit.ON_EXIT){
		Action.sendBroadcastIntentForAction(context, t.getPublisherKey(), ruleKey,
									ruleName, false,
									t.getPublisherKey(), COMMAND_FIRE, t.getConfig(), t.get_id());
        }
    }

    /** handles the reverting of stateful actions after conflict resolution.
     * 	1. Determine if this action is at the top of conflict resolution stack.
     *  2. Revert the action if at the top of the conflict resolution stack.
     *
     * @param context - context
     * @param actionCursor - action table cursor
     * @param ruleKey - rule key
     * @param ruleName - rule name
     */
    public static void revertStatefulAction(final Context context, final ActionTuple t,
                                            final String ruleKey, final String ruleName) {
        if(LOG_DEBUG) Log.d(TAG, "Action is Stateful - conflict resolution needs to be done");
        Cursor conflictingActionsCursor = 
        		Conflicts.getConflictingActionsCursor(context, t.getPublisherKey(), Conflicts.Type.ACTIVE_ONLY);
        String action = null;
        if(conflictingActionsCursor != null) {
            try {
                if(conflictingActionsCursor.moveToFirst()) {
                    if(LOG_DEBUG) {
                        Log.e(TAG, "Dumping conflicts cursor");
                        DatabaseUtils.dumpCursor(conflictingActionsCursor);
                    }
                    if(t.get_id() == conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(Conflicts.ACTION_ID))) {
                        if(LOG_DEBUG) Log.d(TAG, "Current rule action is at the top of the conflict actions cursor stack - fetch next item in the stack and fire");
                        if(conflictingActionsCursor.moveToNext()) {
				long nextRuleId = conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(RuleTable.Columns._ID));
                            String nextRuleName = conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndex(RuleTable.Columns.NAME));
                            String nextRuleKey = conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndex(RuleTable.Columns.KEY));
                            String config = conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndexOrThrow(ActionTable.Columns.CONFIG));
                            if(LOG_DEBUG) Log.d(TAG, "Rule ID for the next rule is "+nextRuleId);
                            boolean restoreDefault = RulePersistence.isNextRuleDefaultRule(context, nextRuleKey);

                            if(restoreDefault){
                                Action.sendBroadcastIntentForAction(context,
						t.getPublisherKey(), nextRuleKey, nextRuleName,
						false, t.getPublisherKey(), COMMAND_REVERT, config, t.get_id());
                                setDefaultRecordForActionInactive(context, t.getPublisherKey());
                            } else
                                Action.sendBroadcastIntentForAction(context,
						t.getPublisherKey(), nextRuleKey, nextRuleName,
						false, t.getPublisherKey(), COMMAND_FIRE, config, t.get_id());

                            // Update the Columns.CONFLICT_WINNER_FLAG
                            updateConflictWinnerFlag(context, 
                            		conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(Conflicts.ACTION_ID)),
                                                     t.getPublisherKey(), ActionTable.ConflictWinner.WINNER);
                        } else {
                            Log.e(TAG, "Cannot move to the next item on the conflicts cursor - so set the default value");
                            fetchAndFireActionForDefaultRule(context, t.getPublisherKey(), true);
                            setDefaultRecordForActionInactive(context, t.getPublisherKey());
                            // Update the Columns.CONFLICT_WINNER_FLAG
                            updateConflictWinnerFlag(context, t.get_id(), t.getPublisherKey(), ActionTable.ConflictWinner.LOSER);
                        }
                    } else {
                        if(LOG_DEBUG) Log.d(TAG, "Conflict: id's do not match actionId = "+ t.get_id() +
                        		"; conflict cusror id = "+conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(Columns._ID)));
                        String state = context.getString(R.string.action_not_fired) + BLANK_SPC +
                                       conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndex(RuleTable.Columns.KEY));
                        DebugTable.writeToDebugViewer(context, DebugTable.Direction.INTERNAL, state, ruleName, ruleKey,
                        		SMARTRULES_INTERNAL_DBG_MSG, action, null,
                        		Constants.PACKAGE, Constants.PACKAGE);
                        // Update the Columns.CONFLICT_WINNER_FLAG
                        updateConflictWinnerFlag(context, t.get_id(), t.getPublisherKey(), ConflictWinner.LOSER);
                    }
                }
                else
                    Log.e(TAG, "conflictingActionsCursor.moveToFirst() failed for action Id "+ t.get_id());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(! conflictingActionsCursor.isClosed())
                    conflictingActionsCursor.close();
            }
        }
        else
            Log.e(TAG, "conflictingActionsCursor is null for action Id "+ t.get_id());
    }

    /** Returns the list of action publisher keys corresponding to a Rule Key.
    *
    * @param context
    * @param ruleFk - Rule table foreign key
    */
    public static List<String> getPublisherKeys(Context context, long ruleFk) {

        List<String> actionKeys = new ArrayList<String>();

        if (context != null) {

            Cursor cursor = null;
            String whereClause = ActionTable.Columns.PARENT_FKEY + DbSyntax.EQUALS + DbSyntax.Q +
                                 ruleFk + DbSyntax.Q;
            try {

                cursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI,
                         new String[] { ActionTable.Columns.ACTION_PUBLISHER_KEY},
                         whereClause, null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        do {
                            actionKeys.add(cursor.getString(cursor.getColumnIndex(ActionTable.Columns.ACTION_PUBLISHER_KEY)));
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
        }

        return actionKeys;
    }

    /** Returns the list of rule keys corresponding to an action publisher Key.
    *
    * @param context
    * @param actionPubKey - action publisher key of the action
    */
    public static List<String> getRuleKeys(Context context, String actionPubKey) {
        List<String> ruleKeys = new ArrayList<String>();

        if (context != null && actionPubKey != null) {

            Cursor cursor = null;
            String whereClause = ActionTable.Columns.ACTION_PUBLISHER_KEY + EQUALS + Q +
                    actionPubKey + Q;
            try {

                cursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI,
                         new String[] { ActionTable.Columns.PARENT_FKEY},
                         whereClause, null, null);
                if (cursor != null && cursor.moveToFirst() ){
                        do {
                            long ruleId = cursor.getLong(cursor.getColumnIndex(ActionTable.Columns.PARENT_FKEY));
                            ruleKeys.add(RulePersistence.getRuleKeyForRuleId(context, ruleId));
                        } while(cursor.moveToNext());

                }
            } catch(Exception e) {
                Log.e(TAG, "Query failed for " + whereClause);
            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
        	Log.e(TAG, " Input parameters are null + context : " + context + " actionPubKey : " + actionPubKey);
        }

        return ruleKeys;
    }
    /** Returns the Action Table Cursor
     *  Note: The caller is expected to close the Cursor
     *
     *  @param context
     *  @param ruleId - the rule ID for Actions
     *
     *  @return - actionCursor - Action Table Cursor
     */
    public static Cursor getActionCursor(Context context, final long ruleId) {

        Cursor actionCursor = null;

        if (context != null)
        {
            try {
                String tableSelection = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
                actionCursor = context.getContentResolver().query(
                                   Schema.ACTION_TABLE_CONTENT_URI,
                                   null, tableSelection, null,  null);
            } catch(Exception e) {
                Log.e(TAG, "Query failed for Action Table");
            }
        } else {
            Log.e(TAG,"context is null");
        }

        return actionCursor;
    }

    /** fetches and returns true if the value of Enabled column in action table for the publisher
     * key  and rule id passed in is equal to ActionTable.Enabled.ENABLED.
    *
    * @param context - context
    * @param publisherKey - publisher key in the rule table
    * @param ruleId - Rule Id for which Enabled value is retrieved
    * @return - true if the Enabled column value of the action that matches the publisher key in the table
    *           is set to ActionTable.Enabled.ENABLED
    */
    public static boolean isActionEnabled(final Context context, final long actionId, final long ruleId) {
        int enabled = 0;
        boolean isEnabled = false;
        String whereClause = ActionTable.Columns._ID + EQUALS + Q +
        		actionId + Q  + AND + ActionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
        Cursor cursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, new String[] {Columns.ENABLED},
                            whereClause, null, null);
        if(cursor != null) {
            try {
                if(cursor.moveToFirst()) {
                    enabled = cursor.getInt(cursor.getColumnIndex(Columns.ENABLED));
                } else
                    Log.e(TAG, "cursor.moveToFirst() failed for "+whereClause);
            } catch (Exception e) {
                Log.e(TAG, PROVIDER_CRASH);
                e.printStackTrace();
            } finally {
                if(!cursor.isClosed())
                    cursor.close();
            }
        } else
            Log.e(TAG, " cursor fetched is null for whereClause "+whereClause);
        if(enabled == Enabled.ENABLED) isEnabled = true;
        if(LOG_DEBUG) Log.d(TAG, "returning Enabled as "+ isEnabled +" for publisherKey "+ actionId);
        return isEnabled;
    }
    
    /**
     * This method helps to find out if there any blocks which are enabled and 
     * un configured.
     * TODO Improve the query to query enabled action blocks with null config and 
     * if the query returns any records, this method can return true.
     * @param context
     * @param ruleId
     * @return boolean - true - un configured enabled blocks exist
     */
    public static boolean anyUnconfiguredEnabledActions(final Context context, long ruleId) {
   	 
	    if (LOG_DEBUG) Log.d(TAG,"Action: anyConnectedUnConfiguredBlocks");
	   	 
	   	// No un configured connected blocks
	   	boolean status = false;
	   	 
	    String columns[] = {ActionTable.Columns.CONFIG};
	   	 
	    String whereClause = ActionTable.Columns.PARENT_FKEY  + EQUALS + Q + ruleId + Q +AND +
			  				  ActionTable.Columns.ENABLED + EQUALS + Q + Enabled.ENABLED  +Q;
	      
	   	Cursor actCursor = null;
	    try {
		    actCursor = context.getContentResolver().query(
	                           Schema.ACTION_TABLE_CONTENT_URI,
	                           columns, whereClause, null,  null);	   		    
		    if(actCursor == null){
		        Log.e(TAG, "Null Cursor in anyConnectedUnConfiguredBlocks");
		        return status;
		    }
	    	  
		    if(actCursor.moveToFirst()){
	            do{
	                String config = actCursor.getString(actCursor.getColumnIndexOrThrow(
	                						ActionTable.Columns.CONFIG));
	                if (status = Util.isNull(config))  break;  
	            } while(actCursor.moveToNext());
		    } 
	    } catch (IllegalArgumentException e) {
	        Log.e(TAG, "IllegalArgumentException in anyConnectedUnConfiguredBlocks");
	        e.printStackTrace();
	    } finally {
	    	if (actCursor != null) actCursor.close();
	    } 
	        
	    return status;
    }

}
