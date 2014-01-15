/*
 * @(#)Airplane.java
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

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.motorola.contextual.smartrules.R;

/**
 * This class extends the BinarySetting for Airplane Mode <code><pre>
 * CLASS:
 *     Extends BinarySetting
 *
 * RESPONSIBILITIES:
 *     Overrides setState for setting Airplane Mode
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

public final class Airplane extends BinarySetting implements Constants {

    private static final String TAG = TAG_PREFIX + Airplane.class.getSimpleName();
    private static final String AIRPLANE_ACTION_KEY = ACTION_KEY_PREFIX + Airplane.class.getSimpleName();
    private static final String EXTRA_AIRPLANE_STATE = "state";
    private static final String AIRPLANE_STATE_KEY = "airplane_current_state";

    public Airplane() {
        mActionKey = AIRPLANE_ACTION_KEY;
        mBroadcastAction = Intent.ACTION_AIRPLANE_MODE_CHANGED;
    }

    public boolean setState(Context context, Intent intent) {

        boolean status = true;
        mState = intent.getBooleanExtra(EXTRA_STATE, true);
        if (LOG_INFO)
            Log.i(TAG, "Airplane state to be set to " + mState);

        String settingName = Settings.System.AIRPLANE_MODE_ON;
        int state = DISABLED;
        try {
            state = Settings.System.getInt(context.getContentResolver(), settingName);
        } catch (SettingNotFoundException e) {
            Log.e(TAG, settingName + " setting not found");
        }
        mOldState = (state == ENABLED);

        if (mOldState != mState) {
            status = Settings.System.putInt(context.getContentResolver(), settingName, (mState) ? ENABLED
                                            : DISABLED);
            if (status) {
                doBroadcast(context, mState);
            }
        }
        Persistence.commitValue(context, AIRPLANE_STATE_KEY, mState);

        return status;

    }

    public String getActionString(Context context) {
        return context.getString(R.string.ap);
    }

    public Status handleSettingChange(Context context, Object obj) {
        Status status = Status.FAILURE;
        if (obj instanceof Intent) {
            status = Status.NO_CHANGE;
            boolean oldState = Persistence.retrieveBooleanValue(context, AIRPLANE_STATE_KEY);

            try {
                mState = (Settings.System.getInt(context.getContentResolver(),
                        Settings.System.AIRPLANE_MODE_ON) == ENABLED) ? true : false;
            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }
            if(oldState != mState) {
                status = Status.SUCCESS;
            } else {
                status = Status.NO_CHANGE;
            }
            return status;
        }
        return status;
    }

    /**
     * Broadcasts ACTION_AIRPLANE_MODE_CHANGED intent
     *
     * @param context
     * @param state
     */
    private static void doBroadcast(Context context, boolean state) {
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra(EXTRA_AIRPLANE_STATE, state);
        context.sendBroadcast(intent);
    }

}
