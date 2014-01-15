/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: PowerProfileSvc.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 * 01-20-10       A24178       IKMAP-4047: Optimize data disable algo
 *                -Ashok
 * 02-15-10       A24178       IKMAP-6046: Make smart mode default
 *                -Ashok
 * 07-13-10       A16462       IKSTABLETWO-2784: Dynamic Data Mode change
 *                -Selvi
 **********************************************************
 */

package com.motorola.batterymanager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Random;

import com.motorola.batterymanager.Utils;
import com.motorola.batterymanager.R;

public class PowerProfileSvc extends Service implements Handler.Callback {

    public final static String SVC_START_ACTION = 
        "android.intent.action.batteryprofile.SVC_START";

    public final static String ALRM_DATA_OFF_TIMER_ACTION = 
        "android.intent.action.batteryprofile.CHK_ALRM";
    public final static String ALRM_DATA_ON_TIMER_ACTION = 
        "android.intent.action.batteryprofile.DATA_ALRM";
    public final static String ALRM_WAKE_TIMER_ACTION = 
        "android.intent.action.batteryprofile.DATA_CHECK";
    public final static String SCREEN_ON_ACTION = 
        "android.intent.action.batteryprofile.SCR_ON";
    public final static String SCREEN_OFF_ACTION = 
        "android.intent.action.batteryprofile.SCR_OFF";
    public final static String PLUGGED_ACTION = 
        "android.intent.action.batteryprofile.PLUG_ON";
    public final static String UNPLUGGED_ACTION = 
        "android.intent.action.batteryprofile.PLUG_OFF";

    // APN Optimize
    public final static String CELL_DATA_ENABLED_ACTION =
        "android.intent.action.batteryprofile.CELL_DATA_ENABLED";

    // BM Service intents
    public final static String KEEP_DATA_OFF_ACTION =
        "android.intent.action.batteryprofile.KEEP_DATA_OFF";
    public final static String KEEP_DATA_ON_ACTION =
        "android.intent.action.batteryprofile.KEEP_DATA_ON";
    public final static String WITHDRAW_DATA_OFF_ACTION =
        "android.intent.action.batteryprofile.WITHDRAW_DATA_OFF";
    public final static String WITHDRAW_DATA_ON_ACTION =
        "android.intent.action.batteryprofile.WITHDRAW_DATA_ON";
    public final static String ALRM_KEEP_DATA_ON_TIMER_ACTION =
        "android.intent.action.batteryprofile.KEEP_DATA_ON_ALRM";
    public final static String ENABLE_APN_TYPE_ACTION =
        "android.intent.action.batteryprofile.ENABLE_APN_TYPE";

    // Notice display params
    private final static int NOTICE_ID = 1;

    //3 mins delay.
    private final static long NOTICE_DELAY_MS = 6 * 60 * 1000L;

    public final static int INVALID_ACTION = -1;
    public final static int ALRM_DATA_OFF_TIMER_EXPIRED = 0;
    public final static int ALRM_DATA_ON_TIMER_EXPIRED = 1;
    public final static int SCR_ON_STATE_REACHED = 2;
    public final static int SCR_OFF_STATE_REACHED = 3;
    public final static int PLUGGED_STATE_REACHED = 4;
    public final static int UNPLUGGED_STATE_REACHED = 5;
    public final static int ALRM_WAKE_TIMER_EXPIRED = 6;
    public final static int CELL_DATA_ENABLED = 7;
    public final static int KEEP_DATA_OFF = 8;
    public final static int KEEP_DATA_ON = 9;
    public final static int WITHDRAW_DATA_OFF = 10;
    public final static int WITHDRAW_DATA_ON = 11;
    public final static int KEEP_DATA_ON_ALRM = 12;
    public final static int ENABLE_APN_TYPE = 13;

    private final static int SVC_MSG_WIFI_ENABLE_ID = 0;

    private final static String LOG_TAG = "PowerProfileSvc";

    private boolean mInitDone = false;

    private Thread mWorkerThread;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int Id) {
        super.onStartCommand(startIntent, flags, Id);

        if(startIntent == null) {
            Utils.Log.d(LOG_TAG, "StartCommand:Received null start intent with -->" + flags);
            handleStart();
            handleUpdates();
            if(!handleStopCondition()) {
                handlepostStart();
            }
            Utils.Log.flush(getContentResolver());
            Utils.Log.setLogFlush(true);
            return Service.START_STICKY;
        }

        String action = startIntent.getAction();

        Utils.Log.setLogFlush(false);

        if(action.equals(SVC_START_ACTION)) {
            Utils.Log.d(LOG_TAG, "Start:Start action received.init state is : "
                    + mInitDone);

            Utils.Log.d(LOG_TAG, "Time: " + Utils.getCurrentTime());

            handleStart();
            handleUpdates();
            if(!handleStopCondition()) {
                handlepostStart();
            }

            Utils.Log.flush(getContentResolver());
            Utils.Log.setLogFlush(true);
            return Service.START_STICKY;
        }else if ((!mInitDone) && (isDataSettingAction(action))) {
            // Init has not been done. As trigger from BM, need to consider as startup actions
            Utils.Log.d(LOG_TAG, "Start: action for BM Service, init state is :" + mInitDone);
            handleStart();
            handleUpdates();

            if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                // Do the needful and Stop Service
                Utils.Log.d(LOG_TAG, "Start: Prefs Mode");
                msvcAlgo.run(startIntent);
                handleStopCondition();
                Utils.Log.flush(getContentResolver());
                Utils.Log.setLogFlush(true);
                return Service.START_STICKY;
            }

            handlepostStart();
            // fall through for other modes
        }else{ // 2011.11.29 jrw647 added to fix cr 4727
            handleStart();
            handleUpdates();
            handlepostStart();
        }

        PowerManager.WakeLock runWL = mpowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);

        runWL.acquire();

        if(mInitDone) {
            //Utils.Log.d(LOG_TAG, this.getString(R.string.powerprofile_app_name));
            Utils.Log.d(LOG_TAG, "Start: Received action: " + action);

            if(action.equals(ALRM_WAKE_TIMER_ACTION)) {
                msvcWaker.updateAlarm();
            }	
            msvcAlgo.run(startIntent);
        }else {
            Utils.Log.d(LOG_TAG, "Start: Received action when not INIT'ed : " 
                    + action);
        }
        Utils.Log.flush(getContentResolver());
        Utils.Log.setLogFlush(true);

        runWL.release();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Utils.Log.d(LOG_TAG, "Destroy: We are going down...");
        handleStop();
        super.onDestroy();
    }

    public void handleStart() {
        if(!mInitDone) {
            msvcAlarms = new Alarms(this);
            msvcHandler = new Handler(this);
            msvcProfiles = new ProfileManager(this);
            // Instanciate DataAlgorithm after ProfileManager always,
            // as DataAlgorithm needs instance of ProfileManager
            msvcAlgo = new DataAlgorithm(this);
            //svcDispControl = new DisplayControl(this);

            mInitDone = true;
        }
    }

    public void handlepostStart() {
        if(msvcReceivers == null) {
            msvcReceivers = new Receiver(this);
            msvcWaker = new WakeManager();
        }
        msvcWaker.updateAlarm();
        
        int newState = msvcProfiles.getProfileType();
        if(newState == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
            // Show notification to user if first time boot 
            if(!msvcProfiles.isUserNotified()) {
                msvcProfiles.userNotified(this);
                msvcHandler.postDelayed(mNotifierTask, NOTICE_DELAY_MS);
            } 
        }
        
        mpowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // In case of smart preset, we are needed not to control display
        if(newState == BatteryProfile.OPTION_PRESET_NTSAVER_MODE &&
                msvcProfiles.isPresetMode()) {
            DisplayControl.noteModeChange(this.getApplicationContext(),
                    BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE);
        }else {
            DisplayControl.noteModeChange(this.getApplicationContext(),
                    newState);
        }
    }

    public void handleUpdates() {
        msvcProfiles.update(this);
    }

    public boolean handleStopCondition() {
        if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
            Utils.Log.d(LOG_TAG, "handleStopCondition: We have moved to MAX_PERF_MODE..time to go down");
            DisplayControl.noteModeChange(this.getApplicationContext(),
                    BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE);
            handleStop();
            stopSelf();
            return true;
        }
        return false;
    }

    public void handleStop() {
        Utils.Log.d(LOG_TAG, "handleStop: init state is " + mInitDone);

        if(mInitDone) {
            if(msvcReceivers != null) {
                msvcReceivers.stop();
                msvcWaker.stop();
            }
            if(msvcHandler != null) {
                msvcHandler.removeCallbacks(mNotifierTask);
            }
            msvcAlgo.stop();
            msvcAlarms.stop();
            msvcAlarms = null;
            msvcAlgo = null;
            msvcProfiles = null;
            msvcReceivers = null;
            msvcWaker = null;

            mInitDone = false;
        }
    }

    private Alarms msvcAlarms;
    private DataAlgorithm msvcAlgo;
    private ProfileManager msvcProfiles;
    private Receiver msvcReceivers;
    private WakeManager msvcWaker;
    private PowerManager mpowerManager;
    private Handler msvcHandler;
    private PowerManager.WakeLock mwifiLock = null;

    private Runnable mNotifierTask = new Runnable() {
        public void run() {
            notifyUserOfProfile();
        }
    };

    private void notifyUserOfProfile() {

        Notification note = new Notification(R.drawable.stat_notify_battery_manager,
                this.getString(R.string.pwrup_notice_title),
                System.currentTimeMillis());

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent launchIntent = new Intent(this, PowerProfileNotifyUi.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, launchIntent, 
                PendingIntent.FLAG_ONE_SHOT);
        note.setLatestEventInfo(getApplicationContext(),
                this.getString(R.string.pwrup_notice_title),
                this.getString(R.string.pwrup_notice_subtext),
                pIntent);
        note.flags = note.flags | Notification.FLAG_AUTO_CANCEL;
        nm.notify(NOTICE_ID, note);
    } 

    private boolean isDataSettingAction(String action) {
        return ((action.equals(WITHDRAW_DATA_OFF_ACTION))
                || (action.equals(KEEP_DATA_OFF_ACTION))
                || (action.equals(KEEP_DATA_ON_ACTION))
                || (action.equals(WITHDRAW_DATA_ON_ACTION))
                || (action.equals(ALRM_KEEP_DATA_ON_TIMER_ACTION))
                || (action.equals(ENABLE_APN_TYPE_ACTION))
                );
    }

    private class Alarms extends BroadcastReceiver{
        public final static int MAX_ALARMS = 2;

        private void addToStore(String action, PendingIntent intent) {
            mAlarms.put(action, intent);
        }

        private PendingIntent deleteFromStore(String key) {
            PendingIntent pInt = null;

            pInt = mAlarms.get(key);
            mAlarms.remove(key);
            return pInt;
        }

        public void expired(String action) {
            Utils.Log.d(LOG_TAG, "Alarms:Expired: --> " + action);
            mAlarms.remove(action);
        }

        public void cancelAlarm(String action) {
            Utils.Log.d(LOG_TAG, "Alarms:Cancel: --> " + action);
            PendingIntent pInt = deleteFromStore(action);
            if(pInt != null) {
                malarmManager.cancel(pInt);
            }
        }

        public void stop() {
            mContext.unregisterReceiver(this);

            Set<String> keys = mAlarms.keySet();
            Iterator<String> entry = keys.iterator();

            while(entry.hasNext()) {
                cancelAlarm(entry.next());
            }
        }

        private HashMap<String, PendingIntent> mAlarms = new HashMap<String, PendingIntent>(MAX_ALARMS);
        private AlarmManager malarmManager;
        private Context mContext;

        public Alarms(Context ctx) {
            malarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            mContext = ctx;

            IntentFilter filter = new IntentFilter();
            filter.addAction(ALRM_WAKE_TIMER_ACTION);
            filter.addAction(ALRM_DATA_ON_TIMER_ACTION);
            filter.addAction(ALRM_DATA_OFF_TIMER_ACTION);

            mContext.registerReceiver(this, filter);
        }

        public void setAlarm(final long atTime, String action) {
            Intent bcIntent = new Intent(action);
            final PendingIntent pInt = PendingIntent.getBroadcast(mContext, 0, 
                    bcIntent, 
                    PendingIntent.FLAG_CANCEL_CURRENT);

            long rt_msecs = atTime - System.currentTimeMillis();

            Utils.Log.d(LOG_TAG, "Alarms:Set: --> " + action + ", " 
                    + rt_msecs + "ms from now");
            addToStore(action, pInt);
            //malarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + rt_msecs, pInt);
            //malarmManager.set(AlarmManager.RTC_WAKEUP, atTime, pInt);
	   mWorkerThread = new Thread(new Runnable() {
                public void run() {
                 Utils.Log.d(LOG_TAG, "AlarmManager.RTC");
                  malarmManager.set(AlarmManager.RTC, atTime, pInt);
            }
        });
         mWorkerThread.setName("set alarm service");
           mWorkerThread.start();
        }

        public void setRepeatingAlarm(long atTime, long interval, String action) {
            Intent bcIntent = new Intent(action);
            PendingIntent pInt = PendingIntent.getBroadcast(mContext, 0, 
                    bcIntent, 
                    PendingIntent.FLAG_CANCEL_CURRENT);

            Utils.Log.d(LOG_TAG, "Alarms:Repeating Set: --> " + action);
            addToStore(action, pInt);
            malarmManager.setRepeating(AlarmManager.RTC, atTime, interval, pInt);
            // - STUB
            //malarmManager.setRepeating(AlarmManager.RTC_WAKEUP, atTime, interval, pInt);
        }

        public void setNonWakeUpAlarm(long atTime, String action) {
            Intent bcIntent = new Intent(action);
            PendingIntent pInt = PendingIntent.getBroadcast(mContext, 0,
                    bcIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            long rt_msecs = atTime - System.currentTimeMillis();

            Utils.Log.d(LOG_TAG, "Alarms:SetNonWakeUp: --> " + action + ", "
                    + rt_msecs + "ms from now");
            addToStore(action, pInt);
            //malarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + rt_msecs, pInt);
            malarmManager.set(AlarmManager.RTC, atTime, pInt);
        }

        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            Intent svcIntent = new Intent(action);

            Utils.Log.d(LOG_TAG, "Alarms:Receiver: action is: " + action);

            expired(action);
            mContext.startService(svcIntent);
        }
    }

    private class DataAlgorithm {
        private final static int STATE_IDLE = 0;
        private final static int STATE_THRESHOLD_WAIT = 1;
        private final static int STATE_SLEEP = 2;
        private final static int STATE_KEEP_DATA_OFF = 3;
        private final static int STATE_KEEP_DATA_ON = 4;

        private int mState;
        private DataToggler mToggler;
        private Environs mEnv;
        private ToleranceTimer mdataTimers;
        private DataStateReceiver mDataListener;
        WifiToggler mwifiControl;
        private Random mRandom;
        private Intent mStartIntent;

        private boolean mDataConnectedBeforeSleep;

        public synchronized void run(Intent startIntent) {
            int next_state = mState;
            mStartIntent = startIntent;
            int arg = getMyAction(startIntent.getAction());
            Utils.Log.d(LOG_TAG, "Algo:Run: current state --> " + mState + "Arg --> " + arg);

            switch(mState) {
            case STATE_IDLE:
                /***
                 * We can move out of this state ONLY if screen goes off and NOT plugged
                 * But, we do need to be aware of the environment, so make note of everything
                 */
                switch(arg) {
                case PLUGGED_STATE_REACHED:
                    mEnv.mPlugged = true;
                    break;
                case SCR_ON_STATE_REACHED:
                    mEnv.mScreenOn = true;
                    break;
                case UNPLUGGED_STATE_REACHED:
                    mEnv.mPlugged = false;
                    if(!mEnv.mScreenOn) {
                        Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Plug Off: starting tolerance timer");
                        if(mdataTimers == null) {
                            mdataTimers = new ToleranceTimer();

                            next_state = mdataTimers.getNextState(STATE_IDLE,
                                                       SCR_OFF_STATE_REACHED);

                            if(next_state == STATE_IDLE) {
                                mdataTimers = null;
                            }
                        }
                    }else {
                        Utils.Log.d(LOG_TAG, "Algo:Run:Idle: plug off but screen ON, nothing to do..");
                    }
                    break;
                case SCR_OFF_STATE_REACHED:
                    mEnv.mScreenOn = false;
                    if(!mEnv.mPlugged) {
                        Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Screen Off: starting tolerance timer");
                        if(mdataTimers == null) {
                            mdataTimers = new ToleranceTimer();

                            next_state = mdataTimers.getNextState(STATE_IDLE, 
                                                       SCR_OFF_STATE_REACHED);

                            if(next_state == STATE_IDLE) {
                                mdataTimers = null;
                            }
                        }
                    }else {
                        Utils.Log.d(LOG_TAG, "Algo:Run:Idle: We are plugged, nothing to do..");
                    }
                    break;
                case ALRM_WAKE_TIMER_EXPIRED:
                    if(!mEnv.mPlugged && !mEnv.mScreenOn) {
                        if(mdataTimers == null) {
                            Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Data check alarm: starting tolerance timer");
                            mdataTimers = new ToleranceTimer();

                            next_state = mdataTimers.getNextState(STATE_IDLE, 
                                    ALRM_WAKE_TIMER_EXPIRED);

                            if(next_state == STATE_IDLE) {
                                mdataTimers = null;
                            }
                        }
                    }else {
                        Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Data check alarm: We are plugged or screen ON, nothing to do..");
                    }
                    break;
                case CELL_DATA_ENABLED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Data enabled, ignore");
                    break;
                case KEEP_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Keep Data OFF, change state");
                    handleKeepDataOff();
                    next_state = STATE_KEEP_DATA_OFF;
                    break;
                case WITHDRAW_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:WithdrawDataOff: Error: "
                                         + " Should not be getting this here");
                    break;
                case KEEP_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Keep Data ON, change state ");
                    msvcProfiles.setRequestedDataMode(BatteryProfile.KEEP_DATA_ON);
                    handleKeepDataOn();
                    next_state = STATE_KEEP_DATA_ON;
                    break;
                case WITHDRAW_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:WithdrawDataOn: Error:"
                                         + " Should not be getting this here");
                    break;
                case KEEP_DATA_ON_ALRM:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:KeepDataONALrm: Error:"
                                         + " Should not be getting this here");
                    break;
                case ENABLE_APN_TYPE:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:EnableApnType: Not honouring in this state... ");
                    break;
                default:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Idle:Illegal event received:"
                                            + arg);
                break;
                }
                break;
            case STATE_THRESHOLD_WAIT:
                /***
                 * We will move out of this state --> SLEEP if we are still inactive after timeout
                 * or --> IDLE if something interrupts us from completing this stage
                 */
                switch(arg) {
                case PLUGGED_STATE_REACHED:
                    mEnv.mPlugged = true;
                    break;
                case UNPLUGGED_STATE_REACHED:
                    mEnv.mPlugged = false;
                    break;
                case SCR_ON_STATE_REACHED:
                    mEnv.mScreenOn = true;
                    mdataTimers.stop();
                    mdataTimers = null;
                    next_state = STATE_IDLE;
                    break;
                case ALRM_DATA_OFF_TIMER_EXPIRED:
                    next_state = mdataTimers.getNextState(STATE_THRESHOLD_WAIT, 
                            ALRM_DATA_OFF_TIMER_EXPIRED);

                    if(next_state == STATE_SLEEP) {
                        mdataTimers = null;
                        mDataConnectedBeforeSleep=mToggler.isDataConnected(); //for manual operation
                        mToggler.disable();

                        // Start listening for any other data connects
                        mDataListener = new DataStateReceiver(PowerProfileSvc.this);

                        if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                            long interval = getRemainingIdleDur();

                            Utils.Log.d(LOG_TAG, "Run:Algo:ThresholdWait:Smart: Time to sleep is : " + interval);

                            if(interval > 0) {
                                msvcAlarms.setAlarm(System.currentTimeMillis() + interval, ALRM_DATA_ON_TIMER_ACTION);
                            }else {
                                Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait: Idle calc failed - will turn data on at screen ON");
                            }

                            if(inPeakInterval()) {
                                if(msvcProfiles.shouldControlPeakWifi()) {
                                    mwifiControl.disable();
                                }
                            }else if(msvcProfiles.shouldControlWifi()) {
                                mwifiControl.disable();
                            }
         
                        }else if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                            mwifiControl.disable();
                        }
                    }else {
                        Utils.Log.d(LOG_TAG, "Alog:Run:ThresholdWait:another timer should have been started");
                    }
                    break;
                case ALRM_WAKE_TIMER_EXPIRED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait:Data check received, ignoring");
                    break;
                case CELL_DATA_ENABLED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait:Data enabled, ignore");
                    break;
                case KEEP_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait Data OFF, change state");
                    handleKeepDataOff();
                    mdataTimers.stop();
                    mdataTimers = null;
                    next_state = STATE_KEEP_DATA_OFF;
                    break;
                case WITHDRAW_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait:WithdrawDataOff: Error: "
                                         + " Should not be getting this here");
                    break;
                case KEEP_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait Data ON, change state");
                    msvcProfiles.setRequestedDataMode(BatteryProfile.KEEP_DATA_ON);
                    handleKeepDataOn();
                    mdataTimers.stop();
                    mdataTimers = null;
                    next_state = STATE_KEEP_DATA_ON;
                    break;
                case WITHDRAW_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait:WithdrawDataOn: Error: "
                                         + " Should not be getting this here");
                    break;
                case KEEP_DATA_ON_ALRM:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait:KeepDataONALrm: Error:"
                                         + " Should not be getting this here");
                    break;
                case ENABLE_APN_TYPE:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait:EnableApnType: Not honouring in this state... ");
                    break;
                default:
                    Utils.Log.d(LOG_TAG, "Algo:Run:ThresholdWait: Illegal event received : " + arg);
                break;
                }
                break;
            case STATE_SLEEP:
                /***
                 * We are currently done with transition to SLEEP mode for data..Either the user
                 * or the alarm will get us out of this mode
                 */
                switch(arg) {
                case PLUGGED_STATE_REACHED:
                    mEnv.mPlugged = true;
                    break;
                case UNPLUGGED_STATE_REACHED:
                    mEnv.mPlugged = false;
                    break;
                case SCR_ON_STATE_REACHED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep: Screen is ON --> time to wakeup");

                    mEnv.mScreenOn = true;
                    msvcAlarms.cancelAlarm(ALRM_DATA_ON_TIMER_ACTION);
                    mDataListener.stop();
                    if(mDataConnectedBeforeSleep){ // don't turn on if the connection was turned off manually
                      mToggler.enable();
                    }
                    delayedWifienable();
                    next_state = STATE_IDLE;
                    break;
                case ALRM_DATA_ON_TIMER_EXPIRED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep: Our alarm has fired!!! time to wakeup");
                    mDataListener.stop();
                    if(mDataConnectedBeforeSleep){ // don't turn on if the connection was turned off manually
                      mToggler.enable();
                    }
                    if(msvcProfiles.shouldControlWifi() && 
                            (msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_NTSAVER_MODE)) {                        
                        /*
                         * We will directly enable wifi in this case as a delay without 
                         * wakelocks held causes a device sleep delay to be added to 
                         * what we add through the handler message post
                         * delayedWifienable();
                         */
                        mwifiControl.enable();
                    }
                    next_state = STATE_IDLE;
                    break;
                case ALRM_WAKE_TIMER_EXPIRED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:Data check received, ignoring");
                    break;
                case CELL_DATA_ENABLED:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:Data enabled, check and disable the new APN");
                    mToggler.disable();
                    break; 
                case KEEP_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:Keep Data OFF, change state");
                    msvcAlarms.cancelAlarm(ALRM_DATA_ON_TIMER_ACTION);
                    mDataListener.stop();
                    mwifiControl.enable();
                    handleKeepDataOff();
                    next_state = STATE_KEEP_DATA_OFF;
                    break;
                case WITHDRAW_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:WithdrawDataOff: Error: "
                                         + " Should not be getting this here");
                    break;
                case KEEP_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:Keep Data ON, change state ");
                    msvcAlarms.cancelAlarm(ALRM_DATA_ON_TIMER_ACTION);
                    mDataListener.stop();
                    mToggler.enable();
                    mwifiControl.enable();
                    msvcProfiles.setRequestedDataMode(BatteryProfile.KEEP_DATA_ON);
                    handleKeepDataOn();
                    next_state = STATE_KEEP_DATA_ON;
                    break;
                case WITHDRAW_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:WithdrawDataOn: Error: "
                                         + " Should not be getting this here");
                    break;
                case KEEP_DATA_ON_ALRM:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:KeepDataONALrm: Error:"
                                         + " Should not be getting this here");
                    break;
                case ENABLE_APN_TYPE:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep:EnableApnType: Not honouring in this state... ");
                    break;
                default:
                    Utils.Log.d(LOG_TAG, "Algo:Run:Sleep: Illegal event received : " + arg);
                break;
                }
                break;
            case STATE_KEEP_DATA_OFF:
                /***
                 * We will move out of this state --> IDLE, only on receiving WITHDRAW_DATA_OFF
                 */
                switch(arg) {
                case PLUGGED_STATE_REACHED:
                    mEnv.mPlugged = true;
                    break;
                case UNPLUGGED_STATE_REACHED:
                    mEnv.mPlugged = false;
                    break;
                case SCR_ON_STATE_REACHED:
                    mEnv.mScreenOn = true;
                    break;
                case SCR_OFF_STATE_REACHED:
                    mEnv.mScreenOn = false;
                    break;
                case KEEP_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:KeepDataOff Already is data off ");
                    handleKeepDataOff();
                    break;
                case WITHDRAW_DATA_OFF:
                    msvcProfiles.setKeepDataOff(msvcProfiles.keepDataOff() -1);
                    boolean persistent = mStartIntent.getBooleanExtra("PERSISTENT", false);
                    if (persistent) {
                        msvcProfiles.setKeepDataOffPersistent(msvcProfiles.keepDataOffPersistent() -1);
                    }
                    if (msvcProfiles.keepDataOff() != 0) {
                        // Still there are clients for data OFF, remain here.
                        Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:WithdrawDataOff:" +
                                "Persistent = " + persistent + ";Active clients present for Data OFF, remain in same state");
                    } else {
                        // Enable Data
                        mToggler.enable();
                        if (isClientForKeepDataOn()) {
                            // still there are some clients for KEEP_DATA_ON
                            msvcProfiles.setRequestedDataMode(BatteryProfile.KEEP_DATA_ON);
                            next_state = STATE_KEEP_DATA_ON;
                            Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:WithdrawDataOff:" +
                                "Persistent = " + persistent + "; Switching Data ON, Active client(s) present for Data_ON");
                        } else {
                            // No clients for Data ON/OFF, move to BM mode
                            msvcProfiles.setRequestedDataMode(BatteryProfile.NO_ACTIVE_MODE);
                            next_state = getMyNextState();
                            Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:WithdrawDataOff:" +
                                "Persistent = " + persistent + "; Switching Data ON, Moving to BM Mode");
                       }
                    }
                    break;
                case KEEP_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:KeepDataON: Do not change state" );
                    handleKeepDataOn();
                    break;
                case WITHDRAW_DATA_ON:
                    boolean pers = mStartIntent.getBooleanExtra("PERSISTENT", false);
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:WithdrawDataOn; Persistent = " + pers);
                    msvcProfiles.setKeepDataOn(msvcProfiles.keepDataOn() -1);
                    if (pers) {
                        msvcProfiles.setKeepDataOnPersistent(msvcProfiles.keepDataOnPersistent() -1);
                    }
                    break;
                case KEEP_DATA_ON_ALRM:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:KeepDataOnALrm ");
                    msvcProfiles.setKeepDataOnWithTimer(false);
                    break;
                case ENABLE_APN_TYPE:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOff:EnableApnType ");
                    mToggler.enableApnType(mStartIntent.getStringExtra("TYPE"));
                    break;
                default:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOff: Illegal event received : " + arg);
                    break;
                }
                break;
            case STATE_KEEP_DATA_ON:
                /***
                 * We will move out of this state --> IDLE, on receiving withdraw/timer expiry
                 * for all the requested clients or KEEP_DATA_OFF request
                 */
                switch(arg) {
                case PLUGGED_STATE_REACHED:
                    mEnv.mPlugged = true;
                    break;
                case UNPLUGGED_STATE_REACHED:
                    mEnv.mPlugged = false;
                    break;
                case SCR_ON_STATE_REACHED:
                    mEnv.mScreenOn = true;
                    break;
                case SCR_OFF_STATE_REACHED:
                    mEnv.mScreenOn = false;
                    break;
                case KEEP_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:KeepDataOff: Put data off immediately");
                    handleKeepDataOff();
                    next_state = STATE_KEEP_DATA_OFF;
                    break;
                case WITHDRAW_DATA_OFF:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:WithdrawDataOff: Error:"
                                        + "Should not be receiving in this state");
                    break;
                case KEEP_DATA_ON:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:KeepDataOn Be in same state");
                    handleKeepDataOn();
                    break;
                case WITHDRAW_DATA_ON:
                    msvcProfiles.setKeepDataOn(msvcProfiles.keepDataOn() -1);
                    boolean persistent = mStartIntent.getBooleanExtra("PERSISTENT", false);
                    if (persistent) {
                        msvcProfiles.setKeepDataOnPersistent(msvcProfiles.keepDataOnPersistent() -1);
                    }
                    if (!isClientForKeepDataOn()) {
                        msvcProfiles.setRequestedDataMode(BatteryProfile.NO_ACTIVE_MODE);
                        next_state = getMyNextState();
                        Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:WithdrawDataON:"
                             + "Persistent =" + persistent + "; No Active clients present, changing state");
                    } else {
                        Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:WithdrawDataON:"
                             + "Persistent =" + persistent + "; Active clients present, remain in same state");
                    }
                    break;
                case KEEP_DATA_ON_ALRM:
                    msvcProfiles.setKeepDataOnWithTimer(false);
                    if (!isClientForKeepDataOn()) {
                        msvcProfiles.setRequestedDataMode(BatteryProfile.NO_ACTIVE_MODE);
                        next_state = getMyNextState();
                        Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:KeepdataonAlrm: No Active clients present, changing state");
                    } else {
                        Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:KeepdataonAlrm: Active clients present, remain in same state");
                    }
                    break;
                case ENABLE_APN_TYPE:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOn:EnableApnType: Not honouring in this state... ");
                    break;
                default:
                    Utils.Log.d(LOG_TAG, "Algo:Run:DataOn: Illegal event received : " + arg);
                    break;
                }
                break;
            }

            // Let display control handle the new events
            runDisplayControlAlgo(arg);

            if(next_state != mState) {
                Utils.Log.d(LOG_TAG, "Time:" + Utils.getCurrentTime() + 
                        "State transition: " + mState + " --> " + next_state);
                Utils.logBattStats();
            }

            mState = next_state;
            Utils.Log.d(LOG_TAG, "Algo:Run: new state is --> " + mState);
        }
        private int getMyAction(String action) {
            int i = INVALID_ACTION;
            if(action.equals(ALRM_DATA_OFF_TIMER_ACTION)) {
                i = ALRM_DATA_OFF_TIMER_EXPIRED;
            }else if(action.equals(ALRM_DATA_ON_TIMER_ACTION)) {
                i = ALRM_DATA_ON_TIMER_EXPIRED;
            }else if(action.equals(ALRM_WAKE_TIMER_ACTION)) {
                i = ALRM_WAKE_TIMER_EXPIRED;
            }else if(action.equals(SCREEN_ON_ACTION)) {
                i = SCR_ON_STATE_REACHED;
            }else if(action.equals(SCREEN_OFF_ACTION)) {
                i = SCR_OFF_STATE_REACHED;
            }else if(action.equals(PLUGGED_ACTION)) {
                i = PLUGGED_STATE_REACHED;
            }else if(action.equals(UNPLUGGED_ACTION)){
                i = UNPLUGGED_STATE_REACHED;
            }else if(action.equals(CELL_DATA_ENABLED_ACTION)) {
                i = CELL_DATA_ENABLED;
            }else if(action.equals(WITHDRAW_DATA_OFF_ACTION)) {
                i = WITHDRAW_DATA_OFF;
            }else if(action.equals(KEEP_DATA_OFF_ACTION)) {
                i = KEEP_DATA_OFF;
            }else if(action.equals(KEEP_DATA_ON_ACTION)) {
                i = KEEP_DATA_ON;
            }else if(action.equals(WITHDRAW_DATA_ON_ACTION)) {
                i = WITHDRAW_DATA_ON;
            }else if(action.equals(ALRM_KEEP_DATA_ON_TIMER_ACTION)) {
                i = KEEP_DATA_ON_ALRM;
            }else if(action.equals(ENABLE_APN_TYPE_ACTION)) {
                i = ENABLE_APN_TYPE;
            }
            return i;
        }

        private int getMyNextState() {
            int next_state = STATE_IDLE;
            if(!mEnv.mPlugged  && !mEnv.mScreenOn){
                Utils.Log.d(LOG_TAG, "getMyNextState: starting tolerance timer");
                if(mdataTimers == null) {
                    mdataTimers = new ToleranceTimer();
                    next_state = mdataTimers.getNextState(STATE_IDLE,SCR_OFF_STATE_REACHED);
                    if(next_state == STATE_IDLE) {
                        mdataTimers = null;
                    }
                }
            }
            return next_state;
        }

        private int getInitState() {
            int state = STATE_IDLE;
            int mode = msvcProfiles.requestedDataMode();

            if (mode == BatteryProfile.KEEP_DATA_OFF)  state = STATE_KEEP_DATA_OFF;
            else if (mode == BatteryProfile.KEEP_DATA_ON) state = STATE_KEEP_DATA_ON;

            Utils.Log.d(LOG_TAG, "Initial State : " + state + "; Mode : " + mode);
            return state;
        }

        private void handleKeepDataOff() {
            if (mState != STATE_KEEP_DATA_OFF) {
                mToggler.disable();
                msvcProfiles.setRequestedDataMode(BatteryProfile.KEEP_DATA_OFF);
            }
            msvcProfiles.setKeepDataOff(msvcProfiles.keepDataOff() + 1);
            boolean persistent = mStartIntent.getBooleanExtra("PERSISTENT", false);
            Utils.Log.d(LOG_TAG, "handleKeepDataOff: Persistent:" + persistent);
            if (persistent) {
                msvcProfiles.setKeepDataOffPersistent(msvcProfiles.keepDataOffPersistent() +1);
            }
        }

        private void handleKeepDataOn() {
            boolean persistent = mStartIntent.getBooleanExtra("PERSISTENT", false);
            if (persistent) {
                msvcProfiles.setKeepDataOnPersistent(msvcProfiles.keepDataOnPersistent() +1);
            }

            long dur = mStartIntent.getLongExtra("DURATION", 0L) * 1000L;
            Utils.Log.d(LOG_TAG, "handleKeepDataOn: Requested Time:" + dur + "Persistent:" + persistent);
            if (dur != 0L) {
                // In Preformance mode, alarm gets canceled on service quit, if gone via
                // local alarm instance; Hence go via Receiver.
                if (msvcProfiles.keepDataOnWithTimer()) {
                    // Timer is On Already
                    long currTime = System.currentTimeMillis();
                    long pendingTime = msvcProfiles.keepDataOnTimerEnd() - currTime;
                    if (pendingTime >= dur) {
                       dur = 0L;
                       Utils.Log.d(LOG_TAG, "handleKeepDataOn:Running Timer holds good, ignore");
                    }
                }
                if (dur != 0L) {
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent bcIntent = new Intent(ALRM_KEEP_DATA_ON_TIMER_ACTION);
                    PendingIntent pInt = PendingIntent.getBroadcast(getBaseContext(), 0,
                            bcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    long currTime = System.currentTimeMillis();
                    alarmManager.set(AlarmManager.RTC_WAKEUP, currTime + dur, pInt);
                    msvcProfiles.setKeepDataOnTimerStart(currTime);
                    msvcProfiles.setKeepDataOnTimerEnd(currTime + dur);
                    msvcProfiles.setKeepDataOnWithTimer(true);
                    Utils.Log.d(LOG_TAG, "handleKeepDataOn:Timer Started:Start:" + currTime + "End:" + currTime+dur);
                }
            } else {
                msvcProfiles.setKeepDataOn(msvcProfiles.keepDataOn() + 1);
            }
        }

        private boolean isClientForKeepDataOn() {
            // Check if there are any timer or non-timer clients active
            return ((msvcProfiles.keepDataOn() == 0)  &&
                (!msvcProfiles.keepDataOnWithTimer()))?false:true;
        }

        private void delayedWifienable() {
            mwifiLock = mpowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
            mwifiLock.acquire();
            Message msg = msvcHandler.obtainMessage(SVC_MSG_WIFI_ENABLE_ID);
            msvcHandler.sendMessageDelayed(msg, 10000);
        }

        private final static int MIN_SLEEP_TIME = 30;
        private final static int MIN_WAKE_TIME = 15;
        private final static int RANDOM_TIME = 60; // 1 hour

        private boolean inPeakInterval() {
            Time myTime = new Time();
            myTime.set(System.currentTimeMillis());

            int currHour = myTime.hour;
            int currMin = myTime.minute;
            int todayMins = (currHour * 60) + currMin;

            ProfileManager.Profile userProf = msvcProfiles.getProfile();
            int startMins = userProf.startMins;
            int endMins = userProf.endMins;

            Utils.Log.d(LOG_TAG, "Algo:PeakCheck: Current: " + todayMins + 
                    ", startMins: " + startMins + ", endMins: " + endMins);

            boolean inRange;

            if(endMins > startMins) {
                inRange = ((todayMins >= startMins) && (todayMins <= endMins));
            }else {
                inRange = !((todayMins >= endMins) && (todayMins <= startMins));
            }

            if(inRange) {
                int timeLeft;
                if(todayMins <= endMins) {
                    timeLeft = endMins - todayMins;
                }else {
                    timeLeft = (24 * 60) - (todayMins - endMins);
                }
                inRange = (timeLeft > MIN_SLEEP_TIME);
            }
            return (!inRange);
        }

        private long getRemainingIdleDur() {
            Time myTime = new Time();
            myTime.set(System.currentTimeMillis());

            int currHour = myTime.hour;
            int currMin = myTime.minute;
            int todayMins = (currHour * 60) + currMin;

            ProfileManager.Profile userProf = msvcProfiles.getProfile();
            int startMins = userProf.startMins;
            int endMins = userProf.endMins;

            Utils.Log.d(LOG_TAG, "Algo:IdleDur: Current: " + todayMins + 
                    ", startMins: " + startMins + ", endMins: " + endMins);

            int timeLeft;
            if(todayMins <= endMins) {
                timeLeft = endMins - todayMins - MIN_WAKE_TIME;
            }else {
                timeLeft = (24 * 60) - (todayMins - endMins) - MIN_WAKE_TIME;
            }

            // Two cases here - whether we apply the randomization or not
            if(timeLeft > RANDOM_TIME) {
                int offset = mRandom.nextInt(RANDOM_TIME - 5);
                timeLeft = timeLeft - offset;
                Utils.Log.v(LOG_TAG, "Algo:IdleDur: random offset: " + offset);
                return (long)(timeLeft * 60L * 1000L);
            }
            return 0;
        }

        public void stop() {
            if (msvcProfiles.requestedDataMode() != BatteryProfile.KEEP_DATA_OFF) {
                mToggler.enable();
            }
            if(msvcHandler.hasMessages(SVC_MSG_WIFI_ENABLE_ID)) {
                msvcHandler.removeMessages(SVC_MSG_WIFI_ENABLE_ID);
                mwifiControl.enable();
                mwifiLock.release();
                mwifiLock = null;
            }
            mwifiControl.stop();
            mwifiControl = null;
            mEnv = null;
            mToggler = null;
        }

        public DataAlgorithm(Context ctx) {
            mState = getInitState();
            mToggler = new DataToggler(ctx);
            mEnv = new Environs();
            mwifiControl = new WifiToggler(ctx);
            mRandom = new Random();
            if (msvcProfiles.requestedDataMode() != BatteryProfile.KEEP_DATA_OFF) {
                mToggler.enable(false);
            }
            mDataConnectedBeforeSleep=true;
        }

        private class Environs {
            public boolean mScreenOn;
            public boolean mPlugged;

            public Environs() {
                mScreenOn = true;
                mPlugged = false;
            }
        }

        private class DataStateReceiver extends BroadcastReceiver {
            private Context mContext;

            public DataStateReceiver(Context ctx) {
                mContext = ctx;
                mContext.registerReceiver(this, new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));
            }

            public void stop() {
                mContext.unregisterReceiver(this);
            }

            @Override
            public void onReceive(Context ctx, Intent bcInt) {
                if(bcInt.getAction().equals(
                        ConnectivityManager.CONNECTIVITY_ACTION)) {
                    NetworkInfo nI = bcInt.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if((nI != null) && (nI.getType() != ConnectivityManager.TYPE_WIFI) && (nI.isConnected())) {
                        // A new network connected, inform the service
                        Intent svcIntent = new Intent(CELL_DATA_ENABLED_ACTION);
                        svcIntent.setClass(mContext,PowerProfileSvc.class);
                        Utils.Log.d(LOG_TAG, "New apn connected: " +
                                nI.getExtraInfo());
                        mContext.startService(svcIntent);
                    }
                }
            }
        }

        private class ToleranceTimer {
            ///***
            private final static long ONE_MIN = 1L * 60L * 1000L;
            private final static long TWO_MIN = 2L * 60L * 1000L;
            private final static long THREE_MIN = 3L * 60L * 1000L;
            private final static long FOUR_MIN = 4L * 60L * 1000L;
            private final static long FIVE_MIN = 5L * 60L * 1000L;
            private final static long TEN_MIN = 10L * 60L * 1000L;
            private final static long FIFTEEN_MIN = 15L * 60L * 1000L;
            private final static long THIRTY_MIN = FIFTEEN_MIN * 2L;
            private final static long FORTY_FIVE_MIN = FIFTEEN_MIN * 3L;
            private final static long SIXTY_MIN = FIFTEEN_MIN * 4L;
            //***/

            private final long SmartAlarms_Low[] = 
                {FIFTEEN_MIN, THREE_MIN, TWO_MIN};
            private final long SmartAlarms_MedLow[] = 
                {THIRTY_MIN, TEN_MIN, THREE_MIN, TWO_MIN};
            private final long SmartAlarms_MedHigh[] = 
                {FORTY_FIVE_MIN, FIFTEEN_MIN, TEN_MIN, THREE_MIN, TWO_MIN};
            private final long SmartAlarms_High[] = 
                {SIXTY_MIN, FIFTEEN_MIN, TEN_MIN, TEN_MIN, FIVE_MIN, 
                 THREE_MIN, TWO_MIN};

            private final long SmartAlarms_OffPeak[] = 
                {TEN_MIN, FIVE_MIN, THREE_MIN, TWO_MIN};
            private final static int sSmartOffpeakIndex = 4;
            
            private final long MaxAlarms[] = 
                {FIFTEEN_MIN, FIVE_MIN, FOUR_MIN, THREE_MIN, TWO_MIN, ONE_MIN};  
            private final static int sMaxAlarmsIndex = 5;

            private final long mAlarmSet[][] =
                                            {SmartAlarms_Low, 
                                            SmartAlarms_MedLow, 
                                            SmartAlarms_MedHigh, 
                                            SmartAlarms_High, 
                                            SmartAlarms_OffPeak,
                                            MaxAlarms};
            private int mcurrAlarmSet = -1;
            private int mcurrIndex = 0;

            long[] getAlarmsSet() {
                if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    if(inPeakInterval()) {
                        mcurrAlarmSet = msvcProfiles.getUserTimerIndex();
                        Utils.Log.d(LOG_TAG, "Algo:Tolerance:getAlarmSet:Smart: In Peak Interval, get user chosen set :" + mcurrAlarmSet);
                    }else {
                        mcurrAlarmSet = msvcProfiles.getUserOffPeakTimerIndex();
                        Utils.Log.d(LOG_TAG, "Algo:Tolerance:getAlarmSet:Smart: In off-peak Interval, get user chosen set" + mcurrAlarmSet);
                    }
                }else if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                    //mcurrAlarmSet = msvcProfiles.getUserTimerIndex();
                    mcurrAlarmSet = sMaxAlarmsIndex;
                    Utils.Log.d(LOG_TAG, "Algo:Tolerance:getAlarmSet:MaxBatt: get user choses set: " + mcurrAlarmSet);
                }
                if(mcurrAlarmSet == BatteryProfile.INVALID_DATA_TIMEOUT) {
                    mcurrAlarmSet = -1;
                    return null;
                }
                return mAlarmSet[mcurrAlarmSet];
            }

            int getNextState(int currState, int event) {
                int next_state = currState;
                long[] currAlarms;

                Utils.Log.d(LOG_TAG, "Tolerance: In state is: " + currState +
                        ",index is: " + mcurrIndex);

                switch(event) {
                case SCR_OFF_STATE_REACHED:
                case ALRM_WAKE_TIMER_EXPIRED:
                    currAlarms = getAlarmsSet();

                    if(currAlarms != null) {
                        msvcAlarms.setAlarm(System.currentTimeMillis() + currAlarms[mcurrIndex], ALRM_DATA_OFF_TIMER_ACTION);
                        mcurrIndex++;
                        next_state = STATE_THRESHOLD_WAIT;
                        msvcReceivers.startDataMonitor();
                    }
                    break;

                case ALRM_DATA_OFF_TIMER_EXPIRED:
                    if(msvcReceivers.getMonitorState()) {
                        Utils.Log.d(LOG_TAG, "Tolerance: No data activity, we are done, timer index is " + (mcurrIndex - 1));

                        msvcReceivers.stopDataMonitor();
                        next_state = STATE_SLEEP;
                    }else {
                        currAlarms = mAlarmSet[mcurrAlarmSet];

                        if(mcurrIndex < currAlarms.length) {
                            Utils.Log.d(LOG_TAG, "Tolerance: setting next alarm, index is: " + mcurrIndex);

                            msvcAlarms.setAlarm(System.currentTimeMillis() + currAlarms[mcurrIndex], ALRM_DATA_OFF_TIMER_ACTION);
                            mcurrIndex++;
                            msvcReceivers.startDataMonitor();
                        }else {
                            Utils.Log.d(LOG_TAG, "Tolerance: data is still on, but no user activity, we are shutting data down");

                            msvcReceivers.stopDataMonitor();
                            //msvcNotifier.setDataDisabledNotification();
                            next_state = STATE_SLEEP;
                        }
                    }
                    break;
                }

                Utils.Log.d(LOG_TAG, "Tolerance:Exit state is: " + next_state);
                return next_state;
            }

            public void stop() {
                mcurrIndex = 0;
                mcurrAlarmSet = -1;
                msvcAlarms.cancelAlarm(ALRM_DATA_OFF_TIMER_ACTION);
                msvcReceivers.stopDataMonitor();
            }
        }

        private void runDisplayControlAlgo(int event) {
            int mode = msvcProfiles.getProfileType();
            if(event == SCR_ON_STATE_REACHED) {
                if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    try {
                        int capacity = Integer.parseInt(Utils.getBattCapacity());
                        DisplayControl.noteCapacity(capacity, mEnv.mPlugged, PowerProfileSvc.this.getApplicationContext());
                    }catch (NumberFormatException nfEx) {
                        Utils.Log.e(LOG_TAG, "Display: Could not parse battery capacity");
                    }
                }
            }else if(event == PLUGGED_STATE_REACHED) {
                DisplayControl.noteModeChange(PowerProfileSvc.this.getApplicationContext(), 
                        BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE);
                DisplayControl.noteChargingMode(true);
            }else if(event == UNPLUGGED_STATE_REACHED) {
                DisplayControl.noteChargingMode(false);
                // If we are in preset smart mode - ignore this
                if(!msvcProfiles.isPresetMode() ||
                        mode != BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    DisplayControl.noteModeChange(PowerProfileSvc.this.getApplicationContext(),
                            mode);
                }
            }
        }
    }
    
    private class ProfileManager {
        private class Profile {
            public int startMins;
            public int endMins;
        }

        private Profile mProfile;
        private int mType;
        private boolean mIsPreset;
        private boolean mWifiOption;
        private boolean mPeakWifiOption;
        private int mUserSmartBattTimeout;
        private int mUserMaxBattTimeout;
        private int mUserSmartBattOffPeakTimeout;
        private boolean mUserNotified;
        private int mRequestedDataMode;
        private int mKeepDataOn;
        private boolean mKeepDataOnWithTimer;
        private long mKeepDataOnTimerStart;
        private long mKeepDataOnTimerEnd;
        private int mKeepDataOff;
        private int mKeepDataOffPersistent;
        private int mKeepDataOnPersistent;
        private Context mContext;

        public ProfileManager(Context ctx) {
            mContext = ctx;
            update(ctx);
        }

        public void update(Context ctx) {
            SharedPreferences myPref = ctx.getSharedPreferences(BatteryProfile.OPTIONS_STORE, Context.MODE_PRIVATE);

            mType = myPref.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE,
                                    BatteryProfile.DEFAULT_PRESET_MODE);
            mIsPreset = myPref.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET,
                                    BatteryProfile.DEFAULT_OPTION_SELECT);

            if(mProfile == null) {
                mProfile = new Profile();
            }

            mProfile.startMins = myPref.getInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                    BatteryProfile.DEFAULT_OFFPEAK_START);
            mProfile.endMins = myPref.getInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                    BatteryProfile.DEFAULT_OFFPEAK_END);

            Utils.Log.d(LOG_TAG, "Profiles: startMins:" + mProfile.startMins +
                    ",endMins:" + mProfile.endMins);

            mWifiOption = myPref.getBoolean(BatteryProfile.KEY_OPTION_OFFPEAK_ALLOW_WIFI,
                                             BatteryProfile.DEFAULT_OFFPEAK_ALLOW_WIFI);
            mPeakWifiOption = myPref.getBoolean(BatteryProfile.KEY_OPTION_PEAK_ALLOW_WIFI,
                                            BatteryProfile.DEFAULT_PEAK_ALLOW_WIFI);
            mUserSmartBattTimeout = myPref.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT,
                    BatteryProfile.DEFAULT_PEAK_DATATIMEOUT);
            mUserMaxBattTimeout = myPref.getInt(
                    BatteryProfile.KEY_OPTION_PRESET_MAXSAVER_DATATIMEOUT,
                    BatteryProfile.DEFAULT_PRESET_MAXSAVER_DATATIMEOUT);
            mUserSmartBattOffPeakTimeout = myPref.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                    BatteryProfile.DEFAULT_OFFPEAK_DATATIMEOUT);
            mUserNotified = myPref.getBoolean(BatteryProfile.KEY_NOTIFY_USER_PROFILE_SELECT,
                    false);
            mRequestedDataMode = myPref.getInt(BatteryProfile.KEY_REQ_DATA_MODE,
                    BatteryProfile.DEF_REQ_DATA_MODE);
            mKeepDataOn = myPref.getInt(BatteryProfile.KEY_KEEP_DATA_ON, 0);
            mKeepDataOnWithTimer = myPref.getBoolean(BatteryProfile.KEY_KEEP_DATA_ON_WITH_TIMER, false);
            mKeepDataOff = myPref.getInt(BatteryProfile.KEY_KEEP_DATA_OFF, 0);
            mKeepDataOnTimerStart = myPref.getLong(BatteryProfile.KEY_KEEP_DATA_ON_TIMER_START, 0L);
            mKeepDataOnTimerEnd = myPref.getLong(BatteryProfile.KEY_KEEP_DATA_ON_TIMER_END, 0L);
            mKeepDataOffPersistent = myPref.getInt(BatteryProfile.KEY_KEEP_DATA_OFF_PERSISTENT, 0);
            mKeepDataOnPersistent = myPref.getInt(BatteryProfile.KEY_KEEP_DATA_ON_PERSISTENT, 0);
        }
 
        private void writeInt(String key, int value) {
            SharedPreferences myPref = mContext.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor myPrefEditor = myPref.edit();
            myPrefEditor.putInt(key, value);
            myPrefEditor.commit();
        }

        private void writeBoolean(String key, boolean value) {
            SharedPreferences myPref = mContext.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor myPrefEditor = myPref.edit();
            myPrefEditor.putBoolean(key, value);
            myPrefEditor.commit();
        }

        private void writeLong(String key, long value) {
            SharedPreferences myPref = mContext.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor myPrefEditor = myPref.edit();
            myPrefEditor.putLong(key, value);
            myPrefEditor.commit();
        }

        public int keepDataOn() { return mKeepDataOn; }

        public boolean keepDataOnWithTimer() { return mKeepDataOnWithTimer; }

        public long keepDataOnTimerStart() { return mKeepDataOnTimerStart; }

        public long keepDataOnTimerEnd() { return mKeepDataOnTimerEnd; }

        public int requestedDataMode() { return mRequestedDataMode; }

        public int keepDataOff() { return mKeepDataOff; }

        public int keepDataOffPersistent() { return mKeepDataOffPersistent; }

        public int keepDataOnPersistent() { return mKeepDataOnPersistent; }

        public void setKeepDataOff(int value) {
            // This should not happen, in case happens keep it as 0
            int v = (value < 0)?0:value;
            writeInt(BatteryProfile.KEY_KEEP_DATA_OFF, v);
            mKeepDataOff = v;
            Utils.Log.d(LOG_TAG,"KeepDataOff Clients =" + mKeepDataOff);
        }

        public void setKeepDataOn(int value) {
            // This should not happen, in case happens keep it as 0
            int v = (value < 0)?0:value;
            writeInt(BatteryProfile.KEY_KEEP_DATA_ON, v);
            mKeepDataOn = v;
            Utils.Log.d(LOG_TAG,"KeepDataOn Clients =" + mKeepDataOn);
        }

        public void setKeepDataOnWithTimer(boolean value) {
            writeBoolean(BatteryProfile.KEY_KEEP_DATA_ON_WITH_TIMER, value);
            mKeepDataOnWithTimer = value;
            Utils.Log.d(LOG_TAG,"Timer Client =" + mKeepDataOnWithTimer);
        }

        public void setKeepDataOnTimerStart(long value) {
            writeLong(BatteryProfile.KEY_KEEP_DATA_ON_TIMER_START, value);
            mKeepDataOnTimerStart = value;
            Utils.Log.d(LOG_TAG,"Timer Start =" + mKeepDataOnTimerStart);
        }

        public void setKeepDataOnTimerEnd(long value) {
            writeLong(BatteryProfile.KEY_KEEP_DATA_ON_TIMER_END, value);
            mKeepDataOnTimerEnd = value;
            Utils.Log.d(LOG_TAG,"Timer End =" + mKeepDataOnTimerEnd);
        }

        public void setKeepDataOffPersistent(int value) {
            // This should not happen, in case happens keep it as 0
            int v = (value < 0)?0:value;
            writeInt(BatteryProfile.KEY_KEEP_DATA_OFF_PERSISTENT, v);
            mKeepDataOffPersistent = v;
            Utils.Log.d(LOG_TAG,"KeepDataOffPersistent Clients =" + mKeepDataOffPersistent);
        }

        public void setKeepDataOnPersistent(int value) {
            // This should not happen, in case happens keep it as 0
            int v = (value < 0)?0:value;
            writeInt(BatteryProfile.KEY_KEEP_DATA_ON_PERSISTENT, v);
            mKeepDataOnPersistent = v;
            Utils.Log.d(LOG_TAG,"KeepDataOnPersistent Clients =" + mKeepDataOnPersistent);
        }

        public void setRequestedDataMode(int value) {
            writeInt(BatteryProfile.KEY_REQ_DATA_MODE, value);
            mRequestedDataMode = value;
            Utils.Log.d(LOG_TAG,"Requested BM mode =" + mRequestedDataMode);
        }

        public boolean isUserNotified() {
            return mUserNotified;
        }
 
        public void userNotified(Context ctx) {
            SharedPreferences myPref = ctx.getSharedPreferences(BatteryProfile.OPTIONS_STORE, 
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor myPrefEditor = myPref.edit();
            myPrefEditor.putBoolean(BatteryProfile.KEY_NOTIFY_USER_PROFILE_SELECT, true);
            myPrefEditor.commit();
            mUserNotified = true;
        }

        public int getProfileType() {
            return mType;
        }

        public boolean isPresetMode() {
            return mIsPreset;
        }

        public boolean shouldControlWifi() {
            return !mWifiOption;
        }
        
        public boolean shouldControlPeakWifi() {
            return !mPeakWifiOption;
        }

        public int getUserTimerIndex() {
            if(mType == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                return getIndex(mUserSmartBattTimeout);
            }else if(mType == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                return getIndex(mUserMaxBattTimeout);
            }
            return -1;
        }
        
        public int getUserOffPeakTimerIndex() {
            if(mType == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                return getIndex(mUserSmartBattOffPeakTimeout);
            }
            return -1;
        }

        private int getIndex(int timeout) {
            int index = -1;
            if(timeout == 15) {
                index = 0;
            }else if(timeout == 30) {
                index = 1;
            }else if(timeout == 45) {
                index = 2;
            }else if(timeout == 60) {
                index = 3;
            }
            return index;
        }

        public Profile getProfile() {
            return mProfile;
        }
    }

    private class WakeManager {

        public void updateAlarm() {
            stop();
            if(msvcProfiles.getProfileType() == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                ///***
                long wakeTime = 0;
                long endwakeTime = 0;

                /**
                 * Peak wake-up, we will keep updating our alarms
                 * using the off-pak hour start+5 and off-peak hour end + 30
                 * as our markers to make sure the sleep scenarios before
                 * and after start and end off-peak times respectively
                 **/
                ProfileManager.Profile myProfile = msvcProfiles.getProfile();

                Time currTime = new Time();
                currTime.set(System.currentTimeMillis());
                long currMins = (long)(currTime.hour * 60) + (long)currTime.minute;
                long startMins = (long)myProfile.startMins;
                long endMins = (long)myProfile.endMins;

                if(currMins < startMins) {
                    wakeTime = startMins - currMins;
                }else if(currMins >= startMins) {
                    wakeTime = (24L * 60L) - (currMins - startMins);
                }

                if(currMins < endMins) {
                    endwakeTime = endMins - currMins + 15;
                }else if(currMins >= endMins) {
                    endwakeTime = (24L * 60L) - (currMins - endMins) + 15;
                }

                if(endwakeTime < wakeTime) {
                    wakeTime = endwakeTime;
                } 

                wakeTime = (wakeTime + 5) * 60L * 1000L;

                msvcAlarms.setNonWakeUpAlarm(wakeTime + System.currentTimeMillis(), ALRM_WAKE_TIMER_ACTION);
            }
        }

        public void stop() {
            msvcAlarms.cancelAlarm(ALRM_WAKE_TIMER_ACTION);
        }
    }

    private class Receiver extends BroadcastReceiver {

        private Context mContext;
        private int mplug_changed;
        private int mscreen_on;

        private final static int SCREEN_ON = 1;
        private final static int SCREEN_OFF = 0;

        @Override
        public void onReceive(Context ctx, Intent bcIntent) {
            String bcast = bcIntent.getAction();

            if(bcast.equals(Intent.ACTION_SCREEN_ON)) {
                if(mscreen_on != SCREEN_ON) {
                    mscreen_on = SCREEN_ON;
                    mContext.startService(new Intent(SCREEN_ON_ACTION)
                            .setClass(mContext, PowerProfileSvc.class));
                    Utils.Log.d(LOG_TAG, "Receiver: Screen On");
                }
            }else if(bcast.equals(Intent.ACTION_SCREEN_OFF)){
                if(mscreen_on != SCREEN_OFF) {
                    mscreen_on = SCREEN_OFF;
                    mContext.startService(new Intent(SCREEN_OFF_ACTION)
                            .setClass(mContext, PowerProfileSvc.class));
                    Utils.Log.d(LOG_TAG, "Receiver: Screen Off");
                }
            }else if(bcast.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int plugged = bcIntent.getIntExtra("plugged", 0);
                if(plugged != mplug_changed) {
                    mplug_changed = plugged;
                    Utils.Log.d(LOG_TAG, "Receiver: Plugged --> " + mplug_changed);
                    if((plugged == BatteryManager.BATTERY_PLUGGED_AC) || 
                            (plugged == BatteryManager.BATTERY_PLUGGED_USB)) {
                        mContext.startService(new Intent(PLUGGED_ACTION)
                                .setClass(mContext, PowerProfileSvc.class));
                    }else {
                        mContext.startService(new Intent(UNPLUGGED_ACTION)
                                .setClass(mContext, PowerProfileSvc.class));
                    }
                }
            }
        }

        public Receiver(Context ctx) {
            mContext = ctx;
            mplug_changed = -1;
            mscreen_on = -1;
            IntentFilter ifilter = new IntentFilter();
            ifilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            ifilter.addAction(Intent.ACTION_SCREEN_ON);
            ifilter.addAction(Intent.ACTION_SCREEN_OFF);
            mContext.registerReceiver(this, ifilter);
        }

        public void stop() {
            mContext.unregisterReceiver(this);
        }

        private long baseTxPackets = -1;
        private long baseRxPackets = -1;

        public void startDataMonitor() {
            baseTxPackets = TrafficStats.getMobileTxPackets();
            baseRxPackets = TrafficStats.getMobileRxPackets();
        }

        public boolean getMonitorState() {
            boolean misdataIdle = ((baseTxPackets != TrafficStats.getMobileTxPackets()) ||
                    (baseRxPackets != TrafficStats.getMobileRxPackets()));
            Utils.Log.d(LOG_TAG, "DynamicDataReceiver: Data state is " +
                    misdataIdle);
            return !misdataIdle;
        }

        public void stopDataMonitor() {
        }
    }

    public boolean handleMessage(Message msg) {
        if(msg.what == SVC_MSG_WIFI_ENABLE_ID) {
            msvcAlgo.mwifiControl.enable();
            mwifiLock.release();
            mwifiLock = null;
            return true;
        }
        return false;
    }

}

