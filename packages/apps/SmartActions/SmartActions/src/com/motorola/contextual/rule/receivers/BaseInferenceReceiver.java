/*
 * @(#)BaseInferenceReceiver.java
 *
 * (c) COPYRIGHT 2012-2013 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/07/14 NA                Initial version
 *
 */

package com.motorola.contextual.rule.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.publisher.PublisherIntentService;

/**
 * This class is the parent class of the broadcasts received
 * for inference use cases
 *
 * <code><pre>
 * CLASS:
 *  Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *  - Receive broadcasts
 *  - start RP service
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 *
 * @author a21693
 *
 */
public abstract class BaseInferenceReceiver extends BroadcastReceiver {

    private static final String TAG = Constants.RP_TAG
        + BaseInferenceReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if(Constants.LOG_VERBOSE) Log.i(TAG, intent.toString());

        if(intent != null){

            Intent serviceIntent = new Intent(context, PublisherIntentService.class);
            Bundle b = intent.getExtras();
            if(b != null) serviceIntent.putExtras(b);
            serviceIntent.putExtra(Constants.IS_STICKY, isInitialStickyBroadcast());
            serviceIntent.putExtra(CoreConstants.EXTRA_EVENT_TYPE, Constants.INFERENCE_INTENT);
            serviceIntent.setAction(intent.getAction());

            ComponentName component = context.startService(serviceIntent);
            if (component == null) {
                Log.e(TAG, "context.startService() failed for: "
                        + PublisherIntentService.class.getSimpleName());
            }
        }
    }
}