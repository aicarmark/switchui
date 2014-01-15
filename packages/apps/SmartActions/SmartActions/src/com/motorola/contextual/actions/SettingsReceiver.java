/*
 * @(#)SettingsReceiver.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.actions.StatefulAction.Status;
import com.motorola.contextual.debug.DebugTable;

/**
 * This class handles the system-wide broadcasts sent for changes in BT/WiFi/Ringer/Airplane
 * Mode/Background Sync states. <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver.
 *
 * RESPONSIBILITIES:
 *     When the broadcast for a particular setting is received:
 *      1. Checks if this was triggered by Smart Rules, if so sends
 *         the action exec status intent.
 *      2. If this was caused by an external entity (user or other apps)
 *          a. If Smart Rules is interested in this setting, sends SETTING_CHANGE
 *             intent.
 *          b. Else ignores the broadcast.
 *
 * COLLABORATORS:
 *     Smart Rules
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class SettingsReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = TAG_PREFIX + SettingsReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null || intent.getExtras() == null) {
            Log.w(TAG, "Null intent or action or extras");
            return;
        }

        if (LOG_INFO) Log.i(TAG, "onReceived Called " + intent.toUri(0));
        
        Intent svc = new Intent(context, SettingsUtilityService.class);
        svc.putExtra(EXTRA_INTENT_ACTION, intent.getAction());
        svc.putExtras(intent.getExtras());
        context.startService(svc);
    }
}
