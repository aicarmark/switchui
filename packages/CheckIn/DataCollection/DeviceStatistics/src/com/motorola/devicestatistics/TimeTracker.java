/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: RealtimeTracker.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 30, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author bluremployee
 *
 */
public class TimeTracker {

    public final static String TRIGGER_ACTION = "com.motorola.devicestatistics.TimeTracker";
    public final static Intent TRIGGER_INTENT = new Intent(TRIGGER_ACTION);
    
    private final static String REALTIME_SHTDNREF = "realtime.shtdnref";
    private final static String REALTIME_TIMEOUTREF = "realtime.timeoutref";
    private final static String REALTIME_TOTALREF = "realtime.totalref";
    private final static String UPTIME_SHTDNREF = "uptime.shtdnref";
    private final static String UPTIME_TIMEOUTREF = "uptime.timeoutref";
    private final static String UPTIME_TOTALREF = "uptime.totalref";
    
    private final static String TAG = "TimeTracker";
    private final static boolean DUMP = false;
    
    public static void noteBootup(SharedPreferences sp, SharedPreferences.Editor spe) {
        // Add up any pending shutdown refs
        long shref = sp.getLong(REALTIME_SHTDNREF, -1);
        long toref = sp.getLong(REALTIME_TIMEOUTREF, -1);
        if(DUMP) Log.v(TAG, "noteBootup: realtime: adding shref:" + shref + ":toref:" + toref);
        if(shref != -1 && toref != -1 && shref > toref) {
            long totalref = sp.getLong(REALTIME_TOTALREF, 0);
            totalref = totalref + (shref - toref);
            spe.putLong(REALTIME_TOTALREF, totalref);
        }
        
        shref = sp.getLong(UPTIME_SHTDNREF, -1);
        toref = sp.getLong(UPTIME_TIMEOUTREF, -1);
        if(DUMP) Log.v(TAG, "noteBootup: uptime: adding shref:" + shref + ":toref:" + toref);
        if(shref != -1 && toref != -1 && shref > toref) {
            long totalref = sp.getLong(UPTIME_TOTALREF, 0);
            totalref = totalref + (shref - toref);
            spe.putLong(UPTIME_TOTALREF, totalref);
        }
        spe.putLong(REALTIME_TIMEOUTREF, SystemClock.elapsedRealtime());
        spe.putLong(UPTIME_TIMEOUTREF, SystemClock.uptimeMillis());
        spe.putLong(REALTIME_SHTDNREF, -1);
        spe.putLong(UPTIME_SHTDNREF, -1);
    }
    
    public static void noteShutdown(SharedPreferences sp, SharedPreferences.Editor spe) {
        spe.putLong(REALTIME_SHTDNREF, SystemClock.elapsedRealtime());
        spe.putLong(UPTIME_SHTDNREF, SystemClock.uptimeMillis());
    }
    
    public static void noteTimeout(SharedPreferences sp, SharedPreferences.Editor spe) {
        long nowrt = SystemClock.elapsedRealtime();
        long nowut = SystemClock.uptimeMillis();
        
        long toref = sp.getLong(REALTIME_TIMEOUTREF, -1);
        if(DUMP) Log.v(TAG, "noteTimeout: realtime: adding nowrt:" + nowrt + ":toref:" + toref);
        if(toref != -1 && nowrt > toref) {
            long totalref = sp.getLong(REALTIME_TOTALREF, 0);
            totalref += (nowrt - toref);
            spe.putLong(REALTIME_TOTALREF, totalref);
        }
        
        toref = sp.getLong(UPTIME_TIMEOUTREF, -1);
        if(DUMP) Log.v(TAG, "noteTimeou: uptime: adding nowut:" + nowut + ":toref:" + toref);
        if(toref != -1 && nowut > toref) {
            long totalref = sp.getLong(UPTIME_TOTALREF, 0);
            totalref += (nowut - toref);
            spe.putLong(UPTIME_TOTALREF, totalref);
        }
        spe.putLong(REALTIME_TIMEOUTREF, nowrt);
        spe.putLong(UPTIME_TIMEOUTREF, nowut);
        Utils.saveSharedPreferences(spe);
    }
    
    public static long getRealtime(SharedPreferences sp) {
        noteTimeout(sp, sp.edit());
        return sp.getLong(REALTIME_TOTALREF, 0);
    }
    
    public static long getUptime(SharedPreferences sp) {
        noteTimeout(sp, sp.edit());
        return sp.getLong(UPTIME_TOTALREF, 0);
    }
}

