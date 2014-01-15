/*
 * @(#)TimeFramesCancelHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of TimeFramesCancelHandler
 *
 */

package com.motorola.contextual.pickers.conditions.timeframe;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameConstants;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameDBAdapter;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameXmlSyntax;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrames;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFramesDetailComposer;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeUtil;


/**
 * This class handles "cancel" command from Smart Actions Core for TimeFrame
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements TimeFrameConstants, TimeFrameXmlSyntax
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
public final class TimeFramesCancelHandler extends  CommandHandler implements TimeFrameConstants, TimeFrameXmlSyntax  {

    private final static String LOG_TAG = TimeFramesCancelHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        if(LOG_INFO) Log.i(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = SUCCESS;
        String config = intent.getStringExtra(EXTRA_CONFIG);

        if(config != null) {

            if (config.equals(ALL_CONFIGS)) {
                List<String> valueList = Persistence.retrieveValuesAsList(context, TIMEFRAME_CONFIG_PERSISTENCE);

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
     * This method handles cancel of a given config
     * @param context
     * @param intent
     * @param respond or not
     * @return status
     */
    private String handleConfigCancel(Context context, Intent intent, boolean respond) {
        String status = SUCCESS;

        removeMonitoredConfig(context, intent, TIMEFRAME_CONFIG_PERSISTENCE);
        unregisterTimeFramesFromConfig(context, intent.getStringExtra(EXTRA_CONFIG));

        if (respond) {
            constructAndPostResponse(context, intent);
        }

        return status;
    }

    /**
     * This method unregisters timeframes for a given config
     * @param context
     * @param config
     */
    void unregisterTimeFramesFromConfig(Context context, String config) {
        if(config != null) {
            ArrayList<String> internalNameList = TimeFramesDetailComposer.getIntNameFromConfig(context, config);
            TimeFrames timeframes = new TimeFrames().getData(context);
            if (LOG_INFO) {
                Log.i(LOG_TAG, "unregisterTimeFramesFromConfig = " + config);
            }
            if((internalNameList != null) && (timeframes != null)) {
                for(String internalName : internalNameList) {
                    TimeFrame timeFrame = timeframes.getTimeFrameByInternalName(internalName);
                    if ((timeFrame != null) &&
                            (TimeUtil.isTimeFrameRegistered(context, timeframes.getFriendlyNameForTimeFrame(internalName)))) {
                        if (LOG_INFO) {
                            Log.i(LOG_TAG,
                                  "WorkerThread.run REQ_TYPE_UNREGISTER_TIMEFRAME unregistering timeframe = "
                                  + internalName);
                        }
                        timeFrame.deRegsiterAllIntents();
                        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
                        dbAdapter
                        .setTimeFrameAsUnregistered(internalName);
                        dbAdapter.setTimeFrameAsInactive(internalName);
                        dbAdapter.close();
                    }
                }
            }
        }
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
