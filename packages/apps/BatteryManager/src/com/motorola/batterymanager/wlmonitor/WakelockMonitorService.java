/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: WlMonitorService.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       FJM225       Created file
 *                -Jatin
 **********************************************************
 */

package com.motorola.batterymanager.wlmonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Timer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.SparseArray;
import android.util.Xml;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.motorola.batterymanager.devicestatistics.SysClassNetUtils;
import com.motorola.batterymanager.devicestatistics.DevStatPrefs;
import com.motorola.batterymanager.Utils;
import com.motorola.batterymanager.wlmonitor.WakelockMonitorConstants.Intents;

public class WakelockMonitorService extends Service {

    // Handler - Message Ids
    private static final int CHECK_STATUS = 3;
    private static final int DISABLE = 4;
    private static final int ENABLE = 5;
    private static final int USB_PLUGGED_IN = 6;
    private static final int USB_PLUGGED_OUT = 7;
    private static final int CHECKIN = 12;
    private static final int MONITOR_RESULT = 13;
    private static final int WHITELIST_INIT_DONE = 14;

    // WLMonitor - Force close Options
    private static final int AUTO_IGNORE = 2;
    private static final int AUTO_FORCECLOSE = 1;
    private static final int ASK_USER = 0;
 
    // Alarm Actions
    private final static String CHECK_STATUS_ACTION =
            "com.motorola.batterymanager.wlmonitor.CHECK_STATUS";
    private final static String CHECKIN_ACTION =
            "com.motorola.batterymanager.wlmonitor.CHECKIN";

    private static final String TAG = "WakelockMonitorSvc";
    private final static boolean DUMP = false;

    // Wakelock check intervals in milliseconds
    private final static long[] sTimeoutValues = {30000,
                                                  90000,
                                                  1800000,
                                                  2700000,
                                                  3600000,
                                                  7200000,
                                                  0};
    private final static long BUFFER = 15L * 60L * 1000L;
    //private final static long BUFFER = 15L * 1000L;

    private static HashMap<String, Integer> mAlarmToMessageMap =
            new HashMap<String, Integer>();
    static {
        mAlarmToMessageMap.put(CHECK_STATUS_ACTION, CHECK_STATUS);
        mAlarmToMessageMap.put(CHECKIN_ACTION, CHECKIN);
    }

    private boolean mMonitorSystem;
    private boolean mMonitorEnabled;
    private boolean mPlugged;
    private boolean mFlexInitDone;
    private long mTimeout;
    private long mCheckinTimeout;

    private EventReceiver mReceiver;
    private AlarmService mAlarmService;

    private static final String WHITELIST_STORE_FS_PATH = "whitelistconf.xml";

    private ArrayList<WhitelistEntry> mWhiteList
            = new ArrayList<WhitelistEntry>();

    private ArrayList<WakelockEntry> mStuckWakelockList
            = new ArrayList<WakelockEntry>();

    private SparseArray<? extends Uid> mSavedUidStats;
    private long mSavedStatTime;
    private long mSavedUpTime;
    private long mSavedBatteryRealtime;
    private long mSeqNumber;
    private long mSavedSeqNumber;
    private static Object sLock = new Object();

    @Override
    public int onStartCommand(Intent startInt, int flags, int startld) {
        Utils.Log.setContentResolver(getContentResolver());
        Utils.Log.setLogFlush(false);

        if(startInt == null) {
            Utils.Log.e(TAG, "onStartCommand: start Intent is null, note crash restart");
            Utils.Log.flush(getContentResolver());
            Utils.Log.setLogFlush(true);
            initStart();
            return Service.START_STICKY;
        }

        String action = startInt.getAction();

        Utils.Log.d(TAG, "onStartCommand: received action " + action);
        if(action != null && action.equals(Intents.ACTION_START_MONITOR)) {
            initServices();
            initOptions();
            initReceivers();
            startMonitor();
        }
        Utils.Log.flush(getContentResolver());
        Utils.Log.setLogFlush(true);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Utils.Log.d(TAG, "onDestroy: unregister everything");
        deinitServices();
        deinitReceivers();
    }

    @Override
    public IBinder onBind(Intent bindInt) {
        return null;
    }

    private void initStart() {
        initServices();
        initOptions();
        initReceivers();
        startMonitor();
    }

    private void initServices() {
        mAlarmService = new AlarmService(this, mPostMan);
    }

    private void initOptions() {
        SharedPreferences pref = getSharedPreferences(
                WakelockMonitorConstants.OPTIONS,
                Context.MODE_PRIVATE);
        int timeoutIndex = pref.getInt(
                WakelockMonitorConstants.KEY_WAKELOCK_TIMEOUT,
                WakelockMonitorConstants.DEF_TIMEOUT_INDEX);

        mTimeout = timeoutIndex >= sTimeoutValues.length || timeoutIndex < 0 ?
                sTimeoutValues[WakelockMonitorConstants.DEF_TIMEOUT_INDEX] :
                sTimeoutValues[timeoutIndex]; 

        mMonitorSystem = pref.getBoolean(
                WakelockMonitorConstants.KEY_SYSTEM_MONITOR,
                WakelockMonitorConstants.DEF_SYSTEM_MONITOR);

        mCheckinTimeout = pref.getLong(
                WakelockMonitorConstants.KEY_WAKELOCK_CHECKIN_INTERVAL,
                WakelockMonitorConstants.DEF_CHECKIN_INTERVAL);

        Utils.Log.d(TAG, "initOptions: timeout is :" + mTimeout +
                ", system monitor:" + mMonitorSystem);
    }

    private void initReceivers() {
        mReceiver = new EventReceiver(this);
    }

    private void deinitServices() {
        if(mAlarmService != null) { // 2011.10.21 added by jrw647 to fix cr 3328
            mAlarmService.stop();
        }
    }

    private void deinitReceivers() {
        if(mReceiver != null) { // 2011.12.20 added by jrw647 to fix cr 5300
            mReceiver.stop();
        }
    }

    private void startMonitor() {
        if(mTimeout == 0) {
            // We are disabled, suicide
            stopSelf();
            return;
        }
        mMonitorEnabled = true;
        mReceiver.start();
        mAlarmService.start(mAlarmToMessageMap.keySet().iterator());

        mAlarmService.setAlarm(CHECK_STATUS_ACTION, mTimeout);
        mAlarmService.setAlarm(CHECKIN_ACTION, mCheckinTimeout);
        mFlexInitDone = false;
        mWhiteList.clear();
        initWhitelistFromFlex();
    }

    private IMessenger mPostMan = new IMessenger() {
 
        public void onPostMessage(String id) {
            Integer i = mAlarmToMessageMap.get(id);
            if(DUMP) Utils.Log.v(TAG, "post arrived:" + id + i);
            if(i != null) {
                mHandler.sendEmptyMessage(i);
            }
        }
    };

    private ILockFoundListener mTerminator =
            new ILockFoundListener() {

        public void onLockFound(WakelockEntry lock) {
            lockFoundHook(lock);
        }
    };

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CHECK_STATUS:
                Utils.Log.d(TAG, "Handler: Received CHECK_STATUS req, env:" +
                        mMonitorEnabled + "," + mPlugged);
                monitorCheckStatusHook();

                if(mMonitorEnabled) {
                    if(!mPlugged) {
                        InvestigatorThread monitor = new InvestigatorThread(
                                WakelockMonitorService.this,
                                obtainMessage(MONITOR_RESULT));
                        monitor.start();
                    }
                }
                mAlarmService.setAlarm(CHECK_STATUS_ACTION,
                        mTimeout + BUFFER);
                break;
            case DISABLE:
                Utils.Log.d(TAG, "Handler: Stopping Monitoring");
                mMonitorEnabled = false;
                break;
            case ENABLE:
                Utils.Log.d(TAG, "Handler: Start Monitoring");
                mMonitorEnabled = true;
                break;
            case USB_PLUGGED_IN:
                Utils.Log.d(TAG, "Handler: Usb plugged in");
                mPlugged = true;
                mSeqNumber++;
                break;
            case USB_PLUGGED_OUT:
                Utils.Log.d(TAG, "Handler: Usb plugged out");
                mPlugged = false;
                mSeqNumber++;
                break;
            case CHECKIN:
                Utils.Log.d(TAG, "Handler: Starting checkin");
                checkinStuckWakelocks();
                mAlarmService.setAlarm(CHECKIN_ACTION, mCheckinTimeout);
                break;
            case WHITELIST_INIT_DONE:
                Utils.Log.d(TAG, "Handler: whitelist init done, read:" + msg.arg1);
                mFlexInitDone = true;
                break;
            case MONITOR_RESULT:
                Utils.Log.d(TAG, "Handler: received investigator result, plugged:" +
                        mPlugged);
                ArrayList<WakelockEntry> locks = (ArrayList<WakelockEntry>)msg.obj;
                if(!mPlugged && locks.size() > 0) {
                    processHeldLocks(locks, mFlexInitDone);
                }
                break;
            default:
                Utils.Log.d(TAG, "Handler: Unknown message");
                break;
            }
        }
    };

    private void monitorCheckStatusHook() {
        SysClassNetUtils.updateNetStats(this);
    }

    private void processHeldLocks(ArrayList<WakelockEntry> locks, boolean useflex) {
        Processor processor = new Processor(locks, useflex,
                getPackageManager(), mTerminator);
        processor.start();
    }

    private void checkinStuckWakelocks() {
        Uploader uploader = new Uploader(this);
        uploader.start();
    }

    private void lockFoundHook(WakelockEntry lock) {
        if(DUMP) Utils.Log.d(TAG, "Sending notification to performance manager");
        try {
            Intent showDialogintent = new Intent();
            showDialogintent.setClassName("com.motorola.PerformanceManager",
                    "com.motorola.PerformanceManager.PMWakelockDialog");
            showDialogintent.putExtra("com.motorola.PerformanceManager.WAKELOCKUID", 
                    lock.mUid);
            showDialogintent.putExtra("com.motorola.PerformanceManager.WAKEOCKTIME", 
                    lock.mDuration);
            showDialogintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(showDialogintent);
        }catch(Exception e) { }
    }

    private void initWhitelistFromFlex() {
        FlexReadThread dbReadThread = new FlexReadThread();
        dbReadThread.setCallback(mHandler.obtainMessage(WHITELIST_INIT_DONE));
        dbReadThread.start();
    }

    final static class Parser {
        public final static String DOC_START_TAG_ID = "whitelistapps";

        public final static String NODE_START_TAG_ID = "whitelistapp";
        public final static String NODE_ATTR_PKG_ID = "package";
        public final static String NODE_ATTR_FC_ID = "forceclose";

        public final static class WhiteListParserException extends Exception {
            private static final long serialVersionUID = 1L;

            public WhiteListParserException(String err) {
                super(err);
            }
        }
    }

    class FlexReadThread extends Thread {
        private Message mResult;

        public void setCallback(Message msg) {
            mResult = msg;
        }

        public void run() {
            FileReader whitelistConfReader = null;
            int numEntries = 0;

            try {
                File whitelistConf = new File(Environment.getRootDirectory(), WHITELIST_STORE_FS_PATH);
                whitelistConfReader = new FileReader(whitelistConf);

                XmlPullParser whitelistparser = Xml.newPullParser();

                whitelistparser.setInput(whitelistConfReader);

                // Parse to the start of the first tag - should always be DOC_START_TAG_ID
                int id;
                while(((id = whitelistparser.next()) != XmlPullParser.END_DOCUMENT) &&
                        (id != XmlPullParser.START_TAG))
                    ;

                if((id == XmlPullParser.END_DOCUMENT) ||
                        (!whitelistparser.getName().equals(Parser.DOC_START_TAG_ID))) {
                    throw new Parser.WhiteListParserException("Doc Start TAG error: not found or mismatch");
                }

                // We now have a valid start tag, start looking for the apps
                while(true) {
                    while(((id = whitelistparser.next()) != XmlPullParser.END_DOCUMENT) &&
                            (id != XmlPullParser.START_TAG))
                        ;

                    if(id == XmlPullParser.END_DOCUMENT) {
                        Utils.Log.d(TAG, "initWLDb: reached end of doc, so far read: " + numEntries);
                        break;
                    }

                    if(!whitelistparser.getName().equals(Parser.NODE_START_TAG_ID)) {
                        throw new Parser.WhiteListParserException("Node Start TAG error: mismatch");
                    }

                    // We have the node start tag, now look for the req attributes
                    String packageName = whitelistparser.getAttributeValue(null, Parser.NODE_ATTR_PKG_ID);
                    String fcOption = whitelistparser.getAttributeValue(null, Parser.NODE_ATTR_FC_ID);

                    if((packageName == null) || (fcOption == null)) {
                        throw new Parser.WhiteListParserException("Node Attr. error: missing an attribute: "
                                + numEntries);
                    }

                    int option;
                    if(fcOption.equals(WakelockMonitorConstants.WHITELIST_ACTION_FORCECLOSE)) {
                        option = AUTO_FORCECLOSE;
                    }else if(fcOption.equals(WakelockMonitorConstants.WHITELIST_ACTION_IGNORE)) {
                        option = AUTO_IGNORE;
                    }else {
                        throw new Parser.WhiteListParserException("Node Attr. error: unrecognized value:"
                                + numEntries);
                    }
                    WhitelistEntry entry = new WhitelistEntry(packageName, option);
                    mWhiteList.add(entry);
                    numEntries++;
                }
            }catch(Exception gEx) {
                Utils.Log.d(TAG, "initWlDb: Whitelist conf parsing, parsing error" + gEx);
            }finally {
                try {
                    if(whitelistConfReader != null) {
                        whitelistConfReader.close();
                    }
                }catch(IOException ioEx) {
                    Utils.Log.d(TAG, "initWlDb: IO exception closing reader");
                }
                mResult.arg1 = numEntries;
                mResult.sendToTarget();
            }
        }
    }

    // TODO: Try tp make this more generic as with AlarmService
    class EventReceiver extends BroadcastReceiver {
        private Context mContext;
        private int mPlugStatus;
        private boolean mRegistered;

        @Override
        public void onReceive(Context ctx, Intent bcIntent) {
            String bcast = bcIntent.getAction();

            if(bcast!= null && bcast.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int plugged = bcIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                if(plugged != mPlugStatus) {
                    mPlugStatus = plugged;
                    Utils.Log.d(TAG, "EventReceiver: Plugged --> " + mPlugStatus);
                    Message msg = mHandler.obtainMessage();
                    if(plugged == 0) { 
                        msg.what = USB_PLUGGED_OUT;
                    } else {
                        msg.what = USB_PLUGGED_IN;
                    }
                    //If below statement is commented out, that means its for debugging.
                    //In official release this should be uncommented.
                    mHandler.sendMessage(msg);
                }
            }
        }

        public EventReceiver(Context ctx) {
            mContext = ctx;
            mPlugStatus = -1;
            mRegistered = false;
        }

        public void start() {
            if(!mRegistered) {
                IntentFilter ifilter = new IntentFilter();
                ifilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                mContext.registerReceiver(this, ifilter);
                mRegistered = true;
                mPlugStatus = -1;
            }
        }

        public void stop() {
            if(mRegistered) {
                mContext.unregisterReceiver(this);
                mPlugStatus = -1;
                mRegistered = false;
            }
        }
    }

    class WhitelistEntry {
        String mPackage;
        int mAutoOption;

        WhitelistEntry(String pkg, int option) {
            mPackage = pkg;
            mAutoOption = option;
        }

        int getOption(String[] packages) {
            int option = ASK_USER;
            for(int i = 0; i < packages.length; ++i) {
                if(mPackage.compareToIgnoreCase(packages[i]) == 0) {
                    option = mAutoOption;
                    break;
                }
            }
            return option;
        }
    }

    class WakelockEntry {
        String[] mPackages;
        String mName;
        int mUid;
        int mType;
        long mDuration;
        long mDetectTime;
        long mCount;

        WakelockEntry(int uid, String name, int type, long dur, long count) {
            mUid = uid;
            mName = name;
            mType = type;
            mDuration = dur;
            mDetectTime = System.currentTimeMillis();
            mCount = count;
        }

        void parsePackages(PackageManager pm) {
            mPackages = pm.getPackagesForUid(mUid);
        }

        String[] getPackages() {
            return mPackages;
        }

        boolean matches(WakelockEntry lock, long buffer) {
            return ((mName == null ? lock.mName == null : mName.equals(lock.mName)) &&
                    mUid == lock.mUid &&
                    mCount == lock.mCount &&
                    mType == lock.mType);
                    // mDetectTime >= lock.mDetectTime - buffer);
        } 
    }

    class InvestigatorThread extends Thread {
        Context mContext;
        Message mCallback;
        PowerManager.WakeLock mRunSlice;
   
        InvestigatorThread(Context ctx, Message msg) {
            mContext = ctx;
            mCallback = msg;
            PowerManager pm = (PowerManager)mContext.getSystemService(
                    Context.POWER_SERVICE);
            mRunSlice = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    TAG);
        }

        @Override
        public void run() {
            /*
            */
            synchronized(sLock) {
                ArrayList<WakelockEntry> heldLocks = new ArrayList<WakelockEntry>();
                SparseArray<? extends Uid> uidStats = null;
                long statTime = mSavedStatTime;
                long seq = 0;
                long batteryRealtime = mSavedBatteryRealtime;
                long upTime = mSavedUpTime;
                try {
                    mRunSlice.acquire();
                    IBatteryStats iface = IBatteryStats.Stub.asInterface(
                            ServiceManager.getService("batteryinfo"));

                    byte[] data;
                    int statsType = BatteryStats.STATS_CURRENT;
                    data = iface.getStatistics();
                    Parcel dataBundle = Parcel.obtain();
                    dataBundle.unmarshall(data, 0, data.length);
                    dataBundle.setDataPosition(0);
                    BatteryStatsImpl battStats = BatteryStatsImpl.CREATOR.createFromParcel(dataBundle);
    
                    statTime = SystemClock.elapsedRealtime();
                    long rawRealtime = statTime * 1000;
                    batteryRealtime = battStats.getBatteryRealtime(rawRealtime);
    
                    upTime = SystemClock.uptimeMillis();
                    seq = mSeqNumber;
                    uidStats = battStats.getUidStats();
                    if(mSavedUidStats == null || seq != mSavedSeqNumber) {
                        return;
                    }
                    long diffRealtime = statTime - mSavedStatTime;
                    long diffUptime = upTime - mSavedUpTime;
    
                    if(DUMP) Utils.Log.v(TAG, "Investigator: rt,ut:" + diffRealtime + "," + diffUptime);
                    if(diffUptime < mTimeout) {
                        return;
                    }

                    final int NU = uidStats.size();
                    for (int iu = 0; iu < NU; iu++) {
                        Uid u = uidStats.valueAt(iu);
                        if(u.getUid() < Process.FIRST_APPLICATION_UID && !mMonitorSystem) continue;
                        Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats =
                                u.getWakelockStats();
                        if (wakelockStats.size() > 0) {
                            for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> ent
                                    : wakelockStats.entrySet()) {
                                Uid.Wakelock wl = ent.getValue();
                                checkLock(u.getUid(), wl, ent.getKey(), BatteryStats.WAKE_TYPE_PARTIAL,
                                        statsType, batteryRealtime, diffUptime, diffRealtime,
                                        heldLocks);
                                checkLock(u.getUid(), wl, ent.getKey(), BatteryStats.WAKE_TYPE_FULL,
                                        statsType, batteryRealtime, diffUptime, diffRealtime,
                                        heldLocks);
                            }  
                        }
                    }
                }catch(Exception e) {
                    if(DUMP) Utils.Log.d(TAG, "Investigator: Got exception ", e);
                }finally {
                    mSavedUidStats = uidStats;
                    mSavedStatTime = statTime;
                    mSavedSeqNumber = seq;
                    mSavedUpTime = upTime;
                    mSavedBatteryRealtime = batteryRealtime;
                    mCallback.obj = heldLocks;
                    mCallback.sendToTarget();
                    if(mRunSlice.isHeld()) {
                        mRunSlice.release();
                    }
                }
            }
        }

        private void checkLock(int uid, Uid.Wakelock wl, String key, int type, int statsType,
                long batteryRealtime, long diffUptime, long diffRealtime,
                ArrayList<WakelockEntry> heldLocks) {
            Timer timer = wl.getWakeTime(type);
            boolean valid = false;
            long duration = 0;
            long time, prevTime;
            int count, prevCount, refCount = 0;
            if (timer != null) {
                // Convert from microseconds to milliseconds with rounding
                time = (timer.getTotalTimeLocked(
                        batteryRealtime, statsType) + 500) / 1000;
                count = timer.getCountLocked(statsType);
                refCount = timer.getCountLocked(BatteryStats.STATS_CURRENT);
                if(count != 0) {
                    if(DUMP) Utils.Log.v(TAG, "Investigator: found lock:" +
                            key + ",type:" + type + ",time:" + time + ",count:" + count);
                    // Compare with previous run to confirm
                    Timer prevTimer = getWakelock(mSavedUidStats,
                            uid, key,
                            type);
                    if(prevTimer != null) {
                        prevTime = (prevTimer.getTotalTimeLocked(
                                mSavedBatteryRealtime, statsType) + 500) / 1000;
                        prevCount = prevTimer.getCountLocked(statsType);
                        if(DUMP) Utils.Log.v(TAG, "Investigator: found prev lock:" +
                                key + ",time:" + prevTime + ",count:" + prevCount);
                        long heldTime = time - prevTime;
                        if(heldTime <= diffRealtime && heldTime <= diffUptime) {
                            long heldCount = count - prevCount;
                            if(heldCount < 0) return;
                            double perwl = heldCount > 0
                                    ? (double)heldTime/heldCount
                                    : heldTime;
                            if(perwl > mTimeout) {
                                duration = (long)perwl;
                                valid = true;
                                if(DUMP) Utils.Log.v(TAG, "Investigator: found held lock:" +
                                        key + ",duration:" + duration + ",refCount:" + refCount);
                            }
                        }
                    }else {
                        double perwl = count > 0
                                ? (double)time/count
                                : time;
                        if(time <= diffRealtime && time <= diffUptime && perwl > mTimeout) {
                            duration = (long)perwl;
                            valid = true;
                            if(DUMP) Utils.Log.v(TAG, "Investigator: found held lock:" +
                                    key + ",duration:" + duration + ",refCount:" + refCount);
                        }
                    }
                }
            }
            if(valid) {
                WakelockEntry lock = new WakelockEntry(uid, key,
                        type, duration, refCount);
                heldLocks.add(lock);
            }
        }

        private Timer getWakelock(SparseArray<? extends Uid> stats, int uid,
                String name, int type) {
            Timer timer = null;
            final int NU = stats.size();
            for (int iu = 0; iu < NU; iu++) {
                Uid u = stats.valueAt(iu);
                if(u.getUid() != uid) continue;

                Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
                if (wakelockStats.size() > 0) {
                    for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> ent
                            : wakelockStats.entrySet()) {
                        if(!ent.getKey().equals(name)) continue;

                        Uid.Wakelock wl = ent.getValue();
                        timer = wl.getWakeTime(type);
                        break;
                    }
                }
                break;
            }
            return timer;
        }
        
    }

    interface IMessenger {
        public void onPostMessage(String id);
    }

    class AlarmService extends BroadcastReceiver {

        HashMap<String, PendingIntent> mAlarms = new HashMap<String, PendingIntent>();
        boolean mStarted;
        AlarmManager mAlarmManager;
        Context mContext;
        IMessenger mMessenger;
        
        AlarmService(Context ctx, IMessenger msger) {
            mContext = ctx;
            mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            mMessenger = msger;
        }

        void setAlarm(String id, long whence) {
            cancelAlarm(id);
            if(DUMP) Utils.Log.v(TAG, "setAlarm: " + id + " at:" + whence);
            PendingIntent pI = PendingIntent.getBroadcast(mContext, 0,
                    new Intent(id),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            synchronized(mAlarms) {
                mAlarms.put(id, pI);
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + whence,
                        pI);
            }
        }
 
        void cancelAlarm(String id) {
            if(DUMP) Utils.Log.v(TAG, "cancelAlarm: " + id);
            synchronized(mAlarms) {
                PendingIntent pI = mAlarms.get(id);
                if(pI != null) {
                    mAlarms.remove(id);
                    mAlarmManager.cancel(pI);
                    pI = null;
                }
            }
        }

        void handleAlarmExpiry(String id) {
            synchronized(mAlarms) {
                mAlarms.remove(id);
            }
            mMessenger.onPostMessage(id);
        }

        void start(Iterator<String> actions) {
            if(!mStarted) {
                IntentFilter filter = new IntentFilter();
                while(actions.hasNext()) {
                    String id = actions.next();
                    if(DUMP) Utils.Log.v(TAG, "AlarmService: adding action:" + id);
                    filter.addAction(id);
                }
                mContext.registerReceiver(this,
                        filter);
                mStarted = true;
            }
        }

        void stop() {
            if(mStarted) {
                mContext.unregisterReceiver(this);
                synchronized(mAlarms) {
                    Iterator<String> iterator = mAlarms.keySet().iterator();
                    while(iterator.hasNext()) {
                        PendingIntent pI = mAlarms.get(iterator.next());
                        if(pI != null) mAlarmManager.cancel(pI);
                    }
                    mAlarms.clear();
                }
                mStarted = false;
            }
        }

        @Override
        public void onReceive(Context ctx, Intent intent) {
            handleAlarmExpiry(intent.getAction());
        }
    }

    interface ILockFoundListener {
        public void onLockFound(WakelockEntry lock);
    }

    class Processor extends Thread {

        ArrayList<WakelockEntry> mLocks;
        boolean mUseFlex;
        PackageManager mPackageManager;
        ILockFoundListener mCallback;
        
        Processor(ArrayList<WakelockEntry> lock, boolean useFlex,
                PackageManager pm, ILockFoundListener cb) {
            mLocks = lock;
            mUseFlex = useFlex;
            mPackageManager = pm;
            mCallback = cb;
        }

        @Override
        public void run() {
            for(int i = 0; i < mLocks.size(); ++i) {
                int option = ASK_USER;

                WakelockEntry lock = mLocks.get(i);
                lock.parsePackages(mPackageManager);
                if(mUseFlex) {
                    for(int j = 0; j < mWhiteList.size(); ++j) {
                        WhitelistEntry entry = mWhiteList.get(j);
                        option = entry.getOption(lock.getPackages());
                        if(option != ASK_USER) break;
                    }
                }
                if(option != AUTO_IGNORE) {
                    boolean first = true;
                    synchronized(mStuckWakelockList) {
                        int N = mStuckWakelockList.size();
                        for(int k = 0; k < N; ++k) {
                            WakelockEntry one = mStuckWakelockList.get(k);
                            if(one.matches(lock, (mTimeout + BUFFER) * 2)) {
                                if(DUMP) Utils.Log.d(TAG, "Long wakelock: " +
                                        one.mName + ",prev dur:" + one.mDuration +
                                        ",new dur:" + lock.mDuration);
                                one.mDuration += lock.mDuration;
                                first = false;
                                break;
                            }
                        }
                        if(first) mStuckWakelockList.add(lock);
                    }
                    if(first) {
                        mCallback.onLockFound(lock);
                    }
                }
            }
        }    
    }

    class Uploader extends Thread {

        Context mContext;

        Uploader(Context ctx) {
            mContext = ctx;
        }

        @Override
        public void run() {
            ArrayList<WakelockEntry> locks = new ArrayList<WakelockEntry>();
            synchronized(mStuckWakelockList) {
                for(int i = 0; i < mStuckWakelockList.size(); ++i) {
                    locks.add(mStuckWakelockList.get(i));
                }
                mStuckWakelockList.clear();
            }
            if(locks.size() > 0) {
                String TYPE_PREFIX = "[ID=StuckWakelocks;ver=" + DevStatPrefs.VERSION + ";time=" +
                        (System.currentTimeMillis()/1000) + ";]";
                StringBuilder sb = new StringBuilder(TYPE_PREFIX);
                String PKG_PREFIX = "[ID=SWL;pkg=";
                String TAG_PREFIX = "tag=";
                String TIME_PREFIX = "at=";
                String DUR_PREFIX = "dur=";
                String WLTYPE_PREFIX = "type=";
                String SUFFIX = ";";
                String END_SUFFIX = ";]";
                boolean gotOne = false;

                int len = locks.size();
                for(int j = 0; j < len; ++j) {
                    gotOne = true;
                    WakelockEntry wL = locks.get(j);
                    if(wL.getPackages() == null) {
                        wL.parsePackages(mContext.getPackageManager());
                    }
    
                    sb.append(PKG_PREFIX)
                      .append((wL.mPackages != null && wL.mPackages.length > 0 ?
                              wL.mPackages[0] :
                              "null:" + wL.mUid))
                      .append(SUFFIX)
                      .append(TAG_PREFIX)
                      .append(wL.mName)
                      .append(SUFFIX)
                      .append(TIME_PREFIX)
                      .append(wL.mDetectTime/1000)
                      .append(SUFFIX)
                      .append(DUR_PREFIX)
                      .append(wL.mDuration/1000)
                      .append(SUFFIX)
                      .append(WLTYPE_PREFIX)
                      .append(wL.mType)
                      .append(END_SUFFIX);
                }
                if(gotOne) {
                    Utils.Log.i(DevStatPrefs.CHECKIN_EVENT_ID, sb.toString());
                }
            }
        }
    }
}

