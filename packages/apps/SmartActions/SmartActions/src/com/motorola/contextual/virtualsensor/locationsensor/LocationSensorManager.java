/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number            Brief Description
 * ------------- ---------- -----------------    ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19          Initial version
 * w04917        2010/06/30 IKSTABLEFIVE-13038   Fix ANR caused by writing to SharedPreferences in
 *                                               broadcast receiver
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;

import com.motorola.android.wrapper.AlarmManagerWrapper;
import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.contextual.cache.SystemProperty;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.LocTimeTable.Tuple;
import com.motorola.contextual.virtualsensor.locationsensor.metrics.MetricsLogger;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 *<code><pre>
 * CLASS:
 *  implements background location track service mother.
 *
 * RESPONSIBILITIES:
 *  Coordinate all the components to tracking and logging location information.
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public class LocationSensorManager extends Service {

    private static final String TAG = "LSAPP_LSMan";
    private static final long METRIC_INTERVAL = DateUtils.HOUR_IN_MILLIS * 24;

    private static LocationSensorManager _sinstance = null;

    public interface Msg {
        int START_MONITORING 		= 100;    	// start monitoring
        int STOP_MONITORING 		= 101;    	// stop monitoring
        int LOCATION_MATCHED_EVENT 	= 102; 		// fire event, done

        int HEARTBEAT_TIMER_EXPIRED = 200; 	  	// expired every 2 hours with idle
        int START_SELF_HEALING 		= 201;	  	// start self healing
        int LOCATION_TIMER_EXPIRED  = 202;

        int CELLTOWER_CHANGED 	    = 300;  	// get network fix
        int CELLTOWER_DONE 			= 302;

        int BEACONSCAN_RESULT		= 401;
        int DETECTION_ENTERING		= 402;
        int DETECTION_LEAVING		= 403;
        int DETECTION_POIREFRESH	= 404;
        int METRICS_TIMER_EXPIRED 	= 1000;
        int METRICS_WIFI_SCAN_RESULTS_AVAILABLE = 1001;

        int NULL 					= 99999;
    }

    private MetricsLogger metricsLogger = null;
    private AlarmManagerWrapper metricAlarm = null;

    //expose bindable location sensor APIs
    //private LocationSensorEngineApiHandler mAPIHandler;
    //private VSMApiHandler mVSMHandler;   // IF to virtual sensor manager

    public enum LSManState {
        UNINITIALIZED(0), CREATED(1), RUNNING(2), CELLCHANGED(3), TIMEREXPIRED_WAITING4FIX(4), LOCATONCAPTURED(5);
        private final int state;
        LSManState(int state) {
            this.state = state;
        }
        public int getState() {
            return state;
        }
    };
    private LSManState   mState = LSManState.UNINITIALIZED;

    /**
     * when user opted in, will use google fix algorithm. If user opted out, will use wifi based algorithm.
     */
    enum DiscoveryAlgorithmType {
        DISCALG_GOOGLEFIX(0), DISCALG_WIFIMATCHING(1<<0);
        int algorithm;
        DiscoveryAlgorithmType(int algo) {
            this.algorithm = algo;
        }
        public int getDiscoveryAlgorithmType() {
            return this.algorithm;
        }
    }
    DiscoveryAlgorithmType mAlgo = DiscoveryAlgorithmType.DISCALG_GOOGLEFIX;  //default to google fix

    private  WorkHandler mWorkHandler;
    private  MessageHandler mHandler;

    protected LocationSensorApp  	mLSApp;
    protected LocationStore 		mStore;
    protected LocationMonitor    	mLocMon;
    protected TelephonyMonitor      mTelMon;
    protected LocationDetection 	mDetection;
    private   Geocoder 				mGeoCoder;

    private AlarmManagerWrapper mAlarmMan;
    private PendingIntent mTimerExpiredIntent = null;   // the intent give to alarm manager for broadcast.
    private PendingIntent mMetricIntent = null; // intent to trigger metric checkin
    private TimerEventReceiver mTimerEventRecvr;

    private long 	mNewCellStartTime 	= 0;  	// set together with scheduling of 15 min timer.
    private String 	mCurPOI;
    Tuple   mCurLocTuple = null;    // the top entry in loctime is also the current entry.

    //WSCheckin mCheckin;
    BeaconSensors mBeacon;
    private BroadcastReceiver systemBroadcastReceiver = null;

    private void _initialize() {
        if (_sinstance != null) {
            if(LOG_DEBUG) Log.d(TAG, "_initialize, already initialized, do nothing.");
            return;
        }
        
        // Read the system prop on start since
        // Intent.ACTION_AIRPLANE_MODE_CHANGED is not sticky. If the service is
        // killed by the system for some reason, past intent won't be
        // re-delivered.
        SystemProperty.readAirplaneMode(getApplicationContext());

        _sinstance = this;
        mWorkHandler = new WorkHandler(TAG);
        mHandler = new MessageHandler(mWorkHandler.getLooper());
        mAlarmMan = (AlarmManagerWrapper)SystemWrapper.getSystemService(this, Context.ALARM_SERVICE);
        mGeoCoder = new Geocoder(this, Locale.getDefault());

        mNewCellStartTime = 0;
        mCurLocTuple = null;

        //mLSApp = (LocationSensorApp)getApplication();
        mLSApp = new LocationSensorApp((Context)this);
        mLSApp.setLSMan(this); // set the upper layer know the reference to lsman
        // open database first, upgrade first as needed, before provider gets in and query the db
        mStore = new LocationStore(this);
        //mStore.scheduleConsolidationJob();  // setup heal time once started!

        metricsLogger = new MetricsLogger(this);
        mTimerEventRecvr = new TimerEventReceiver(this, mHandler, metricsLogger);
        mTimerEventRecvr.start();

        mDetection = new LocationDetection(this, mHandler);
        mTelMon = new TelephonyMonitor(this, mHandler, mDetection.getDetectionHandler());
        mTelMon.startTelMon();
        mLocMon = new LocationMonitor(this, mHandler);
        mBeacon = new BeaconSensors(this, mTelMon);
        mDetection.setTelMonBeacon(mTelMon, mBeacon);	// set telmon now!!
        // mCheckin = mLSApp.getCheckin();

        // metric logging related
        systemBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (metricsLogger != null) {
                    String intentAction = intent.getAction();
                    if (intentAction != null && intentAction.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                        Message msg = mHandler.obtainMessage(Msg.METRICS_WIFI_SCAN_RESULTS_AVAILABLE);
                        mHandler.sendMessage(msg);
                    }
                }
            }
        };

        metricAlarm = (AlarmManagerWrapper)SystemWrapper.getSystemService(this, Context.ALARM_SERVICE);
        scheduleMetricsAlarmTimer();
        metricsLogger.setAppVersion("2.00");
        metricsLogger.logServiceStarted();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(systemBroadcastReceiver, filter);

        mState = LSManState.RUNNING;   // running... should not have any race condition here!
    }

    /**
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // check settings first. when ada start broadcast come in, the setting already been set.
        if (!isUserOptedIn()) {
            LSAppLog.i(TAG, "onCreate start running : ada_accepted not set...return immediately");
            //return;  // XXX we can still run the service, but wont check in data if user not opted-in.
        }

        _initialize();

        if(LOG_DEBUG) Log.d(TAG, "LocationSensor onCreate() done with state to running...");
    }

    /**
     * If startCommand fired as a result of VSM init complete, then simply notify VSM if we are at a meaningful location
     * (a.k.a. POI). However, if startCommand comes in and we're not started at all, then start up. If we are at a meaningful location
     * upon normal startup, we will also inform the VSM via an intent about the fact we are at a meaningful location.
     *
     * @see android.app.Service#onStart(Intent,int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // check settings first. when ada start broadcast come in, the setting already been set.
        if (!isUserOptedIn()) {
            if(LOG_DEBUG) Log.d(TAG, "onStartCommand start running : ada_accepted not set...stopSelf");
            //stopSelf();       // XXX we can still run even user not opted-in, as we write 0 in db.
            //return START_NOT_STICKY;  // standard privacy covers the using of location based rules.
        }

        // now permission granted, start if not already running.
        if (mState.getState() >= LSManState.RUNNING.getState()) {  // if state is already in running,
            if(LOG_DEBUG) Log.d(TAG, "onStartCommand : already running, do nothing");
            if (intent != null && true == intent.getBooleanExtra(INTENT_PARM_STARTED_FROM_VSM, false)) {
                if(LOG_DEBUG) Log.d(TAG, "onStartCommand : receiving VSM Init complete after running, notify vsm");
                notifyVSMOnStartCommand();  // we are virtual sensor and we need to notify vsm we are up and running.
            }
        }
        // if not running, and boot from ada accepted, start directly.
        else {
            if(LOG_DEBUG) Log.d(TAG, "onStartCommand: service started first time : ada_accepted set, initializing...");

            _initialize();
        }

        // check user location consent intent, start run detection.
        if ( intent != null && true == intent.getBooleanExtra(INTENT_PARM_STARTED_FROM_CONSENT, false)) { // LOCATION_CONSENT_KEY.equals(intent.getAction())){
            if(LOG_DEBUG) Log.d(TAG, "onStartCommand: intent : " + LOCATION_CONSENT_KEY);
            mDetection.getDetectionHandler().sendEmptyMessage(LocationDetection.Msg.POI_REFRESH);  // go thru all poi's to detect
        }

        return START_STICKY;
    }

    public LSManState getLSManState() {
        return mState;
    }

    public Handler getLSManHandler() {
        return mHandler;
    }

    /**
     * singleton pattern.
     */
    public synchronized static LocationSensorManager getInstance() {
        return _sinstance;
    }

    /**
     * @see android.app.Service#onBind(Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        if(LOG_DEBUG) Log.d(TAG, "LocationSensor onBind() called ");
        return null;
    }

    @Override
    public void onDestroy() {
        if (mState.getState() < LSManState.RUNNING.getState()) {
            return;
        }

        if(LOG_DEBUG) Log.d(TAG, "LocationSensor onDestroy...checkin current location");
        if(mCurLocTuple != null  && (int)(mCurLocTuple.getLat()) != 0) {
            updateCurrentLocationDuration();
            mStore.checkinUponLeavingLocation(mCurLocTuple);
        }

        mTelMon.stopTelMon();  // unregister cell change first
        mLocMon.cleanup();
        mDetection.cleanup();
        mBeacon.cleanupReceivers();

        cancelAlarmTimer();
        cancelMetricsAlarmTimer();
        mTimerEventRecvr.stop();

        if (systemBroadcastReceiver != null) {
            unregisterReceiver(systemBroadcastReceiver);
            systemBroadcastReceiver = null;
        }

        metricsLogger.logServiceStopped();
        metricsLogger.checkin();

        _sinstance = null;

        if(LOG_DEBUG) Log.d(TAG, "LocationSensor got killed...update current location time..." );
    }

    /**
     * check whether user has opted in by checking settings. Flow will be different with opt-in or not
     * if user not opted in, we still run the service, as we write 0 in db. We wont check-in data if not opted-in.
     * @return true if user opted in, false otherwise.
     * Side effect, setting the global discovery algorithm accordingly based on opt in choice.
     */
    boolean isUserOptedIn() {
        boolean optedin = false;
        // check settings first. when ada start broadcast come in, the setting already been set.
        if (Settings.System.getInt(this.getContentResolver(), ADA_ACCEPTED_KEY, 0) == 0) {
            if(LOG_DEBUG) Log.d(TAG, "isUserOptedIn : user did NOT opt in");
            mAlgo = DiscoveryAlgorithmType.DISCALG_WIFIMATCHING;
        } else {
            if(LOG_DEBUG) Log.d(TAG, "isUserOptedIn : user opt in");
            mAlgo = DiscoveryAlgorithmType.DISCALG_GOOGLEFIX;
            optedin = true;
        }
        mAlgo = DiscoveryAlgorithmType.DISCALG_WIFIMATCHING;  // for testing.
        return optedin;
    }
    
    /**
     * check whether user consent on auto scan, or generally motorola location privacy
     */
    boolean isUserConsentOnMotoLocation() {
        //boolean initComplete = mLSApp.mAppPref.getBoolean("com.motorola.contextual.smartrules.ri.initcomplete", "com.motorola.contextual.smartrules.ri.initcomplete.string");
        String consentValue = mLSApp.mAppPref.getStringFromPref(mLSApp.mAppPref.LOC_CONSENT_PREF_NAME, mLSApp.mAppPref.HAS_USER_LOC_CONSENT);
        if(LOG_DEBUG) Log.d(TAG, "isUserConsentOnMotoLocation : " + consentValue);
        if ( consentValue != null && "1".equals(consentValue)) {
            return true;
        }
        return false;
    }

    /**
     * if null pending intent, means no pending 15 min timer schedule
     */
    public PendingIntent getPendingIntent() {
        return mTimerExpiredIntent;
    }

    /**
     * set the intent for 15 min delay timer..so that we can discover locations user dwelled for extended stay.
     */
    private final void setPendingIntent(Intent intent) {
        if (intent != null) {
            //mTimerExpiredIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            mTimerExpiredIntent = PendingIntent.getBroadcast(this, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            if(LOG_DEBUG) Log.d(TAG, "setPendingIntent : setting with intent :" + intent.toString());
        } else {
            mTimerExpiredIntent = null;
            if(LOG_DEBUG) Log.d(TAG, "setPendingIntent : nullify the pending intent :");
        }
    }

    /**
     * cancel the existing on-going 15 minute timer, if any...in order for scheduling a new one.
     */
    private final void cancelAlarmTimer() {
        mAlarmMan.cancel(mTimerExpiredIntent);
        if (mTimerExpiredIntent != null) {
            if(LOG_DEBUG) Log.d(TAG, "cancelAlarmTimer : canceling existing pending 15 min timer" + mTimerExpiredIntent.toString());
            mTimerExpiredIntent.cancel();
            mTimerExpiredIntent = null;
        }
    }

    /**
     * set the alarm timer to log the current system metrics.
     * e.g. arriving leaving location, wifi status, location measurements and accuracy.
     */
    private void scheduleMetricsAlarmTimer() {
        Intent intent = new Intent(ALARM_TIMER_METRIC_EXPIRED); // create intent with this action
        mMetricIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        long firstWake = System.currentTimeMillis() + METRIC_INTERVAL;
        metricAlarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstWake, METRIC_INTERVAL, mMetricIntent);
    }

    /**
     * cancel any pending timer for metrics logging.
     */
    private void cancelMetricsAlarmTimer() {
        metricAlarm.cancel(mMetricIntent);
        if (mMetricIntent != null) {
            mMetricIntent.cancel();
            mMetricIntent = null;
        }
    }

    /**
     * set up a pending intent to alarm manager to fire upon 15 minute timer expired.
     * @param delay
     */
    private void scheduleAlarmTimer(long delay) {
        // first, cancel all the pending alarm
        cancelAlarmTimer();

        long nowtime = System.currentTimeMillis();
        // schedule a new alarm with new pending intent
        Intent timeoutIntent = new Intent(ALARM_TIMER_LOCATION_EXPIRED); // create intent with this action
        timeoutIntent.putExtra(ALARM_TIMER_SET_TIME, nowtime );
        if(LOG_DEBUG) Log.d(TAG, "scheduleAlarmTimer with Time :" + nowtime);
        setPendingIntent(timeoutIntent);

        mAlarmMan.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+delay, mTimerExpiredIntent);
        if(LOG_DEBUG) Log.d(TAG, "scheduleAlarmTimer : " + delay/60000 + " min later from : " + nowtime);
    }

    /**
     * message handler looper to handle all the msg sent to location manager.
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

    private interface ErrTxt {
        String PREFIX_PC = "processMessage(): ";
        String PREFIX_0 = "celltower changed... ";
        String PREFIX_1 = PREFIX_0+" first time app is running. ";
        String PREFIX_2 = PREFIX_0+" listening location updates: ";
        String PREFIX_3 = PREFIX_0+" new cell..we are moving..";
    }

    /**
     * the main message process loop.
     * @param msg
     */
    private void processMessage(android.os.Message msg) {

        switch (msg.what) {
        case Msg.NULL:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : MSG_NULL");
            break;
        case Msg.CELLTOWER_CHANGED:   // from tel mon when cell tower changed
            metricsLogger.logMsgCellTowerChanged(mTelMon.getValueJSONString());
            handleCelltowerChange();
            mState = LSManState.CELLCHANGED;
            break;
        case Msg.START_MONITORING:
            if (msg.obj != null) {  // when timer expired, always know timer start time
                mNewCellStartTime = ((Long)msg.obj).longValue();
            } else {
                mNewCellStartTime = System.currentTimeMillis();
            }
            mBeacon.startBeaconScan(null, mHandler); // scan and store beacon's around POI.
            if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_2+"...timer expired, do a wifi scan first, then get a fix after warmup...timer started at : " + mNewCellStartTime);
            break;
        case BEACONSCAN_RESULT:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : BEACONSCAN_RESULT...fresh wifi scan now stored in beacon module...request google fix or direct capture");
            mBeacon.removeCallerHandler(mHandler);  // done with beacon scan, no wifi scan update anymore.

            if (mLocMon.isNetworkProviderEnabled()) {
                if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_2+"...request a google fix..timer started at : " + mNewCellStartTime);
                requestLocationFix();   // request location fix, flow conti after fix comes in.
            } else {
                if(LOG_DEBUG) Log.d(TAG, "Network location provider not enabled, fake a invalid google fix...timer started at : " + mNewCellStartTime);
                mHandler.sendEmptyMessage(LOCATION_UPDATE); // fake a google fix
            }
            break;

        case LOCATION_UPDATE:
            // onLocationChanged event from location provider, mean a new location candidate, or a fake one comes in if user not enabled the provider.
            // need to filter out two loc fix back-by-back. Two entry in db should be more than 15 min.
            if(mCurLocTuple != null && (System.currentTimeMillis() - mCurLocTuple.getEndTime() < 10*DateUtils.MINUTE_IN_MILLIS)) {
                if(LOG_DEBUG) Log.d(TAG, "processMessage() : LOCATION_UPDATE...do nothing on too close location fixes");
            } else {
                discoverLocation();  // next step should always be stop monitoring
                // updateDiscoveredLocationPoiBeacon(null);   //  // XXX Please, do not update poi wifi from discovery
                mState = LSManState.LOCATONCAPTURED;  // done with location fix.
                mHandler.sendEmptyMessage(Msg.STOP_MONITORING);
            }
            //mLSApp.mGraph.addLastEdge();          // add last edge.
            break;
        case Msg.START_SELF_HEALING:
            // Alarm timer for location consolidate expired, normally at 4-5 am....perform consolidation.
            if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_PC+": MSG_START_SELF_HEALING...submit fix task!");
            // mStore.consolidateLocations(true);  // IKCTXTAW-170, disable daily healing job as we heal in real-time upon each location.
            break;
        case Msg.DETECTION_POIREFRESH:
            if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_PC+": DETECTION_POIREFRESH...update poi wifi ?");
            //handleDetectionPoiRefresh();
            break;
        case Msg.STOP_MONITORING:
            if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_PC+": MSG_STOP_MONITORING");
            setPendingIntent(null);  // nullify mTimerExpiredIntent so bouncing cell map can be cleared.
            stopLocationMonitor();   // stop after getting location fix.
            mHandler.removeMessages(Msg.START_MONITORING);
            break;

        case Msg.METRICS_TIMER_EXPIRED:
            metricsLogger.checkin();
            break;
        case Msg.METRICS_WIFI_SCAN_RESULTS_AVAILABLE:
            metricsLogger.logWifiScanResult();
            break;

        default:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : unknown msg::" + msg.what);
            break;
        }
    }

    /**
     * cell tower changed event comes from telmon, means user is moving, start location discovery and detection.
     * Update previous location times, and schedule 15 min timer to track the new location.
     * If the cell tower belongs to any meaningful location, start active detection logic.
     */
    private void handleCelltowerChange() {
        if (mCurLocTuple == null) {
            // cell info not available, means the first time thru
            // restore myself to the last location status, not from real measurement....and start from there.
            mCurLocTuple = mStore.getLastLocation();
            if (mCurLocTuple != null) {
                if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_1+", calibrate to last location: " + mCurLocTuple.toString());
                // because process got killed and restarted, (reschedule crashed service to run in 5000ms)
                if( System.currentTimeMillis() - mCurLocTuple.getEndTime() < SystemClock.elapsedRealtime() ) { // canot exceed elapsetime after boot
                    updateCurrentLocationDuration();
                }
            } else {  // if I do not have history, just get one immediately, populate
                if (null == getPendingIntent()) {
                    scheduleAlarmTimer(3000);    // delay only 3 seconds the first time, request location update when timer expired.
                    // mBeacon.setWifi(true);    // wifi on upon power up, first time measurement, // disable wifi in discovery for stable 5.
                    if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_1+", start monitoring immediately..");
                } else {
                    if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_1+"...in the process of monitoring..");
                }
            }
        }

        if (mCurLocTuple != null) {  // ok, I am calibrated to either last location, or to a real measurement.
            // not first time thru, update last entry's ticks only if timer is not pending, otherwise, double counting.
            if (null == getPendingIntent() &&  (int)(mCurLocTuple.getLat()) != 0 ) {  // avoid update old loc when could not get lat/lng
                updateCurrentLocationDuration();
            }
            if (isNewCellTower(mTelMon.getValueJSONString())) {
                if (isLatestLocationWithinPoi()) {
                    cancelAlarmTimer();
                    if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_3+" :: latest location within poi, stop timer, do nothing...");
                } else if (mTelMon.getCurrentLocationValue().mCellId != -1 ||
                           (mTelMon.getCurrentLocationValue().mCellId == -1 && null == getPendingIntent())) {
                    if (null == getPendingIntent() && isUserOptedIn() && (int)(mCurLocTuple.getLat()) != 0) {  // check in after left a POI or moved if not in any poi.
                        if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_3 + "moving to new cell and update current location end time and do checkin.");
                        mStore.checkinUponLeavingLocation(mCurLocTuple);
                        //mLSApp.mGraph.mCurLat = mCurLat; mLSApp.mGraph.mCurLng = mCurLgt; mLSApp.mGraph.displayDestinations();
                        mCurLocTuple.setLat(0);
                        mCurLocTuple.setLgt(0);   // timer will expire, and we will get a new lat lng for the new current location
                    }
                    // valid cell, or invalid cell but no pending intent,
                    scheduleAlarmTimer(TIMER_HEARTBEAT_INTERVAL);  // set 15 min timer...
                    if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_3+" :: schedule the 15 timer started to discover location....");
                } else {
                    if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_3+" :: NO timer because -1 cell id with pending intent");
                }
                // mDetection.startDetection(); XXX no need this, cell tower change always sent to detection.
            } else {
                // if cell not changed, do nothing
                if(LOG_DEBUG) Log.d(TAG, "cell change not new..cell bouncing...filtering out and do nothing!");
            }
        }
    }

    /**
     * We have another filter here to filter out bouncing cells...not used for now.
     * @param curcell current cell tower key value string
     * @return true, always think cell change event is valid in this module.
     */
    private boolean isNewCellTower(String curcell) {
        // Everything coming out of bouncing cell set need to be handled.
        Set<String> cellset = Utils.convertStringToSet(mCurLocTuple.getCellJsonValue());  // create a set every cell push, expensive ?
        if (cellset.contains(curcell)) {
            if(LOG_DEBUG) Log.d(TAG, "hasCellTowerChanged :: No : cur cell :" + mCurLocTuple.getCellJsonValue() + ":: new cell :" + curcell);
            return false;
        } else {
            if(LOG_DEBUG) Log.d(TAG, "hasCellTowerChanged :: Yes :start tracking : cur cell :" + mCurLocTuple.getCellJsonValue() + ":: new cell :" + curcell);
            return true;
        }
    }

    /**
     * request location fix immediately upon 15 timer expired when we need to discover the location.
     * set the state to fix pending.
     */
    private void requestLocationFix() {
        // start network loc listening and schedule check every 10 interval
        if (! mLocMon.isNetworkProviderEnabled() && ! mLocMon.isGpsProviderEnabled()) {
            if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_2+"...network provider not enabled....do nothing!!!");
        } else if (!isDataConnectionGood()) {
            if(LOG_DEBUG) Log.d(TAG, ErrTxt.PREFIX_2+"....timer expired, data connection is bad...do nothing!!!");
            mHandler.sendEmptyMessage(Msg.STOP_MONITORING);  // send a stop monitoring message to stop.
        } else {
            if(LOG_DEBUG) Log.d(TAG, " requestLocationFix : timer expired, timer started at : " + mNewCellStartTime);
            startLocationMonitor(0, 0);  // need location fix now...waiting for location update
            mState = LSManState.TIMEREXPIRED_WAITING4FIX;
        }
    }


    /**
     * when entering this func, network tracking should be on
     * get the current location fix, figure out whether within any POIs, and store in the location db
     * @return next step always be STOP_MONITORING.
     */
    private Message discoverLocation() {
        Message msg = mHandler.obtainMessage();
        msg.what = Msg.STOP_MONITORING;    // init to be stop network tracking

        String bouncingcelljsons = Utils.convertSetToString(mTelMon.getBouncingCells());
        String wifissid = mBeacon.getLastWifScanSsid(true);

        if (mLocMon.isNetworkProviderEnabled()) {  // get locatino with google fix lat/lng/accuracy
            Location curloc = mLocMon.getCurrentLocation();
            if (curloc == null) {
                if(LOG_DEBUG) Log.d(TAG, "discoverLocation : race condition...onLocationChange from locmon in the queue before stop monitoring remove the listener");
                return msg;
            }
            if (isLatestLocationWithinPoi()) {
                if(LOG_DEBUG) Log.d(TAG, "discoverLocation : current Location within POI, DRY. no capture location again!");
                return msg;
            }

            double lat = (int)(curloc.getLatitude()*1E6)/1E6;   // 1E6 is good enough
            double lgt = (int)(curloc.getLongitude()*1E6)/1E6;
            long accuracy = curloc.hasAccuracy() ? (long)curloc.getAccuracy() : 0;  // get the accuracy
            metricsLogger.logDiscoveryReceiveLocationUpdate(lat, lgt, (int) accuracy);

            // slow path...will change *CurPOI* inside this function, order matters!
            mCurPOI = mDetection.checkDiscoveredLocationPoi(lat, lgt, accuracy, mCurPOI, mCurLocTuple==null);

            Tuple t = mStore.storeDiscoveredLocation(lat, lgt, accuracy, mNewCellStartTime, mCurPOI, bouncingcelljsons, wifissid);
            mCurLocTuple = mStore.getLastLocation();  // now remember the last top location in loctime.
            if (mCurLocTuple != null) {
                mCurLocTuple.setLat(t.getLat());
                mCurLocTuple.setLgt(t.getLgt());
                mCurLocTuple.setAccuracy(t.getAccuracy());
                mCurLocTuple.setAccuName(t.getAccuName());
                broadcastDiscoveredLocation(mCurLocTuple.get_id(), t.getLat(), t.getLgt(), t.getAccuracy(), t.getAccuName(), bouncingcelljsons, mCurPOI);
            }
            if(LOG_DEBUG) Log.d(TAG, "discoverLocation :: LatLng based: match POI:" + mCurPOI + " : " + t.toString()); // /loctime/2
            // mStore.checkinUponLeavingLocation(mCurLocTuple);  // phoenix testing
        } else { // just get location with wifi
            Uri entry = mStore.storeWifiLocation(mNewCellStartTime, bouncingcelljsons, wifissid);
            if(mDetection.getCurrentPoiTuple() != null) {
                mCurPOI = mDetection.getCurrentPoiTuple().getPoiName();
            } else {
                mCurPOI = "";  // empty string
            }
            mCurLocTuple = mStore.getLastLocation();  // now remember the last top location in loctime.
            if (mCurLocTuple != null) {
                broadcastDiscoveredLocation(mCurLocTuple.get_id(), 0, 0, 0, "", bouncingcelljsons, mCurPOI);
                if(LOG_DEBUG) Log.d(TAG, "discoverLocation :: Wifi based: match POI:" + mCurPOI + " : " + mCurLocTuple.toString()); // /loctime/2
            }
        }
        return msg;  // upon here, msg is stop monitoring.
    }

    /**
     * update the duration of current location upon moving
     */
    private void updateCurrentLocationDuration() {
        //if (mCurLocTuple != null && (int)(mCurLocTuple.getLat()) != 0) { // to avoid stuck with old loc when loc fix not available after timer exp.
        if (mCurLocTuple != null ) { // to avoid stuck with old loc when loc fix not available after timer exp.
            long nowtime = System.currentTimeMillis();
            mCurLocTuple.setEndTime(nowtime);
            mCurLocTuple.setCount((nowtime-mCurLocTuple.getStartTime())/DateUtils.MINUTE_IN_MILLIS);
            mStore.updateCurrentLocationDuration(mCurLocTuple.getStartTime(), nowtime);
        }
    }

    /**
     * check whether the current location, the latest entry in loctime table, within the detection current POI.
     * if matches, we do not need to re-capture the location, as it is already in the database. just update end time.
     * return true if lastest entry in loctime matches detection poi, false otherwise.
     */
    private boolean isLatestLocationWithinPoi() {
        if (mCurLocTuple != null && mDetection.getCurrentPoiTuple() != null) {
            if(LOG_DEBUG) Log.d(TAG, "isLatestLocationWithinPoi: DetPoi:" + mDetection.getCurrentPoiTuple().getPoiName() + " CurLocPoi:" + mCurLocTuple.getPoiTag());
            if (mDetection.getCurrentPoiTuple().getPoiName().equals(mCurLocTuple.getPoiTag())) {  // 3 references in one statement.
                return true;
            }
        }
        return false;
    }

    /**
     * grab the wifi mac addr around the location and store them.
     * @param matchedpoi  meaningful location name.
     */
    protected void updateDiscoveredLocationPoiBeacon(String matchedpoi) {
        String scaned_wifissid = mBeacon.getLastWifScanSsid(true);
        if(LOG_DEBUG) Log.d(TAG, "updateDiscoveredLocationPoiBeacon:  poi:" + matchedpoi + ": Wifis:" + scaned_wifissid);
        mDetection.updatePoiBeaconAfterDiscovery(matchedpoi, scaned_wifissid);
    }

    /**
     * Broadcast the newly discovered location...Inference Engine will handle it.
     * Do not inform Inference engine if user do not opted in
     */
    public void broadcastDiscoveredLocation(long id, double lat, double lgt, long accu, String accuname, String celljsons, String poi) {
        if(!isUserOptedIn()) {
            return;
        }

        Intent i = new Intent(VSM_LOCATION_CHANGE);
        i.putExtra("status", "location");

        Bundle b = new Bundle();
        b.putLong("ID", id);
        b.putDouble("Lat", lat);
        b.putDouble("Lgt", lgt);
        b.putLong("Accuracy", accu);
        b.putString("Accuname", accuname);
        b.putString("CellJsonValue", celljsons);
        b.putString("poi", poi);
        i.putExtra("locdata", b);

        if(LOG_DEBUG) Log.d(TAG, "broadcastDiscoveredLocation :: " + b.toString());
        sendBroadcast(i, PERMISSION);
    }

    /**
     * called from detection upon POI refresh and location manager does not know the POI.
     * check to see current location matches POI, if so, update wifi and bouncing cells.
     * Not used for now...reserved for future extension.
     */
    private void handleDetectionPoiRefresh() {
        if (mCurPOI == null && mCurLocTuple != null) {
            mCurPOI = mDetection.checkDiscoveredLocationPoi(mCurLocTuple.getLat(), mCurLocTuple.getLgt(), mCurLocTuple.getAccuracy(), mCurPOI, false);
            if(LOG_DEBUG) Log.d(TAG, "handleDetectionPoiRefresh :: current location matchs, start beacon scan upon match POI:" + mCurPOI);
            if (mCurPOI != null) {
                mBeacon.startBeaconScan(mCurPOI, mHandler); // scan and store beacon's around POI.
            }
        }
    }

    /**
     * Reverse Geocode, note that this is a sync call but very time consuming...should be designed as async call!
     * @param lat
     * @param lgt
     * @return String Address
     */
    public String getLocationAddress(double lat, double lgt) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Address> addresses = mGeoCoder.getFromLocation(lat, lgt, 1);
            if (addresses != null && addresses.size() > 0) {
                for (int i=0; i<addresses.get(0).getMaxAddressLineIndex(); i++) {
                    sb.append(addresses.get(0).getAddressLine(i) + " ");
                }
            }
        } catch (IOException e) {
            LSAppLog.e(TAG, "getLocationAddress() : Lat=" + lat + " Lgt=" + lgt + " Exception :: " + e.toString());
        }

        if(LOG_DEBUG) Log.d(TAG, "getLocationAddress() : Lat=" + lat + " Lgt=" + lgt + " return addr :: " + sb.toString());
        return sb.toString();
    }

    /**
     * the timer expired event handler for location 15min timer and self healing timer
     */
    final static private class TimerEventReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final Handler mHandler;
        private MetricsLogger metricLogger;
        private boolean mStarted = false;

        TimerEventReceiver(final Context ctx, final Handler handler, MetricsLogger mLogger) {
            this.mContext = ctx;
            this.mHandler = handler;
            this.metricLogger = mLogger;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : onReceiver : Got timer expired Event :" + action);
            Message msg = mHandler.obtainMessage();

            if (ALARM_TIMER_LOCATION_EXPIRED.equals(action)) {
                metricLogger.logAlarmTimerLocationExpired();
                // 15 min timer expired. get location fix now!
                msg.what = Msg.START_MONITORING;
                msg.obj =  Long.valueOf(intent.getLongExtra(ALARM_TIMER_SET_TIME, System.currentTimeMillis()));
                if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : discovery timer(15min) expired since :" + msg.obj );
            } else if (ALARM_TIMER_SELF_HEALING_EXPIRED.equals(action)) {
                msg.what = Msg.START_SELF_HEALING;
                if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : ALARM_TIMER_SELF_HEALING_EXPIRED : send MSG_START_SELF_HEALING");
            } else if (ALARM_TIMER_ALIVE_EXPIRED.equals(action)) { // 2 housr alive timer expired
                msg.what = Msg.HEARTBEAT_TIMER_EXPIRED;
                if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : ALARM_TIMER_ALIVE_EXPIRED : send MSG_HEARTBEAT_TIMER");
            } else if (ALARM_TIMER_METRIC_EXPIRED.equals(action)) {
                msg.what = Msg.METRICS_TIMER_EXPIRED;
                if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : ALARM_TIMER_METRIC_EXPIRED : send MSG_METRIC_TIMER_EXPIRED");
            }
            mHandler.sendMessage(msg);
        }

        synchronized final void start() {
            if (!mStarted) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ALARM_TIMER_LOCATION_EXPIRED);
                filter.addAction(ALARM_TIMER_SELF_HEALING_EXPIRED);
                filter.addAction(ALARM_TIMER_METRIC_EXPIRED);
                mContext.registerReceiver(this, filter);
                mStarted = true;
                if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : started and registered");
            }
        }

        synchronized final void stop() {
            if (mStarted) {
                mContext.unregisterReceiver(this);
                mStarted = false;
                if(LOG_DEBUG) Log.d(TAG, "TimerEventReceiver : stoped and unregistered");
            }
        }
    }

    /**
     * Request location update upon discoverying a location
     */
    private boolean startLocationMonitor(long minTime, float minDistance) {
        if(LOG_DEBUG) Log.d(TAG, "LSMan Start listening location update...");
        // mBeacon.setWifi(true);  // ON wifi upon start location mon, // disable wifi in discovery for stable 5.
        return mLocMon.startLocationUpdate(minTime, minDistance, null);
    }

    /**
     * stop location update after location discovered.
     */
    private void stopLocationMonitor() {
        if(LOG_DEBUG) Log.d(TAG, "LSMan Stop listening location update...");
        //mBeacon.setWifi(false);  // no play with wifi in discovery
        mLocMon.stopLocationUpdate();
    }

    /**
     * we need to notify vsm we are up and running on start.
     */
    private void notifyVSMOnStartCommand() {
        String poi = null;
        if (mDetection != null && mDetection.getCurrentPoiTuple() != null) {
            poi = mDetection.getCurrentPoiTuple().getPoiName();
            mLSApp.getVsmProxy().sendVSMLocationUpdate(poi);
        } else if(mDetection != null) {
            mDetection.setNotifyVSMOnRestart(false);  // reset the notify flag, request re-notify
            if(LOG_DEBUG) Log.d(TAG, "notifyVSMOnStartCommand: LSMan requests notify detection state.");
        }
    }

    /**
     * check whether we have good data connectivity, drop location update when data connectivity is bad as
     * google will give last known location when data connectivity is not good
     * Data connection: either Telephony data connection or Wifi Connection.
     * @return true if data connectivity good, false else
     */
    public boolean isDataConnectionGood() {
        return ( mTelMon.isDataConnectionGood() || mBeacon.isWifiConnected());
    }

    /**
     * return reference to detection module.
     * @return
     */
    public LocationDetection getDetection() {
        return mDetection;
    }

    /**
     * return the phone's device Id
     * @return string of phone device id
     */
    public String getPhoneDeviceId() {
        return mTelMon.getPhoneDeviceId();
    }

    public MetricsLogger getMetricsLogger() {
        return metricsLogger;
    }

    /**
     * getter for all the package protected dependents
     */
    public TelephonyMonitor getTelephonyMonitor() {
        return mTelMon;
    }
    public BeaconSensors getBeaconSensors() {
        return mBeacon;
    }

    /**
     * holding wakelock
     * used before wifi scan to avoid empty scan issue.
     * @param duration in millisecond
     */
    private PowerManager.WakeLock sCpuWakeLock = null;
    public void holdWakeLock(long timeout) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            sCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            sCpuWakeLock.acquire(timeout);  // acquire the wakelock for duration
        }
    }

    /**
     * releasing wakelock
     * called whenever scan result is available.
     */
    public void releaseWakeLock() {
        try{
            if (sCpuWakeLock != null && sCpuWakeLock.isHeld()) {
                sCpuWakeLock.release();
                sCpuWakeLock = null;
            }
        }catch(Exception e){
            // This is bad, but it is a framework issue. need to catch  java.lang.RuntimeException: WakeLock under-locked LSAPP_LSMan
            // wakelock with a timeout might causing the problem
            LSAppLog.e(TAG, e.toString());
        }
    }
}
