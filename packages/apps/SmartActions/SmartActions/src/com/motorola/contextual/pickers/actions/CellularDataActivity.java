/**
 * @(#)CellularDataAct.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21693       2011/02/20  NA                  Initial version
 *
 */

package com.motorola.contextual.pickers.actions;


import android.content.Intent;
import android.os.Bundle;
import android.content.res.Configuration;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select the Cellular Data setting
 * to be set as part of Rule activation.
 *
 * <code><pre>
 * CLASS:
 *     Extends BinaryDialogActivity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select On/Off for Cellular Data.
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
public class CellularDataActivity extends BinaryDialogActivity {

	private static final String CELLULAR_DATA_ACTION_KEY = "com.motorola.contextual.actions.CellularData";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setItems(new ListItem(R.drawable.ic_cellular_data_w, getString(R.string.toggle_data_on), null, ListItem.typeONE, null, null),
                new ListItem(R.drawable.ic_cellular_data_off_w, getString(R.string.toggle_data_off), null, ListItem.typeONE, null, null));
        mButtonText = getString(R.string.iam_done);
        mActionBarTitle = getString(R.string.toggle_data_title);
        mTitle = getString(R.string.toggle_data_prompt);
        mActionKey = CELLULAR_DATA_ACTION_KEY;
        mActionString = getString(R.string.toggle_data);

        setHelpHTMLFileUrl(this.getClass());

        super.onCreate(savedInstanceState);
    }

    /**
     * On config changes such as screen rotation, keyboard changes and screen size changes, we don't want to lose state
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    /** Returns the state based on the item that was checked
      *
      * @param state
      * @return config string
      */
    @Override
    public final String getConfig(boolean state) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_STATE, state);
        //Delay param to be added to config
        intent.putExtra(EXTRA_DELAY, 0);
        return intent.toUri(0);
    }
}
