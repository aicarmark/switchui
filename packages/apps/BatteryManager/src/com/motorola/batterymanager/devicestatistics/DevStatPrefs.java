/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Following provides access to preference file
 */
public class DevStatPrefs {

    // Checkin Event Log ID
    public static final String CHECKIN_EVENT_ID = "MOT_DEVICE_STATS";
    public static final String VERSION = "2.3";

    // Preferences
    private static final String PREFS_FILE = "DevStatsPrefs";
    private static final String DEVSTATS_LAST_CHECKIN_TIME = "lastCheckinTime";
    private static final String DEVSTATS_WIFI_RXBYTES = "wifiRxBytes";
    private static final String DEVSTATS_WIFI_TXBYTES = "wifiTxBytes";
    private static final String DEVSTATS_WIFI_CUMRXBYTES = "wifiCumRxBytes";
    private static final String DEVSTATS_WIFI_CUMTXBYTES = "wifiCumTxBytes";
    private static final String DEVSTATS_MOBILE_RXBYTES = "mobileRxBytes";
    private static final String DEVSTATS_MOBILE_TXBYTES = "mobileTxBytes";
    private static final String DEVSTATS_MOBILE_CUMRXBYTES = "mobileCumRxBytes";
    private static final String DEVSTATS_MOBILE_CUMTXBYTES = "mobileCumTxBytes";

    private static final String DEVSTATS_MAX_LOG_SIZE = "maxlogsize";

    private static final String DEVSTATS_CHECKIN_FREQUENCY = "checkinFrequency";

    // Frequecy of checkin
    private static final long LAUNCH_PERIOD = 24L * 60L * 60L * 1000L; //24hrs
    private static final int DEF_MAX_LOG_SIZE = 4 * 1024; //4K

    private Context mContext;
    private static DevStatPrefs sInstance;
    private long mDevStatCheckinFreq = 0L;
    private int mMaxLogSize;
    SharedPreferences mPrefs;

    private DevStatPrefs(Context ctx) {
        mContext = ctx;
        initialisePrefs();
    }

    public static synchronized DevStatPrefs getInstance(Context context) {
       if (sInstance == null) {
           sInstance = new DevStatPrefs(context);
       }
       return sInstance;
    }

    private void initialisePrefs() {
        mPrefs = getPrefs();
        mDevStatCheckinFreq = mPrefs.getLong(DEVSTATS_CHECKIN_FREQUENCY, LAUNCH_PERIOD);
        mMaxLogSize = mPrefs.getInt(DEVSTATS_MAX_LOG_SIZE, DEF_MAX_LOG_SIZE);
    }

    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    private void updatePrefs(String prefId, long value) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong(prefId, value);
        editor.commit();
    }

    /**
     * Public methods to access Preferences 
     */
    public int getMaxLogSize() { return mMaxLogSize; }

    public long getDevStatCheckinFrequency() { return mDevStatCheckinFreq; }

    public long getDevStatLastCheckinTime(long defaultValue) {
        return mPrefs.getLong(DEVSTATS_LAST_CHECKIN_TIME, defaultValue);
    }
    
    public long getWifiRxBytes() { 
        return mPrefs.getLong(DEVSTATS_WIFI_RXBYTES, 0L);
    }

    public long getWifiTxBytes() { 
        return mPrefs.getLong(DEVSTATS_WIFI_TXBYTES, 0L);
    }

    public long getWifiCumRxBytes() { 
        return mPrefs.getLong(DEVSTATS_WIFI_CUMRXBYTES, 0L);
    }

    public long getWifiCumTxBytes() { 
        return mPrefs.getLong(DEVSTATS_WIFI_CUMTXBYTES, 0L);
    }

    public long getMobileRxBytes() { 
        return mPrefs.getLong(DEVSTATS_MOBILE_RXBYTES, 0L);
    }   

    public long getMobileTxBytes() { 
        return mPrefs.getLong(DEVSTATS_MOBILE_TXBYTES, 0L);
    }   

    public long getMobileCumRxBytes() { 
        return mPrefs.getLong(DEVSTATS_MOBILE_CUMRXBYTES, 0L);
    }   

    public long getMobileCumTxBytes() { 
        return mPrefs.getLong(DEVSTATS_MOBILE_CUMTXBYTES, 0L);
    }   

    public void setDevStatLastCheckinTime(long time) {
        updatePrefs(DEVSTATS_LAST_CHECKIN_TIME, time);
    }

    public void setWifiRxBytes(long bytes) {
        updatePrefs(DEVSTATS_WIFI_RXBYTES, bytes);
    }

    public void setWifiTxBytes(long bytes) {
        updatePrefs(DEVSTATS_WIFI_TXBYTES, bytes);
    }

    public void setWifiCumRxBytes(long bytes) {
        updatePrefs(DEVSTATS_WIFI_CUMRXBYTES, bytes);
    }

    public void setWifiCumTxBytes(long bytes) {
        updatePrefs(DEVSTATS_WIFI_CUMTXBYTES, bytes);
    }

    public void setMobileRxBytes(long bytes) {
        updatePrefs(DEVSTATS_MOBILE_RXBYTES, bytes);
    }

    public void setMobileTxBytes(long bytes) {
        updatePrefs(DEVSTATS_MOBILE_TXBYTES, bytes);
    }

    public void setMobileCumRxBytes(long bytes) {
        updatePrefs(DEVSTATS_MOBILE_CUMRXBYTES, bytes);
    }

    public void setMobileCumTxBytes(long bytes) {
        updatePrefs(DEVSTATS_MOBILE_CUMTXBYTES, bytes);
    }
}
