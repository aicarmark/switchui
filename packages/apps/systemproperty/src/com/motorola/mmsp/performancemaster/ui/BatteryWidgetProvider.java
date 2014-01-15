/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * bntw34                      02/05/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
//import android.content.SharedPreferences;

import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;

/**
 * battery widget provider
 */
public class BatteryWidgetProvider extends AppWidgetProvider {
    private final static String LOG_TAG = "BatteryWidget: ";
    
    /*
    private void checkDisclaimer(Context context) {
        SharedPreferences sharePrefs = context.getSharedPreferences(BatteryModeMgr.BATTERY_DISCLAIMER_PREFS, 0);
        int shown = sharePrefs.getInt(BatteryModeMgr.BATTERY_DISCLAIMER_SHOWN, -1);
        if (shown != 1) {
            Intent i = new Intent(context, DisclaimerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
    */
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        
        Log.e(LOG_TAG, "onEnabled");
        //checkDisclaimer(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.e(LOG_TAG, "onUpdate called");

        ComponentName thisWidget = new ComponentName(context.getApplicationContext(),
                BatteryWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Build the intent to call the service
        Intent intent = new Intent(context.getApplicationContext(),
                BatteryWidgetService.class);
        intent.putExtra(BatteryWidgetService.EXTRA_WIDGET_IDS, allWidgetIds);
        intent.setAction(BatteryWidgetService.ACTION_APPWIDGET_UPDATE);

        // Update the widgets via the service
        context.startService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED)) {
                    Log.e(LOG_TAG, "BatteryWidgetProvider -->ACTION_BATTERY_MODE_CHANGED");
                    Intent i = new Intent(context.getApplicationContext(),
                            BatteryWidgetService.class);
                    i.setAction(BatteryWidgetService.ACTION_MODE_UPDATE);
                    context.startService(i);
                } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.e(LOG_TAG, "BatteryWidgetProvider -->ACTION_SCREEN_OFF");
                    Intent i = new Intent(context.getApplicationContext(),
                            BatteryWidgetService.class);
                    i.setAction(BatteryWidgetService.ACTION_SCR_OFF_UPDATE);
                    context.startService(i);
                } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    Log.e(LOG_TAG, "BatteryWidgetProvider -->ACTION_SCREEN_ON");
                    Intent i = new Intent(context.getApplicationContext(),
                            BatteryWidgetService.class);
                    i.setAction(BatteryWidgetService.ACTION_SCR_ON_UPDATE);
                    context.startService(i);
                }
            }
        }
    }
}
