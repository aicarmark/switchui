/*
 * @(#)TimeFramesSubscribeHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of TimeFramesSubscribeHandler
 *
 */

package com.motorola.contextual.pickers.conditions.timeframe;


import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameConstants;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameDBAdapter;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrames;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFramesDetailComposer;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeUtil;




/**
 * This class handles "subscribe" command from Smart Actions Core for TimeFrame
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements TimeFrameConstants
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

public final class TimeFramesSubscribeHandler extends  CommandHandler implements TimeFrameConstants  {


    private final static String LOG_TAG = TimeFramesSubscribeHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));
        String status = FAILURE;
        String config = intent.getStringExtra(EXTRA_CONFIG);
        if((config != null) && (TimeFramesDetailComposer.validateConfig(context, config))){
            saveMonitoredConfig(context, intent, TIMEFRAME_CONFIG_PERSISTENCE);
            registerTimeFramesFromConfig(context, intent.getStringExtra(EXTRA_CONFIG));
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
        responseIntent.putExtra(EXTRA_STATE, TimeFramesDetailComposer.getStateForConfig(context, config));

        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }

    /**
     * This method registers timeframes for a given config
     * @param context
     * @param config
     */
    void registerTimeFramesFromConfig(Context context, String config) {
        if (config != null) {
            ArrayList<String> internalNameList = TimeFramesDetailComposer.getIntNameFromConfig(context, config);
            TimeFrames timeframes = new TimeFrames().getData(context);
            if((internalNameList != null) && (timeframes != null)) {
                for(String internalName : internalNameList) {

                    TimeFrame timeFrame = timeframes
                                          .getTimeFrameByInternalName(internalName);
                    if ((timeFrame != null) && 
                       !(TimeUtil.isTimeFrameRegistered(context, timeframes.getFriendlyNameForTimeFrame(internalName)))) {
                        if (LOG_INFO) {
                            Log.i(LOG_TAG, "registering timeframe = "
                                  + internalName);
                        }
                        timeFrame.regsiterAllIntents();
                        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(
                            context);
                        dbAdapter
                        .setTimeFrameAsRegistered(internalName);
                        dbAdapter.close();
                    }                    
                }
            }
        }
    }
}
