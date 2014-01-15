/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: WifiToggler.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 * 04-06-10       A24178       Make Wi-Fi ctrl flexible
 *                -Ashok  
 * 04-22-10       A24178       Add check for Wi-Fi sleep policy
 *                -Ashok
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;

import com.motorola.internal.intents.BatteryIntents;

// package-scope
class WifiToggler extends BroadcastReceiver {
    private final static String LOG_TAG = "PowerProfileWifiToggler";
    private final static String SYS_WIFI_PROP_NAME = "ro.mot.battmanager.wifictrl";

    private Context mContext;
    private int mWifiState = WifiManager.WIFI_STATE_UNKNOWN;
    private boolean mshouldToggle; 
    private boolean minCoolDownMode;
    private WifiManager wm;
    private boolean mSysWifiProp;

    public WifiToggler(Context ctx) {
        mContext = ctx;

        wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        IntentFilter wifistatefilter = 
            new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifistatefilter.addAction(BatteryIntents.ACTION_BATTERY_COOLDOWN_MODE);
        mContext.registerReceiver(this, wifistatefilter);
        mshouldToggle = false;
        minCoolDownMode = false;
        mSysWifiProp = SystemProperties.getBoolean(SYS_WIFI_PROP_NAME, true);
    }

    public void stop() {
        mshouldToggle = false;
        mContext.unregisterReceiver(this);
    }

    public void enable() {
        ///** Disabling Wifi ctrl in BatteryManager
        if(mshouldToggle && mSysWifiProp) {
            Utils.Log.d(LOG_TAG, "Enable:Before: State --> " + wm.getWifiState()
                    + " enabled --> " + wm.isWifiEnabled());

            if(minCoolDownMode) {
                mshouldToggle = false;
                Utils.Log.d(LOG_TAG, "Enable:Before: In Cool down mode, not enabling Wifi");
                return;
            }

            if(wm.setWifiEnabled(true)) {
                Utils.Log.d(LOG_TAG, "Enable:After: Done");
            }else {
                Utils.Log.e(LOG_TAG, "Enable:After: Failed to enabled Wifi");
            }
        }else {
            Utils.Log.d(LOG_TAG, "Enable: No need to enable Wifi per flag");
        }
        //**/
        mshouldToggle = false;
    }

    public void disable() {
        ///** Disabling Wifi ctrl in BatteryManager 
        mWifiState = wm.getWifiState();
        mshouldToggle = wm.isWifiEnabled();

        int wifiSleepPolicy = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_DEFAULT);

        mshouldToggle = mshouldToggle && 
                (wifiSleepPolicy == Settings.System.WIFI_SLEEP_POLICY_NEVER); 

        Utils.Log.d(LOG_TAG, "Disable: State --> " + mWifiState + " toggle -->" 
                + mshouldToggle + ",sys WiFi prop -->" + mSysWifiProp);
        if(mshouldToggle && mSysWifiProp) {
            if(wm.setWifiEnabled(false)) {
                Utils.Log.d(LOG_TAG, "Disable: Done");
            }else {
                Utils.Log.e(LOG_TAG, "Disable: Error while disabling...");
            }
        }
        //**/
    }

    @Override
    public void onReceive(Context ctx, Intent bcInt) {
        if(bcInt.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            int wifistate = bcInt.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 
                    WifiManager.WIFI_STATE_UNKNOWN);
            if(wifistate == WifiManager.WIFI_STATE_DISABLED) {
                Utils.Log.d(LOG_TAG, "Receiver: Wifi State is disabled");
            }else if(wifistate == WifiManager.WIFI_STATE_ENABLED) {
                Utils.Log.d(LOG_TAG, "Receiver: Wifi State is enabled");
            }
        }else if(bcInt.getAction().equals(BatteryIntents.ACTION_BATTERY_COOLDOWN_MODE)) {
            minCoolDownMode = bcInt.getBooleanExtra("state", false);
        }
    }
}

