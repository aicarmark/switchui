/*
 * @(#)ListResponse.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/07/03  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator.protocol;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.smartrules.psf.mediator.MediatorConstants;
import com.motorola.contextual.smartrules.psf.mediator.MediatorHelper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles "list response" from condition publishers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "list response" intent to desired consumer
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

public class ListResponse implements IMediatorProtocol, MediatorConstants {

    private static final String TAG = MEDIATOR_PREFIX + ListResponse.class.getSimpleName();

    private String mPublisher;
    private String mResponseId;
    private String mConfigItems;
    private String mNewStateTitle;

    public ListResponse(Intent intent) {
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mResponseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
        mConfigItems = intent.getStringExtra(EXTRA_CONFIG_ITEMS);
        mNewStateTitle = intent.getStringExtra(EXTRA_NEW_STATE_TITLE);
    }

    public boolean processRequest(Context context, Intent intent) {
        return (mPublisher != null && mResponseId != null &&
                mResponseId.contains(MEDIATOR_SEPARATOR)) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();

        int separatorIndex = mResponseId.indexOf(MEDIATOR_SEPARATOR);
        String consumer = mResponseId.substring(0, separatorIndex);
        mResponseId = mResponseId.substring(separatorIndex + MEDIATOR_SEPARATOR.length());

        if (!StringUtils.isEmpty(consumer) && !StringUtils.isEmpty(mResponseId)) {
            intentsToBroadcast.add(MediatorHelper.createListResponseIntent(consumer, mResponseId, mPublisher,
                                   mConfigItems, mNewStateTitle));
        } else {
            Log.e(TAG, "Consumer or responseId empty. Aborting");
        }

        return intentsToBroadcast;
    }

}