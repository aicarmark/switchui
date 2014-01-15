/*
 * Copyright (C) 2010-2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 08, 2012   MXDN83       Created file
 **********************************************************
 */

package  com.motorola.contextual.pickers.conditions.charging;

import java.util.Hashtable;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;

/**
 * Constants for Charging
 *
 */
interface ChargingConstants {

    String NOT_CHARGING = "Charging=NotCharging;Version=1.0";

    String USB_CHARGING = "Charging=USBCharging;Version=1.0";

    String AC_CHARGING = "Charging=ACCharging;Version=1.0";

    String USB_AC_CHARGING = "Charging=USB/ACCharging;Version=1.0";

    String CHARGING_PERSISTENCE = "com.motorola.contextual.smartprofile.charging.persistence";

    String CHARGING_PUB_KEY = "com.motorola.contextual.smartprofile.charging";

    String CHARGING_STATE_MONITOR = "com.motorola.contextual.pickers.conditions.charging.ChargingStateMonitor";

    String NEW_CHARGING_CONFIG_PREFIX = "Charging=";

    String NEW_CHARGING_CONFIG_PREFIX_KEY = "Charging";

    String CHARGING_CONFIG_VERSION = "1.0";
}

/**
 * This class displays options for Charging precondition and allows the user to chose one
 * This also constructs the rule for Charging precondition and passes to the Condition Builder
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends MultiScreenPickerActivity which is a super class for the pickers
 *     Implements ChargingModeConstants which contains constants for rule construction
 *
 * RESPONSIBILITIES:
 *     This class displays options for Charging precondition and allows the user to chose one
 *     This also constructs the rule for Charging precondition and passes to the Condition Builder
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     None
 *
 * </PRE></CODE>
 */

public final class ChargingActivity extends MultiScreenPickerActivity
    implements ChargingConstants, Constants{

    private final static String TAG = ChargingActivity.class.getSimpleName();

    protected static final String sSupportedChargingLevels[] = new String[] {
        USB_AC_CHARGING,
        AC_CHARGING,
        USB_CHARGING,
        NOT_CHARGING
    };

    private Hashtable<String, String> mModeToDescriptionMap;
    private ChargingStatusFragment chargingStatus;
    private ChargingSourceFragment sourceFragment;

    //Called when the activity is first created.
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.chargingstatus_title));

        // Update super class members
        mModeToDescriptionMap = new Hashtable<String, String>(4);
        mModeToDescriptionMap.put(USB_AC_CHARGING, getString(R.string.using_anysourcecharging));
        mModeToDescriptionMap.put(AC_CHARGING, getString(R.string.using_accharging));
        mModeToDescriptionMap.put(USB_CHARGING, getString(R.string.using_usbcharging));
        mModeToDescriptionMap.put(NOT_CHARGING, getString(R.string.notcharging));

        final Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            mModeSelected = incomingIntent.getStringExtra(EXTRA_CONFIG);
            // cjd - what happens if this param is missing? is this a "killer?"
            //   can "throw new IllegalArgumentException("missing required param -"+CURRENT_MODE);
        }

        chargingStatus = new ChargingStatusFragment();
        chargingStatus.setSelectedItem(mModeSelected);
        this.launchNextFragment(chargingStatus, R.string.chargingstatus_title, true);
    }

    /**
     * On config changes such as screen rotation, keyboard changes and screen size changes, we don't want to lose state
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Launch the charging source fragment
     */
    public void launchChargingSourceFragment() {
        sourceFragment = new ChargingSourceFragment();
        if(mModeSelected != null && !mModeSelected.equals(NOT_CHARGING)) {
            sourceFragment.setSelectedItem(mModeSelected);
        }
        launchNextFragment(sourceFragment, R.string.chargingsource_title, false);
    }

    /**
     * Sets the result
     */
    protected void setResult(final String modeSelected) {
        // Construct and set the result
        final Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_CONFIG, modeSelected);
        returnIntent.putExtra(EXTRA_DESCRIPTION, mModeToDescriptionMap.get(modeSelected));
        if(LOG_INFO) Log.i(TAG, "resultsIntent : " + returnIntent.toUri(0));
        setResult(RESULT_OK, returnIntent);
    }

    String getDescriptionForMode(final String mode) {
        return mModeToDescriptionMap.get(mode);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.pickers.PickerFragment.ReturnFromFragment#onReturn(java.lang.Object)
     */
    @Override
    public void onReturn(final Object returnValue, final PickerFragment fromFragment) {
        if(returnValue == null) {
            //Just go back to the previous fragment or screen
            if(getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            }else {
                finish();
            }
        }
        if(returnValue instanceof String) {
            mModeSelected = (String) returnValue;
            if(getFragmentManager().getBackStackEntryCount() > 0) {
                //Send the selected mode to charging source fragment and pop the stack
                chargingStatus.setSelectedItem(mModeSelected);
                getFragmentManager().popBackStack();
            }else {
                //This is the top fragment, set the result and finish
                setResult(mModeSelected);
                finish();
            }
        }
    }
}
