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
 * w04917 (Brian Lee)          2012/02/14   IKCTXTAW-441    Use the new Checkin API
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.CheckinEventHelper;
import com.motorola.datacollection.perfstats.data.PerformanceStatsDataProvider;
import com.motorola.datacollection.perfstats.data.PerformanceStatsDb;
import com.motorola.kpi.perfstats.LogSetting;
import com.motorola.kpi.perfstats.LogSetting.Metrics;
import com.motorola.kpi.perfstats.Logger;
import com.motorola.kpi.perfstats.PerformanceStats;

/**
 * @author w04917 (Brian Lee)
 * Stores performance metric information about the specified component and action
 */
public final class PerformanceStatsHandler {
    private static final String TAG = "PerfStatsHandler";

    /* version for check-in event. only change if check-in event/segment format changed */
    static final String CHECK_IN_VERSION = "2";
    static final String CHECK_IN_TAG = "MOT_DEVICE_STATS_PERF";

    private static final String CHECK_IN_EVENT_PERF_STATS = "PERF_STATS";

    private static final String SEGMENT_PERFORMANCE = "perf";
    public static final String SEGMENT_CPU = "cpu";
    public static final String SEGMENT_MEMORY = "mem";
    public static final String SEGMENT_PROCESS = "proc";
    public static final String SEGMENT_NETWORK = "net";
    public static final String SEGMENT_DISK = "disk";

    private static final String FIELD_COMPONENT = "COMP";
    private static final String FIELD_ACTION = "ACT";
    private static final String FIELD_ACTION_DURATION = "ACTDUR";
    private static final String FIELD_OVERHEAD_FW = "OVRFW";
    private static final String FIELD_OVERHEAD_SERVICE = "OVRSVC";

    private static final Uri DATA_URI =
        Uri.parse("content://com.motorola.datacollection.perfstats.perfstatsdata");

    private final String mComponent, mAction;
    private final LogSetting mLogSetting;
    private final Context mContext;
    private final Collection<Metric> mMetrics;

    private boolean mStarted = false;
    private long mLogTime, mActionStartTime, mActionStopTime, mOverheadFw, mOverheadService;


    public PerformanceStatsHandler(String component, String action, Context context) {
        mComponent = component;
        mAction = action;
        /* if the hosting service that provided the Context dies,
         * this object dies with it too, so it's ok to save the context
         */
        mContext = context;
        mLogSetting = new LogSetting(mComponent, mAction, mContext);
        mMetrics = new LinkedList<Metric>();
    }

    /**
     * @return component name
     */
    public String getComponent() {
        return mComponent;
    }

    /**
     * @return action name
     */
    public String getAction() {
        return mAction;
    }

    public void handleStart(Bundle intentData) {
        if (intentData != null) {
            if (mStarted && PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                /* if already started, just overwrite with newer start values, but log it */
                Log.d(TAG, "Start called again before Stop was called for: " +
                      mComponent + "/" + mAction);
            }

            long callStartTime = Logger.getCallTime();

            /* we still want to update the basic stats even if loglimit was reached */
            mLogTime = intentData.getLong(PerformanceStats.INTENT_EXTRA_LOG_TIME);
            mActionStartTime = intentData.getLong(PerformanceStats.INTENT_EXTRA_ACTION_START_TIME);
            mOverheadFw = intentData.getLong(PerformanceStats.INTENT_EXTRA_OVERHEAD_TIME);

            Bundle metricData = intentData.getBundle(PerformanceStats.INTENT_EXTRA_METRIC_DATA);

            /* if metric data is null, loglimit was reached already.
             * if metric data is not null but logLimitReached() is true,
             * it's also possible that the component log limit was reached by some other action
             * or component during this short interval.
             * in both cases, we just want to update basic stats
             * without taking measurements when stop is called
             */
            if (metricData != null && !mLogSetting.logLimitReached()) {
                if (mMetrics.isEmpty()) {
                    if (mLogSetting.isMetricLogged(Metrics.DEVICE_CPU)) {
                        mMetrics.add(new DeviceCpuMetric(mLogSetting));
                    }

                    if (mLogSetting.isMetricLogged(Metrics.DEVICE_MEMORY)) {
                        mMetrics.add(new DeviceMemoryMetric(mLogSetting));
                    }

                    if (mLogSetting.isMetricLogged(Metrics.DEVICE_DISK)) {
                        mMetrics.add(new DeviceDiskMetric(mLogSetting));
                    }

                    if (mLogSetting.isMetricLogged(Metrics.DEVICE_NETWORK)) {
                        mMetrics.add(new DeviceNetworkMetric(mLogSetting));
                    }

                    if (mLogSetting.isMetricLogged(Metrics.PROCESS_CPU)) {
                        mMetrics.add(new ProcessCpuMetric(mLogSetting));
                    }

                }

                for(Metric metric : mMetrics) {
                    if (metric != null) {
                        metric.handleStart(metricData);
                    }
                }
            } else if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "metricData is null.");
            }

            mOverheadService = Logger.getCallTime() - callStartTime;
            mStarted = true;
        } else {
            Log.w(TAG, "Unable to handleStart for " + mComponent + "/" + mAction);
        }
    }

    public void handleStop(Bundle intentData, Context context) {
        if (mStarted && intentData != null) {
            long callStartTime = Logger.getCallTime();

            mStarted = false;
            mActionStopTime = intentData.getLong(PerformanceStats.INTENT_EXTRA_ACTION_STOP_TIME);
            mOverheadFw = mOverheadFw +
                          intentData.getLong(PerformanceStats.INTENT_EXTRA_OVERHEAD_TIME);

            /* we still want to update the basic stats even if loglimit was reached */
            long threshold = mLogSetting.getThreshold();
            long executionTime = mActionStopTime - mActionStartTime;
            PerformanceStatsDb perfDb = new PerformanceStatsDb(context);
            perfDb.updateActionStats(mComponent, mAction, executionTime, threshold);

            long callStartTime2 = 0;
            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                callStartTime2 = Logger.getCallTime();
            }
            /* schedule daily check-in, since we now have action stats to check-in */
            Intent checkInIntent = new Intent(PerformanceStatsWorker.INTENT_CHECK_IN_DAILY_STATS);

            PendingIntent serviceIntent = PendingIntent.getService(context, 0, checkInIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            calendar.clear(Calendar.AM_PM);
            calendar.clear(Calendar.HOUR);
            calendar.clear(Calendar.HOUR_OF_DAY);
            calendar.clear(Calendar.MINUTE);
            calendar.clear(Calendar.SECOND);
            calendar.clear(Calendar.MILLISECOND);
            long scheduleTime = calendar.getTimeInMillis();

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC, scheduleTime, serviceIntent);

            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Logger.logExecutionTime(TAG, "handleStop pendingIntent", callStartTime2);
            }

            Bundle metricData = intentData.getBundle(PerformanceStats.INTENT_EXTRA_METRIC_DATA);

            /* if metric data is null, loglimit was reached already.
             * if metric data is not null but logLimitReached() is true,
             * it's possible that the component log limit was reached by some other action
             * during this short interval.
             * in both cases, we just want to update basic stats
             * without taking measurements or checking in logs
             */
            if (metricData != null && !mLogSetting.logLimitReached()) {
                CheckinEventHelper checkinEvent = new CheckinEventHelper(CHECK_IN_TAG,
                        CHECK_IN_EVENT_PERF_STATS, CHECK_IN_VERSION, mLogTime);

                /* individual metric segment */
                for (Metric metric : mMetrics) {
                    if (metric != null) {
                        metric.handleStop(metricData);
                        checkinEvent.addSegments(metric.getCheckinSegments());
                    }
                }
                mOverheadService = mOverheadService + (Logger.getCallTime() - callStartTime);

                /* perf segment */
                Segment perfSegment = new Segment(SEGMENT_PERFORMANCE);
                perfSegment.setValue(FIELD_COMPONENT, mComponent);
                perfSegment.setValue(FIELD_ACTION, mAction);
                perfSegment.setValue(FIELD_ACTION_DURATION, executionTime);
                perfSegment.setValue(FIELD_OVERHEAD_FW, mOverheadFw);
                perfSegment.setValue(FIELD_OVERHEAD_SERVICE, mOverheadService);
                checkinEvent.addSegment(perfSegment);

                if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "handleStop - " + checkinEvent);
                }

                boolean checkinSuccess = false;
                try {
                    checkinEvent.publish(mContext.getContentResolver());
                    checkinSuccess = true;
                } catch (Exception e) {
                    Log.w(TAG, "Error checking in data.");
                    if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                        e.printStackTrace();
                    }
                }

                /* go through the provider to update logged bytes so the cache gets updated too.
                 * Since we're checking in first and then updating the log total, we may go over
                 * the limit once at the boundary.
                 */
                if (checkinSuccess) {
                    Bundle extras = new Bundle();
                    extras.putString(PerformanceStatsDataProvider.KEY_COMPONENT, mComponent);
                    extras.putString(PerformanceStatsDataProvider.KEY_ACTION, mAction);
                    extras.putInt(PerformanceStatsDataProvider.KEY_LOGGED_BYTES,
                                  checkinEvent.length());
                    mContext.getContentResolver().call(DATA_URI,
                                                       PerformanceStatsDataProvider.CALL_UPDATE_LOGGED_BYTES,
                                                       null, extras);
                }
            } else if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "metricData is null.");
            }
        } else {
            Log.w(TAG, "Unable to handleStop for " + mComponent + "/" + mAction);
        }
    }

}
