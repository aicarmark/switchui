/*
 * @(#)BatteryLevelStateMonitor.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/16  NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.battery;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Intent;


import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;


/**
 * This class extends CommonStateMonitor for battery publisher
 *
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     extends CommonStateMonitor
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Extends methods of CommonStateMonitor for battery publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class BatteryLevelStateMonitor extends  CommonStateMonitor implements Constants  {

    private static BatteryLevelReceiver mReceiver = null;

    @Override
    public BroadcastReceiver getReceiver() {
        if(mReceiver == null) mReceiver = new BatteryLevelReceiver();
        return mReceiver;
    }

    @Override
    public void setReceiver(BroadcastReceiver receiver) {
        mReceiver = (BatteryLevelReceiver)receiver;
    }

    @Override
    public String getType() {
        return RECEIVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> actions = new ArrayList<String>();

        actions.add(Intent.ACTION_BATTERY_CHANGED);
        return actions;
    }

}
