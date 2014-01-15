/*
 * @(#)ScreenTimeout.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

/**
 * This class extends the StatefulAction class for Screen Timeout <code><pre>
 * CLASS:
 *     extends StatefulAction
 *
 * RESPONSIBILITIES:
 *     Uses Settings.System interface to get/set Screen Timeout values
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public final class ScreenTimeout extends StatefulAction implements Constants {

    private static final String TAG = TAG_PREFIX + ScreenTimeout.class.getSimpleName();
    private static final String SCREEN_TIMEOUT_ACTION_KEY = ACTION_KEY_PREFIX + ScreenTimeout.class.getSimpleName();
    private static final String CURRENT_SCREEN_TIMEOUT = "current_screen_timeout";

    private int mOldTimeout;
    private int mTimeout;

    @Override
    public boolean setState(Context context, Intent intent) {

        boolean status = true;

        mTimeout = intent.getIntExtra(EXTRA_TIMEOUT, 0);

        if (LOG_INFO)
            Log.i(TAG, "Screen timeout value is " + mTimeout);

        try {
            mOldTimeout = Settings.System.getInt(context.getContentResolver(),
                                                 Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (SettingNotFoundException e) {
            Log.w(TAG, Settings.System.SCREEN_OFF_TIMEOUT + "Setting not found");
        }

        if (LOG_DEBUG) Log.d(TAG, "Screen timeout: old:"+mOldTimeout+"  current:"+mTimeout);
        if (mOldTimeout != mTimeout) {
            status = Settings.System.putInt(context.getContentResolver(),
                                            Settings.System.SCREEN_OFF_TIMEOUT, mTimeout);
            if (LOG_DEBUG) Log.d(TAG, "Set screen timeout to msecs:"+mTimeout);
        }
        
        Persistence.commitValue(context, CURRENT_SCREEN_TIMEOUT, mTimeout);
        return status;
    }

    @Override
    public String getState(Context context) {
        return Integer.toString(mTimeout);
    }

    @Override
    public String getSettingString(Context context) {
        return convertScreenTimeoutToString(context, mTimeout);
    }

    @Override
    public String getDefaultSetting(Context context) {
        return getConfig(mOldTimeout);
    }

    @Override
    public void registerForSettingChanges(Context context) {
        StatefulActionHelper.registerForSettingChanges(context, SCREEN_TIMEOUT_ACTION_KEY);

    }

    @Override
    public void deregisterFromSettingChanges(Context context) {
        StatefulActionHelper.deregisterFromSettingChanges(context, SCREEN_TIMEOUT_ACTION_KEY);
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.screen_timeout);
    }

    @Override
    public Status handleSettingChange(Context context, Object obj) {

        Status status = Status.FAILURE;
        if (obj instanceof String) {
            String observed = (String)obj;
            try {
                mTimeout = Settings.System.getInt(context.getContentResolver(),
                        observed);
                int oldTimeout = Persistence.retrieveIntValue(context,
                        CURRENT_SCREEN_TIMEOUT);
                if (oldTimeout != mTimeout) {
                    status = Status.SUCCESS;
                } else {
                    status = Status.NO_CHANGE;
                }
            } catch (SettingNotFoundException e) {
                Log.w(TAG, "Setting Not Found");
            }
        }

        return status;
    }

    @Override
    public String[] getSettingToObserve() {
        return new String[] {
                   Settings.System.SCREEN_OFF_TIMEOUT
               };
    }

    @Override
    public Uri getUriForSetting(String setting) {
        return Settings.System.getUriFor(setting);
    }

    @Override
    public String getActionKey() {
       return SCREEN_TIMEOUT_ACTION_KEY;
    }

    /**
     * Converts the screen timeout setting into a user readable string
     *
     * @param context
     * @param timeout
     * @return
     */
    public String convertScreenTimeoutToString(Context context, int timeout) {
        String s = getActionString(context);
        final String entries[] = context.getResources().getStringArray(
                                     R.array.screen_timeout_entries);
        final int values[] = context.getResources().getIntArray(R.array.screen_timeout_values);
        for (int i = 0; i < values.length; ++i) {
            if (values[i] == timeout) {
                s = entries[i];
                break;
            }
        }
        return s;
    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        return convertScreenTimeoutToString(context,
                configIntent.getIntExtra(EXTRA_TIMEOUT, 0));
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getIntExtra(EXTRA_TIMEOUT, 0));
    }

    @Override
    public List<String> getConfigList(Context context) {
        int[] values = context.getResources().getIntArray(
                R.array.screen_timeout_values);
        ArrayList<String> configList = new ArrayList<String>();
        for (int timeout : values) {
            configList.add(getConfig(timeout));
        }
        return configList;
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        return Arrays.asList(context.getResources().getStringArray(
                R.array.screen_timeout_entries));
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        return configIntent.getIntExtra(EXTRA_TIMEOUT, -2) != -2;
    }

    /**
     * This method returns the config intent uri based upon the timeout value
     *
     * @param timeout
     *            - the timeout value
     * @return - config intent uri
     */
    public static String getConfig(int timeout) {
        Intent config = new Intent();
        config.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        config.putExtra(EXTRA_TIMEOUT, timeout);
        return config.toUri(0);
    }
}
