/*
 * @(#)CancelHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version
 *
 */

package com.motorola.contextual.smartprofile;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.util.Util;


/**
 * This class handles "cancel" command from Smart Actions Core for
 * Condition Publishers. This is a common cancel handler.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *
 * RESPONSIBILITIES:
 * This class unregisters receivers/observers related to a config
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public final class CancelHandler extends  CommandHandler implements Constants {

    private final static String LOG_TAG = CancelHandler.class.getSimpleName();
    private final static String SEMICOLON = ":";
    private final static String PERIOD = ".";
    private final static String PERSISTENCE = ".persistence";
    private final static String STATE_MONITOR = "StateMonitor";

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = SUCCESS;
        String config = intent.getStringExtra(EXTRA_CONFIG);

        if(config != null) {
            String actName = Util.getPublisherNameFromPublisherKey(context, intent.getAction());
            if(actName == null) {
                if (!config.equals(ALL_CONFIGS)) {
                    constructAndPostResponse(context, intent);
                }
                return status;
            }
            String conditionKey = actName.substring((actName.lastIndexOf(SEMICOLON)) + 1);
            actName = actName.substring(0, (actName.lastIndexOf(SEMICOLON)));
            if(LOG_DEBUG) Log.d(LOG_TAG, "Act name - " + actName + " : " + conditionKey);

            if (config.equals(ALL_CONFIGS)) {
                List<String> valueList = Persistence.retrieveValuesAsList(context, intent.getAction()+PERSISTENCE);
                for (String configFromPersistence : valueList) {
                    intent.putExtra(EXTRA_CONFIG, configFromPersistence);
                    if (removeMonitoredConfig(context, intent, intent.getAction() + PERSISTENCE)) {
                        unregisterReceiver(context, actName.substring(0, (actName.lastIndexOf(PERIOD)+1)) + conditionKey + STATE_MONITOR);
                    }
                }
            }  else {
                constructAndPostResponse(context, intent);
                if(removeMonitoredConfig(context, intent, intent.getAction()+PERSISTENCE)) {
                    // No more configs depend on the receiver, so unregister
                    unregisterReceiver(context, actName.substring(0, (actName.lastIndexOf(PERIOD)+1)) + conditionKey + STATE_MONITOR);
                }
            }
        } else {
        	return FAILURE;
        }

        return status;
    }

    /**
     * This method constructs response for "cancel" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     */
    private final void constructAndPostResponse(Context context, Intent intent) {
        Intent responseIntent = constructCommonResponse(intent);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, CANCEL_RESPONSE);
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
