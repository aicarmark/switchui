/*
 * Copyright (C) 2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * MXDN83        05/12/2012 Smart Actions 2.1 Created file
 */

package com.motorola.contextual.pickers.actions;

import android.content.Intent;
import android.os.Bundle;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.LaunchApp;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This activity presents launch app picker fragment.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - activity base class for pickers
 *
 * RESPONSIBILITIES:
 *  Launches the app picker fragment
 *  and sends the chosen app back to the rule builder activity.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class LaunchAppPickerActivity extends MultiScreenPickerActivity implements Constants{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setActionBarTitle(getString(R.string.launch_an_application_title));
        
        // check if fragment already created
        if (getFragmentManager().findFragmentByTag(getString(R.string.launch_an_application)) == null) {
            this.launchNextFragment(LaunchAppPickerFragment.newInstance(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_LAUNCHER, 
                    null,
                    getString(R.string.launch_an_application_prompt), 
                    ListItem.typeONE
                ),
                R.string.launch_an_application, true);
        }
    }

    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG, LaunchApp.getConfig(null,
                ((Intent)((ListItem)returnValue).mMode).getComponent().flattenToString()));
        intent.putExtra(EXTRA_DESCRIPTION, ((ListItem)returnValue).mLabel);
        intent.putExtra(EXTRA_RULE_ENDS, false);

        setResult(RESULT_OK, intent);
        finish();
    }

}
