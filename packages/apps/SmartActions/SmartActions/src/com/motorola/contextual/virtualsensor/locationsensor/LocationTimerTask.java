/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2011/05/27 IKCTXTAW-19		   Initial version
 */

package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateUtils;

import com.motorola.android.wrapper.AlarmManagerWrapper;
import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import java.util.HashMap;
import java.util.Map;


/**
 *<code><pre>
 * CLASS:
 *  This class check the following condition to decide how to perform periodical updates.
 *  STOP: 1. wifi connected or no movement, handle inside detection.
 *        2. PNO registered
 *        3. wifi off, smart scan supported from framework supported!
 *
 * RESPONSIBILITIES:
 *   responsible for triggering periodical updates, requesting either network location updates, or timer wifi scan.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public final class LocationTimerTask extends BroadcastReceiver {

    private static final String TAG = "LSAPP_TimerTask";
    public static final String TIMERTASK_STARTSCAN =  PACKAGE_NAME + ".startscan";

    static final long EXTENDED_STATION = 10*DateUtils.MINUTE_IN_MILLIS;   // wait for 10 minutes since no movement.

    static final String MOTION_INTENT = "com.motorola.intent.action.MOTION";
    static final String STILL_INTENT = "com.motorola.intent.action.STILL";
    static final String MOTION_TRIGGER_INTENT = "com.motorola.contextual.Motion";
    static final String PUBLISH_EXTRA = "com.motorola.contextual"+".PUBLISH";
    static final String START = "start";

    private Context mContext;
    private LocationSensorManager mLSMan;
    private LocationDetection mLSDet;
    private WorkHandler mWorkHandler;
    private MessageHandler mHandler;

    // Strategy for periodical update, either request periodical update from LNP, or periodical timer event to trigger wifi scan.
    private LocationMonitor mLocMon;   //
    private AlarmManagerWrapper mAlarmMan;
    private PhoneModel mPhoneModel;
    long mMotionIntentTime;         // when the last time we recvd motion intent.
    boolean mEndoOfMovement = false;  // STILL intent recvd for no movement, so we can stop scan.

    private Map<String, PendingIntent> mTasks;   // periodic task name, and its trigger intent for re-use.

    /**
     * Constructor
     * @param context
     * @param hdl
     */
    public LocationTimerTask(final LocationSensorManager lsman, final LocationDetection lsdet) {
        mLSMan = lsman;
        mLSDet = lsdet;
        mContext = (Context)mLSMan;

        mWorkHandler = new WorkHandler(TAG);
        mHandler = new MessageHandler(mWorkHandler.getLooper());

        // periodical update strategy, either timer to tigger wifi scan, or LNP updates.
        mAlarmMan = (AlarmManagerWrapper)SystemWrapper.getSystemService(mContext, Context.ALARM_SERVICE);
        // pass the detection handler, so location updates go to detection.
        mLocMon = new LocationMonitor(mLSMan, mLSDet.getDetectionHandler());

        mMotionIntentTime = System.currentTimeMillis();  // initialize

        IntentFilter filter = new IntentFilter();
        filter.addAction(TIMERTASK_STARTSCAN);
        filter.addAction(MOTION_INTENT);
        filter.addAction(STILL_INTENT);
        mContext.registerReceiver(this, filter);

        mPhoneModel = new PhoneModel(mContext, mLSDet, mHandler);  // this is the handle of timer task.

        mTasks = new HashMap<String, PendingIntent>();  // list of tasks, not used for now.
        // let sensor hub work hard
        // mContext.sendBroadcast(new Intent(MOTION_TRIGGER_INTENT).putExtra(PUBLISH_EXTRA, START));
    }

    /**
     * finalizer, un-reg the listener, called by location detection upon destroy.
     */
    public void clean() {
        mContext.unregisterReceiver(this);
        mPhoneModel.clean();
    }

    /**
     * get the phone model we are running on
     */
    public PhoneModel getPhoneModel() {
        return mPhoneModel;
    }

    /**
     * End of movement for more than 10 minutes. 10 min was choose as 2 location request cycle(5 min)
     * @param howlong for how long being in no movement.
     * @return true: STILL intent recvd for more than 10 minutes.
     *         false: otherwise.
     */
    public boolean isEndOfMovement(long howlong) {
        if(mEndoOfMovement && (System.currentTimeMillis() - mMotionIntentTime > howlong)) {
            return true;
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LSAppLog.d(TAG, ": onReceiver :" + action);
        Message msg = mLSDet.getDetectionHandler().obtainMessage();

        if (TIMERTASK_STARTSCAN.equals(action)) {
            msg.what = LocationDetection.Msg.START_SCAN;
            LSAppLog.i(TAG, ": onReceive : scan timer expired, start scan :" + action);
        } else if (MOTION_INTENT.equals(action)) {
            mMotionIntentTime = System.currentTimeMillis();
            mEndoOfMovement = false;    // not end of movement, scan, work hard.
            msg.what = LocationDetection.Msg.MOTION_TRIGGER;
            // should I trigger a run detection here ?
            LSAppLog.i(TAG, ": onReceive : motion trigger: motion intent :" + action);
        } else if (STILL_INTENT.equals(action)) {
            mMotionIntentTime = System.currentTimeMillis();
            mEndoOfMovement = true;    // not end of movement, scan, work hard.
            msg.what = LocationDetection.Msg.MOTION_TRIGGER;
            LSAppLog.i(TAG, ": onReceive : motion trigger: still intent :" + action);
        }
        mLSDet.getDetectionHandler().sendMessage(msg);
    }

    /**
     * messge looper
     */
    final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processMessage(msg);
        }
    }
    private void processMessage(android.os.Message msg) {
        switch (msg.what) {
        }
    }


    /**
     * we need to get periodical update of location to detect. Drive the periodical update from
     * either android location provider, or from periodical timer.
     * startPeriodicalUpdate, to save power, do not use location request if all poi's have wifi
     */
    public void startPeriodicalUpdate() {
        if(mLSDet.mPoiAdapter.areAllPoisHaveWifi()
                || (mLSDet.mDetCtrl.getNextPoi() != null && mLSDet.mDetCtrl.poiHasWifi(mLSDet.mDetCtrl.getNextPoi()))
          ) {
            startPeriodicalUpdateWifi(LOCATION_DETECTING_UPDATE_INTERVAL_MILLIS);
        } else {
            startPeriodicalUpdateLocation(LOCATION_DETECTING_UPDATE_INTERVAL_MILLIS, LOCATION_DETECTING_UPDATE_MAX_DIST_METERS);
        }
    }

    /**
     * stop the periodical update engine.
     * @param stopLocProvider  if true, stop android network provider update.
     * @param stopWifiTimer    if true, stop our periodical timer.
     */
    public void stopPeriodicalUpdate(boolean stopLocProvider, boolean stopWifiTimer) {
        LSAppLog.pd(TAG, "stopPeriodicalUpdate : network provider:" + stopLocProvider + " wifiscan: " + stopWifiTimer);
        if(stopLocProvider) {
            stopLocationUpdate();  //  no harm if you remove a not registered callback
        }
        if(stopWifiTimer) {
            stopPeriodicalUpdateWifi();
        }
    }

    /**
     * start repeated wifi polling task, add the task to the hash map.
     */
    private void startPeriodicalUpdateWifi(long cycle) {
        PendingIntent pi = mTasks.get(TIMERTASK_STARTSCAN);
        if (pi == null) {
            // trigger a scan if has no scan in last cycle, otherwise, will need 5 min later. This is bad when restarted, will be delayed by 5 min.
            if( (System.currentTimeMillis() - mLSDet.mBeacon.mMyLastScanTimestamp) >= cycle) {
                mLSDet.mBeacon.startBeaconScan(null, mLSDet.getDetectionHandler());
            }
            Intent i = new Intent(TIMERTASK_STARTSCAN);
            pi = PendingIntent.getBroadcast(mContext, 0, i,  PendingIntent.FLAG_UPDATE_CURRENT);
            mTasks.put(TIMERTASK_STARTSCAN, pi);
            long firstWake = System.currentTimeMillis() + cycle;
            mAlarmMan.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstWake, cycle, pi);
            LSAppLog.pd(TAG, "startPeriodicalUpdateWifi : started: cycle: " + cycle);
        } else {
            LSAppLog.pd(TAG, "startPeriodicalUpdateWifi : already started: " + cycle);
        }
    }

    /**
     * stop the running periodical wifi polling
     */
    private void stopPeriodicalUpdateWifi() {
        mPhoneModel.setBackgroundScan(false);
        stopTimerTask();
    }

    private void stopTimerTask() {
        PendingIntent pi = mTasks.get(TIMERTASK_STARTSCAN);
        if (pi != null) {
            pi.cancel();
            mAlarmMan.cancel(pi);
            mTasks.remove(TIMERTASK_STARTSCAN);
            LSAppLog.pd(TAG, "stopPeriodicalUpdateWifi : stopped");
        }
    }

    /**
     * start periodical LNP polling
     */
    private void startPeriodicalUpdateLocation(long interval, long meter) {
        LSAppLog.pd(TAG, "startPeriodicalUpdateLocation  cycle: " + interval);
        //mLocMon.startLocationUpdate(LOCATION_DETECTING_UPDATE_INTERVAL_MILLIS, LOCATION_DETECTING_UPDATE_MAX_DIST_METERS, PendingIntent.getBroadcast(mContext, 0, new Intent(LOCATION_UPDATE_AVAILABLE), PendingIntent.FLAG_UPDATE_CURRENT));
        mLocMon.startLocationUpdate(LOCATION_DETECTING_UPDATE_INTERVAL_MILLIS, LOCATION_DETECTING_UPDATE_MAX_DIST_METERS, null);
    }

    /**
     * stop periodical LNP update
     */
    private void stopLocationUpdate() {
        mLocMon.stopLocationUpdate();
    }

    /**
     * get the current location fix from NLP
     */
    public Location getCurrentLocation() {
        return mLocMon.getCurrentLocation();
    }

    /**
     * TDD drives out roles and IF between collaborators.
     */
    public static class Test {
        /**
         * start the timer task, return the job Id
         * @param cycle, the periodical cycle
         * @return the job id
         */
        public static void startPeriodicalPolling() {
        }
        /**
         * stop the on-going periodical job
         * @param jobId
         */
        public static void stopPeriodicalPolling(int jobId) {
        }
    }
}
