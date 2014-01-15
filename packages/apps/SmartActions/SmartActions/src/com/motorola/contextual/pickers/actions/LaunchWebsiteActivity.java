/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/07/02 Smart Actions 2.1  Initial Version
 */

package com.motorola.contextual.pickers.actions;

import android.content.Intent;
import android.os.Bundle;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This activity presents launch web site picker fragment.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - activity base class for pickers
 *
 * RESPONSIBILITIES:
 *  Launches the web site picker fragment
 *  and sends the chosen web site back to the rule builder activity.
 *
 * COLLABORATORS:
 *  LaunchWebsitePickerFragment - the fragment that presents the web site picker UI.
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class LaunchWebsiteActivity extends MultiScreenPickerActivity implements Constants{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.open_website_title));

        Intent intent = getIntent();
        Intent inputConfigs = ActionHelper.getConfigIntent(intent.getStringExtra(Constants.EXTRA_CONFIG));
        Intent outputConfigs = new Intent();

        // Launch the first fragment.
        // check if fragment already created
        if (getFragmentManager().findFragmentByTag(getString(R.string.open_website)) == null) {
            launchNextFragment(LaunchWebsitePickerFragment.newInstance(inputConfigs, outputConfigs), R.string.open_website, true);
        }
    }

    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        if(returnValue != null)
            setResult(RESULT_OK, (Intent)returnValue);

        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }
}
