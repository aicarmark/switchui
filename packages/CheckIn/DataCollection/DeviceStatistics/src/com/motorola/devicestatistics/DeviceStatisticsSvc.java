/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Service handles passive logging of devicestatistics to blur server
 */
public class DeviceStatisticsSvc extends Service implements DeviceStatsConstants {

    private static final boolean DUMP = false;
    private static final boolean WAKELOCK_DUMP = true;
    private static final String TAG = "DeviceStatsSvc";
    private static final String INTERNAL_TRIGGER = "InternalTrigger";
    private static final String MIDNIGHT_PREF = "MidnightTimerElapsed";

    // Intents
    private static final String DEVSTATS_APP_STATS_ALARM = 
            "com.motorola.devicestatistics.APP_STATS_ALARM";

    // Actions
    public final static int DEVSTATS_START_ALARM_ACTION = 1;
    public final static int DEVSTATS_CHECKIN_STATS_ACTION = 2;
    public final static int DEVSTATS_WIFI_CONNECTION_CHANGE_ACTION = 3;
    public final static int DEVSTATS_WORK_DONE_ACTION = 4;
    public final static int DEVSTATS_APP_STATS_ACTION = 5;
    public final static int DEVSTATS_TIMEWARP_MONITOR_ACTION = 6;
    private static final long MAX_WLOCK_HOLD_MS = 1 * 60 * 1000; // 1 minute

    //Preferences
    private DevStatPrefs mPrefs;
    private LifeCycle mLifeCycle;

    private PowerManager.WakeLock mWlock = null;
    private long mWlAcquireTime = 0;
    private static boolean mOnBootCheckinReq = false;
 
    @Override
    public int onStartCommand(final Intent startInt, final int flags, final int startId) {
        new Utils.RunInBackgroundThread() {
            public void run() {
                onStartCommandImpl(startInt, flags, startId);
            }
        };
        return Service.START_STICKY;
    }

    private void onStartCommandImpl(Intent startInt, int flags, int startId) {
        if (Watchdog.isDisabled()) return;

        if(startInt == null) {
            Log.d(TAG, "onStartCommand: start Intent is null, note restart");
            // We cannot do much here - so shut down right away
            stopSelf(startId);
            return;
        }

        mPrefs = DevStatPrefs.getInstance(this);

        Message msg = serviceHandler.obtainMessage();
        String action = startInt.getAction();
        if (action == null) {
            stopSelf(startId);
            return;
        }
        msg.arg1 = startId;
        boolean acquireWl = false;

        Log.d(TAG, "onStartCommand: received action " + action);
	    if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            msg.what = DEVSTATS_START_ALARM_ACTION;
            int reason = isCheckinRequired();
            if ((reason & DevStatPrefs.STARTUP_MASK) != DevStatPrefs.STARTUP_NO_LOG) {
                acquireWl = true;
                mOnBootCheckinReq = true;
                msg.arg2 = reason; // needed for checkin thread
            }
        } else if (action.equals(DEVSTATS_CHECKIN_STATS_ALARM)) {
            msg.what = DEVSTATS_CHECKIN_STATS_ACTION;
            msg.obj = startInt;
            acquireWl = true;
        } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            msg.what = DEVSTATS_WIFI_CONNECTION_CHANGE_ACTION;
        } else if (action.equals(DEVSTATS_APP_STATS_ALARM)) {
            msg.what = DEVSTATS_APP_STATS_ACTION;
            acquireWl = true;
        } else if (action.equals(TIME_WARP_MONITOR_ACTION)) {
            msg.what = DEVSTATS_TIMEWARP_MONITOR_ACTION;
            acquireWl = false;
        }
  
        // For now acquire Wl only if checkin thread needs to be started
        if (acquireWl) {
            if(WAKELOCK_DUMP) Log.d(TAG, "onStartCommand: Device Stat Service acquiring wl");
            if(mWlock == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) { mWlock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG); }
            }
            if (mWlock != null) {
                mWlock.acquire(MAX_WLOCK_HOLD_MS);
                if (mWlAcquireTime == 0) mWlAcquireTime = SystemClock.elapsedRealtime();
            }
        }
	    serviceHandler.sendMessage(msg);
        mLifeCycle.noteStart();
		
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Utils.RunInBackgroundThread() {
            public void run() {
                mLifeCycle = new LifeCycle(new LifeCycle.IDeathIndicator() {
                    public void onDead() {
                        DeviceStatisticsSvc.this.onDead();
                    }
                });
            }
        };
    }

    // implements IDeathIndicator
    public void onDead() {
        stopSelf();
    }

    protected Handler serviceHandler = new Handler() { 
        /* (non-Javadoc) 
         * @see android.os.Handler#handleMessage(android.os.Message) 
         */ 
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: Message=" + msg.what);

            boolean stopSvc = false;
            switch (msg.what) {
                case DEVSTATS_CHECKIN_STATS_ACTION:
                    Intent intent = (Intent)msg.obj;
                    String usbExtra = intent.getStringExtra(DEVSTATS_CHECKIN_STATS_USB_EXTRA );
                    boolean internalTrigger = intent.getBooleanExtra(INTERNAL_TRIGGER, false);

                    // When this call is due to midnight alarm, usbExtra will be null.
                    if (usbExtra == null && internalTrigger == true) {
                        restartRepeatingAlarm();

                        long lastElapsedMs = mPrefs.getLongSetting(MIDNIGHT_PREF, 0);
                        if (lastElapsedMs != 0 ) {
                            // At least 6 hours should elapse between successive midnight logs
                            long elapsedSinceLastMs = SystemClock.elapsedRealtime() - lastElapsedMs;
                            if (elapsedSinceLastMs > 0 &&
                                    elapsedSinceLastMs < 6 * MS_IN_HOUR) {
                                Log.e(TAG, "Time since last is too low : " + elapsedSinceLastMs );
                                return;
                            }
                        }
                        mPrefs.setLongSetting(MIDNIGHT_PREF, SystemClock.elapsedRealtime());
                    }

                    int reason = DevStatPrefs.STARTUP_DEV_STATS_LOG |
                            getPackageMapStatus();
                    checkinDeviceStats(reason, msg.arg1, usbExtra);
                break;
                case DEVSTATS_START_ALARM_ACTION:
                    // Allow the first midnight report after boot
                    mPrefs.setLongSetting(MIDNIGHT_PREF, 0);

                    restartRepeatingAlarm();
                    calibrateAppStatsAlarm();

                    // Since boot time, reset current rx & tx bytes to 0
                    mPrefs.setMobileRxBytes(0);
                    mPrefs.setMobileTxBytes(0);

                    if (mOnBootCheckinReq) {
                        checkinDeviceStats(msg.arg2,
                                msg.arg1, null);
                        PkgStatsUtils.resetMapperDumpTime(DeviceStatisticsSvc.this,
                                System.currentTimeMillis());
                    } else {
                        stopSvc = true;
                    }
                    break;
                case DEVSTATS_WIFI_CONNECTION_CHANGE_ACTION:
                    handleWifiDisconnect();
                    stopSvc = true;
                    break;
                case DEVSTATS_WORK_DONE_ACTION:
                    if (mWlAcquireTime != 0) {
                        long holdMs = SystemClock.elapsedRealtime() - mWlAcquireTime;
                        if (holdMs >= MAX_WLOCK_HOLD_MS) Log.e(TAG, "Long wakelock " + holdMs );
                        mWlAcquireTime = 0;
                    }
                    if(mWlock != null && mWlock.isHeld()) {
                        if(WAKELOCK_DUMP) Log.d(TAG, "Handler: DeviceStat Service releasing wakelock");
                        mWlock.release();
                    }
                    stopSvc = true;
                break;
                case DEVSTATS_APP_STATS_ACTION:
                    if(isValidWakeup()) {
                        if(DUMP)
                            Log.v(TAG, "Handler: starting app stats log");
                        checkinDeviceStats(DevStatPrefs.STARTUP_APP_STATS_LOG,
                                msg.arg1, null);
                        updateAppStatsRefTime();
                        calibrateAppStatsAlarm();
                    }else {
                        if(WAKELOCK_DUMP) Log.d(TAG, "Handler: aborting app stats because invalid");
                        calibrateAppStatsAlarm();

                        if (mWlAcquireTime != 0) {
                            long holdMs= SystemClock.elapsedRealtime() - mWlAcquireTime;
                            if (holdMs > MAX_WLOCK_HOLD_MS) Log.e(TAG, "Long wakelock " + holdMs );
                            mWlAcquireTime = 0;
                        }
                        if(mWlock != null && mWlock.isHeld()) {
                            mWlock.release();
                        }
                        stopSvc = true;
                    }
                break;
                case DEVSTATS_TIMEWARP_MONITOR_ACTION:
                    restartRepeatingAlarm();
                    stopSvc = true;
                break;
            }
     
            if (stopSvc) {
                // We cannot do out-of-order stopSelf requests with ids, which pretty
                // much removes the usability of this API. We need to maintain our own
                // life cycle of when we want to stop ourselves.
                //stopSelf(msg.arg1);
                mLifeCycle.noteStop();
            }
        }
    };

    private boolean isValidWakeup() {
        // sanity check - should atleast be 23 hours from last poll
        long now = System.currentTimeMillis();
        long last = mPrefs.getLongSetting(DevStatPrefs.DEVSTATS_APPSTATS_REFTIME
                , 0);
        if(DUMP)
            Log.v(TAG, "isValidWakeup:now:" + now + ",last:" + last);
        return (last == 0 ||
                (now - last > (23*3600*1000)));
    }

    private void updateAppStatsRefTime() {
        if(DUMP)
            Log.v(TAG, "updateAppStatsRef:" + System.currentTimeMillis());
        mPrefs.setLongSetting(DevStatPrefs.DEVSTATS_APPSTATS_REFTIME,
                System.currentTimeMillis());
    }

    private void calibrateAppStatsAlarm() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(
                TimeZone.getTimeZone("GMT+0"));
        cal.setTimeInMillis(now);
        int dayHour = cal.get(Calendar.HOUR_OF_DAY);
        int hourMin = cal.get(Calendar.MINUTE);

        if(DUMP)
            Log.v(TAG, "calibrateApp:n:" + now + ",h:" + dayHour +
                    ",m:" + hourMin);
        // now find time to 23:30 on this day
        long msLeft = (23 * 60) + 30;
        msLeft -= ((dayHour * 60) + hourMin);
        if(msLeft < 60) {
            // not enough time today, go to next day
            msLeft = (24 * 60) + msLeft;
        }
        msLeft *= (60 * 1000);

    	AlarmManager alarmManager = (AlarmManager)
                getSystemService(Context.ALARM_SERVICE);
  
    	Intent bcIntent = new Intent(DEVSTATS_APP_STATS_ALARM);
        PendingIntent pInt = PendingIntent.getBroadcast(getBaseContext(), 0, 
                 bcIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pInt);
        alarmManager.set(AlarmManager.RTC_WAKEUP, 
                now + msLeft, pInt);
        if(DUMP)
            Log.v(TAG, "calibrateApp: Alarm will be at:" + msLeft);
    }

    /**
     * Starts a Repeating Alarm for passive device Stats checkin, after removing any older alarms
     */
    private void restartRepeatingAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
  
        Intent bcIntent = new Intent(DEVSTATS_CHECKIN_STATS_ALARM);
        bcIntent.putExtra(INTERNAL_TRIGGER, true);

        // Do not use FLAG_CANCEL_CURRENT as the flag here, because that caused multiple
        // cancelled pending intents to accumulate in AlarmManager.
        PendingIntent pInt = PendingIntent.getBroadcast(getBaseContext(), 0, 
                 bcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pInt);

        // Added, so that frequency inputs can be changed for testing
        final long frequency = mPrefs.getDevStatCheckinFrequency();
        long timeOfFirstAlarmMs = System.currentTimeMillis() + frequency;

        if (frequency == MS_IN_DAY) {
            /* If checkin frequency is at the default 24 hours,
             * then change the logic to report at midnight everyday.
             */
            timeOfFirstAlarmMs = DevStatUtils.getMillisecsAtMidnight(MS_IN_DAY);
        }

        // Although this is a repeating alarm, it is cancelled and recreated each time it expires
        alarmManager.setRepeating(AlarmManager.RTC, timeOfFirstAlarmMs, frequency, pInt);
        Log.d(TAG, "RepeatingAlarm Set for " + frequency);
    }

    /**
     * In case, use powercycle the device before launch interval hit everytime. Then there 
     * will be no checkins. Hence on everypower cycle, check if launch interval is crossed. 
     * Note: This uses system clock.
     * Return bit mask of what needs to be logged if checkin to be started otherwise false.
     */
    private int isCheckinRequired() {
        int reason = DevStatPrefs.STARTUP_NO_LOG;

        // First check for periodic stats log
        final long currentTime = System.currentTimeMillis();
        final long lastCheckinTime = mPrefs.getDevStatLastDailyCheckinTime(currentTime);
        final long frequency = mPrefs.getDevStatCheckinFrequency();
        Log.d(TAG, "startCheckinIfDelayedSeen: " +
                 " LastCheckinTime= " + lastCheckinTime +
                 " currentTime = " + currentTime + " Launch Period = " + frequency);
        if (((currentTime - lastCheckinTime) >= frequency) ||
            (currentTime == lastCheckinTime) ||
            (DevStatUtils.getMillisecsAtMidnight(lastCheckinTime, MS_IN_DAY) !=
             DevStatUtils.getMillisecsAtMidnight(MS_IN_DAY))){
            Log.d(TAG, "startCheckinIfDelayedSeen: Its time to Checkins Device Statistics..." );
            reason |= DevStatPrefs.STARTUP_DEV_STATS_LOG;
        }

        // Next check if batt cap has changed
        String oldCap = mPrefs.getStringSetting(DevStatPrefs.DEVSTATS_BATT_CAP);
        String currCap = DevStatUtils.getBattTotalCapacity();
        boolean diff = oldCap == null ? currCap != null : (!oldCap.equals(currCap));
        if(diff) reason |= DevStatPrefs.STARTUP_BATT_CAP_LOG;

        // Added for package map checkin
        reason = reason | getPackageMapStatus();

        return reason;
    }

    private int getPackageMapStatus() {
        int reason = DevStatPrefs.STARTUP_NO_LOG;
        long now = System.currentTimeMillis();
        long ref = mPrefs.getLongSetting(DevStatPrefs.DEVSTATS_PMAPS_REFTIME, 0);
        if (ref != 0 && (now - ref > (7*24*3600*1000)))
            reason |= DevStatPrefs.STARTUP_PMAPS_LOG;
        return reason;
    }

    /**
     * Starts a low priority thread to collect statistics and checkin to DB.
     */
    private void checkinDeviceStats(int reason, int id, String usbStateExtra) {
        Message result = 
                serviceHandler.obtainMessage(DEVSTATS_WORK_DONE_ACTION, id, 0);
        Thread statsThread = new CheckInThread(this, result, reason, usbStateExtra);
        statsThread.setPriority(Thread.MIN_PRIORITY);
        statsThread.start(); 
    }

    /**
     * Handles Wifi Disconnect to accumulate WifiBytes
     */
    private void handleWifiDisconnect() {
        SysClassNetUtils.updateWifiRxTxBytes(this);
        SysClassNetUtils.updateWifiRxTxPkts(this);
    }

    static class LifeCycle {
        // Simple class to keep track of outstanding requests between service and
        // threads. Every onStartCommand() increments life, every complete cb
        // decrements life, if done, stopSelf() will be called

        interface IDeathIndicator {
            void onDead();
        }

        private int mLives;
        private IDeathIndicator mCb;

        public synchronized void noteStart() {
            mLives++;
        }

        public synchronized void noteStop() {
            mLives--;
            if(mLives < 0) {
                // If we are here, we are past dead but still around..
                // iow, we are a zombie!!!
                Log.v(TAG, "LifeCycle: ERROR!!! - lives negative");
            }
            if(mLives <= 0 && mCb != null) {
                mCb.onDead();
            }
        }

        public LifeCycle(IDeathIndicator cb) {
            mCb = cb;
        }
    }
}
