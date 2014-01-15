/*
 * @(#)Rule.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/05/09 NA				  Initial version
 * a18385        2011/06/20 NA                Added Rule business logic to this file
 *
 */
package com.motorola.contextual.smartrules.uiabstraction;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.Source;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;
import com.motorola.contextual.smartrules.db.table.RuleTuple;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.publishermanager.PublisherManager;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderUtils;
import com.motorola.contextual.smartrules.service.DumpDbService;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel.ActionInteractionModelList;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel.ConditionInteractionModelList;
import com.motorola.contextual.smartrules.util.Util;

/** 
* This implements the business rules around the SmartRules Rule as 
* well as the corresponding Actions and Conditions for 1 rule. This class holds the list of
* related conditions and actions. 
*
*<code><pre>
* CLASS:
* 	Implements all information exchange that feeds logic that converge to a RuleTuple transaction
*   All the data stays in RuleTuple , this class wraps up fetching and updating that data
*   from the UI
*
* RESPONSIBILITIES:
* 	implement business-layer of the Rule.
*
* COLABORATORS:
*   RuleTuple - has all the data that can be persisted to DB
* 	RulePersistence - knows how to persist an instance of this class
* 	Action - implements the business layer of the individual Actions.
* 	Condition - implements the business layer of the individual Conditions.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class RuleInteractionModel implements Constants, RulesBuilderConstants, DbSyntax {
	
	private static final String TAG = RuleInteractionModel.class.getSimpleName();
	private Rule mRuleInst;
	private ActionInteractionModelList mActionList;
	private ConditionInteractionModelList mConditionList;
	private boolean mIsPresetOrSuggested;
	private boolean mIsCopy;
	private boolean mQuickSaveOfActiveRule;
	private boolean mIsEditWhileActive;
	private boolean mHasRuleIconChanged;
	private boolean mIsInSaveState;
	private boolean isRuleModified;
    private ArrayList<String> mOldConfigPubKeyList = new ArrayList<String>();

	/**
	 * Basic constructor
	 * Not currently used, can be get rid of
        * @param context TODO
	 */
	public RuleInteractionModel(){
		// we create the Rule instance in this case in setRule
		setRule(null); 			
		mIsPresetOrSuggested = false;
		mIsCopy = false;
		mQuickSaveOfActiveRule = false;
		mHasRuleIconChanged = false;
		mIsInSaveState = false;
		mActionList = new ActionInteractionModelList();
		mConditionList = new ConditionInteractionModelList();
	}
	
	/**
	 * constructor from a rule tuple
	 * @param tuple
	 */
	public RuleInteractionModel(Rule rule) {
		setRule(rule);
		this.mActionList = new ActionInteractionModelList();
		this.mConditionList = new ConditionInteractionModelList();
		mIsPresetOrSuggested = false;
		mIsCopy = false;
		mQuickSaveOfActiveRule = false;
		mHasRuleIconChanged = false;
		mIsInSaveState = false;
	}


	/** constructor from a rule tuple, action list and condition list 
	 *  Needs to be investigated and if not required to be removed
	 *  
	 * @param tuple - RuleTuple, can be obtained via new RuleTupe(field list); 
	 * @param actions - list of actions for this rule
	 * @param conditions - list of conditions for this rule
	 */
	public RuleInteractionModel(Rule rule, ActionInteractionModelList actions, ConditionInteractionModelList conditions) {
		setRule(rule);
		this.mActionList = actions;
		this.mConditionList = conditions;
	}
	
	/**This function updates the rule key by concatenating the rule name with the rule key, so that it 
	  * makes it easy to identify the sensors that are created. 
      * 
	  * @param name - Rule name
	  * @param key - Rule key
	  */
	  public void setUpdatedRuleKey(){
		  String name = mRuleInst.getName();
		  if (name != null){
			  // IKINTNETAPP-220 - The rule key needs to be follow this convention 
			  // com.motorola.contextual.rule%20name.datetime
			  String newKey = RULE_KEY_PREFIX + name.replaceAll(" ", "%20") + "." 
					  				+  new Date().getTime();
			  // IKINTNETAPP-248 - Remove all non-alphanumeric characters from the key, 
			  // except for '.' and '%' to maintain readabi
			  newKey = newKey.replaceAll("[^a-zA-Z0-9.%]", "");
			  mRuleInst.setKey(newKey);
		  }
	  }
	
	/** getter - isPresetOrSuggested flag
	 * 
	 * @return
	 */
	public boolean getIsPresetOrSuggestedFlag(){
		return mIsPresetOrSuggested;
	}
	
	/** setter - setIsPresetOrSuggestedFlag
	 *
	 */
	public void setIsPresetOrSuggestedFlag(){
		mIsPresetOrSuggested = isPresetOrSuggestedRule();
	}
	
	/** getter - isCopyFlag
	 * 
	 * @return
	 */
	public boolean getIsCopyFlag(){
		return mIsCopy;
	}
	
	/** setter - isCopyFlag value
	 * 
	 * @param isCopy
	 */
	public void setIsCopyFlag(boolean isCopy){
		this.mIsCopy = isCopy;
	}

	/** setter - edit While Active flag
	 * 
	 * @param editWhileActive
	 */
	public void setEditWhileActiveFlag(boolean editWhileActive){
		mIsEditWhileActive = editWhileActive;
	}
	
	/** getter - edit While Active flag
	 * 
	 * @return
	 */
	public boolean getEditWhileActiveFlag(){
		return mIsEditWhileActive;
	}
	
	/**
     * This function determines whether a rule is manual or not. A rule is manual if there are no 
     * enabled conditions.
     * 
     */
    public boolean isManualRule(){
    	boolean manualRule = false;
    	ConditionInteractionModelList conditions = this.getConditionList();
    	if (!conditions.isConnectedConditionPresent()) {
    	    manualRule = true;
    	}
    	return manualRule;
    }
    

    /**
     * This function determines whether a rule is valid or not. A rule is not valid if there are no 
     * actions and no conditions or if there are no actions but there are conditions.
     * 
     */
    public boolean isValidRule(){
       	boolean validRule = true;
        ActionInteractionModelList actions = this.getActionList();
    	if (!actions.isConnectedActionPresent() || !this.isCompletelyConfigured()) 
    	    validRule = false;
    	return validRule;
    }
	  
	/** Determines if the rule is a preset or suggested rule
	 */
	public boolean isPresetOrSuggestedRule() {
		boolean isPresetOrSuggested = false;
		int source = mRuleInst.getSource();
		int suggState = mRuleInst.getSuggState();
		if ((mRuleInst.get_id() == DEFAULT_RULE_ID && source == Source.FACTORY)
				|| (source == Source.SUGGESTED && suggState != SuggState.ACCEPTED))
			isPresetOrSuggested = true;
		return isPresetOrSuggested;
	}
    
    /** Checks if a rule is completely configured. A Rule is identified to be completely 
     * configured if all the conditions in that rule are configured.
     * 
     * @return True if it is completely configured
     */
    public boolean isCompletelyConfigured() {
    	boolean isConfigured = true;
    	ConditionInteractionModelList conditions = this.getConditionList();
    	ActionInteractionModelList actions = this.getActionList();
    	
    	if ( conditions.isUnconfiguredConditionPresent() || actions.isUnconfiguredActionPresent() )
    		isConfigured = false;
    	return isConfigured;
    }
   
	/** setter - QuickSaveOfActiveRuleFlag
	 * 
	 * @param quickSave
	 */
	public void setQuickSaveOfActiveRuleFlag(boolean quickSave){
		this.mQuickSaveOfActiveRule = quickSave;
	}
	
	/** getter - QuickSaveOfActiveRuleFlag
	 * 
	 * @return
	 */
	public boolean getQuickSaveOfActiveRuleFlag(){
		return mQuickSaveOfActiveRule;
	}

	/** getter - Action list - the list of all the actions for this rule
	 * 
	 * @return the actionList
	 */
	public ActionInteractionModelList getActionList() {
		return mActionList;
	}

	/** setter - sets the Action list for this rule
	 * 
	 * @param actionList the actionList to set
	 */
	public void setActionList(ActionInteractionModelList actionList) {
		mActionList = actionList;
	}

	/** setter - sets the boolean flag to indicate the rule icon has changed.
	 * 
	 * @param mHasRuleIconChanged the mHasRuleIconChanged to set
	 */
	public void setHasRuleIconChanged(boolean mHasRuleIconChanged) {
		this.mHasRuleIconChanged = mHasRuleIconChanged;
	}

	/** getter - returns true if the rule icon has changed else false.
	 * 
	 * @return the mHasRuleIconChanged
	 */
	public boolean getHasRuleIconChanged() {
		return mHasRuleIconChanged;
	}

	/** getter - returns true if save button is enabled else false.
	 * 
	 * @return the mSaveButtonEnabled
	 */
	public boolean isSaveButtonEnabled() {
		return mIsInSaveState;
	}

	/** setter - sets the boolean flag to indicate if save button is enabled or not.
	 * 
	 * @param mIsInSaveState the mSaveButtonEnabled to set
	 */
	public void setSaveButtonEnabled(boolean mIsSaveButtonEnabled) {
		this.mIsInSaveState = mIsSaveButtonEnabled;
	}

	/**
	 * @return the isRuleModified
	 */
	public boolean isRuleModified() {
		return isRuleModified;
	}

	/**
	 * @param isRuleModified the isRuleModified to set
	 */
	public void setRuleModified(boolean isRuleModified) {
		this.isRuleModified = isRuleModified;
	}
	
	/** getter - Condition list - the list of all the conditions for this rule
	 * 
	 * @return the conditionList
	 */
	public ConditionInteractionModelList getConditionList() {
		return mConditionList;
	}

	/** setter - Condition List
	 * 
	 * @param conditionList the conditionList to set
	 */
	public void setConditionList(ConditionInteractionModelList conditionList) {
		mConditionList = conditionList;
	}
	
	/** Get the number of actions already added for a particular block
	 * 
	 * @param blockId
	 * @return  number of actions
	 */
	public int getNumberOfActionEntriesForBlockId(String blockId) {
		int i = 0, numEntries = 0;
		while (i<this.mActionList.size()) {
			if (this.mActionList.get(i).getBlockId().equals(blockId)) 
				numEntries++;
			i ++;
	  }
	  return numEntries;
	}
	
	/** Get the number of conditions already added for a particular block
	 * 
	 * @param blockId
	 * @return number of conditions
	 */
	public int getNumberOfConditionEntriesForBlockId(String blockId) {
		int i = 0, numEntries = 0;
		while (i<this.mConditionList.size()) {
			if (this.mConditionList.get(i).getBlockId().equals(blockId)) 
				numEntries++;
			i ++;
	  }
	  return numEntries;
	}
	
	/** Get the action corresponding to the block instance id
	 * 
	 * @param blockInstanceId
	 * @return Action
	 */
	public ActionInteractionModel getActionForBlockInstanceId(String blockInstanceId)
	{
		ActionInteractionModel result = null;
		int i =0;
		while (result == null && i<this.mActionList.size()) {
			if (this.mActionList.get(i).getBlockInstanceId().equals(blockInstanceId))
              result = this.mActionList.get(i);
          i++;
		}
		return result;
	}
	
	/** Get the condition corresponding to the block instance id
	 * 
	 * @param blockInstanceId
	 * @return Condition
	 */
	public ConditionInteractionModel getConditionForBlockInstanceId(String blockInstanceId)
	{
		ConditionInteractionModel result = null;
		int i =0;
		while (result == null && i<this.mConditionList.size()) {
			if (this.mConditionList.get(i).getBlockInstanceId().equals(blockInstanceId))
              result = this.mConditionList.get(i);
          i++;
		}
		return result;
	}
	
	/** This function inserts a new rule into the Rules DB, along with its actions and conditions.
	 * There is a requirement to write debug info to the Debug table for every action and condition
	 * that is inserted.
	 * 
	 * @param context
	 */
	public void insertNewRule(Context context){
		///RuleTuple ruleTuple = this.toRuleTuple();
		Uri ruleKey = RulePersistence.insertRule(context, mRuleInst);
		
		ActionInteractionModelList actions = this.getActionList();
    	ConditionInteractionModelList conditions = this.getConditionList();
    	
    	if (ruleKey != null){
	    	Long newRuleId = Long.parseLong(ruleKey.getLastPathSegment());
	    	mRuleInst.set_id(newRuleId);
	    	
	        //For every action in the action container that is not marked as deleted, add to the Action table
	        for ( int i = 0; i < actions.size(); i++ ) {
	            if (!actions.get(i).getState().equals(EditType.DELETED)) {
	            	this.insertAction(context, actions.get(i));
	            	String debugString = ACTION_ON_CREATE;
	            	if(actions.get(i).getAction().getEnabled() != ActionTable.Enabled.ENABLED)
	            		debugString = debugString + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
	            	PublisherManager pubMgr = PublisherManager.getPublisherManager(context, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.ACTION);
					if (pubMgr.isValidPublisher(actions.get(i).getAction().getPublisherKey())) {
						RulesBuilderUtils.writeInfoToDebugTable(context, DebugTable.Direction.OUT, SMARTRULES_INTERNAL_DBG_MSG,
	                		mRuleInst.getName(), mRuleInst.getKey(),actions.get(i).getAction().getDescription(),
	                		Util.getLastSegmentPublisherKey(actions.get(i).getAction().getPublisherKey()), debugString);
					}
	            }
	        }
	       //For every condition in the condition container that is not marked as deleted, add to the condition table
	        for ( int i = 0; i < conditions.size(); i++ ) {
	            if (!conditions.get(i).getState().equals(EditType.DELETED)) {
	            	this.insertCondition(context, conditions.get(i));
	            	
	            	String debugString = CONDITION_ON_CREATE;
	            	if(conditions.get(i).getCondition().getEnabled() != ConditionTable.Enabled.ENABLED)
	            		debugString = debugString + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
	            	PublisherManager pubMgr = PublisherManager.getPublisherManager(context, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.CONDITION);
					if (pubMgr.isValidPublisher(conditions.get(i).getCondition().getPublisherKey())) {
						RulesBuilderUtils.writeInfoToDebugTable(context, DebugTable.Direction.OUT, VSM_OUT_MESSAGE,
	            			mRuleInst.getName(), mRuleInst.getKey(),
	            			conditions.get(i).getCondition().getDescription(),
	            			Util.getLastSegmentPublisherKey(conditions.get(i).getCondition().getPublisherKey()), debugString);
					}
	           }
	        }
	        String rulePub = mRuleInst.getPublisherKey();
	        String pkgName = context.getPackageName();
	        if(rulePub != null) {
                       pkgName = Util.getPackageNameForRulePublisher(context, rulePub);
	        }
	        IconPersistence.insertIcon(context, mRuleInst.getIcon(), pkgName, (int)mRuleInst.get_id());
    	}
    	else 
    		Log.e(TAG, "Rule key is null");
	}
	
	/** This function updates an existing rule for all the 3 tables - Rule, Action and Condition.
	 * 
	 * @param context
	 */
	public void updateExistingRule(Context context){
		// NOTE: This order should not be changed. The order is to make sure
		// the changes made via CR IKSTABLE6-16442 work properly.
		this.updateConditionTable(context);
		this.updateActionTable(context);
		this.updateRuleTable(context);
		if(this.getHasRuleIconChanged()) {
			updateIconTable(context);
		}
	}
		
	
	/** This function updates the Rule table with values that would have changed at the Rule level, after 
	* a rule has been edited.
	*
	* @param context
	 */
	private void updateIconTable(Context context){
			IconPersistence.updateIcon(context, mRuleInst.getIcon(), (int)mRuleInst.get_id());
	}
	
	/** This function updates the Rule table with values that would have changed at the Rule level, after 
	 * a rule has been edited.
	 * 
	 * @param context
	 */
	private void updateRuleTable(Context context){
		ContentValues updateValues = new ContentValues();
    	this.getUpdatedRuleTableValues(context, updateValues);
    	String whereClause = RuleTableColumns._ID + EQUALS + Q + mRuleInst.get_id() + Q;
    	RulePersistence.updateRule(context, updateValues, whereClause);
	}
	
	
	/** This function updates the Action table for a rule that has been edited.
	 *  There is a requirement to write debug info to the Debug table for every action 
	 *  that is edited (added/edited/deleted).
	 * 
	 * @param context
	 */
	private void updateActionTable(Context context){
		ActionInteractionModelList actions = this.getActionList();
		for(int i=0; i < actions.size(); i++) {
			String debugString = null;
		    if (actions.get(i).getState().equals(EditType.ADDED)) {
                this.insertAction(context, actions.get(i));
                debugString = ACTION_ON_CREATE;
            }
            else if (actions.get(i).getState().equals(EditType.EDITED)) {
                this.editExistingAction(context, actions.get(i));
                debugString = ACTION_ON_EDIT;
                if(actions.get(i).getAction().getEnabled() == ActionTable.Enabled.ENABLED)
                	debugString = debugString + COLON + RuleTable.StateForActionsAndConditions.ENABLED_STATE;
                else
                	debugString = debugString + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
            }
            else if (actions.get(i).getState().equals(EditType.DELETED)) {
            	this.deleteAction( context, actions.get(i));
            	debugString = ACTION_ON_DELETE;
            }
            
            if (debugString != null) {
            	this.setRuleModified(true);
            	PublisherManager pubMgr = PublisherManager.getPublisherManager(context, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.ACTION);
            	if (pubMgr.isValidPublisher(actions.get(i).getAction().getPublisherKey())) {
            		RulesBuilderUtils.writeInfoToDebugTable(context, DebugTable.Direction.OUT, SMARTRULES_INTERNAL_DBG_MSG,
            								mRuleInst.getName(),
            								mRuleInst.getKey(), actions.get(i).getAction().getDescription(),
            								Util.getLastSegmentPublisherKey(actions.get(i).getAction().getPublisherKey()), debugString);
            	}
            }
        }
	}
	
	/**This function updates the Condition table for a rule that has been edited.
	 *  There is a requirement to write debug info to the Debug table for every Condition 
	 *  that is edited (added/edited/deleted).
	 * 
	 * @param context
	 */
	private void updateConditionTable(Context context){
		ConditionInteractionModelList conditions = this.getConditionList();		
		for(int i=0; i < conditions.size(); i++) {
	    	String debugString = null;
	    	
	    	// Only for automatic rules, if the rule was in ready state or is a sample or suggestion
	    	// process to notify the condition publishers.
	    	if(mRuleInst.isAutomatic())
	    		if((!mRuleInst.isActive() && mRuleInst.isEnabled()) || mRuleInst.isSample() || mRuleInst.isSuggested())
	    			ConditionInteractionModel.notifyConditionPublisher(context, conditions.get(i), mRuleInst.getFlags());
	    	
	        if (conditions.get(i).getState().equals(EditType.ADDED)) {
	        	this.insertCondition(context, conditions.get(i));
	        	debugString = CONDITION_ON_CREATE;
	        }
	        else if (conditions.get(i).getState().equals(EditType.EDITED)) {
	        	this.editExistingCondition(context, conditions.get(i));
	        	debugString = CONDITION_ON_EDIT;
	        	if(conditions.get(i).getCondition().getEnabled() == ConditionTable.Enabled.ENABLED)
	        		debugString = debugString + COLON + RuleTable.StateForActionsAndConditions.ENABLED_STATE;
	        	else
	        		debugString = debugString + COLON + RuleTable.StateForActionsAndConditions.DISABLED_STATE;
	        }
	        else if (conditions.get(i).getState().equals(EditType.DELETED)) {
	        	this.deleteCondition(context, conditions.get(i));
	        	debugString = CONDITION_ON_DELETE;
	        }
	        if (debugString != null) {
	        	this.setRuleModified(true);      
	        	PublisherManager pubMgr = PublisherManager.getPublisherManager(context, com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.CONDITION);
				if (pubMgr.isValidPublisher(conditions.get(i).getCondition().getPublisherKey())) {
	        	RulesBuilderUtils.writeInfoToDebugTable(context, DebugTable.Direction.OUT, VSM_OUT_MESSAGE,
	        									mRuleInst.getName(),mRuleInst.getKey(),conditions.get(i).getCondition().getDescription(),
	        									Util.getLastSegmentPublisherKey(conditions.get(i).getCondition().getPublisherKey()), debugString);
				}
	        }
	    }
	 }
	
	/** This function inserts an Action tuple into Action table.
	 * 
	 * @param action
	 */
	private void insertAction(Context context, ActionInteractionModel action){
	    if (LOG_DEBUG) Log.d(TAG, "insertAction Action PubKey is " + action.getPubKey());
		action.getAction().setParentFk(mRuleInst.get_id());
		ActionTuple actionTuple = action.getAction();
		String ruleXml = action.getChildRuleXml();
        String childRuleKey = null;
        Rule childRule = action.getChildRule();
        
        if(childRule != null) {
        	// insert the child rule
        	childRule.insert(context);
        }
        
        if (ruleXml != null) {
            // There is a child rule to be imported
            if (LOG_INFO)
                Log.i(TAG, "Invoking Rules Importer for " + ruleXml);

            childRuleKey = ruleXml.substring(ruleXml.indexOf(KEY_TAG_S)
                    + KEY_TAG_S.length(), ruleXml.indexOf(KEY_TAG_E));

            if (LOG_DEBUG) Log.d(TAG, "childRuleKey is " + childRuleKey
                    + " parent rule Key is " + mRuleInst.getKey());

            Intent i = new Intent(LAUNCH_CANNED_RULES);
            i.putExtra(IMPORT_TYPE, 9);
            i.putExtra(XML_CONTENT, ruleXml);
            i.putExtra(EXTRA_REQUEST_ID, PUZZLE_BUILDER_RULE_ID);
            i.putExtra(RuleTable.Columns.PARENT_RULE_KEY, mRuleInst.getKey());
            context.sendBroadcast(i, SMART_RULES_PERMISSION);
        }
		//This code needs to be revisited for better design in next iteration
		//for updating the actions db, instead of deleting and adding
		if(actionTuple != null){
			actionTuple.setActive(ActionTable.Active.INACTIVE);
	        actionTuple.setConfWinner(ActionTable.ConflictWinner.LOSER);
	        if (childRuleKey != null)
	            actionTuple.setChildRuleKey(childRuleKey);
	        ActionPersistence.insertAction(context, actionTuple);
		}
	}
	
	/** This is called when an existing Action has been edited. So the action row is deleted and
	 * re-inserted with the latest values.
	 * 
	 * @param context
	 * @param action
	 */
	 private void editExistingAction(Context context, ActionInteractionModel action) {
		this.deleteAction(context,action);
		this.insertAction(context,action);
	 }
	 
	/** This deletes a single action from the Action table.
	 * 
	 * @param context
	 * @param action
	 */
	 private void deleteAction(Context context, ActionInteractionModel action) {
    	Long action_id = action.getAction().get_id();
    	String whereActionClause = ActionTable.Columns._ID + EQUALS + Q + action_id + Q;
    	ActionPersistence.deleteAction(context, whereActionClause );
	 }
	
	/** This is called when an existing Condition has been edited. So the condition row is deleted and
	 * re-inserted with the latest values.
	 * 
	 * @param event
	 * @param ruleId
	 * @param editType
	 */
    private void editExistingCondition( Context context, ConditionInteractionModel condition) {
    	this.deleteCondition(context,condition);
        this.insertCondition(context, condition);
    }
    
    /** This function inserts an Condition tuple into Condition table and the corresponding
     * Condition Sensor Tuple into the Condition Sensor Table.
     * 
     * @param context
     * @param condition
     */
    private void insertCondition(Context context, ConditionInteractionModel condition){
    	condition.getCondition().setParentFkey(mRuleInst.get_id());
    	ConditionTuple conditionTuple = condition.getCondition();
	ConditionPersistence.insertCondition(context, conditionTuple);
    }
    
    /** This deletes a single condition from the Condition table, and the corresponding Condition Sensor from the
     * Condition Sensor Table.
     * 
     * @param context
     * @param condition
     */
    private void deleteCondition(Context context, ConditionInteractionModel condition){
    	String whereConditionClause = ConditionTable.Columns._ID + EQUALS + Q + condition.getCondition().get_id() + Q ;
        ConditionPersistence.deleteCondition(context, whereConditionClause);
    }
   
    /** This gets the final set of values for a Rule that has been edited and saved. This values will be updated
	 * in the Rule Table for the existing rule entry.
	 * 
	 * @param context
	 * @param updateValues
	 */
	private void getUpdatedRuleTableValues(Context context, ContentValues updateValues){

        int ruleTableEnabled = DEFAULT_VALUE;
        String ruleFlags = null;
        int ruleTableRuleType = DEFAULT_VALUE;
        int ruleTableActive = DEFAULT_VALUE;
        int ruleTableSource = mRuleInst.getSource();

        ConditionInteractionModelList conditions = this.getConditionList();
        switch (ruleTableSource) {
        
	        case RuleTable.Source.FACTORY:
	        case RuleTable.Source.SUGGESTED:
	        {
	        	if (!this.getQuickSaveOfActiveRuleFlag()){
		        	String debugString = "";
		            if (conditions.isConnectedConditionPresent()) {
		                ruleTableRuleType = RuleTable.RuleType.AUTOMATIC;
		            }
		            else {
		            	ruleTableEnabled = RuleTable.Enabled.DISABLED;
		                ruleTableRuleType = RuleTable.RuleType.MANUAL;
		            }
		            if (ruleTableSource == RuleTable.Source.FACTORY) 
		            	debugString = SAMPLE_RULE_ACCEPTED_DBG_MSG;
		            else 
		            	debugString = SUGG_ACCEPTED_DBG_MSG;
		            
		            RulesBuilderUtils.writeInfoToDebugTable(context, DebugTable.Direction.OUT,
		            		                                SMARTRULES_INTERNAL_DBG_MSG, mRuleInst.getName(), mRuleInst.getKey(), 
		            		                                null, null,debugString);
		            
		            Intent dumpActionConditionServiceIntent = new Intent(context, DumpDbService.class);
		            dumpActionConditionServiceIntent.putExtra(DumpDbService.SERVICE_TYPE, DumpDbService.REGULAR_REQUEST);
		            dumpActionConditionServiceIntent.putExtra(RuleTable.Columns._ID, mRuleInst.get_id());
		            context.startService(dumpActionConditionServiceIntent);
		            
		            ruleFlags = EMPTY_STRING;
	        	}
	            break;
	        }
	
	        case RuleTable.Source.USER:
	        {
	        	if (!this.getQuickSaveOfActiveRuleFlag()){
		            if (conditions.isConnectedConditionPresent()) 
		            	ruleTableRuleType = RuleTable.RuleType.AUTOMATIC;
		            else {
		            	ruleTableRuleType = RuleTable.RuleType.MANUAL;
		                ruleTableEnabled = RuleTable.Enabled.DISABLED;
		            }
	        	}
	        	break;
	        }
        }
        
        if (ruleTableEnabled != DEFAULT_VALUE) updateValues.put(RuleTableColumns.ENABLED, ruleTableEnabled);
		if (ruleFlags != null) updateValues.put(RuleTableColumns.FLAGS, ruleFlags);
		if (ruleTableRuleType != DEFAULT_VALUE) updateValues.put(RuleTableColumns.RULE_TYPE, ruleTableRuleType);
		if (ruleTableActive != DEFAULT_VALUE) updateValues.put(RuleTableColumns.ACTIVE, ruleTableActive);
		if (this.isRuleModified()) updateValues.put(RuleTableColumns.LAST_EDITED_DATE_TIME, new Date().getTime());
		
        updateValues.put(RuleTableColumns.SOURCE, ruleTableSource);
        updateValues.put(RuleTableColumns.ICON, mRuleInst.getIcon());
        updateValues.put(RuleTableColumns.NAME, mRuleInst.getName());
	}
	
	/** Reads all the actions corresponding to a particular rule from the action list of
	 *  the passed in rule instance and, and stores it in the Action List for this rule.
	 * 
	 * @param context - context
	 */
	public void readFromActionList(Context context) {
		if (mRuleInst.getActionList() != null) {
			for (int i = 0; i < mRuleInst.getActionList().size(); i++) {
				mActionList.add(new ActionInteractionModel(mRuleInst
						.getActionList().get(i)));
			}
		}
	}

	/** Reads all the conditions corresponding to a particular rule from the condition list, 
	 *  including the corresponding entry from the Condition Sensor, and stores it in
	 *  the Condition List for this rule.
	 * 
	 * @param context - context
	 */
	public void readFromConditionList(Context context) {
		if (mRuleInst.getConditionList() != null) {
			for (int i = 0; i < mRuleInst.getConditionList().size(); i++) {
				mConditionList.add(new ConditionInteractionModel(mRuleInst
						.getConditionList().get(i)));
			}
		}
	}
    
	public Rule getRuleInstance() {
		return mRuleInst;
	}

	/**
	 * Sets the Rule instance in Model, if null creates one
	 * @param rule
	 */
	private void setRule(Rule rule) {
		if(rule!=null){
			mRuleInst = rule;
		}else{
			mRuleInst = new Rule(new RuleTuple(DEFAULT_RULE_ID,
        		RuleTable.Enabled.ENABLED,
                0,
                new Date().getTime()+"",
                RuleTable.Active.INACTIVE,
                RuleTable.RuleType.AUTOMATIC ,
                RuleTable.Source.USER,
                0,  //Rating
                NULL_STRING, //Community Author
                EMPTY_STRING, //Flags
                EMPTY_STRING, //Name
                EMPTY_STRING, //Description
                EMPTY_STRING, //Tags
                NULL_STRING,  //Conditions
                NULL_STRING,  //Infer logic
                NULL_STRING,  //Infer status
                RuleTable.SuggState.ACCEPTED,
                NULL_STRING,  //Suggested reason
                RuleTable.Lifecycle.NEVER_EXPIRES, //Lifecycle
                RuleTable.Silent.TELL_USER,
                EMPTY_STRING, //Vsensor
                NULL_STRING, //ruleSyntax
                0, // lastActiveDateTime
                0, //lastInactiveDateTime
                0, //created date and time
                0, //lastEditedDateTime
                DEFAULT_RULE_ICON,
                RuleTable.DEFAULT_SAMPLE_FKEY_OR_COUNT_VALUE,
                NULL_STRING, // Publisher Key
                TableBase.Validity.VALID, // Valid rule by default
                NULL_STRING, // Ui Intent
				NULL_STRING, // Parent Rule Key
				0));         // Adopt Count
		}
	}

	/**
	 * This method returns the Config + Publisher Key pairs which are to be unsubscribed.
	 * @return List of Config + Publisher Key pairs which are to be unsubscribed.
	 */
	 public ArrayList<String> getOldConfigPubKeyList(){
		 return mOldConfigPubKeyList;
	 }

	 /**
	  * This method adds the Config and publisher key passed in as parameters to
	  *  mOldConfigPubKeyList data structure.
	  * @param config Config to be added
	  * @param pubKey Publisher key to be added in array.
	  */
	 public void addToOldConfigPubKeyList(String config, String pubKey){
		 mOldConfigPubKeyList.add(config + "," + pubKey);
	 }
}
