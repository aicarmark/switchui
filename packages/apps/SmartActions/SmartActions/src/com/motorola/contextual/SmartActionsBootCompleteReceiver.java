/*
 * @(#)SmartActionsBootCompleteReceiver.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383       2012/01/18  NA                  Initial version
 */

package com.motorola.contextual;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles the BOOT_COMPLETED intent received on power up
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver.
 *
 * RESPONSIBILITIES:
 *     Launches SettingsService
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class SmartActionsBootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = SmartActionsBootCompleteReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
    	String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Null intent or action");
            return;
        }
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
        	Log.d(TAG, " Boot Complete received");
            Intent bootCompleteIntent = new Intent("com.motorola.intent.action.BOOT_COMPLETE_RECEIVED");
            context.sendBroadcast(bootCompleteIntent);
        } else {
            Log.w(TAG, "Action not recognized");
        }

    }

}
