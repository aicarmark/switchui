/*
 * @(#)BTDeviceCancelHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of BTDeviceCancelHandler
 *
 */

package com.motorola.contextual.pickers.conditions.bluetooth;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class handles "cancel" command from Smart Actions Core for Battery Level
 * Condition Publisher
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
public final class BTDeviceCancelHandler extends  CommandHandler implements BTConstants  {

    private final static String LOG_TAG = BTDeviceCancelHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = SUCCESS;
        String config = intent.getStringExtra(EXTRA_CONFIG);
        if(config != null) {

            if (config.equals(ALL_CONFIGS)) {
                List<String> configList = Persistence.retrieveValuesAsList(context, BT_CONFIG_PERSISTENCE);

                for (String configFromPersistence : configList) {
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
     * This method handles cancel request
     * @param context
     * @param intent - incoming intent
     * @param whether to respond or not
     * @return status
     */
    private String handleConfigCancel(Context context, Intent intent, boolean respond) {
        String status = SUCCESS;
        removeMonitoredConfig(context, intent, BT_CONFIG_PERSISTENCE);
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
}
