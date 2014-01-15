package com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor;

import com.motorola.contextual.smartprofile.CommandInvokerIntentService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * This class extends {@link BroadcastReceiver} and provides the functionality
 * for handling {@link WifiManager#NETWORK_STATE_CHANGED_ACTION} intent
 *
 * @author wkh346
 *
 */
public class WiFiConnectionReceiver extends BroadcastReceiver implements
        WiFiNetworksRuleConstants {

    /**
     * Tag for logging
     */
    private static final String TAG = WiFiConnectionReceiver.class
            .getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOG_INFO) {
            Log.i(TAG, "onReceive intent " + intent.toUri(0));
        }
        String action = intent.getAction();
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)
                || (SA_CORE_INIT_COMPLETE.equals(action) && FALSE.equals(intent
                        .getStringExtra(EXTRA_DATA_CLEARED)))) {
            Intent service = new Intent(WIFI_CONNECTION_PUBLISHER_KEY);
            service.putExtra(EXTRA_COMMAND, NOTIFY_REQUEST);
            service.setClass(context, CommandInvokerIntentService.class);
            context.startService(service);
        }
    }

}
