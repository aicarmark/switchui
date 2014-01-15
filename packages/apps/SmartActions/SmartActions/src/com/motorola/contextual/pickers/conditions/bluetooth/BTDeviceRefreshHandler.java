/*
 * @(#)BTDeviceRefreshHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of BTDeviceRefreshHandler
 *
 */

package com.motorola.contextual.pickers.conditions.bluetooth;

import android.content.Context;
import android.content.Intent;


import com.motorola.contextual.smartprofile.CommandHandler;



/**
 * This class handles "refresh" command from Smart Actions Core for BT Device
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements BTConstants
 *
 * RESPONSIBILITIES:
 * This class updates the config parameters and sends reponse to Smart
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

public final class BTDeviceRefreshHandler extends  CommandHandler implements BTConstants {

    @SuppressWarnings("unused")
    private final static String LOG_TAG = BTDeviceRefreshHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        String status = FAILURE;
        if(intent.getStringExtra(EXTRA_CONFIG) != null) {
            constructAndPostResponse(context, intent);
            status = SUCCESS;
        }
        return status;
    }

    /**
     * This method constructs response for "refresh" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     */
    private final void constructAndPostResponse(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);

        Intent responseIntent = constructCommonResponse(intent);

        responseIntent.putExtra(EXTRA_EVENT_TYPE, REFRESH_RESPONSE);

        config = BTDeviceDetailComposer.getUpdatedConfig(context, config);
        responseIntent.putExtra(EXTRA_CONFIG, config);
        if(config != null) {
            responseIntent.putExtra(EXTRA_DESCRIPTION, BTDeviceDetailComposer.getDescriptionForConfig(context, config));
            responseIntent.putExtra(EXTRA_STATE, BTDeviceDetailComposer.getStateForConfig(context, config));
        }
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }


}
