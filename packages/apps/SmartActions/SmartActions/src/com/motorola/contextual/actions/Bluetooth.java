/*
 * @(#)Bluetooth.java
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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.telephony.TelephonyManager;

import com.motorola.contextual.actions.Persistence;
import com.motorola.contextual.smartrules.R;

/**
 * This class extends the BinarySetting for Bluetooth <code><pre>
 * CLASS:
 *     Extends BinarySetting
 *
 * RESPONSIBILITIES:
 *     Overrides setState for setting Bluetooth state
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

public final class Bluetooth extends BinarySetting implements Constants {

    private static final String TAG = TAG_PREFIX + Bluetooth.class.getSimpleName();
    private static final String BLUETOOTH_STATE_KEY = "cd_bluetooth_state";

    /**
     * Key used to store state info if state change request comes during transition.
     * If there is no value stored with this key then state change request didn't happen during transition
     * Otherwise, it contains the expected state to be set after transition.
     * This is done to ensure that state change request is not sent to BluetoothAdapter during transition.
     */
    private static final String BLUETOOTH_STATE_AFTER_TRANSITION = "bt_state_after_transition";

    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    public Bluetooth() {
        mActionKey = BLUETOOTH_ACTION_KEY;
        mBroadcastAction = BluetoothAdapter.ACTION_STATE_CHANGED;
    }

    public boolean setState(Context context, Intent intent) {
        mState = intent.getBooleanExtra(EXTRA_STATE, true);
        if (LOG_INFO)
            Log.i(TAG, "Bluetooth state to be set to " + mState);

        return setState(context, mState);
    }

    public String getActionString(Context context) {
        return context.getString(R.string.bluetooth);
    }

    public Status handleSettingChange(Context context, Object obj) {
        Status status = Status.FAILURE;
        if (obj instanceof Intent) {
            Intent intent = (Intent)obj;
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
                mState = (state == BluetoothAdapter.STATE_ON);

                boolean oldState = Persistence.retrieveBooleanValue(context, BLUETOOTH_STATE_KEY);;
                if(oldState != mState) {
                    status = Status.SUCCESS;
                } else {
                    status = Status.NO_CHANGE;
                }

                //Removing any pending actions waiting for a call to end
                Persistence.removeValue(context, BLUETOOTH_STATE_AFTER_CALL);

                String stateToSet = Persistence.removeValue(context, BLUETOOTH_STATE_AFTER_TRANSITION);
                if (stateToSet != null) {
                    setState(context, (stateToSet.equals(STATE_TRUE)) ? true : false);
                }
            } else {
                status = Status.TRANSIENT;
                if (LOG_INFO)
                    Log.i(TAG, "Transient BT state is " + state);
            }
        }
        return status;
    }

    public boolean handlePhoneState(Context context, String phoneState) {
        if (LOG_DEBUG) Log.d(TAG, "handlePhoneState phoneState is " + phoneState);

        String stateToSet = Persistence.removeValue(context, BLUETOOTH_STATE_AFTER_CALL);
        if (stateToSet == null)
            return true;
        else
            return setState(context, (stateToSet.equals(STATE_TRUE)) ? true : false);
    }

    private boolean setState(Context context, boolean newState) {
        if (LOG_DEBUG) Log.d(TAG, "setState state to be set is " + newState);
        boolean status = true;

        if(mAdapter != null) {
            int state = mAdapter.getState();
            if (LOG_DEBUG) Log.d(TAG, "Current BT state: "+state);

            //If there is a value stored in Persistence storage then there is pending BT state change request
            //In that case the pending state is expected to be the old state
            String stateToSet = Persistence.retrieveValue(context, BLUETOOTH_STATE_AFTER_TRANSITION);
            if (stateToSet == null)
                mOldState = (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_TURNING_ON) ? true : false;
            else
                mOldState = (stateToSet.equals(STATE_TRUE)) ? true : false;

            if (state == BluetoothAdapter.STATE_TURNING_ON || state == BluetoothAdapter.STATE_TURNING_OFF) {
                //Bluetooth is in transition. Wait for transition to complete
                //Save the state to be set after transition
                //handleSettingChange will be called when transition completes. Handle this request there.
                Persistence.commitValue(context, BLUETOOTH_STATE_AFTER_TRANSITION, (newState ? STATE_TRUE : STATE_FALSE));
            } else {
                if (mOldState != newState) {
                    //If Request is to turn off BT, Need to check the phone state
                    if (newState == false) {
                        //Get the current phone State
                        TelephonyManager telephonyManager = (TelephonyManager) context
                                                            .getSystemService(Context.TELEPHONY_SERVICE);
                        int phoneState = telephonyManager.getCallState();
                        if (LOG_INFO) {
                            Log.i(TAG, "setState : Phone state is "
                                  + phoneState);
                        }
                        if (phoneState != TelephonyManager.CALL_STATE_IDLE) {
                            //Add to pending action,
                            Persistence.commitValue(context, BLUETOOTH_STATE_AFTER_CALL, (newState ? STATE_TRUE : STATE_FALSE));
                            return status;
                        }
                    }
                    status = (newState) ? mAdapter.enable() : mAdapter.disable();
                }
                Persistence.commitValue(context, BLUETOOTH_STATE_KEY, newState);

            }
        } else {
            status = false;
            Log.e(TAG, "setState : Could not get BT adapter");
        }

        return status;
    }

}
