/*
 * @(#)QuickActionsService.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/15 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;

/** This is a IntentService class for processing the intents from Quick Actions.
*
*<code><pre>
* CLASS:
* 	extends IntentService
*
*  implements
*  	Constants - for the constants used
*   DbSyntax - for the DB related constants
*
* RESPONSIBILITIES:
* 	Processes the intents from Quick Actions for default values and user override of actions
*   	- stores the default value for the action into the default rule record
*   	- store a failure message if the action was not successful
*   	- marks actions as inactive when the user manually overrides an action value from settings
*
* COLABORATORS:
* 	None.
*
* USAGE:
* 	See each method.
*</pre></code>
*/

public class QuickActionsService extends IntentService implements Constants {

	private static final String TAG = QuickActionsService.class.getSimpleName();
	
	private Context mContext = null;
	
	/** default constructor
	 * 
	 */
	public QuickActionsService() {
		super(TAG);
	}
	
	/** Constructor
	 * 
	 * @param name
	 */
	public QuickActionsService(String name) {
		super(name);
	}

	/** onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
	}

	/** onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	/** onHandleIntent()
	 * 
	 * @param intent - Intent that invokes this Service
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

    	Bundle bundle = intent.getExtras();
        if(bundle != null) {
            String actionPublisherKey = bundle.getString(EXTRA_PUBLISHER_KEY);
            String intentAction = bundle.getString(INTENT_ACTION);

            if(intentAction != null) {
                if(intentAction.equals(ACTION_PUBLISHER_EVENT)) {
                    // For now, both FIRE_RESPONSE and REVERT RESPONSE are treated in same way.
                    String eventType = bundle.getString(EXTRA_EVENT_TYPE);
                    if(eventType != null){
	                    if (!eventType.equals(REFRESH_RESPONSE)) {
	                        if(LOG_INFO) Log.i(TAG, "In onHandleIntent to handle "+intent.toUri(0));
	                    }
	                    if(eventType.equals(FIRE_RESPONSE) ||
	                            eventType.equals(REVERT_RESPONSE)) {
	                    	handleFireOrRevertResponse(bundle, eventType, actionPublisherKey);                    
	                    } else if(eventType.equals(STATE_CHANGE)) {
	                    	handleStateChangeEvent(bundle, actionPublisherKey);
	                    }else{ 
	                    	if(LOG_DEBUG) Log.d(TAG, " Not handling " + eventType);
	                    }
                    }

                }
            }
        }
    }
	
	/**
	 * Handles FIRE_RESPONSE or REVERT_RESPONSE events from Action Publishers
	 * @param bundle - Extras bundle
	 * @param eventType - Type of event
	 * @param actionPublisherKey - Publisher key of the action publisher which sent this event
	 */
	private void handleFireOrRevertResponse(Bundle bundle, String eventType, String actionPublisherKey){
		String status = bundle.getString(EXTRA_STATUS);

        if(actionPublisherKey != null && status != null) {
            if(LOG_DEBUG) Log.d(TAG, "Status from QA is " + status);
		String responseId = bundle.getString(EXTRA_RESPONSE_ID);
            String ruleKey = responseId.substring(0, (responseId.lastIndexOf(COLON)));
            String actionIdStr = responseId.substring((responseId.lastIndexOf(COLON) + 1));
            long actionId = Long.parseLong(actionIdStr);
            if(status.equals(SUCCESS) &&
                    eventType.equals(FIRE_RESPONSE) &&
                    !DEFAULT_RULE_KEY.equals(ruleKey)) {

                if(LOG_DEBUG) Log.d(TAG, "Status from QA is success : " + ruleKey + ": " + actionId);
                ActionPersistence.updateDefaultRecordForActionKey(mContext, actionPublisherKey);
            } else if(status.equals(FAILURE)) {
                if(LOG_DEBUG) Log.d(TAG, "status from QA is failure: " + ruleKey + ": " + actionId);
                String failureString = bundle.getString(EXTRA_FAILURE_DESCRIPTION);
                if(ruleKey != null) {
                    if(failureString != null)
                        failureString = status + ":" + failureString;
                    else
                        failureString = status;
                    if(LOG_DEBUG) Log.d(TAG, "process the failure message "+failureString+" for ruleKey "+ruleKey);
                    long ruleId = RulePersistence.getRuleIdForRuleKey(mContext, ruleKey);
                    if(ruleId != DEFAULT_RULE_ID)
                        ActionPersistence.updateActionFailureStatus(mContext, ruleId, failureString, actionId);
                } else
                    Log.e(TAG, "ruleKey is null");
            } else if(status.equals(IN_PROGRESS) && eventType.equals(FIRE_RESPONSE)){
                ActionPersistence.updateDefaultRecordForActionKey(mContext, actionPublisherKey);
                return;
            } else if (status.equals(IN_PROGRESS)) {
                // Dont do anything for revert_response-in_progress
		if(LOG_DEBUG) Log.d(TAG, " Revert response in progress received, do nothing");
                return;
            }

            //Send the broadcast so this action is removed from the Smartrules Service Queue
            Intent processedIntent = new Intent(QA_EXEC_STATUS_PROCESSED);
            processedIntent.putExtras(bundle);
            mContext.sendBroadcast(processedIntent);
        } else
            Log.e(TAG, "Input Params are null: actionPublisherKey is "+(actionPublisherKey==null?"null":actionPublisherKey)
                  +" or status is "+(status==null?"null":status));
	}
	
	
	/**
	 * Handles STATE_CHANGE event from Action Publishers
	 * @param bundle - Extras bundle
	 * @param actionPublisherKey - Publisher key of the action publisher which sent this event
	 */
	private void handleStateChangeEvent(Bundle bundle, String actionPublisherKey){
        String newSettingValue = bundle.getString(EXTRA_STATE_DESCRIPTION);
        if(actionPublisherKey != null && newSettingValue != null) {
            if(LOG_DEBUG) Log.d(TAG, "User changed setting for action "+ actionPublisherKey);
            ActionPersistence.markActionsInactiveForActionKey(mContext, actionPublisherKey, newSettingValue);
        } else
            Log.e(TAG, "Input Params are null: actionPublisherKey is "+(actionPublisherKey==null?"null":actionPublisherKey)
                  +" or settingValue is "+(newSettingValue==null?"null":newSettingValue));
	}
}