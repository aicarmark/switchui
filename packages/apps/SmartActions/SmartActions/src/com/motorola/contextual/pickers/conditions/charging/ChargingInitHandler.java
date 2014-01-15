/*
 * @(#)ChargingInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of ChargingInitHandler
 *
 */

package com.motorola.contextual.pickers.conditions.charging;



import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class handles "init" command from Smart Actions Core for Charging
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *
 * RESPONSIBILITIES:
 * This class initializes Charging Condition Publisher and sends current status
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
public final class ChargingInitHandler extends  CommandHandler implements ChargingConstants {

    private final static String LOG_TAG = ChargingInitHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(final Context context, final Intent intent) {


        final List<String> valueList = Persistence.retrieveValuesAsList(context, CHARGING_PERSISTENCE);
        if(!valueList.isEmpty()) {
            // First config for this publisher, so register
            registerReceiver(context, CHARGING_STATE_MONITOR);
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        final Intent chargingIntent = context.registerReceiver(null, filter);
        if(chargingIntent != null) {
        	final int plugged = chargingIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            ChargingReceiver.postNotify(context, plugged);
        }
        return SUCCESS;
    }

}

