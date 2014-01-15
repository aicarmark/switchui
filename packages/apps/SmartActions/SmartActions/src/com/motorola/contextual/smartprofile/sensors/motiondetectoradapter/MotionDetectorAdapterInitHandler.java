/*
 * @(#)MotionDetectorAdapterInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MotionDetectorAdapterInitHandler
 *
 */

package com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.CommandReceiver;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class handles "init" command from Smart Actions Core for Motion Detection Adapter
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements MotionConstants
 *
 * RESPONSIBILITIES:
 * This class initializes Motion Detection Adapter Condition Publisher and sends current status
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
public final class MotionDetectorAdapterInitHandler extends  CommandHandler implements MotionConstants {

    private final static String LOG_TAG = CommandReceiver.class.getSimpleName();
    private final static String STILL_ACTION = "com.motorola.intent.action.STILL";
    private final static String START = "start";

    @Override
    protected final String executeCommand(Context context, Intent intent) {

        if( LOG_INFO) Log.i(LOG_TAG, " executeCommand - init ");

        if((MotionDetectorAdapterDetailComposer.isMDMPresent(context)) ||
                (!MotionDetectorAdapterDetailComposer.isNewArchMD(context))) {
            List<String> valueList = Persistence.retrieveValuesAsList(context, MOTION_PERSISTENCE);
            if(!valueList.isEmpty()) {
                // First config for this publisher, so register
                registerReceiver(context, MOTION_STATE_MONITOR);
                long timeStamp = new Date().getTime();
                MotionDetectorAdapterDetailComposer.sendBroadcastToPublisher(context, MOTION_PUB_KEY, START, timeStamp);
            }

            IntentFilter filter = new IntentFilter();
            filter.addAction(STILL_ACTION);
            Intent regIntent = context.registerReceiver(null, filter);
            MotionDetectorAdapterReceiver.postNotify(context, (regIntent != null) ? true : false);

        }
        return SUCCESS;
    }
}
