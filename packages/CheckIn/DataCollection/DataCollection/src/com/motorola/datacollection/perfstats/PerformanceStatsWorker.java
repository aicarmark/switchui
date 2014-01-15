/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/02/07   IKCTXTAW-359    Initial version
 * w04917 (Brian Lee)          2012/02/14   IKCTXTAW-441    Use the new Checkin API
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.CheckinEventHelper;
import com.motorola.datacollection.perfstats.data.PerformanceStatsDb;
import com.motorola.kpi.perfstats.Logger;

/**
 * @author w04917 (Brian Lee)
 * Takes care of low priority lengthy tasks like aggregating log data to check in.
 * We don't want this to block start/stop of perfStats collection, so we need
 * a separate thread to run this. If we posted this to perfStat service's
 * background thread queue, start/stop collection won't be handled.
 */
public class PerformanceStatsWorker extends IntentService {
    private static final String TAG = "PerfStatsWorker";

    static final String INTENT_INIT_PERF_STATS =
        "com.motorola.datacollection.perfstats.INTENT_INIT_PERF_STATS";
    static final String INTENT_CHECK_IN_DAILY_STATS =
        "com.motorola.datacollection.perfstats.INTENT_CHECK_IN_DAILY_STATS";

    private static final Uri READ_SETTINGS_URI =
        Uri.parse("content://com.motorola.datacollection.perfstats.perfstatssettings");
    private static final Uri WRITE_SETTINGS_URI =
        Uri.parse("content://com.motorola.datacollection.perfstats.perfstatssettingswriter");

    private static final String KEY_INITIALIZED = "initialized";

    public PerformanceStatsWorker() {
        /* There's nothing much documented about the constructor.
         * Following Google platform code example
         */
        super("PerformanceStatsWorker");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onHandleIntent() - " + action);
            }

            if (INTENT_CHECK_IN_DAILY_STATS.equals(action)) {
                checkInDailyStats();
            } else if (INTENT_INIT_PERF_STATS.equals(action)) {
                initPerfStats();
            }
        }
    }

    /**
     * Set default perfstats settings on first boot
     */
    private void initPerfStats() {
        long callStartTime = 0;
        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            callStartTime = Logger.getCallTime();
        }
        Bundle settings = getContentResolver().call(READ_SETTINGS_URI,
                          PerformanceStatsSettingsCache.CALL_GET_SINGLE_SETTING,
                          KEY_INITIALIZED, null);
        if (settings == null) {
            /* was never initialized, set default values */
            int numSettings = PerformanceStatsDefaultSettings.DEFAULT_SETTINGS.size();

            /* read the default settings into an array */
            ArrayList<String> keys = new ArrayList<String>();
            ArrayList<String> values = new ArrayList<String>();
            /* add 1 to set KEY_INITIALIZED */
            keys.ensureCapacity(numSettings + 1);
            values.ensureCapacity(numSettings + 1);
            for (Map.Entry<String, String> setting :
                    PerformanceStatsDefaultSettings.DEFAULT_SETTINGS.entrySet()) {
                keys.add(setting.getKey());
                values.add(setting.getValue());
            }
            if (keys.size() == values.size()) {
                /* set the initialized field */
                keys.add(KEY_INITIALIZED);
                values.add("1");
                /* bulk write all the fields */
                Bundle data = new Bundle();
                data.putStringArray(PerformanceStatsSettingsWriter.SETTING_KEY,
                                    keys.toArray(new String[keys.size()]));
                data.putStringArray(PerformanceStatsSettingsWriter.SETTING_VALUE,
                                    values.toArray(new String[values.size()]));
                getContentResolver().call(WRITE_SETTINGS_URI,
                                          PerformanceStatsSettingsWriter.CALL_SET_BULK_SETTINGS,
                                          null, data);
            }
        }
        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Logger.logExecutionTime(TAG, "initPerfStats", callStartTime);
        }
    }

    private void checkInDailyStats() {
        long callStartTime = 0;
        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            callStartTime = Logger.getCallTime();
        }
        PerformanceStatsDb db = new PerformanceStatsDb(this);
        Collection<DailyStats> statsList = db.getOldActionStats();
        if (statsList != null) {
            CheckinEventHelper checkinEvent = DailyStats.getCheckinEventHelper();
            for (DailyStats stat : statsList) {
                if (stat != null) {
                    checkinEvent.addSegments(stat.getCheckinSegments());
                }
            }

            if (checkinEvent.hasSegments()) {
                if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "checkInDailyStats() - " + checkinEvent);
                }

                boolean checkinSuccess = false;
                try {
                    checkinEvent.publish(getContentResolver());
                    checkinSuccess = true;
                } catch (Exception e) {
                    Log.w(TAG, "Error checking in data.");
                    if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                        e.printStackTrace();
                    }
                }

                /* since this is our daily job, clear the old logged bytes as well */
                if (checkinSuccess) {
                    db.clearOldLoggedBytes();
                    db.clearOldActionStats();
                }
            }
        }
        db.close();
        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Logger.logExecutionTime(TAG, "checkInDailyStats", callStartTime);
        }
    }
}
