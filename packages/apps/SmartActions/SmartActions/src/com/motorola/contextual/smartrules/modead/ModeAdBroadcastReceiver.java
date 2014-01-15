/*
 * @(#)ModeAdBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383        2012/05/03 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.modead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/** This is a receiver class
 *
 *<code><pre>
 * CLASS:
 * 	 extends BroadcastReceiver.
 *
 *  implements
 *   Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	 Starts the IntentService  to process the requests
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class ModeAdBroadcastReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = ModeAdBroadcastReceiver.class.getSimpleName();

    /** onReceive()
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent != null) {
            String action = intent.getAction();
            if(action != null) {
                if(LOG_DEBUG) Log.i(TAG, "In onReceive to handle "+intent.toUri(0));
                Intent serviceIntent = new Intent(context, ModeAdService.class);
                serviceIntent.setAction(action);
                if(intent.getExtras() != null) serviceIntent.putExtras(intent.getExtras());
                context.startService(serviceIntent);
            } else
                Log.e(TAG, "Receiver invoked with a null action");
        } else
            Log.e(TAG, "Receiver invoked with a null intent");

    }

}