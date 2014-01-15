/*
 * @(#)Wifi.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/17  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * This class extends the BinarySetting for Wifi <code><pre>
 * CLASS:
 *     Extends BinarySetting
 *
 * RESPONSIBILITIES:
 *     Overrides setState for setting Wifi state
 *     Inherits the rest of the methods from BinarySetting
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public final class Wifi extends BinarySetting implements Constants {

    private static final String TAG = TAG_PREFIX + Wifi.class.getSimpleName();
    public static final String WIFI_ACTION_KEY = ACTION_KEY_PREFIX + Wifi.class.getSimpleName();
    private static final String WIFI_STATE_KEY = "cd_wifi_state";
    public static final String WIFI_STATE_AFTER_TRANSITION = "wifi_state_after_transition";

    private String mStateString;
    private String mSettingString;

    public Wifi() {
        mActionKey = WIFI_ACTION_KEY;
        mBroadcastAction = WifiManager.WIFI_STATE_CHANGED_ACTION;
    }

    public boolean setState(Context context, Intent intent) {

        boolean status = true;
        mState = intent.getBooleanExtra(EXTRA_STATE, true);
        if (LOG_INFO)
            Log.i(TAG, "Wifi state to be set to " + mState);

        WifiManager wfm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        int state = wfm.getWifiState();
        if (LOG_INFO) Log.i(TAG, "Current Wifi state: "+state);

        String stateToSet = Persistence.retrieveValue(context, WIFI_STATE_AFTER_TRANSITION);
        if (stateToSet == null)
            mOldState = (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) ? true : false;
        else
            mOldState = (stateToSet.equals(STATE_TRUE)) ? true: false;

        if (state == WifiManager.WIFI_STATE_ENABLING || state == WifiManager.WIFI_STATE_DISABLING) {
            //Wifi is in transition. Wait for transition to complete
            //Save the state to be set after transition
            Persistence.commitValue(context, WIFI_STATE_AFTER_TRANSITION, (mState ? STATE_TRUE : STATE_FALSE));
        } else {
            if ((state == WifiManager.WIFI_STATE_ENABLED && mState == true) ||
                    (state == WifiManager.WIFI_STATE_DISABLED && mState == false)) {
                status = true;
                if (LOG_INFO) Log.i(TAG, "No change in state ; return success");
            } else {
                try {
                    status = wfm.setWifiEnabled(mState);
                    Persistence.commitValue(context, WIFI_STATE_KEY, mState);
                } catch (Exception e) {
                    status = false;
                    Log.e(TAG, "Error while setting wifi state");
                }
            }
            Persistence.removeValue(context, WIFI_STATE_AFTER_TRANSITION);
        }

        return status;

    }

    public Status handleSettingChange(Context context, Object obj) {
        Status status = Status.FAILURE;
        if (obj instanceof Intent) {
            WifiManager wfm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            int state = wfm.getWifiState();

            // We care only about the final states, unknown state occurs when the operation
            // failed
            if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_DISABLED
                    || state == WifiManager.WIFI_STATE_UNKNOWN) {

                boolean oldState = Persistence.retrieveBooleanValue(context, WIFI_STATE_KEY);
                boolean currentState = (state == WifiManager.WIFI_STATE_ENABLED) ? true : false;
                if(oldState != currentState) {
                    status = Status.SUCCESS;
                } else {
                    status = Status.NO_CHANGE;
                }
                if (state == WifiManager.WIFI_STATE_UNKNOWN) {
                    status = Status.FAILURE;
                }

                mStateString = convertWifiStateToString(state);
                mSettingString = convertWifiStateToDisplayString(context, state);

                String stateToSet = Persistence.removeValue(context, WIFI_STATE_AFTER_TRANSITION);
                if (stateToSet != null) {
                    //There was a state change request during transition.
                    //Handle the request here
                    boolean newState = (stateToSet.equals(STATE_TRUE)) ? true : false;
                    if (LOG_INFO) Log.i(TAG, "handleSettingChange trying to set state: "+newState);
                    try {
                        boolean updateStatus = wfm.setWifiEnabled(newState);
                        if (!updateStatus) {
                            Log.e(TAG, "Wifi state change failed after quick transition");
                        }
                        Persistence.commitValue(context, WIFI_STATE_KEY, newState);
                    } catch (Exception e) {
                        Log.e(TAG, "Error while setting wifi state");
                    }
                }

            } else {
                status = Status.TRANSIENT;
                if (LOG_INFO) Log.i(TAG, "Transient Wifi state is " + state);
            }
        }

        return status;

    }

    public String getState(Context context) {
        return (mStateString != null) ? mStateString : super.getState(context);
    }

    public String getSettingString(Context context) {
        return (mSettingString != null) ? mSettingString : super.getSettingString(context);
    }

    public String getActionString(Context context) {
        return context.getString(R.string.wifi);
    }

    /**
     * Returns a string representation of Wifi State
     *
     * @param state - Wifi state
     * @return
     */
    private static String convertWifiStateToString(int state) {
        String value = FAILED;
        if (state == WifiManager.WIFI_STATE_ENABLED)
            value = ENABLE;
        else if (state == WifiManager.WIFI_STATE_DISABLED)
            value = DISABLE;
        return value;
    }

    /**
     * Returns a string representation of Wifi State that can be displayed to the user
     *
     * @param context
     * @param state
     * @return
     */
    private static String convertWifiStateToDisplayString(Context context, int state) {
        String displayString = context.getString(R.string.unknown);
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            displayString = context.getString(R.string.on);
        } else if (state == WifiManager.WIFI_STATE_DISABLED) {
            displayString = context.getString(R.string.off);
        }

        return displayString;
    }

}
