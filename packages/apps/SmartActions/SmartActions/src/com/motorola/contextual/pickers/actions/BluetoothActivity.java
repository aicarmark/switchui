/*
 * @(#)BluetoothActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/09  NA                  Initial version
 *
 */

package com.motorola.contextual.pickers.actions;

import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.actions.Bluetooth;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select the Bluetooth setting to be set as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends BinaryDialogActivity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select On/Off for Bluetooth.
 *     The base class takes care of sending the intent containing the setting to
 *     Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class BluetoothActivity extends BinaryDialogActivity {

    private static final String TAG = TAG_PREFIX + BluetoothActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (LOG_INFO) Log.i(TAG, "onCreate called");

        setItems(getString(R.string.bluetooth_on), getString(R.string.bluetooth_off));
        setItemIcons(R.drawable.ic_bluetooth_w, R.drawable.ic_bluetooth_off_w);
        mActionBarTitle = getString(R.string.bluetooth_title);
        mTitle = getString(R.string.bluetooth_prompt);
        mActionKey = new Bluetooth().getActionKey();
        mActionString = getString(R.string.bluetooth);

        super.onCreate(savedInstanceState);
    }
}
