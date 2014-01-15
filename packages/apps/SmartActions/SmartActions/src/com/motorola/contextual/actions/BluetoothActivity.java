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

package com.motorola.contextual.actions;

import android.os.Bundle;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setItems(getString(R.string.on), getString(R.string.off));
        mTitle = getString(R.string.bluetooth);
        mIconId = R.drawable.ic_dialog_bluetooth;
        mActionKey = new Bluetooth().getActionKey();
        mActionString = getString(R.string.bluetooth);
        if (savedInstanceState == null) {
            super.showDialog();
        }
    }
}
