/*
 * @(#)Gps.java
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
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.motorola.contextual.smartrules.R;

/**
 * This class extends the BinarySetting for Gps <code><pre>
 * CLASS:
 *     Extends BinarySetting
 *
 * RESPONSIBILITIES:
 *     Overrides majority of the methods for Gps
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public final class Gps extends BinarySetting implements Constants {

    private static final String TAG = TAG_PREFIX + Gps.class.getSimpleName();
    private static final String GPS_ACTION_KEY = ACTION_KEY_PREFIX + Gps.class.getSimpleName();
    private static final String GPS_STATE_KEY = "gps_current_state";


    public Gps() {
        mActionKey = GPS_ACTION_KEY;
    }

    public boolean setState(Context context, Intent intent) {

        mState = intent.getBooleanExtra(EXTRA_STATE, true);
        if (LOG_INFO)
            Log.i(TAG, "GPS state to be set to " + mState);

        mOldState = Settings.Secure.isLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER);
        if (mOldState != mState) {
        	try {
        		Settings.Secure.setLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER, mState);        		
        	} catch (SecurityException e) {
        		// this should never happen in production, only if the /system/app/ version of SA is removed, or the one.apk is run on non-Moto phone
        		Toast.makeText(context, context.getResources().getString(R.string.gps_error_secure), Toast.LENGTH_SHORT).show();
        		e.printStackTrace();
        	}
        }

        Persistence.commitValue(context, GPS_STATE_KEY, mState);

        return true;
    }

    public void registerForSettingChanges(Context context) {
        StatefulActionHelper.registerForSettingChanges(context, mActionKey);
    }

    public void deregisterFromSettingChanges(Context context) {
        StatefulActionHelper.deregisterFromSettingChanges(context, mActionKey);
    }

    public String getActionString(Context context) {
        return context.getString(R.string.gps);
    }

    public Status handleSettingChange(Context context, Object obj) {

        Status status = Status.NO_CHANGE;
        boolean oldState = Persistence.retrieveBooleanValue(context, GPS_STATE_KEY);

        mState = Settings.Secure.isLocationProviderEnabled(context.getContentResolver(),
                 LocationManager.GPS_PROVIDER);

        if(oldState != mState) {
            status = Status.SUCCESS;
        } else {
            status = Status.NO_CHANGE;
        }
        return status;
    }

    public String[] getSettingToObserve() {
        return new String[] {
                   Settings.Secure.LOCATION_PROVIDERS_ALLOWED
               };
    }

    public Uri getUriForSetting(String setting) {
        return Settings.Secure.getUriFor(setting);
    }

}
