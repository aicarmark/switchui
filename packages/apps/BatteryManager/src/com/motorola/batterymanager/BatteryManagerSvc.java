/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: BatteryManagerSvc.java
 * Description: Interface implementation for BatteryManager
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 07-13-10       A16462       IKMAIN-736: Dynamic Data Mode change
 *                -Selvi       Initial Creation
 **********************************************************
 */

package com.motorola.batterymanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Binder;

import com.android.internal.telephony.Phone;

import com.motorola.batterymanager.BatteryManagerDefs;
import com.motorola.batterymanager.BatteryManagerDefs.Mode;
import com.motorola.batterymanager.BatteryManagerDefs.DataConnection;
import com.motorola.batterymanager.BatteryManagerDefs.DataSettings;
import com.motorola.batterymanager.devicestatistics.DevStatPrefs;

public class BatteryManagerSvc extends Service {
    private final static String LOG_TAG = "BatteryManagerSvc";

    private final IBatteryManager.Stub mBinder = new IBatteryManager.Stub() {

        public void changeDataSettings(int state, long duration, boolean persistent) {
            Utils.Log.i(DevStatPrefs.CHECKIN_EVENT_ID,
                   "[ID=BMCallerInfo;ver=" + DevStatPrefs.VERSION + ";time=" + (System.currentTimeMillis()/1000) + ";]"
                   + "[ID=chgData;pid=" + Binder.getCallingPid()
                   + ";state=" + state + ";dur=" + duration + ";persistent=" + persistent + ";]");

            Intent intent = new Intent();
            intent.setClassName("com.motorola.batterymanager",
                    "com.motorola.batterymanager.PowerProfileSvc");
            intent.putExtra("PERSISTENT", persistent);
            boolean validRequest = true;
            switch (state) {
                case DataSettings.DATA_ON_ALWAYS:
                    intent.setAction(PowerProfileSvc.KEEP_DATA_ON_ACTION);
                    intent.putExtra("DURATION",
                        (duration != BatteryManagerDefs.NO_DURATION)?duration:BatteryManagerDefs.NO_DURATION);
                    break;

                case DataSettings.DATA_OFF_ALWAYS:
                    intent.setAction(PowerProfileSvc.KEEP_DATA_OFF_ACTION);
                    break;

                case DataSettings.RESET_DATA_ON_ALWAYS:
                    intent.setAction(PowerProfileSvc.WITHDRAW_DATA_ON_ACTION);
                    break;

                case DataSettings.RESET_DATA_OFF_ALWAYS:
                    intent.setAction(PowerProfileSvc.WITHDRAW_DATA_OFF_ACTION);
                    break;
 
                default:
                   Utils.Log.d(LOG_TAG, "changeDataSettings: Invalid State");
                   validRequest = false;
                   break;
            }

            if (validRequest) {
                startService(intent);
            }
        }

        public int enableApnType(String type) {
            Utils.Log.i(DevStatPrefs.CHECKIN_EVENT_ID, 
                   "[ID=BMCallerInfo;ver=" + DevStatPrefs.VERSION + ";time=" + (System.currentTimeMillis()/1000) + ";]"
                   + "[ID=enApn;pid=" + Binder.getCallingPid() + ";type=" + type + ";]");

            // If type ! = mms reject the request
            if (!type.equals(Phone.APN_TYPE_MMS)) return BatteryManagerDefs.APN_TYPE_NOT_SUPPORTED;

            Intent intent = new Intent();
            intent.setClassName("com.motorola.batterymanager",
                    "com.motorola.batterymanager.PowerProfileSvc");
            intent.setAction(PowerProfileSvc.ENABLE_APN_TYPE_ACTION);
            intent.putExtra("TYPE", type);
            startService(intent);
            return BatteryManagerDefs.APN_ENABLE_REQUEST_ACCEPTED;
        }

        public int getCurrentBatteryManagerMode() {
            Utils.Log.d(LOG_TAG, "getCurrentBatteryManagerMode:PID=" + Binder.getCallingPid());
            SharedPreferences pref = getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
            int mode = pref.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE,
                    BatteryProfile.DEFAULT_PRESET_MODE);
            boolean isPreset = pref.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET,
                    BatteryProfile.DEFAULT_OPTION_SELECT);
            // Map here to avoid confusion
            int retMode = 0;
            if (isPreset) {
                if (mode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                    retMode = Mode.PERFORMANCE;
                } else if (mode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    retMode = Mode.NIGHT_SAVER;
                } else if (mode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                    retMode = Mode.BATTERY_SAVER;
                }
            } else {
                retMode = Mode.CUSTOM;
            }

            Utils.Log.d(LOG_TAG, "getCurrentBatteryManagerMode: Current Mode = " + retMode);
            return retMode;
        }

        public int getCurrentDataStatus() {
            Utils.Log.d(LOG_TAG, "getCurrentDataStatus:PID=" + Binder.getCallingPid());
            SharedPreferences pref = getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
            int state = pref.getInt(BatteryProfile.KEY_DATA_CONNECTION_STATE,
                    BatteryProfile.DEF_DATA_CONN_STATE);
            return (state == BatteryProfile.DATA_CONN_OFF)?DataConnection.OFF:DataConnection.ON;
        }

        public void changeBatteryManagerMode(int mode) {
            Utils.Log.i(DevStatPrefs.CHECKIN_EVENT_ID,
                   "[ID=BMCallerInfo;ver=" + DevStatPrefs.VERSION + ";time=" + (System.currentTimeMillis()/1000) + ";]"
                   + "[ID=chgBmMode;pid=" + Binder.getCallingPid() + ";mode=" + mode + ";]");

            //TODO: Not supported, as no clients for this yet
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(BatteryManagerDefs.ACTION_START_BATTERY_MANAGER)) {
            Utils.Log.d(LOG_TAG, "onBind:BatteryManagerService");
            return mBinder;
        }
        return null;
    }
}
