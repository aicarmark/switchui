package com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;

/**
 * This class provides several utility functions which are helpful in context of
 * Wi-Fi connection condition publisher
 *
 * @author wkh346
 *
 */
public class WiFiConnectionUtils implements WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionUtils.class.getSimpleName();

    /**
     * This method returns the description String for configuration passed as an
     * argument
     *
     * @param context
     *            - the application's context
     * @param configString
     *            - the configuration String
     * @return - the description String. null is returned if config is null or
     *         empty
     */
    public static final String getDescriptionString(Context context,
            String configString) {
        String description = null;
        if (context != null && configString != null && !configString.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            ArrayList<String> networksIds = getListOfNetworkIds(configString);
            if (!networksIds.isEmpty()) {
                if (networksIds.contains(ANY_WIFI_NETWORK)) {
                    builder.append(context.getString(R.string.any_wifi_network));
                } else {
                    int size = networksIds.size();
                    if (size == 1) {
                        builder.append(networksIds.get(0));
                    } else {
                        String orString = BLANK_SPC
                                + context.getString(R.string.or) + BLANK_SPC;
                        if (size == 2) {
                            builder.append(networksIds.get(0)).append(orString)
                                    .append(networksIds.get(1));
                        } else {
                            builder.append(networksIds.get(0)).append(orString)
                                    .append(size - 1).append(BLANK_SPC)
                                    .append(context.getString(R.string.more));
                        }
                    }

                }
            }
            description = builder.toString();
        }
        return description;
    }

    /**
     * This method returns the configuration String created from the network
     * SSIDs stored in networksIds parameter
     *
     * @param networksIds
     *            - the list of network SSIDs
     * @return - the configuration String
     */
    public static final String getConfigString(ArrayList<String> networksIds) {
        StringBuilder builder = new StringBuilder();
        builder.append(CONFIG_VERSION).append(BLANK_SPC)
                .append(CURRENT_VERSION).append(CONFIG_DELIMITER);
        if (networksIds != null && !networksIds.isEmpty()) {
            Collections.sort(networksIds);
            builder.append(SELECTED_NETWORKS_IDS).append(BLANK_SPC);
            if (networksIds.contains(ANY_WIFI_NETWORK)) {
                builder.append(ANY_WIFI_NETWORK);
            } else {
                int size = networksIds.size();
                int index = 0;
                builder.append(networksIds.get(index));
                for (index = 1; index < size; index++) {
                    builder.append(OR_STRING).append(networksIds.get(index));
                }
            }
        }
        return builder.toString();
    }

    /**
     * This method returns the list of network SSIDs present in config parameter
     *
     * @param config
     *            - the configuration String
     * @return - the list of network SSIDs
     */
    public static final ArrayList<String> getListOfNetworkIds(String config) {
        ArrayList<String> networksIds = new ArrayList<String>();
        if (config != null && !config.isEmpty()) {
            int start = config.indexOf(CONFIG_DELIMITER);
            if (start != -1) {
                start++;
                String[] networksIdsArr = config
                        .substring(start, config.length()).trim()
                        .substring(SELECTED_NETWORKS_IDS.length()).trim()
                        .split(OR_STRING);
                for (String ssid : networksIdsArr) {
                    networksIds.add(ssid);
                }
            }
        }
        return networksIds;
    }

    /**
     * This method returns the current state of the config
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the configuration String
     * @return - the current state. This will be either {@link Constants#TRUE}
     *         or {@link Constants#FALSE}
     */
    public static final String getConfigState(Context context, String config) {
        String state = FALSE;
        ArrayList<String> networksIds = getListOfNetworkIds(config);
        if (!networksIds.isEmpty()) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (!mWifi.isConnected()) {
                if (LOG_INFO) Log.i(TAG, "ConnectivityManager Wifi Connection is disconnected: ");
            } else {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {               	
                	String networkSSID = wifiInfo.getSSID();
                	if (networkSSID != null && !networkSSID.isEmpty()) {
                        // The documentation says that network ssid may be
                        // returned surrounded by quotes. However such a
                        // scenario is not observed. Anyway adding this for
                        // being consistent with the way the config is created
                        // with ssids
                        networkSSID = networkSSID.replace(QUOTE_STRING,
                                EMPTY_STRING);
                        if (LOG_INFO) {
                            Log.i(TAG, "getConfigState current networkSSID is "
                                    + networkSSID);
                        }
                        if (networksIds.contains(ANY_WIFI_NETWORK)) {
                            state = TRUE;
                        } else {
                            for (String ssid : networksIds) {
                                if (ssid.equals(networkSSID)) {
                                    state = TRUE;
                                    break;
                                }
                            }
                        }
                    } else {
                        if (LOG_INFO) {
                            Log.i(TAG,
                                    "getConfigState networkSSID is null hence state for config "
                                            + config + " is " + FALSE);
                        }
                    }
                } else {
                    if (LOG_INFO) {
                        Log.i(TAG,
                                "getConfigState wifiInfo is null hence state for config "
                                        + config + " is " + FALSE);
                    }
                }
            }
            
        }
        return state;
    }

    /**
     * This method generates the new config from old config
     *
     * @param context
     *            - the application's context
     * @param oldConfig
     *            - the old config {@link Intent#toUri(int)}
     * @return - the new config String
     */
    public static final String getNewConfigFromOldConfig(Context context,
            String oldConfig) {
        if (LOG_INFO) {
            Log.i(TAG, "getNewConfigFromOldConfig oldConfig " + oldConfig);
        }
        String newConfig = oldConfig;
        if (oldConfig != null && oldConfig.contains(INTENT_PREFIX)) {
            // This is old config and if this doesn't get parsed then null shall
            // be returned
            newConfig = null;
            try {
                Intent configIntent = Intent.parseUri(oldConfig, 0);
                if (configIntent != null) {
                    String networksIds = configIntent
                            .getStringExtra(EXTRA_CURRENT_MODE);
                    if (networksIds != null && !networksIds.isEmpty()) {
                        // In older versions the separator is locale dependent
                        String orSplitString = BLANK_SPC
                                + context.getString(R.string.or) + BLANK_SPC;
                        String[] networksIdsArray = networksIds
                                .split(orSplitString);
                        if (networksIdsArray != null) {
                            if (networksIdsArray.length == 1) {
                                // This network id may be any wifi network. This
                                // is locale dependent in older versions
                                if (context.getString(
                                        R.string.any_wifi_network_gb).equals(
                                        networksIdsArray[0])) {
                                    networksIdsArray[0] = ANY_WIFI_NETWORK;
                                }
                            }
                            newConfig = getConfigString(new ArrayList<String>(
                                    Arrays.asList(networksIdsArray)));
                        }
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG,
                        "getNewConfigFromOldConfig error while parsing oldConfig "
                                + oldConfig);
                exception.printStackTrace();
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "getNewConfigFromOldConfig newConfig " + newConfig);
        }
        return newConfig;
    }

}
