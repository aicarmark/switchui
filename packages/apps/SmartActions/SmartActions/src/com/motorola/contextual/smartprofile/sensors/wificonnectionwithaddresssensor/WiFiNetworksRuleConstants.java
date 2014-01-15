package com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor;

import com.motorola.contextual.smartprofile.Constants;

/**
 * This interface contains constants used in context of Wi-Fi connection
 * condition publisher
 *
 * @author wkh346
 *
 */
public interface WiFiNetworksRuleConstants extends Constants {
    public static final String PATTERN = "pattern";
    public static final String CHECK = "check";
    public static final String EMPTY_STRING = "";
    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final String SETTINGS_COMPONENT_NAME = "com.android.settings.Settings$WifiSettingsActivity";
    public static final String QUOTE_STRING = "\"";
    public static final String ANY_WIFI_NETWORK = "ANY_WIFI_NETWORK";
    public static final String CONFIG_DELIMITER = ";";
    public static final String SELECTED_NETWORKS_IDS = "selected_networks_ids";
    public static final String KEY_WIFI_CONNECTION_CONFIGS = "wifi_connection_configs";
    public static final String WIFI_CONNECTION_PUBLISHER_KEY = "com.motorola.contextual.smartprofile.wificonnectionwithaddresssensor";
    public static final String EXTRA_CURRENT_MODE = "CURRENT_MODE";
    public static final String WIFI_CONNECTION_STATE_MONITOR = "com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionStateMonitor";
}
