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

/**This class is a receiver for receiving all the intents intended for the RulesValidatorService
 *<code><pre>
 * CLASS:
 *     RuleValidatorReceiver Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *     Starts a service to process the intents
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class RuleValidatorReceiver extends BroadcastReceiver implements PublisherManagerConstants {
    private static final String TAG = RuleValidatorReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if(LOG_DEBUG) Log.d(TAG, "onReceive " + intent.toUri(0));
        String action = intent.getAction();
        if(action != null) {
            Intent serviceIntent = new Intent(context, RulesValidatorService.class);
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
