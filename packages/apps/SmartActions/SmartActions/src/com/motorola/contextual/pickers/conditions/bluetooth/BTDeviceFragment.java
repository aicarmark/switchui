/*
 * @(#)BtConnectionChooserFragment.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a bluetooth device condition chooser.
 * <code><pre>
 * CLASS:
 *     Extends PickerFragment
 *
 * RESPONSIBILITIES:
 *     Presents a list a Bluetooth devices for user selection
 *
 * COLLABORATORS:
 *      WifiConnectionChooserActivity.java - Launches this chooser fragment and collects results
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class BTDeviceFragment extends PickerFragment implements
        OnMultiChoiceClickListener, OnClickListener, Constants,  BTConstants {

    private static final String TAG = BTDeviceFragment.class
                                      .getSimpleName();

    private String mPromptText;
    private ListView mListView;
    private List<ListItem> mItems = null;

    private BluetoothAdapter mBtAdapter;

    private ArrayList<String> mNewDevicesArray = new ArrayList<String>();
    private ArrayList<String> mNewDevicesAddressArray = new ArrayList<String>();

    private final List<Map<String, Object>> mNewDevicesListForAdapter = new ArrayList<Map<String, Object>>();

    private final static String PROMPT_ARG = "PROMPT_ARG";
    
    public static BTDeviceFragment newInstance(final String prompt) {
    	Bundle args = new Bundle();
    	args.putString(PROMPT_ARG, prompt);
    	BTDeviceFragment f = new BTDeviceFragment();
		f.setArguments(args);
		return f;
    }
    
    /**
     * All subclasses of Fragment must include a public empty constructor. The framework
     * will often re-instantiate a fragment class when needed, in particular during state
     * restore, and needs to be able to find this constructor to instantiate it. If the
     * empty constructor is not available, a runtime exception will occur in some cases
     * during state restore.
     */
    public BTDeviceFragment() {
    }

    /**
     * Constructor to set the app picker fragment.
     *
     * @param prompt prompt text to display at the top
     */
    public BTDeviceFragment(final String prompt) {
        mPromptText = prompt;
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

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Populate our available device list
        extractPairedDevices();

        // Populate the item list with the bluetooth devices scanned
        mItems = new ArrayList<ListItem>(mNewDevicesArray.size());
        for (int i = 0; i < mNewDevicesArray.size(); ++i) {
            mItems.add( new ListItem(-1, mNewDevicesArray.get(i), null,
                                     ListItem.typeONE, null, null));
        }

        // Add the "Any Bluetooth" entry as the first item
        // TODO - "Any device" functionality currently unsupported in the intent, code removed for now
        //mItems.add(ANY_CHOICE_INDEX, new ListItem(-1, getResources().getString(R.string.any_bluetooth_device),
        //           null, ListItem.typeONE, null, null));

        // Add the item to discover other devices
        mItems.add(new ListItem(R.drawable.ic_add, getResources().getString(R.string.btconnection_discovery),
                   null, ListItem.typeTHREE, null,
                   new View.OnClickListener() {
                        public void onClick(final View v) {
                            doDiscovery();
                        }
                   }));

        final Picker picker = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(mPromptText))
            .setMultiChoiceItems(mItems, null, BTDeviceFragment.this)
            .setPositiveButton(R.string.iam_done, BTDeviceFragment.this)
            .create();

        final View view = picker.getView();
        mListView = (ListView) view.findViewById(R.id.list);

        // Check the intent to see if we're in Edit Mode
        // If Edit Mode then restore the correct checked items
        final Intent incomingIntent = mHostActivity.getIntent();

        final int numItems = mItems.size();
        if (incomingIntent != null) {
            String config = incomingIntent.getStringExtra(EXTRA_CONFIG);
            SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            config = profileConfig.getValue(BT_NAME);
            if (config != null) {
                for (int index = 0; index < numItems; index++) {
                    if (mItems.get(index) != null) {
                        String addr = getMacAddressFromDeviceName((String)mItems.get(index).mLabel);
                        if (addr != null && config.contains(addr)) {
                            mListView.setItemChecked(index, true);
                        }
                    }
                }
            }
        }

        return picker.getView();
    }

    // Required by OnMultiChoiceClickListener interface
    public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
        // TODO - "Any device" functionality currently unsupported in the intent, code removed for now
        //if (which == ANY_CHOICE_INDEX) {
        //    // Enable/disable all other list options based on the "Any Bluetooth" option
        //    for (int i = 1; i < mListView.getCount() - 1; ++i) {
        //        if (mListView.getItemAtPosition(i) != null)
        //            ((ListItem)mListView.getItemAtPosition(i)).mEnabled = !isChecked;
        //    }
        //    // Requires a list refresh
        //    mListView.invalidate();
        //}
    }

    // Required by OnClickListener interface
    public void onClick(final DialogInterface dialog, final int which) {
        final List<Pair<String,String>> returnItems = new ArrayList<Pair<String,String>>();

        final long[] ids = mListView.getCheckItemIds();

        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            // NB - we always skip the last item (as it isn't a proper checkbox)
            for (int i = 0; i < ids.length; ++i) {
                // Return a list of all the items that were checked
                // Return values are pairs of Strings in the form of (deviceName, deviceAddress)
                // TODO - "Any device" functionality currently unsupported in the intent, removed for now
                //if (ids[i] == ANY_CHOICE_INDEX) {
                //    Pair<String,String> pair = new Pair<String,String>((String)mItems.get((int)ids[i]).mLabel,"");
                //    returnItems.add(pair);
                //    break;
                //} else
                {
                    final int id = (int)ids[i];
                    if ((id < mNewDevicesAddressArray.size()) && mItems.get(id) != null && mNewDevicesAddressArray.get(id) != null) {
                        final Pair<String,String> pair = new Pair<String,String>((String)mItems.get(id).mLabel,mNewDevicesAddressArray.get(id));
                        returnItems.add(pair);
                    }
                }
            }

            mHostActivity.onReturn(returnItems, this);
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
        default:
            break;
        }
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
                if (LOG_INFO)
                    Log.i(TAG, "extractPairedDevices - out of loop : " + name);
            }
        }
        return mNewDevicesArray.size();
    }

    /**
     * Method to get the MAC address from the device name
     *
     * @param deviceName
     * @return corresponding MAC address
     */
    private final String getMacAddressFromDeviceName(final String deviceName) {
        if (deviceName == null)
            return null;

        int size = mNewDevicesArray.size();
        for (int index = 0; index < size; index++) {
            if (deviceName.equals(mNewDevicesArray.get(index))) {
                return (mNewDevicesAddressArray.get(index));
            }
        }
        return null;
    }


    /**
     * Start device discovery with the BluetoothAdapter
     */
    private final void doDiscovery() {
        if (LOG_DEBUG) Log.d(TAG, "doDiscovery()");

        final Intent btIntent = new Intent(Intent.ACTION_MAIN);
        btIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        btIntent.setComponent(new ComponentName(SETTINGS_PACKAGE_NAME,
                                                SETTINGS_COMPONENT_NAME));
        startActivity(btIntent);
    }

}