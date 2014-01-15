/*
 * @(#)MissedCallCancelHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MissedCallCancelHandler
 *
 */

package com.motorola.contextual.pickers.conditions.missedcall;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.SmartProfileConfig;

/**
 * This class handles "cancel" command from Smart Actions Core for Missed Call
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements MissedCallConstants
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
public final class MissedCallCancelHandler extends  CommandHandler implements MissedCallConstants  {

    private final static String LOG_TAG = MissedCallCancelHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String config = intent.getStringExtra(EXTRA_CONFIG);
        String status = SUCCESS;
        if(config != null) {
            if (config.equals(ALL_CONFIGS)) {
                List<String> valueList =
                    Persistence.retrieveValuesAsList(context, MISSED_CALLS_CONFIG_PERSISTENCE);
                for (String configFromPersistence : valueList) {
                    intent.putExtra(EXTRA_CONFIG, configFromPersistence);
                    handleConfigCancel(context, intent, false);
                }
            } else {
                handleConfigCancel(context, intent, true);
            }
        } else {
            status = FAILURE;
        }

        return status;
    }

    /**
     * This method handles config cancel and responds to core if needed
     * @param context
     * @param intent - incoming intent
     * @param respond - to be responded or not
     * @return status
     */
    private String handleConfigCancel(Context context, Intent intent, boolean respond) {
        String status = SUCCESS;

        if(removeMonitoredConfig(context, intent, MISSED_CALLS_CONFIG_PERSISTENCE)) {
            if(LOG_DEBUG) Log.d(LOG_TAG, "unregister receiver");
            // First config for this publisher, so register
            unregisterReceiver(context, MISSED_CALLS_OBSERVER_STATE_MONITOR);

        }
        clearDBDataFromConfig(context, intent.getStringExtra(EXTRA_CONFIG));

        if (respond) {
            constructAndPostResponse(context, intent);
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

    /**
     * This method clears DB data using config
     * @param context
     * @param config
     */
    private final void clearDBDataFromConfig(Context context, String config) {

        if(!config.contains(MISSED_CALLS_CONFIG_STRING)) return;

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String value = profileConfig.getValue(MISSED_CALLS_NAME);
        if(value == null) return;

        config = value.replace(MISSED_CALLS_CONFIG_STRING, "");

        String numberConfig = config.substring((OPEN_B).length(), config.indexOf(CLOSE_B));
        ArrayList<String> numberList = MissedCallDetailComposer.getNumberListFromConfig(numberConfig);

        numberList = constructRemovableNumberList(context, numberList);
        MissedCallDBAdapter dbAdapter = new MissedCallDBAdapter(context);
        dbAdapter.deleteRows(numberList);
        dbAdapter.close();

    }

    /**
     * This method constructs list of removable numbers, if numbers are not used by other configs
     * @param context
     * @param numberList
     */
    public ArrayList<String> constructRemovableNumberList(Context context, ArrayList<String> numberList) {
        List<String> valueList = Persistence.retrieveValuesAsList(context, MISSED_CALLS_CONFIG_PERSISTENCE);

        for(int i = 0; i < numberList.size(); i++) {
            if(valueList.contains(numberList.get(i))) {
                numberList.remove(numberList.get(i));
            }
        }
        return numberList;
    }
}
