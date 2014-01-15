/*
 * @(#)IntentReceiver.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */

package com.motorola.contextual.smartrules.psf;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**This class is a service for receiving all the intents intended for the PSF
 *<code><pre>
 * CLASS:
 *     Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 *     Starts a service to process the intents
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PsfReceiver extends BroadcastReceiver implements PsfConstants {

    public static final String TAG = PsfConstants.PSF_PREFIX + PsfReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String incomingAction = intent.getAction();

        Intent serviceIntent = new Intent(context, PsfService.class);
        serviceIntent.setAction(incomingAction);
        serviceIntent.setData(intent.getData());

        Bundle parameters = intent.getExtras();
        if (parameters != null) {
            serviceIntent.putExtras(parameters);
        }

        ComponentName compName = context.startService(serviceIntent);
        if (compName == null) {
            if (LOG_ERROR) Log.e(TAG, "Unable to start " + serviceIntent.toUri(0));
        }
    }
}
