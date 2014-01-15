/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/17 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This activity presents a ringtone chooser.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - Fragment display and transition activity
 *
 * RESPONSIBILITIES:
 *  Present a ringtone chooser that plays the current selection.
 *
 * COLLABORATORS:
 *  RingtoneChooserFragment.java - Presents a UI for the user to choose a ringtone
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class RingtoneChooserActivity extends MultiScreenPickerActivity {
    protected static final String TAG = RingtoneChooserActivity.class.getSimpleName();

    /**
     * Creates activity initial state.
     *
     * @param savedInstanceState Bundle from previous instance; else null
     * @see com.motorola.contextual.pickers.MultiScreenPickerActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.ringtone_title));
        
        Log.d(TAG, "onCreate");
        // check if fragment already created
        if (getFragmentManager().findFragmentByTag(getString(R.string.ringtone_question)) == null) {
            Log.d(TAG, "fragment is null");
            launchNextFragment(RingtoneChooserFragment.newInstance(), R.string.ringtone_question, true);
        } else {
            Log.d(TAG, "fragment not null");
        }
    }
    
    /**
     * Returns values from fragments.
     * Passes a fragment reference and its return value to container Activity.
     *
     * @param fromFragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    @Override
    public void onReturn(final Object returnValue, final PickerFragment fromFragment) {
        if (fromFragment == null) {
            Log.w(TAG, "null return fragment");
        } else if (returnValue == null) {
            Log.w(TAG, "null return value");
        } else if (fromFragment instanceof RingtoneChooserFragment) {
            if (returnValue instanceof Intent) {
                final Intent resultIntent = (Intent) returnValue;
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Log.e(TAG, fromFragment.getClass().getSimpleName()
                        + " did not return expected instanceof Intent");
            }
        } else {
            Log.i(TAG, "Ignoring return value from Fragment "
                    + fromFragment.getClass().getSimpleName());
        }
    }
}
