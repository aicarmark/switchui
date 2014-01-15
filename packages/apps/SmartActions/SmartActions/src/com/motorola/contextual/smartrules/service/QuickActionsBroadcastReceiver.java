/*
 * @(#)QuickActionsBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/12/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/** This is a receiver class for listening to the intents from Quick Actions.
 *
 *<code><pre>
 * CLASS:
 * 	 extends BroadcastReceiver.
 *
 *  implements
 *    Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	 Launches the IntentService QuickActionsService to process the request from VSM
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class QuickActionsBroadcastReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = QuickActionsBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            if(LOG_DEBUG) Log.d(TAG, "In onReceive to handle "+intent.toUri(0));
            String action = intent.getAction();
            if(action != null) {
		         if(action.equals(ACTION_PUBLISHER_EVENT)) {

			        Bundle bundle = intent.getExtras();
			        if(bundle != null) {
				        bundle.putString(INTENT_ACTION, intent.getAction());
						Intent serviceIntent = new Intent(context, QuickActionsService.class);
						serviceIntent.putExtras(bundle);
						context.startService(serviceIntent);    
			        } else
			        	Log.e(TAG, "intent.getExtras() is null");
		        }else
		        	Log.e(TAG, "Receiver not invoked for handling = "+intent.getAction()+" which is not a subscribed action");
            } else
            	Log.e(TAG, "Receiver invoked with null intent action");
        } else
        	Log.e(TAG, "Receiver invoked with a null intent");
    }
}
