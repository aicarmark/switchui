/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Following provides access to preference file
 */
public class DevStatPrefs {

    // Checkin Event Log ID
    public static final String CHECKIN_EVENT_ID = "MOT_DEVICE_STATS";
    public static final String CHECKIN_EVENT_ID_BYCHARGER = "MOT_DEVICE_STATS_CHG";
    public static final String BATTERY_STATS_TAG = "MOT_DEVICE_STATS_BAT";
    public static final String VERSION = "4.7";
    public static final String DATASIZES_VERSION = "4.8";
    public static final String PERUIDSTATS_VERSION = "4.8";

    // Startup reasons - Bit Field - bit position defs follows - when 
    //                   adding something mind the mask
    // Bit Position - Description
    // 0            - Batt cap has changed,new cap to be logged
    // 1            - Device stats need to be logged 
    // 2            - App usage to be logged
    // 3            - Package Maps dump to be logged
    public static final int STARTUP_NO_LOG = 0;
    public static final int STARTUP_MASK = 0xFF; // 8 values allowed
    public static final int STARTUP_BATT_CAP_LOG = 1;
    public static final int STARTUP_DEV_STATS_LOG = 2;
    public static final int STARTUP_APP_STATS_LOG = 4;
    public static final int STARTUP_PMAPS_LOG = 8;

    // Batt cap settings
    public static final String DEVSTATS_BATT_CAP = "battCap";
    public static final String DEVSTATS_BATTCAP_TIME = "BattCapReportTime";
    public static final String DEVSTATS_APPSTATS_REFTIME = "lastAppStatsPollTime";
    public static final String DEVSTATS_PMAPS_REFTIME = "lastPmapsDumpTime";
    public static final String DEVSTATS_BATTRST_HINT = "battResetHint";

    // Preferences
    public static final String PREFS_FILE = "DevStatsPrefs";
    private static final String DEVSTATS_LAST_CHECKIN_TIME = "lastCheckinTime";
    private static final String DEVSTATS_WIFI_RXBYTES = "wifiRxBytes";
    private static final String DEVSTATS_WIFI_TXBYTES = "wifiTxBytes";
    private static final String DEVSTATS_WIFI_CUMRXBYTES = "wifiCumRxBytes2";
    private static final String DEVSTATS_WIFI_CUMTXBYTES = "wifiCumTxBytes2";
    private static final String DEVSTATS_MOBILE_RXBYTES = "mobileRxBytes";
    private static final String DEVSTATS_MOBILE_TXBYTES = "mobileTxBytes";
    private static final String DEVSTATS_MOBILE_CUMRXBYTES = "mobileCumRxBytes2";
    private static final String DEVSTATS_MOBILE_CUMTXBYTES = "mobileCumTxBytes2";

    private static final String DEVSTATS_WIFI_RXPKTS = "wifiRxPkts";
    private static final String DEVSTATS_WIFI_TXPKTS = "wifiTxPkts";
    private static final String DEVSTATS_WIFI_CUMRXPKTS = "wifiCumRxPkts";
    private static final String DEVSTATS_WIFI_CUMTXPKTS = "wifiCumTxPkts";
    private static final String DEVSTATS_MOBILE_RXPKTS = "mobileRxPkts";
    private static final String DEVSTATS_MOBILE_TXPKTS = "mobileTxPkts";
    private static final String DEVSTATS_MOBILE_CUMRXPKTS = "mobileCumRxPkts";
    private static final String DEVSTATS_MOBILE_CUMTXPKTS = "mobileCumTxPkts";

    private static final String DEVSTATS_MAX_LOG_SIZE = "maxlogsize";

    private static final String DEVSTATS_CHECKIN_FREQUENCY = "checkinFrequency";

    // Frequecy of checkin
    //private static final long LAUNCH_PERIOD = 15L * 60L * 1000L; //24hrs
    private static final long LAUNCH_PERIOD = 24L * 60L * 60L * 1000L; //24hrs
    private static final int DEF_MAX_LOG_SIZE = 4 * 1024; //4K

    // Battery age reporting
    public static final String DEVSTATS_BATTERY_CYCLECOUNT = "batteryCycleCount";
    public static final String DEVSTATS_BATTERY_TIMESTAMP_SEC = "BatteryCycleTimestamp";

    public static final String DEVSTATS_GPS_TIME = "gpsTime";

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
           sInstance = new DevStatPrefs(context.getApplicationContext());
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
        Utils.saveSharedPreferences(editor);
    }

    private void updatePrefs(String prefId, String value) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(prefId, value);
        Utils.saveSharedPreferences(editor);
    }

    /**
     * Public methods to access Preferences 
     */
    public int getMaxLogSize() { return mMaxLogSize; }

    public long getDevStatCheckinFrequency() { return mDevStatCheckinFreq; }

    public long getDevStatLastDailyCheckinTime(long defaultValue) {
        return mPrefs.getLong(DEVSTATS_LAST_CHECKIN_TIME, defaultValue);
    }
    
    public long getWifiRxBytes() {
        return mPrefs.getLong(DEVSTATS_WIFI_RXBYTES, 0L);
    }

    public long getWifiTxBytes() {
        return mPrefs.getLong(DEVSTATS_WIFI_TXBYTES, 0L);
    }

    private long[] getThreeLongsFromPrefs(String prefKey) {
        String encodedString = null;
        try {
            encodedString = mPrefs.getString(prefKey, null);
        } catch (ClassCastException e) {
            // Ignore, encodedString will be null
        }

        long[] result = (long[])DevStatUtils.deSerializeObject(encodedString, long[].class);
        if ( result == null || result.length != 3 ) result = new long[3];
        return result;
    }

    public long[] getWifiCumRxBytes() {
        return getThreeLongsFromPrefs(DEVSTATS_WIFI_CUMRXBYTES);
    }

    public long[] getWifiCumTxBytes() {
        return getThreeLongsFromPrefs(DEVSTATS_WIFI_CUMTXBYTES);
    }

    public long getMobileRxBytes() { 
        return mPrefs.getLong(DEVSTATS_MOBILE_RXBYTES, 0L);
    }

    public long getMobileTxBytes() { 
        return mPrefs.getLong(DEVSTATS_MOBILE_TXBYTES, 0L);
    }

    public long[] getMobileCumRxBytes() {
        return getThreeLongsFromPrefs(DEVSTATS_MOBILE_CUMRXBYTES);
    }

    public long[] getMobileCumTxBytes() {
        return getThreeLongsFromPrefs(DEVSTATS_MOBILE_CUMTXBYTES);
    }

    public long getWifiRxPkts() {
        return mPrefs.getLong(DEVSTATS_WIFI_RXPKTS, 0L);
    }

    public long getWifiTxPkts() {
        return mPrefs.getLong(DEVSTATS_WIFI_TXPKTS, 0L);
    }

    public long[] getWifiCumRxPkts() {
        return getThreeLongsFromPrefs(DEVSTATS_WIFI_CUMRXPKTS);
    }

    public long[] getWifiCumTxPkts() {
        return getThreeLongsFromPrefs(DEVSTATS_WIFI_CUMTXPKTS);
    }

    public long getMobileRxPkts() {
        return mPrefs.getLong(DEVSTATS_MOBILE_RXPKTS, 0L);
    }

    public long getMobileTxPkts() {
        return mPrefs.getLong(DEVSTATS_MOBILE_TXPKTS, 0L);
    }

    public long[] getMobileCumRxPkts() {
        return getThreeLongsFromPrefs(DEVSTATS_MOBILE_CUMRXPKTS);
    }

    public long[] getMobileCumTxPkts() {
        return getThreeLongsFromPrefs(DEVSTATS_MOBILE_CUMTXPKTS);
    }

    public void setDevStatLastDailyCheckinTime(long time) {
        updatePrefs(DEVSTATS_LAST_CHECKIN_TIME, time);
    }

    public void setWifiRxBytes(long bytes) {
        updatePrefs(DEVSTATS_WIFI_RXBYTES, bytes);
    }

    public void setWifiTxBytes(long bytes) {
        updatePrefs(DEVSTATS_WIFI_TXBYTES, bytes);
    }

    private void updateThreeLongsToPrefs(String prefId, long[] values) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(prefId, DevStatUtils.serializeObject(values));
        Utils.saveSharedPreferences(editor);
    }

    public void setWifiCumRxBytes(long[] bytes) {
        updateThreeLongsToPrefs(DEVSTATS_WIFI_CUMRXBYTES, bytes);
    }

    public void setWifiCumTxBytes(long[] bytes) {
        updateThreeLongsToPrefs(DEVSTATS_WIFI_CUMTXBYTES, bytes);
    }

    public void setMobileRxBytes(long bytes) {
        updatePrefs(DEVSTATS_MOBILE_RXBYTES, bytes);
    }

    public void setMobileTxBytes(long bytes) {
        updatePrefs(DEVSTATS_MOBILE_TXBYTES, bytes);
    }

    public void setMobileCumRxBytes(long[] bytes) {
        updateThreeLongsToPrefs(DEVSTATS_MOBILE_CUMRXBYTES, bytes);
    }

    public void setMobileCumTxBytes(long[] bytes) {
        updateThreeLongsToPrefs(DEVSTATS_MOBILE_CUMTXBYTES, bytes);
    }

    public void setWifiRxPkts(long pkts) {
        updatePrefs(DEVSTATS_WIFI_RXPKTS, pkts);
    }

    public void setWifiTxPkts(long pkts) {
        updatePrefs(DEVSTATS_WIFI_TXPKTS, pkts);
    }

    public void setWifiCumRxPkts(long[] pkts) {
        updateThreeLongsToPrefs(DEVSTATS_WIFI_CUMRXPKTS, pkts);
    }

    public void setWifiCumTxPkts(long[] pkts) {
        updateThreeLongsToPrefs(DEVSTATS_WIFI_CUMTXPKTS, pkts);
    }

    public void setMobileRxPkts(long pkts) {
        updatePrefs(DEVSTATS_MOBILE_RXPKTS, pkts);
    }

    public void setMobileTxPkts(long pkts) {
        updatePrefs(DEVSTATS_MOBILE_TXPKTS, pkts);
    }

    public void setMobileCumRxPkts(long[] pkts) {
        updateThreeLongsToPrefs(DEVSTATS_MOBILE_CUMRXPKTS, pkts);
    }

    public void setMobileCumTxPkts(long[] pkts) {
        updateThreeLongsToPrefs(DEVSTATS_MOBILE_CUMTXPKTS, pkts);
    }

    public String getStringSetting(String name) {
        return mPrefs.getString(name, null);
    }

    public long getLongSetting(String name, long defaultV) {
        return mPrefs.getLong(name, defaultV);
    }

    public void setStringSetting(String name, String value) {
        updatePrefs(name, value);
    }

    public void setLongSetting(String name, long newV) {
        updatePrefs(name, newV);
    }
}
