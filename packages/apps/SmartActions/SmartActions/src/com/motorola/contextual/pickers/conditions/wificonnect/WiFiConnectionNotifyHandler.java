package com.motorola.contextual.pickers.conditions.wificonnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionUtils;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiNetworksRuleConstants;

/**
 * This class extends {@link CommandHandler} and provides functionality for
 * handling {@link Constants#NOTIFY} command
 *
 * @author wkh346
 *
 */
public class WiFiConnectionNotifyHandler extends CommandHandler implements
        WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionNotifyHandler.class
            .getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {
        ArrayList<String> configs = (ArrayList<String>) Persistence
                .retrieveValuesAsList(context, KEY_WIFI_CONNECTION_CONFIGS);
        if (!configs.isEmpty()) {
            HashMap<String, String> configsStatesMap = new HashMap<String, String>();
            for (String config : configs) {
                configsStatesMap.put(config,
                        WiFiConnectionUtils.getConfigState(context, config));
            }
            sendNotifyIntent(context, configsStatesMap);
        }
        return SUCCESS;
    }

    /**
     * This method broadcasts the {@link Constants#NOTIFY} intent for various
     * configs along with their states
     *
     * @param context
     *            - the application's context
     * @param configsStatesMap
     *            - the HashMap with config as key and state as value
     */
    private void sendNotifyIntent(Context context,
            HashMap<String, String> configsStatesMap) {
        Intent notifyIntent = CommandHandler.constructNotification(
                configsStatesMap, WIFI_CONNECTION_PUBLISHER_KEY);
        context.sendBroadcast(notifyIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Set<String> keySet = configsStatesMap.keySet();
            for (String config : keySet) {
                Log.i(TAG, "sendNotifyIntent notify send for config " + config
                        + " with state " + configsStatesMap.get(config));
            }
        }
    }

}
