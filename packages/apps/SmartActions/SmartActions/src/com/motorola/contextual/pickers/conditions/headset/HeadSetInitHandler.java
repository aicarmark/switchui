/*
 * @(#)HeadSetInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of HeadSetInitHandler
 *
 */

package com.motorola.contextual.pickers.conditions.headset;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class handles "init" command from Smart Actions Core for Headset
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements HeadSetConstants
 *
 * RESPONSIBILITIES:
 * This class initializes Headset Condition Publisher and sends current status
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
public final class HeadSetInitHandler extends  CommandHandler implements HeadSetConstants {

    private final static String LOG_TAG = HeadSetInitHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(final Context context, final Intent intent) {


        final List<String> valueList = Persistence.retrieveValuesAsList(context, HEADSET_PERSISTENCE);
        if(!valueList.isEmpty()) {
            // First config for this publisher, so register
            registerReceiver(context, HEADSET_STATE_MONITOR);
        }


        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        final Intent headsetIntent = context.registerReceiver(null, filter);
        final int state = (headsetIntent != null) ? headsetIntent.getIntExtra("state", 0) : 0;
        HeadSetReceiver.postNotify(context, state);

        return SUCCESS;
    }

}
