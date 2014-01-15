package com.motorola.mmsp.performancemaster.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.motorola.mmsp.performancemaster.engine.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "BootReceiver: ";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(LOG_TAG, "onReceive Boot completed");
        // Build the intent to call the service
        Intent intentStart = new Intent(context.getApplicationContext(),
                BatteryWidgetService.class);
        intentStart.setAction(BatteryWidgetService.ACTION_BOOT_START);

        // Update the widgets via the service
        context.startService(intentStart);
    }
}
