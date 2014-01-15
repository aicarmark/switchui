/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: BatteryLogger.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 2, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics.eventlogs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;

import com.motorola.devicestatistics.StatsController;
import com.motorola.devicestatistics.StatsTrigger;
import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.eventlogs.EventConstants.Events;
import com.motorola.devicestatistics.eventlogs.EventConstants.Source;
import android.util.Log;

/**
 * @author bluremployee
 *
 */
public class BatteryLogger {
    // Constants
    // Deltas
    final static int LVL_STEP = 1;
    final static int VLT_STEP = 400;
    final static int TMP_STEP = 30;
    private final static long MIN_CYCLE_TIME_MS = 10 * 1000; // 10 secs
   
    // IDs 
    final static int LEVEL = 0;
    final static int TEMP = 1;
    final static int VOLT = 2;
    final static int STATUS = 3;
    final static int DISCHARGE = 4;
    final static int CHARGE = 5;
    final static int CHARGE_FULL = 6;
    
    // Masks && Grouping
    final static int LEVEL_MASK = 1 << LEVEL;
    final static int TEMP_MASK = 1 << TEMP;
    final static int VOLT_MASK = 1 << VOLT;
    final static int LEVEL_GROUP_MASK = LEVEL_MASK | TEMP_MASK | VOLT_MASK;
    final static int STATUS_MASK = 1 << STATUS;
    final static int DISCHARGE_MASK = 1 << DISCHARGE;
    final static int CHARGE_MASK = 1 << CHARGE;
    final static int CHARGE_FULL_MASK = 1 << CHARGE_FULL;
    final static int ALL_MASK = LEVEL_GROUP_MASK |
            STATUS_MASK | DISCHARGE_MASK | CHARGE_MASK | CHARGE_FULL_MASK;
   
    // Output - Syntax 
    final static String SEPARATOR = ",";
    final static String DELIMITER = ";";
   
    // Output - Fixed 
    final static int FORCE_LEVEL = 2;
    final static int FORCE_SHUTDOWN = 1;
   
    // State 
    static int sBatteryLevel;
    static int sBatteryVolt;
    static int sBatteryTemp;
    static int sPlugStatus;
    
    static int sLastLevel;
    static int sLastStatus;
    
    static long sDischargeTime;
    static int sDischargeLevel;
    static long sChargeTime;
    static int sChargeLevel;
    
    static StringBuilder sBuilder = new StringBuilder();

    private final static boolean DUMP = false;
    private final static String TAG = "BatteryLogger";
    
    public static synchronized void noteEvent(Intent intent, long now,
            SharedPreferences state, SharedPreferences.Editor store) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        int volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        int level = Utils.getCurrentBatteryLevel();
        if (DUMP) Log.v(TAG, "noteEvent called. level = "+level);
 
        if(level == -1 || status == -1 || temp == -1 || volt == -1) return;

        StatsTrigger trigger = StatsController.getTrigger();
        if(trigger != null) {
            trigger.noteLevel(level, status, sBatteryLevel, sPlugStatus,
                    sLastStatus, sChargeLevel);
        }
        sLastStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        
        boolean reverse = false;
        int tolog = 0;
        if(sBatteryLevel == -1 || Math.abs(level-sBatteryLevel) >= LVL_STEP) {
            tolog |= LEVEL_MASK;
            sBatteryLevel = level;
        }
        if(sBatteryTemp == -1 || Math.abs(temp-sBatteryTemp) >= TMP_STEP) {
            tolog |= TEMP_MASK;
            sBatteryTemp = temp;
        }
        if(sBatteryVolt == -1 || Math.abs(volt-sBatteryVolt) >= VLT_STEP) {
            tolog |= VOLT_MASK;
            sBatteryVolt = volt;
        }
        if(sPlugStatus > 0 && level == 100 && level != sLastLevel) {
            tolog |= CHARGE_FULL_MASK;
        }
        if(status != sPlugStatus) {
            tolog |= STATUS_MASK;
            sPlugStatus = status;
            if(sPlugStatus == 0) {
                tolog |= CHARGE_MASK;
            }else {
                tolog |= DISCHARGE_MASK;
            }
        }else if(sPlugStatus == 0 && level != sLastLevel && 
                 (level == 15 || level == 10) && level != sDischargeLevel) {
            tolog |= DISCHARGE_MASK;
            reverse = true;
        }
        sLastLevel = level;
        
        if((tolog & ALL_MASK) != 0) {
            boolean commit = false;
            if((tolog & LEVEL_GROUP_MASK) != 0) {
                sBuilder.setLength(0);
                String pre = state.getString(EventConstants.LEVEL_TIME, null);
                if(pre != null) {
                    sBuilder.append(pre).append(SEPARATOR);
                }
                sBuilder.append(now).append(DELIMITER)
                        .append(sBatteryLevel).append(DELIMITER)
                        .append(sBatteryVolt).append(DELIMITER)
                        .append(sBatteryTemp).append(DELIMITER);
                store.putString(EventConstants.LEVEL_TIME, sBuilder.toString());
                commit = true;
            }
            if((tolog & STATUS_MASK) != 0) {
                sBuilder.setLength(0);
                String pre = state.getString(EventConstants.STATUS_TIME, null);
                if(pre != null) {
                    sBuilder.append(pre).append(SEPARATOR);
                }
                sBuilder.append(now).append(DELIMITER)
                        .append(sPlugStatus).append(DELIMITER)
                        .append(level).append(DELIMITER);
                store.putString(EventConstants.STATUS_TIME, sBuilder.toString());
                commit = true;
            }
            if((tolog & CHARGE_FULL_MASK) != 0) {
                sBuilder.setLength(0);
                String pre = state.getString(EventConstants.CHARGE_FULL_TIME, null);
                if(pre != null) {
                    sBuilder.append(pre).append(SEPARATOR);
                }
                sBuilder.append(now).append(DELIMITER);
                store.putString(EventConstants.CHARGE_FULL_TIME, sBuilder.toString());
                commit = true;
            }
            if((tolog & DISCHARGE_MASK) != 0) {
                long nowrt = SystemClock.elapsedRealtime();
                if(sDischargeTime != 0 && ((sDischargeLevel != level) ||
                            (nowrt-sDischargeTime) > MIN_CYCLE_TIME_MS))  {
                    sBuilder.setLength(0);
                    String pre = state.getString(EventConstants.DISCHARGE_TIME, null);
                    if(pre != null) {
                        sBuilder.append(pre).append(SEPARATOR);
                    }
                    sBuilder.append(now).append(DELIMITER)
                            .append(sDischargeLevel-level).append(DELIMITER)
                            .append(nowrt-sDischargeTime).append(DELIMITER);
                    if(reverse)
                        sBuilder.append(FORCE_LEVEL).append(DELIMITER);
                    store.putString(EventConstants.DISCHARGE_TIME, sBuilder.toString());

                    // Add up the cumulative discharge level also
                    long sofar = state.getLong(EventConstants.CUMULATIVE_DISCHARGE, 0);
                    store.putLong(EventConstants.CUMULATIVE_DISCHARGE,
                            sofar + sDischargeLevel - level);

                    commit = true;
                }
                if(reverse) {
                    // Start another discharge cycle from now
                    sDischargeTime = nowrt;
                    sDischargeLevel = level;
                }else {
                    sChargeTime = nowrt;
                    sChargeLevel = level;
                    sDischargeTime = 0;
                    sDischargeLevel = -1;
                }
            }else if((tolog & CHARGE_MASK) != 0) {
                long nowrt = SystemClock.elapsedRealtime();
                if(sChargeTime != 0 && ((sChargeLevel != level) ||
                            (nowrt-sChargeTime) > MIN_CYCLE_TIME_MS)) {
                    sBuilder.setLength(0);
                    String pre = state.getString(EventConstants.CHARGE_TIME, null);
                    if(pre != null) {
                        sBuilder.append(pre).append(SEPARATOR);
                    }
                    sBuilder.append(now).append(DELIMITER)
                            .append(level-sChargeLevel).append(DELIMITER)
                            .append(nowrt-sChargeTime).append(DELIMITER);
                    store.putString(EventConstants.CHARGE_TIME, sBuilder.toString());
                    commit = true;
                }
                sDischargeTime = nowrt;
                sDischargeLevel = level;
                sChargeTime = 0;
                sChargeLevel = -1;
            }
            if(commit) Utils.saveSharedPreferences(store);
        }
    }
    
    public static void init() {
        sBatteryLevel = sBatteryVolt = sBatteryTemp = sPlugStatus = -1;
        sDischargeTime = sChargeTime = 0;
        sChargeLevel = sDischargeLevel = sLastLevel = -1;
        sLastStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    }
    
    public static synchronized boolean getEvents(SharedPreferences state,
            SharedPreferences.Editor store, ILogger logger) {
        boolean found = false;
        final int source = Source.RECEIVER;
        String times = state.getString(EventConstants.CHARGE_TIME, null);
        if(times != null) {
            found = true;
            if(times.contains(SEPARATOR)) {
                String[] parts = times.split(SEPARATOR);
                for(int i = 0; i < parts.length; ++i) {
                    logger.log(source, Events.CHARGE, EventConstants.CHECKIN_ID,
                            "chrg;" + parts[i]);
                }
            }else {
                logger.log(source, Events.CHARGE, EventConstants.CHECKIN_ID,
                        "chrg;" + times);
            }
        }
        times = state.getString(EventConstants.DISCHARGE_TIME, null);
        if(times != null) {
            found = true;
            if(times.contains(SEPARATOR)) {
                String[] parts = times.split(SEPARATOR);
                for(int i = 0; i < parts.length; ++i) {
                    logger.log(source, Events.DISCHARGE, EventConstants.CHECKIN_ID,
                            "dchrg;" + parts[i]);
                }
            }else {
                logger.log(source, Events.DISCHARGE, EventConstants.CHECKIN_ID,
                        "dchrg;" + times);
            }
        }
        times = state.getString(EventConstants.CHARGE_FULL_TIME, null);
        if(times != null) {
            found = true;
            if(times.contains(SEPARATOR)) {
                String[] parts = times.split(SEPARATOR);
                for(int i = 0; i < parts.length; ++i) {
                    logger.log(source, Events.CHARGEFULL, EventConstants.CHECKIN_ID,
                            "bfc;" + parts[i]);
                }
            }else {
                logger.log(source, Events.CHARGEFULL, EventConstants.CHECKIN_ID,
                        "bfc;" + times);
            }
        }
        times = state.getString(EventConstants.LEVEL_TIME, null);
        if(times != null) {
            found = true;
            if(times.contains(SEPARATOR)) {
                String[] parts = times.split(SEPARATOR);
                for(int i = 0; i < parts.length; ++i) {
                    logger.log(source, Events.BATTLVL, EventConstants.CHECKIN_ID,
                            "blvl;" + parts[i]);
                }
            }else {
                logger.log(source, Events.BATTLVL, EventConstants.CHECKIN_ID,
                        "blvl;" + times);
            }
        }
        times = state.getString(EventConstants.STATUS_TIME, null);
        if(times != null) {
            found = true;
            if(times.contains(SEPARATOR)) {
                String[] parts = times.split(SEPARATOR);
                for(int i = 0; i < parts.length; ++i) {
                    logger.log(source, Events.BATTSTS, EventConstants.CHECKIN_ID,
                            "bsts;" + parts[i] );
                }
            }else {
                logger.log(source, Events.BATTSTS, EventConstants.CHECKIN_ID,
                        "bsts;" + times);
            }
        }
        
        if(found) {
            store.remove(EventConstants.LEVEL_TIME);
            store.remove(EventConstants.STATUS_TIME);
            store.remove(EventConstants.CHARGE_TIME);
            store.remove(EventConstants.DISCHARGE_TIME);
            store.remove(EventConstants.CHARGE_FULL_TIME);
        }
        return found; // If we modified store, then commit will be ensured by caller
    }
    
    public static synchronized void forceEvent(SharedPreferences state,
            SharedPreferences.Editor store, int level, long now) {
        if(sPlugStatus == 0) {
            if(sDischargeTime != 0 && sDischargeLevel != level) {
                long nowrt = SystemClock.elapsedRealtime();
                sBuilder.setLength(0);
                String pre = state.getString(EventConstants.DISCHARGE_TIME, null);
                if(pre != null) {
                    sBuilder.append(pre).append(SEPARATOR);
                }
                sBuilder.append(now).append(DELIMITER)
                        .append(sDischargeLevel-level).append(DELIMITER)
                        .append(nowrt-sDischargeTime).append(DELIMITER)
                        .append(FORCE_SHUTDOWN).append(DELIMITER);
                store.putString(EventConstants.DISCHARGE_TIME, sBuilder.toString());

                // Add up the cumulative discharge level also
                long sofar = state.getLong(EventConstants.CUMULATIVE_DISCHARGE, 0);
                store.putLong(EventConstants.CUMULATIVE_DISCHARGE,
                        sofar + sDischargeLevel - level);
            }
        }else if(sPlugStatus != -1) {
            if(sChargeTime != 0 && sChargeLevel != level) {
                long nowrt = SystemClock.elapsedRealtime();
                sBuilder.setLength(0);
                String pre = state.getString(EventConstants.CHARGE_TIME, null);
                if(pre != null) {
                    sBuilder.append(pre).append(SEPARATOR);
                }
                sBuilder.append(now).append(DELIMITER)
                        .append(level-sChargeLevel).append(DELIMITER)
                        .append(nowrt-sChargeTime).append(DELIMITER)
                        .append(FORCE_SHUTDOWN).append(DELIMITER);
                store.putString(EventConstants.CHARGE_TIME, sBuilder.toString());
            }
        }
        // Caller ensures commit of store
    }

    public static synchronized long getCumulativeDischarge(SharedPreferences state,
            int level) {
        long sofar = state.getLong(EventConstants.CUMULATIVE_DISCHARGE, 0);
        if(sPlugStatus == 0 && level != -1) {
            if(sDischargeTime != 0 && sDischargeLevel != level) {
                sofar += (sDischargeLevel - level);
            }
        }
        return sofar;
    }
}

