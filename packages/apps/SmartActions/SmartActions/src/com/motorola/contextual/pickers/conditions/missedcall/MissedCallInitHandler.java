/*
 * @(#)MissedCallInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/03/06  NA                Initial version of MissedCallInitHandler
 *
 */

package com.motorola.contextual.pickers.conditions.missedcall;

import java.util.HashMap;
import java.util.List;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;



/**
 * This class handles "init" command from Smart Actions Core for Missed Call
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *
 * RESPONSIBILITIES:
 * This class initializes Missed Call Condition Publisher and sends current status
 * of configs associated with rules
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class MissedCallInitHandler extends  CommandHandler  implements MissedCallConstants   {

    private final static String LOG_TAG = MissedCallInitHandler.class.getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {

        if(!MissedCallNotifyHandler.getUserSeenInfoAndNotify(context)) {
            notifyAllConfigs(context);
        }
        return SUCCESS;
    }

    /**
     * Method to notify current state for all configs
     *
     * @param context
     * @return none
     */
    private void notifyAllConfigs(Context context) {
        HashMap<String, String> configStateMap = new HashMap<String, String>();
        List<String> valueList = Persistence.retrieveValuesAsList(context, MISSED_CALLS_CONFIG_PERSISTENCE);

        int size = valueList.size();
        for(int i=0; i<size; i++) {
            String value = valueList.get(i);
            configStateMap.put(value, MissedCallDetailComposer.getStateForConfig(context, value));
        }
        if(!valueList.isEmpty()) {
            // First config for this publisher, so register
            registerReceiver(context, MISSED_CALLS_OBSERVER_STATE_MONITOR);
            Intent newIntent = CommandHandler.constructNotification(configStateMap, MISSED_CALLS_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + valueList + " : " + configStateMap);
        }
    }
}
