/*
 * @(#)BTDeviceSubscribeHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of BTDeviceSubscribeHandler
 *
 */

package com.motorola.contextual.pickers.conditions.bluetooth;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;




/**
 * This class handles "subscribe" command from Smart Actions Core for BT Device
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements BTConstants
 *
 * RESPONSIBILITIES:
 * This class saves subscribed config from Smart Actions Core
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public final class BTDeviceSubscribeHandler extends  CommandHandler implements BTConstants   {


    private final static String LOG_TAG = BTDeviceSubscribeHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));
        String status = FAILURE;
        String config = intent.getStringExtra(EXTRA_CONFIG);
        if((config != null) && (BTDeviceDetailComposer.validateConfig(context, config))) {
            saveMonitoredConfig(context, intent, BT_CONFIG_PERSISTENCE);
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
        String config = intent.getStringExtra(EXTRA_CONFIG);

        Intent responseIntent = constructCommonResponse(intent);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, SUBSCRIBE_RESPONSE);
        responseIntent.putExtra(EXTRA_STATE, BTDeviceDetailComposer.getStateForConfig(context, config));

        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }

}
