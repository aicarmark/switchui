/*
 * @(#)FrameworkResponseReceiver.java
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
 * This receiver receives command response intents from old Smart Actions FW
 * and converts them to new format before sending them to SA Core
 *
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *    Converts command response intents from old format to new format
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

public class FrameworkResponseReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = TAG_PREFIX + FrameworkResponseReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (LOG_INFO) Log.i(TAG, "Received broadcast: "+intent.toUri(0));
        String action = intent.getAction();

        if (action != null) {
            String actionKey = intent.getStringExtra(EXTRA_ACTION_KEY);

            if (action.equals(ACTION_EXEC_STATUS)) {
                String responseEventType =
                    (intent.getIntExtra(EXTRA_REQ_TYPE, FrameworkUtils.REQ_TYPE_FIRE) == FrameworkUtils.REQ_TYPE_FIRE)
                    ? EXTRA_FIRE_RESPONSE : EXTRA_REVERT_RESPONSE;
                //In old FW status was sent as "Success" or "Failure"
                //In new arch it is changed to "success" / "failure"
                Intent responseIntent = ActionHelper.getResponseIntent(context, actionKey,
                                        intent.getStringExtra(EXTRA_OLD_STATUS).toLowerCase(), intent.getStringExtra(RULEKEY),
                                        intent.getStringExtra(EXTRA_EXCEPTION_STRING), responseEventType);
                if (responseEventType == EXTRA_FIRE_RESPONSE) {
                    //SA Core won't send config in revert case
                    //Save the values to persistence
                    String defaultUri = intent.getStringExtra(EXTRA_OLD_DEFAULT_URI);
                    Persistence.commitValue(context, actionKey + DEFAULT_FW_SUFFIX, defaultUri);
                }
                if (responseIntent != null) {
                    context.sendBroadcast(responseIntent, PERM_ACTION_PUBLISHER_ADMIN);
                    if (LOG_INFO) {
                        Log.i(TAG, "Sending broadcast: " + responseIntent.toUri(0));
                    }
                }

            } else if (action.equals(ACTION_SETTING_CHANGE)) {
                StatefulActionHelper.sendSettingsChange(context, actionKey, intent.getStringExtra(EXTRA_SETTING_DISPLAY_STRING));
            }
        } else {
            Log.e(TAG, "Error. Intent action is null");
        }
    }

}
