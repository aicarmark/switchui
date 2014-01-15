/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2011/11/01   IKCTXTAW-359    Initial version
 * w04917 (Brian Lee)          2012/02/07   IKCTXTAW-359    New configurable settings and caching.
 *                                                          Added daily stats collection.
 */

package com.motorola.datacollection.perfstats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;

import com.motorola.kpi.perfstats.Logger;
import com.motorola.kpi.perfstats.PerformanceStats;

public class PerformanceStatsService extends Service {

    private static final String TAG = "PerfStatsService";

    public static final boolean LOGD = Logger.LOGD || false;

    private static final int INITIAL_CAPACITY = 20;
    private final Map<String, PerformanceStatsHandler> mSyncMap = Collections.synchronizedMap(
                new HashMap<String, PerformanceStatsHandler>(INITIAL_CAPACITY));

    private static final int MSG_PERF_STATS_TIMEOUT = 1;
    private static final long PERF_STATS_TIMEOUT_MS = 30 * DateUtils.SECOND_IN_MILLIS;

    private Handler mBackgroundHandler;
    private Thread mBackgroundThread;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBackgroundThread  = new Thread() {
            public void run() {
                Looper.prepare();
                mBackgroundHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        messageHandler(msg);
                    }
                };
                synchronized(this) {
                    notifyAll();
                }
                Looper.loop();
            }
        };

        mBackgroundThread.start();

        synchronized(mBackgroundThread) {
            if (mBackgroundHandler == null) {
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Waiting for background handler to initialize.");
                }
                try {
                    mBackgroundThread.wait();
                }  catch (InterruptedException e) {
                    if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, Log.getStackTraceString(e));
                    }
                }
            }
        }

        if (mBackgroundHandler == null) {
            Log.e(TAG, "Fatal Error - Unable to create background handler.");
        } else if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Created background handler.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            mBackgroundHandler.getLooper().quit();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Intent handlerIntent = new Intent(intent);
        if (mBackgroundHandler != null) {
            mBackgroundHandler.post(new Runnable() {
                public void run() {
                    handleIntent(handlerIntent);
                }
            });
        } else {
            Log.e(TAG, "Fatal Error - Background handler is not initialized.");
        }
        return START_NOT_STICKY;
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            String intentAction = intent.getAction();
            String extraComponent = intent.getStringExtra(PerformanceStats.INTENT_EXTRA_COMPONENT);
            String extraAction = intent.getStringExtra(PerformanceStats.INTENT_EXTRA_ACTION);
            Bundle extraData = intent.getExtras();

            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "start called with " + intentAction);
            }

            if (extraData != null && extraComponent != null && extraAction != null &&
                    extraComponent.length() > 0 && extraAction.length() > 0) {
                if (PerformanceStats.INTENT_START_COLLECTION.equals(intentAction)) {
                    handleStart(extraComponent, extraAction, extraData);
                } else if (PerformanceStats.INTENT_STOP_COLLECTION.equals(intentAction)) {
                    handleStop(extraComponent, extraAction, extraData);
                }
            }
        }
    }

    /**
     * @param component Component Name. Cannot be null or blank.
     * @param action action. Cannot be null or blank.
     * @return Key to use in hashmap, generated using component name and tag
     */
    private String getKey(String component, String action) {
        String key = null;
        if (component != null && action != null && component.length() > 0 && action.length() > 0) {
            key = component + action;
        }
        return key;
    }

    private void handleStart(String component, String action, Bundle data) {
        String key = getKey(component, action);

        if (key != null && key.length() > 0) {
            boolean newRequest = false;
            if (!mSyncMap.containsKey(key)) {
                mSyncMap.put(key, new PerformanceStatsHandler(component, action, this));
                newRequest = true;
            }
            PerformanceStatsHandler perfStatsHandler = mSyncMap.get(key);

            if (perfStatsHandler != null) {
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "handleStart(): " + component + " / " + action);
                }
                perfStatsHandler.handleStart(data);
                if (!newRequest) {
                    /* remove any previous timeouts if we're getting start again */
                    mBackgroundHandler.removeMessages(MSG_PERF_STATS_TIMEOUT, perfStatsHandler);
                }
                /* can't use key as message object, since message object comparison is done using
                 * ==, not .equals()
                 */
                Message timeoutMsg = mBackgroundHandler.obtainMessage(MSG_PERF_STATS_TIMEOUT,
                                     perfStatsHandler);
                mBackgroundHandler.sendMessageDelayed(timeoutMsg, PERF_STATS_TIMEOUT_MS);
            }
        }
    }

    private void handleStop(String component, String action, Bundle data) {
        String key = getKey(component, action);

        if (key != null && key.length() > 0) {
            PerformanceStatsHandler perfStatsHandler = mSyncMap.get(key);

            if (perfStatsHandler != null) {
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "handleStop(): " + component + " / " + action);
                }
                mBackgroundHandler.removeMessages(MSG_PERF_STATS_TIMEOUT, perfStatsHandler);
                perfStatsHandler.handleStop(data, this);
                mSyncMap.remove(key);
            } else {
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "handleStop() without matching handleStart(): " +
                          component + " / " + action);
                }
            }
        }
    }

    private void messageHandler(Message msg) {
        if (msg != null) {
            switch(msg.what) {
            case MSG_PERF_STATS_TIMEOUT:
                if (msg.obj instanceof PerformanceStatsHandler) {
                    PerformanceStatsHandler perf = (PerformanceStatsHandler) msg.obj;
                    String key = getKey(perf.getComponent(), perf.getAction());
                    if (key != null) {
                        PerformanceStatsHandler removed = mSyncMap.remove(key);
                        if (removed != null) {
                            Log.w(TAG, "Timed out and removed: " + removed.getComponent() +
                                  "/" + removed.getAction());
                        } else {
                            /* this really shouldn't happen... */
                            Log.e(TAG, "Timed out but failed to remove: " +
                                  perf.getComponent() + "/" + perf.getAction());
                        }
                    }
                }
                break;

            default:
            }
        }
    }
}
