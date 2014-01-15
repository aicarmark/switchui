package com.motorola.contextual.pickers.conditions.wificonnect;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionUtils;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiNetworksRuleConstants;

/**
 * This class extends {@link CommandHandler} and provides the functionality for
 * handling Subscribe command and broadcasting
 * {@link Constants#SUBSCRIBE_RESPONSE} intent
 *
 * @author wkh346
 *
 */
public class WiFiConnectionSubscribeHandler extends CommandHandler implements
        WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionSubscribeHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null
                && intent.hasExtra(EXTRA_REQUEST_ID)
                && intent.hasExtra(EXTRA_CONFIG)) {
            if (saveMonitoredConfig(context, intent,
                    KEY_WIFI_CONNECTION_CONFIGS)) {
                if (LOG_INFO) {
                    Log.i(TAG, "executeCommand registering receiver");
                }
                registerReceiver(context, WIFI_CONNECTION_STATE_MONITOR);
            }
            sendSubscribeResponse(context, intent);
            return SUCCESS;
        }
        return FAILURE;
    }

    /**
     * This method broadcasts {@link Constants#SUBSCRIBE_RESPONSE} intent for
     * current configuration
     *
     * @param context
     *            - the application's context
     * @param intent
     *            - the {@link Constants#SUBSCRIBE_RESPONSE} intent
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
        String state = WiFiConnectionUtils.getConfigState(context, config);
        subscribeResponseIntent.putExtra(EXTRA_STATE, state);
        context.sendBroadcast(subscribeResponseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Log.i(TAG, "sendSubscribeResponse subscribe response send for "
                    + intent.getStringExtra(EXTRA_CONFIG) + " with state "
                    + state);
        }
    }

}
