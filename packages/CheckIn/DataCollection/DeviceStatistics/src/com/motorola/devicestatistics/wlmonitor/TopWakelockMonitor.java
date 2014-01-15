package com.motorola.devicestatistics.wlmonitor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.os.SystemProperties;
import android.util.Log;
import android.util.SparseArray;

import com.motorola.devicestatistics.CheckinHelper;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;
import com.motorola.devicestatistics.DevStatPrefs;
import com.motorola.devicestatistics.DevStatUtils;
import com.motorola.devicestatistics.DeviceStatsConstants;
import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.wlmonitor.JavaWakelocks.UidToLockDataMap;
import com.motorola.devicestatistics.wlmonitor.WakelockMonitorService.WakelockEntry;

/**
 * Class for monitoring the top java/kernel/window wakelocks held for each day.
 */
final class TopWakelockMonitor implements WakelockMonitorConstants, DeviceStatsConstants {
    private static final String TAG = "TopWakelockMonitor";

    private static final boolean DUMP = GLOBAL_DUMP;
    private static final boolean VERBOSE_DUMP = false;

    // update this whenever the format of the data serialized by this class changes
    private static final int SERIALIZE_VERSION = 1;

    //The values returned by BatteryStats should get reset whenever usb is unplugged
    private static final int BATTERY_STATS_TYPE = BatteryStats.STATS_SINCE_UNPLUGGED;
    private static final int DUMMY_KERNEL_UID = -1; // Dummy uid for kernel wakelocks

    /**
     * Name of file to which top wakelock data is serialized
     */
    private static final String WAKELOCK_STATS_FILE = "wakelocks.bin";

    /**
     * Temporary file that is renamed to WAKELOCK_STATS_FILE at the end, if serialization happened
     * successfully.
     */
    private static final String WAKELOCK_STATS_FILE_TEMP = "wakelocks-tmp.bin";

    // If this list/order is changed, the JAVA_WAKELOCK_TYPE and BATTSTATS_WAKELOCK_TYPE arrays
    // also need to be updated.
    private static final int PARTIAL_WAKELOCK_INDEX = 0;
    // Implicit: private static final int FULL_WAKELOCK_INDEX = 1;
    // Implicit: private static final int WINDOW_WAKELOCK_INDEX = 2;
    private static final int WAKELOCK_ARRAY_SIZE = 3;

    // This array should be in sync with WAKELOCK_ARRAY_SIZE and other constants above
    private static final char[] JAVA_WAKELOCK_TYPE = { 'p', 'f', 'w' };

    // This array should be in sync with WAKELOCK_ARRAY_SIZE and other constants above
    private static final int[] BATTSTATS_WAKELOCK_TYPE = {
        BatteryStats.WAKE_TYPE_PARTIAL,
        BatteryStats.WAKE_TYPE_FULL,
        BatteryStats.WAKE_TYPE_WINDOW
    };

    private static final char KERNEL_WAKELOCK_TYPE = 'k';

    // Possible values of sLastChargerState below
    private static final int CHARGER_UNKNOWN = -1;
    private static final int CHARGER_DISCONNECTED = 0;
    private static final int CHARGER_CONNECTED = 1;

    // The last known state of the USB.
    // Note that sLastKernelLockChargerState is static and not persisted to filesystem.
    // This is because otherwise if our process crashes and comes back, the charger state could
    // have changed many times in between. In that scenario, we cannot accumulate kernel wakelock
    // data based on sLastKernelLockChargerState persisted to filesystem.
    private static int sLastKernelLockChargerState = CHARGER_UNKNOWN;

    /**
     * 24 hour usage information for all Java partial/full/window wakelocks
     */
    private final JavaWakelocks[] mJavaWakelocks;

    /**
     * 24 hour usage information for all kernel wakelocks
     */
    private final HashMap<String,PerWakelockData> mKernelWakelocks;

    /**
     * true if previous data was successfully read from disk earlier
     */
    private boolean mValidDataReadFromDisk;

    /**
     * This is not really the kernel boot time, but is the the time at which system_server process
     * first came up. However when system server crashes and comes back, this value retains its
     * old value. In that sense, this value changes only if the kernel crashes and restarts,
     */
    private static Long sKernelBootTime;
    private Long mSystemServerBootTimeReadFromFile;

    /**
     * The pid of the system_server process
     */
    private static long sCurrentSystemServerPid;
    private long mSystemServerPidReadFromFile;

    private static final long INVALID_BOOT_TIME = 0;
    private static final String FIRST_BOOT_TIME_PROPERTY = "ro.runtime.firstboot";

    /**
     * Private constructor. The class can be created only via createFromFile
     */
    private TopWakelockMonitor() {
        mSystemServerPidReadFromFile = INVALID_PID;
        mJavaWakelocks = new JavaWakelocks[WAKELOCK_ARRAY_SIZE];
        for (int i=0; i<WAKELOCK_ARRAY_SIZE; i++) {
            mJavaWakelocks[i] = new JavaWakelocks();
        }
        mKernelWakelocks = new HashMap<String,PerWakelockData>();
    }

    /**
     * Retrieve and accumulate the java wakelock times on battery.
     * @param unplugTime The time at which the phone was last unplugged
     * @param batteryRealtime The current battery real time in microseconds
     * @param uidStats The android framework batterystats statistics for each uid
     */
    @SuppressWarnings("unused")
    final void handleJavaLocks(long unplugTime, long batteryRealtime,
            SparseArray<? extends Uid> uidStats) {
        for (int lockIndex = 0; lockIndex < WAKELOCK_ARRAY_SIZE; lockIndex++ ) {
            int wakeLockType = BATTSTATS_WAKELOCK_TYPE[lockIndex];

            JavaWakelocks wlContext = mJavaWakelocks[lockIndex];
            long lastUnplugTime = wlContext.mJavaLastUnplugTime;
            JavaWakelocks.NameToUidDataMap javaWakelock = wlContext.mJavaWakelocks;

            // Iterate over all the uids, and then over all the wakelocks for that uid
            final int numUids = uidStats.size();
            for (int index = 0; index < numUids; index++) {
                Uid uid = uidStats.valueAt(index);
                int uidNumber = uid.getUid();
                Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats =
                        uid.getWakelockStats();

                if (wakelockStats.size() <= 0) continue;

                for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> lockEntry
                        : wakelockStats.entrySet()) {
                    Uid.Wakelock lock = lockEntry.getValue();
                    if (lock==null) {
                        if (DUMP) Log.e(TAG, "value is null for wakelock " + lockEntry.getKey());
                        continue;
                    }
                    Timer timer = lock.getWakeTime(wakeLockType);
                    String lockName = lockEntry.getKey();
                    if (lockName==null || timer==null) {
                        if (DUMP && lockName == null) {
                            Log.d(TAG, "name is null for timer " + " " + timer);
                        }
                        continue;
                    }

                    // Get the time and count for this wakelock since the phone was last unplugged
                    long timeMs = timer.getTotalTimeLocked(batteryRealtime,
                            BATTERY_STATS_TYPE) / MICROSEC_IN_MSEC;
                    long count = timer.getCountLocked(BATTERY_STATS_TYPE);

                    JavaWakelocks.UidToLockDataMap uidMap = javaWakelock.get(lockName);
                    if (uidMap==null) {
                        uidMap = new JavaWakelocks.UidToLockDataMap();
                        javaWakelock.put(lockName, uidMap);
                    }
                    PerWakelockData wlData = uidMap.get(uidNumber);
                    if (wlData==null) {
                        wlData = new PerWakelockData();
                        uidMap.put(uidNumber, wlData);
                    }

                    /* After data has been reported to checkin, the wakelockstats.bin gets
                     * deleted. The mValidDataReadFromDisk check below is to ensure that when
                     * this code gets called after the above delete, we don't double count the
                     * data that we report to checkin.
                     */
                    if  (mValidDataReadFromDisk) {
                        if (lastUnplugTime==unplugTime) {
                            // If the time "read from filesystem" at which phone was last unplugged,
                            // is same as that reported by BatteryManager, then add the incremental
                            // change to the cumulative usage of this wakelock
                            long countDiff = count - wlData.mPreviousCount;
                            long timeDiff = timeMs - wlData.mPrevioustTimeMs;

                            // -1 is for count going back in android framework batterystats
                            if (countDiff >= -1 && timeDiff >= 0) {
                                wlData.mAccumulatedCount += countDiff;
                                wlData.mAccumulatedTimeMs += timeDiff;
                                wlData.noteDetectTime();
                            } else {
                                Log.e(TAG, "Invalid countDiff/timeDiff " + lockName +
                                        countDiff + " " + timeDiff);
                            }
                        } else {
                            // Since the time of last unplug is different, this is a new unplugged
                            // session. Hence add the full values to the cumulative usage of this
                            // wakelock.
                            wlData.mAccumulatedCount += count;
                            wlData.mAccumulatedTimeMs += timeMs;
                        }
                    }
                    // Save the current values reported by batterymanager, for use when this
                    // function gets called next
                    wlData.mPreviousCount = count;
                    wlData.mPrevioustTimeMs = timeMs;
                }
            }

            // Save the time at which phone was last unplugged, for use when this function gets
            // called next
            wlContext.mJavaLastUnplugTime = unplugTime;
        }
    }

    /**
     * Accumulate the passed current kernel wakelock usage, to the daily kernel wakelock usage
     * on battery
     * @param chargerConnected true if the charger is currently connected
     * @param locks The current kernel wakelock usage
     */
    final void handleKernelLocks(boolean chargerConnected, HashMap<String, WakelockEntry> locks) {
        if (DUMP) Log.d(TAG,"In handleKernelLocks lastcharger " + sLastKernelLockChargerState );

        for ( Map.Entry<String, WakelockEntry> lock : locks.entrySet() ) {
            String lockName = lock.getKey();
            WakelockEntry lockEntry = lock.getValue();
            if (lockName==null || lockEntry==null) {
                if (DUMP) Log.d(TAG, "name=" + lockName + " wl=" + lockEntry);
                continue;
            }

            PerWakelockData wakeLockUsage = mKernelWakelocks.get(lockName);
            if (wakeLockUsage==null) {
                wakeLockUsage = new PerWakelockData();
                mKernelWakelocks.put(lockName, wakeLockUsage);
            }

            if (mValidDataReadFromDisk) {
                // If the charger was disconnected earlier,
                // then accumulate the incremental wakelock usage on battery
                if (sLastKernelLockChargerState == CHARGER_DISCONNECTED) {
                    long countDiff = lockEntry.mCount - wakeLockUsage.mPreviousCount;
                    long timeDiff = lockEntry.mDuration - wakeLockUsage.mPrevioustTimeMs;

                    // -1 is for count going back in android framework batterystats
                    if (countDiff>=-1 && timeDiff>=0) {
                        wakeLockUsage.mAccumulatedCount += countDiff;
                        wakeLockUsage.mAccumulatedTimeMs += timeDiff;
                        wakeLockUsage.noteDetectTime();
                    } else {
                        Log.e(TAG, "Invalid count/time " + lockName + " " + lockEntry.mCount + " " +
                                wakeLockUsage.mPreviousCount + " " + lockEntry.mDuration + " " +
                                wakeLockUsage.mPrevioustTimeMs );
                    }
                }
            }

            // Save the current wakelock usage on battery
            wakeLockUsage.mPreviousCount = lockEntry.mCount;
            wakeLockUsage.mPrevioustTimeMs = lockEntry.mDuration;
        }

        // Save the current charger usage for use when this function is called next
        sLastKernelLockChargerState = chargerConnected ? CHARGER_CONNECTED : CHARGER_DISCONNECTED;
    }

    /**
     * Add the java wakelocks held for at least a threshold duration, to the sortData ArrayList
     * @param allNames If non-null, the names of all added wakelocks will be added to this as well
     * @param sortData List of wakelocks to be considered for reporting to checkin
     * @param mJavaWakelocks Current daily wakelock usage for full/partial/window wakelocks
     * @param lockType Identifies the type of wakelocks passed in mJavaWakelocks
     */
    private final void addLargeJavaWakelocks(TreeSet<String> allNames,
            ArrayList<WakelockSortData> sortData, JavaWakelocks.NameToUidDataMap mJavaWakelocks,
            char lockType ) {

        // Iterate over the names+usage of each java wakelock
        for (Map.Entry<String, JavaWakelocks.UidToLockDataMap> lockNames :
                mJavaWakelocks.entrySet() ) {
            String lockName = lockNames.getKey();
            for (Map.Entry<Integer, PerWakelockData> uids : lockNames.getValue().entrySet()) {
                PerWakelockData accumulatedLockUsage = uids.getValue();

                // Report only wakelocks held for at least a certain duration
                if (accumulatedLockUsage.mAccumulatedTimeMs >= WAKELOCK_REPORT_MIN_DURATION_MS) {
                    sortData.add( new WakelockSortData(lockName, uids.getKey(),
                            accumulatedLockUsage, lockType));

                    // Keep track of partial wakelocks, because otherwise they get duplicated as
                    // kernel wakelocks
                    if (allNames != null) allNames.add(lockName);
                }
            }
        }
    }

    /**
     * Report the top 20 wakelocks of the day to checkin.
     * The accumulated daily statistics is cleared as a side effect of calling this function.
     * @param context Android context
     */
    final void reportTopWakelocksToCheckin(Context context) {
        TreeSet<String> partialLockNames = new TreeSet<String>();
        ArrayList<WakelockSortData> sortData = new ArrayList<WakelockSortData>();

        for (int lockIndex=0; lockIndex<WAKELOCK_ARRAY_SIZE; lockIndex++) {
            TreeSet<String> duplicateCheckNames =
                    lockIndex==PARTIAL_WAKELOCK_INDEX ? partialLockNames : null;

            // Add the wakelocks held for larger times
            addLargeJavaWakelocks(duplicateCheckNames, sortData,
                    mJavaWakelocks[lockIndex].mJavaWakelocks,
                    JAVA_WAKELOCK_TYPE[lockIndex] );
        }

        // Add the kernel wakelocks
        for ( Entry<String, PerWakelockData> lock : mKernelWakelocks.entrySet() ) {
            PerWakelockData lockData = lock.getValue();
            // Include only wakelocks held for at least a minute
            if (lockData.mAccumulatedTimeMs >= WAKELOCK_REPORT_MIN_DURATION_MS) {
                String lockName = lock.getKey();

                if (partialLockNames.contains(lockName) == false ) {
                    // Java Partial wakelocks are implemented as kernel wakelocks.
                    // So since we have already included java partial wakelocks above, dont include
                    // kernel wakelocks with the same name as a partial java wakelock

                    sortData.add( new WakelockSortData(lockName, DUMMY_KERNEL_UID, lockData,
                            KERNEL_WAKELOCK_TYPE ) );
                }
            }
        }

        // Sort in descending order of time
        Collections.sort(sortData);

        int n = sortData.size();
        if (n>0) {
            PackageManager pm = context.getPackageManager();

            // Report only top 20 wakelocks
            if (n>WAKELOCK_REPORT_MAX_ITEMS) n = WAKELOCK_REPORT_MAX_ITEMS;

            // An example log looks like this
            // [ID=TopWakelocks;ver=4.7;time=1327320279;][j;p;Oops;3450;1;1327320243;10127;
            //    com.test.wakelocktest;com.test.wakelocktest;][k;main;1869;4;1327071932;]

            DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(
                    DevStatPrefs.CHECKIN_EVENT_ID, "TopWakelocks", DevStatPrefs.VERSION,
                    (System.currentTimeMillis()/MS_IN_SEC) );

            for (int i=0; i<n; i++) {
                WakelockSortData lockData = sortData.get(i);
                String encodedName = DevStatUtils.getUrlEncodedString(lockData.mName);

                DsSegment segment;
                int position;
                if (lockData.mLockType != KERNEL_WAKELOCK_TYPE) {
                    segment = CheckinHelper.createUnnamedSegment("j");
                    segment.setValue(1,String.valueOf(lockData.mLockType));
                    position = 2;
                } else {
                    segment = CheckinHelper.createUnnamedSegment(
                            String.valueOf(lockData.mLockType));
                    position = 1;
                }

                segment.setValue(position++, encodedName);
                segment.setValue(position++,
                        String.valueOf(lockData.mWakelock.mAccumulatedTimeMs/MS_IN_SEC));
                segment.setValue(position++,
                        String.valueOf(lockData.mWakelock.mAccumulatedCount));
                segment.setValue(position++,
                        String.valueOf(lockData.mWakelock.mDetectTimeMs/MS_IN_SEC));

                if (lockData.mLockType != KERNEL_WAKELOCK_TYPE) {
                    // this block is for java wakelocks

                    // report shareduserid name instead of uid number, when possible
                    String sharedUserName = pm.getNameForUid(lockData.mUid);
                    sharedUserName = sharedUserName == null ? String.valueOf(lockData.mUid):
                        DevStatUtils.getUrlEncodedString(sharedUserName);

                    String pkgs[] = pm.getPackagesForUid(lockData.mUid);
                    String pkg = (pkgs!=null && pkgs.length>0) ?
                            DevStatUtils.getUrlEncodedString(pkgs[0]) : "UNKNOWN";

                    segment.setValue(position++, String.valueOf(lockData.mUid));
                    segment.setValue(position++, sharedUserName);
                    segment.setValue(position++, pkg);
                }
                checkinEvent.addSegment(segment);
            }

            checkinEvent.publish(context.getContentResolver());
        }

        // Delete the accumulated statistics on disk
        if ( context.getFileStreamPath(WAKELOCK_STATS_FILE).delete() == false ) {
            Log.e(TAG, "Error deleting " + WAKELOCK_STATS_FILE );
        }
    }

    /**
     * Read the accumulated daily wakelock usage from disk
     * @param context Android context
     * @return TopWakelockMonitor Object containing accumulated wakelock usage for the day
     */
    static final TopWakelockMonitor createFromFile(Context context) {
        TopWakelockMonitor monitor = new TopWakelockMonitor();

        ObjectInputStream ois = null;
        try {
            // No BufferedInputStream being used here because
            // profiling showed a small performance degradation.
            ois = new ObjectInputStream( context.openFileInput(WAKELOCK_STATS_FILE) );
            monitor.readObject(ois);
            monitor.mValidDataReadFromDisk = true;
        } catch (FileNotFoundException e) {
            //Ignore. "monitor" is already initialized in this case.
        } catch (Exception e) {
            DevStatUtils.logExceptionMessage(TAG, "Error opening wakelock input", e);
            monitor = new TopWakelockMonitor();
        } finally {
            try {
                if (ois!=null) ois.close();
            } catch (Exception e) {
                DevStatUtils.logException(TAG, e);
            }
        }

        if (VERBOSE_DUMP) {
            Log.d(TAG,"Dumping wakelock data read from file");
            Log.d(TAG, "lklcs=" + sLastKernelLockChargerState + " vdrfd=" +
                    monitor.mValidDataReadFromDisk + " fssbt=" + sKernelBootTime +
                    " ssbtrff=" + monitor.mSystemServerBootTimeReadFromFile + " cssp=" +
                    sCurrentSystemServerPid + " ssprfd=" + monitor.mSystemServerPidReadFromFile);
            for (int i=0; i<WAKELOCK_ARRAY_SIZE; i++) {
                Log.d(TAG, "Java Lock Type " + i);
                JavaWakelocks lockTypeData = monitor.mJavaWakelocks[i];
                Log.d(TAG, "jlut=" + lockTypeData.mJavaLastUnplugTime);
                JavaWakelocks.NameToUidDataMap javaWakelocks = lockTypeData.mJavaWakelocks;
                for (Entry<String, UidToLockDataMap> e: javaWakelocks.entrySet()) {
                    StringBuilder sb = new StringBuilder("JavaLockName=").append(e.getKey());
                    for ( Entry<Integer, PerWakelockData> e2 : e.getValue().entrySet() ) {
                        PerWakelockData v = e2.getValue();
                        sb.append(" { uid=" + e2.getKey() + " t=" + v.mAccumulatedTimeMs + " c=" + v.mAccumulatedCount +
                                " pt=" + v.mPrevioustTimeMs + " pc=" + v.mPreviousCount +
                                " dt=" + v.mDetectTimeMs + " }");
                    }
                    Log.e(TAG, sb.toString());
                }
            }
            for (Entry<String, PerWakelockData> e : monitor.mKernelWakelocks.entrySet() ) {
                PerWakelockData v = e.getValue();
                Log.d(TAG, "KernelLockName=" + e.getKey() + " t=" + v.mAccumulatedTimeMs + " c=" + v.mAccumulatedCount +
                        " pt=" + v.mPrevioustTimeMs + " pc=" + v.mPreviousCount +
                        " dt=" + v.mDetectTimeMs );
            }
        }

        /*
         * To handle a special case of, say
         *     phone is on battery
         *     a kernel wakelock is held for say 2 hours
         *     phone is power cycled
         *     charger was connected/disconnected 100 times, and finally disconnected
         *     for the first 6 hours, the above wakelock does not exist in /proc/wakelocks
         *     suddenly after 6 hours, the wakelock is held and hence appears in /proc/wakelocks
         *     In this case, we need to clear the previous count/times values from previous boot
         */
        if ( sKernelBootTime == null ) {
            sKernelBootTime = SystemProperties.getLong(FIRST_BOOT_TIME_PROPERTY,
                    INVALID_BOOT_TIME);

            sCurrentSystemServerPid = DevStatUtils.getSystemServerPid(context);

            boolean clearJavaLocks = false;
            // clear java wakelock previous count/times if the phone rebooted or if system server
            // process restarted. Note that we dont clear if there was just a crash in our process,
            // because android frameworks wakelock stats collection is not impacted in that case.
            if ( sKernelBootTime.equals(INVALID_BOOT_TIME) ||
                    !sKernelBootTime.equals(monitor.mSystemServerBootTimeReadFromFile) ||
                    sCurrentSystemServerPid == INVALID_PID ||
                    monitor.mSystemServerPidReadFromFile != sCurrentSystemServerPid ) {
                clearJavaLocks = true;
            }

            if (DUMP) {
                Log.d(TAG, "fssbt=" + sKernelBootTime + "," +
                        monitor.mSystemServerBootTimeReadFromFile + " ssp=" +
                        sCurrentSystemServerPid + "," + monitor.mSystemServerPidReadFromFile );
            }

            // Our process may have restarted, or this is a new boot
            // Since we dont know what charger states changed in between, always clear previous
            // count/times of kernel wakelocks.
            monitor.clearPreviousDeltaValues(true, clearJavaLocks);
        }

        return monitor;
    }

    /**
     * Clear the previous count/times of all wakelock data
     * @param clearKernel true if kernel wakelocks should be updated
     * @param clearJava true of java wakelocks should be updated
     */
    private final void clearPreviousDeltaValues(boolean clearKernel, boolean clearJava) {
        if ( clearKernel ) {
            if (DUMP) Log.d(TAG,"Clearing previous count/time for kernellocks");
            for ( PerWakelockData lockData : mKernelWakelocks.values()) {
                lockData.mPreviousCount = 0;
                lockData.mPrevioustTimeMs = 0;
            }
        }

        if (clearJava) {
            if (DUMP) Log.d(TAG,"Clearing previous count/time for javalocks");
            for (int i=0; i<WAKELOCK_ARRAY_SIZE; i++) {
                for (UidToLockDataMap uidMap : mJavaWakelocks[i].mJavaWakelocks.values()) {
                    for (PerWakelockData lockData : uidMap.values()) {
                        lockData.mPreviousCount = 0;
                        lockData.mPrevioustTimeMs = 0;
                    }
                }
            }
        }
    }

    /**
     * Save the accumulated wakelock usage to disk
     * @param context Android context
     */
    final void writeToFile(Context context) {
        ObjectOutputStream oos = null;
        boolean fileWriteSucceeded = false;
        try {
            // First save the wakelock usage to a temporary file
            oos = new ObjectOutputStream( new BufferedOutputStream(
                    context.openFileOutput(WAKELOCK_STATS_FILE_TEMP, Context.MODE_PRIVATE)) );
            writeObject(oos);
            fileWriteSucceeded = true;
        } catch (Exception e) {
            DevStatUtils.logExceptionMessage(TAG, "Error writing wakelock stats", e);
        } finally {
            try {
                if (oos!=null) oos.close();
            } catch (Exception e) {
                fileWriteSucceeded = false;
                DevStatUtils.logExceptionMessage(TAG, "Error closing wakelock stats", e);
            }
        }

        File oldFile = context.getFileStreamPath(WAKELOCK_STATS_FILE_TEMP);
        if (fileWriteSucceeded) {
            // If everything succeeded, move the temp file to its correct name.
            File newFile = context.getFileStreamPath(WAKELOCK_STATS_FILE);
            if ( oldFile.renameTo(newFile) == false ) {
                Log.e( TAG, "Error renaming wakelock stats");
            }
        } else {
            // In case of errors, delete the file that we wrote
            if ( oldFile.delete() == false ) {
                Log.e(TAG, "Error deleting " + WAKELOCK_STATS_FILE_TEMP );
            }
        }
    }

    /**
     * Save the state of TopWakelockMonitor to an outputstream
     * @param out The outputstream to which the state should be saved
     * @throws IOException
     */
    final void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(SERIALIZE_VERSION);
        out.writeLong(sKernelBootTime);
        out.writeLong(sCurrentSystemServerPid);
        // Write the partial and full wakelocks
        for (int i=0;i<WAKELOCK_ARRAY_SIZE;i++) {
            mJavaWakelocks[i].writeObject(out);
        }
        out.writeInt(mKernelWakelocks.size()); // write the kernel wakelocks
        for (Entry<String, PerWakelockData> entry : mKernelWakelocks.entrySet()) {
            out.writeUTF(entry.getKey());
            entry.getValue().writeObject(out);
        }
    }

    /**
     * Read the state of this object from the specified inputstream
     * @param in The inputstream from which the state is to be read
     * @throws IOException
     * @throws ClassNotFoundException
     */
    final void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        if (version != SERIALIZE_VERSION) {
            throw new ClassNotFoundException("Expected " + SERIALIZE_VERSION + " got " + version);
        }
        mSystemServerBootTimeReadFromFile = in.readLong();
        mSystemServerPidReadFromFile = in.readLong();
        // Read the partial/full java wakelocks
        for (int i=0; i<WAKELOCK_ARRAY_SIZE; i++) {
            mJavaWakelocks[i].readObject(in);
        }
        // Read the kernel wakelocks
        for (int size=in.readInt(); size>0; size--) {
            String name = in.readUTF();
            PerWakelockData wl = new PerWakelockData();
            wl.readObject(in);
            mKernelWakelocks.put(name, wl);
        }
    }
}

/**
 * Information about each wakelock that is persisted to disk
 *
 */
final class PerWakelockData {
    /**
     * The total number of times this wakelock has been acquired
     */
    long mAccumulatedCount;
    /**
     * The total duration for which this wakelock has been held
     */
    long mAccumulatedTimeMs;
    /**
     * The last count value reported by Android BatteryStats module
     */
    long mPreviousCount;
    /**
     * The last time reported by BatteryStats module
     */
    long mPrevioustTimeMs;

    /**
     * Time at which we first observed this wakelock to be consuming time/count
     */
    long mDetectTimeMs;

    /**
     * Check if this lock has a non zero time or count, and if so note this time as the time
     * at which we first detected this wakelock, provided the time was not noted earlier
     */
    final void noteDetectTime() {
        if (mDetectTimeMs==0 && (mAccumulatedTimeMs>0 || mAccumulatedCount>0)) mDetectTimeMs = System.currentTimeMillis();
    }

    /**
     * Save this object to the specified outputstream
     * @param out
     * @throws IOException
     */
    final void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(mAccumulatedCount);
        out.writeLong(mAccumulatedTimeMs);
        out.writeLong(mPreviousCount);
        out.writeLong(mPrevioustTimeMs);
        out.writeLong(mDetectTimeMs);
    }

    /**
     * Read the state of this object from the specified inputstream
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    final void readObject(ObjectInputStream in) throws IOException {
        mAccumulatedCount = in.readLong();
        mAccumulatedTimeMs = in.readLong();
        mPreviousCount = in.readLong();
        mPrevioustTimeMs = in.readLong();
        mDetectTimeMs = in.readLong();
    }
}

/**
 * Class that determines usage/counts and persistence of java wakelocks
 *
 */
final class JavaWakelocks {
    private static final long INVALID_TIME = -1;

    long mJavaLastUnplugTime = INVALID_TIME;

    @SuppressWarnings("serial")
    static final class NameToUidDataMap extends HashMap<String,UidToLockDataMap> {};
    @SuppressWarnings("serial")
    static final class UidToLockDataMap extends TreeMap<Integer,PerWakelockData> {};

    final NameToUidDataMap mJavaWakelocks = new NameToUidDataMap();

    final void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(mJavaLastUnplugTime);
        out.writeInt(mJavaWakelocks.size());
        for (Map.Entry<String, UidToLockDataMap> nameEntry : mJavaWakelocks.entrySet()) {
            out.writeUTF(nameEntry.getKey());
            UidToLockDataMap uidMap = nameEntry.getValue();
            out.writeInt(uidMap.size());
            for ( Entry<Integer, PerWakelockData> uidEntry : uidMap.entrySet()) {
                out.writeInt(uidEntry.getKey());
                uidEntry.getValue().writeObject(out);
            }
        }
    }

    final void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        mJavaLastUnplugTime = in.readLong();
        for (int nameCount=in.readInt(); nameCount>0; nameCount--) {
            UidToLockDataMap nameData = new UidToLockDataMap();
            mJavaWakelocks.put(in.readUTF(),nameData);
            for (int uidCount=in.readInt(); uidCount>0; uidCount--) {
                PerWakelockData wl = new PerWakelockData();
                nameData.put(in.readInt(), wl);
                wl.readObject(in);
            }
        }
    }
}

/**
 * Helper class to sort wakelocks based on time
 *
 */
final class WakelockSortData implements Comparable<WakelockSortData>{
    final String mName;
    final int mUid;
    final PerWakelockData mWakelock;
    final char mLockType;

    WakelockSortData(String name, int uid, PerWakelockData wakelock, char lockType) {
        mName = name;
        mUid = uid;
        mWakelock = wakelock;
        mLockType = lockType;
    }

    public int compareTo(WakelockSortData another) {
        // Ensure that sort is in descending order of time
        return (int) (another.mWakelock.mAccumulatedTimeMs - mWakelock.mAccumulatedTimeMs);
    }
};
