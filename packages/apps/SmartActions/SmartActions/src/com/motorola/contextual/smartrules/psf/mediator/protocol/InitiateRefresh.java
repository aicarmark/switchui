/*
 * @(#)InitiateRefresh.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/07/09  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator.protocol;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.psf.mediator.MediatorConstants;
import com.motorola.contextual.smartrules.psf.mediator.MediatorHelper;

import android.content.Context;
import android.content.Intent;

/**
 * This class handles "initiate refresh" from condition publishers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send "initiate refresh" intent to all the consumers
 *
 * COLABORATORS:
 *     Consumer - Uses the preconditions available across the system
 *     ConditionPublisher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class InitiateRefresh implements IMediatorProtocol, MediatorConstants {

    private String mPublisher;

    public InitiateRefresh(Intent intent) {
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
    }


    public boolean processRequest(Context context, Intent intent) {
        return (mPublisher != null);
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        for (String consumer : CONSUMERS) {
            intentsToBroadcast.add(MediatorHelper.createInitRefreshIntent(consumer, mPublisher));
        }
        return intentsToBroadcast;
    }

}
