/*
 * @(#)FrameworkActionReceiver.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/04/16  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles the request to execute commands for all actions present in Smart Actions FW
 * It checks if the FW is old and accordingly converts the incoming intent to execute the action
 *
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *    Checks if Smart Actions FW is old and converts the intent accordingly
 *
 * COLABORATORS:
 *     Smart Actions Framework
 *     Smart Actions Core
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class FrameworkActionReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = TAG_PREFIX + FrameworkActionReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String pubKey = intent.getAction();
        if (pubKey == null) {
            Log.e(TAG, "Null intent or action");
            return;
        }

        if (LOG_INFO) {
            Log.i(TAG, "Received intent with action:" + pubKey + ", event:" +
                    intent.getStringExtra(EXTRA_EVENT_TYPE) + ", config:" + intent.getStringExtra(EXTRA_CONFIG));
        }

        if (FrameworkUtils.isFrameworkOld(context)) {
            if (LOG_INFO) Log.i(TAG, "Old Smart Actions framework needed to execute this action");
            FrameworkUtils.handleIntent(context, intent);
        }
    }
}
