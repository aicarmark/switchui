/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2011/11/01   IKCTXTAW-359    Initial version
 *
 */

package com.motorola.datacollection.perfstats;

import android.util.Log;

public class NetworkDevice {
    private static final String TAG = "NetworkDevice";

    private final String mDeviceName;

    private long mBaseRxBytes, mBaseRxErrors, mBaseTxBytes, mBaseTxErrors;
    private long mCurrentRxBytes, mCurrentRxErrors, mCurrentTxBytes, mCurrentTxErrors;

    public NetworkDevice(String deviceName, String rxBytes, String rxErrors,
                         String txBytes, String txErrors) {
        mDeviceName = deviceName;

        try {
            mBaseRxBytes = Long.parseLong(rxBytes);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse base rxBytes");
        }

        try {
            mBaseRxErrors = Long.parseLong(rxErrors);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse base rxErrors");
        }

        try {
            mBaseTxBytes = Long.parseLong(txBytes);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse base txBytes");
        }

        try {
            mBaseTxErrors = Long.parseLong(txErrors);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse base txErrors");
        }

        /* for net read/write of 0 */
        mCurrentRxBytes = mBaseRxBytes;
        mCurrentRxErrors = mBaseRxErrors;
        mCurrentTxBytes = mBaseTxBytes;
        mCurrentTxErrors = mBaseTxErrors;
    }

    public void update(String rxBytes, String rxErrors, String txBytes, String txErrors) {
        try {
            mCurrentRxBytes = Long.parseLong(rxBytes);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse current rxBytes");
        }

        try {
            mCurrentRxErrors = Long.parseLong(rxErrors);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse current rxErrors");
        }

        try {
            mCurrentTxBytes = Long.parseLong(txBytes);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse current txBytes");
        }

        try {
            mCurrentTxErrors = Long.parseLong(txErrors);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Unable to parse current txErrors");
        }
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public long getRxBytes() {
        return mCurrentRxBytes - mBaseRxBytes;
    }

    public long getRxErrors() {
        return mCurrentRxErrors - mBaseRxErrors;
    }

    public long getTxBytes() {
        return mCurrentTxBytes - mBaseTxBytes;
    }

    public long getTxErrors() {
        return mCurrentTxErrors - mBaseTxErrors;
    }
}
