/*
 * @(#)WifiConnectionChooserActivity.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * tqrb48        2012/06/12                   Initial version
 * XPR643        2012/08/10 Smart Actions 2.2 New architecture for data I/O
 */

package com.motorola.contextual.pickers.conditions.wificonnect;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionUtils;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

// Interface copied fully from previous WiFi Connection picker
interface WiFiNetworksRuleConstants {

    public static final String WIFI_CONNECTION_WITH_ADDRESS_URI_TO_FIRE_STRING = "#Intent;action=android.intent.action.EDIT;"
            + "component=com.motorola.contextual.smartrules/com.motorola.contextual.pickers.conditions.WifiConnectionChooserActivity;"
            + "S.CURRENT_MODE=";

    public static final String RULE1 = "(#sensor;name=com.motorola.virtualsensor.WiFiSensor;p0=";
    public static final String RULE2 = "(#sensor;name=com.motorola.virtualsensor.WiFiSensor;p0=none;end)";
    public static final String END = ";end)";

    public static final String WIFI_CONNECTION_WITH_ADDRESS_VIRTUAL_SENSOR_STRING = "com.motorola.contextual.WiFiNetwork";
    public static final String WIFI_NETWORK_CONNECTED_VIRTUAL_SENSOR_STRING = "Connected";
    public static final String WIFI_NETWORK_DISCONNECTED_VIRTUAL_SENSOR_STRING = "Disconnected";

    public static final String WIFI_NETWORK_NAME = "WiFiNetworks";
    public static final String WIFI_NETWORKS_DESC_PREFIX = "When the phone is connected to ";
    public static final String WIFI_NETWORKS_DESC_SUFIX = "";
    public static final String WIFI_NETWORKS_SECURITY_SEPARATOR = "[";
    public static final String WIFI_NETWORKS_NAMES_SEPARATOR = "|||";

    public static final String PATTERN = "pattern";
    public static final String CHECK = "check";
    public static final String REG_EX_TO_IGNORE = "[!@#$%^&*;:\"\', ?/\\()~`|{}<>+=-]";
    public static final String EMPTY_STRING = "";
    public static final String SECURE_NETWORK = "secure";
    public static final String NON_SECURE_NETWORK = "nonsecure";
    public static final String SECURITY_WEP = "WEP";
    public static final String SECURITY_PSK = "PSK";
    public static final String SECURITY_EAP = "EAP";
    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final String SETTINGS_COMPONENT_NAME = "com.android.settings.Settings$WifiSettingsActivity";
    public static final String STAR_STRING = ".*";
    public static final String QUOTE_STRING = "\"";
    public static final String SENSOR_ANY_WIFI = "AnyWiFiSensor";
    public static final String WIFI_SENSOR_NAME_END_STRING = ";p0=false;end)";
    public static final String ANY_WIFI_TRUE_CLAUSE_LOGIC = "(#sensor;name=com.motorola.virtualsensor.WiFiSensor;p0=none;end)";
    public static final String ANY_WIFI_FALSE_CLAUSE_LOGIC = "";
    public static final String ANY_WIFI_NETWORK = "ANY_WIFI_NETWORK";
}

/**
 * This activity presents a wifi connection condition chooser.
 * <code><pre>
 * CLASS:
 *     Extends MultiSelectDialogActivity
 *
 * RESPONSIBILITIES:
 *     Does a startActivity with options of WiFi networks to detect
 *         incoming call.
 *
 * COLLABORATORS:
 *      WifiConnectionChooserFragment.java - Presents the UI for the user to choose an option
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class WifiConnectionChooserActivity extends MultiScreenPickerActivity implements
    Constants, WiFiNetworksRuleConstants {

    protected static final String TAG = WifiConnectionChooserActivity.class.getSimpleName();

    /**
     * Creates activity initial state.
     *
     * @param savedInstanceState Bundle from previous instance; else null
     * @see com.motorola.contextual.pickers.MultiScreenPickerActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.wifi_connectivity_status_title));

        if (getFragmentManager().findFragmentByTag(getString(R.string.wificonnection_prompt)) == null) {
            launchNextFragment(WifiConnectionChooserFragment.newInstance(getString(R.string.wificonnection_prompt)),
                R.string.wificonnectionwithaddress, true);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        List<String> returnItems = null;
        if (returnValue != null && returnValue instanceof List<?> )
            returnItems = (List<String>)returnValue;

        constructReturnIntent( returnItems );
        finish();
    }

    /**
     * Configures our final return intent based on the list of WiFi network names selected in the picker fragment
     *
     * @param namesBuffer Wifi network names
     */
    private final void constructReturnIntent(List<String> namesBuffer) {

        // This might be conditional later
        boolean useAnyNetwork;
        if (!namesBuffer.isEmpty()){//We need to check for empty before trying to get. There is a decently difficult to reproduce race condition that causes a breakage here as well (IKJBREL1-3148).
            useAnyNetwork = namesBuffer.get(0).equals(ANY_WIFI_NETWORK);
        } else {
            Log.e(TAG, "namesBuffer was empty. Failing gracefully"); //We need to bail here. We don't actually know what the user wants at this point.
            return;
        }

        Intent intent = new Intent();
        String config = WiFiConnectionUtils.getConfigString((ArrayList<String>)namesBuffer);
        String description = WiFiConnectionUtils.getDescriptionString(this,
                config);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        setResult(RESULT_OK, intent);
        if (LOG_INFO) {
            Log.i(TAG, "sendResults config = " + config + " description = "
                    + description);
        }
    }
}
