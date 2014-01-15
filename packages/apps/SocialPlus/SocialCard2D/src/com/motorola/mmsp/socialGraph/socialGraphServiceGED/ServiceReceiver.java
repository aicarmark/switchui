/*
 * the broadcast receiver will be launched after it received the boot completed intent,
 * and then will launch the social graph service
 */

package com.motorola.mmsp.socialGraph.socialGraphServiceGED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class ServiceReceiver extends BroadcastReceiver {
	private final String TAG = "SocialGraphService";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
               if (intent == null) {
                    return;
               }
		if (/*"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())||*/"com.motorola.mmsp.intent.action.test".equals(intent.getAction())) {
            // launch the service
			if(ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD){
				Log.d(TAG, "SocialGraph receive the boot notification");
			}
            context.startService(new Intent(ConstantDefinition.SOCIAL_SERVICE_ACTION));		
        }else if("android.intent.action.PACKAGE_DATA_CLEARED".equals(intent.getAction())){
            // launch the service
			if(ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD){
				Log.d(TAG, "SocialGraph receive the PACKAGE_DATA_CLEARED notification");
			}
			Uri data = intent.getData();
			if (data != null) {
			String pkgName = data.getSchemeSpecificPart();
			Log.d(TAG, "SocialGraph receive the PACKAGE_DATA_CLEARED notification" + pkgName);
			if (pkgName != null&& "com.android.providers.contacts".equals(pkgName)) {
	            context.startService(new Intent("com.motorola.mmsp.intent.action.CONTACTS_CLEAR_ACTION"));	
			}
			}
	        	
        }
	}

		
}
