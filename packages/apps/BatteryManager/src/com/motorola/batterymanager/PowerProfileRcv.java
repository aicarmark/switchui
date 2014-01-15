/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: PowerProfileRcv.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 * 07-13-10       A16462       IKSTABLETWO-2784: Dynamic Data Mode change
 *                -Selvi
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemProperties;

import com.motorola.batterymanager.BatteryManagerDefs;
import com.motorola.batterymanager.BatteryManagerDefs.DataConnection;
import com.motorola.batterymanager.BatteryManagerDefs.Mode;

public class PowerProfileRcv extends BroadcastReceiver{

    private final static String LOG_TAG = "PowerProfileBoot";
    private Context mContext;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        mContext = ctx;
        SharedPreferences myPreference = ctx.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                Context.MODE_PRIVATE);

        int mode = myPreference.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE, 
                BatteryProfile.DEFAULT_PRESET_MODE);
        int loglevel = myPreference.getInt(BatteryProfile.KEY_LOG_LEVEL, 
                BatteryProfile.DEFAULT_CHECKIN_LEVEL);

        Utils.Log.setCheckinLevel(loglevel);

        // Default Change check
        // Two checks here:
        // 1. Change default mode on first boot if necessary
        // 2. If default is perf mode, then reset flag
        boolean isFirstBoot = !myPreference.getBoolean(
                    BatteryProfile.KEY_NOTIFY_USER_PROFILE_SELECT, false);
        if(isFirstBoot) {
            int defMode = SystemProperties.getInt("ro.mot.battmanager.defmode", mode);
            SharedPreferences.Editor myEditor = myPreference.edit();
            if(mode != defMode) {
                myEditor.putInt(BatteryProfile.KEY_OPTION_PRESET_MODE,
                        defMode);
                myEditor.commit();
                mode = defMode;
            }
            if(mode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                myEditor.putBoolean(BatteryProfile.KEY_NOTIFY_USER_PROFILE_SELECT,
                        true);
                myEditor.commit();
            }    
        }
        // Default Change check
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // As on Preformance mode service is not started,reset preferences here.
            // Done here for all modes to avoid resetting in multiple places
            resetPreferences(myPreference);

            if(mode != BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                Utils.Log.setContentResolver(ctx.getContentResolver());
                Utils.Log.d(LOG_TAG, "Boot Receiver: Starting service in mode: " +
                        mode);

                Intent svcIntent = new Intent(ctx, PowerProfileSvc.class);
                svcIntent.setAction(PowerProfileSvc.SVC_START_ACTION);
                ctx.startService(svcIntent);

                DisplayControl.noteModeChange(ctx, mode);
            }
        } else if (action.equals(PowerProfileSvc.ALRM_KEEP_DATA_ON_TIMER_ACTION)){
            Utils.Log.d(LOG_TAG, "TIMER Expiry for Keep Data ON");
            Intent in = new Intent(PowerProfileSvc.ALRM_KEEP_DATA_ON_TIMER_ACTION);
            in.setClass(ctx, PowerProfileSvc.class);
            ctx.startService(in);
        }
    }

    private void resetPreferences(SharedPreferences myPref) {
        SharedPreferences.Editor myPrefEditor = myPref.edit();
        int dataOffPersistent = myPref.getInt(BatteryProfile.KEY_KEEP_DATA_OFF_PERSISTENT, 0);
        int dataOnPersistent = myPref.getInt(BatteryProfile.KEY_KEEP_DATA_ON_PERSISTENT, 0);

        if (dataOffPersistent == 0) {
            myPrefEditor.putInt(BatteryProfile.KEY_KEEP_DATA_OFF, 0);
            myPrefEditor.putInt(BatteryProfile.KEY_DATA_CONNECTION_STATE,
                                BatteryProfile.DEF_DATA_CONN_STATE);

            // Send Broadcast for data state change
            Intent bcIntent = new Intent(BatteryManagerDefs.ACTION_BM_STATE_CHANGED);
            bcIntent.putExtra(BatteryManagerDefs.KEY_DATA_CONNECTION, DataConnection.ON);
            int prefMode = myPref.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE,
                    BatteryProfile.DEFAULT_PRESET_MODE);
            boolean isPreset = myPref.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET,
                    BatteryProfile.DEFAULT_OPTION_SELECT);

            // Map here to avoid confusion
            int mode = 0;
            if (isPreset) {
                if (prefMode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                    mode = Mode.PERFORMANCE;
                } else if (prefMode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    mode = Mode.NIGHT_SAVER;
                } else if (prefMode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                    mode = Mode.BATTERY_SAVER;
                }
            } else {
                mode = Mode.CUSTOM;
            }
            bcIntent.putExtra(BatteryManagerDefs.KEY_BM_MODE, mode);
            mContext.sendBroadcast(bcIntent);
        }

        if (dataOnPersistent == 0) {
            myPrefEditor.putInt(BatteryProfile.KEY_KEEP_DATA_ON, 0);
        }

        if ((dataOffPersistent == 0) && (dataOnPersistent == 0)) {
            myPrefEditor.putInt(BatteryProfile.KEY_REQ_DATA_MODE, BatteryProfile.NO_ACTIVE_MODE);
        }

        myPrefEditor.putBoolean(BatteryProfile.KEY_KEEP_DATA_ON_WITH_TIMER, false);
        myPrefEditor.commit();
    }
}
