/*
 * @(#)BaseReceiver.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/02/14 NA                Initial version
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
import com.motorola.contextual.rule.publisher.PublisherIntentService;

/**
 * This class receives boot complete broadcast.
 *
 * <code><pre>
 * CLASS:
 *  Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *  - Receive broadcasts
 *  - start RP service if a valid broadcast has been received
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
public class BaseReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = RP_TAG + BaseReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.e(TAG, "null intent received!");
            return;
        }

        String action = intent.getAction();
        if (action != null) {

            Intent serviceIntent = new Intent(context, PublisherIntentService.class);
            serviceIntent.setAction(action);
            Bundle b = intent.getExtras();
            if(b != null) serviceIntent.putExtras(b);
            serviceIntent.putExtra(IS_STICKY, isInitialStickyBroadcast());

            if (LOG_DEBUG) Log.d(TAG, "Action=" + action);
            ComponentName component = context.startService(serviceIntent);
            if (component == null) {
                Log.e(TAG, "context.startService() failed for: "
                        + PublisherIntentService.class.getSimpleName());
            }
        } else if (LOG_INFO) Log.e(TAG, "Null Action");
    }
}
