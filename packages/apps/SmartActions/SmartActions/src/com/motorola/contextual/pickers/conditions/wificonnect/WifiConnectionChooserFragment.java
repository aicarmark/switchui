/*
 * @(#)WifiConnectionChooserFragment.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionUtils;
import com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor.WiFiConnectionWithAddress;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a wifi connection condition chooser.
 * <code><pre>
 * CLASS:
 *     Extends PickerFragment
 *
 * RESPONSIBILITIES:
 *     Presents a list a WiFi connections for user selection
 *
 * COLLABORATORS:
 *      WifiConnectionChooserActivity.java - Launches this chooser fragment and collects results
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class WifiConnectionChooserFragment extends PickerFragment implements
        OnMultiChoiceClickListener, OnClickListener, Constants, WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionWithAddress.class
                                      .getSimpleName();

    // The index of the "Any..." choice item
    private final int ANY_CHOICE_INDEX = 0;

    private String mPromptText;
    private ListView mListView;
    private List<ListItem> mItems = null;

    /**
     * mWiFiManager - WiFi adapter used for scanning the nearby networks
     */
    private WifiManager mWiFiManager;

    /**
     * mNewNetworksArray - Array list to hold Scanned networks for rules.
     */
    private final ArrayList<String> mNewNetworksArray = new ArrayList<String>();

    /**
     * mInfoSelected - Array list to hold the information of the networks
     * selected from the list view. The information is used in constructing the
     * rules once the user saves the configuration.
     */
    private ArrayList<String> mInfoSelected = new ArrayList<String>();

    /**
     * mNewNetworksListForAdapter - List of maps for the Simple Adapter
     */
    private final List<Map<String, Object>> mNewNetworksListForAdapter = new ArrayList<Map<String, Object>>();

    private final static String PROMPT_ARG = "PROMPT_ARG";

    public static WifiConnectionChooserFragment newInstance(final String prompt) {
        Bundle args = new Bundle();
        args.putString(PROMPT_ARG, prompt);
        WifiConnectionChooserFragment f = new WifiConnectionChooserFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mPromptText = getArguments().getString(PROMPT_ARG);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {

        // Get the local WiFi service
        mWiFiManager = (WifiManager) this.getActivity().getSystemService(Context.WIFI_SERVICE);
        // Populate our available wi-fi list
        extractScannedNetworks();

        // Populate the item list with the networks scanned
        mItems = new ArrayList<ListItem>(mNewNetworksArray.size());
        for (int i = 0; i < mNewNetworksArray.size(); ++i) {
            mItems.add( new ListItem(-1, mNewNetworksArray.get(i), null,
                                     ListItem.typeONE, null, null) );
        }

        // Add the "Any Wifi network" entry as the first item
        mItems.add(ANY_CHOICE_INDEX, new ListItem(-1, getResources().getString(R.string.any_wifi_network),
                   null, ListItem.typeONE, null, null));

        // Add the item to discover other devices
        mItems.add(new ListItem(R.drawable.ic_add, getResources().getString(R.string.wificonnection_discovery),
                   null, ListItem.typeTHREE, null,
                   new View.OnClickListener() {
                        public void onClick(final View v) {
                            doDiscovery();
                        }
                   }));

        final Picker picker = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(mPromptText))
            .setMultiChoiceItems(mItems, null, WifiConnectionChooserFragment.this)
            .setPositiveButton(R.string.iam_done, WifiConnectionChooserFragment.this)
            .create();

        final View view = picker.getView();
        mListView = (ListView) view.findViewById(R.id.list);

        // Handle edit mode
        // Check the intent to see if we're in Edit Mode
        // If Edit Mode then restore the correct checked items
        final Intent incomingIntent = mHostActivity.getIntent();
        final int numItems = mItems.size();
        if (incomingIntent != null) {
            final String config = incomingIntent.getStringExtra(EXTRA_CONFIG);
            if (config != null) {
                mInfoSelected = WiFiConnectionUtils.getListOfNetworkIds(config);
                for (String ssid : mInfoSelected) {
                    // "Any network" intent does not use the item label
                    // Check for it specifically
                    if (ssid.equals(ANY_WIFI_NETWORK)) {
                        mListView.setItemChecked(ANY_CHOICE_INDEX, true);
                        handleAnyNetworkClicked(true);
                    } else {
                        for (int index = 0; index < numItems; index++) {
                            if (mItems.get(index) != null
                                    && ssid.equals(mItems.get(index).mLabel)) {
                                mListView.setItemChecked(index, true);
                            }
                        }
                    }
                }
                if (LOG_INFO) {
                    Log.i(TAG, "onCreate incoming config " + config);
                }
            }
        }
        return picker.getView();
    }

    // Required by OnMultiChoiceClickListener interface
    public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
        if (which == ANY_CHOICE_INDEX) {
            handleAnyNetworkClicked(isChecked);
        }
    }

    // Required by OnClickListener interface
    public void onClick(final DialogInterface dialog, final int which) {
        final List<String> returnItems = new ArrayList<String>();

        final long[] ids = mListView.getCheckItemIds();

        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            for (int i = 0; i < ids.length; ++i) {
                // If this is the "Any Wifi" result
                // then we don't need the rest
                if (ids[i] == ANY_CHOICE_INDEX){
                    returnItems.add(ANY_WIFI_NETWORK);//not adding label, but string constant
                    break;
                }
                // Return a list of all the items that were checked
                returnItems.add((String)mItems.get((int)ids[i]).mLabel);
            }

            mHostActivity.onReturn(returnItems, this);
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
        default:
            break;
        }
    }

    private void handleAnyNetworkClicked(final boolean isChecked) {
        // Enable/disable all other list options based on the "Any Wifi" option
        for (int i = 1; i < mListView.getCount() - 1; ++i) {
            if(mListView.getItemAtPosition(i) != null)
                ((ListItem)mListView.getItemAtPosition(i)).mEnabled = !isChecked;
        }
        // Requires a list refresh
        mListView.invalidateViews();
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
            return mNewNetworksArray.size();
        }

        // If there are scanned networks, add each one to the ArrayAdapter. If
        // configuredNetworks is null, error will be shown in the above "if"
        // block and we should not reach this point
        if (configuredNetworks.size() > 0) {

            // Get last scan results for the secure/non-secure icon to be
            // associated with the item in the list

            for (final WifiConfiguration network : configuredNetworks) {
                if (network == null || network.SSID == null) {
                    continue;
                }
                network.SSID = network.SSID.replace(QUOTE_STRING, EMPTY_STRING);
                if (!mNewNetworksArray.contains(network.SSID)) {

                    mNewNetworksArray.add(network.SSID);
                    // Construct a map for the SimpleAdapter using network list
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put(PATTERN, network.SSID);
                    map.put(CHECK, network.SSID);

                    mNewNetworksListForAdapter.add(map);

                    if (LOG_INFO) Log.i(TAG, "extractScannedNetworks-in-loop : " + network.SSID);
                }
                if (LOG_INFO) Log.i(TAG, "extractScannedNetworks-out-of-loop : " + network.SSID);
            }
        }
        return mNewNetworksArray.size();
    }

    /**
     * Start network discovery with the WiFi Service
     */
    private final void doDiscovery() {
        if (LOG_DEBUG) Log.d(TAG, "doDiscovery()");
        final Intent wifiIntent = new Intent(Intent.ACTION_MAIN);
        wifiIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        wifiIntent.setComponent(new ComponentName(SETTINGS_PACKAGE_NAME,
                                SETTINGS_COMPONENT_NAME));
        startActivity(wifiIntent);
    }

}


