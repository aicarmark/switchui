/*
 * @(#)PublisherManagerReceiver.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/04/12                   Initial version
 *
 */
package com.motorola.contextual.smartrules.publishermanager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**This class is a receiver for receiving all the intents intended for the PublisherManager
 *<code><pre>
 * CLASS:
 *     PublisherManagerReceiver Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *     Starts a service to process the intents
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PublisherManagerReceiver extends BroadcastReceiver implements PublisherManagerConstants {
    private static final String TAG = PublisherManagerReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if(LOG_DEBUG) Log.d(TAG, "onReceive " + intent.toUri(0));
        String action = intent.getAction();
        if(action != null) {
            Intent serviceIntent = new Intent(context, PublisherManagerService.class);
            if(action.equals(ACTION_PUBLISHER_EVENT) ||
                    action.equals(SA_CORE_KEY)) {
                String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
                if(eventType == null || (!eventType.equals(REFRESH_RESPONSE) &&
                                         !eventType.equals(INITIATE_REFRESH_REQUEST))) {
                    if(LOG_DEBUG) Log.d(TAG, " Not handling " + eventType);
                    return;
                }
            } else if(action.equals(ACTION_RULES_VALIDATE_REQUEST)) {
                serviceIntent = new Intent(context, RulesValidatorService.class);
            }
            serviceIntent.setAction(action);
            Bundle extras = intent.getExtras();
            if(extras != null) {
                serviceIntent.putExtras(extras);
            }
            ComponentName compName = context.startService(serviceIntent);
            if (compName == null) {
                Log.e(TAG, "Unable to start " + serviceIntent.toUri(0));
            }
        }
    }
}
