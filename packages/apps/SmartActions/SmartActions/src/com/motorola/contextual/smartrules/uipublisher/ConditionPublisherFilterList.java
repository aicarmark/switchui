/*
 * @(#)ConditionPublisherFilterList.java
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
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.RuleInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel.ActionInteractionModelList;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel.ConditionInteractionModelList;
import com.motorola.contextual.smartrules.util.ConflictingActionTriggerFilterList;

import android.content.Context;

/**
 * Rule specific
 * This dynamic list would have all the conditions that could be suitable candidates
 * to be added to an existing set of Conditions
 *
 * Grey list to be implemented here , static(conflicting from xml) as well as
 *  dynamic ( avoiding statefull duplicates)
 *
 */

public class ConditionPublisherFilterList extends PublisherList implements Constants{

	private static final long serialVersionUID = 8170801096509362333L;
	private Context mContext;
	private ActionInteractionModelList mActionList; //rule-specific
	private ConditionInteractionModelList mConditionList; //rule-specific
	private ConditionPublisherList conditionPubList;
	ConflictingActionTriggerFilterList instConflictingActionTriggerFilterlist;

	/**
	 * This initializes and return
	 * @param context
	 * @param ruleInst
	 * @param condPubList
	 */
	public ConditionPublisherFilterList(Context context, RuleInteractionModel ruleModel,
												ConditionPublisherList condPubList){
		mContext = context;
		if(ruleModel!=null){
			mActionList = ruleModel.getActionList();
			mConditionList = ruleModel.getConditionList();
		}
		conditionPubList =  condPubList;
		instConflictingActionTriggerFilterlist = ConflictingActionTriggerFilterList.getConflictingActionTriggerFilterListInst();
	}

	/**
	 * Get list of elements to grey out by virtue of conflict determined from xml
     * xml's should be present at following loc
     * /system/etc/smartactions/com.motorola.smartactions_triggerfilterlist.xml
     * /system/etc/smartactions/com.motorola.smartactions_actionfilterlist.xml
	 */
	public PublisherList getGreyListOfConditions(){
		if(mConditionList!=null && conditionPubList!=null){
			for(int i=0;i<mConditionList.size();i++){
				if( conditionPubList.containsKey(mConditionList.get(i).getPubKey())){
					//for this action part of the rule, we find matching publisher from list of action publishers
					Publisher matchingPublisher = conditionPubList.get(mConditionList.get(i).getPubKey());
					//check for if it qualifies for greying
					//first check for dynamic greying , if already qualified no need to check for static/xml conflict check
					if( dynamicGreying(mConditionList.get(i), matchingPublisher) != true){
						//Log msg
					}
				}
			}
			if(mActionList!=null) staticXmlGreying();
		}
		return this;
	}

	/**
	 * Add elements to grey out depending on existing STATEFULL action
	 *
	 */
	private boolean dynamicGreying(ConditionInteractionModel condition, Publisher matchingPublisher){
		boolean ret = false;
		if(matchingPublisher !=null){
			//if in action Model list this has already been deleted, we can add it back, we dont need to grey it
			//also only for StateFul publisher duplicates are not to be allowed, and hence greyed
			if( !condition.getState().equals(EditType.DELETED)	&&
					matchingPublisher.getStateType().equals(STATEFUL)){
				this.put(condition.getPubKey(), matchingPublisher);
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Add elements to grey out by virtue of conflict determined from xml
	 */
	private void staticXmlGreying(){

		Vector<String> actionsToLookForGrayedTriggers =
			instConflictingActionTriggerFilterlist.getListOfActionsToCheckForCorrespondingTriggerFilters(mContext);
		for(int k=0; k<actionsToLookForGrayedTriggers.size();k++){
			for(int m=0;m<mActionList.size();m++){
				if( !mActionList.get(m).getState().equals(EditType.DELETED) &&
						mActionList.get(m).getPubKey().equalsIgnoreCase(actionsToLookForGrayedTriggers.elementAt(k))){
					Vector<String> is_blocked = instConflictingActionTriggerFilterlist.isTriggerToBeFiltered(mContext,
																 actionsToLookForGrayedTriggers.elementAt(k) );
					if(is_blocked!=null && !is_blocked.isEmpty() ){
						for(int n=0;n<is_blocked.size();n++){
							if(conditionPubList.containsKey(is_blocked.elementAt(n))){
								this.put(is_blocked.elementAt(n), conditionPubList.get(is_blocked.elementAt(n)));
							}
						}
					}
					break;//we already found the matching one and dealt with all actions to be grayed
				}
			}
		}
	}


}
