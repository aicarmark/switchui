package com.motorola.contextual.pickers.conditions.calendar;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;

/**
 * This class extends CommandHandler for handling refresh command
 *
 * @author wkh346
 *
 */
public class CalendarEventRefreshHandler extends CommandHandler implements
        CalendarEventSensorConstants {

    private static final String TAG = CalendarEventRefreshHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        String status = FAILURE;
        if (intent != null && intent.getAction() != null
                && intent.hasExtra(EXTRA_REQUEST_ID)
                && intent.hasExtra(EXTRA_CONFIG)) {
            String config = CalendarEventUtils.validateConfig(intent
                    .getStringExtra(EXTRA_CONFIG));
            if (config != null) {
                Intent serviceIntent = new Intent(ACTION_REFRESH_CONFIG);
                serviceIntent.putExtra(EXTRA_CONFIG, config);
                serviceIntent.putExtra(EXTRA_REQUEST_ID,
                        intent.getStringExtra(EXTRA_REQUEST_ID));
                serviceIntent.setClass(context, CalendarEventService.class);
                context.startService(serviceIntent);
                status = SUCCESS;
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "executeCommand returning with status " + status);
        }
        return status;
    }

}
