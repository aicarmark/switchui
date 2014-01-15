/*
 * @(#)MotionDetectorAdapterStateMonitor.java
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

package com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends CommonStateMonitor for motion detection adapter publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommonStateMonitor
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Extends methods of CommonStateMonitor for motion detection adapter publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MotionDetectorAdapterStateMonitor extends CommonStateMonitor implements Constants {

    private static MotionDetectorAdapterReceiver mReceiver = null;
    private static String ACTION_MOTION = "com.motorola.intent.action.MOTION";
    private static String ACTION_STILL = "com.motorola.intent.action.STILL";
    

    @Override
    public BroadcastReceiver getReceiver() {
        if(mReceiver == null) mReceiver = new MotionDetectorAdapterReceiver();
        return mReceiver;
    }

    @Override
    public void setReceiver(BroadcastReceiver receiver) {
        mReceiver = (MotionDetectorAdapterReceiver)receiver;
    }

    @Override
    public String getType() {
        return RECEIVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> actions = new ArrayList<String>();

        actions.add(ACTION_MOTION);
        actions.add(ACTION_STILL);
        return actions;
    }
}
