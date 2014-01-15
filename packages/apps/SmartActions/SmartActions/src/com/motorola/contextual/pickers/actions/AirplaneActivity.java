/*
 * @(#)AirplaneActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 *
 */

package com.motorola.contextual.pickers.actions;

import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.actions.Airplane;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select the Airplane mode setting to be set as part of Rule
 * activation. <code><pre>
 * CLASS:
 *     Extends BinaryDialogActivity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select On/Off for Airplane Mode.
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

public class AirplaneActivity extends BinaryDialogActivity {

    private static final String TAG = TAG_PREFIX + AirplaneActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (LOG_INFO) Log.i(TAG, "onCreate called");


        setItems(getString(R.string.ap_on), getString(R.string.ap_off));
        setItemIcons(R.drawable.ic_airplane_w, R.drawable.ic_airplane_off_w);
        mActionBarTitle = getString(R.string.ap_title);
        mTitle = getString(R.string.ap_prompt);
        mActionKey = new Airplane().getActionKey();
        mActionString = getString(R.string.ap);

        super.onCreate(savedInstanceState);
    }
}
