/*
 * @(#)SubscribeHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of SubscribeHandler
 *
 */

package com.motorola.contextual.smartprofile;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.util.Util;

/**
 * This class handles "subscribe" command from Smart Actions Core for
 * Condition Publishers
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *
 * RESPONSIBILITIES:
 * This class registers receivers and sends reponse to Smart
 * Actions Core
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public final class SubscribeHandler extends  CommandHandler implements  Constants  {


    private final static String LOG_TAG = SubscribeHandler.class.getSimpleName();
    private final static String PERSISTENCE = ".persistence";
    private final static String STATE_MONITOR = "StateMonitor";

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = FAILURE;

        if(intent.getStringExtra(EXTRA_CONFIG) != null) {
            String actName = Util.getPublisherNameFromPublisherKey(context, intent.getAction());
            
            if(actName != null) {
                AbstractDetailComposer ruleConstr = null;

                if (actName != null) {
                    ruleConstr = instantiateDetailComposer(actName);
                }

                if(ruleConstr != null && ruleConstr.validateConfig(context, intent.getStringExtra(EXTRA_CONFIG))) {
	            	constructAndPostResponse(context, intent, ruleConstr);
	
	                if(saveMonitoredConfig(context, intent, intent.getAction()+PERSISTENCE)) {
	
	                    String conditionKey = actName.substring((actName.lastIndexOf(":")) + 1);
	                    actName = actName.substring(0, (actName.lastIndexOf(":")));
	                    if(LOG_DEBUG) Log.d(LOG_TAG, "Act name - " + actName + " : " + conditionKey);
	
	                    // First config for this publisher, so register
	                    registerReceiver(context, actName.substring(0, (actName.lastIndexOf(".")+1)) + conditionKey +STATE_MONITOR );                    
	                }
	                status = SUCCESS;
                }
            }            
        }
        return status;
    }

    /**
     * This method constructs response for "subscribe" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     * @param ruleConstr
     */
    private void constructAndPostResponse(Context context, Intent intent, AbstractDetailComposer ruleConstr) {
        String config = intent.getStringExtra(EXTRA_CONFIG);

        Intent responseIntent = constructCommonResponse(intent);

        responseIntent.putExtra(EXTRA_STATE, (ruleConstr != null) ? ruleConstr.getCurrentState(context, config) : FALSE);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, SUBSCRIBE_RESPONSE);
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
