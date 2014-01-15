/*
 * @(#)ActionPublisherFilterList.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2012/04/03  NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.uipublisher;

import java.util.Vector;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.RuleInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel.ActionInteractionModelList;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel.ConditionInteractionModelList;
import com.motorola.contextual.smartrules.util.ConflictingActionTriggerFilterList;

import android.content.Context;

/**
 * Rule specific
 * This dynamic list would have all the actions that could be suitable candidates
 * to be added to an existing set of Actions
 *
 * Grey list to be implemented here , static(conflicting from xml) as well as
 *  dynamic ( avoiding statefull duplicates)
 */

public class ActionPublisherFilterList extends PublisherList implements Constants{

	private static final long serialVersionUID = 9180206917939075395L;
	private Context mContext;
	private RuleInteractionModel mRuleModel = null;
	private ActionInteractionModelList mActionList;
	private ConditionInteractionModelList mConditionList;
	private ActionPublisherList actionPubList;
	ConflictingActionTriggerFilterList instConflictingActionTriggerFilterlist;

	public ActionPublisherFilterList(Context context, RuleInteractionModel ruleModel, ActionPublisherList actPubList){
		mContext = context;
		mRuleModel = ruleModel;
		if(mRuleModel!=null){
			mActionList = mRuleModel.getActionList();
			mConditionList = mRuleModel.getConditionList();
		}
		actionPubList =  actPubList;
		instConflictingActionTriggerFilterlist = ConflictingActionTriggerFilterList.getConflictingActionTriggerFilterListInst();
	}

	/**
	 * Get list of elements to grey out by virtue of conflict determined from xml
     * xml's should be present at following loc
     * /system/etc/smartactions/com.motorola.smartactions_triggerfilterlist.xml
     * /system/etc/smartactions/com.motorola.smartactions_actionfilterlist.xml
	 */
	public PublisherList getGreyListOfActions(){
		//need to refresh action/condition list to account for any addition/deletion
		if(mRuleModel!=null){
			mActionList = mRuleModel.getActionList();
			mConditionList = mRuleModel.getConditionList();
		}
		if(mActionList!=null && actionPubList!=null){
			for(int i=0;i<mActionList.size();i++){
				if( actionPubList.containsKey(mActionList.get(i).getPubKey())){
					//for this action part of the rule, we find matching publisher from list of action publishers
					Publisher matchingPublisher = actionPubList.get(mActionList.get(i).getPubKey());
					//check for if it qualifies for greying
					//first check for dynamic greying , if already qualified no need to check for static/xml conflict check
					if( dynamicGreying(mActionList.get(i), matchingPublisher) != true){
						//Log message
					}
				}
			}
			if(mConditionList!=null) staticXmlGreying(); // conflict resolved from xml
		}
		return this;
	}

	/**
	 * Add elements to grey out depending on existing STATEFULL action
	 *
	 */
	private boolean dynamicGreying(ActionInteractionModel action, Publisher matchingPublisher){
		boolean ret = false;
		if(matchingPublisher !=null){
			//if in action Model list this has already been deleted, we can add it back, we dont need to grey it
			//also only for StateFul publisher duplicates are not to be allowed, and hence greyed
			if( !action.getState().equals(EditType.DELETED)	&&
					matchingPublisher.getStateType().equals(STATEFUL)){
				this.put(action.getPubKey(), matchingPublisher);
				ret = true;
			}
		}
		return ret;
	}

	private void staticXmlGreying(){

		Vector<String> triggersToLookForGrayedActions =
			instConflictingActionTriggerFilterlist.getListOfTriggersToCheckForCorrespondingActionsFilters(mContext);
		for(int k=0; k<triggersToLookForGrayedActions.size();k++){
			for(int m=0;m<mConditionList.size();m++){ //compare the triggers which can conflict to set of triggers in the rule
				
				if( !mConditionList.get(m).getState().equals(EditType.DELETED) &&
						mConditionList.get(m).getPubKey().equalsIgnoreCase(triggersToLookForGrayedActions.elementAt(k))){
					Vector<String> is_blocked = instConflictingActionTriggerFilterlist.isActionToBeFiltered(mContext,
																		triggersToLookForGrayedActions.elementAt(k) );
					if(is_blocked!=null && !is_blocked.isEmpty() ){
						for(int n=0;n<is_blocked.size();n++){
							if(actionPubList.containsKey(is_blocked.elementAt(n))){
								this.put(is_blocked.elementAt(n), actionPubList.get(is_blocked.elementAt(n)));
							}
						}
					}
					break;//we already found the matching one and dealt with all actions to be grayed
				}
			}
		}
	}

}
