/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * dfb746                      16/04/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BatteryInfo extends InfoBase {
    private final static String TAG = "BatteryInfo";

    /**
     * application context
     */
    private Context mContext;
    private int mStatus;
    private int mHealth;
    private String mTechnology;
    private int mRawlevel;
    private int mScale;
    private int mVoltage;
    private int mTemperature;
    private int mPlugged;

    public BatteryInfo(Context context) {
        super();

        this.mContext = context;

        startBatteryMonitor();
    }

    private void startBatteryMonitor() {
        IntentFilter battFilter = new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(battReceiver, battFilter);
    }

    private BroadcastReceiver battReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Enter battReceiver onReceive ");

            mRawlevel = intent.getIntExtra("level", -1);
            mScale = intent.getIntExtra("scale", -1);
            mStatus = intent.getIntExtra("status", -1);
            mHealth = intent.getIntExtra("health", -1);
            mPlugged = intent.getIntExtra("plugged", -1);
            mTemperature = intent.getIntExtra("temperature", -1);
            mVoltage = intent.getIntExtra("voltage", -1);
            mTechnology = intent.getStringExtra("technology");

            onInfoUpdate();

            Log.i(TAG, BatteryInfo.this.toString());
        }
    };

    public int getStatus() {
        return mStatus;
    }

    public int getHealth() {
        return mHealth;
    }

    public String getTechnology() {
        return mTechnology;
    }

    public int getRawlevel() {
        return mRawlevel;
    }

    public int getScale() {
        return mScale;
    }

    public int getVoltage() {
        return mVoltage;
    }

    public int getTemperature() {
        return mTemperature;
    }

    public int getPlugged() {
        return mPlugged;
    }

    @Override
    public String toString() {
        return "BatteryInfo: " + this.mStatus + "/" + this.mPlugged + "/"
                + this.mRawlevel + "/" + this.mScale + "/" + this.mHealth + "/"
                + this.mTechnology + "/" + this.mTemperature + "/" + this.mVoltage;
    }
}
