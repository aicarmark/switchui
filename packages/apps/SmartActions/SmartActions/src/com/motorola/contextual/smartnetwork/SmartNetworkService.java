/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/06/21   IKCTXTAW-480    Handle power plugged in before START_TIME
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add NetworkConditionDeterminer & refactor
 * w04917 (Brian Lee)        2012/07/16   IKCTXTAW-492    Broadcast HIDDEN_LOCATION_RULE intent
 */

package com.motorola.contextual.smartnetwork;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.HIDDEN_LOCATION_RULE;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.motorola.android.wrapper.AlarmManagerWrapper;
import com.motorola.android.wrapper.SystemWrapper;
public class SmartNetworkService extends IntentService {
    private static final String TAG = SmartNetworkService.class.getSimpleName();
    private static final boolean LOGD = true;

    static final String INTENT_MANAGE_SMART_NETWORK =
        "com.motorola.contextual.smartnetwork.INTENT_MANAGE_SMART_NETWORK";
    // allow debug mode to override wait interval and update delay
    private static final String EXTRA_DEBUG_MODE = "debug_mode";

    // HIDDEN_LOCATION_RULE intent for LocationSensor
    private static final String PUBLISH_EXTRA = "com.motorola.contextual"+".PUBLISH";
    private static final String RULE_START = "start";
    private static final String RULE_STOP = "stop";

    private static final String PREF_NAME = "SmartNetworkService";
    private static final String PREF_TIMESTAMP = "timestamp";

    private static final int BATTERY_UNPLUGGED = 0;
    private static final int START_TIME = 0; // 12 am, so include up to 0:59am
    private static final int END_TIME = 5; // 5am, so include up to 5:59am

    // maximum allowed time without update
    private static final long MAX_UPDATE_WAIT_INTERVAL = 3 * DateUtils.DAY_IN_MILLIS;
    // only update at most every 18 hours
    private static final long MINIMUM_UPDATE_DELAY = 18 * DateUtils.HOUR_IN_MILLIS;

    /*
     * TopLocation
     */
    // how many top locations to store
    private static final int MAX_TOP_LOCATIONS = 3;
    // search interval in LocTime table
    private static final long LOC_TIME_SEARCH_PERIOD = 30 * DateUtils.DAY_IN_MILLIS;

    /*
     * NetworkDeterminer
     */
    // max amount of monitoring time allowed before stopping monitoring a location
    private static final long MAX_MONITOR_TIME = 40 * DateUtils.HOUR_IN_MILLIS;
    /* if we accumulated MAX_MONITOR_TIME in the past MONITOR_PERIOD, stop monitoring the location
     * and just use available data to make a determination on the network condition,
     * whether we have enough data or not
     */
    private static final long MONITOR_PERIOD = 14 * DateUtils.DAY_IN_MILLIS;

    // how long to keep old data around before purging
    private static final long MAX_ARCHIVE_TIME = 30 * DateUtils.DAY_IN_MILLIS;


    public SmartNetworkService() {
        super(TAG);
    }

    private long getLastUpdateTime() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getLong(PREF_TIMESTAMP, 0);
    }

    private void updateLastUpdateTime() {
        SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(PREF_TIMESTAMP, System.currentTimeMillis());
        editor.commit();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (INTENT_MANAGE_SMART_NETWORK.equals(intent.getAction())) {
                // make sure power is still plugged in
                Intent batteryStatus = registerReceiver(null, new IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED));
                int plugState = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,
                                BATTERY_UNPLUGGED);
                if (plugState == BATTERY_UNPLUGGED) {
                    if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Not running since phone is on battery.");
                    }
                    return;
                }

                boolean debugMode = intent.hasExtra(EXTRA_DEBUG_MODE);
                if (debugMode) {
                    Log.d(TAG, "Debug mode enabled.");
                }

                // check when we last updated
                long lastUpdated = getLastUpdateTime();
                long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdated;
                boolean runTasks = false;

                if (timeSinceLastUpdate >= MAX_UPDATE_WAIT_INTERVAL) {
                    /* if time since last update is more than MAX_UPDATE_WAIT_INTERVAL,
                     * update regardless of the time of day
                     */
                    runTasks = true;
                } else if (timeSinceLastUpdate >= MINIMUM_UPDATE_DELAY) {
                    // make sure the minumum delay has passed since last update

                    // only run update during specified times
                    Calendar c = Calendar.getInstance();
                    int hour = c.get(Calendar.HOUR_OF_DAY);

                    if (hour >= START_TIME && hour <= END_TIME) {
                        runTasks = true;
                    } else {
                        // reschedule to run at the next interval
                        long delay = START_TIME - hour;
                        if (delay < 0) {
                            delay = delay + 24;
                        }
                        delay = delay * DateUtils.HOUR_IN_MILLIS;
                        // when power gets unplugged, this will get cancelled from ChargerReceiver
                        scheduleService(this, System.currentTimeMillis() + delay);
                    }
                }

                if (runTasks || debugMode) {
                    // run TopLocationManager
                    TopLocationManager manager = new TopLocationManager(MAX_TOP_LOCATIONS,
                            LOC_TIME_SEARCH_PERIOD);
                    boolean updated = manager.manageTopLocations(this);
                    if (updated) {
                        updateLastUpdateTime();
                    }
                    // only enable hidden location rule if we have active top locations
                    sendHiddenLocationRuleIntent(updated);

                    // run NetworkConditionDeterminer
                    NetworkConditionDeterminer determiner = new NetworkConditionDeterminer(
                        MAX_MONITOR_TIME, MONITOR_PERIOD, MAX_ARCHIVE_TIME);
                    determiner.purgeOldData(this);
                    determiner.updateObsoleteLocations(this);
                    determiner.determineNetworkConditions(this);

                } else if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Log.d(TAG, "Not running. Last execution was at " + sdf.format(lastUpdated));
                }
            }
        }
    }

    /**
     * Broadcasts HIDDEN_LOCATION_RULE intent to let other components (i.e. LocationSensor)
     * know that there's a hidden location rule active/inactive
     * @param isActive true if Hidden Location Rule is active, false otherwise
     */
    private void sendHiddenLocationRuleIntent(boolean isActive) {
        Intent ruleIntent =new Intent(HIDDEN_LOCATION_RULE);
        if (isActive) {
            ruleIntent.putExtra(PUBLISH_EXTRA, RULE_START);
        } else {
            ruleIntent.putExtra(PUBLISH_EXTRA, RULE_STOP);
        }
        sendBroadcast(ruleIntent);
    }

    /**
     * Schedule the SmartNetworkService to run
     * @param context Context to use
     * @param scheduledTime time the SmartNetworkService should run
     */
    public static void scheduleService(final Context context, final long scheduledTime) {
    	    	
    	Thread runnable = new Thread() {
			public void run() {
		        AlarmManagerWrapper am = (AlarmManagerWrapper) SystemWrapper.getSystemService(context,
                        Context.ALARM_SERVICE);
				if (am != null) {
				   if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
				       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				       Log.i(TAG, "Scheduling to run at " + sdf.format(scheduledTime));
				   }
				   PendingIntent pi = getAlarmIntent(context);
				   am.cancel(pi);
				   am.set(AlarmManager.RTC_WAKEUP, scheduledTime, pi);
				} else {
				   Log.e(TAG, "Unable to get AlarmManager for scheduling.");
				}
			}			
    	};
    	runnable.setPriority(Thread.NORM_PRIORITY-1);
    	runnable.start();
    	
    }

    /**
     * Cancel the scheduled SmartNetworkService
     * @param context Context to use
     */
    public static void cancelService(Context context) {
        AlarmManagerWrapper am = (AlarmManagerWrapper) SystemWrapper.getSystemService(context,
                                 Context.ALARM_SERVICE);
        if (am != null) {
            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Cancelling scheduled service.");
            }
            am.cancel(getAlarmIntent(context));
        } else {
            Log.e(TAG, "Unable to get AlarmManager for cancelling.");
        }
    }

    /**
     * @param context
     * @return pending intent to start SmartNetworkService
     */
    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(INTENT_MANAGE_SMART_NETWORK);
        PendingIntent pi = PendingIntent.getService(context, 0, intent,
                           PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
}
