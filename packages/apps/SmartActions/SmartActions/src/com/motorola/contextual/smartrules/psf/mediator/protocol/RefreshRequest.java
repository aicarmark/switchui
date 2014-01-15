/*
 * @(#)RefreshRequest.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/22  NA                Initial version
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
 * This class handles "refresh" command from consumers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "refresh" command intent to requested publisher
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

public class RefreshRequest implements IMediatorProtocol, MediatorConstants {

    private String mConsumer;
    private String mConfig;
    private String mPublisher;
    private String mRequestId;

    public RefreshRequest(Intent intent) {
        mConsumer = intent.getStringExtra(EXTRA_CONSUMER);
        mConfig = intent.getStringExtra(EXTRA_CONFIG);
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mRequestId = intent.getStringExtra(EXTRA_REQUEST_ID);
    }


    public boolean processRequest(Context context, Intent intent) {
        return (mConsumer != null && mConfig != null && mPublisher != null &&
                mRequestId != null) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        intentsToBroadcast.add(MediatorHelper.createCommandIntent(mPublisher, mConfig, mConsumer + MEDIATOR_SEPARATOR + mRequestId, REFRESH_EVENT));
        return intentsToBroadcast;
    }

}
