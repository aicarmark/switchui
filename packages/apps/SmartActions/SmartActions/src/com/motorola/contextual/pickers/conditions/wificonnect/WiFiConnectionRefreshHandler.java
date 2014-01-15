package com.motorola.contextual.pickers.conditions.wificonnect;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionUtils;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiNetworksRuleConstants;

/**
 * This class extends {@link CommandHandler} and provides functionality for
 * handling Refresh command and broadcasts {@link Constants#REFRESH_RESPONSE}
 * intent
 *
 * @author wkh346
 *
 */
public class WiFiConnectionRefreshHandler extends CommandHandler implements
        WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionRefreshHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null
                && intent.hasExtra(EXTRA_REQUEST_ID)
                && intent.hasExtra(EXTRA_CONFIG)) {
            sendRefreshResponse(context, intent);
            return SUCCESS;
        }
        return FAILURE;
    }

    /**
     * This method broadcasts {@link Constants#REFRESH_RESPONSE} intent for a
     * particular configuration
     *
     * @param context
     *            - The application's context
     * @param intent
     *            - the {@link Constants#REFRESH_RESPONSE} intent
     */
    private void sendRefreshResponse(Context context, Intent intent) {
        Intent refreshIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
        refreshIntent.putExtra(EXTRA_EVENT_TYPE, REFRESH_RESPONSE);
        refreshIntent.putExtra(EXTRA_PUB_KEY, WIFI_CONNECTION_PUBLISHER_KEY);
        refreshIntent.putExtra(EXTRA_RESPONSE_ID,
                intent.getStringExtra(EXTRA_REQUEST_ID));
        refreshIntent.putExtra(EXTRA_STATUS, SUCCESS);
        String config = WiFiConnectionUtils.getNewConfigFromOldConfig(context,
                intent.getStringExtra(EXTRA_CONFIG));
        String description = WiFiConnectionUtils.getDescriptionString(context,
                config);
        String state = WiFiConnectionUtils.getConfigState(context, config);
        refreshIntent.putExtra(EXTRA_DESCRIPTION, description);
        refreshIntent.putExtra(EXTRA_STATE, state);
        refreshIntent.putExtra(EXTRA_CONFIG, config);
        context.sendBroadcast(refreshIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Log.i(TAG, "sendRefreshResponse refresh response send for config "
                    + config + " with state " + state + " with description "
                    + description);
        }
    }

}
