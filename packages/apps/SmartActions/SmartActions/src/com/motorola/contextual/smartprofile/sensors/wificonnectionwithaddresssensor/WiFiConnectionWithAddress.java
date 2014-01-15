/*
 * @(#)WiFiConnectionWithAddress.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2010/11/26 NA                Initial version
 * a18491        2011/3/29  IKINTNETAPP-155   Req to use Managed Networks
 * wkh346        2011/08/18 NA                Added functionality for Any Wifi Network
 *
 */
package com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.app.ActionBar;
import android.app.Fragment;
import android.view.MenuItem;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/**
 * This class displays an option to scan for the near by WiFi networks to the
 * user. When the user opts to scan, a WiFi scan shall be initiated and a list
 * of networks available from the scan results shall be displayed to the user.
 * The user selects the WiFi networks of his preference from the list and
 * constructs a rule. When any of the selected WiFi networks is connected to the
 * phone, the rule kicks in. When all the selected networks are disconnected,
 * the rule is reset.
 *
 * <CODE><PRE>
 *
 * CLASS:
 * 		Implements Constants
 *      Implements View.OnClickListener for buttons                                             ,
 *      Implements SimpleAdapter.ViewBinder for SimpleAdapter of the ListView
 *      Implements WiFiNetworksRuleConstants
 *      Implements AdapterView.OnItemClickListener
 *
 * RESPONSIBILITIES:
 * This class displays an option to scan for the near by WiFi networks to the user.
 * When the user opts to scan, a WiFi scan shall be initiated and a list of networks
 * available from the scan results shall be displayed to the user. The user selects the WiFi
 * networks of his preference from the list and constructs a rule. When any of the selected
 * WiFi networks is connected to the phone, the rule kicks in. When all the selected networks are
 * disconnected, the rule is reset.
 * Note : Separation of UI from Rule construction may not be needed in this case, since SSID
 * is used for the rules construction which is UI independent.
 * TODO : New requirements to be implemented
 * 1. Any network selection option support - the requirement not confirmed.
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class WiFiConnectionWithAddress extends Activity implements
        View.OnClickListener, SimpleAdapter.ViewBinder,
        WiFiNetworksRuleConstants, AdapterView.OnItemClickListener {

    private static final String TAG = WiFiConnectionWithAddress.class
                                      .getSimpleName();

    /**
     * mNewNetworksArrayAdapter - Simple Adapter for the list view to display
     * WiFi networks of the scan result.
     */
    private SimpleAdapter mNewNetworksArrayAdapter;

    /**
     * mWiFiManager - WiFi adapter used for scanning the nearby networks
     */
    private WifiManager mWiFiManager;

    /**
     * mNewNetworksArray - Array list to hold Scanned networks for rules.
     */
    private ArrayList<String> mNewNetworksArray = new ArrayList<String>();

    /**
     * mInfoSelected - Array list to hold the information of the networks
     * selected from the list view. The information is used in constructing the
     * rules once the user saves the configuration.
     */
    private ArrayList<String> mInfoSelected = new ArrayList<String>();

    /**
     * mNewNetworksListView - List view to show the list of networks of the scan
     * result
     */
    private ListView mNewNetworksListView = null;

    /**
     * mNewNetworksListForAdapter - List of maps for the Simple Adapter
     */
    private List<Map<String, Object>> mNewNetworksListForAdapter = null;

    /**
     * mAnyWifiNetwork - String for Any Wi-Fi Network read from resources
     */
    private String mAnyWifiNetwork = null;
    private boolean mDisableActionBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_network_list);
        mAnyWifiNetwork = getResources().getString(R.string.any_wifi_network);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.wificonnectionwithaddress);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);

        // Handle edit mode
        String config = getIntent().getStringExtra(EXTRA_CONFIG);
        if (config != null) {
            mInfoSelected = WiFiConnectionUtils.getListOfNetworkIds(config);
            for (String ssid : mInfoSelected) {
                if (ssid.equals(ANY_WIFI_NETWORK)) {
                    mNewNetworksArray.add(mAnyWifiNetwork);
                } else {
                    mNewNetworksArray.add(ssid);
                }
            }
            if (LOG_INFO) {
                Log.i(TAG, "onCreate incoming config " + config);
            }
        }

        // Get the local WiFi service
        mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Fill the Simple adapter
        mNewNetworksArrayAdapter = prepareSimpleAdapterForListView();

        // Find and set up the ListView for newly scanned networks
        mNewNetworksListView = (ListView) findViewById(R.id.new_networks);
        mNewNetworksListView.setAdapter(mNewNetworksArrayAdapter);
        mNewNetworksListView.setOnItemClickListener(this);

        if (extractScannedNetworks() == 0) {
            showErrorMessage(getString(R.string.wifi_error_message));
        } else {
            mNewNetworksListView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LOG_INFO) Log.i(TAG, "onSaveInstanceState");
        mDisableActionBar = true;
    }

    /**
     * onOptionsItemSelected() handles the back press of icon in the ICS action
     * bar.
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
            if (LOG_INFO)
                Log.i(TAG, "OK button clicked");
            sendResults();
            finish();
            result = true;
            break;
        case R.id.edit_cancel:
            if (LOG_INFO)
                Log.i(TAG, "Cancel button clicked");
            result = true;
            setResult(RESULT_CANCELED);
            finish();
            break;
        }
        return result;
    }

    /**
     * Method for enabling and disabling save button in Action Bar
     *
     * @param enableSaveButton
     *            - true if save button shall be enabled, false otherwise
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if (enableSaveButton) {
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        } else {
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        }
        // Add menu items from fragment
    	Fragment fragment = EditFragment.newInstance(editFragmentOption, true);
        getFragmentManager().beginTransaction()
        .replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    /**
     * This method displays the error message to the user whenever there are no
     * nearby networks in the list mNewNetworksArray
     *
     * @param errorMsg
     */
    private final void showErrorMessage(String errorMsg) {
        LinearLayout errRl;
        TextView errTextView = (TextView) findViewById(R.id.failmessage_text);

        if (errTextView != null) {

            if (LOG_INFO)
                Log.i(TAG, "showErrorMessage");
            errRl = (LinearLayout) errTextView.getParent();
            errRl.setVisibility(View.VISIBLE);
            errTextView.setVisibility(View.VISIBLE);
            mNewNetworksListView.setVisibility(View.GONE);
            errTextView.setText(errorMsg);

        }
        setupActionBarItemsVisibility(false);
    }

    /**
     * This method clears the lists in case if WiFi is disabled or no results
     * from scan
     */
    private final void clearListsIfNoWiFi() {
        for (int index = 0; index < mNewNetworksArray.size(); index++) {
            if (!mInfoSelected.contains(mNewNetworksArray.get(index))
                    && !mNewNetworksArray.get(index).equals(mAnyWifiNetwork)) {

                if (LOG_INFO)
                    Log.i(TAG,
                          "clearListsIfNoWiFi "
                          + mNewNetworksArray.get(index));

                if (index < mNewNetworksListForAdapter.size()) {
                    if (LOG_INFO)
                        Log.i(TAG,
                              "clearListsIfNoWiFi - mNewNetworksListForAdapter: "
                              + mNewNetworksListForAdapter.get(index)
                              .toString());
                    mNewNetworksListForAdapter.remove(index);
                }
                mNewNetworksArray.remove(index);
            }
        }

    }

    /**
     * If the array holding the list of managed networks is empty show error
     * message
     */
    private final void showErrorIfNoManagedNetworks() {

        // If the array is empty show error message
        if (mNewNetworksArray.size() == 0) {
            showErrorMessage(getString(R.string.wifi_error_message));
        } else {
            mNewNetworksListView.setVisibility(View.VISIBLE);
            TextView errTextView = (TextView) findViewById(R.id.failmessage_text);

            if (errTextView != null) {
                errTextView.setVisibility(View.GONE);
            }
        }

        // Refresh the List View with the latest scan results.
        mNewNetworksArrayAdapter.notifyDataSetChanged();
        mNewNetworksListView.invalidateViews();
    }

    /**
     * This method gets the scanned networks using the WiFi service. If the
     * network is already found in mNewNetworksArray, may be as part of edit
     * functionality, it will not be added again. The list is also invalidated
     * to reflect the changes
     *
     * @return number of the nearby networks in mNewNetworksArray
     */
    private final int extractScannedNetworks() {

        List<WifiConfiguration> configuredNetworks = null;

        if ((mWiFiManager == null)
                || ((mWiFiManager != null) && ((configuredNetworks = mWiFiManager
                                                .getConfiguredNetworks()) == null))
                || ((configuredNetworks != null) && (configuredNetworks.size() == 0))) {
            clearListsIfNoWiFi();
            showErrorIfNoManagedNetworks();
            return mNewNetworksArray.size();
        }

        // If there are scanned networks, add each one to the ArrayAdapter. If
        // configuredNetworks is null, error will be shown in the above "if"
        // block and we should not reach this point
        if (configuredNetworks.size() > 0) {

            clearListsIfNoWiFi();

            // Get last scan results for the secure/non-secure icon to be
            // associated with the item in the list

            for (WifiConfiguration network : configuredNetworks) {
                if (network.SSID != null && !network.SSID.isEmpty()) {
                    network.SSID = network.SSID.replace(QUOTE_STRING,
                            EMPTY_STRING);
                    if (!mNewNetworksArray.contains(network.SSID)) {
                        mNewNetworksArray.add(network.SSID);
                        // Construct a map for the SimpleAdapter using network
                        // list
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put(PATTERN, network.SSID);
                        map.put(CHECK, network.SSID);
                        mNewNetworksListForAdapter.add(map);
                        if (LOG_INFO)
                            Log.i(TAG, "extractScannedNetworks-in-loop : "
                                    + network.SSID);
                    }
                    // Refresh the List View with the latest scan results.
                    mNewNetworksArrayAdapter.notifyDataSetChanged();
                    mNewNetworksListView.invalidateViews();
                }
                if (LOG_INFO)
                    Log.i(TAG, "extractScannedNetworks-out-of-loop : "
                            + network.SSID);
            }

        }
        return mNewNetworksArray.size();
    }

    /**
     * Method implemented from View.OnClickListener This is used to handle the
     * selection/de-selection of the checkbox
     */
    public final void onClick(View view) {

        if (LOG_DEBUG)
            Log.d(TAG, "onClick :" + view);

        if ((view.getId() == R.id.placelist_checkbox)
                && (view instanceof CheckBox)) {

            if (LOG_DEBUG)
                Log.d(TAG, "onClick : checkbox :" + view);

            // Retrieve the saved Tag information for each checkbox. The
            // information associated is the corresponding network name.
            CheckBox checkBox = (CheckBox) view;
            String info = (String) ((View) checkBox.getParent()).getTag();

            // Save the information, i.e. the network name, associated with the
            // checkbox when checked and remove when unchecked.
            if (checkBox.isChecked()) {
                if (info.equals(mAnyWifiNetwork)) {
                    mInfoSelected.add(ANY_WIFI_NETWORK);
                } else {
                    mInfoSelected.add(info);
                }
                checkBox.setChecked(true);
                setupActionBarItemsVisibility(true);
            } else {
                if (info.equals(mAnyWifiNetwork)) {
                    mInfoSelected.remove(ANY_WIFI_NETWORK);
                } else {
                    mInfoSelected.remove(info);
                }
                checkBox.setChecked(false);
                if (mInfoSelected.isEmpty()) {
                    setupActionBarItemsVisibility(false);
                } else {
                    setupActionBarItemsVisibility(true);
                }
            }
            if (info.equals(mAnyWifiNetwork)) {
                mNewNetworksArrayAdapter.notifyDataSetChanged();
                mNewNetworksListView.invalidateViews();
            }

            if (LOG_INFO)
                Log.i(TAG, "onClick : checkbox :" + view + ":" + info);
        }
    }

    public final boolean setViewValue(final View view, final Object obj,
                                      final String str) {
        int id = view.getId();
        if (LOG_DEBUG) {
            Log.d(TAG, "setViewValue : " + id);
        }
        if (id == R.id.placelist_checkbox && view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            setCheckBoxValue(checkBox, str);
            return true;
        } else if (id == R.id.placelist_textview && view instanceof TextView) {
            TextView textView = (TextView) view;
            setTextViewValue(textView, str);
            return true;
        }
        return false;
    }

    /**
     * Method for setting the text view enabled or disabled
     *
     * @param textview
     *            - The text view instance reference
     * @param str
     *            - The string associated with the text view
     */
    void setTextViewValue(TextView textView, String str) {
        textView.setText(str);
        textView.setEnabled(true);
        if (mInfoSelected.contains(ANY_WIFI_NETWORK)) {
            if (!str.equals(mAnyWifiNetwork)) {
                textView.setEnabled(false);
            }
        }
    }

    /**
     * Method for setting the check box enabled or disabled. Also it associates
     * the network name with the check box which is used when a check box is
     * clicked
     *
     * @param checkBox
     *            - The check box instance reference
     * @param str
     *            - The string associated with the check box
     */
    void setCheckBoxValue(CheckBox checkBox, String str) {
        checkBox.setOnClickListener(this);
        checkBox.setVisibility(View.VISIBLE);
        checkBox.setEnabled(true);
        LinearLayout parent = (LinearLayout) checkBox.getParent();
        parent.setTag(str);
        if (mInfoSelected.contains(ANY_WIFI_NETWORK)) {
            if (str.equals(mAnyWifiNetwork)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
                checkBox.setEnabled(false);
            }
        } else {
            if (mInfoSelected.contains(str)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOG_DEBUG)
            Log.d(TAG, "onResume");
        mDisableActionBar = false;

        // Get the local WiFi service
        mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (extractScannedNetworks() == 0) {
            showErrorMessage(getString(R.string.wifi_error_message));
        } else {
            mNewNetworksListView.setVisibility(View.VISIBLE);
            TextView errTextView = (TextView) findViewById(R.id.failmessage_text);

            if (errTextView != null) {

                errTextView.setVisibility(View.GONE);

            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clear the lists
        mNewNetworksArray.clear();
        mInfoSelected.clear();
        mNewNetworksListForAdapter.clear();
        mDisableActionBar = true;

    }

    /**
     * Method to construct the rule and send the required parameters to the
     * Rules Builder.
     *
     */
    private final void sendResults() {
        // This check is added to fix IKJBREL1-486
        if (!mInfoSelected.isEmpty()) {
            Intent intent = new Intent();
            String config = WiFiConnectionUtils.getConfigString(mInfoSelected);
            String description = WiFiConnectionUtils.getDescriptionString(this,
                    config);
            intent.putExtra(EXTRA_CONFIG, config);
            intent.putExtra(EXTRA_DESCRIPTION, description);
            setResult(RESULT_OK, intent);
            if (LOG_INFO) {
                Log.i(TAG, "sendResults config = " + config + " description = "
                        + description);
            }
        } else {
            setResult(RESULT_CANCELED);
        }
    }

    /**
     * Start network discover with the WiFi Service
     */
    private final void doDiscovery() {

        if (LOG_DEBUG)
            Log.d(TAG, "doDiscovery()");
        Intent wifiIntent = new Intent(Intent.ACTION_MAIN);
        wifiIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        wifiIntent.setComponent(new ComponentName(SETTINGS_PACKAGE_NAME,
                                SETTINGS_COMPONENT_NAME));
        startActivity(wifiIntent);

    }

    /**
     * This method sets up the list view adapter and the list view. The adapter
     * is filled with the information from the network name holder lists.
     *
     * @return prepared SimpleAdapter
     */
    private final SimpleAdapter prepareSimpleAdapterForListView() {

        if (!mNewNetworksArray.contains(mAnyWifiNetwork)) {
            mNewNetworksArray.add(0, mAnyWifiNetwork);
        }

        mNewNetworksListForAdapter = new ArrayList<Map<String, Object>>();

        // Construct a map for the SimpleAdapter using network list
        for (int i = 0; i < mNewNetworksArray.size(); i++) {

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(PATTERN, mNewNetworksArray.get(i));
            map.put(CHECK, mNewNetworksArray.get(i));
            mNewNetworksListForAdapter.add(map);
        }

        String[] from = { PATTERN, CHECK };
        int[] to = { R.id.placelist_textview, R.id.placelist_checkbox };

        // Set up List View SimpleAdapter
        SimpleAdapter simpleAdapter = new SimpleAdapter(
            getApplicationContext(), mNewNetworksListForAdapter,
            R.layout.wifi_network_name, from, to);
        simpleAdapter.setViewBinder(WiFiConnectionWithAddress.this);

        return (simpleAdapter);
    }

    /**
     * handles the click events in the list This is used to handle the
     * selection/de-selection of the items in the list
     */
    public final void onItemClick(AdapterView<?> arg0, View view, int arg2,
                                  long arg3) {
        if (LOG_DEBUG)
            Log.d(TAG, "onListItemClick ");
        CheckBox checkBox = (CheckBox) view
                            .findViewById(R.id.placelist_checkbox);
        String info = (String) ((View) checkBox.getParent()).getTag();
        if (checkBox.isEnabled()) {
            if (!checkBox.isChecked()) {
                if (info.equals(mAnyWifiNetwork)) {
                    mInfoSelected.add(ANY_WIFI_NETWORK);
                } else {
                    mInfoSelected.add(info);
                }
                if (LOG_DEBUG)
                    Log.d(TAG, "Checked " + info);
                checkBox.setChecked(true);
                setupActionBarItemsVisibility(true);
            } else {
                if (info.equals(mAnyWifiNetwork)) {
                    mInfoSelected.remove(ANY_WIFI_NETWORK);
                } else {
                    mInfoSelected.remove(info);
                }
                if (LOG_DEBUG)
                    Log.d(TAG, "UnChecked " + info);
                checkBox.setChecked(false);
                if (mInfoSelected.isEmpty()) {
                    setupActionBarItemsVisibility(false);
                } else {
                    setupActionBarItemsVisibility(true);
                }
            }
        }
        if (info.equals(mAnyWifiNetwork)) {
            mNewNetworksArrayAdapter.notifyDataSetChanged();
            mNewNetworksListView.invalidateViews();
        }

    }

}
