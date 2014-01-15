/*
 * @(#)StateMonitorInterface.java
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

package com.motorola.contextual.smartrules.monitorservice;


import java.util.ArrayList;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.ContentObserver;


/**
 * This is an interface to represent a state monitor
 *
 * <CODE><PRE>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * This is an interface to represent a state monitor
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public abstract class  CommonStateMonitor  {

    /**
     * This method gets the broadcast receiver from the state monitor
     * @return BroadcastReceiver
     */
    public BroadcastReceiver getReceiver() {
        return null;
    }

    /**
     * This method sets the broadcast receiver in the state monitor
     * @param BroadcastReceiver
     */
    public void setReceiver(BroadcastReceiver receiver) {

    }

    /**
     * This method sets the content observer in the state monitor
     * @param ContentObserver
     */
    public void setObserver(ContentObserver receiver) {

    }

    /**
     * This method gets the content observer from the state monitor
     * @return ContentObserver
     */
    public ContentObserver getObserver(Context context) {
        return null;
    }
    /**
     * This method gets the type of the state monitor
     * @return type - Receiver / Observer
     */
    public String getType() {
        return null;
    }
    /**
     * This method gets the list of monitored actions[for Receivers] or
     * URIs[for Observers] monitored by the state monitors
     * @return List of actions
     */
    public ArrayList<String> getStateMonitorIdentifiers() {
        return null;
    }

}
