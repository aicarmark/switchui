/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * pjw346                      28/05/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import com.motorola.mmsp.performancemaster.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;

public class BattRemainingUpdate extends BroadcastReceiver {
    public static final String ACTION_BATTERY_REMAINING_TIMES =
            "com.motorola.batterymanager.bws.UpdateRemainingTime";
    public static final String EXTRA_KEY_REMAINING_TIMES = "RemainTime";

    private Context mContext;
    private TextView mTView;
    private static long mRemainingMinutes = 24 * 60; // default value

    public BattRemainingUpdate(Context ctx, TextView tv) {
        mContext = ctx;
        mTView = tv;

        mContext.registerReceiver(this,
                new IntentFilter(ACTION_BATTERY_REMAINING_TIMES));

        updateDisplay();
    }

    public void stop() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String strActionString = intent.getAction();
        if (0 == strActionString.
                compareTo(ACTION_BATTERY_REMAINING_TIMES)) {
            mRemainingMinutes = intent.getExtras()
                    .getLong(EXTRA_KEY_REMAINING_TIMES);
            
            // first time, we init mRemainingMinutes, so
            // mTView == null
            if (null == mTView) {
                stop();
            }

            updateDisplay();
        }
    }

    public void setDefaultRemainTime(long nRemainTime) {
        mRemainingMinutes = nRemainTime;
    }

    private String getTotal(long nMinutes) {
        return String.valueOf((nMinutes / 60)) +
                 mContext.getResources().getString(R.string.bm_hour) +
                 (nMinutes % 60) +
                 mContext.getResources().getString(R.string.bm_minute);
    }

    private void updateDisplay() {
        if (null != mTView) {
            mTView.setText(getTotal(mRemainingMinutes));
        }
    }
}
