/*
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 */

package com.motorola.acousticwarning.mmcp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.os.SystemProperties;

public class AcousticBootReceiver extends BroadcastReceiver {

    private static String ACTION_ACOUSTIC_SERVICE= "com.motorola.acousticwarning.START_SERVICE";
    private static final boolean DEBUG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    //private static final boolean DEBUG = true;
    private static final String LOG_TAG = "AcousticWarning";

    public static final String ACOUSTIC_PREF_NAME       = "acoustic_shared_pref";
    public static final String IS_ACOUSTIC_FTR_ENABLED  = "IsAcousticEnabled";
    public static final String REMINDER_EXPIRY_PERIOD   = "ReminderPeriod";
    public static final String BOOT_COMPLETE_STATUS     = "BootCompleteStatus";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(LOG_TAG, "You are in AcousticBootReceiver");

	SharedPreferences sharedPrefs;
        SharedPreferences.Editor sharedPrefEditor;
        sharedPrefs = context.getSharedPreferences(ACOUSTIC_PREF_NAME, 0);
        sharedPrefEditor = sharedPrefs.edit();
        sharedPrefEditor.putString(BOOT_COMPLETE_STATUS,"BootCompletedUnchecked");
        sharedPrefEditor.apply();

        // Start the service as the feature is ON
        context.startService(new Intent(ACTION_ACOUSTIC_SERVICE));
        
        if (context.getSharedPreferences(ACOUSTIC_PREF_NAME, 0)
                   .getBoolean(IS_ACOUSTIC_FTR_ENABLED, false)) { 
           if (DEBUG) Log.d(LOG_TAG, "35096 feature turned On");
            // Start the service as the feature is ON
            context.startService(new Intent(ACTION_ACOUSTIC_SERVICE));
            if (DEBUG) Log.d(LOG_TAG, ": sent ACTION_ACOUSTIC_SERVICE Intent");
        } else {
            if (DEBUG) Log.d(LOG_TAG,"35096 feature turned off");
       }
	
    }
}
