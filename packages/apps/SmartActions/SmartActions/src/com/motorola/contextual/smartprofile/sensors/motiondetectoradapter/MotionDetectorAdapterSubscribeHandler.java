/*
 * @(#)MotionDetectorAdapterSubscribeHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MotionDetectorAdapterSubscribeHandler
 *
 */

package com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

import 	java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.CommandReceiver;
import com.motorola.contextual.smartprofile.Constants;

/**
 * This class handles "subscribe" command from Smart Actions Core for Motion Detection Adapter
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements MotionConstants, Constants
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

public final class MotionDetectorAdapterSubscribeHandler extends  CommandHandler implements MotionConstants, Constants  {


    private final static String LOG_TAG = CommandReceiver.class.getSimpleName();
    private final static String START = "start";

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = FAILURE;
        if(intent.getStringExtra(EXTRA_CONFIG) != null) {
            if(!MotionDetectorAdapterDetailComposer.isNewArchMD(context)) {
                constructAndPostResponse(context, intent);
            }

            if((MotionDetectorAdapterDetailComposer.isMDMPresent(context)) ||
                    (!MotionDetectorAdapterDetailComposer.isNewArchMD(context))) {
                if(saveMonitoredConfig(context, intent, MOTION_PERSISTENCE)) {
                    // First config for this publisher, so register
                    registerReceiver(context, MOTION_STATE_MONITOR);
                }


                long timeStamp = new Date().getTime();
                MotionDetectorAdapterDetailComposer.sendBroadcastToPublisher(context, MOTION_PUB_KEY, START, timeStamp);
            }
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

        responseIntent.putExtra(EXTRA_STATE, MotionDetectorAdapterDetailComposer.getCurrentState(context, config));
        responseIntent.putExtra(EXTRA_EVENT_TYPE, SUBSCRIBE_RESPONSE);
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }


}
