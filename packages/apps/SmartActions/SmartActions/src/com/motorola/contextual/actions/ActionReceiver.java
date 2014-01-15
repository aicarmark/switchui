/*
 * @(#)ActionReceiver.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/03/20  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles the request to execute commands for all actions.
 *
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *    Executes fire, revert, refresh and list for all actions
 *
 * COLABORATORS:
 *     Actions
 *     Smart Actions Core
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class ActionReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = TAG_PREFIX + ActionReceiver.class.getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {

        String pubKey = intent.getAction();
        String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
        if (pubKey == null) {
            Log.e(TAG, "Null intent or action");
            return;
        }

        if (LOG_INFO) Log.i(TAG, "Received intent with action:" + pubKey + ", event:" + eventType +
                ", config:" + intent.getStringExtra(EXTRA_CONFIG));
        
        if (pubKey.equals(POWER_SETTING_INTENT)) {
          //Start Service to change powerManager Options
            Intent serviceIntent = new Intent(POWER_SETTING_INTENT, null, context, FWSettingChangeService.class);
            serviceIntent.putExtras(intent.getExtras());
            context.startService(serviceIntent);
            return;
        } else if (pubKey.equals(ACTION_PUBLISHER_EVENT) && EXTRA_STATE_CHANGE.equalsIgnoreCase(eventType)) {
            //Start Service to handle setting change
            Intent serviceIntent = new Intent(ACTION_PUBLISHER_EVENT, null, context, FWSettingChangeService.class);     
            serviceIntent.putExtras(intent.getExtras());
            context.startService(serviceIntent);
            return;
        } else {    
            // Get the corresponding action
            Action action = ActionHelper.getAction(context, pubKey);
            // did we get a valid action and command i.e. recognize the action and command ?
            if (action != null && eventType != null) {
                Intent responseIntent = action.handleChildActionCommands(context, intent);
                if (responseIntent == null) {
                    if (eventType.equalsIgnoreCase(COMMAND_FIRE)) {
                        responseIntent = action.handleFire(context, intent);
                    } else if (eventType.equalsIgnoreCase(COMMAND_REVERT)) {
                        responseIntent = action.handleRevert(context, intent);
                    } else if(eventType.equalsIgnoreCase(COMMAND_REFRESH)) {
                        responseIntent = action.handleRefresh(context, intent);
                    } else if(eventType.equalsIgnoreCase(COMMAND_LIST)) {
                        responseIntent = action.handleList(context, intent);
                    }
                }
        
                if (responseIntent != null) {
                    if (LOG_DEBUG) {
                        Log.d(TAG, "Sending response intent: " + responseIntent.toUri(0));
                    }
                    context.sendBroadcast(responseIntent, PERM_ACTION_PUBLISHER_ADMIN);
                }
            } else {
                if (!pubKey.equals(ACTION_PUBLISHER_EVENT))
                    Log.w(TAG, "Action not recognized for event = " + eventType);
            }
        }
    }

}
