/*
 * @(#)MissedCallSubscribeHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MissedCallSubscribeHandler
 *
 */

package com.motorola.contextual.smartprofile.sensors.missedcallsensor;


import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.SmartProfileConfig;




/**
 * This class handles "subscribe" command from Smart Actions Core for Missed Call
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements MissedCallConstants
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

public final class MissedCallSubscribeHandler extends  CommandHandler implements MissedCallConstants   {


    private final static String LOG_TAG = MissedCallSubscribeHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = FAILURE;
        String config = intent.getStringExtra(EXTRA_CONFIG);
        if((config != null) && (MissedCallDetailComposer.validateConfig(context, config))) {
            if(saveMonitoredConfig(context, intent, MISSED_CALLS_CONFIG_PERSISTENCE)) {
                if(LOG_DEBUG) Log.d(LOG_TAG, "executeCommand first one " + config);
                // First config for this publisher, so register
                registerReceiver(context, MISSED_CALLS_OBSERVER_STATE_MONITOR);
            }

            updateDBDataFromConfig(context, config);
            constructAndPostResponse(context, intent);
            status = SUCCESS;
        }

        return status;
    }

    /**
     * This method constructs response for "subscribe" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     */
    private final void constructAndPostResponse(Context context, Intent intent) {
        String state = MissedCallNotifyHandler.getUserSeenInfo(context) ? FALSE : null;

        Intent responseIntent = constructCommonResponse(intent);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, SUBSCRIBE_RESPONSE);
        responseIntent.putExtra(EXTRA_STATE, state);

        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }

    /**
     * This method updates DB data from given config
     * @param context
     * @param config
     */
    private final void updateDBDataFromConfig(Context context, String config) {

        if(!config.contains(MISSED_CALLS_CONFIG_STRING)) return;

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String value = profileConfig.getValue(MISSED_CALLS_NAME);
        if(value == null) return;
        
        config = value.replace(MISSED_CALLS_CONFIG_STRING, "");
        String numberConfig = config.substring((OPEN_B).length(), config.indexOf(CLOSE_B));

        ArrayList<String> numberList = MissedCallDetailComposer.getNumberListFromConfig(numberConfig);
        ArrayList<String> nameList = MissedCallActivity.getNamesFromNumbers(context, numberList);

        MissedCallDBAdapter dbAdapter = new MissedCallDBAdapter(context);
        dbAdapter.insertRowToMissedCallTable(numberList, nameList);
        dbAdapter.close();

    }

}
