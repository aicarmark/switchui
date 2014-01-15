/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * gkp864                      07/06/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MemoryCleanWidget extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "Widget_Update";

    public static final String TAG = "MemoryCleanWidget";

    public static int mRamFree = -10;

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.i(TAG, "!!!!!!!!!!onDeleted!!!!!!!!!");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {

        Log.i(TAG, "!!!!!!!!!!onDisabled!!!!!!!!!");
        // Intent intent = new Intent(context, MemoryCleanerService.class);
        // context.stopService(intent);
        super.onDisabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.i(TAG, "!!!!!!!!!!onUpdate!!!!!!!!!");

        for (int i = 0; i < appWidgetIds.length; i++) {
            Log.i(TAG, "!!!!!!!!!!appWidgetIds" + i + "=" + appWidgetIds[i]);
        }
        /****
         * when MemoryCleanWidget is added home, ensure MemoryCleanerService
         * just start only one service don't get interface whether running or
         * stop
         ****/
        Intent i = new Intent(context, MemoryCleanerService.class);
        i.setAction(ACTION_UPDATE_WIDGET);
        i.putExtra(MemoryCleanerService.EXTRA_WIDGET_IDS, appWidgetIds);
        Log.i(TAG, "start widget service");
        context.startService(i);
    }

}
