package com.motorola.contextual.pickers.conditions.calendar;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;

/**
 * This class extends the CommandHandler for handing cancel command
 *
 * @author wkh346
 *
 */
public class CalendarEventCancelHandler extends CommandHandler implements
        CalendarEventSensorConstants {

    private static final String TAG = CalendarEventCancelHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        String status = FAILURE;
        if (intent != null
                && ALL_CONFIGS.equals(intent.getStringExtra(EXTRA_CONFIG))) {
            removeAllConfigs(context);
            status = SUCCESS;
        } else if (intent != null && intent.getAction() != null
                && intent.hasExtra(EXTRA_REQUEST_ID)
                && intent.hasExtra(EXTRA_CONFIG)) {
            String config = CalendarEventUtils.validateConfig(intent
                    .getStringExtra(EXTRA_CONFIG));
            if (config != null) {
                removeMonitoredConfig(context, intent,
                        KEY_CALENDAR_EVENTS_CONFIGS);
                if (CalendarEventUtils.canRemoveConfigFromDb(context, config)) {
                    startServiceForRemovingConfig(context, config);
                } else {
                    // In this case calendar service was not started so
                    // explicitly ask for refresh event aware
                    CalendarEventUtils
                            .notifyServiceForSchedulingEventAware(context);
                }
                sendCancelResponse(context, intent);
                status = SUCCESS;
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "executeCommand returning with status " + status);
        }
        return status;
    }

    /**
     * This method constructs and broadcasts the
     * {@link Constants#CANCEL_RESPONSE} intent
     *
     * @param context
     *            - the application's context
     * @param intent
     *            - the {@link Constants#CANCEL_RESPONSE} intent
     */
    private void sendCancelResponse(Context context, Intent intent) {
        Intent cancelResponseIntent = new Intent(
                ACTION_CONDITION_PUBLISHER_EVENT);
        cancelResponseIntent.putExtra(EXTRA_EVENT_TYPE, CANCEL_RESPONSE);
        cancelResponseIntent.putExtra(EXTRA_PUB_KEY, intent.getAction());
        cancelResponseIntent.putExtra(EXTRA_RESPONSE_ID,
                intent.getStringExtra(EXTRA_REQUEST_ID));
        cancelResponseIntent.putExtra(EXTRA_STATUS, SUCCESS);
        String config = intent.getStringExtra(EXTRA_CONFIG);
        cancelResponseIntent.putExtra(EXTRA_CONFIG, config);
        context.sendBroadcast(cancelResponseIntent,
                PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Log.i(TAG, "sendCancelResponse cancel response send for config "
                    + config);
        }
    }

    /**
     * This method removes all configs from persistence
     *
     * @param context
     *            - application's context
     */
    private void removeAllConfigs(Context context) {
        List<String> configsList = Persistence.retrieveValuesAsList(context,
                KEY_CALENDAR_EVENTS_CONFIGS);
        for (String config : configsList) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_CONFIG, config);
            if (removeMonitoredConfig(context, intent,
                    KEY_CALENDAR_EVENTS_CONFIGS)) {
                if (LOG_INFO) {
                    Log.i(TAG, "removeAllConfigs unregistering observer");
                }
                unregisterReceiver(context, STATE_MONITOR);
            }
        }
        startServiceForRemovingConfig(context, ALL_CONFIGS);
    }

    /**
     * This method starts the CalendarEventService for removing the
     * configuration for which cancel command is received
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the configuration String
     */
    private void startServiceForRemovingConfig(Context context, String config) {
        if (LOG_INFO) {
            Log.i(TAG,
                    "startServiceForRemovingConfig starting service for removing config "
                            + config);
        }
        Intent serviceIntent = new Intent(ACTION_REMOVE_CONFIG);
        serviceIntent.putExtra(EXTRA_CONFIG, config);
        serviceIntent.setClass(context, CalendarEventService.class);
        context.startService(serviceIntent);
    }

}
