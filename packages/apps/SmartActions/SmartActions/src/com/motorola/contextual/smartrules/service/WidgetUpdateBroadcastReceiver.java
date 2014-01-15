/*
 * @(#)WidgetUpdateBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/03/12 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/** This is a receiver class for handling the widget update request.
*
*<code><pre>
* CLASS:
* 	 extends BroadcastReceiver.
*
*  implements
*   Constants - for the constants used
*
* RESPONSIBILITIES:
* 	 Starts the IntentService WidgetUpdateService to process the request from widget.
*
* COLABORATORS:
* 	 None.
*
* USAGE:
* 	 See each method.
*</pre></code>
*/
public class WidgetUpdateBroadcastReceiver extends BroadcastReceiver implements Constants {

	private static final String TAG = WidgetUpdateBroadcastReceiver.class.getSimpleName();
		
	/** onReceive()
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent == null) {
            Log.e(TAG, "intent is null");
		}
		else {
            if(LOG_INFO) Log.i(TAG, "In onReceive to handle "+intent.toURI());
            String action = intent.getAction();
            if(action != null && action.equals(WIDGET_UPDATE_INTENT)) {
            	if(LOG_DEBUG) Log.d(TAG, "Launched for WIDGET_UPDATE_INTENT");
            	String[] ruleKeys = intent.getStringArrayExtra(WidgetUpdateService.RULE_KEYS_EXTRA);
            	if(ruleKeys == null || ruleKeys.length == 0) {
            		Log.e(TAG, "nothing to update as the ruleKeys array is "+(ruleKeys == null ? "null" : ruleKeys.length));
            	} else {
            		Intent serviceIntent = new Intent(context, WidgetUpdateService.class);
            		serviceIntent.putExtra(WidgetUpdateService.RULE_KEYS_EXTRA, ruleKeys);
            		context.startService(serviceIntent);
            	}
            }
		}
	}
}