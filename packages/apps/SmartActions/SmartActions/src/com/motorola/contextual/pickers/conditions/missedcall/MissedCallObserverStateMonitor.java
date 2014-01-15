/*
 * @(#)MissedCallObserverStateMonitor.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/16  NA                Initial version of MissedCallObserverStateMonitor
 *
 */

package com.motorola.contextual.pickers.conditions.missedcall;

import java.util.ArrayList;
import android.content.Context;
import android.database.ContentObserver;
import android.provider.CallLog;


import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends CommonStateMonitor for Missed Call publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommonStateMonitor
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Extends methods of CommonStateMonitor for Missed Call publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MissedCallObserverStateMonitor extends CommonStateMonitor implements Constants {

    private static MissedCallObserver mObserver = null;

    @Override
    public ContentObserver getObserver(Context context) {

        if(mObserver == null) mObserver = new MissedCallObserver(context);
        return mObserver;
    }

    @Override
    public void setObserver(ContentObserver observer) {
        mObserver = (MissedCallObserver)observer;
    }

    @Override
    public String getType() {
        return OBSERVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> uris = new ArrayList<String>();

        uris.add(CallLog.Calls.CONTENT_URI.toString());
        return uris;
    }
}
