package com.motorola.contextual.pickers.conditions.wificonnect;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiNetworksRuleConstants;

/**
 * This class extends {@link CommandHandler} and provides the functionality for
 * handling Cancel command
 *
 * @author wkh346
 *
 */
public class WiFiConnectionCancelHandler extends CommandHandler implements
        WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionCancelHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        String result = FAILURE;
        if (intent != null
                && ALL_CONFIGS.equals(intent.getStringExtra(EXTRA_CONFIG))) {
            removeAllConfigs(context);
            result = SUCCESS;
        } else if (intent != null && intent.getAction() != null
                && intent.hasExtra(EXTRA_REQUEST_ID)
                && intent.hasExtra(EXTRA_CONFIG)) {
            if (removeMonitoredConfig(context, intent,
                    KEY_WIFI_CONNECTION_CONFIGS)) {
                if (LOG_INFO) {
                    Log.i(TAG, "executeCommand unregistering receiver");
                }
                unregisterReceiver(context, WIFI_CONNECTION_STATE_MONITOR);
            }
            sendCancelResponse(context, intent);
            result = SUCCESS;
        }
        return result;
    }

    /**
     * This method broadcasts the {@link Constants#CANCEL_RESPONSE} intent
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
        context.sendBroadcast(cancelResponseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
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
                KEY_WIFI_CONNECTION_CONFIGS);
        for (String config : configsList) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_CONFIG, config);
            if (removeMonitoredConfig(context, intent,
                    KEY_WIFI_CONNECTION_CONFIGS)) {
                if (LOG_INFO) {
                    Log.i(TAG, "removeAllConfigs unregistering receiver");
                }
                unregisterReceiver(context, WIFI_CONNECTION_STATE_MONITOR);
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "removeAllConfigs removed all configs for "
                    + KEY_WIFI_CONNECTION_CONFIGS);
        }
    }

}
