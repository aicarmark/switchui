/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: BatteryStatsState.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Feb 2, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.BatteryStats;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.os.SystemClock;
import android.telephony.SignalStrength;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.os.BatteryStatsImpl;
import com.motorola.devicestatistics.DataAccumulator.AccumulatorPolicy;
import com.motorola.devicestatistics.StatsCollector.DataPolicy;
import com.motorola.devicestatistics.StatsCollector.StatsException;
import com.motorola.devicestatistics.TrafficCollectionService.UidInfo;
import com.motorola.devicestatistics.eventlogs.EventConstants;

/**
 * @author bluremployee
 *
 */
/*package*/ class BatteryStatsState {
    static class BatteryStatsConstants {

        // Same as package protected android.os.BatteryStats.SCREEN_BRIGHTNESS_NAMES
        final static String[] SCREEN_BRIGHTNESS_NAMES = getNamesByReflection(
                "android.os.BatteryStats", "SCREEN_BRIGHTNESS_NAMES", null );
        static final int NUM_SCREEN_BRIGHTNESS_BINS = SCREEN_BRIGHTNESS_NAMES.length;

        static final String[] SIGNAL_STRENGTH_NAMES =
            getArrayCopyWithPrefix(SignalStrength.SIGNAL_STRENGTH_NAMES, "ss_");
        static final int NUM_SIGNAL_STRENGTH_BINS = SIGNAL_STRENGTH_NAMES.length;

        // Same as package protected android.os.BatteryStats.DATA_CONNECTION_NAMES with "rt_" prefix
        private final static String[] DATA_CONN_NAMES = getNamesByReflection(
                "android.os.BatteryStats", "DATA_CONNECTION_NAMES", "rt_" );
        static final int NUM_DATA_CONNECTION_TYPES = DATA_CONN_NAMES.length;

        /**
         * Use java reflection to retrieve the values of the package protected field
         * in the specified class
         * @param className Name of class that has the specified field
         * @param fieldName Name of field whose value needs to be retrieved
         * @param prefix When non-null, a prefix that should be added to each element
         * of the array being returned
         * @return The string array containing the value of the requested field 
         */
        private final static String[] getNamesByReflection(String className, String fieldName,
                String prefix) {
            try {
                // Get class object for which we want the inaccessible field
                Class<?> batteryStats = Class.forName(className);

                // Get the field within the class
                Field f = batteryStats.getDeclaredField(fieldName);

                // Bypass Java access control
                f.setAccessible(true);

                // Get the value of the field. The argument is null because this is a static field
                String[] stringArray = (String[])f.get(null);

                // In data connection names, the fields need to be prefixed
                // with "rt_", like "rt_gprs"
                if ( prefix != null ) stringArray = getArrayCopyWithPrefix(stringArray, prefix);

                return stringArray;
            } catch (Exception e) {
                Log.e( TAG, Log.getStackTraceString(e) );
                return new String[0];
            }
        }

        /**
         * Get a copy of the passed array, with a prefix string added to each element
         * @param stringArray The array to be copied
         * @param A prefix that should be added to each element of the returned array
         * @return The copied array with prefix added to each element
         */
        private final static String[] getArrayCopyWithPrefix(String[] stringArray, String prefix) {
            String []prefixedStringArray = new String[stringArray.length];
            for (int i=0; i<stringArray.length; i++) {
                prefixedStringArray[i] = prefix + stringArray[i];
            }
            return prefixedStringArray;
        }
    }
    
    private final static boolean DUMP = true; // Leave this on for logging errors below
    private final static String TAG = "BStatsState";
    private final static int STAT_TYPE = BatteryStats.STATS_SINCE_CHARGED;
    
    static final class SensorData implements Serializable {
        private static final long serialVersionUID = 1L;

        long time;
        long count;

        public String toString() { return "{ time=" + time + ", count=" + count + " }"; }
    }

    static final class SensorHashMap extends HashMap<String,SensorData> {
        private static final long serialVersionUID = 1L;

        final SensorData getSensorData( String name ) {
            SensorData data = get( name );
            if ( data == null ) {
                data = new SensorData();
                put( name, data );
            }
            return data;
        }

        static final SensorHashMap copy(SensorHashMap s) {
            SensorHashMap result = new SensorHashMap();
            for ( Entry<String,SensorData> entry : s.entrySet() ) {
                SensorData resultData = result.getSensorData( entry.getKey() );
                SensorData addValue = entry.getValue();
                resultData.time  = addValue.time;
                resultData.count = addValue.count;
            }
            return result;
        }

        static final SensorHashMap add( SensorHashMap s1, SensorHashMap s2 ) {
            SensorHashMap result = copy(s1);

            for ( Entry<String,SensorData> entry : s2.entrySet() ) {
                SensorData resultData = result.getSensorData( entry.getKey() );
                SensorData addValue = entry.getValue();
                resultData.time  += addValue.time;
                resultData.count += addValue.count;
            }
            return result;
        }

        static final SensorHashMap subtract( SensorHashMap s1, SensorHashMap s2 ) {
            SensorHashMap result = copy(s1);

            for ( Entry<String,SensorData> entry : s2.entrySet() ) {
                SensorData resultData = result.getSensorData( entry.getKey() );
                SensorData addValue = entry.getValue();
                resultData.time  -= addValue.time;
                resultData.count -= addValue.count;
            }
            return result;
        }
    };

    static class UidState {
        //String mPackageName;
        int mUid;
        long mCpuTime;
        long mNetstatBytes;
        long mMobileNetstatBytes;
        long mWifiNetstatBytes;
        long mTcpOnBattery;
        long mMobileTcpOnBattery;
        long mWifiTcpOnBattery;
        long mPackets;
        long mFullWakelockTime, mFullWakelockCount, mPartialWakelockTime, mPartialWakelockCount;
        SensorHashMap mSensorData = new SensorHashMap();

        final SensorData getSensorData(String name) {
            SensorData data = mSensorData.get(name);
            if (data == null) {
                data = new SensorData();
                mSensorData.put(name,data);
            }
            return data;
        }
    }
    
    long mRefTime;
    long mBatteryRealtime, mBatteryUptime;
    long mTotalRealtime, mTotalUptime;
    long mGpsTimeSec;
    long mScreenOnTime;
    long mPhoneOnTime;
    long[] mScreenBrightnessTimes;
    long mInputEventCount;
    long mWifiOnTime, mWifiRunTime;
    long mBTOnTime;
    long mSignalScanTime;
    long[] mSignalStrengthTimes, mSignalStrengthCount;
    long[] mDataConnTimes, mDataConnCount;
    HashMap<Integer, UidState> mUidStates;
    
    public static BatteryStatsState createFromDatabase(StatsDatabase db) {
        BatteryStatsState state = new BatteryStatsState();
        state.loadTimes(db);
        state.loadUids(db);
        return state;
    }
    
    public static BatteryStatsState createFromSource(Object source, Context context)
            throws StatsException {
        if(source == null || !(source instanceof BatteryStatsImpl)) {
            throw new StatsException("Can't create battery " +
                    "stats state from what is not a stat source");
        }
        BatteryStatsImpl stats = (BatteryStatsImpl)source;
        
        BatteryStatsState state = new BatteryStatsState();
        
        final long rawRealtime = SystemClock.elapsedRealtime() * 1000; // microsec
        final long rawUptime = SystemClock.uptimeMillis() * 1000; // microsec
        
        long batteryRealtime = stats.computeBatteryRealtime(rawRealtime, STAT_TYPE);
        state.collectTimeStats(stats, rawRealtime, rawUptime, batteryRealtime, context);
        state.collectUidStats(stats, batteryRealtime);
        return state;
    }
    
    public boolean isValid() {
        return mTotalRealtime > 0;
    }
    
    public void commitToDatabase(StatsDatabase db) {
        commitTimes(db);
        commitUids(db);
    }
    
    public boolean compare(BatteryStatsState other) {
        // In the caller, this is laststate, and other is currentstate

        // use the "simple" conflict resolver to decide.
        // this fn is called with "this" being the last state
        // and "other" being the new current state
        // TODO: Use battery time for now, should ideally use total realtime
        //       but FW doesn't reset that
        /*
        if(DUMP) Log.v(TAG, "compare inputs:" + mTotalRealtime + ","
                + other.mTotalRealtime + "," + mRefTime + "," + other.mRefTime);
        int policy = AccumulatorPolicy.
                sSimpleConflictResolver.getPolicy(
                        mTotalRealtime, other.mTotalRealtime, mRefTime, other.mRefTime);
        if(DUMP) Log.v(TAG, "compare inputs:" + mBatteryRealtime + ","
                + other.mBatteryRealtime + "," + mRefTime + "," + other.mRefTime);
        */
        int policy = AccumulatorPolicy.
                sSimpleConflictResolver.getPolicy(
                        mBatteryRealtime, other.mBatteryRealtime, mRefTime, other.mRefTime);
        boolean add = false;
        if(policy == DataPolicy.PASS_THROUGH) {
            add = false;
        }else if(policy == DataPolicy.CUMULATIVE) {
            // do a diff
            add = !subtract(other);
         }else if(policy == DataPolicy.NON_CUMULATIVE) {
            // trt_tr/trt_ut etc are computed by devicestatistics, and never reset even if android
            // framework data resets. Even if the rest of the data is going to be added, these
            // fields should still be diff-ed.
            diffOnFrameworkReset(other);

            // do a copy
            duplicate(other);
            add = true;
        }
        return add;
    }
    
    public void addFrom(BatteryStatsState other) {
        mRefTime = other.mRefTime;
        mBatteryRealtime += other.mBatteryRealtime;
        mBatteryUptime += other.mBatteryUptime;
        mTotalRealtime += other.mTotalRealtime;
        mTotalUptime += other.mTotalUptime;
        mGpsTimeSec += other.mGpsTimeSec;
        mScreenOnTime += other.mScreenOnTime;
        mPhoneOnTime +=other.mPhoneOnTime;
        if(mScreenBrightnessTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS; ++i) {
                mScreenBrightnessTimes[i] += other.mScreenBrightnessTimes[i];
            }
        }else {
            mScreenBrightnessTimes = other.mScreenBrightnessTimes;
        }
        mInputEventCount += other.mInputEventCount;
        mWifiOnTime += other.mWifiOnTime;
        mWifiRunTime += other.mWifiRunTime;
        mBTOnTime += other.mBTOnTime;
        mSignalScanTime += other.mSignalScanTime;
        if(mSignalStrengthTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                mSignalStrengthTimes[i] += other.mSignalStrengthTimes[i];
            }
        }else {
            mSignalStrengthTimes = other.mSignalStrengthTimes;
        }
        
        if(mSignalStrengthCount != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                mSignalStrengthCount[i] += other.mSignalStrengthCount[i];
            }
        }else {
            mSignalStrengthCount = other.mSignalStrengthCount;
        }
        
        if(mDataConnTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
                mDataConnTimes[i] += other.mDataConnTimes[i];
            }
        }else {
            mDataConnTimes = other.mDataConnTimes;
        }
        
        if(mDataConnCount != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
                mDataConnCount[i] += other.mDataConnCount[i];
            }
        }else {
            mDataConnCount = other.mDataConnCount;
        }
        if(mUidStates != null) {
            if(other.mUidStates != null) {
                Iterator<UidState> iterator = other.mUidStates.values().iterator();
                while(iterator.hasNext()) {
                    UidState ou = iterator.next();
                    UidState u = mUidStates.get(ou.mUid);
                    if(u == null) {
                        u = new UidState();
                        u.mUid = ou.mUid;
                        mUidStates.put(u.mUid, u);
                    }

                    u.mCpuTime += ou.mCpuTime;
                    u.mNetstatBytes += ou.mNetstatBytes;
                    u.mMobileNetstatBytes += ou.mMobileNetstatBytes;
                    u.mWifiNetstatBytes += ou.mWifiNetstatBytes;
                    u.mSensorData = SensorHashMap.add( u.mSensorData, ou.mSensorData );
                    u.mTcpOnBattery += ou.mTcpOnBattery;
                    u.mMobileTcpOnBattery += ou.mMobileTcpOnBattery;
                    u.mWifiTcpOnBattery += ou.mWifiTcpOnBattery;
                    u.mPackets += ou.mPackets;
                    u.mFullWakelockTime += ou.mFullWakelockTime;
                    u.mFullWakelockCount += ou.mFullWakelockCount;
                    u.mPartialWakelockTime += ou.mPartialWakelockTime;
                    u.mPartialWakelockCount += ou.mPartialWakelockCount;
                }
            }
        }else {
            mUidStates = other.mUidStates;
        }
    }
    
    private boolean subtract(BatteryStatsState other) {
        final boolean DEBUG_SANITY = true;

        StringBuilder sb;
        if(DEBUG_SANITY && DUMP) sb = new StringBuilder();
    
        mRefTime = other.mRefTime;    
        mBatteryRealtime = other.mBatteryRealtime - mBatteryRealtime;
        mBatteryUptime = other.mBatteryUptime - mBatteryUptime;
        long diffTotalRealtime = other.mTotalRealtime - mTotalRealtime;
        long diffTotalUptime = other.mTotalUptime - mTotalUptime;
        mTotalRealtime = diffTotalRealtime;
        mTotalUptime = diffTotalUptime;

        long diffGpsTimeSec = other.mGpsTimeSec - mGpsTimeSec;
        mGpsTimeSec = diffGpsTimeSec;

        mScreenOnTime = other.mScreenOnTime - mScreenOnTime;
        mPhoneOnTime = other.mPhoneOnTime - mPhoneOnTime;

        if(DEBUG_SANITY) {
            if(mBatteryRealtime < 0 || mBatteryUptime < 0 || mScreenOnTime < 0 || mPhoneOnTime < 0) {
                if(DUMP) {
                    sb.append("subtract invalid times:").append(mBatteryRealtime)
                        .append(";").append(other.mBatteryRealtime).append(";")
                        .append(mBatteryUptime).append(";").append(other.mBatteryUptime)
                        .append(";").append(diffTotalRealtime).append(";").append(other.mTotalRealtime)
                        .append(";").append(diffTotalUptime).append(";").append(other.mTotalUptime)
                        .append(";").append(diffGpsTimeSec).append(";").append(other.mGpsTimeSec)
                        .append(";").append(mScreenOnTime).append(";").append(other.mScreenOnTime)
                        .append(";").append(mPhoneOnTime).append(";").append(other.mPhoneOnTime);
                    Log.v(TAG, sb.toString()); 
                }
                return true;
            }
        }
        
        if(mScreenBrightnessTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS; ++i) {
                mScreenBrightnessTimes[i] = other.mScreenBrightnessTimes[i] -
                        mScreenBrightnessTimes[i];
                if(DEBUG_SANITY && mScreenBrightnessTimes[i] < 0) {
                    if(DUMP) {
                        sb.append("subtract invalid screen bright time:").append(i)
                                .append(";").append(mScreenBrightnessTimes[i])
                                .append(";").append(other.mScreenBrightnessTimes[i]);
                        Log.v(TAG, sb.toString());
                    }
                    return true;
                }
            }
        }else {
            mScreenBrightnessTimes = other.mScreenBrightnessTimes;
        }
        mInputEventCount = other.mInputEventCount - mInputEventCount;
        mWifiOnTime = other.mWifiOnTime - mWifiOnTime;
        mWifiRunTime = other.mWifiRunTime - mWifiRunTime;
        mBTOnTime = other.mBTOnTime - mBTOnTime;
        mSignalScanTime = other.mSignalScanTime - mSignalScanTime;
        
        if(DEBUG_SANITY) {
            if(mWifiOnTime < 0 || mWifiRunTime < 0 || mBTOnTime < 0 ||
                    mSignalScanTime < 0) {
                if(DUMP) {
                    sb.append("subtract invalid wifi/bt times:").append(mWifiOnTime)
                            .append(";").append(other.mWifiOnTime).append(";")
                            .append(mWifiRunTime).append(";").append(other.mWifiRunTime)
                            .append(";").append(mBTOnTime).append(";").append(other.mBTOnTime)
                            .append(";").append(mSignalScanTime).append(";").append(other.mSignalScanTime);
                    Log.v(TAG, sb.toString());
                }
                return true;
            }
        }
        
        if(mSignalStrengthTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                mSignalStrengthTimes[i] = other.mSignalStrengthTimes[i] -
                        mSignalStrengthTimes[i];
                if(DEBUG_SANITY && mSignalStrengthTimes[i] < 0) {
                    if(DUMP) {
                        sb.append("subtract invalid signal times:").append(i)
                                .append(";").append(mSignalStrengthTimes[i]).append(";")
                                .append(other.mSignalStrengthTimes[i]);
                        Log.v(TAG, sb.toString());
                    }
                    return true;
                }
            }
        }else {
            mSignalStrengthTimes = other.mSignalStrengthTimes;
        }
        
        if(mSignalStrengthCount != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                mSignalStrengthCount[i] = other.mSignalStrengthCount[i] -
                        mSignalStrengthCount[i];
                if ( mSignalStrengthCount[i] == -1 ) mSignalStrengthCount[i] = 0;
                if(DEBUG_SANITY && mSignalStrengthCount[i] < 0) {
                    if(DUMP) {
                        sb.append("subtract invalid signal count:").append(i)
                                .append(";").append(mSignalStrengthCount[i]).append(";")
                                .append(other.mSignalStrengthCount[i]);
                        Log.v(TAG, sb.toString());
                    }
                    return true;
                }
            }
        }else {
            mSignalStrengthCount = other.mSignalStrengthCount;
        }
        
        if(mDataConnTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
                mDataConnTimes[i] = other.mDataConnTimes[i] -
                        mDataConnTimes[i];
                if(DEBUG_SANITY && mDataConnTimes[i] < 0) {
                    if(DUMP) {
                        sb.append("subtract invalid data times:").append(i)
                                .append(";").append(mDataConnTimes[i]).append(";")
                                .append(other.mDataConnTimes[i]);
                        Log.v(TAG, sb.toString());
                    }
                    return true;
                }
            }
        }else {
            mDataConnTimes = other.mDataConnTimes;
        }
        
        if(mDataConnCount != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
                mDataConnCount[i] = other.mDataConnCount[i] -
                        mDataConnCount[i];
                if(DEBUG_SANITY && mDataConnCount[i] < 0) {
                    if(DUMP) {
                        sb.append("subtract invalid data count:").append(i)
                                .append(";").append(mDataConnCount[i]).append(";")
                                .append(other.mDataConnCount[i]);
                        Log.v(TAG, sb.toString());
                    }
                    return true;
                }
            }
        }else {
            mDataConnCount = other.mDataConnCount;
        }
        if(mUidStates != null) {
            int did = 0;
            Iterator<UidState> iterator = other.mUidStates.values().iterator();
            while(iterator.hasNext()) {
                UidState ou = iterator.next();
                UidState u = mUidStates.get(ou.mUid);
                if(u != null) {
                    did++;
                } else {
                    u = new UidState();
                    u.mUid = ou.mUid;
                    mUidStates.put(u.mUid, u);
                }

                u.mCpuTime = ou.mCpuTime - u.mCpuTime;
                u.mNetstatBytes = ou.mNetstatBytes - u.mNetstatBytes;
                u.mMobileNetstatBytes = ou.mMobileNetstatBytes -u.mMobileNetstatBytes;
                u.mWifiNetstatBytes = ou.mWifiNetstatBytes - u.mWifiNetstatBytes;
                u.mSensorData = SensorHashMap.subtract(ou.mSensorData, u.mSensorData);
                boolean negativeSensorTime = false;
                for ( SensorData sensorData : u.mSensorData.values() ) {
                    if ( sensorData.time < 0 ) negativeSensorTime = true;
                }
                u.mTcpOnBattery = ou.mTcpOnBattery - u.mTcpOnBattery;
                u.mMobileTcpOnBattery = ou.mMobileTcpOnBattery - u.mMobileTcpOnBattery;
                u.mWifiTcpOnBattery = ou.mWifiTcpOnBattery - u.mWifiTcpOnBattery;
                u.mPackets = ou.mPackets - u.mPackets;
                u.mFullWakelockTime = ou.mFullWakelockTime - u.mFullWakelockTime;
                u.mFullWakelockCount = ou.mFullWakelockCount - u.mFullWakelockCount;
                u.mPartialWakelockTime = ou.mPartialWakelockTime- u.mPartialWakelockTime;
                u.mPartialWakelockCount = ou.mPartialWakelockCount - u.mPartialWakelockCount;
                if(DEBUG_SANITY) {
                    // Note that the "counts" are not checked in the code below.
                    // In BatteryStatsImpl.StopwatchTimer.stopRunningLocked, the count is
                    // sometimes decremented. So for a wakelock that is acquired and released
                    // frequently, the "count" sometimes goes back and forth. The same applies
                    // to sensor "counts". So the "count"s are not checked here.
                    if (u.mCpuTime < 0 || u.mNetstatBytes < 0 || u.mMobileNetstatBytes < 0
                            || u.mWifiNetstatBytes < 0 || negativeSensorTime ||
                            u.mTcpOnBattery < 0 || u.mMobileTcpOnBattery < 0
                            || u.mWifiTcpOnBattery < 0 || u.mFullWakelockTime < 0 ||
                            u.mPartialWakelockTime < 0 || u.mPackets < 0) {
                        if (DUMP) {
                            sb.append("subtract invalid uid values:").append(u.mUid)
                                    .append(";").append(u.mCpuTime).append(";").append(ou.mCpuTime)
                                    .append(";").append(u.mNetstatBytes)
                                    .append(";").append(ou.mNetstatBytes)
                                    .append(";").append(u.mMobileNetstatBytes)
                                    .append(";").append(ou.mMobileNetstatBytes)
                                    .append(";").append(u.mWifiNetstatBytes)
                                    .append(";").append(ou.mWifiNetstatBytes)
                                    .append(';').append("u=").append(u.mSensorData)
                                    .append(';').append("ou=").append(ou.mSensorData)
                                    .append(";").append(u.mTcpOnBattery).append(";")
                                    .append(ou.mTcpOnBattery)
                                    .append(";").append(u.mMobileTcpOnBattery)
                                    .append(";").append(ou.mMobileTcpOnBattery)
                                    .append(";").append(u.mWifiTcpOnBattery)
                                    .append(";").append(ou.mWifiTcpOnBattery)
                                    .append(";").append(u.mPackets)
                                    .append(";").append(ou.mPackets)
                                    .append(";").append(u.mFullWakelockTime)
                                    .append(";").append(ou.mFullWakelockTime)
                                    .append(";").append(u.mPartialWakelockTime)
                                    .append(";").append(ou.mPartialWakelockTime);
                            Log.v(TAG, sb.toString());
                            sb.setLength(0);
                        }
                        u.mCpuTime = u.mCpuTime < 0 ? 0 : u.mCpuTime;
                        // Bytes per Uid is computed from reboot and reset
                        // whenever bootup.
                        // Current bytes won't less than that in last db
                        // except the first time to do accumulation since
                        // bootup.
                        // I supply current bytes rather than 0 to avoid
                        // subtract the first time at bootup is missed.
                        u.mNetstatBytes = u.mNetstatBytes < 0 ? ou.mNetstatBytes : u.mNetstatBytes;
                        u.mMobileNetstatBytes = u.mMobileNetstatBytes < 0 ? ou.mMobileNetstatBytes
                                : u.mMobileNetstatBytes;
                        u.mWifiNetstatBytes = u.mWifiNetstatBytes < 0 ? ou.mWifiNetstatBytes
                                : u.mWifiNetstatBytes;
                        for (Entry<String, SensorData> entry : u.mSensorData.entrySet()) {
                            SensorData sensorData = entry.getValue();
                            if (sensorData.time < 0) {
                                sensorData.time = 0;
                            }
                        }
                        // u.mTcpOnBattery=u.mTcpOnBattery<0 ? ou.mTcpOnBattery : u.mTcpOnBattery;
                        // Revert mTcpOnBattery back to find why if there is a discrepancy
                        // between byt and byt_mobile/byt_wifi.
                        u.mTcpOnBattery = u.mTcpOnBattery < 0 ? 0 : u.mTcpOnBattery;
                        u.mMobileTcpOnBattery = u.mMobileTcpOnBattery < 0 ? ou.mMobileTcpOnBattery
                                : u.mMobileTcpOnBattery;
                        u.mWifiTcpOnBattery = u.mWifiTcpOnBattery < 0 ? ou.mWifiTcpOnBattery
                                : u.mWifiTcpOnBattery;
                        u.mPackets = u.mPackets < 0 ? ou.mPackets : u.mPackets;
                        u.mFullWakelockTime = u.mFullWakelockTime < 0 ? 0 : u.mFullWakelockTime;
                        u.mPartialWakelockTime =
                                u.mPartialWakelockTime < 0 ? 0 : u.mPartialWakelockTime;
                        // return true;
                    }
                }
            }

            if(!(did == mUidStates.size())) {
                iterator = mUidStates.values().iterator();
                while(iterator.hasNext()) {
                    UidState u = iterator.next();
                    UidState ou = other.mUidStates.get(u.mUid);
                    if(ou == null) {
                        // this uid is no longer present - zero out to remove traces
                        u.mCpuTime = 0;
                        u.mNetstatBytes = 0;
                        u.mMobileNetstatBytes = 0;
                        u.mWifiNetstatBytes = 0;
                        u.mSensorData.clear();
                        u.mTcpOnBattery = 0;
                        u.mMobileTcpOnBattery = 0;
                        u.mWifiTcpOnBattery = 0;
                        u.mPackets = 0;
                        u.mFullWakelockTime = 0;
                        u.mFullWakelockCount = 0;
                        u.mPartialWakelockTime = 0;
                        u.mPartialWakelockCount = 0;
                    }
                }
            }
        }else {
            mUidStates = other.mUidStates;
        }
        return false;
    }
    
    private void duplicate(BatteryStatsState other) {
        /*
         * We do straight reference copies (especially in this function)
         * we do this because we know the flow forward at the upper level
         * will not screw this up - remember this while making any changes
         * in the future
         */
        mRefTime = other.mRefTime;
        mBatteryRealtime = other.mBatteryRealtime;
        mBatteryUptime = other.mBatteryUptime;
        mTotalRealtime = other.mTotalRealtime;
        mTotalUptime = other.mTotalUptime;
        mGpsTimeSec = other.mGpsTimeSec;
        mScreenOnTime = other.mScreenOnTime;
        mPhoneOnTime = other.mPhoneOnTime;
        mScreenBrightnessTimes = other.mScreenBrightnessTimes;
        mInputEventCount = other.mInputEventCount;
        mWifiOnTime = other.mWifiOnTime;
        mWifiRunTime = other.mWifiRunTime;
        mBTOnTime = other.mBTOnTime;
        mSignalScanTime = other.mSignalScanTime;
        mSignalStrengthTimes = other.mSignalStrengthTimes;
        mSignalStrengthCount = other.mSignalStrengthCount;
        mDataConnTimes = other.mDataConnTimes;
        mDataConnCount = other.mDataConnCount;
        mUidStates = other.mUidStates;
    }
    
    private void commitTimes(StatsDatabase db) {
        final String GROUP = "default";
        final String SUBID = "default";
        
        db.putValue(GROUP, SUBID, "bstat_reftime", Long.toString(mRefTime));
        db.putValue(GROUP, SUBID, "trt_rt", Long.toString(mTotalRealtime));
        db.putValue(GROUP, SUBID, "trt_ut", Long.toString(mTotalUptime));
        db.putValue(GROUP, SUBID, "gps_rt", Long.toString(mGpsTimeSec));
        db.putValue(GROUP, SUBID, "tob_rt", Long.toString(mBatteryRealtime));
        db.putValue(GROUP, SUBID, "tob_ut", Long.toString(mBatteryUptime));
        
        db.putValue(GROUP, SUBID, "sc_ot", Long.toString(mScreenOnTime));
        db.putValue(GROUP, SUBID, "ph_ot",Long.toString(mPhoneOnTime));
        if(mScreenBrightnessTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS; ++i) {
                db.putValue(GROUP, SUBID, BatteryStatsConstants.SCREEN_BRIGHTNESS_NAMES[i],
                        Long.toString(mScreenBrightnessTimes[i]));
            }
        }
        
        db.putValue(GROUP, SUBID, "wifi_ot", Long.toString(mWifiOnTime));
        db.putValue(GROUP, SUBID, "wifi_rt", Long.toString(mWifiRunTime));
        db.putValue(GROUP, SUBID, "bt_ot", Long.toString(mBTOnTime));
        db.putValue(GROUP, SUBID, "ipe", Long.toString(mInputEventCount));
        
        db.putValue(GROUP, SUBID, "sst", Long.toString(mSignalScanTime));
        if(mSignalStrengthTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                db.putValue(GROUP, SUBID,
                        BatteryStatsConstants.SIGNAL_STRENGTH_NAMES[i] + "_t",
                        Long.toString(mSignalStrengthTimes[i]));
            }
        }
        if(mSignalStrengthCount != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                db.putValue(GROUP, SUBID,
                        BatteryStatsConstants.SIGNAL_STRENGTH_NAMES[i] + "_cnt",
                        Long.toString(mSignalStrengthCount[i]));
            }
        }
        if(mDataConnTimes != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
                db.putValue(GROUP, SUBID,
                        BatteryStatsConstants.DATA_CONN_NAMES[i] + "_t",
                        Long.toString(mDataConnTimes[i]));
            }
        }
        if(mDataConnCount != null) {
            for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
                db.putValue(GROUP, SUBID,
                        BatteryStatsConstants.DATA_CONN_NAMES[i] + "_cnt",
                        Long.toString(mDataConnCount[i]));
            }
        }
    }
    
    private void commitUids(StatsDatabase db) {
        if(DUMP) {
            Log.d(TAG, "commitUids");
        }
        final String GROUP = "UID";
        
        if(mUidStates != null) {
            Iterator<UidState> iterator = 
                    mUidStates.values().iterator();
            while(iterator.hasNext()) {
                UidState u = iterator.next();
                String uid = Integer.toString(u.mUid);
                db.putValue(GROUP, uid, "u", Integer.toString(u.mUid));
                db.putValue(GROUP, uid, "cpu", Long.toString(u.mCpuTime));
                db.putValue(GROUP, uid, "nsb", Long.toString(u.mNetstatBytes));
                db.putValue(GROUP, uid, "nsb_mobile", Long.toString(u.mMobileNetstatBytes));
                db.putValue(GROUP, uid, "nsb_wifi", Long.toString(u.mWifiNetstatBytes));
                String encodedSensorData = DevStatUtils.serializeObject( u.mSensorData );
                if ( encodedSensorData != null ) {
                    db.putValue(GROUP, uid, "sensordata", encodedSensorData);
                }
                db.putValue(GROUP, uid, "byt", Long.toString(u.mTcpOnBattery));
                db.putValue(GROUP, uid, "byt_mobile", Long.toString(u.mMobileTcpOnBattery));
                db.putValue(GROUP, uid, "byt_wifi", Long.toString(u.mWifiTcpOnBattery));
                db.putValue(GROUP, uid, "pkt", Long.toString(u.mPackets));
                db.putValue(GROUP, uid, "fwt", Long.toString(u.mFullWakelockTime));
                db.putValue(GROUP, uid, "fwc", Long.toString(u.mFullWakelockCount));
                db.putValue(GROUP, uid, "pwt", Long.toString(u.mPartialWakelockTime));
                db.putValue(GROUP, uid, "pwc", Long.toString(u.mPartialWakelockCount));
            }
        }
    }
    
    /**
     * @param stats
     * @param batteryRealtime
     */
    private void collectUidStats(BatteryStatsImpl stats,
            long batteryRealtime) {
        SparseArray<? extends BatteryStats.Uid> uidStats = stats.getUidStats();
        final int NU = uidStats.size();
        mUidStates = new HashMap<Integer, UidState>(NU);
        for(int i = 0; i < NU; ++i) {           
            BatteryStats.Uid u = uidStats.valueAt(i);
            int uid = u.getUid();
            
            UidState state = new UidState();
            state.mUid = uid; 
            mUidStates.put(uid, state);
            
            addCpuTime(u, state);
            addSensorTime(u, batteryRealtime, state);
            addDataUsage(u, state);
            addWakelockUsage(u, batteryRealtime, state);
        }
    }
    
    private void addCpuTime(BatteryStats.Uid u, UidState state) {
        long totalCpuTime = 0L;
        Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
        if (processStats.size() > 0) {
            for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent
                    : processStats.entrySet()) {
                Uid.Proc ps = ent.getValue();
                long userTime = ps.getUserTime(STAT_TYPE);
                long systemTime = ps.getSystemTime(STAT_TYPE);
                long cpuTime = (userTime + systemTime) * 10; //in millis

                totalCpuTime += cpuTime;
            }
        }
        state.mCpuTime = divideByK(totalCpuTime);
    }

    static final Object knownSensors[] = {
        "gps", (int)BatteryStats.Uid.Sensor.GPS,
        "acs", (int)Sensor.TYPE_ACCELEROMETER,
        "grs", (int)Sensor.TYPE_GRAVITY,
        "gys", (int)Sensor.TYPE_GYROSCOPE,
        "lis", (int)Sensor.TYPE_LIGHT,
        "las", (int)Sensor.TYPE_LINEAR_ACCELERATION,
        "mfs", (int)Sensor.TYPE_MAGNETIC_FIELD,
        "prs", (int)Sensor.TYPE_PRESSURE,
        "pxs", (int)Sensor.TYPE_PROXIMITY,
        "rvs", (int)Sensor.TYPE_ROTATION_VECTOR,
        "tms", (int)Sensor.TYPE_AMBIENT_TEMPERATURE
    };

    private final String getSensorNameFromUid(int sensorType) {
        int len = knownSensors.length;
        for (int i=0; i<len; i+=2 ) {
            if (((Integer)knownSensors[i+1]).equals(sensorType)) return (String)knownSensors[i];
        }
        return null;
    }
    
    private void addSensorTime(BatteryStats.Uid u, long batteryRealtime,
            UidState state) {
        Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
        if (sensorStats.size() > 0) {
            for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> ent
                    : sensorStats.entrySet()) {
                Uid.Sensor se = ent.getValue();
                int sensorType = se.getHandle(); 
                Timer timer = se.getSensorTime();
                if (timer != null) {
                    // Convert from microseconds to milliseconds with rounding
                    long time = 
                        (timer.getTotalTimeLocked(batteryRealtime, STAT_TYPE) + 500) / 1000;
                    int count = timer.getCountLocked(STAT_TYPE);
                    String sensorShortName = getSensorNameFromUid(sensorType);
                    if ( sensorShortName != null ) {
                        SensorData sensorData = state.getSensorData(sensorShortName);
                        sensorData.time = divideByK(time);
                        sensorData.count = count;
                    }
                }
            }
        }
    }
    
    private void addDataUsage(BatteryStats.Uid u, UidState state) {
        int uid = u.getUid();
        TrafficCollectionService tcs = TrafficCollectionService.getService();
        if (tcs == null) {
            if (DUMP) {
                Log.d(TAG, "TrafficCollectionService instance is null now.");
            }
            return;
        }
        UidInfo uidInfo = tcs.getUidInfo(uid);
        if (uidInfo == null) {
            state.mMobileTcpOnBattery = 0L;
            state.mWifiTcpOnBattery = 0L;
            state.mTcpOnBattery = 0L;
            state.mMobileNetstatBytes = 0L;
            state.mWifiNetstatBytes = 0L;
            state.mNetstatBytes = 0L;
            state.mPackets = 0L;
            return;
        }
        tcs.refreshTrafficList();
        state.mMobileTcpOnBattery = uidInfo.mByteMobileOnBattery;
        state.mWifiTcpOnBattery = uidInfo.mByteWifiOnBattery;
        // state.mTcpOnBattery = state.mMobileTcpOnBattery + state.mWifiTcpOnBattery;
        // Revert it back to u.getTcpBytesReceived(STAT_TYPE) + u.getTcpBytesSent(STAT_TYPE)
        // to find why if there is a discrepancy between byt and byt_mobile/byt_wifi
        state.mTcpOnBattery = u.getTcpBytesReceived(STAT_TYPE) + u.getTcpBytesSent(STAT_TYPE);
        state.mMobileNetstatBytes = uidInfo.mByteMobilePerUid;
        state.mWifiNetstatBytes = uidInfo.mByteWifiPerUid;
        state.mNetstatBytes = uidInfo.mByteTotalPerUid;
        state.mPackets = uidInfo.mPacketTotalPerUid;
        if (DUMP) {
            Log.d(TAG, "addDataUsage: " + "uid=" + uid + "; mMobileNetstatBytes="
                    + state.mMobileNetstatBytes + "; mWifiNetstatBytes="
                    + state.mWifiNetstatBytes + "; mNetstatBytes="
                    + state.mNetstatBytes + "; mMobileTcpOnBattery="
                    + state.mMobileTcpOnBattery + "; mWifiTcpOnBattery="
                    + state.mWifiTcpOnBattery + "; mTcpOnBattery="
                    + state.mTcpOnBattery + "; mPackets="
                    + state.mPackets);
        }
    }

    private void addWakelockUsage(BatteryStats.Uid u, long batteryRealtime, UidState state) {
        state.mFullWakelockTime = state.mFullWakelockCount =
            state.mPartialWakelockTime = state.mPartialWakelockCount = 0;

        Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
        for ( BatteryStats.Uid.Wakelock wl : wakelockStats.values() ) {
            Timer timer = wl.getWakeTime(BatteryStats.WAKE_TYPE_FULL);
            if ( timer != null ) {
                // convert microsec to millisec
                state.mFullWakelockTime += divideByK( timer.getTotalTimeLocked(batteryRealtime, STAT_TYPE) );
                state.mFullWakelockCount += timer.getCountLocked(STAT_TYPE);
            }

            timer = wl.getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
            if ( timer != null ) {
                // convert microsec to millisec
                state.mPartialWakelockTime += divideByK( timer.getTotalTimeLocked(batteryRealtime, STAT_TYPE) );
                state.mPartialWakelockCount += timer.getCountLocked(STAT_TYPE);
            }
        }
    }

    /**
     * @param stats
     * @param rawRealtime
     * @param rawUptime
     * @param batteryRealtime
     */
    private void collectTimeStats(BatteryStatsImpl stats,
            long rawRealtime, long rawUptime, long batteryRealtime, Context context) {
        mRefTime = System.currentTimeMillis();

        SharedPreferences sp = context.getSharedPreferences(EventConstants.OPTIONS,
                Context.MODE_PRIVATE);
        mTotalRealtime = divideByK(TimeTracker.getRealtime(sp));
        mTotalUptime = divideByK(TimeTracker.getUptime(sp));

        mGpsTimeSec = divideByK(GpsTracker.getTimeMs(context));

        mBatteryRealtime = divideByM(stats.computeBatteryRealtime(
                rawRealtime, STAT_TYPE));
        mBatteryUptime = divideByM(stats.computeBatteryUptime(rawUptime,
                STAT_TYPE));
        
        mScreenOnTime = divideByM(stats.getScreenOnTime(batteryRealtime,
                STAT_TYPE));
        mPhoneOnTime = divideByM(stats.getPhoneOnTime(batteryRealtime, STAT_TYPE));
        mScreenBrightnessTimes = new long[BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS];
        for(int i = 0; i < BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS; ++i) {
            mScreenBrightnessTimes[i] = divideByM(
                    stats.getScreenBrightnessTime(i, batteryRealtime, STAT_TYPE));
        }
        
        mInputEventCount = stats.getInputEventCount(STAT_TYPE);
        
        mWifiOnTime = divideByM(stats.getWifiOnTime(batteryRealtime, STAT_TYPE));
        mWifiRunTime = divideByM(
                    stats.getGlobalWifiRunningTime(batteryRealtime, STAT_TYPE));
        
        mBTOnTime = divideByM(stats.getBluetoothOnTime(batteryRealtime, STAT_TYPE));
        
        mSignalScanTime = divideByM(
                stats.getPhoneSignalScanningTime(batteryRealtime, STAT_TYPE));
        
        mSignalStrengthTimes = new long[BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS];
        mSignalStrengthCount = new long[BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS];

        for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
            mSignalStrengthTimes[i] = divideByM(
                    stats.getPhoneSignalStrengthTime(i, batteryRealtime, STAT_TYPE));
            mSignalStrengthCount[i] = stats.getPhoneSignalStrengthCount(i, STAT_TYPE);
        }
        
        mDataConnTimes = new long[BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES];
        mDataConnCount = new long[BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES];

        for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
            mDataConnTimes[i] = divideByM(
                    stats.getPhoneDataConnectionTime(i, batteryRealtime, STAT_TYPE));
            mDataConnCount[i] = stats.getPhoneDataConnectionCount(i, STAT_TYPE);
        }
    }
    
    private void loadTimes(StatsDatabase db) {
        final String GROUP = "default";
        final String SUBID = "default";
       
        // TODO: Read these values by getting the subid pairs instead
        //       of one by one - getValue() is generic but too many
        //       reads is slower 
        mRefTime = parseLong(db.getValue(GROUP, SUBID, "bstat_reftime"));
        mTotalRealtime = parseLong(db.getValue(GROUP, SUBID, "trt_rt"));
        mTotalUptime = parseLong(db.getValue(GROUP, SUBID, "trt_ut"));
        mGpsTimeSec = parseLong(db.getValue(GROUP, SUBID, "gps_rt"));
        mBatteryRealtime = parseLong(db.getValue(GROUP, SUBID, "tob_rt"));
        mBatteryUptime = parseLong(db.getValue(GROUP, SUBID, "tob_ut"));
        
        mScreenOnTime = parseLong(db.getValue(GROUP, SUBID, "sc_ot"));
        mPhoneOnTime = parseLong(db.getValue(GROUP, SUBID, "ph_ot"));
        mScreenBrightnessTimes = new long[BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS];
        for(int i = 0; i < BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS; ++i) {
            mScreenBrightnessTimes[i] =
                parseLong(db.getValue(GROUP, SUBID, 
                        BatteryStatsConstants.SCREEN_BRIGHTNESS_NAMES[i]));
        }
        
        mWifiOnTime = parseLong(db.getValue(GROUP, SUBID, "wifi_ot"));
        mWifiRunTime = parseLong(db.getValue(GROUP, SUBID, "wifi_rt"));
        mBTOnTime = parseLong(db.getValue(GROUP, SUBID, "bt_ot"));
        
        mInputEventCount = parseLong(db.getValue(GROUP, SUBID, "ipe"));
        
        mSignalScanTime = parseLong(db.getValue(GROUP, SUBID, "sst"));
        mSignalStrengthTimes = new long[BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS];
        for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
            mSignalStrengthTimes[i] =
                parseLong(db.getValue(GROUP, SUBID, 
                        BatteryStatsConstants.SIGNAL_STRENGTH_NAMES[i] + "_t"));
        }
        mSignalStrengthCount = new long[BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS];
        for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
            mSignalStrengthCount[i] =
                parseLong(db.getValue(GROUP, SUBID, 
                        BatteryStatsConstants.SIGNAL_STRENGTH_NAMES[i] + "_cnt"));
        }
        
        mDataConnTimes = new long[BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES];
        for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
            mDataConnTimes[i] =
                parseLong(db.getValue(GROUP, SUBID, 
                        BatteryStatsConstants.DATA_CONN_NAMES[i] + "_t"));
        }
        mDataConnCount = new long[BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES];
        for(int i = 0; i < BatteryStatsConstants.NUM_DATA_CONNECTION_TYPES; ++i) {
            mDataConnCount[i] =
                parseLong(db.getValue(GROUP, SUBID, 
                        BatteryStatsConstants.DATA_CONN_NAMES[i] + "_cnt"));
        }
    }
    
    private void loadUids(StatsDatabase db) {
        final String GROUP = "UID";
        
        int N = db.getGroupSize(GROUP);
        if(N <= 0) return;
        
        Iterator<String> uids = db.getGroupSubIds(GROUP);
        if (uids == null) return;

        mUidStates = new HashMap<Integer, UidState>(N);
        while(uids.hasNext()) {
            UidState u = new UidState();
            String subid = uids.next();
            u.mCpuTime = parseLong(db.getValue(GROUP, subid, "cpu"));
            u.mNetstatBytes = parseLong(db.getValue(GROUP, subid, "nsb"));
            u.mMobileNetstatBytes= parseLong(db.getValue(GROUP, subid, "nsb_mobile"));
            u.mWifiNetstatBytes= parseLong(db.getValue(GROUP, subid, "nsb_wifi"));
            u.mSensorData = decodeSensorData(db.getValue(GROUP, subid, "sensordata"));
            u.mUid = (int)parseLong(db.getValue(GROUP, subid, "u"));
            u.mTcpOnBattery = parseLong(db.getValue(GROUP, subid, "byt"));
            u.mMobileTcpOnBattery = parseLong(db.getValue(GROUP, subid, "byt_mobile"));
            u.mWifiTcpOnBattery = parseLong(db.getValue(GROUP, subid, "byt_wifi"));
            u.mPackets = parseLong(db.getValue(GROUP, subid, "pkt"));
            u.mFullWakelockTime = parseLong(db.getValue(GROUP, subid, "fwt"));
            u.mFullWakelockCount = parseLong(db.getValue(GROUP, subid, "fwc"));
            u.mPartialWakelockTime = parseLong(db.getValue(GROUP, subid, "pwt"));
            u.mPartialWakelockCount = parseLong(db.getValue(GROUP, subid, "pwc"));
            mUidStates.put(u.mUid, u);
        }
    }

    static final SensorHashMap decodeSensorData(String encoded) {
        SensorHashMap sensorData = null;
        sensorData = (SensorHashMap)DevStatUtils.deSerializeObject(encoded,SensorHashMap.class);
        if ( sensorData == null ) sensorData = new SensorHashMap();
        return sensorData;
    }
    
    private static long parseLong(String value) {
        long val = 0;
        try {
            if(value != null) val = Long.parseLong(value);
        }catch(NumberFormatException nfEx) {}
        return val;
    }
    
    private static long divideByM(long micros) {
        return divideByK(divideByK(micros));
    }
    
    private static long divideByK(long millis) {
        return (millis + 500)/1000;
    }

    /**
     * Update currentState with "currentState - this", for fields that are computed outside of the
     * android framework. This method should be called only when the android framework has been
     * reset and the entire data in currentState has to be added to the cumulative stats in the
     * database
     *
     * @param currentState
     *            - The current batterystats read from android framework, that has to to be diff-ed
     *            with "this" for the non android framework fields
     */
    public void diffOnFrameworkReset(BatteryStatsState currentState) {
        // this is lastState. currentState needs to be updated with currentState - lastState
        currentState.mTotalRealtime -= mTotalRealtime;
        currentState.mTotalUptime -= mTotalUptime;
        currentState.mGpsTimeSec -= mGpsTimeSec;
        if (mUidStates != null && currentState.mUidStates != null) {
            int did = 0;
            Iterator<UidState> iterator = currentState.mUidStates.values().iterator();
            UidState tmp = new UidState();
            while (iterator.hasNext()) {
                UidState cu = iterator.next();
                UidState lu = mUidStates.get(cu.mUid);
                if (lu == null) {
                    continue;
                } else {
                    tmp.mNetstatBytes = cu.mNetstatBytes - lu.mNetstatBytes;
                    tmp.mMobileNetstatBytes = cu.mMobileNetstatBytes - lu.mMobileNetstatBytes;
                    tmp.mWifiNetstatBytes = cu.mWifiNetstatBytes - lu.mWifiNetstatBytes;
                    tmp.mMobileTcpOnBattery = cu.mMobileTcpOnBattery - lu.mMobileTcpOnBattery;
                    tmp.mWifiTcpOnBattery = cu.mWifiTcpOnBattery - lu.mWifiTcpOnBattery;
                    tmp.mPackets = cu.mPackets - lu.mPackets;
                    if (tmp.mNetstatBytes < 0 || tmp.mMobileNetstatBytes < 0
                            || tmp.mWifiNetstatBytes < 0 || tmp.mMobileTcpOnBattery < 0
                            || tmp.mWifiTcpOnBattery < 0 || tmp.mPackets < 0) {
                        if (DUMP) {
                            StringBuilder sb;
                            sb = new StringBuilder();
                            sb.append("diffOnFrameworkReset get invalid uid values:")
                                    .append(cu.mUid)
                                    .append(";").append(cu.mNetstatBytes)
                                    .append(";").append(lu.mNetstatBytes)
                                    .append(";").append(cu.mMobileNetstatBytes)
                                    .append(";").append(lu.mMobileNetstatBytes)
                                    .append(";").append(cu.mWifiNetstatBytes)
                                    .append(";").append(lu.mWifiNetstatBytes)
                                    .append(";").append(cu.mMobileTcpOnBattery)
                                    .append(";").append(lu.mMobileTcpOnBattery)
                                    .append(";").append(cu.mWifiTcpOnBattery)
                                    .append(";").append(lu.mWifiTcpOnBattery)
                                    .append(";").append(cu.mPackets)
                                    .append(";").append(lu.mPackets);
                            Log.v(TAG, sb.toString());
                            sb.setLength(0);
                        }
                        continue;
                    } else {
                        cu.mNetstatBytes = tmp.mNetstatBytes;
                        cu.mMobileNetstatBytes = tmp.mMobileNetstatBytes;
                        cu.mWifiNetstatBytes = tmp.mWifiNetstatBytes;
                        cu.mMobileTcpOnBattery = tmp.mMobileTcpOnBattery;
                        cu.mWifiTcpOnBattery = tmp.mWifiTcpOnBattery;
                        cu.mPackets = tmp.mPackets;
                    }
                }
            }
        }
    }
}

