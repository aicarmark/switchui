/*
 * @(#)BatteryLevelInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/03/06  NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.battery;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;



/**
 * This class handles "init" command from Smart Actions Core for Battery Level
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *
 * RESPONSIBILITIES:
 * This class initializes Battery Level Condition Publisher and sends current status
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
public final class BatteryLevelInitHandler extends  CommandHandler implements BatteryLevelConstants {

//    private final static String LOG_TAG = BatteryLevelInitHandler.class.getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {


        List<String> valueList = Persistence.retrieveValuesAsList(context, BATTERY_LEVEL_PERSISTENCE);
        if(!valueList.isEmpty()) {
            // First config for this publisher, so register
            registerReceiver(context, BATTERY_LEVEL_STATE_MONITOR);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent chargingIntent = context.registerReceiver(null, filter);
        if(chargingIntent != null) {
            int level = chargingIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            BatteryLevelReceiver.postNotify(context, level);
        }
        return SUCCESS;
    }
}

