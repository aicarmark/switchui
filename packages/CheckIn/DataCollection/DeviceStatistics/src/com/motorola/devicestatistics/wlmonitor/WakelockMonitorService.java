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

package com.motorola.devicestatistics.wlmonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
// BEGIN MMCP
//import com.motorola.datacollection.DataCollectionListener;
// END MMCP
import com.motorola.devicestatistics.CheckinHelper;
import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.DevStatPrefs;
import com.motorola.devicestatistics.DevStatUtils;
import com.motorola.devicestatistics.DeviceStatisticsSvc;
import com.motorola.devicestatistics.DeviceStatsConstants;
import com.motorola.devicestatistics.SettingsStat;
import com.motorola.devicestatistics.StatsController;
import com.motorola.devicestatistics.SysClassNetUtils;
import com.motorola.devicestatistics.TelephonyListener;
import com.motorola.devicestatistics.TimeTracker;
import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.Watchdog;
import com.motorola.devicestatistics.eventlogs.AppState;
import com.motorola.devicestatistics.eventlogs.BatteryLogger;
import com.motorola.devicestatistics.eventlogs.EventLoggerService;
import com.motorola.devicestatistics.eventlogs.EventNote;
import com.motorola.devicestatistics.util.EventWork;
import com.motorola.devicestatistics.util.Scheduler;
import com.motorola.devicestatistics.util.Scheduler.Options;


public class WakelockMonitorService extends Service implements
        WakelockMonitorConstants, DeviceStatsConstants {
    private static final String LAST_REPORT_TIME_PREF = "LastStuckWakelockReportTime";
    // Handler - Message Ids
    private static final int CHECK_STATUS = 3;
    private static final int DISABLE = 4;
    private static final int ENABLE = 5;
    private static final int USB_PLUGGED_IN = 6;
    private static final int USB_PLUGGED_OUT = 7;
    private static final int CHECKIN = 12;
    private static final int MONITOR_RESULT = 13;
    private static final int WHITELIST_INIT_DONE = 14;
    private static final int FORCE_START_SERVICE = 16;

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
    private final static boolean DUMP = GLOBAL_DUMP;

    // Safety limit on number of wakelocks to read from /proc/wakelocks
    // There are 77 lines, at powerup time on solana
    private static final int MAX_PROC_WAKELOCK_LINES = 400;
    private static final long MS_IN_SEC = DeviceStatsConstants.MS_IN_SEC;

    // Wakelock check intervals in milliseconds
    private final static long[] sTimeoutValues = {30000,
                                                  90000,
                                                  1800000,
                                                  2700000,
                                                  3600000,
                                                  7200000,
                                                  0};
    private static long mCheckStatusBufferMs = 15L * DeviceStatsConstants.MS_IN_MINUTE; // 15 mins

    /**
     * Tolerance for checking whether the time spent holding the wakelock is equal or lesser than
     * the elapsed realtime
     */
    static final long TOLERANCE_MS = 1 * MS_IN_SEC; // 1 second

    // Event Log
    private final static int SCHED_OPTIONS = Options.ALLOW_INLINE_EXEC |
            Options.ALLOW_THREAD_SYNC_EXEC;

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
    private long mCheckStatusTimeoutMs;
    private long mCheckinTimeoutMs;

    private EventReceiver mReceiver;
    private AlarmService mAlarmService;

    private static final String WHITELIST_STORE_FS_PATH = "whitelistconf.xml";

    private ArrayList<WhitelistEntry> mWhiteList
            = new ArrayList<WhitelistEntry>();

    private ArrayList<WakelockEntry> mStuckWakelockList
            = new ArrayList<WakelockEntry>();

    /**
     * List of kernel wakelocks to be reported to checkin
     */
    private HashMap<String,ArrayList<WakelockEntry>> mStuckKernelWakelockList
    = new HashMap<String,ArrayList<WakelockEntry>>();

    private HashMap<String, ArrayList<WakelockEntry>> mStuckKernelWakelockListDchrgCycle
    = new HashMap<String,ArrayList<WakelockEntry>>();

    private boolean mDchrgCycleStatsCheckin;

    private SparseArray<? extends Uid> mSavedUidStats;
    /**
     * Copy of wakelock statistics saved when last read from /proc/wakelocks.
     */
    private HashMap<String, WakelockEntry> mSavedKernelWakelocks =
            new HashMap<String, WakelockEntry>();
    private long mSavedStatTime;
    private long mSavedUpTime;
    private long mSavedBatteryRealtime;
    private long mSeqNumber;
    private long mSavedSeqNumber;
    private static Object sLock = new Object();

    // Event Log classes
    private Scheduler mScheduler;
    private SettingsStat mStatData;

    private static final int CHECK_FOR_START_DELAY_MS = 5000; // 5 seconds
    private boolean mOnStartCommandCalled;
    private boolean mFrameworkListenerCreated;

    private static final String PROC_WAKELOCK_FILE =  "/proc/wakelocks";
    private static final String WHITESPACE_REGEX =  "\\s+";

    // Index of fields read from /proc/wakelocks
    private static final int PROC_WAKELOCK_NAME_INDEX = 0;
    private static final int PROC_WAKELOCK_COUNT_INDEX = 1;
    private static final int PROC_WAKELOCK_DURATION_INDEX = 5;
    private static final int PROC_WAKELOCK_LAST_CHANGE_INDEX = 8;

    private static final int PROC_WAKELOCK_MIN_NAME_SIZE = 2;
    // Dummy uid for kernel wakelocks
    private static final int KERNEL_WAKELOCK_UID = -1;
    // Dummy wakelocktype for kernel wakelocks
    private static final int KERNEL_WAKELOCK_TYPE = 0;

    @Override
    public void onCreate() {
        new Utils.RunInBackgroundThread() {
            public void run() {
                if (Watchdog.isDisabled()) return;

                // Wait 5 seconds to see if onStartCommand has been called
                mHandler.sendEmptyMessageDelayed( FORCE_START_SERVICE, CHECK_FOR_START_DELAY_MS );
            }
        };
    }

    public int onStartCommand(final Intent startInt, final int flags, final int startld) {
        mOnStartCommandCalled = true;
        new Utils.RunInBackgroundThread() {
            public void run() {
                onStartCommandImpl(startInt,flags,startld);
            }
        };
        return Service.START_STICKY;
    }

    private void onStartCommandImpl(Intent startInt, int flags, int startld) {
        if (Watchdog.isDisabled()) return;

            // BEGIN MMCP
       /* if (mFrameworkListenerCreated == false) {
            mFrameworkListenerCreated = true;

            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            DataCollectionListener listener = new DataCollectionListener() {

                AppState es = AppState.getInstance(getApplicationContext());

                @Override
                public void onOldLogs(String logs) {
                    // onOldLogs is called once when you start listening the events
                    // ie when am.listenEventLog(listener, true); is called.
                    // arg0 will have events separated by "\n" and will have the time stamp
                    // typical format of the arg0 is "time:ts, <event> \n time:ts, <event>"
                    String[] subStrings = logs.split("\n");
                    for(int i=0;i < subStrings.length; i++){
                        es.storeEventsInSp(subStrings[i], false);
                    }
                }

                @Override
                public void onEventLog(String logs) {
                    es.storeEventsInSp(logs,true);
                }
            };
            am.listenEventLog(listener, true); // true means register, false is unregister 
        } */
            // END MMCP

        EventLoggerService.getInstance(this).startThread(); // 2nd start is ignored

        if(startInt == null) {
            Utils.Log.d(TAG, "onStartCommand: start Intent is null, note spurious restart");
            initStart();
            return;
        }

        String action = startInt.getAction();

        Utils.Log.d(TAG, "onStartCommand: received action " + action);
        if(action != null && action.equals(Intents.ACTION_START_MONITOR)) {
            initServices();
            initOptions();
            initReceivers();
            startMonitor();
        }

        if(action != null && action.equals(DeviceStatsConstants.TIME_WARP_MONITOR_ACTION)) {
            handleTimeWarpMonitor();
        }
    }

    private void handleTimeWarpMonitor() {
        if (mAlarmService == null) {
            Log.e(TAG, "handleTimeWarpMonitor: null mAlarmService");
            return;
        }

        // When user moves the time back by a year, RTC timers may not expire for a year.
        // So this intent fires once in 24 hours of elapsed realtime, to recalibrate the
        // midnight timer
        mAlarmService.setAlarm(true, CHECKIN_ACTION, getTimeOfNextCheckin());

        Intent intent = new Intent(DeviceStatsConstants.TIME_WARP_MONITOR_ACTION);
        intent.setClass(this, DeviceStatisticsSvc.class);
        startService(intent);
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
        mScheduler = new Scheduler(SCHED_OPTIONS);
        BatteryLogger.init();
        StatsController.getController(this); // TODO: Is this call really required
    }

    private void initOptions() {
        SharedPreferences pref = getSharedPreferences(
                OPTIONS,
                Context.MODE_PRIVATE);
        int timeoutIndex = pref.getInt(
                KEY_WAKELOCK_TIMEOUT,
                DEF_TIMEOUT_INDEX);

        mCheckStatusTimeoutMs = timeoutIndex >= sTimeoutValues.length || timeoutIndex < 0 ?
                sTimeoutValues[DEF_TIMEOUT_INDEX] :
                sTimeoutValues[timeoutIndex];

        // When mTimeout is the default of 45 mins, this will be 15 mins
        mCheckStatusBufferMs = mCheckStatusTimeoutMs / 3;

        mMonitorSystem = pref.getBoolean(
                KEY_SYSTEM_MONITOR,
                DEF_SYSTEM_MONITOR);

        mCheckinTimeoutMs = pref.getLong(
                KEY_WAKELOCK_CHECKIN_INTERVAL,
                DEF_CHECKIN_INTERVAL_MS);

        Utils.Log.d(TAG, "initOptions: timeout is :" + mCheckStatusTimeoutMs +
                ", system monitor:" + mMonitorSystem);
    }

    private void initReceivers() {
        mReceiver = new EventReceiver(this);
        mStatData = new SettingsStat(this, mHandler);
        TelephonyListener.startTelephonyListeners(this);
    }

    private void deinitServices() {
        mAlarmService.stop();
        mScheduler.stop();
        TelephonyListener.stopTelephonyListeners(this);
    }

    private void deinitReceivers() {
        mReceiver.stop();
    }

    private void startMonitor() {
        if(mCheckStatusTimeoutMs == 0) {
            // We are disabled, suicide
            stopSelf();
            return;
        }
        mMonitorEnabled = true;
        mReceiver.start();
        mAlarmService.start(mAlarmToMessageMap.keySet().iterator());

        mAlarmService.setAlarm(false, CHECK_STATUS_ACTION,
                SystemClock.elapsedRealtime() + mCheckStatusTimeoutMs);
        mAlarmService.setAlarm(true, CHECKIN_ACTION, getTimeOfNextCheckin());
        mFlexInitDone = false;
        mWhiteList.clear();
        initWhitelistFromFlex();
        mStatData.start();

        startTimeWarpMonitor();
        reportStuckWakelocksOnStartup();
    }

    private final void saveWakelockReportTime() {
        SharedPreferences pref = getSharedPreferences( OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putLong(LAST_REPORT_TIME_PREF, System.currentTimeMillis());
        Utils.saveSharedPreferences(edit);
    }

    private final void reportStuckWakelocksOnStartup() {
        SharedPreferences pref = getSharedPreferences( OPTIONS, Context.MODE_PRIVATE);
        long lastReportTime = pref.getLong(LAST_REPORT_TIME_PREF, 0);
        if (lastReportTime == 0) {
            saveWakelockReportTime();
        } else if ( DevStatUtils.getMillisecsAtMidnight(lastReportTime, MS_IN_DAY) !=
                DevStatUtils.getMillisecsAtMidnight(MS_IN_DAY) ) {
            checkinStuckWakelocks();
        }
    }

    private final void startTimeWarpMonitor() {
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(DeviceStatsConstants.TIME_WARP_MONITOR_ACTION);
        intent.setClass(this, WakelockMonitorService.class);
        PendingIntent pInt = PendingIntent.getService(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pInt);

        long MS_IN_DAY = DeviceStatsConstants.MS_IN_DAY;
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + MS_IN_DAY, MS_IN_DAY, pInt);
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

                createInvestigatorThread();
                restartCheckStatusAlarm();

                monitorCheckStatusHook();
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
                mDchrgCycleStatsCheckin = true;

                createInvestigatorThread();

                // The accumulated stats doesnt change when phone is on charger.
                // So stop the alarm when the phone battery is being charged.
                mAlarmService.cancelAlarm(CHECK_STATUS_ACTION);
                break;
            case USB_PLUGGED_OUT:
                Utils.Log.d(TAG, "Handler: Usb plugged out");
                mPlugged = false;
                mSeqNumber++;

                createInvestigatorThread();
                restartCheckStatusAlarm();
                break;
            case CHECKIN:
                Utils.Log.d(TAG, "Handler: Starting checkin");
                checkinStuckWakelocks();
                mAlarmService.setAlarm(true, CHECKIN_ACTION, getTimeOfNextCheckin());
                break;
            case WHITELIST_INIT_DONE:
                Utils.Log.d(TAG, "Handler: whitelist init done, read:" + msg.arg1);
                mFlexInitDone = true;
                break;
            case MONITOR_RESULT:
                Utils.Log.d(TAG, "Handler: received investigator result, plugged:" +
                        mPlugged+", mDchrgCycleStatsCheckin = "+mDchrgCycleStatsCheckin);
                if(!mPlugged || mDchrgCycleStatsCheckin) {
                    processHeldLocks((Object[])msg.obj, mFlexInitDone);
                }
                break;
            case FORCE_START_SERVICE:
                if ( mOnStartCommandCalled == false ) {
                    // Forcibly start the wakelock monitor service
                    Utils.Log.d(TAG, "Forcibly starting wakelockmonitor service");
                    startService(
                            new Intent(WakelockMonitorService.this, WakelockMonitorService.class).
                            setAction(Intents.ACTION_START_MONITOR));
                }
                break;
            default:
                Utils.Log.d(TAG, "Handler: Unknown message");
                break;
            }
        }

        /**
         * Recreate the alarm that checks and accumulates wakelock usage
         */
        private void restartCheckStatusAlarm() {
            mAlarmService.setAlarm(false, CHECK_STATUS_ACTION, SystemClock.elapsedRealtime() +
                    mCheckStatusTimeoutMs + mCheckStatusBufferMs);
        }
    };

    /**
     * Create a thread to accumulate wakelock usage
     */
    void createInvestigatorThread() {
        if(mMonitorEnabled) {
            InvestigatorThread monitor = new InvestigatorThread(
                    WakelockMonitorService.this,
                    mHandler.obtainMessage(MONITOR_RESULT));
            monitor.start();
        }
    }

    private void monitorCheckStatusHook() {
        SysClassNetUtils.updateNetStats(this);
        monitorEventHook(TimeTracker.TRIGGER_INTENT);
    }

    private void monitorEventHook(Intent intent) {
        int sched = Options.THREAD_SYNC_EXEC;
        EventLoad task = null;

        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        if(action.equals(Intent.ACTION_SHUTDOWN)) {
            sched = Options.INLINE_EXEC;
            // This is to avoid one new
            task = mShutdownLoad;
            task.mTime = System.currentTimeMillis();
            task.mContext = this;
        }else {
            task = new EventLoad(this, intent, System.currentTimeMillis());
        }
        try {
            mScheduler.schedule(task, sched);
        }catch(Scheduler.SchedulerException schEx) {
            if(true) Utils.Log.v(TAG, "Failed to schedule:" + intent + " due to:" + schEx);
        }
    }

    private static EventLoad mShutdownLoad =
            new EventLoad(null, new Intent(Intent.ACTION_SHUTDOWN), 0);

    private static final class EventLoad extends EventWork {
        public Context mContext;

        EventLoad(Context context, Intent intent, long time) {
            super(intent, time);
            mContext = context;
        }

        @Override
        public void processWork() {
            EventNote.noteEvent(mContext, mIntent, mTime);
            mContext = null;
        }
    }

    private void processHeldLocks(Object[] locks, boolean useflex) {
        Processor processor = new Processor(locks, useflex,
                getPackageManager(), mTerminator);
        processor.start();
    }

    private void checkinStuckWakelocks() {
        saveWakelockReportTime();
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
        }catch(Exception e) {
            if (DUMP) DevStatUtils.logException(TAG, e);
        }
    }

    /**
     * Get the time of ms to go till the stuck wakelocks need to be checked in.
     * For normal builds, this will return the number of milliseconds left till midnight
     * @return Number of ms after which stuck wakelocks should be checked in
     */
    private long getTimeOfNextCheckin() {
        if (mCheckinTimeoutMs != MS_IN_DAY) {
            return System.currentTimeMillis() + mCheckinTimeoutMs;
        }
        return DevStatUtils.getMillisecsAtMidnight(MS_IN_DAY);
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
                    if(fcOption.equals(WHITELIST_ACTION_FORCECLOSE)) {
                        option = AUTO_FORCECLOSE;
                    }else if(fcOption.equals(WHITELIST_ACTION_IGNORE)) {
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
        public void onReceive(final Context ctx, final Intent bcIntent) {
            new Utils.RunInBackgroundThread() {
                public void run() {
                    onReceiveImpl(ctx, bcIntent);
                }
            };
        }

        private void onReceiveImpl(Context ctx, Intent bcIntent) {
            if (Watchdog.isDisabled()) return;

            String bcast = bcIntent.getAction();

            if(bcast != null) {
                monitorEventHook(bcIntent);

                if(bcast.equals(Intent.ACTION_BATTERY_CHANGED)) {
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
                } else if (bcast.equals(Intent.ACTION_SHUTDOWN)) {
                    // Update the accumulated wakelock usage at graceful shutdown
                    Message msg = mHandler.obtainMessage();
                    msg.what = CHECK_STATUS;
                    mHandler.sendMessage(msg);
                } else if(bcast.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                    handleTimeWarpMonitor();
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
                ifilter.addAction(Intent.ACTION_BATTERY_LOW);
                ifilter.addAction(Intent.ACTION_SHUTDOWN);
                ifilter.addAction(Intent.ACTION_SCREEN_ON);
                ifilter.addAction(Intent.ACTION_SCREEN_OFF);
                ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
                ifilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                ifilter.addAction(LocationManager.GPS_ENABLED_CHANGE_ACTION);
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

    static class WhitelistEntry {
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

    static class WakelockEntry implements Cloneable {
        String[] mPackages;
        String mName;
        int mUid;
        int mType;
        long mDuration;
        long mDetectTime;
        long mLastChangeMs; // Time in ms when this kernel wakelock was last modified.
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

        public WakelockEntry clone() {
            WakelockEntry cloneWl = null;
            try {
                cloneWl = (WakelockEntry)super.clone();
            } catch (CloneNotSupportedException e) {
                if (DUMP) Utils.Log.e( TAG, "Clone not supported: " + Log.getStackTraceString(e));
            }
            return cloneWl;
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
            if (pm != null) {
                mRunSlice = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    TAG);
            } else {
                if (DUMP) { Utils.Log.d (TAG, "Cant get partial wake lock as PowerManager is NULL"); }
            }
        }

        @Override
        public void run() {
            /*
            */
            synchronized(sLock) {
                ArrayList<WakelockEntry> heldLocks = new ArrayList<WakelockEntry>();
                ArrayList<WakelockEntry> heldKernelLocks = new ArrayList<WakelockEntry>();
                HashMap<String, WakelockEntry> allKernelWakelocks =
                        new HashMap<String, WakelockEntry>();
                SparseArray<? extends Uid> uidStats = null;
                long statTime = mSavedStatTime;
                long seq = 0;
                long batteryRealtime = mSavedBatteryRealtime;
                long upTime = mSavedUpTime;
                try {
                    if (mRunSlice != null) { mRunSlice.acquire(); }

                    // Read the kernel wakelock data from /proc/wakelocks
                    getKernelWakelocks(allKernelWakelocks);

                    // Read the previously saved top wakelock information from disk
                    TopWakelockMonitor monitor = TopWakelockMonitor.createFromFile(mContext);

                    // Update the wakelock usage for kernel wakelocks
                    monitor.handleKernelLocks(mPlugged, allKernelWakelocks);

                    IBatteryStats iface = IBatteryStats.Stub.asInterface(
                            ServiceManager.getService("batteryinfo"));
                    if (iface == null) {
                        Log.e(TAG, "null batteryinfo");
                        return;
                    }

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

                    // Time at which charger was unplugged is current time minus time for which
                    // phone has been running on battery
                    long unPlugTime = batteryRealtime - battStats.computeRealtime(batteryRealtime,
                            BatteryStats.STATS_SINCE_UNPLUGGED);

                    // Update the daily java wakelock usage on battery
                    monitor.handleJavaLocks(unPlugTime, batteryRealtime, uidStats);

                    // Save the daily java/kernel wakelock usage to disk
                    monitor.writeToFile(mContext);

                    if((mPlugged || mSavedUidStats == null || seq != mSavedSeqNumber) &&
                            !mDchrgCycleStatsCheckin) {
                        return;
                    }
                    long diffRealtime = statTime - mSavedStatTime;
                    long diffUptime = upTime - mSavedUpTime;
    
                    if(DUMP) {
                        Utils.Log.v(TAG, "Investigator: rt,ut:" + diffRealtime + "," +
                                    diffUptime+", mCheckStatusTimeoutMs = "+mCheckStatusTimeoutMs);
                    }
                    if(diffUptime < mCheckStatusTimeoutMs) {
                        return;
                    }

                    if (mSavedUidStats != null) {
                        final int NU = uidStats.size();
                        for (int iu = 0; iu < NU; iu++) {
                            Uid u = uidStats.valueAt(iu);
                            if(u.getUid() < Process.FIRST_APPLICATION_UID && !mMonitorSystem)
                                continue;
                            Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats =
                                    u.getWakelockStats();
                            if (wakelockStats.size() > 0) {
                                for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> ent
                                        : wakelockStats.entrySet()) {
                                    Uid.Wakelock wl = ent.getValue();
                                    checkLock(u.getUid(), wl, ent.getKey(),
                                            BatteryStats.WAKE_TYPE_PARTIAL, statsType,
                                            batteryRealtime, diffUptime, diffRealtime, heldLocks);
                                    checkLock(u.getUid(), wl, ent.getKey(),
                                            BatteryStats.WAKE_TYPE_FULL, statsType, batteryRealtime,
                                            diffUptime, diffRealtime, heldLocks);
                                }
                            }
                        }
                    }

                    findHeldKernelLocks(allKernelWakelocks, mSavedKernelWakelocks, heldKernelLocks,
                            diffUptime, diffRealtime);
                }catch(Exception e) {
                    if(DUMP) Utils.Log.d(TAG, "Investigator: Got exception ", e);
                }finally {
                    mSavedUidStats = uidStats;
                    mSavedKernelWakelocks = allKernelWakelocks;
                    mSavedStatTime = statTime;
                    mSavedSeqNumber = seq;
                    mSavedUpTime = upTime;
                    mSavedBatteryRealtime = batteryRealtime;
                    mCallback.obj = new Object[] { heldLocks, heldKernelLocks };
                    mCallback.sendToTarget();
                    if (mRunSlice != null ) {
                        if(mRunSlice.isHeld()) {
                            mRunSlice.release();
                        }
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
                        if(heldTime <= (diffRealtime + TOLERANCE_MS) && heldTime <= (diffUptime + TOLERANCE_MS)) {
                            long heldCount = count - prevCount;
                            if(heldCount < 0) return;
                            double perwl = heldCount > 0
                                    ? (double)heldTime/heldCount
                                    : heldTime;
                            if(perwl > mCheckStatusTimeoutMs) {
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
                        if(time <= (diffRealtime + TOLERANCE_MS) &&
                                time <= (diffUptime + TOLERANCE_MS) && perwl > mCheckStatusTimeoutMs) {
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

    static class AlarmService extends BroadcastReceiver {

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

        private void setAlarm(boolean rtcClock, String id, long atTime) {
            cancelAlarm(id);
            if(DUMP) Utils.Log.v(TAG, "setAlarm: " + id + " at:" + atTime);

            // If instead of FLAG_UPDATE_CURRENT, FLAG_CANCEL_CURRENT is passed here, stale
            // pending intents get accumulated in AlarmManager after a process restart
            PendingIntent pI = PendingIntent.getBroadcast(mContext, 0,
                    new Intent(id),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mAlarmManager.cancel(pI); // Cancel any stale pending intents in alarm manager

            synchronized(mAlarms) {
                mAlarms.put(id, pI);
                if (mAlarmManager != null) {
                    int clock = rtcClock ? AlarmManager.RTC : AlarmManager.ELAPSED_REALTIME;
                    mAlarmManager.set(clock, atTime, pI);
                }
            }
        }
 
        void cancelAlarm(String id) {
            if(DUMP) Utils.Log.v(TAG, "cancelAlarm: " + id);
            synchronized(mAlarms) {
                PendingIntent pI = mAlarms.get(id);
                if(pI != null) {
                    mAlarms.remove(id);
                    if (mAlarmManager != null) {
                        mAlarmManager.cancel(pI);
                    }
                    pI = null;
                }
            }
        }

        void handleAlarmExpiry(String id) {
            if (Watchdog.isDisabled()) return;

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
                        if((pI != null) && (mAlarmManager != null)) mAlarmManager.cancel(pI);
                    }
                    mAlarms.clear();
                }
                mStarted = false;
            }
        }

        @Override
        public void onReceive(Context ctx, final Intent intent) {
            new Utils.RunInBackgroundThread() {
                public void run() {
                    handleAlarmExpiry(intent.getAction());
                }
            };
        }
    }

    interface ILockFoundListener {
        public void onLockFound(WakelockEntry lock);
    }

    class Processor extends Thread {

        private static final int INDEX_JAVA_LIST = 0;
        private static final int INDEX_KERNEL_LIST = 1;
        /**
         * 2 element array of java and kernel wakelock array lists
         */
        Object[] mLocks;
        boolean mUseFlex;
        PackageManager mPackageManager;
        ILockFoundListener mCallback;
        
        Processor(Object locks[], boolean useFlex,
                PackageManager pm, ILockFoundListener cb) {
            mLocks = locks;
            mUseFlex = useFlex;
            mPackageManager = pm;
            mCallback = cb;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (!mDchrgCycleStatsCheckin) {
                processHeldLocks((ArrayList<WakelockEntry>)mLocks[INDEX_JAVA_LIST]);
                processHeldKernelLocks((ArrayList<WakelockEntry>)mLocks[INDEX_KERNEL_LIST],
                        mStuckKernelWakelockList);
                processHeldKernelLocks((ArrayList<WakelockEntry>)mLocks[INDEX_KERNEL_LIST],
                        mStuckKernelWakelockListDchrgCycle);
            } else {
                processHeldKernelLocks((ArrayList<WakelockEntry>)mLocks[INDEX_KERNEL_LIST],
                        mStuckKernelWakelockListDchrgCycle);
                // Checkin the stuckKernelWakelockList in discharge cycle right now
                mAlarmService.setAlarm(true, CHECKIN_ACTION, 0);
            }
        }

        private void processHeldLocks( ArrayList<WakelockEntry> locks) {
            for(int i = 0; i < locks.size(); ++i) {
                int option = ASK_USER;

                WakelockEntry lock = locks.get(i);
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
                            if(one.matches(lock, (mCheckStatusTimeoutMs + mCheckStatusBufferMs) * 2)) {
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

    /**
     * Merge the held locks detected in previous runs, with that from current run.
     * The mStuckKernelWakelockList object will be updated to reflect the list of
     * wakelocks that should be reported to checkin
     * @param locks
     */
    private final void processHeldKernelLocks( ArrayList<WakelockEntry> locks,
            HashMap<String, ArrayList<WakelockEntry>> stuckKernelWakelockList ) {
        for (WakelockEntry wl : locks) {
            WakelockEntry logWl = wl;
            synchronized(stuckKernelWakelockList) {
                ArrayList<WakelockEntry> stuckList = stuckKernelWakelockList.get(wl.mName);
                boolean added = false;

                if (stuckList == null) {
                    // If name doesn't exist, create empty list and associate with name
                    stuckList = new ArrayList<WakelockEntry>();
                    stuckKernelWakelockList.put(wl.mName,stuckList);
                } else {
                    // For this name, find the data added most recently
                    WakelockEntry lastWl = stuckList.get(stuckList.size()-1);

                    // mLastChageMs is the time reported by the kernel,
                    // when this lock was last modified
                    if (lastWl.mLastChangeMs == wl.mLastChangeMs) {
                        // The same wakelock has been held across multiple runs
                        lastWl.mDuration += wl.mDuration;
                        lastWl.mCount += wl.mCount;
                        logWl = lastWl;
                        added = true;
                    }
                }

                // Add this wakelock to the end of the arraylist for this name, if required
                if ( added == false ) stuckList.add(wl);

                if(DUMP) {
                    Utils.Log.d(TAG, "Long kernel wakelock: " + logWl.mName + ",dur:" +
                            logWl.mDuration );
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
            synchronized (sLock) {
                if (mDchrgCycleStatsCheckin) {
                    reportStuckKernelLocks(mStuckKernelWakelockListDchrgCycle);
                    mDchrgCycleStatsCheckin = false;
                } else {
                    reportStuckWakelocks();
                    reportStuckKernelLocks(mStuckKernelWakelockList);
                    TopWakelockMonitor.createFromFile(mContext).reportTopWakelocksToCheckin(mContext);
                }
            }
            createInvestigatorThread();
        }

        private void reportStuckWakelocks() {
            ArrayList<WakelockEntry> locks = new ArrayList<WakelockEntry>();
            synchronized(mStuckWakelockList) {
                for(int i = 0; i < mStuckWakelockList.size(); ++i) {
                    locks.add(mStuckWakelockList.get(i));
                }
                mStuckWakelockList.clear();
            }
            if(locks.size() > 0) {
                String TAG_PREFIX = "tag";
                String TIME_PREFIX = "at";
                String DUR_PREFIX = "dur";
                String WLTYPE_PREFIX = "type";
                boolean gotOne = false;
                DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID,
                        "StuckWakelocks", DevStatPrefs.VERSION, (System.currentTimeMillis()/1000));

                int len = locks.size();
                for(int j = 0; j < len; ++j) {
                    gotOne = true;
                    WakelockEntry wL = locks.get(j);
                    if(wL.getPackages() == null) {
                        wL.parsePackages(mContext.getPackageManager());
                    }
    
                    CheckinHelper.addNamedSegment(checkinEvent, "SWL",
                            "pkg", ((wL.mPackages != null && wL.mPackages.length > 0 ?
                              wL.mPackages[0] : "null:" + wL.mUid)),
                            TAG_PREFIX, DevStatUtils.getUrlEncodedString(wL.mName),
                            TIME_PREFIX, String.valueOf(wL.mDetectTime/1000),
                            DUR_PREFIX, String.valueOf(wL.mDuration/1000),
                            WLTYPE_PREFIX, String.valueOf(wL.mType));
                }
                if(gotOne) {
                    checkinEvent.publish(mContext.getContentResolver());
                    if (DUMP) {
                        Utils.Log.v(DevStatPrefs.CHECKIN_EVENT_ID,
                                checkinEvent.serializeEvent().toString());
                    }
                }
            }
        }

        /**
         * Send information about stuck wakelocks seen earlier, to checkin.
         * Also clears the list of stuck wakelocks, so that a new list can be reported next time.
         */
        private void reportStuckKernelLocks(HashMap<String,
                ArrayList<WakelockEntry>> stuckKernelWakelockList) {
            DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(
                    DevStatPrefs.CHECKIN_EVENT_ID, "StuckKernelWakelocks", DevStatPrefs.VERSION,
                    System.currentTimeMillis()/MS_IN_SEC );

            synchronized(stuckKernelWakelockList) {
                if(stuckKernelWakelockList.isEmpty()) return;

                for ( ArrayList<WakelockEntry> wlList : stuckKernelWakelockList.values() ) {
                    for ( WakelockEntry wl : wlList ) {
                        CheckinHelper.addNamedSegment(checkinEvent, "SKWL",
                                "tag", DevStatUtils.getUrlEncodedString(wl.mName),
                                "at", String.valueOf(wl.mDetectTime/MS_IN_SEC),
                                "dur", String.valueOf(wl.mDuration/MS_IN_SEC),
                                "count", String.valueOf(wl.mCount),
                                "dc", String.valueOf(mDchrgCycleStatsCheckin));
                    }
                }
                stuckKernelWakelockList.clear();
            }
            checkinEvent.publish(mContext.getContentResolver());
            if(DUMP) Utils.Log.v(DevStatPrefs.CHECKIN_EVENT_ID,
                    checkinEvent.serializeEvent().toString());
        }
    }

    /**
     * Parses /proc/wakelocks to get information about kernel wakelocks
     * @param locks
     */
    private void getKernelWakelocks(HashMap<String, WakelockEntry> locks) {
        // Side Note:
        //  The first attempt was to use BatteryStatsImpl.getKernelWakelockStats() provided by
        //  Android framework, rather than parsing the files ourselves.
        //  But the Android framework implementation has many issues.
        //    a) It uses a buffer of size 4192 to read /proc/wakelocks.
        //        On solana, this file is 4100 bytes at bootup, and soon goes beyond 4192 bytes.
        //        So, sometimes wakelocks will be captured, while at other times they dont get
        //        captured
        //    b) The data maintained in Android framework is updated only when the charger is
        //        connected or disconnected. Within a discharge cycle, it always reports the same
        //        old stale value even if a wakelock has been held for more than 10 hours
        //  So rather than using android framework, we parse the /proc/wakelocks by ourselves

        BufferedReader r = null;
        try {
            Pattern whitespace = Pattern.compile(WHITESPACE_REGEX);
            r = new BufferedReader(new FileReader( PROC_WAKELOCK_FILE ));

            // The first line is skipped because it is a header that contains the format of the
            // lines that follow. Also, if there is an error reading the first line, exit the
            // function.
            if ( r.readLine() == null ) return;

            for (int i=0; i<MAX_PROC_WAKELOCK_LINES; i++) {
                String line = r.readLine();
                if ( line == null ) break;

                // Lines are in the following format
                //  name count expire_count wake_count active_since total_time sleep_time max_time
                //    last_change
                // An example line is
                //  "main"  1 0 0 1974911407424 1974911407424 0 1974911407424 398437502
                String[] fields = whitespace.split(line);
                if ( fields.length > PROC_WAKELOCK_LAST_CHANGE_INDEX ) {
                    String name = fields[PROC_WAKELOCK_NAME_INDEX];
                    long count = Long.valueOf(fields[PROC_WAKELOCK_COUNT_INDEX]);
                    long duration = Long.valueOf(fields[PROC_WAKELOCK_DURATION_INDEX]) /
                        DeviceStatsConstants.NANOSECS_IN_MSEC;
                    long lastChangeMs = Long.valueOf(fields[PROC_WAKELOCK_LAST_CHANGE_INDEX]);

                    // All names are surrounded by double quotes. So its size must be greater than 2
                    if ( name.length() > PROC_WAKELOCK_MIN_NAME_SIZE ) {
                        name = name.substring(1,name.length()-1); // remove double quotes

                        WakelockEntry wl = locks.get(name);
                        if (wl == null) {
                            wl = new WakelockEntry(KERNEL_WAKELOCK_UID, name,
                                    KERNEL_WAKELOCK_TYPE, duration, count );
                            wl.mLastChangeMs = lastChangeMs;
                            locks.put(name,wl);
                        } else {
                            // Some locks have the same name. e.g. "power-supply" wakelock
                            // BatteryStatsImpl sums up their values, so we do the same
                            wl.mCount += count;
                            wl.mDuration += duration;
                            if (lastChangeMs < wl.mLastChangeMs) {
                                wl.mLastChangeMs = lastChangeMs;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e( TAG, Log.getStackTraceString(e));
        } finally {
            if (r!=null) {
                try {
                  r.close();
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }
    }

    /**
     * Compares the current and previous times of kernel wakelocks, to determine those that
     * have been held for times longer than a threshold
     * @param currentLocks Current kernel wakelocks data
     * @param previousLocks Kernel wakelocks data from previous run
     * @param heldLocks Array to be updated with wakelocks being held for times longer than a
     * threshold
     * @param diffUptime uptime elapsed between current and previous times. Used for validation.
     * @param diffRealtime realtime elapsed between current and previous times. Used for
     * validation.
     */
    private final void findHeldKernelLocks( HashMap<String,WakelockEntry> currentLocks,
            HashMap<String,WakelockEntry> previousLocks, ArrayList<WakelockEntry> heldLocks,
            long diffUptime, long diffRealtime) {
        for (WakelockEntry wl : currentLocks.values()) {
            long heldTime = wl.mDuration;

            WakelockEntry oldWl = previousLocks.get(wl.mName);
            if ( oldWl != null ) {
                if(DUMP) Utils.Log.v(TAG, "Investigator: found prev kernel lock:" +
                        oldWl.mName + ",time:" + oldWl.mDuration + ",count:" + oldWl.mCount);
                // if the lock was held earlier, subtract the previous time for which the
                // wakelock was held
                heldTime  -= oldWl.mDuration;
            }
            // The time for which the wakelock was held cannot be higher than the elapsed
            // uptime or realtime
            if(heldTime <= (diffRealtime + TOLERANCE_MS) &&
                    heldTime <= (diffUptime + TOLERANCE_MS)) {
                if ( heldTime >= mCheckStatusTimeoutMs ) {
                    // Modifying wl is not allowed, because it gets saved into mSavedKernelWakelocks
                    // So create a clone of wl, and modify that instead.
                    WakelockEntry heldWl = wl.clone();
                    if (heldWl==null) continue; // for klocworks/find_bugs

                    // Update heldWl to contain the diff wrt previous run
                    heldWl.mDuration = heldTime;
                    if ( oldWl != null ) heldWl.mCount -= oldWl.mCount;

                    if(DUMP) {
                        Utils.Log.v(TAG, "Investigator: found held kernel lock:" + heldWl.mName +
                            ",duration:" + heldWl.mDuration + ",Count:" + heldWl.mCount);
                    }
                    heldLocks.add( heldWl );
                }
            }
        }
    }
}
