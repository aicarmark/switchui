/*
 * @(#)InitCompleteBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/12/03 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/** This is a receiver class for listening to the ACTION_SA_CORE_INIT_COMPLETE intent.
 *
 *<code><pre>
 * CLASS:
 * 	 extends BroadcastReceiver.
 *
 *  implements
 *    Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	 Broadcasts an Smart Rules init complete intent.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class InitCompleteBroadcastReceiver extends BroadcastReceiver implements Constants {

	private static final String TAG = InitCompleteBroadcastReceiver.class.getSimpleName();
		
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent == null) {
            Log.e(TAG, "intent is null");
		}
		else {
            if(LOG_INFO) Log.i(TAG, "In onReceive to handle "+intent.toUri(0));
            String action = intent.getAction();
            if(action != null && action.equals(ACTION_SA_CORE_INIT_COMPLETE)) {
            	if(LOG_DEBUG) Log.d(TAG, "Launched for ACTION_SA_CORE_INIT_COMPLETE");
            	
				Intent responseIntent = new Intent(SMARTRULES_INIT_EVENT);
				context.sendBroadcast(responseIntent, SMART_RULES_PERMISSION);	
				
				Intent jarfileIntent = new Intent(context, JarFileService.class);
				jarfileIntent.putExtra(EXTRA_ACTIONNAME, EXTRA_ACTION_LOADFILE);
				context.startService(jarfileIntent);
            }else
            	Log.e(TAG, "invalid action");
		}		
	}
}
