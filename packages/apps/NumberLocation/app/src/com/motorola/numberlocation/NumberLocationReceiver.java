package com.motorola.numberlocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NumberLocationReceiver extends BroadcastReceiver {

	private static final String TAG = "NumberLocationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "###onReceive get intent " + intent.getAction());
		if (!NumberLocationUtilities.isAutoUpdateEnabled(context)){
			Log.d(TAG, "###isAutoUpdateEnabled return false, skip intent -- " + intent.getAction());
			return;
		}
		Intent i = new Intent(context, NumberLocationService.class);
		// if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		// i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(intent.getAction());
		context.startService(i);
	}

}
