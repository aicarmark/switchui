/*
 * @(#)LoadRuleInUserInterface.java
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
package com.motorola.contextual.smartrules.uiabstraction;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;

import android.content.Context;
import android.util.Log;

/**
 * Helper class to be used by RuleController to facilitate Loading
 * of a Rule to be edited/re-configured
 * 
 * <code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 * Provides utilities and contains logic to transform a DB entity into
 * Rule and Publisher Models which can be easily manipulated in the MVC design
 *
 * COLABORATORS:
 * 	Context
 *  RuleInteractionModel
 *  Rule
 *  IEditRulePluginCallback
 *  
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class LoadRuleInUserInterface implements Constants{
	private static final String TAG = LoadRuleInUserInterface.class.getSimpleName();
	
	private Context						mContext = null;
	private RuleInteractionModel       	mRuleModel = null;
	private Rule						mRuleInst = null;	
	private IEditRulePluginCallback	mEditActivityCallback;
    
	LoadRuleInUserInterface(Context context, RuleInteractionModel ruleModel){
		mContext = context;
		mRuleModel = ruleModel;		

	}
	
	/**
	 * After instantiation this call will load the Rule in UI
	 * 
	 * @param callBack
	 * @param actList
	 * @param cdList
	 */
	public void initLoad(IEditRulePluginCallback callBack, ActionPublisherList actList, ConditionPublisherList cdList){
		mRuleInst = mRuleModel.getRuleInstance();
		mEditActivityCallback = callBack;
        // The Rule id has been passed in - could correspond to an Edit/Copy/Preset/Suggested rule.
        // In this case, read the relevant fields from Rule table to store in the Rule Container
         	
         	if ( mRuleInst !=null && mRuleModel != null){
         		
         		//TBD need to be removed later
         		if ((mRuleInst.getIcon() == null) || mRuleInst.getIcon().equals(NULL_STRING) )
         			mRuleInst.setIcon(DEFAULT_RULE_ICON);

			if(LOG_DEBUG) Log.d(TAG, "setIconDrawable is called " + mRuleInst.getRuleIconId() + COLON +
					mRuleInst.getPublisherKey() + COLON +  mRuleInst.getIcon());

			mRuleInst.setIconBlob(IconPersistence.getIconBlob(mContext, mRuleInst.getRuleIconId(),
					mRuleInst.getPublisherKey(), mRuleInst.getIcon()));
         		mRuleModel.readFromActionList(mContext);
	        	mRuleModel.readFromConditionList(mContext);

         	}
         	else {
         		Log.e(TAG, "Null mRuleInst or mRuleModel");
         	}
	/*else{
        	mRuleModel = new RuleInteractionModel();
               mRuleInst.setIconDrawable(IconPersistence.getDefaulfIcon(mContext));
        }
      */  
        mEditActivityCallback.loadGenericRuleViews();
    	mEditActivityCallback.loadPluginRuleViews(mRuleModel, actList, cdList);
	}
}