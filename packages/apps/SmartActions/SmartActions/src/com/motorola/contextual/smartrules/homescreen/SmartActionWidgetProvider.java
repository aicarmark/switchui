/**
 * @(#)SmartActionWidgetProvider.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author          Date       CR Number            Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * VXMD37        2012/03/11        NA              Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.homescreen.DriveModeService.RequestType;

/**
 * <code><pre>
 * CLASS: SmartActionWidgetProvider
 * 	 extends AppWidgetProvider
 *
 *  implements
 *   Constants - for the constants used.
 *
 * RESPONSIBILITIES:
 *  Used by the home screen to create the drive mode widget.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class SmartActionWidgetProvider extends AppWidgetProvider implements
        Constants {
    private static final String TAG = "SmartActionWidget";

    /**
     * Constructor
     */
    public SmartActionWidgetProvider() {
        if (LOG_DEBUG) {
            Log.d(TAG, "New widget instance");
        }
        // if (sManager == null) sManager = new RuleManager();
    }

    /**
     * onReceive()
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOG_INFO) Log.d(TAG, "onReceive " + intent.toUri(0));

        super.onReceive(context, intent);
        String action = intent.getAction();
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
            new ComponentName(context, SmartActionWidgetProvider.class.getName()));
        if (LOG_DEBUG) {
            Log.d(TAG, "onReceive widgets# " + appWidgetIds.length);
        }
        if (action.equals(RULE_DELETED_MESSAGE)) {
            context.startService(this.getServiceIntent(context, RequestType.RULE_DELETED, intent));

        } else if (action.equals(DATA_CLEAR)) {
            context.startService(getServiceIntent(context, RequestType.DATA_CLEAR));
        } else if (appWidgetIds.length < 1) {

            // there are no widgets
            Log.i(TAG, "No Drive mode widget");
        } else if (action.equals(SMARTRULES_INIT_EVENT)) {
            context.startService(getServiceIntent(context, RequestType.INIT_COMPLETE));
        } else if (action.equals(RULE_ADDED_ACTION)) {
            context.startService(this.getServiceIntent(context, RequestType.SCHEDULE_SYNC, intent));

        } else {
            if (action.equals(RULE_PROCESSED_RESPONSE) || action.equals(RULE_MODIFIED_MESSAGE)) {
                context.startService(this.getServiceIntent(context, RequestType.SYNC_IMMEDIATE,
                    intent));

            } else if (action.equals(RULE_ATTACHED_ACTION)) {
                context.startService(this.getServiceIntent(context, RequestType.SYNC_IMMEDIATE,
                    intent).putExtra(DriveModeService.MAP_RULE, true));
            } else if (action.equals(WIDGET_UPDATE_RESPONSE)) {
                context.startService(this.getServiceIntent(context, RequestType.SYNC_RESPONSE,
                    intent));
            } else if (action.equals(INFERRED_RULES_ADDED)) {
                context.startService(this.getServiceIntent(context, RequestType.SYNC_REQUEST,
                    intent));
            }
        }
    }

    /**
     * onUpdate()
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (LOG_DEBUG) {
            Log.d(TAG, "onUpdate widgets# " + appWidgetIds.length + " : "
                    + (appWidgetIds.length > 0 ? appWidgetIds[0] : "INVALID"));
        }
        context.startService(this.getServiceIntent(context, RequestType.NEW_WIDGET).putExtra(
            EXTRA_RESPONSE_ID, appWidgetIds));
    }

    /**
     * onDeleted()
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (LOG_DEBUG) {
            Log.d(TAG, "onDeleted widgets# " + appWidgetIds.length);
        }
        context.startService(this.getServiceIntent(context, RequestType.WIDGET_DELETED).putExtra(
            EXTRA_RESPONSE_ID, appWidgetIds));
    }

    /**
     * onEnabled()
     */
    @Override
    public void onEnabled(Context context) {
        if (LOG_DEBUG) {
            Log.d(TAG, "onEnabled");
        }
    }

    /**
     * onDisabled()
     */
    @Override
    public void onDisabled(Context context) {
        if (LOG_DEBUG) {
            Log.d(TAG, "onDisabled");
        }
    }

    private Intent getServiceIntent(Context context, RequestType type) {
        Intent intent = new Intent(context, DriveModeService.class);
        intent.putExtra(DriveModeService.EXTRA_REQUEST_TYPE, type);
        return intent;
    }

    private Intent getServiceIntent(Context context, RequestType type, Intent fwdIntent) {
        Intent intent = getServiceIntent(context, type);
        intent.putExtras(fwdIntent);
        return intent;
    }
}
