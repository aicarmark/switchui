/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/06/21   IKCTXTAW-480    Stop monitoring on main thread
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add NetworkConditionDeterminer & refactor
 */

package com.motorola.contextual.smartnetwork;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.motorola.android.wrapper.PowerManagerWrapper;
import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.TelephonyManagerWrapper;
import com.motorola.contextual.smartnetwork.db.Table;
import com.motorola.contextual.smartnetwork.db.Tuple;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTable;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTuple;
import com.motorola.contextual.smartnetwork.db.table.ServiceStateTable;
import com.motorola.contextual.smartnetwork.db.table.ServiceStateTuple;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTable;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTuple;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;

public class NetworkMonitorService extends Service {
    private static final String TAG = NetworkMonitorService.class.getSimpleName();
    private static final boolean LOGD = true;
    private static final String BACKGROUND_THREAD = "BgNetworkMonitorServiceThread";

    public static final String INTENT_START_NETWORK_MONITOR =
        "com.motorola.contextual.smartnetwork.INTENT_START_NETWORK_MONITOR";
    public static final String INTENT_STOP_NETWORK_MONITOR =
        "com.motorola.contextual.smartnetwork.INTENT_STOP_NETWORK_MONITOR";

    private final static String NETWORK_TYPE_PREFIX = "NETWORK_TYPE_";

    private Handler mBgHandler;
    private final NetworkStateListener mNetworkListener = new NetworkStateListener();
    private ScreenStateReceiver mScreenReceiver;

    private final NetworkSession mSession = new NetworkSession();
    private boolean mMonitoring = false;

    private static final int UNKNOWN = -9999;
    private int mPrevServiceState = UNKNOWN;
    private String mPrevSignalType = null;
    private int mPrevSignalLevel = UNKNOWN;
    private boolean mScreenOn = false;

    public static String formatNetworkType(String networkType) {
        String newType = networkType;
        if (newType != null) {
            newType = newType.replaceFirst(NETWORK_TYPE_PREFIX, "");
        }
        return newType;
    }

    /**
     * Listens to data connection state, service state, and signal strength changes
     * and logs the changes to the SmartNetwork database
     */
    private class NetworkStateListener extends PhoneStateListener {
        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (mBgHandler != null && mSession.mLocationId > 0 && mSession.mSessionId > 0) {
                String dataState = null;
                switch(state) {
                case TelephonyManager.DATA_DISCONNECTED:
                    dataState = NetworkSession.DATA_DISCONNECTED;
                    break;
                case TelephonyManager.DATA_CONNECTING:
                    dataState = NetworkSession.DATA_CONNECTING;
                    break;
                case TelephonyManager.DATA_CONNECTED:
                    dataState = NetworkSession.DATA_CONNECTED;
                    break;
                case TelephonyManager.DATA_SUSPENDED:
                    dataState = NetworkSession.DATA_SUSPENDED;
                    break;
                default:
                    // unknown, or possibly random value from unit test
                    dataState = String.valueOf(state);
                }
                String networkTypeName = TelephonyManagerWrapper.getNetworkTypeName(networkType);
                if (networkTypeName == null) {
                    // unknown type, or possibly random value from unit test
                    networkTypeName = String.valueOf(networkType);
                }
                // remove common prefix
                networkTypeName = formatNetworkType(networkTypeName);

                final DataConnectionStateTuple tuple = new DataConnectionStateTuple();
                tuple.put(DataConnectionStateTuple.COL_FK_MONITOR_SESSION, mSession.mSessionId);
                tuple.put(DataConnectionStateTuple.COL_STATE, dataState);
                tuple.put(DataConnectionStateTuple.COL_NETWORK_TYPE, networkTypeName);
                tuple.put(DataConnectionStateTuple.COL_TIMESTAMP, System.currentTimeMillis());
                // insert on background thread
                mBgHandler.post(new Runnable() {
                    public void run() {
                        long rowId = insertTuple(new DataConnectionStateTable(), tuple);
                        if (rowId <= 0) {
                            Log.e(TAG, "Unable to save data connection state change.");
                        }
                    }
                });
            } else {
                Log.e(TAG, "Unable to handle data connection state change.");
            }
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (mBgHandler != null && mSession.mLocationId > 0 && mSession.mSessionId > 0
                    && serviceState != null) {
                if  (mPrevServiceState != serviceState.getState()) {
                    /* only log changes if service state changed, not when some other
                     * variable we don't care about changes
                     */
                    final ServiceStateTuple tuple = new ServiceStateTuple();
                    tuple.put(ServiceStateTuple.COL_FK_MONITOR_SESSION, mSession.mSessionId);
                    String serviceStateName =
                        TelephonyManagerWrapper.getServiceStateName(serviceState.getState());
                    if (serviceStateName == null) {
                        serviceStateName = String.valueOf(serviceState.getState());
                    }
                    tuple.put(ServiceStateTuple.COL_STATE, serviceStateName);
                    tuple.put(ServiceStateTuple.COL_TIMESTAMP, System.currentTimeMillis());
                    // insert on background thread
                    mBgHandler.post(new Runnable() {
                        public void run() {
                            long rowId = insertTuple(new ServiceStateTable(), tuple);
                            if (rowId <= 0) {
                                Log.e(TAG, "Unable to save service state change.");
                            }
                        }
                    });
                    mPrevServiceState = serviceState.getState();
                }
            } else {
                Log.e(TAG, "Unable to handle service state changed.");
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            /* ignore signal strength changes coming in while screen is off.
             * BP isn't supposed to wake up AP with signal strength changes while screen is off.
             * There may be random wake ups while screen is off but ignore these.
             */
            if (mScreenOn) {
                if (mBgHandler != null && mSession.mLocationId > 0 && mSession.mSessionId > 0
                        && signalStrength != null) {
                    String signalType;
                    int signalLevel;
                    // TODO: is there any other signal types? how do we tell from the API?
                    if (signalStrength.isGsm()) {
                        signalType = NetworkSession.SIGNAL_TYPE_GSM;
                        signalLevel = signalStrength.getGsmSignalStrength();
                    } else {
                        signalType = NetworkSession.SIGNAL_TYPE_CDMA;
                        signalLevel = signalStrength.getCdmaDbm();
                    }
                    if (!signalType.equals(mPrevSignalType) || signalLevel != mPrevSignalLevel) {
                        // only log if either signal type or signal level changed
                        final SignalStrengthTuple tuple = new SignalStrengthTuple();
                        tuple.put(SignalStrengthTuple.COL_FK_MONITOR_SESSION, mSession.mSessionId);
                        tuple.put(SignalStrengthTuple.COL_SIGNAL_TYPE, signalType);
                        tuple.put(SignalStrengthTuple.COL_SIGNAL_LEVEL, signalLevel);
                        tuple.put(SignalStrengthTuple.COL_TIMESTAMP, System.currentTimeMillis());
                        // insert on background thread
                        mBgHandler.post(new Runnable() {
                            public void run() {
                                long rowId = insertTuple(new SignalStrengthTable(), tuple);
                                if (rowId <= 0) {
                                    Log.e(TAG, "Unable to save signal strength change.");
                                }
                            }
                        });
                        mPrevSignalType = signalType;
                        mPrevSignalLevel = signalLevel;
                    }
                } else {
                    Log.e(TAG, "Unable to handle signal strength change.");
                }
            } else if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Signal strength change came in while screen is off.");
            }
        }
    }

    /**
     * Listens to screen state changes and logs them in the signal strength table.
     * This is needed because signal strength changes only come in while screen is on.
     */
    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* BP filters out signal strength change while screen is off. This may
             * skew accumulated time for each signal strength, so we have to
             * mark when screen is turned off.
             */
            if (intent != null && mSession.mLocationId > 0 && mSession.mSessionId > 0) {
                String action = intent.getAction();
                String screenState = null;
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    screenState = NetworkSession.SIGNAL_SCREEN_OFF;
                    mScreenOn = false;
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    screenState = NetworkSession.SIGNAL_SCREEN_ON;
                    mScreenOn = true;
                }

                if (screenState != null) {
                    final SignalStrengthTuple tuple = new SignalStrengthTuple();
                    tuple.put(SignalStrengthTuple.COL_FK_MONITOR_SESSION, mSession.mSessionId);
                    tuple.put(SignalStrengthTuple.COL_SIGNAL_TYPE, screenState);
                    tuple.put(SignalStrengthTuple.COL_SIGNAL_LEVEL,
                              NetworkSession.SIGNAL_LEVEL_UNKNOWN);
                    tuple.put(SignalStrengthTuple.COL_TIMESTAMP, System.currentTimeMillis());
                    mBgHandler.post(new Runnable() {
                        public void run() {
                            long rowId = insertTuple(new SignalStrengthTable(), tuple);
                            if (rowId <= 0) {
                                Log.e(TAG, "Unable to save screen state change.");
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * @param networkCondition Network condition from TopLocationTable
     * @return true if network monitoring is needed, false otherwise
     */
    public static boolean isMonitoringNeeded(String networkCondition) {
        boolean monitor = false;
        if (TopLocationTable.NETWORK_CONDITION_VERIFY.equals(networkCondition)
                || TopLocationTable.NETWORK_CONDITION_UNKNOWN.equals(networkCondition)) {
            monitor = true;
        }
        return monitor;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LOGD && Log.isLoggable(TAG,  Log.DEBUG)) {
            Log.d(TAG, "onCreate()");
        }

        HandlerThread bgThread = new HandlerThread(BACKGROUND_THREAD,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        bgThread.start();
        Looper looper = bgThread.getLooper();
        if (looper != null) {
            mBgHandler = new Handler(looper);
        } else {
            Log.e(TAG, "Unable to create background handler.");
        }
    }

    @Override
    public void onDestroy() {
        stopMonitoring();
        if (mBgHandler != null) {
            mBgHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (mBgHandler == null) {
            stopSelf();
        } else {
            if (intent != null) {
                String action = intent.getAction();
                if (INTENT_START_NETWORK_MONITOR.equals(action)) {
                    mBgHandler.post(new Runnable() {
                        public void run() {
                            startMonitoring(intent);
                        }
                    });
                } else if (INTENT_STOP_NETWORK_MONITOR.equals(action)) {
                    onExitLocation();
                } else {
                    Log.w(TAG, "Unknown intent received: " + action);
                    stopSelf();
                }
            } else {
                stopSelf();
            }
        }
        // if the service crashes, don't restart it
        return START_NOT_STICKY;
    }

    private void startMonitoring(Intent intent) {
        long locationId = intent.getLongExtra(PoiHandler.EXTRA_LOCATION, 0);

        if (!mMonitoring && locationId > 0) {
            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Start monitoring: " + locationId);
            }

            // update session variables
            mSession.clear();
            mSession.mLocationId = locationId;
            mSession.mStartTime = System.currentTimeMillis();
            mSession.mEndTime = 0;
            mSession.mTimeSpent = intent.getLongExtra(PoiHandler.EXTRA_TIME_SPENT, 0);
            mSession.mRank = intent.getIntExtra(PoiHandler.EXTRA_RANK, 0);;
            mSession.mRankUpdated = intent.getLongExtra(PoiHandler.EXTRA_RANK_UPDATED, 0);

            // log session start and generate sessionId
            mSession.logSessionStart(this);

            if (mSession.mSessionId > 0) {
                TelephonyManagerWrapper telephonyManager = (TelephonyManagerWrapper)
                        SystemWrapper.getSystemService(this, Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    // get current screen state first
                    PowerManagerWrapper pm = (PowerManagerWrapper) SystemWrapper.getSystemService(this,
                                             Context.POWER_SERVICE);
                    mScreenOn = pm.isScreenOn();

                    mScreenReceiver = new ScreenStateReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_SCREEN_ON);
                    filter.addAction(Intent.ACTION_SCREEN_OFF);
                    registerReceiver(mScreenReceiver, filter);

                    telephonyManager.listen(mNetworkListener,
                                            PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                                            PhoneStateListener.LISTEN_SERVICE_STATE |
                                            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

                    mMonitoring = true;
                } else {
                    Log.e(TAG, "Unable to get TelephonyManager.");
                    stopSelf();
                }
            } else {
                Log.e(TAG, "Unable to log session start.");
                stopSelf();
            }
        } else if (mMonitoring && locationId > 0) {
            Log.e(TAG, "Already monitoring " + mSession.mLocationId
                  + " but requested to monitor " + locationId);
            // if we're already monitoring the same location, nothing needs to be done
            if (mSession.mLocationId != locationId) {
                /* if we're already monitoring a location and a request to monitor
                 * a different location comes in without leaving the old location,
                 * then something went wrong so stop monitoring and log an error.
                 */
                Log.e(TAG, "Request to monitor a new location came in without"
                      + " leaving old location.");
                onExitLocation();
            }
        } else {
            Log.e(TAG, "Invalid location.");
            stopSelf();
        }
    }

    private void stopMonitoring() {
        if (mMonitoring) {
            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Stop monitoring: " + mSession.mLocationId);
            }
            TelephonyManagerWrapper telephonyManager = (TelephonyManagerWrapper)
                    SystemWrapper.getSystemService(this, Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(mNetworkListener, PhoneStateListener.LISTEN_NONE);
            } else {
                Log.e(TAG, "Unable to get TelephonyManager.");
            }
            if (mScreenReceiver != null) {
                unregisterReceiver(mScreenReceiver);
                mScreenReceiver = null;
            }
            mMonitoring = false;
        }
    }

    private void onExitLocation() {
        if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Exiting location: " + mSession.mLocationId);
        }
        if (mMonitoring && mSession.mLocationId > 0 && mSession.mSessionId > 0) {
            // stop monitoring and log session end
            stopMonitoring();
            mSession.mEndTime = System.currentTimeMillis();
            mSession.logSessionEnd(this);

            // make a copy before clearing
            final NetworkSession session = mSession.clone();
            final Context context = this;
            mSession.clear();

            // checkin on background thread
            mBgHandler.post(new Runnable() {
                public void run() {
                    session.checkin(context);
                    // checkin before stopping
                    stopSelf();
                }
            });
        } else {
            Log.e(TAG, "Exiting location but never entered one.");
            stopSelf();
        }
    }

    /**
     * Helper function to insert on background thread
     * @param table Table to insert into
     * @param tuple Tuple to insert
     * @return the row id of the newly inserted Tuple, -1 on error
     */
    private <T extends Tuple> long insertTuple(Table<T> table, T tuple) {
        return table.insert(this, tuple);
    }
}
