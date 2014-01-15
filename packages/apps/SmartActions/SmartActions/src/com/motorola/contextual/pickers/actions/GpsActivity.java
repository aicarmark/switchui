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

package com.motorola.contextual.pickers.actions;

import android.os.Bundle;

import com.motorola.contextual.actions.Gps;
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
 
        setItems(getString(R.string.gps_on), getString(R.string.gps_off));
        setItemIcons(R.drawable.ic_gps_w, R.drawable.ic_gps_off_w);
        mActionBarTitle = getString(R.string.gps_title);
        mTitle = getString(R.string.gps_prompt);
        mActionKey = new Gps().getActionKey();
        mActionString = getString(R.string.gps);

        super.onCreate(savedInstanceState);
    }
}


