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

import android.net.TrafficStats;

import com.motorola.mmsp.performancemaster.engine.Job.JobDoneListener;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Network speed for 2G/3G network
 * 
 * @author dfb746
 */
public class NetSpeedInfo extends InfoBase implements JobDoneListener {
    private final static String TAG = "NetSpeedInfo";

    private float mNetworkSpeed = -1; // unit KB/S, -1 is invalid value

    private static int UPDATE_INTERVAL = 2000;
    private Timer mUpdateTimer = null;
    private TimerTask mTimeTask = null;
    private Job mUpdateJob = null;

    private Date mStartDate = null;
    private Date mEndDate = null;
    private long mStartTxRxBytes = 0;
    private long mEndTxRxBytes = 0;

    public NetSpeedInfo() {
        super();

        mUpdateJob = new Job(NetSpeedInfo.this) {
            @Override
            public void doJob() {
                // caculate neetwork speed
                float netSpeed = calculateNetworkSpeed();
                //
                synchronized (NetSpeedInfo.this) {
                    mNetworkSpeed = netSpeed;
                }
            }
        };
    }

    @Override
    public void onJobDone() {
        synchronized (NetSpeedInfo.this) {
            // invalid network speed
            if (this.mNetworkSpeed < 0) {
                return;
            }

            onInfoUpdate();
        }
    }

    @Override
    protected void startInfoUpdate() {
        mStartDate = null;
        mEndDate = null;
        mStartTxRxBytes = 0;
        mEndTxRxBytes = 0;

        mUpdateTimer = new Timer();
        mTimeTask = new TimerTask() {
            public void run() {
                Worker.getInstance().addJob(mUpdateJob);
            }
        };
        mUpdateTimer.schedule(this.mTimeTask, 0, UPDATE_INTERVAL);
    }

    @Override
    protected void stopInfoUpdate() {
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mTimeTask = null;
        }
    }

    private float calculateNetworkSpeed() {
        float netSpeed = -1;
        mEndDate = new Date();
        mEndTxRxBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();

        if (mStartDate != null) {
            long between = mEndDate.getTime() - mStartDate.getTime();
            if (0 != between) {
                netSpeed = (mEndTxRxBytes - mStartTxRxBytes) / between;
            }
        }

        mStartTxRxBytes = mEndTxRxBytes;
        mStartDate = mEndDate;

        return netSpeed;
    }

    public float getNetworkSpeed() {
        return this.mNetworkSpeed;
    }
}
