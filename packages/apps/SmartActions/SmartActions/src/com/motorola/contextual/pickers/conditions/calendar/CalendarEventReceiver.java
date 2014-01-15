/*
 * @(#)CalendarEventReceiver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/08/22 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class implements the broadcast receiver for handling the incoming
 * Intents
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends BroadcastReceiver
 *      Implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * This class is responsible for handling the incoming Intents
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class CalendarEventReceiver extends BroadcastReceiver implements
    CalendarEventSensorConstants {

    /**
     * TAG for logging the messages
     */
    private static final String TAG = "CalendarEventReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOG_INFO) {
            Log.i(TAG, "onReceive incoming intent = " + intent.toUri(0));
        }
        String intentAction = intent.getAction();
        if (intentAction != null) {
            if (intentAction.equals(ACTION_SCHEDULE_EVENT_AWARE)) {
                // Start service for scheduling event aware functionality
                CalendarEventUtils
                        .notifyServiceForSchedulingEventAware(context);
            } else if (intentAction.equals(SA_CORE_INIT_COMPLETE)
                    && FALSE.equals(intent.getStringExtra(EXTRA_DATA_CLEARED))) {
                // Notify CalendarEventService for handling core init complete
                // when data cleared is false
                CalendarEventUtils.notifyServiceForCoreInitComplete(context);
            } else if (intentAction.equals(CALENDAR_EVENTS_IMPORT_ACTION)) {
                // Start service for importing the rules
                CalendarEventUtils.notifyServiceToImportRules(context, intent);
            }
        }
    }
}
