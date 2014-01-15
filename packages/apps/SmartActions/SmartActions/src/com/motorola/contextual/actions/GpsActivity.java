/*
 * @(#)GpsActivity.java
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
 * This class allows the user to select the GPS state to be set as part of Rule Activation.
 * <code><pre>
 * CLASS:
 *     Extends BinaryDialogActivity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select On/Off for GPS.
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

public class GpsActivity extends BinaryDialogActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setItems(getString(R.string.on), getString(R.string.off));
        mTitle = getString(R.string.gps);
        mIconId = R.drawable.ic_dialog_gps;
        mActionKey = new Gps().getActionKey();
        mActionString = getString(R.string.gps);

        // Do a show Dialog, only when the activity is first created, don't do it for
        // orientation changes
        if (savedInstanceState == null) {
            super.showDialog();
        }
    }
}
