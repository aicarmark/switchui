/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.hardware.SensorManager;
import android.net.TrafficStats;
import android.os.BatteryStats;
import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Timer;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.PowerManager;
import android.telephony.SignalStrength;
import android.util.SparseArray;

import android.util.Log;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.PowerProfile;
import com.motorola.batterymanager.Utils;

/**
 * Thread controlled by DeviceStatisticsSvc to log passive device statistics to 
 * Checkin DB
 */
public class CollectStatistics {
    private final static String TAG = "CollectStatistics";
    
    // TODO: CUrrently there is the overhead of keeping the below in sync 
    //       with Battery Stats Service. Ideally should have a method
    //       implemented in Battery Stats for our specific purpose OR
    //       export the constants so that they will be visible here 

    private Context mContext;
    
    final String[] SCREEN_BRIGHTNESS_NAMES = {
        "dark", "dim", "medium", "light", "bright"
    };
    
    final String[] SIGNAL_STRENGTH_NAMES = {
        "none", "poor", "moderate", "good", "great"
    };
    
    final String[] DATA_CONNECTION_NAMES = {
        "none", "gprs", "edge", "umts", "other"
    };

    private IBatteryStats getStatsInterface() throws RemoteException {
        IBatteryStats iface = IBatteryStats.Stub.asInterface(
                ServiceManager.getService("batteryinfo"));
        if(iface == null) {
            throw new RemoteException();
        }
        return iface;
    }

    public ArrayList<String> getDeviceStats(Context ctx, long checkinTime) throws RemoteException{
        mContext = ctx;
        mCheckinTime = checkinTime;
        IBatteryStats svcIface = getStatsInterface();
        return parseStats(svcIface);
    }

    BatteryStatsImpl mBattStats;
    int mStatsType = BatteryStats.STATS_SINCE_CHARGED;
    long mCheckinTime;
    long mBatteryRealtime = 0L;
    long mRawRealtime = 0L;
    long mRawUptime = 0L;

    /**
     * parseStats collects stats and logs into checkin DB.
     */
    private ArrayList<String> parseStats(IBatteryStats service)
            throws RemoteException {
        ArrayList<String> logList = new ArrayList<String>();
        // Get the statistics from service
        byte[] data;
        data = service.getStatistics();
        Parcel dataBundle = Parcel.obtain();
        dataBundle.unmarshall(data, 0, data.length);
        dataBundle.setDataPosition(0);
        mBattStats = BatteryStatsImpl.CREATOR.createFromParcel(dataBundle);
        
        // Now make up the individual logs
        synchronized(mBattStats) {
            // Uptime
            mRawUptime = SystemClock.uptimeMillis() * 1000;
            mRawRealtime = SystemClock.elapsedRealtime() * 1000;
            // final long batteryUptime = mBattStats.getBatteryUptime(rawUptime);
            mBatteryRealtime = mBattStats.getBatteryRealtime(mRawRealtime);
            // final long phoneOnTime = mBattStats.getPhoneOnTime(mBatteryRealtime, mStatsType);
            
            StringBuilder sb = new StringBuilder();
            String emptyStr = new String("");

            // Phone uptime, Screen Stats, WifiUptime & BT uptime
            sb.setLength(0);
            getPhoneTimeStats(sb);
            logList.add(sb.toString());

            // Cell Signal Strength
            sb.setLength(0);
            getSignalStrength(sb);
            logList.add(sb.toString());

            // Radio Types
            sb.setLength(0);
            getRadioTypes(sb);
            logList.add(sb.toString());

            // Mobile & WIFI Data
            String wifiData = SysClassNetUtils.getWifiRxTxBytes(mContext);
            String mobileData = SysClassNetUtils.getMobileRxTxBytes(mContext);
            if ((wifiData != null) || (mobileData != null)) {
                sb.setLength(0);
                sb.append("[ID=DataSizes;ver=" + DevStatPrefs.VERSION + ";time=" + mCheckinTime + ";");
                sb.append((mobileData != null)?mobileData:emptyStr);
                sb.append((wifiData != null)?wifiData:emptyStr);
                sb.append("]");
                logList.add(sb.toString());
            }
    
            // SMS & Call Info
            String smsInfo = DevStatUtils.getSMSDetails(mContext);
            String callInfo = DevStatUtils.getCallDetails(mContext);
            if ((smsInfo != null) || (callInfo != null)) {
                sb.setLength(0); 
                sb.append("[ID=SMSCalls;ver=" + DevStatPrefs.VERSION + ";time=" + mCheckinTime + ";");
                sb.append((smsInfo != null)?smsInfo:emptyStr);
                sb.append((callInfo != null)?callInfo:emptyStr);
                sb.append("]");
                logList.add(sb.toString());
            }

            // Contact & Email Info
            String contactInfo = DevStatUtils.getContactsDetails(mContext);
            String emailInfo = DevStatUtils.getEmailDetails(mContext);
            if ((contactInfo != null) || (emailInfo != null)) {
                sb.setLength(0);
                sb.append("[ID=ContactEmailDetails;ver=" + DevStatPrefs.VERSION + ";time=" + mCheckinTime + ";]");
                sb.append((contactInfo != null)?contactInfo:emptyStr);
                sb.append((emailInfo != null)?emailInfo:emptyStr);
                logList.add(sb.toString());
            }

            // Application Usage Data
            ArrayList<String> appList = getPerUidStats();
            if (appList.size() > 0) {
                Iterator<String> it = appList.iterator();
                while (it.hasNext()) {
                    logList.add(it.next());
                }
            }

            // Pkg Usage Data
            ArrayList<String> pkgList = PkgStatsUtils.getUsageStats(mContext, mCheckinTime);
            if (pkgList.size() > 0) {
                Iterator<String> it = pkgList.iterator();
                while (it.hasNext()) {
                    logList.add(it.next());
                }
            }
        }
        return logList;
    }

    /**
     * Converts microseconds to second, as per checkin Server format
     */
    private long timeInSecs(long timeInMicro) {
        return (timeInMicro/1000)/1000;
    }

    private void getPhoneTimeStats(StringBuilder sb) {
        sb.append("[ID=PhoneTimeStats;ver=" + DevStatPrefs.VERSION + ";time=" + mCheckinTime + ";");

        // Time on Battery and Total RunTime
        long whichBatteryUptime = mBattStats.computeBatteryUptime(mRawUptime, mStatsType);
        long whichBatteryRealtime = mBattStats.computeBatteryRealtime(mRawRealtime, mStatsType);
        long totalRealtime = mBattStats.computeRealtime(mRawRealtime, mStatsType);
        long totalUptime = mBattStats.computeUptime(mRawUptime, mStatsType);
        sb.append("tob_rt=" + timeInSecs(whichBatteryRealtime)  +  
                   ";tob_ut=" + timeInSecs(whichBatteryUptime)  + 
                   ";trt_rt=" + timeInSecs(totalRealtime) +
                   ";trt_ut=" + timeInSecs(totalUptime) + ";");

        // ScreenStats
        final long screenOnTime = mBattStats.getScreenOnTime(mBatteryRealtime, mStatsType);
        sb.append("sc_ot=" + timeInSecs(screenOnTime) + ";");
            for(int i=0; i<BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS; i++) {
                final long time = mBattStats.getScreenBrightnessTime(i, mBatteryRealtime, mStatsType);
                sb.append(SCREEN_BRIGHTNESS_NAMES[i] + "=" + timeInSecs(time) + ";");
            }
        sb.append("ipe=" + mBattStats.getInputEventCount(mStatsType) + ";");

        // Wifi RunTime and Uptime
        final long wifiRunningTime = mBattStats.getGlobalWifiRunningTime(mBatteryRealtime, mStatsType);
        final long wifiOnTime = mBattStats.getWifiOnTime(mBatteryRealtime, mStatsType);
        if ((wifiRunningTime != 0) || (wifiOnTime != 0)) {
            sb.append("wifi_ot=" + timeInSecs(wifiOnTime) + 
                      ";wifi_rt=" + timeInSecs(wifiRunningTime) + ";");
        }

        // BT Uptime
        final long bluetoothOnTime = mBattStats.getBluetoothOnTime(mBatteryRealtime, mStatsType);
        if (bluetoothOnTime != 0) {
            sb.append("bt_ot=" + timeInSecs(bluetoothOnTime) + ";");
        }

        // Add data and sd card status to this as well
        getFsStats(sb);

        // Add eMMC stats
        geteMMCStats(sb);

        // End the counter
        sb.append("]");
    }

    /**
     * Collects Signal Strength stats. This consists of signal types and time spent in 
     * each type. Along with it is the signal scanning time
     */
    private void getSignalStrength(StringBuilder sb) {
        sb.append("[ID=SignalStrengths;ver=" + DevStatPrefs.VERSION + ";time=" + mCheckinTime + 
                ";sst=" + timeInSecs(mBattStats.getPhoneSignalScanningTime(mBatteryRealtime, mStatsType)) + ";");
        for (int i=0; i<SignalStrength.NUM_SIGNAL_STRENGTH_BINS; i++) {
            final long time = mBattStats.getPhoneSignalStrengthTime(i, mBatteryRealtime, mStatsType);
            sb.append("ss_" + SIGNAL_STRENGTH_NAMES[i] + "_t=" + timeInSecs(time) +
                    ";ss_" + SIGNAL_STRENGTH_NAMES[i] + "_cnt=" + 
                    (mBattStats.getPhoneSignalStrengthCount(i, mStatsType)) + ";" );
        }
        // End the counter
        sb.append("]");
    }

    /**
     * Collects Radio types and time spent in each radio type. Along with it is the total
     * time spent on radio
     */
    private void getRadioTypes(StringBuilder sb) {
        // Currently, radio data uptime is always returning 0 due to file not present
        // hence removing from logs. Moreover this time is for last unplug state which we are not intersted
        // "[ID=RadioType;radio_data_uptime_unplugged=" + timeInSecs(mBattStats.getRadioDataUptime()) + ";]"
        sb.append("[ID=RadioTypes;ver=" + DevStatPrefs.VERSION + ";time=" + mCheckinTime + ";");
        for (int i=0; i<BatteryStats.NUM_DATA_CONNECTION_TYPES; i++) {
            final long time = mBattStats.getPhoneDataConnectionTime(i, mBatteryRealtime, mStatsType);
            sb.append("rt_" + DATA_CONNECTION_NAMES[i] + "_t=" + timeInSecs(time) +
                    ";rt_" + DATA_CONNECTION_NAMES[i] + "_cnt=" + 
                    mBattStats.getPhoneDataConnectionCount(i, mStatsType) + ";" );
        }
        // End the counter
        sb.append("]");
    }

    private void getFsStats(StringBuilder sb) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long availableBlocks = stat.getAvailableBlocks();

        sb.append("datatotl=" + (blockSize*totalBlocks))
          .append(";datafree=" + (blockSize*availableBlocks));

        // Now SD card
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            status = Environment.MEDIA_MOUNTED;
        }

        long totalSdSize = 0;
        long avlSdSize = 0;
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            try {
                path = Environment.getExternalStorageDirectory();
                stat = new StatFs(path.getPath());
                blockSize = stat.getBlockSize();
                totalBlocks = stat.getBlockCount();
                availableBlocks = stat.getAvailableBlocks();

                totalSdSize = totalBlocks * blockSize;
                avlSdSize = availableBlocks * blockSize;
            } catch (IllegalArgumentException e) {
                // this can occur if the SD card is removed, but we haven't received the
                // ACTION_MEDIA_REMOVED Intent yet.
            }
        } 
        sb.append(";sdtotl=" + totalSdSize)
          .append(";sdavl=" + avlSdSize);

        sb.append(";");
    }

    private void geteMMCStats(StringBuilder sb) {
        long numSectors = readeMMCSectorWrites();
        sb.append("bootuptime=" + (SystemClock.elapsedRealtime()/ 1000))
          .append(";emmcwrcnt=" + numSectors)
          .append(";");
    }

    private long readeMMCSectorWrites() {
        byte[] data;
        long count = 0;
        data = readFile("/sys/block/mmcblk1/stat");
        if(data != null) {
            int numValues = 0;
            int startIndex = -1, endIndex = -1;
            int state = 0;
            for(int i = 0; i < data.length && data[i] != 0; ++i) {
                if(state == 0) {
                    // Looking for number
                    if(data[i] != ' ' && data[i] != '\t') {
                        startIndex = i;
                        endIndex = -1;
                        numValues++;
                        state = 1;
                    }
                }else if(state == 1) { // Looking for space
                    if(data[i] == ' ' || data[i] == '\t') {
                        endIndex = i - 1;
                        state = 0;
                        if(numValues == 7) {
                            break;
                        }
                    }
                }
            }
            if(startIndex >= 0  && endIndex > 0 && startIndex <= endIndex && numValues == 7) {
                String longStr = new String(data, startIndex, endIndex - startIndex + 1);
                count = Long.parseLong(longStr);
            }
        }
        return count;
    }

    private byte[] readFile(String filename) {
        FileInputStream ifs = null; 
        try {
            ifs = new FileInputStream(filename);
            byte[] data = new byte[1024];
            int num = ifs.read(data);
            if(num > 0) {
                return data;
            }
        }catch(Exception ex) {
            Utils.Log.v(TAG, "Exception reading eMMC sector write count" + ex);
        }finally {
            try {
                if(ifs != null) ifs.close();
            }catch(IOException ioEx) {
            }
        }
        return null;
    } 

    /**
     * Collects stats per uid. Provides uid name, power usage (cpu, data & sensor), data usage,
     * wakelock usage per Uid from system boot. Code, data and cache usage are at the current
     * instance.
     * Logs are split across multiple records, based on the max log size preference setting.
     */
    private ArrayList<String> getPerUidStats() {
        SparseArray<? extends Uid> uidStats = mBattStats.getUidStats();
        final int NU = uidStats.size();
        PowerProfile powerProfile = new PowerProfile(mContext);
        SizeObserver sizeObserver = new SizeObserver();
        List<ApplicationUsage> appUsageList = new ArrayList<ApplicationUsage>();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            UidInfo uInfo = getNameForUid(u.getUid());
            if (uInfo.mName != null) {
                ApplicationUsage appUsage = new ApplicationUsage(u.getUid(), uInfo.mName, 
                        loadWakelockUsage(u), loadCpuUsage(u, powerProfile), 
                        loadDataUsage(u, powerProfile), loadSensorUsage(u, powerProfile),
                        loadPkgUsage(uInfo, sizeObserver));
                appUsageList.add(appUsage);
            }
        }

        ArrayList<String> aList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        DevStatPrefs dsp = DevStatPrefs.getInstance(mContext);
        int maxPerlog = dsp.getMaxLogSize();
        int logSize = 0;
        sb.setLength(0);
        if (appUsageList.size() > 0) {
            Iterator<ApplicationUsage> au = appUsageList.iterator();
            while (au.hasNext()) {
                String str = au.next().formatForCheckin();
                if (str != null) {
                    if (logSize + str.length() < maxPerlog) {
                        logSize += str.length();
                        sb.append(str);
                    } else {
                       aList.add("[ID=PerUidStats;ver=" + DevStatPrefs.VERSION + ";time=" + 
                              mCheckinTime + ";]" + sb.toString());
                       sb.setLength(0);
                       sb.append(str);
                       logSize = str.length();
                    }
                }
            }
        }

        if (sb.length() > 0) {
            aList.add("[ID=PerUidStats;ver=" + DevStatPrefs.VERSION + ";time=" + 
                    mCheckinTime + ";]" + sb.toString());
        }
            
        return aList;
    }

    class UidInfo {
        public UidInfo(String name, int uid, String[] packages) {
            mName = name;
            mUid = uid;
            mPackages = packages;
        }
      
        public String mName;
        public int mUid;
        public String[] mPackages;
    }

    private UidInfo getNameForUid(int uid) {
        PackageManager pm = mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        String name = null;
        PackageInfo pi = null; 
        if (packages != null) {
            if (packages.length == 1) {
                name = packages[0];
            } else {
                for (String s : packages) {
                    try {
                        pi = pm.getPackageInfo(s, 0);
                        if (pi.sharedUserLabel != 0) {
                            CharSequence nm = pm.getText(s,
                                pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                name = nm.toString();
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) { }
                }
            }
        }
        if(name == null) {
            if(uid == Process.PHONE_UID) {
                // This is made specific because it is important and UID is fixed
                name = "Phone";
            }else if(packages != null && packages.length > 0) {
                name = packages[0];
            }
            // Utils.Log.v(TAG, "Explicit package name: " + name + uid);
        }
        return new UidInfo(name, uid, packages);
    }

    private WakelockUsage loadWakelockUsage(Uid u) {
        Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
        long fullWlTime = 0;
        long partialWlTime = 0;
        int partialWlCount = 0;
        int fullWlCount = 0;
        if (wakelockStats.size() > 0) {
            for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> ent
                    : wakelockStats.entrySet()) {
                Uid.Wakelock wl = ent.getValue();
                Timer partialWakeTimer = wl.getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
                if (partialWakeTimer != null) { 
                     // Convert from microseconds to milliseconds with rounding
                     partialWlTime += (partialWakeTimer.getTotalTimeLocked(
                             mBatteryRealtime, mStatsType) + 500) / 1000; 
                     partialWlCount += partialWakeTimer.getCountLocked(mStatsType);
                }       

                Timer fullWakeTimer = wl.getWakeTime(BatteryStats.WAKE_TYPE_FULL);
                if (fullWakeTimer != null) { 
                     // Convert from microseconds to milliseconds with rounding
                     fullWlTime += (fullWakeTimer.getTotalTimeLocked(
                         mBatteryRealtime, mStatsType) + 500) / 1000; 
                     fullWlCount += fullWakeTimer.getCountLocked(mStatsType);
                 }       
             }       
        }       
        // Store time in secs
        return new WakelockUsage(fullWlTime/1000, fullWlCount, partialWlTime/1000, partialWlCount);
    }

    private CpuUsage loadCpuUsage(Uid u, PowerProfile powerProfile) {
        final int speedSteps = powerProfile.getNumSpeedSteps();
        final long[] cpuSpeedStepTimes = new long[speedSteps];
        final double[] powerCpuNormal = new double[speedSteps];
        for (int p = 0; p < speedSteps; p++) {
            powerCpuNormal[p] = powerProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE, p);
        }

        long totalCpuTime = 0L;
        double power = 0;
        Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
        if (processStats.size() > 0) {
            for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent
                    : processStats.entrySet()) {
                Uid.Proc ps = ent.getValue();
                long userTime = ps.getUserTime(mStatsType);
                long systemTime = ps.getSystemTime(mStatsType);
                long cpuTime = (userTime + systemTime) * 10; //in millis
                int totalTimeAtSpeeds = 0;

                for (int step = 0; step < speedSteps; step++) {
                    cpuSpeedStepTimes[step] = ps.getTimeAtCpuSpeedStep(step, mStatsType);
                    totalTimeAtSpeeds += cpuSpeedStepTimes[step];
                }

                if (totalTimeAtSpeeds == 0) totalTimeAtSpeeds = 1;

                // Then compute the ratio of time spent at each speed
                for (int step = 0; step < speedSteps; step++) {
                    double ratio = (double) cpuSpeedStepTimes[step] / totalTimeAtSpeeds;
                    power += ratio * cpuTime * powerCpuNormal[step];
                }

                totalCpuTime += cpuTime;
            }
        }
        return new CpuUsage(totalCpuTime/1000, power/1000);
    }

    private SensorUsage loadSensorUsage(Uid mUid, PowerProfile mPowerProfile) {
        Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = mUid.getSensorStats();
        long totalSensorTime = 0L;
        int totalSensorCount = 0;
        double totalSensorPower = 0;
        ArrayList <PerSensor> perSensor = new ArrayList<PerSensor>();
        if (sensorStats.size() > 0) {
            for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> ent
                    : sensorStats.entrySet()) {
                Uid.Sensor se = ent.getValue();
                int sensorType = se.getHandle(); 
                Timer timer = se.getSensorTime();
                if (timer != null) {
                    // Convert from microseconds to milliseconds with rounding
                    long time = (timer.getTotalTimeLocked(mBatteryRealtime, mStatsType) + 500) / 1000;
                    int count = timer.getCountLocked(mStatsType);
                    double powerUnit = 0;
                    switch (sensorType) {
                        case BatteryStats.Uid.Sensor.GPS:
                            powerUnit = mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_ON);
                            break;
                        default:
                            SensorManager sm = (SensorManager)mContext.getSystemService(
                                    Context.SENSOR_SERVICE);
                            android.hardware.Sensor sensorData = sm.getDefaultSensor(sensorType);
                            if (sensorData != null) {
                                powerUnit = sensorData.getPower();
                            }
                    }
                    double power = (powerUnit * time)/1000;
                    totalSensorTime += time; 
                    totalSensorPower += power;
                    totalSensorCount += count;
                    perSensor.add(new PerSensor(sensorType, time/1000, power, count));
                }
            }
        }
        return new SensorUsage(totalSensorTime/1000, totalSensorPower, totalSensorCount, perSensor);
    }

    private DataUsage loadDataUsage(Uid mUid, PowerProfile powerProfile) {
        // TODO: As per BatteryHistory. Need to Extract average bit rates from system.
        final long wifi_bps = 1000000;  
        final long mobile_bps = 200000; 

        // Per Sec
        final double wifi_power = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE) / 3600;
        final double mobile_power = powerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) / 3600;

        final long mobileData = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
        DevStatPrefs prefs = DevStatPrefs.getInstance(mContext);
        final long wifiData = prefs.getWifiCumRxBytes() + prefs.getWifiCumTxBytes();

        final long radioDataUptimeMs = mBattStats.getRadioDataUptime() / 1000;
        final long mobileBps = (radioDataUptimeMs != 0) ? mobileData * 8 * 1000 / radioDataUptimeMs : mobile_bps;

        double mobileCostPerByte = mobile_power / (mobileBps / 8);
        double wifiCostPerByte = wifi_power / (wifi_bps / 8);
        double averageCostPerByte = 0;
        if (wifiData + mobileData != 0) {
            averageCostPerByte = (mobileCostPerByte * mobileData + wifiCostPerByte * wifiData) /(mobileData + wifiData);
        } 

        long totalRx = mUid.getTcpBytesReceived(mStatsType);
        long totalTx = mUid.getTcpBytesSent(mStatsType);
        double power = (totalRx + totalTx) * averageCostPerByte;
        return new DataUsage(totalRx, totalTx, power, TrafficStats.getUidRxBytes(mUid.getUid()), 
                TrafficStats.getUidTxBytes(mUid.getUid()));
    }

    private PkgUsage loadPkgUsage(UidInfo uInfo, SizeObserver sizeObserver) {
        boolean succeeded;
        long cache=0L, code=0L, data=0L;

        if(uInfo.mPackages != null) {
            for(int i = 0; i < uInfo.mPackages.length; ++i) {
                String pkg = uInfo.mPackages[i];
                CountDownLatch cdl = new CountDownLatch(1);
                sizeObserver.getSize(mContext, pkg, cdl);
                try {
                    cdl.await();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Failed computing size for pkg : "+ pkg);
                }   
        
                PackageStats pStats = sizeObserver.stats;
                succeeded = sizeObserver.succeeded;
                if(succeeded && pStats != null) {
                      cache += pStats.cacheSize;
                      code += pStats.codeSize;
                      data +=  pStats.dataSize;
                }   
            }
        }
        return new PkgUsage(cache, code, data);
    }

    private class SizeObserver extends IPackageStatsObserver.Stub {
        private CountDownLatch mCount;
        PackageStats stats = null;
        boolean succeeded = false;

        public void getSize(Context ctx, String packageName, CountDownLatch cdl) {
            mCount = cdl;
            stats = null;
            succeeded = false;
            ctx.getPackageManager().getPackageSizeInfo(packageName, this);
        }

        public void onGetStatsCompleted(PackageStats pStats, boolean pSucceeded)
                throws RemoteException {
            succeeded = pSucceeded;
            stats = pStats;
            mCount.countDown();
        }
    }

    class PkgUsage {
        private long mCacheSize;
        private long mCodeSize;
        private long mDataSize;
        public PkgUsage(long cache, long code, long data) {
            mCacheSize = cache;
            mCodeSize = code;
            mDataSize = data;
        }

        public String formatForCheckin() {
            return new String("code=" + mCodeSize + ";data=" + mDataSize + ";cache=" +
                   mCacheSize + ";");
        }        
    }
     
    class DataUsage {
        private long mTotalRx;
        private long mTotalTx;
        private double mTotalPower;
        private long mNetRx;
        private long mNetTx;

        public DataUsage(long rx, long tx, double power, long netRx, long netTx) {
            mTotalRx = rx;
            mTotalTx = tx;
            mTotalPower = power;
            mNetRx = netRx;
            mNetTx = netTx;
        }

        public double getDataPower() { return mTotalPower; }

        public long getDataUsage() { return mTotalRx + mTotalTx; }

        public long getNetDataUsage() { return mNetRx + mNetTx; }
    }

    class PerSensor {
        private int mSensorType;
        private long mTimeinSec;
        private double mPower;
        private int mCount;

        PerSensor(int type, long time, double power, int count) {
            mSensorType = type;
            mTimeinSec = time;
            mPower = power;
            mCount = count;
        }

        public int getSensorType() { return mSensorType; }

        public long getTime() { return mTimeinSec; }

        public int getCount() { return mCount; }
    }


    class SensorUsage {
        private long mTotalSensorTime;
        private double mTotalSensorPower;
        private int mTotalSensorCount;
        private ArrayList<PerSensor> mPerSensor;

        public SensorUsage(long at, double p, int cnt, ArrayList <PerSensor> s) {
            mTotalSensorTime = at;
            mTotalSensorPower = p;
            mTotalSensorCount = cnt;
            mPerSensor = s;
        }
        public double getSensorPower() { return mTotalSensorPower; }

        public String formatForCheckin()  {
            if (mPerSensor.size() > 0) {
                Iterator<PerSensor> i = mPerSensor.iterator();
                while (i.hasNext()) {
                    PerSensor s = i.next();
                    if (s.getSensorType() == BatteryStats.Uid.Sensor.GPS) {
                        return new String("gps_t=" + s.getTime() + ";gps_cnt=" + s.getCount() + ";");
                    }
               }
            }
            // GPS sensor not used, return zero's
            return new String("gps_t=0;gps_cnt=0;");
        }
    }

    class CpuUsage {
        private long mTotalCpuTime;
        private double mCpuPower;
        public CpuUsage(long time, double power) {
            mTotalCpuTime = time;
            mCpuPower = power;
        }

        public double getCpuPower() { return mCpuPower; }

    }

    class WakelockUsage {
        private long mFullWlTime;
        private long mPartialWlTime;
        private int mFullWlCount;
        private int mPartialWlCount;

        public WakelockUsage(long fullWlTime, int fullWlCount, long partialWlTime, 
                int partialWlCount) {
            mFullWlTime = fullWlTime;
            mPartialWlTime = partialWlTime;
            mFullWlCount = fullWlCount;
            mPartialWlCount = partialWlCount;
        }
  
        public String formatForCheckin()  {
            return new String("wl_full=" + mFullWlTime + ";" + "wl_fcnt=" + mPartialWlCount + ";"+
                    "wl_partial=" + mPartialWlTime + ";" + "wl_pcnt=" + mPartialWlCount + ";");
        }        
    }

    class ApplicationUsage {
        private int mUid;
        private WakelockUsage mWl;
        private CpuUsage mCpu;
        private DataUsage mDu;
        private SensorUsage mSu;
        private PkgUsage mPu;
        private String mPkgName;
        private Double mPower;
      
        public ApplicationUsage(int uid, String name, WakelockUsage wl, CpuUsage cpu, DataUsage du,
               SensorUsage su, PkgUsage pu) {
            mUid = uid;
            mPkgName = name;
            mWl = wl; 
            mCpu = cpu; 
            mDu = du; 
            mSu = su; 
            mPu = pu; 
            mPower = getPower();
        }

        private Double getPower() {
            return new Double(mCpu.getCpuPower() + mDu.getDataPower() + mSu.getSensorPower());
        }

        public String formatForCheckin() {
        // Time being removing the check
//           if (mPower != 0) {
                DecimalFormat df = new DecimalFormat("0.0000");
                return new String("[ID=uidStat;pkg=" + mPkgName + ";" + mWl.formatForCheckin() + 
                        "power=" + df.format(mPower) + ";" + "bytes=" + mDu.getDataUsage() + ";" +
                        "netstatbytes=" + mDu.getNetDataUsage() + ";" +
                        mPu.formatForCheckin() + mSu.formatForCheckin() + "]");
 //          } else { 
 //              return null; 
 //          }
        }
    }
}
