/*
 * @(#)TimeFramesRefreshHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of TimeFramesRefreshHandler
 *
 */

package com.motorola.contextual.pickers.conditions.timeframe;

import android.content.Context;
import android.content.Intent;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameConstants;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameXmlSyntax;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFramesDetailComposer;



/**
 * This class handles "refresh" command from Smart Actions Core for TimeFrame
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements TimeFrameConstants, TimeFrameXmlSyntax
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

public final class TimeFramesRefreshHandler extends  CommandHandler implements TimeFrameConstants, TimeFrameXmlSyntax  {

    @SuppressWarnings("unused")
	private final static String LOG_TAG = TimeFramesRefreshHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        String status = FAILURE;
        if(intent.getStringExtra(EXTRA_CONFIG) != null) {
            constructAndPostResponse(context, intent, REFRESH_RESPONSE);
            status = SUCCESS;
        }
        return status;
    }

    /**
     * This method constructs response for "refresh" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     * @param event
     */
    public final void constructAndPostResponse(Context context, Intent intent, String event) {
        String config = intent.getStringExtra(EXTRA_CONFIG);

        Intent responseIntent = constructCommonResponse(intent);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, event);

        config = TimeFramesDetailComposer.getUpdatedConfig(context, config);
        if(config != null) {
            responseIntent.putExtra(EXTRA_CONFIG, config);
            responseIntent.putExtra(EXTRA_DESCRIPTION, TimeFramesDetailComposer.getDescriptionForConfig(context, config));
            responseIntent.putExtra(EXTRA_STATE, TimeFramesDetailComposer.getStateForConfig(context, config));
        } else {
        	responseIntent.putExtra(EXTRA_STATUS, FAILURE);
        }
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
    
    /**
     * This method constructs asynchronous "refresh" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intName - time frame internal name
     */
    public final void constructAndPostAsyncRefresh(Context context, String intName) {   	
    	Intent responseIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
    	responseIntent.putExtra(EXTRA_PUB_KEY, TIMEFRAME_PUBLISHER_KEY);
    	responseIntent.putExtra(EXTRA_EVENT_TYPE, ASYNC_REFRESH);
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
