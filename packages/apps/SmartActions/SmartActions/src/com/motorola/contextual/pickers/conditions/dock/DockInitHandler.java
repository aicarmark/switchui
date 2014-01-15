/*
 * @(#)DockInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.dock;


import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class handles "init" command from Smart Actions Core for Dock
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements DockConstants
 *
 * RESPONSIBILITIES:
 * This class initializes Dock Condition Publisher and sends current status
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
public final class DockInitHandler extends  CommandHandler implements DockConstants {

//    private final static String LOG_TAG = CommandReceiver.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {


        List<String> valueList = Persistence.retrieveValuesAsList(context, DOCK_PERSISTENCE);
        if(!valueList.isEmpty()) {
            // First config for this publisher, so register
            registerReceiver(context, DOCK_STATE_MONITOR);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        Intent androidIntent = context.registerReceiver(null, filter);
        int androidDockState = (androidIntent != null) ? androidIntent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0) : 0;

        filter = new IntentFilter();
        filter.addAction(EXTRA_MOT_DOCK_STATE);
        Intent motIntent = context.registerReceiver(null, filter);
        int motDockState = (motIntent != null) ? motIntent.getIntExtra(EXTRA_MOT_DOCK_STATE, 0) : 0;

        DockReceiver.postNotifyForAndroidDocks(context, androidDockState);
        DockReceiver.postNotifyForMotoDocks(context, motDockState);
        return SUCCESS;
    }
}
