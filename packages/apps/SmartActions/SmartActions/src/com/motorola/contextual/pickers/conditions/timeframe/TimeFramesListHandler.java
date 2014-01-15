/*
 * @(#)TimeFramesListHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of TimeFramesListHandler
 *
 */

package com.motorola.contextual.pickers.conditions.timeframe;



import android.content.Context;
import android.content.Intent;
import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFramesDetailComposer;


/**
 * This class handles "list" command from Smart Actions Core for TimeFrame
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
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

public final class TimeFramesListHandler extends  CommandHandler  {

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        constructAndPostResponse(context, intent);
        return SUCCESS;
    }

    /**
     * This method constructs response for "list" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     */
    private final void constructAndPostResponse(Context context, Intent intent) {

    	Intent responseIntent = constructCommonResponse(intent);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_LIST_RESPONSE);
        responseIntent.putExtra(EXTRA_CONFIG_ITEMS, TimeFramesDetailComposer.getConfigItems(context));
        responseIntent.putExtra(EXTRA_NEW_STATE_TITLE, TimeFramesDetailComposer.getName(context));
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
