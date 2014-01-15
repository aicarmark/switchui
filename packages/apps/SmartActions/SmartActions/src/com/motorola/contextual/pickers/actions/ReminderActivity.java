/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/06/19 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.SetNotification;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This class implements the Reminder (formerly Notification) guided picker.
 *
 * The UI design calls for a multi-screen guided flow.  The individual screens
 * are implemented as separate Fragments.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity
 *
 * RESPONSIBILITIES:
 * Provides the container Activity for the Fragments that implement the Reminder
 * guided picker.
 *
 * COLLABORATORS:
 *  ReminderActivity.java - container Activity for the Reminder picker
 *  ReminderAlertFragment.java - alert style chooser for the Reminder picker
 *  ReminderMessageFragment.java - message composer for the Reminder picker
 *  WhenFragment.java - chooser for start or end trigger condition
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ReminderActivity extends MultiScreenPickerActivity implements Constants, WhenFragment.WhenCallback {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.notif_title));

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            mInputConfigs = ActionHelper.getConfigIntent(intent.getStringExtra(Constants.EXTRA_CONFIG));
            if (mInputConfigs != null) {
                mInputConfigs.putExtra(EXTRA_RULE_ENDS, intent.getBooleanExtra(EXTRA_RULE_ENDS, false));
            }
            mOutputConfigs = new Intent();
    
            // Launch the first fragment.
            launchNextFragment(ReminderAlertFragment.newInstance(mInputConfigs, mOutputConfigs), R.string.reminder_title, true);
        }
    }

    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        
        if (returnValue == null) {
            //Just go back to the previous fragment without committing any changes.
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
        }

        Intent configs = (Intent)returnValue;
        
        if (fromFragment instanceof ReminderAlertFragment) {
            launchNextFragment(ReminderMessageFragment.newInstance(mInputConfigs, configs), R.string.reminder_title, false);
        } else if (fromFragment instanceof ReminderMessageFragment) {
            
            launchNextFragment(WhenFragment.newInstance(mInputConfigs, configs, R.string.reminder_when_prompt, R.string.iam_done),
                                        R.string.reminder_title, false);
        } else {
            // Returning from WhenFragment.
            // Intent configs = (Intent)returnValue;
            Intent output = new Intent();

            String message = configs.getStringExtra(EXTRA_MESSAGE);
            String description = null;
            if (message.length() == 0) {
                description = configs.getStringExtra(EXTRA_DESCRIPTION);
            } else {
                description = DOUBLE_QUOTE + message + DOUBLE_QUOTE;
            }

            output.putExtra(EXTRA_CONFIG, SetNotification.getConfig(message,
                            configs.getBooleanExtra(EXTRA_VIBRATE, false),
                            configs.getBooleanExtra(EXTRA_SOUND, false),
                            configs.getBooleanExtra(EXTRA_RULE_ENDS, false)));
            output.putExtra(EXTRA_RULE_ENDS, configs.getBooleanExtra(EXTRA_RULE_ENDS, false));
            output.putExtra(EXTRA_DESCRIPTION, description);

            setResult(RESULT_OK, output);
            finish();
        }
    }

    public void handleWhenFragment(Fragment fragment, Object returnValue) {
        // Reminder Activity doesn't do anything in this callback.
    }
}
