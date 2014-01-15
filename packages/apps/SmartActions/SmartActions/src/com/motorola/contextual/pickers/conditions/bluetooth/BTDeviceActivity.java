/*
 * @(#)BtConnectionChooserActivity.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * tqrb48       2012/06/12                    Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.bluetooth;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;


interface BTConstants {

    String BT_CONFIG_PERSISTENCE = "BTConfig";

    String BT_PUB_KEY = "com.motorola.contextual.smartprofile.btconnectionwithaddresssensor";

    String BT_CONNECTED_LIST_PERSISTENCE = "BTConnectedList";

    String EXTRA_BT_ACTION = "com.motorola.contextual.smartrules.intent.extra.BT_ACTION";

    String BT_CONFIG_STRING = "BluetoothDevice=";

    String BT_NAME = "BluetoothDevice";

    String OLD_BT_DEVICES_NAME_ADDRESS_SEPARATOR = "/";

    String OLD_BT_DEVICES_ADDRESS_SEPARATOR = "\\[";

    String OLD_BT_DEVICES_ADDRESS_STRING_SEPARATOR = "[";

    String PATTERN = "pattern";

    String CHECK = "check";

    String SETTINGS_PACKAGE_NAME = "com.android.settings";

    String SETTINGS_COMPONENT_NAME = "com.android.settings.Settings$BluetoothSettingsActivity";

    String BT_DEVICES_DESC_SUFIX = "";

    String BT_DEVICES_NAME = "BluetoothDevices";

    String BT_CONFIG_VERSION = Constants.CONFIG_VERSION;

    String BT_VERSION = "1.0";

    int NUM_DEVICE_NAME_SHOWN = 2;
}

/**
 * This activity presents a bluetooth device condition chooser.
 * <code><pre>
 * CLASS:
 *     Extends MultiSelectDialogActivity
 *
 * RESPONSIBILITIES:
 *     launches a startActivity with options of Bluetooth connections
 *         incoming call.
 *
 * COLLABORATORS:
 *      BtConnectionChooserFragment.java - Presents the UI for the user to choose an option
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class BTDeviceActivity extends MultiScreenPickerActivity implements
    Constants, BTConstants {

    /**
     * mInfoSelected - Array list to hold the information of the devices
     * selected from the list view The information is used in constructing the
     * rules once the user saves the configuration.
     */
    ArrayList<String> mInfoSelected = new ArrayList<String>();

    private static final String TAG = BTDeviceActivity.class
                                      .getSimpleName();

    /**
     * Creates activity initial state.
     *
     * @param savedInstanceState Bundle from previous instance; else null
     * @see com.motorola.contextual.pickers.MultiScreenPickerActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.btconnectionwithaddress_title));
    }

    @Override
    public void onResume() {
        super.onResume();
        // List contents are often substantially modified while paused,
        // relaunch fragment on resume to rebuild.
        this.launchNextFragment(BTDeviceFragment.newInstance(getString(R.string.btconnection_prompt)),
                R.string.btconnectionwithaddress, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        // Return values are pairs of Strings in the form of (deviceName, deviceAddress)
        List<Pair<String,String>> returnItems = null;

        if (returnValue != null && returnValue instanceof List<?> ) {
            returnItems = (List<Pair<String,String>>)returnValue;
            constructReturnIntent( returnItems );
        }
        finish();
    }


    /**
     * Configures our final return intent based on the list of BlueTooth device names selected in the picker fragment
     *
     * @param namesBuffer BlueTooth device names
     */
    private final void constructReturnIntent(List<Pair<String,String>> namesBuffer) {
        String orSplitString = BLANK_SPC + getString(R.string.or) + BLANK_SPC;
        StringBuilder descBuffer = new StringBuilder();
        StringBuilder allAddressBuf = new StringBuilder();

        if (namesBuffer.size() <= NUM_DEVICE_NAME_SHOWN) {
            for (int index = 0; index < namesBuffer.size(); index++) {
                // Update rule description with the network names being
                // separated by or
                Pair<String, String> namePair = namesBuffer.get(index);
                if (namePair != null && namePair.first != null) {
                    descBuffer.append(namePair.first).append(
                            index != (namesBuffer.size() - 1) ? orSplitString : "");
                }
            }
        } else {
            Pair<String, String> namePair = namesBuffer.get(0);
            if (namePair != null && namePair.first != null) {
                descBuffer.append(namePair.first).append(orSplitString)
                .append(namesBuffer.size() - 1).append(BLANK_SPC)
                .append(getString(R.string.more));
            }
        }
        for (int index = 0; index < namesBuffer.size(); index++) {
            Pair<String, String> namePair = namesBuffer.get(index);
            if (namePair != null && namePair.second != null) {
                String deviceAddress = namePair.second;
                // The set of all device addresses concatenated are needed for rule
                // editing functionality
                // This information will be sent to the Rules Builder when the user
                // first creates the rule
                // and will be retrieved when the user edits the rule then onwards.
                allAddressBuf.append(deviceAddress).append(
                        index != (namesBuffer.size() - 1) ? OR_STRING : "");
            }
        }
        populateIntentFields(descBuffer, allAddressBuf);
    }

    /**
     * Populates the fields of Intent returned to Condition Builder module.
     *
     * @param descBuffer
     * @param allAddressBuf
     */
    private final void populateIntentFields(StringBuilder descBuffer,
                                            StringBuilder allAddressBuf) {

        descBuffer.append(BT_DEVICES_DESC_SUFIX);
        String newDescription = descBuffer.toString();

        Intent returnIntent = new Intent();
        String config = BT_CONFIG_STRING+LEFT_PAREN+allAddressBuf.toString()+RIGHT_PAREN;
        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        profileConfig.addNameValuePair(BT_CONFIG_VERSION, BT_VERSION);

        returnIntent.putExtra(EXTRA_CONFIG, profileConfig.getConfigString());
        returnIntent.putExtra(EXTRA_DESCRIPTION, newDescription);


        if (LOG_INFO) {
            Log.i(TAG, " Config : " + profileConfig.getConfigString());
        }

        setResult(RESULT_OK, returnIntent);
    }

}
