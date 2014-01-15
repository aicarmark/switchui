package com.motorola.contextual.pickers.conditions.calendar;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;

/**
 * This class extends CommandHandler for handling subscribe command
 *
 * @author wkh346
 *
 */
public class CalendarEventSubscribeHandler extends CommandHandler implements
        CalendarEventSensorConstants {

    private static final String TAG = CalendarEventSubscribeHandler.class
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
                if (saveMonitoredConfig(context, intent,
                        KEY_CALENDAR_EVENTS_CONFIGS)) {
                    if (LOG_INFO) {
                        Log.i(TAG, "executeCommand registering observer");
                    }
                    registerReceiver(context, STATE_MONITOR);
                }
                startServiceForStoringConfig(context, config);
                sendSubscribeResponse(context, intent);
                status = SUCCESS;
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "executeCommand returning with status " + status);
        }
        return status;
    }

    /**
     * This method constructs and broadcasts subscribe_response intent
     *
     * @param context
     *            - the application's context
     * @param intent
     *            - the subscribe intent
     */
    private void sendSubscribeResponse(Context context, Intent intent) {
        Intent subscribeResponseIntent = new Intent(
                ACTION_CONDITION_PUBLISHER_EVENT);
        subscribeResponseIntent.putExtra(EXTRA_EVENT_TYPE, SUBSCRIBE_RESPONSE);
        subscribeResponseIntent.putExtra(EXTRA_PUB_KEY, intent.getAction());
        subscribeResponseIntent.putExtra(EXTRA_RESPONSE_ID,
                intent.getStringExtra(EXTRA_REQUEST_ID));
        subscribeResponseIntent.putExtra(EXTRA_STATUS, SUCCESS);
        String config = intent.getStringExtra(EXTRA_CONFIG);
        subscribeResponseIntent.putExtra(EXTRA_CONFIG, config);
        String state = CalendarEventUtils.getConfigState(context, config);
        subscribeResponseIntent.putExtra(EXTRA_STATE, state);
        context.sendBroadcast(subscribeResponseIntent,
                PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Log.i(TAG,
                    "sendSubscribeResponse subscribe response send for config "
                            + config + " with state " + state);
        }
    }

    /**
     * This method starts CalendarEventService for storing the configuration in
     * database
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the configuration String
     */
    private void startServiceForStoringConfig(Context context, String config) {
        Intent serviceIntent = new Intent(ACTION_STORE_CONFIG);
        serviceIntent.putExtra(EXTRA_CONFIG, config);
        serviceIntent.setClass(context, CalendarEventService.class);
        context.startService(serviceIntent);
    }

}
