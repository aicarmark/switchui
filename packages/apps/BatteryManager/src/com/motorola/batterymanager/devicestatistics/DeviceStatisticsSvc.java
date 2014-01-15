/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.PowerManager;
import android.util.Log;

/**
 * Service handles passive logging of devicestatistics to blur server
 */
public class DeviceStatisticsSvc extends Service{

    private static final String TAG = DevStatPrefs.CHECKIN_EVENT_ID;

    // Intents
    private static final String DEVSTATS_CHECKIN_STATS_ALARM = 
            "android.intent.action.devicestatistics.CHECKIN_STATS_ALARM";

    // Actions
    public final static int DEVSTATS_START_ALARM_ACTION = 1;
    public final static int DEVSTATS_CHECKIN_STATS_ACTION = 2;
    public final static int DEVSTATS_WIFI_CONNECTION_CHANGE_ACTION = 3;
    public final static int DEVSTATS_WORK_DONE_ACTION = 4;

    //Preferences
    private DevStatPrefs mPrefs;

    private PowerManager.WakeLock mWlock = null;
    private static int mCheckinStartId= 0;
    private static boolean mOnBootCheckinReq = false;
 
    @Override
    public int onStartCommand(Intent startInt, int flags, int startId) {
        if(startInt == null) {
            Log.d(TAG, "onStartCommand: start Intent is null, note crash restart");
            return Service.START_STICKY;
        }

        mPrefs = DevStatPrefs.getInstance(this);

        Message msg = serviceHandler.obtainMessage(); 
        String action = startInt.getAction();
        msg.arg1 = startId;
        boolean acquireWl = false;

        Log.d(TAG, "onStartCommand: received action " + action);
	if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            msg.what = DEVSTATS_START_ALARM_ACTION;
            if (isCheckinRequired()) {
                acquireWl = true;
                mOnBootCheckinReq = true;
                mCheckinStartId = startId;
            }
        } else if (action.equals(DEVSTATS_CHECKIN_STATS_ALARM)) {
            msg.what = DEVSTATS_CHECKIN_STATS_ACTION;
            acquireWl = true;
            mCheckinStartId = startId;
        } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            msg.what = DEVSTATS_WIFI_CONNECTION_CHANGE_ACTION;
        }
  
        // For now acquire Wl only if checkin thread needs to be started
        if (acquireWl) {
            Log.d(TAG, "onStartCommand: Device Stat Service acquiring wl");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWlock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWlock.acquire();
        }
	serviceHandler.sendMessage(msg);
		
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
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
                    checkinDeviceStats();
                break;
                case DEVSTATS_START_ALARM_ACTION:
                    startRepeatingAlarm();

                    // Since boot time, reset current rx & tx bytes to 0
                    mPrefs.setMobileRxBytes(0);
                    mPrefs.setMobileTxBytes(0);

                    if (mOnBootCheckinReq) {
                        checkinDeviceStats();
                    } else {
                        stopSvc = true;
                    }
                    break;
                case DEVSTATS_WIFI_CONNECTION_CHANGE_ACTION:
                    handleWifiDisconnect();
                    stopSvc = true;
                    break;
                case DEVSTATS_WORK_DONE_ACTION:
                    msg.arg1 = mCheckinStartId;
                    if(mWlock != null && mWlock.isHeld()) {
                        Log.d(TAG, "Handler: DeviceStat Service releasing wakelock");
                        mWlock.release();
                    }
                    stopSvc = true;
                break;
            }
     
            if (stopSvc) {
                stopSelf(msg.arg1);
            }
        }
    };

    /**
     * Starts a Repeating Alarm for passive device Stats checkin
     */
    private void startRepeatingAlarm() {
    	AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
  
    	Intent bcIntent = new Intent(DEVSTATS_CHECKIN_STATS_ALARM);
        PendingIntent pInt = PendingIntent.getBroadcast(getBaseContext(), 0, 
                 bcIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pInt);

        // Added, so that frequency inputs can be changed for testing
        final long frequency = mPrefs.getDevStatCheckinFrequency();

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 
                SystemClock.elapsedRealtime() + frequency, frequency, pInt);
    	Log.d(TAG, "RepeatingAlarm Set for " + frequency);
    }

    /**
     * In case, use powercycle the device before launch interval hit everytime. Then there 
     * will be no checkins. Hence on everypower cycle, check if launch interval is crossed. 
     * Note: This uses system clock.
     * Return true if checkin to be started otherwise false.
     */
    private boolean isCheckinRequired() {
        final long currentTime = System.currentTimeMillis();
        final long lastCheckinTime = mPrefs.getDevStatLastCheckinTime(currentTime);
        final long frequency = mPrefs.getDevStatCheckinFrequency();
        Log.d(TAG, "startCheckinIfDelayedSeen: " +
                 " LastCheckinTime= " + lastCheckinTime +
                 " currentTime = " + currentTime + " Launch Period = " + frequency);
        if (((currentTime - lastCheckinTime) >= frequency) ||
            (currentTime == lastCheckinTime)){
            Log.d(TAG, "startCheckinIfDelayedSeen: Its time to Checkins Device Statistics..." );
            return true;
        }
        return false;
    }

    /**
     * Starts a low priority thread to collect statistics and checkin to DB.
     */
    private void checkinDeviceStats() {
        Thread statsThread = new CheckInThread(this, serviceHandler);
        statsThread.setPriority(Thread.MIN_PRIORITY);
        statsThread.start(); 
    }

    /**
     * Handles Wifi Disconnect to accumulate WifiBytes
     */
    private void handleWifiDisconnect() {
        SysClassNetUtils.updateWifiRxTxBytes(this);
    }
}
