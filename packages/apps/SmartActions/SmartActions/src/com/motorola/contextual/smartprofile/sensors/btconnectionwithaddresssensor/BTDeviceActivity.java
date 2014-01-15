/*
 * @(#)BTDeviceActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2010/11/26 NA                Initial version
 * a18491        2011/2/17  NA                Incorporated first set of review
 *                                            comments
 * a18491        2011/3/16  IKINTNETAPP-52    Incorporated new UI requirements
 *
 */
package com.motorola.contextual.smartprofile.sensors.btconnectionwithaddresssensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.motorola.contextual.pickers.conditions.bluetooth.BTDeviceUtil;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;


/**
 * This class displays an option to scan for the near by BT devices to the user.
 * When the user opts to scan, a BT scan shall be initiated and a list of
 * devices available from the scan results shall be displayed to the user. The
 * user selects the BT devices of his preference from the list and returns a
 * "config" composed of selected BT devices.
 * <CODE><PRE>
 *
 * CLASS:
 * 		Implements Constants, BTConstants
 *      Implements View.OnClickListener for buttons                                             ,
 *      Implements SimpleAdapter.ViewBinder for SimpleAdapter of the ListView
 *      Implements AdapterView.OnItemClickListener
 *
 * RESPONSIBILITIES:
 * This class displays an option to scan for the near by BT devices to the user.
 * When the user opts to scan, a BT scan shall be initiated and a list of
 * devices available from the scan results shall be displayed to the user. The
 * user selects the BT devices of his preference from the list and returns a
 * "config" composed of selected BT devices.
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
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
public final class BTDeviceActivity extends Activity implements
    Constants, View.OnClickListener, BTConstants,
    SimpleAdapter.ViewBinder,
    AdapterView.OnItemClickListener {

    private static final String TAG = BTDeviceActivity.class
                                      .getSimpleName();

    /**
     * mNewDevicesArrayAdapter - Simple Adapter for the list view to display BT
     * devices of the scan result.
     */
    private SimpleAdapter mNewDevicesArrayAdapter;

    /**
     * mBtAdapter - BT adapter used for scanning the nearby devices
     */
    private BluetoothAdapter mBtAdapter;

    /**
     * mNewDevicesArray - Array list to hold Scanned devices for rules.
     */
    private ArrayList<String> mNewDevicesArray = new ArrayList<String>();

    /**
     * mNewDevicesAddressArray - Array list to hold the addresses of the scanned
     * devices for rules.
     */
    private ArrayList<String> mNewDevicesAddressArray = new ArrayList<String>();

    /**
     * mInfoSelected - Array list to hold the information of the devices
     * selected from the list view The information is used in constructing the
     * rules once the user saves the configuration.
     */
    private ArrayList<String> mInfoSelected = new ArrayList<String>();

    /**
     * mNewDevicesListView - List view to show the list of devices of the scan
     * result
     */
    private ListView mNewDevicesListView = null;

    /**
     * mListForAdapter - List of maps for the Simple Adapter
     */
    private List<Map<String, Object>> mNewDevicesListForAdapter = null;

    private boolean mDisableActionBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bt_device_list);

        // Get Action Bar
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.btconnectionwithaddress);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Fill the Simple adapter
        mNewDevicesArrayAdapter = prepareSimpleAdapterForListView();
        // Find and set up the ListView for newly discovered devices
        mNewDevicesListView = (ListView) findViewById(R.id.new_devices);
        mNewDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        mNewDevicesListView.setOnItemClickListener(this);

        // Get the paired devices, throw error if no paired devices
        if (extractPairedDevices() == 0) {
            showErrorMessage(getString(R.string.bt_error_message));
        } else {
            mNewDevicesListView.setVisibility(View.VISIBLE);
        }

        // Handle edit mode
        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {

            String configExtra = incomingIntent.getStringExtra(EXTRA_CONFIG);
            if(configExtra != null) {
                extractDeviceNameAndAddress(configExtra);
            }
        }

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(
            BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LOG_DEBUG) Log.d(TAG, "onSaveInstanceState");
        mDisableActionBar = true;
    }

    /**
     * onOptionsItemSelected() handles the key presses in ICS action bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_add_button:
            doDiscovery();
            result = true;
            break;
        case R.id.edit_save:
            if (LOG_DEBUG) Log.d(TAG, "OK button clicked");
            sendResults();
            finish();
            result = true;
            break;
        case R.id.edit_cancel:
            result = true;
            finish();
            break;
        }
        return result;
    }

    /**
     * This method sets up visibility for the action bar items.
     *
     * @param enableSaveButton
     *            - whether save button needs to be enabled
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if (enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        else
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(editFragmentOption, true);
        getFragmentManager().beginTransaction()
        .replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    /**
     * This method displays the error message to the user whenever there are no
     * paired devices in the list mNewDevicesArray
     *
     * @param errorMsg
     */
    private final void showErrorMessage(String errorMsg) {
        LinearLayout errRl;
        TextView errTextView = (TextView) findViewById(R.id.failmessage_text);

        if (errTextView != null) {
            errRl = (LinearLayout) errTextView.getParent();
            errRl.setVisibility(View.VISIBLE);
            errTextView.setVisibility(View.VISIBLE);
            mNewDevicesListView.setVisibility(View.GONE);
            errTextView.setText(errorMsg);

        }
        setupActionBarItemsVisibility(false);
    }

    /**
     * This method gets the bonded devices from the Adapter. If the device is
     * already found in mNewDevicesArray, may be as part of edit functionality,
     * it will not be added again. Even the list is invalidated to reflect the
     * changes
     *
     * @return number of the paired devices in mNewDevicesArray
     */
    private final int extractPairedDevices() {

        Set<BluetoothDevice> pairedDevices = null;

        if ((mBtAdapter == null)
                || (((pairedDevices = mBtAdapter.getBondedDevices()) == null))
                || ((pairedDevices.size() == 0))) {
            return mNewDevicesArray.size();
        }

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
            	String name = device.getName();
                if (!mNewDevicesArray.contains(name)) {
                    mNewDevicesArray.add(name);
                    mNewDevicesAddressArray.add(device.getAddress());
                    if (LOG_DEBUG)
                        Log.d(TAG, "extractPairedDevices : " + name);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put(PATTERN, name);
                    map.put(CHECK, name);
                    mNewDevicesListForAdapter.add(map);
                }
                if (LOG_DEBUG)
                    Log.d(TAG, "extractPairedDevices - out of loop : " + name);
            }
            // Refresh the List View with the latest scan results.
            mNewDevicesArrayAdapter.notifyDataSetChanged();
            mNewDevicesListView.invalidateViews();
        }
        return mNewDevicesArray.size();
    }

    /**
     * This method parses config info and extracts
     * previously configured device name and the address This also updates
     * necessary lists for updating in the list view.
     *
     * @param config - String which holds config info
     */
    private final void extractDeviceNameAndAddress(String config) {
        if(!config.contains(BT_CONFIG_STRING)) return;

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String value = profileConfig.getValue(BT_NAME);
        if(value == null) return;
        config = BTDeviceUtil.trimBraces(value);
        StringTokenizer st = new StringTokenizer(config, OR_STRING);

        if (LOG_DEBUG)
            Log.d(TAG, "extractDeviceNameAndAddress : " + config);

        while (st.hasMoreTokens()) {
            //strList.add(st.nextToken());
            String addr = st.nextToken();
            if(mNewDevicesAddressArray.contains(addr)) {
                mInfoSelected.add(mNewDevicesArray.get(mNewDevicesAddressArray.indexOf(addr)));
            }
        }
        TextView errTextView = (TextView) findViewById(R.id.failmessage_text);
        if (errTextView != null) {
            errTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Method implemented from View.OnClickListener. This is used to handle the
     * selection/de-selection of the checkbox
     */
    public final void onClick(View view) {

        if ((view.getId() == R.id.placelist_checkbox)
                && (view instanceof CheckBox)) {

            // Retrieve the saved Tag information for each checkbox
            // The information associated is the corresponding device name.
            CheckBox checkBox = (CheckBox) view;
            String info = (String) ((View) checkBox.getParent()).getTag();

            // Save the information, i.e. the device name,
            // associated with the checkbox when checked and remove when
            // unchecked.
            if (checkBox.isChecked()) {
                mInfoSelected.add(info);
                checkBox.setChecked(true);
                setupActionBarItemsVisibility(true);
            } else {
                mInfoSelected.remove(info);
                checkBox.setChecked(false);
                if (mInfoSelected.isEmpty()) {
                    setupActionBarItemsVisibility(false);
                } else {
                    setupActionBarItemsVisibility(true);
                }
            }

            if (LOG_DEBUG)
                Log.d(TAG, "onClick : checkbox :" + view + ":" + info);
        }
    }

    /**
     * Method implemented from SimpleAdapter.ViewBinder This is used to
     * associate the device name with the checkbox, which is to be extracted
     * when the checkbox is selected.
     */
    public final boolean setViewValue(final View view, final Object obj,
                                      final String str) {
        int id = view.getId();

        if (id == R.id.placelist_checkbox && view instanceof CheckBox) {

            if (LOG_DEBUG)
                Log.d(TAG, "setViewValue : checkbox : " + obj + ":" + str + ":"
                      + view);

            CheckBox checkBox = (CheckBox) view;

            {
                // Get the checkbox in order to associated value with each.
                checkBox.setOnClickListener(this);
                checkBox.setVisibility(View.VISIBLE);

                // Associate the value, i.e. the device name with each checkbox
                // through
                // parent. This is to be used for the rule creation once the
                // user saves the Precondition.
                LinearLayout parent = (LinearLayout) view.getParent();
                parent.setTag(str);

                if (mInfoSelected.contains(str)) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }
            }

            return true;

        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDisableActionBar = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDisableActionBar = false;
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (extractPairedDevices() == 0) {
            if (LOG_DEBUG)
                Log.d(TAG, "onResume zero devices");
            showErrorMessage(getString(R.string.bt_error_message));
        } else {
            if (LOG_DEBUG)
                Log.d(TAG, "onResume non-zero devices");
            mNewDevicesListView.setVisibility(View.VISIBLE);
            TextView errTextView = (TextView) findViewById(R.id.failmessage_text);

            if (errTextView != null) {
                errTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);

        // Clear the lists
        mNewDevicesArray.clear();
        mNewDevicesAddressArray.clear();
        mInfoSelected.clear();
        mNewDevicesListForAdapter.clear();

    }

    /**
     * Method to get the MAC address from the device name
     *
     * @param deviceName
     * @return corresponding MAC address
     */
    private final String getMacAddressFromDeviceName(final String deviceName) {
        int size = mNewDevicesArray.size();
        for (int index = 0; index < size; index++) {
            if (deviceName.equals(mNewDevicesArray.get(index))) {
                return (mNewDevicesAddressArray.get(index));
            }
        }
        return null;
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


        if (LOG_DEBUG) {
            Log.d(TAG, " Config : " + profileConfig.getConfigString());
        }

        setResult(RESULT_OK, returnIntent);
    }

    /**
     * Method to construct the rule and send the required parameters to the
     * Rules Builder.
     *
     */
    private final void sendResults() {
        String orSplitString = BLANK_SPC + getString(R.string.or) + BLANK_SPC;
        StringBuilder descBuffer = new StringBuilder();
        StringBuilder allAddressBuf = new StringBuilder();

        int size = mInfoSelected.size();
        if (size <= NUM_DEVICE_NAME_SHOWN) {
            for (int index = 0; index < size; index++) {
                // Update rule description with the network names being
                // separated by or
                descBuffer.append(mInfoSelected.get(index)).append(
                    index != (size - 1) ? orSplitString : "");
            }
        } else {
            descBuffer.append(mInfoSelected.get(0)).append(orSplitString)
            .append(size - 1).append(BLANK_SPC)
            .append(getString(R.string.more));
        }
        for (int index = 0; index < size; index++) {
            String deviceAddress = getMacAddressFromDeviceName(mInfoSelected
                                   .get(index));
            // The set of all device addresses concatenated are needed for rule
            // editing functionality
            // This information will be sent to the Rules Builder when the user
            // first creates the rule
            // and will be retrieved when the user edits the rule then onwards.
            allAddressBuf.append(deviceAddress).append(
                index != (size - 1) ? OR_STRING : "");
        }
        populateIntentFields(descBuffer,
                             allAddressBuf);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private final void doDiscovery() {

        if (LOG_DEBUG)
            Log.d(TAG, "doDiscovery()");

        // If the BT is not enabled in the device, let the user know through a
        // dialog.
        // Also allow the user to "Turn On" BT, if he wants.
        Intent btIntent = new Intent(Intent.ACTION_MAIN);
        btIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        btIntent.setComponent(new ComponentName(SETTINGS_PACKAGE_NAME,
                                                SETTINGS_COMPONENT_NAME));
        startActivity(btIntent);

    }

    /**
     * This method sets up the list view adapter and the list view. The adapter
     * is filled with the information from the device name holder lists.
     *
     * @return prepared SimpleAdapter
     */
    private final SimpleAdapter prepareSimpleAdapterForListView() {

        mNewDevicesListForAdapter = new ArrayList<Map<String, Object>>();
        int size = mNewDevicesArray.size();

        // Construct a map for the SimpleAdapter using device list
        for (int i = 0; i < size; i++) {

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(PATTERN, mNewDevicesArray.get(i));
            map.put(CHECK, mNewDevicesArray.get(i));
            mNewDevicesListForAdapter.add(map);

        }
        String[] from = { PATTERN, CHECK };
        int[] to = { R.id.placelist_textview, R.id.placelist_checkbox };

        // Set up List View SimpleAdapter
        SimpleAdapter simpleAdapter = new SimpleAdapter(
            getApplicationContext(), mNewDevicesListForAdapter,
            R.layout.device_name, from, to);
        simpleAdapter.setViewBinder(BTDeviceActivity.this);

        return (simpleAdapter);
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and updates the
     * device list and the UI
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                                         .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                	String name = device.getName();
                    if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {                   	
                        if (!mNewDevicesArray.contains(name)) {
                            // Add to the device list
                            mNewDevicesArray.add(name);
                            mNewDevicesAddressArray.add(device.getAddress());
                            if (LOG_DEBUG)
                                Log.d(TAG,
                                      "onReceive : ACTION_DISCOVERY_FINISHED: "
                                      + name);
                        } else {
                            return;
                        }
                        if (LOG_DEBUG)
                            Log.d(TAG,
                                  "onReceive : BOND_BONDED : "
                                  + name);

                    } else if (intent.getIntExtra(
                    BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_NONE) {
                        if (!mInfoSelected.contains(name)) {
                            // Remove to the device list
                            mNewDevicesArray.remove(name);
                            mNewDevicesAddressArray.remove(device.getAddress());
                            if (LOG_DEBUG)
                                Log.d(TAG,
                                      "onReceive : BOND_NONE : "
                                      + name);
                        }

                    }
                }
                mNewDevicesListForAdapter.clear();
                // Fill the maps for List View adapter with the device names
                // from the discovery
                int size = mNewDevicesArray.size();
                for (int i = 0; i < size; i++) {

                    Map<String, Object> map = new HashMap<String, Object>();

                    if (LOG_DEBUG)
                        Log.d(TAG, "onReceive : ACTION_DISCOVERY_FINISHED: "
                              + mNewDevicesArray.get(i));

                    map.put(PATTERN, mNewDevicesArray.get(i));
                    map.put(CHECK, mNewDevicesArray.get(i));
                    mNewDevicesListForAdapter.add(map);

                }
                if (size != 0) {
                    if (LOG_DEBUG)
                        Log.d(TAG, "onResume non-zero devices");
                    mNewDevicesListView.setVisibility(View.VISIBLE);
                    TextView errTextView = (TextView) findViewById(R.id.failmessage_text);

                    if (errTextView != null) {

                        errTextView.setVisibility(View.GONE);

                    }
                }
                // Refresh the List View with the latest scan results.
                mNewDevicesArrayAdapter.notifyDataSetChanged();
                mNewDevicesListView.invalidateViews();

            }

        }
    };

    /**
     * handles the click events in the list This is used to handle the
     * selection/de-selection of the items in the list
     */
    public final void onItemClick(AdapterView<?> arg0, View view, int arg2,
                                  long arg3) {
        if (LOG_DEBUG)
            Log.d(TAG, "onListItemClick1 ");
        CheckBox checkBox = (CheckBox) view
                            .findViewById(R.id.placelist_checkbox);
        String info = (String) ((View) checkBox.getParent()).getTag();
        if (!checkBox.isChecked()) {
            mInfoSelected.add(info);
            if (LOG_DEBUG)
                Log.d(TAG, "Checked1 " + info);
            checkBox.setChecked(true);
            setupActionBarItemsVisibility(true);
        } else {

            mInfoSelected.remove(info);
            if (LOG_DEBUG)
                Log.d(TAG, "UnChecked1 " + info);
            checkBox.setChecked(false);
            if (mInfoSelected.isEmpty()) {
                setupActionBarItemsVisibility(false);
            } else {
                setupActionBarItemsVisibility(true);
            }
        }

    }
}
