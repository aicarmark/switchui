/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: StatsUploader.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Feb 3, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import static android.text.format.DateUtils.WEEK_IN_MILLIS;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.devicestatistics.BatteryStatsState.BatteryStatsConstants;
import com.motorola.devicestatistics.BatteryStatsState.SensorData;
import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.eventlogs.BatteryLogger;
import com.motorola.devicestatistics.eventlogs.EventConstants;
import com.motorola.devicestatistics.eventlogs.EventNote;
import com.motorola.devicestatistics.packagemap.MapDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * @author bluremployee
 *
 */
public class StatsUploader {
    
    private final static String TAG = "StatsUploader";
    private final static boolean DUMP = DevStatUtils.GLOBAL_DUMP;
    /**
     * The capacity of the battery as measured on the last charge cycle that went from "5-" to "95+"
     */
    private static final String BATTERY_HEALTH_LAST_CAPACITY = "cycle_count";

    /**
     * The time at which the the above cycle_count file was updated
     */
    private final static String BATTERY_HEALTH_TIME =
            "/sys/module/cpcap_battery/parameters/timestamp";
    private final static String QCOM_BATTERY_HEALTH_TIME =
            "/sys/module/pm8921_bms/parameters/timestamp";

    // As per Ilya with inputs from Kenny, when the timestamp is not available, the recommendation
    // is to send health value no more often then once every 2 weeks.
    private static final Long NO_TIMESTAMP_DELAY_SEC = 2 * WEEK_IN_MILLIS/1000;

    private Context mContext;

    // When charger is connected/disconnected frequently, the devicestats from one disconnect overlaps
    // with the peruidtstas from another disconnect. This is because the peruidstats takes 5-10 seconds
    // to get logged, while devicestats gets logged immediately. A lock is used here to ensure that
    // the logs come in proper order.
    private final static SemaphoreWrapper sLock = new SemaphoreWrapper(1,true);

    public StatsUploader(Context context) {
        mContext = context;
    }
    
    public void upload(final int type, final long basetime, final boolean incremental) {
        StatsController controller = 
                StatsController.getController(mContext.getApplicationContext());
        if (!sLock.acquire()) return;
        controller.forceTrigger(type, new Runnable() {
            public void run() {
                new Uploader(mContext, type, basetime, incremental).start();
            }
        });
    }

    public void collectWithoutUpload(final int type) {
        new Utils.RunInBackgroundThread() {
            public void run() {
                StatsController controller =
                    StatsController.getController(mContext.getApplicationContext());
                if(!sLock.acquire()) return;
                controller.forceTrigger(type, new Runnable() {
                    public void run() {
                        sLock.release();
                    }
                });
            }
        };
    }

    private static class Uploader extends Thread {
        private final static int MAX_LENGTH = 4000;
        private final static String LAST_PERUID_STATS_FILE = "peruidstats";
        private final static String[] BLOCK_DEVICES = new String[] {"mmcblk0", "mmcblk1"};

        private final static int[] REPORT_DB_TYPES = { StatsDatabase.Type.CUMULATIVE,
            StatsDatabase.Type.SCREEN_ON, StatsDatabase.Type.SCREEN_OFF };
        private final static String[] REPORT_DB_CHECKIN_TAG = { DevStatPrefs.CHECKIN_EVENT_ID,
            DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER, DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER };
        private final static String[] REPORT_DB_ID_SUFFIX = { "", "SOn", "SOff" };
        private final static int CUMULATIVE_DB_INDEX = 0;
        private final static int MAX_DB_TYPES = 3; // cumulative, screenon, screenoff
        private static final String BATTERY_ID = "Battery";
        private static final String PERUIDSTATS_ID = "PerUidStats";

        private int mDbIndex;

        static class SavedUidStats extends HashMap<Integer,String> implements Serializable {
            private static final long serialVersionUID = 1L;
        }

        private static class SizeObserver extends IPackageStatsObserver.Stub {
            private CountDownLatch mCount;
            PackageStats stats = null;
            boolean succeeded = false;

            public void getSize(PackageManager pm, String packageName, CountDownLatch cdl) {
                mCount = cdl;
                stats = null;
                succeeded = false;
                pm.getPackageSizeInfo(packageName, this);
            }

            public void onGetStatsCompleted(PackageStats pStats, boolean pSucceeded)
                    throws RemoteException {
                succeeded = pSucceeded;
                stats = pStats;
                mCount.countDown();
            }
        }

        Context mContext;
        int mType;
        long mCheckinTime;
        DsCheckinEvent mCheckinEvent;
        MapDbHelper mMapper;
        ArrayList<DsCheckinEvent> mLogs;
        SizeObserver mSizeObserver;
        private SavedUidStats mLastUidStats[], mNewUidStats[];
        private boolean mTriggeredByUsb;

        public Uploader(Context context, int type, long baseTime, boolean triggeredByUsb) {
            mContext = context;
            mType = type;
            mCheckinTime = baseTime;
            mCheckinEvent = null;
            mLogs = new ArrayList<DsCheckinEvent>();
            mSizeObserver = new SizeObserver();
            mTriggeredByUsb = triggeredByUsb;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                runImpl();
            } finally {
                sLock.release();
            }
        }

        private final void runImpl() {
            mLastUidStats = (SavedUidStats[])DevStatUtils.loadObjectFromFile(
                    mContext, LAST_PERUID_STATS_FILE, SavedUidStats[].class);
            if (mLastUidStats == null || mLastUidStats.length != MAX_DB_TYPES) {
                mLastUidStats = createEmptySavedUidStats();
            }
            mDbIndex = 0;
            mNewUidStats = createEmptySavedUidStats();
            MapDbHelper dbHelper = MapDbHelper.getInstance(mContext.getApplicationContext());
            
            synchronized(dbHelper) {
                mMapper = dbHelper;
                mMapper.startLog();
                String[] groups = getGroups(mType);

                for ( int dbIndex=0; dbIndex<REPORT_DB_TYPES.length; dbIndex++ ) {
                    mDbIndex = dbIndex;
                    if(groups == null) continue;

                    // Prevent overlap between BatteryStatsAccumulator and StatsUploader
                    synchronized(DatabaseIface.sqliteDbLock) {
                        StatsDatabase db = StatsDatabase.load(mContext, REPORT_DB_TYPES[dbIndex],
                                groups);

                        for(int i = 0; i < groups.length; ++i) {
                            if(db.getGroupSize(groups[i]) <= 0) continue;

                            if(DUMP) Log.v(TAG, "Processing group:" + groups[i]);

                            Iterator<String> subids = db.getGroupSubIds(groups[i]);
                            if (subids != null) {
                                mCheckinEvent = null;
                                while(subids.hasNext()) {
                                    String subid = subids.next();
                                    HashMap<String, String> kv = db.getSubIdPairs(groups[i], subid);
                                    if(kv != null) upload(mType, groups[i], subid, kv);
                                }
                            }
                            // TODO: Not fitting in well - change it in next iter
                            if("UID".equals(groups[i])) {
                                checkin(false );
                            }
                        }
                        db.cleanup();
                    }
                }
                mDbIndex = 0;
                HashMap<String, Long> mapLogs = mMapper.stopLog();
                uploadMapper(mapLogs);
                mMapper = null;
            }
            
            flushUploads();
            DevStatUtils.saveObjectToFile(mContext, LAST_PERUID_STATS_FILE, mNewUidStats );
            mLastUidStats = mNewUidStats = null;
        }
        
        private void upload(int type, String group, String subid, HashMap<String, String> kv) {
            if(type == DataTypes.BATTERY_STATS) {
                uploadBatteryStats(group, subid, kv);
            }
        }
        
        private String[] getGroups(int type) {
            if(type == DataTypes.BATTERY_STATS) {
                return new String[] {StatsDatabase.Group.DEFAULT, "UID"};
            }else {
                return null;
            }
        }
        
        private void uploadBatteryStats(String group, String subid, HashMap<String, String> kv) {
            final String DEFAULT = "default";
            final String UID = "UID";
            
            if(DEFAULT.equals(group)) {
                uploadTimeStats(kv);
                uploadRadioStats(kv);
                uploadDataStats(kv);
            }else if(UID.equals(group)) {
                uploadUidStats(kv);
            }
        }
        
        private void uploadTimeStats(HashMap<String, String> kv) {
            createNewCheckinEvent("PhoneTimeStats");

            CheckinHelper.addKeyValues(mCheckinEvent,
                    "tob_rt", decodeValue(kv.get("tob_rt")),
                    "tob_ut", decodeValue(kv.get("tob_ut")),
                    "trt_rt", decodeValue(kv.get("trt_rt")),
                    "trt_ut", decodeValue(kv.get("trt_ut")),
                    "sc_ot",  decodeValue(kv.get("sc_ot")));

            for(int i = 0; i < BatteryStatsConstants.NUM_SCREEN_BRIGHTNESS_BINS; ++i) {
                String k = BatteryStatsConstants.SCREEN_BRIGHTNESS_NAMES[i];
                CheckinHelper.addKeyValues(mCheckinEvent, k, decodeValue(kv.get(k)));
            }

            CheckinHelper.addKeyValues(mCheckinEvent, "ipe", decodeValue(kv.get("ipe")));

            String wo = kv.get("wifi_ot");
            String wr = kv.get("wifi_rt");
            if(wo != null) {
                CheckinHelper.addKeyValues(mCheckinEvent, "wifi_ot", decodeValue(wo),
                        "wifi_rt", decodeValue(wr));
            }

            wo = kv.get("bt_ot");
            if(wo != null) {
                CheckinHelper.addKeyValues(mCheckinEvent, "bt_ot", decodeValue(wo));
            }

            if ( mDbIndex == CUMULATIVE_DB_INDEX ) {
                // TODO: Find a better place to do this..
                appendFsStats();
            }

            SharedPreferences sp = mContext.getSharedPreferences(EventConstants.OPTIONS,
                    Context.MODE_PRIVATE);
            long dchrg = BatteryLogger.getCumulativeDischarge(sp, EventNote.getBattLevel());
            CheckinHelper.addKeyValues(mCheckinEvent, "cdchrg", String.valueOf(dchrg),
                    "ph_ot", decodeValue(kv.get("ph_ot")),
                    "gps_rt",decodeValue(kv.get("gps_rt")));

            checkin(false);

            // Send the last battery capacity information only once in 24 hours.
            if ( mDbIndex == CUMULATIVE_DB_INDEX && !mTriggeredByUsb) {
                createNewCheckinEvent(BATTERY_ID);
                if (appendBatteryHealthReport()) checkin(false);
            }

            if ( mDbIndex == CUMULATIVE_DB_INDEX ) {
                createNewCheckinEvent("MMCStats");
                appendMMCStats();
                checkin(false);
            }
        }
        private void uploadRadioStats(HashMap<String, String> kv) {
            createNewCheckinEvent("SignalStrengths");

            CheckinHelper.addKeyValues(mCheckinEvent,"sst", decodeValue(kv.get("sst"))); 
            for(int i = 0; i < BatteryStatsConstants.NUM_SIGNAL_STRENGTH_BINS; ++i) {
                String k = BatteryStatsConstants.SIGNAL_STRENGTH_NAMES[i];
                CheckinHelper.addKeyValues(mCheckinEvent, k + "_t", decodeValue(kv.get(k + "_t")),
                        k + "_cnt", decodeValue(kv.get(k + "_cnt")));
            }
            checkin(false);
        }
        
        private void uploadDataStats(HashMap<String, String> kv) {
            // This is required here since we want to preserve the order of checkin
            final String[] DATA_CONN_NAMES = {
                "rt_none", "rt_gprs", "rt_edge", "rt_umts", "rt_other",
                "rt_cdma", "rt_evdo0", "rt_evdoA",
                "rt_1xrtt", "rt_hsdpa", "rt_hsupa", "rt_hspa", "rt_iden", "rt_evdob"
            };
            createNewCheckinEvent("RadioTypes");
            for(int i = 0; i < DATA_CONN_NAMES.length; ++i) {
                String k = DATA_CONN_NAMES[i];
                CheckinHelper.addKeyValues(mCheckinEvent, k+"_t", decodeValue(kv.get(k + "_t")),
                        k+"_cnt", decodeValue(kv.get(k + "_cnt")));
            }
            checkin(false);
        }
        
        private void uploadUidStats(HashMap<String, String> kv) {
            if(mCheckinEvent != null  && mCheckinEvent.length() > MAX_LENGTH) {
                checkin(false);
            }
            String u = kv.get("u");
            if(u != null) {
                int uid = Integer.parseInt(u);

                // First, check if uid exists
                PackageManager pm = mContext.getPackageManager();
                String[] pkgs = pm.getPackagesForUid(uid);
                if((pkgs == null || pkgs.length == 0) && uid >= Process.FIRST_APPLICATION_UID) {
                    return;
                }
                String name = chooseName(pkgs, uid, mMapper);
                if(name != null) {
                    long cache = 0, code = 0, data = 0;
                    if(pkgs != null) {
                        for(int i = 0; i < pkgs.length; ++i) {
                            String pkg = pkgs[i];
                            CountDownLatch cdl = new CountDownLatch(1);
                            mSizeObserver.getSize(pm, pkg, cdl);
                            try {
                                cdl.await();
                            } catch (InterruptedException e) {
                                Log.d(TAG, "Failed computing size for pkg : "+ pkg);
                            }

                            PackageStats pStats = mSizeObserver.stats;
                            if(mSizeObserver.succeeded && pStats != null) {
                                cache += pStats.cacheSize;
                                code += pStats.codeSize;
                                data +=  pStats.dataSize;
                            }
                        }
                    }

                    ArrayList<String> uidData = new ArrayList<String>();
                    String[] keyValues = { "pkg", name, "u", String.valueOf(uid),
                            "cpu", decodeValue(kv.get("cpu")), "byt", decodeValue(kv.get("byt")),
                            "nsb", decodeValue(kv.get("nsb"))};
                    for (String entry : keyValues) uidData.add(entry);

                    // Save this offset because it might be needed below
                    int pkgSizeOffset = uidData.size();

                    int len = BatteryStatsState.knownSensors.length;
                    HashMap<String,SensorData> allSensorData =
                        BatteryStatsState.decodeSensorData(kv.get("sensordata"));
                    for (int i=0; i<len; i+=2 ) {
                        String sensorName = (String)BatteryStatsState.knownSensors[i];
                        SensorData sensorData = allSensorData.get(sensorName);
                        if (sensorData!=null && (sensorData.time!=0)) {
                            long countValue = sensorData.count;
                            if ( countValue < 0 ) countValue = 0;

                            uidData.add(sensorName+"t");
                            uidData.add(String.valueOf(sensorData.time));
                            uidData.add(sensorName+"c");
                            uidData.add(String.valueOf(countValue));
                        }
                    }

                    addNonZeroData( uidData, "fwt", kv );
                    addNonZeroData( uidData, "fwc", kv );
                    addNonZeroData( uidData, "pwt", kv );
                    addNonZeroData( uidData, "pwc", kv );

                    String[] adKeyValues = {
                            "byt_mobile", decodeValue(kv.get("byt_mobile")),
                            "byt_wifi", decodeValue(kv.get("byt_wifi")),
                            "nsb_mobile", decodeValue(kv.get("nsb_mobile")),
                            "nsb_wifi", decodeValue(kv.get("nsb_wifi")),
                            "pkt", decodeValue(kv.get("pkt"))
                    };
                    for (String entry : adKeyValues) uidData.add(entry);

                    String thisUidData = uidData.toString();
                    mNewUidStats[mDbIndex].put(uid, thisUidData);

                    if ( !mTriggeredByUsb ||
                            !thisUidData.equals( mLastUidStats[mDbIndex].get(uid) ) ) {

                        // For backward compatibility at server side, the "code=" etc fields must be
                        // inserted in the middle of this string.
                        if ( mDbIndex == CUMULATIVE_DB_INDEX && !mTriggeredByUsb ) {
                            String[] pkgData = { "code", String.valueOf(code),
                                    "data", String.valueOf(data),
                                    "cache",  String.valueOf(cache) };
                            uidData.addAll(pkgSizeOffset, Arrays.asList(pkgData));
                        }

                        if (mCheckinEvent == null) createNewCheckinEvent(PERUIDSTATS_ID);
                        DsSegment segment = CheckinHelper.createNamedSegment("uid");
                        Iterator<String> iterator = uidData.iterator();
                        boolean valid = false;

                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            if (!iterator.hasNext()) break;
                            String value = iterator.next();
                            try {
                                if (Long.valueOf(value) == 0) continue;
                            } catch (Exception e) {
                                Log.e(TAG, "addPositiveValues " + value, e);
                            }

                            if (valid == false && !("u".equals(key)) && !("pkg".equals(key))) {
                                valid = true;
                            }
                            segment.setValue(key, value);
                        }

                        if (valid) mCheckinEvent.addSegment(segment);
                    }
                }
            }
        }

        private final void addNonZeroData( ArrayList<String> uidData, String type,
                HashMap<String, String> kv ) {
            String value = decodeValue(kv.get(type));
            try {
                if ( value != null && Long.valueOf( value ) > 0 ) {
                    uidData.add(type);
                    uidData.add(value);
                }
            } catch ( NumberFormatException exception ) {
                Log.e( TAG, "NumberFormatException while parsing " + value );
            }
        }
        
        private String chooseName(String[] pkgs, int uid, MapDbHelper mapper) {
            String name = null;
            long id = -1;
            if(pkgs == null || pkgs.length == 0) {
                name = "UID" + uid;
                id = mapper.getId(name, true);
            }else {
                for(int i = 0; i < pkgs.length; ++i) {
                    name = pkgs[i];
                    id = mapper.getId(name, false);
                    if(id != -1) break;
                }
                if(id == -1) {
                    id = mapper.getId(name, true);
                }
            }
            return id != -1 ? Long.toString(id) : null;
        }
        
        private void uploadMapper(HashMap<String, Long> cl) {
            if(!cl.isEmpty()) {
                createNewCheckinEvent("PMaps");
                Iterator<Map.Entry<String, Long>> values = cl.entrySet().iterator();
                while(values.hasNext()) {
                    Map.Entry<String, Long> value = values.next();
                    CheckinHelper.addUnnamedSegment(mCheckinEvent, "m",
                            String.valueOf(value.getValue()), value.getKey());
                }
                checkin(true);
            }
        }
        
        private void appendFsStats() {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            long availableBlocks = stat.getAvailableBlocks();

            CheckinHelper.addKeyValues(mCheckinEvent,
                    "datatotl", String.valueOf(blockSize*totalBlocks),
                    "datafree", String.valueOf(blockSize*availableBlocks));

            // Now SD card
            // On devices with both internal and external SD cards
            // this maps to the internal one
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
                    // this can occur if the SD card is removed, but the 
                    // ACTION_MEDIA_REMOVED Intent is not sent out yet.
                }
            }
            CheckinHelper.addKeyValues(mCheckinEvent, "sdtotl", String.valueOf(totalSdSize),
              "sdavl", String.valueOf(avlSdSize));
        }
        
        private void appendMMCStats() {
            CheckinHelper.addKeyValues(mCheckinEvent,"rt",
                    String.valueOf((SystemClock.elapsedRealtime()/1000)));
            for(int i = 0; i < BLOCK_DEVICES.length; ++i) {
                CheckinHelper.addKeyValues(mCheckinEvent, BLOCK_DEVICES[i],
                        String.valueOf(readMMCSectorWrites(BLOCK_DEVICES[i])));
            }
        }

        private long readMMCSectorWrites(String device) {
            byte[] data;
            long count = 0;
            data = readFile("/sys/block/" + device + "/stat");
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
                Log.v(TAG, "Exception reading MMC stat file" + ex);
            }finally {
                try {
                    if(ifs != null) ifs.close();
                }catch(IOException ioEx) {
                }
            }
            return null;
        }
        
        private String decodeValue(String v) {
            return v == null ? "0" : v;
        }
        
        private void createNewCheckinEvent(String segment0Id) {
          String logTag = REPORT_DB_CHECKIN_TAG[mDbIndex];
          String logVersion = DevStatPrefs.VERSION;

          if (BATTERY_ID.equals(segment0Id)) {
              logTag = DevStatPrefs.BATTERY_STATS_TAG;
          }
          if ( mDbIndex == CUMULATIVE_DB_INDEX && mTriggeredByUsb ) {
              logTag = DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER;
          }
          if (PERUIDSTATS_ID.equals(segment0Id)) {
              logVersion = DevStatPrefs.PERUIDSTATS_VERSION;
          }

          mCheckinEvent = CheckinHelper.getCheckinEvent( logTag,
                  segment0Id + REPORT_DB_ID_SUFFIX[mDbIndex], logVersion, mCheckinTime );
        }

        private void checkin(boolean atFront) {
            if (mCheckinEvent == null) return;
            if(atFront) {
                mLogs.add(0, mCheckinEvent);
            }else {
                mLogs.add(mCheckinEvent);
            }
            mCheckinEvent = null;
        }
        
        private void flushUploads() {
            int N = mLogs.size();
            for(int i = 0; i < N; ++i) {
                DsCheckinEvent checkinEvent = mLogs.get(i);
                if (DUMP) Log.v(TAG, checkinEvent.serializeEvent().toString());
                checkinEvent.publish(mContext.getContentResolver());
            }
        }

        private final SavedUidStats[] createEmptySavedUidStats() {
            SavedUidStats[] savedUidStats = new SavedUidStats[MAX_DB_TYPES];
            for ( int i=0; i<MAX_DB_TYPES; i++ ) {
                savedUidStats[i] = new SavedUidStats();
            }
            return savedUidStats;
        }

        /**
         * Report the last % battery capacity and the time at which it was computed
         * Comments in IKHSS6-2897 indicate the conditions under which the last battery
         * capacity is recomputed. We report if either the last capacity has changed or
         * if the time of computation has changed.
         * @return true if a battery health report was generated
         */
        private final boolean appendBatteryHealthReport() {
            String batteryPath = Utils.getBatteryPath();
            if (batteryPath == null) {
                return false;
            }
            // Construct the path to cycle_count file.
            // /sys/class/power_supply/xxxxbattery/cycle_count
            batteryPath = batteryPath + Utils.FILENAME_SEPARATOR + BATTERY_HEALTH_LAST_CAPACITY;
            String lastCapacity = FileLinesReader.getFirstLine(batteryPath);
            if (lastCapacity==null || lastCapacity.isEmpty()) {
                Log.e(TAG, "Invalid cycle_count " + lastCapacity);
                return false;
            }
            String changeTimeSec = "0";
            // Read the time in epoch at which last battery capacity was updated
            File f1 = new File (BATTERY_HEALTH_TIME);
            File f2 = new File (QCOM_BATTERY_HEALTH_TIME);
            if (f1.exists()) {
                changeTimeSec = FileLinesReader.getFirstLine(BATTERY_HEALTH_TIME);
            } else if (f2.exists()) {
                changeTimeSec = FileLinesReader.getFirstLine (QCOM_BATTERY_HEALTH_TIME);
            } else {
                Log.d(TAG, "Sys File: File not found " + BATTERY_HEALTH_TIME);
            }
            if (changeTimeSec == null) {
                Log.e(TAG, "battery last capacity computation time is null");
                changeTimeSec = "NULL";
            }

            // Don't send 0, which happens after factory reset, to portal
            if ("0".equals(lastCapacity)) {
                return false;
            }

            SharedPreferences sp = mContext.getSharedPreferences(DevStatPrefs.PREFS_FILE,
                    Context.MODE_PRIVATE);
            String oldCapacity = sp.getString(DevStatPrefs.DEVSTATS_BATTERY_CYCLECOUNT, "");
            String oldTimestamp = sp.getString(DevStatPrefs.DEVSTATS_BATTERY_TIMESTAMP_SEC, "");

            // changeTimeSec is 0, implies BATTERY_HEALTH_TIME file is not available
            if ("0".equals(changeTimeSec)) {
                if (oldCapacity.equals(lastCapacity)) {
                    return false;
                }
                // since there is no file to note the timestamp for change in battery capacity,
                // we report it as current time.
                long nowSec = System.currentTimeMillis() / 1000;
                if (!oldTimestamp.isEmpty()) {
                    try {
                        if (Math.abs(nowSec-Long.valueOf(oldTimestamp)) < NO_TIMESTAMP_DELAY_SEC) {
                            return false;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "battery health time " + nowSec + ":" + oldTimestamp, e);
                    }
                }

                changeTimeSec = Long.toString(nowSec);
            } else {
                if (oldCapacity.equals(lastCapacity) && oldTimestamp.equals(changeTimeSec)) {
                    return false;
                }
            }
            SharedPreferences.Editor edit = sp.edit();
            edit.putString(DevStatPrefs.DEVSTATS_BATTERY_CYCLECOUNT, lastCapacity);
            edit.putString(DevStatPrefs.DEVSTATS_BATTERY_TIMESTAMP_SEC, changeTimeSec);
            Utils.saveSharedPreferences(edit);

            CheckinHelper.addKeyValues(mCheckinEvent, "bhc", lastCapacity, "bht", changeTimeSec);
            return true;
        }
   }

    static class SemaphoreWrapper{
        final Semaphore sem;

        SemaphoreWrapper(int permits, boolean fair) {
            sem = new Semaphore(permits,fair);
        }

        final boolean acquire() {
            boolean result = false;
            try {
                sem.acquire();
                result = true;
            } catch ( InterruptedException e ) {
                Log.e( TAG, Log.getStackTraceString(e) );
            }
            return result;
        }

        final void release() { sem.release(); }
    }
}
