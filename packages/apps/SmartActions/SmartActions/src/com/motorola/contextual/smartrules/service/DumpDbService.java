/*
 * @(#)DumpDbService.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * csd053       2012/02/02  NA                  Initial version
 */

package com.motorola.contextual.smartrules.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.Action;
import com.motorola.contextual.smartrules.db.business.ActionList;
import com.motorola.contextual.smartrules.db.business.Condition;
import com.motorola.contextual.smartrules.db.business.ConditionList;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.publishermanager.PublisherManager;
import com.motorola.contextual.smartrules.util.Util;

/**
 * This class is an Intent based service that handles adding Rule info,
 * specifically Actions, Triggers of accepted rules and their state.
 * The service gets triggered on Bootup and every week post Sun midnight
 * 
 * <code><pre>
 * CLASS:
 *     Extends IntentService.
 *
 * RESPONSIBILITIES:
 *     Writing data to debug db
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class DumpDbService extends IntentService implements Constants, DbSyntax {

	private static final String TAG = DumpDbService.class.getSimpleName();
	
	private static final String RULE = "RULE";
	private static final String ACTION = "ACTION";
	private static final String TRIGGER = "CONDITION";

    public static final String BOOTUP_REQUEST = "Bootup";
    public static final String WEEKLY_UPDATE_REQUEST = "Weekly Update";
    public static final String REGULAR_REQUEST = "REGULAR";
    
	public static final String SERVICE_TYPE = PACKAGE + ".servicetype";
	
	private Context mContext = null;
	
	public DumpDbService() {
		super(TAG);
	}
	
	public DumpDbService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mContext = this;

		if(intent == null) {
			Log.e(TAG, "null intent exit service");
		} 
		else {
			String serviceType = intent.getStringExtra(SERVICE_TYPE);
			if (serviceType == null)
				Log.e(TAG,"Invalid Service Type");
			else {
				if (serviceType.equals(REGULAR_REQUEST)) {
					long ruleId = intent.getLongExtra(RuleTable.Columns._ID, DEFAULT_RULE_ID);
					if(ruleId != DEFAULT_RULE_ID)
						startThreadToWriteActionCondition(ruleId);
				}
				else { 
				    if(LOG_DEBUG) Log.d(TAG, "in onHandleIntent fetch the user visibles rules view cursor for "+serviceType);
				    startThreadToLogTheRules(serviceType);
				}
			}
		}
	}
	
	/**
	 * Gets the rule cursor to get list of Actions, Triggers
	 * and write it to the debug table
	 * 
	 * @param serviceType
	 */
    private void startThreadToLogTheRules(final String serviceType) {
    	Thread thread = new Thread() {
    		public void run() {
    			Cursor cursor = RulePersistence.getDebugDumpRulesCursor(mContext);
    			
    			if(cursor == null) {
    				Log.e(TAG, " cursor returned is null");
    			} else {
    				try{
	    				if(cursor.moveToFirst()) {
	    					if(LOG_DEBUG){
	    						Log.d(TAG, " Dumping the cursor of count "+cursor.getCount());
	        					DatabaseUtils.dumpCursor(cursor);
	    					}
	    					if (cursor.getCount() == 0)
	    					{
	    						DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
	    								null, null, null, SMARTRULES_INTERNAL_DBG_MSG, 
	    								null, serviceType, Constants.NO_RULES,
	    								Constants.PACKAGE, Constants.PACKAGE, null, null);
	    					}
	    					else
	    					{
	    						writeSettingsStatus(serviceType);
	    					}
	    					for(int i = 0; i < cursor.getCount(); i++) {
	    						long _id = cursor.getLong(cursor.getColumnIndex(RuleTable.Columns._ID));
	    						writeRuleActionConditionToDebugViewer( _id, serviceType);
	    						cursor.moveToNext();
	    					}
	    					
	    				} else
	    				{
	    					DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
    								null, null, null, SMARTRULES_INTERNAL_DBG_MSG, 
    								null, serviceType, Constants.NO_RULES,
    								Constants.PACKAGE, Constants.PACKAGE, null, null);
	    					Log.e(TAG, "cursor.moveToFirst failed");
	    				}

	    			} catch (Exception e) {
	       				e.printStackTrace();
	       			} finally { 
	       				cursor.close();
	       			}
    			} // end of else
    			    			
    		} //end of run()
    	};   	
    	thread.setPriority(Thread.MIN_PRIORITY);
    	thread.start();
    }
    
    /**
     * Writes the Settings status of SmartAction app
     */
    
    private void writeSettingsStatus(final String serviceType) {
    	
        Boolean status = Util.getSharedPrefStateValue(this, RULE_STATUS_NOTIFICATIONS_PREF, TAG);
	    if(status) {
			DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
					TRUE, null, null, 
					SMARTRULES_INTERNAL_DBG_MSG, SETTINGS_RULE, 
					serviceType, null,
					Constants.PACKAGE, Constants.PACKAGE, null, null);
	   } else {
		   DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
					FALSE, null, null, 
					SMARTRULES_INTERNAL_DBG_MSG, SETTINGS_RULE, 
					serviceType, null,
					Constants.PACKAGE, Constants.PACKAGE, null, null);
	   }
        

       status = Util.getSharedPrefStateValue(this, NOTIFY_SUGGESTIONS_PREF, TAG);
	   if(status) {
		   DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
					TRUE, null, null, 
					SMARTRULES_INTERNAL_DBG_MSG, SETTINGS_SUGG, 
					serviceType, null,
					Constants.PACKAGE, Constants.PACKAGE, null, null);
       }else {
    	   DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
					FALSE, null, null, 
					SMARTRULES_INTERNAL_DBG_MSG, SETTINGS_SUGG, 
					serviceType, null,
					Constants.PACKAGE, Constants.PACKAGE, null, null);
            }
	   
      if(Util.isMotLocConsentAvailable(mContext) ) {
    	  DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
					TRUE, null, null, 
					SMARTRULES_INTERNAL_DBG_MSG, SETTINGS_LOC, 
					serviceType, null,
					Constants.PACKAGE, Constants.PACKAGE, null, null);
      }else {
          DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
  					FALSE, null, null, 
  					SMARTRULES_INTERNAL_DBG_MSG, SETTINGS_LOC, 
  					serviceType, null,
  					Constants.PACKAGE, Constants.PACKAGE, null, null);
         }

    }
    
    /**
     * Writes the passed rule id's actions and triggers to debug viewer db
     * 
     * @param _id
     * @param serviceType
     */
    private void writeRuleActionConditionToDebugViewer(long _id, final String serviceType){
    	Rule rule = RulePersistence.fetchFullRule(mContext, _id);
    	
    	if (rule == null) {
    		Log.e(TAG, "no rule in the DB");
    		return;
    	}
    	
		String status = (rule.getEnabled() == RuleTable.Enabled.ENABLED) ? 
							RuleTable.StateForActionsAndConditions.ENABLED_STATE : RuleTable.StateForActionsAndConditions.DISABLED_STATE;
    	String state = getRuleState(rule);
		DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
							status, String.valueOf(rule.getCreatedDateTime()),
							rule.getKey(), SMARTRULES_INTERNAL_DBG_MSG, 
							state, serviceType, RULE,
							Constants.PACKAGE, Constants.PACKAGE, null, null);
		
		ConditionList<Condition> conditionList = rule.getConditionList();
		if(conditionList == null) {
			Log.e(TAG, "no condition for rule with key "+rule.getKey());
		} else {
			for(int j = 0; j < conditionList.size(); j++) {
				Condition condition = (Condition) conditionList.get(j);
				String conditiontype = TRIGGER;
				if( conditionList.get(j).getEnabled() != ConditionTable.Enabled.ENABLED)
					conditiontype = conditiontype + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
				String term_key_str = Util.getLastSegmentPublisherKey(condition.getPublisherKey());
				PublisherManager pubMgr = PublisherManager.getPublisherManager(mContext, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.CONDITION);
				if (pubMgr.isValidPublisher(condition.getPublisherKey())) {
					DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
								condition.getDescription(), rule.getName(), rule.getKey(), 
								SMARTRULES_INTERNAL_DBG_MSG, term_key_str, 
								serviceType, conditiontype,
								Constants.PACKAGE, Constants.PACKAGE, null, null);
				}
				
			}
		}
		
		ActionList<Action> actionList = rule.getActionList();
		if(actionList == null) {
			Log.e(TAG, "no actions for rule with key "+rule.getKey());
		} else {
			for(int k = 0; k < actionList.size(); k++) {
				Action action = actionList.get(k);
				String term_key_str = Util.getLastSegmentPublisherKey(action.getPublisherKey());
				String actiontype = ACTION;
				if( actionList.get(k).getEnabled() != ActionTable.Enabled.ENABLED)
					actiontype = actiontype + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
				PublisherManager pubMgr = PublisherManager.getPublisherManager(mContext, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.ACTION);
				if (pubMgr.isValidPublisher(action.getPublisherKey())) {
					DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
								action.getDescription(), rule.getName(), rule.getKey(), 
								SMARTRULES_INTERNAL_DBG_MSG, term_key_str, 
								serviceType,  actiontype,
								Constants.PACKAGE, Constants.PACKAGE, null, null);
				}
			}
		}
    }

    /** returns the state for the current rule based on the source,
     *  created date time and last edited date time columns.
     * 
     * @param rule - rule type of the current rule
     * @return - a string of the current state of the rule
     */
    private String getRuleState(Rule rule) {
    	String state = null;
    	
    	switch (rule.getSource()) {
    		case RuleTable.Source.USER:
    			state = USER_CREATED_RULE_DBG_MSG;
    			break;
    			
    		case RuleTable.Source.SUGGESTED:
    		case RuleTable.Source.FACTORY:
    			
    			if(!rule.isAdoptedSuggestion())
    				state = SUGG_INBOX_DBG_MSG;
    			else if (rule.isRuleEdited()) {
    				if(rule.isSuggested())
    					state = SUGG_ACCEPTED_DBG_MSG + EDIT_DBG_MSG;
    				else if(rule.isSample())
    					state = SAMPLE_RULE_ACCEPTED_DBG_MSG + EDIT_DBG_MSG;
    			}
    			else {
    				if(rule.isSuggested())
    					state = SUGG_ACCEPTED_DBG_MSG; 
    				else if(rule.isSample())
    					state = SAMPLE_RULE_ACCEPTED_DBG_MSG;
    			}
    			break;
    	}
    	
    	Log.d(TAG, "Returning state as "+state);
    	return state;
    }
    
	/**
	 * Gets the particular rule ID to get list of Actions, Triggers
	 * and write it to the debug table
	 * 
	 * @param serviceType
	 */
    private void startThreadToWriteActionCondition(final long _id) {
    	Thread thread = new Thread() {
    		public void run() {
    				writeActionConditionToDebugViewer(_id);
	    		    			    			
    		} //end of run()
    	};   	
    	thread.setPriority(Thread.MIN_PRIORITY);
    	thread.start();
    }
    
    /**
     * Writes the passed actions and triggers to debug viewer db
     * 
     * @param _id
     */
    private void writeActionConditionToDebugViewer(long _id){
    	Rule rule = RulePersistence.fetchFullRule(mContext, _id);
    	
    	if (rule == null) {
    		Log.e(TAG, "no rule in the DB");
    		return;
    	}
		
		ConditionList<Condition> conditionList = rule.getConditionList();
		if(conditionList == null) {
			Log.e(TAG, "no condition for rule with key "+rule.getKey());
		} else {
			for(int j = 0; j < conditionList.size(); j++) {
				Condition condition = (Condition) conditionList.get(j);
				String conditiontype = TRIGGER;
				if( conditionList.get(j).getEnabled() != ConditionTable.Enabled.ENABLED)
					conditiontype = conditiontype + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
				String term_key_str = Util.getLastSegmentPublisherKey(condition.getPublisherKey());
				PublisherManager pubMgr = PublisherManager.getPublisherManager(mContext, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.CONDITION);
				if (pubMgr.isValidPublisher(condition.getPublisherKey())) {				
					DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
								condition.getDescription(), rule.getName(), rule.getKey(), 
								SMARTRULES_INTERNAL_DBG_MSG, term_key_str, conditiontype, null,
								Constants.PACKAGE, Constants.PACKAGE, null, null);
				}
			}
		}
		
		ActionList<Action> actionList = rule.getActionList();
		if(actionList == null) {
			Log.e(TAG, "no actions for rule with key "+rule.getKey());
		} else {
			for(int k = 0; k < actionList.size(); k++) {
				Action action = actionList.get(k);
				String term_key_str = Util.getLastSegmentPublisherKey(action.getPublisherKey());
				String actiontype = ACTION;
				if( actionList.get(k).getEnabled() != ActionTable.Enabled.ENABLED)
					actiontype = actiontype + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
				PublisherManager pubMgr = PublisherManager.getPublisherManager(mContext, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.ACTION);
				if (pubMgr.isValidPublisher(action.getPublisherKey())) {
					DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
								action.getDescription(), rule.getName(), rule.getKey(), 
								SMARTRULES_INTERNAL_DBG_MSG, term_key_str, actiontype,  null,
								Constants.PACKAGE, Constants.PACKAGE, null, null);
				}
			}
		}
    }
}
