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

import android.content.Context;

/**
 * the engine class for SystemProperty, which will provide data for upper layer
 * UI, and callback to UI if there's data update
 * 
 * @author dfb746
 */
public class SysPropEngine {
    private final static String TAG = "SystemMgrEngine";

    private static SysPropEngine mInstance = null;
    private static Context mContext = null;
    private CPUInfo mCpuInfo = null;
    private MemInfo mMemInfo = null;
    private BatteryInfo mBattInfo = null;

    private RunningStateInfo mRunningStateInfo = null;
    private CacheListInfo mCacheListInfo = null;

    public static void setContext(Context context) {
        if (SysPropEngine.mContext == null) {
            SysPropEngine.mContext = context;
        } else {
            Log.e(TAG, "Error SystemMgrEngine's context has been set");
        }
    }

    private SysPropEngine() {
        super();

        mCpuInfo = new CPUInfo(mContext);
        mMemInfo = new MemInfo();
        mBattInfo = new BatteryInfo(mContext);

        mRunningStateInfo = new RunningStateInfo(mContext);
        mCacheListInfo = new CacheListInfo(mContext);
    }

    public static SysPropEngine getInstance() {
        if (mContext == null) {
            Log.e(TAG, "Error, please first setContext before use SysPropEngine!");
            return null;
        }

        if (mInstance == null) {
            mInstance = new SysPropEngine();
        }

        return mInstance;
    }

    public CPUInfo getCpuInfo() {
        return mCpuInfo;
    }

    public MemInfo getMemInfo() {
        return mMemInfo;
    }

    public CacheListInfo getCacheListInfo() {
        return mCacheListInfo;
    }

    public RunningStateInfo getRunningStateInfo() {
        return mRunningStateInfo;
    }

    public BatteryInfo getBattInfo() {
        return mBattInfo;
    }

}
