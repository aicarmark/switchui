/*
 * @(#)ChargingStateMonitor.java
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

package com.motorola.contextual.pickers.conditions.charging;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Intent;


import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends CommonStateMonitor for charging publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommonStateMonitor
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Extends methods of CommonStateMonitor for charging publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class ChargingStateMonitor extends  CommonStateMonitor implements Constants {

    private static ChargingReceiver mReceiver = null;

    @Override
    public BroadcastReceiver getReceiver() {
        if(mReceiver == null) mReceiver = new ChargingReceiver();
        return mReceiver;
    }

    @Override
    public void setReceiver(BroadcastReceiver receiver) {
        mReceiver = (ChargingReceiver)receiver;
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
