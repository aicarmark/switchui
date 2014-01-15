package com.motorola.quicknote;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

public class QNUpdateWidget extends BroadcastReceiver {
	private static final String TAG = "QNUpdateWidget";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		// QNDev.log(TAG + " intent action : " + action);
          if(action != null && action.equals("android.intent.action.MEDIA_UNMOUNTED") 
                || action.equals("android.intent.action.MEDIA_MOUNTED")) {
                Intent updateIntent = new Intent(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED);
                context.sendBroadcast(updateIntent);
          }         
	}
}
