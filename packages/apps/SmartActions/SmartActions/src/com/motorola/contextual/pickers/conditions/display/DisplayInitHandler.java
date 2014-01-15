/*
 * @(#)DisplayInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of DisplayInitHandler
 *
 */

package com.motorola.contextual.pickers.conditions.display;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class handles "init" command from Smart Actions Core for Display
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements DisplayConstants
 *
 * RESPONSIBILITIES:
 * This class initializes Display Condition Publisher and sends current status
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
public final class DisplayInitHandler extends  CommandHandler implements DisplayConstants {

//    private final static String LOG_TAG = CommandReceiver.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {


        List<String> valueList = Persistence.retrieveValuesAsList(context, DISPLAY_PERSISTENCE);
        if(!valueList.isEmpty()) {
            // First config for this publisher, so register
            registerReceiver(context, DISPLAY_STATE_MONITOR);
        }
        DisplayReceiver.postNotify(context, true);

        return SUCCESS;
    }

}
