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

import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;

public class ProcessCpuData {
    private static final String TAG = "ProcCpuData";

    public final int mPid;
    private final String mBaseName;
    private String mProcessName;
    /* the times are in jiffies */
    private final long mBaseUserTime, mBaseSystemTime;
    public long mCurrentUserTime, mCurrentSystemTime;

    public ProcessCpuData(int pid, long userTime, long systemTime, String baseName) {
        mPid = pid;
        mBaseUserTime = userTime;
        mBaseSystemTime = systemTime;
        mBaseName = baseName;
    }

    public String getName() {
        if (mProcessName == null) {
            try {
                FileInputStream is = new FileInputStream("/proc/" + mPid + "/cmdline");
                byte[] buffer = new byte[256];
                int len = 0;
                try {
                    len = is.read(buffer);
                } finally {
                    is.close();
                }

                if (len > 0) {
                    int i;
                    for (i=0; i<len; i++) {
                        if (buffer[i] == '\0') {
                            break;
                        }
                    }
                    mProcessName = new String(buffer, 0, i);
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to parse process name for: " + mPid);
            }
        }
        /* sometimes cmdline process name is blank for system processes like events/0 */
        if (mProcessName == null) {
            /* for those processes with blank cmdline, use the baseName from stat file */
            mProcessName = mBaseName;
            Log.d(TAG, "Unable to get process name from cmdline. Using basename for: " +
                  mPid + " / " + mProcessName);
        }
        return mProcessName;
    }

    public long getUserTime() {
        long userTime = 0;
        if (mBaseUserTime > 0 && mCurrentUserTime > mBaseUserTime) {
            userTime = mCurrentUserTime - mBaseUserTime;
        }
        return userTime;
    }

    public long getSystemTime() {
        long systemTime = 0;
        if (mBaseSystemTime > 0 && mCurrentSystemTime > mBaseSystemTime) {
            systemTime = mCurrentSystemTime - mBaseSystemTime;
        }
        return systemTime;
    }
}
