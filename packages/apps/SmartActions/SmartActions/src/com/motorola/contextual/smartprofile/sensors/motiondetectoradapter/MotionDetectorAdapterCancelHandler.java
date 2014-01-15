/*
 * @(#)MotionDetectorAdapterCancelHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MotionDetectorAdapterCancelHandler
 *
 */

package com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;


/**
 * This class handles "cancel" command from Smart Actions Core for Motion Detection
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

public final class MotionDetectorAdapterCancelHandler extends  CommandHandler implements MotionConstants {


    private final static String LOG_TAG = MotionDetectorAdapterCancelHandler.class.getSimpleName();
    private final static String STOP = "stop";

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = FAILURE;
        String config = intent.getStringExtra(EXTRA_CONFIG);

        if (config != null) {

            if (config.equals(ALL_CONFIGS)) {
                List<String> valueList = Persistence.retrieveValuesAsList(context, MOTION_PERSISTENCE);

                for (String configFromPersistence : valueList) {
                    intent.putExtra(EXTRA_CONFIG, configFromPersistence);
                    handleConfigCancel(context, intent, false);
                }
            } else {
                handleConfigCancel(context, intent, true);
            }

            status = SUCCESS;
        }
        return status;
    }

    /**
     * This method handles "cancel" command and sends response
     * to Smart Actions Core if needed
     * @param context
     * @param intent - incoming intent
     * @param respond - need to respond or not
     * @return status
     */
    private String handleConfigCancel(Context context, Intent intent, boolean respond) {
        String status = SUCCESS;

        if (respond && (!MotionDetectorAdapterDetailComposer.isNewArchMD(context))) {
            constructAndPostResponse(context, intent);
        }

        if(MotionDetectorAdapterDetailComposer.isMDMPresent(context) ||
                (!MotionDetectorAdapterDetailComposer.isNewArchMD(context))) {
            if(removeMonitoredConfig(context, intent, MOTION_PERSISTENCE)) {
                // No more configs depend on the receiver, so unregister
                unregisterReceiver(context, MOTION_STATE_MONITOR);
                
                long timeStamp = new Date().getTime();
                MotionDetectorAdapterDetailComposer.sendBroadcastToPublisher(context, MOTION_PUB_KEY, STOP, timeStamp);
            }
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
