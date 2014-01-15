/*
 * @(#)jarfileOnLoadBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * w30219        2012/04/05 NA				  Initial version
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
* 	 Receives update from JarfileLoaderInterface and update the SharedPreference.
*
* COLABORATORS:
* 	 None.
*
* USAGE:
* 	 See each method.
*</pre></code>
*/
public class jarfileOnLoadBroadcastReceiver extends BroadcastReceiver implements Constants {

	private static final String TAG = jarfileOnLoadBroadcastReceiver.class.getSimpleName();
		
	/** onReceive()
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent == null) {
            Log.e(TAG, "intent is null");
		}
		else {
            if(LOG_INFO) Log.i(TAG, "In onReceive to handle "+intent.toUri(0));
            String action = intent.getAction();
            if(action != null && action.equals(ONLOAD_INTENT)) {
            	Intent jarfileIntent = new Intent(context, JarFileService.class);
            	jarfileIntent.putExtra(EXTRA_ACTIONNAME, EXTRA_ACTION_ONLOAD);
            	jarfileIntent.putExtra(EXTRA_CLASSNAME, intent.getStringExtra(EXTRA_CLASSNAME));
				context.startService(jarfileIntent); 
            }
		}
	}
}