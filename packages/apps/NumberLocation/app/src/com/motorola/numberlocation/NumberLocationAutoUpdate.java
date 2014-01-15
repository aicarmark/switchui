/*
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */
package com.motorola.numberlocation;

import java.util.Date;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Auto update in FOTA <p>
 * 
 * FOTA shall automatically request the update in the specified interval, such as 
 * 1 month, which can be configured by the flex. <p>
 * 
 * The first auto-update will occur when the SIM is registered into the network and 
 * the auto-update-enable is set to true. Then the next will occur after the fixed 
 * interval.
 */
public class NumberLocationAutoUpdate {
    // the context
    private Context m_ctx;

    // the log tag
    private static final String TAG = "NumberLocationAutoUpdate";


  
    public NumberLocationAutoUpdate(Context ctx) {
        m_ctx = ctx;
    }
    
    /** 
     * start the timer for next auto update
     */
    public void startTimer() {
        SharedPreferences prefs = m_ctx.getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, Context.MODE_PRIVATE);
        long nextUpTime = prefs.getLong(NumberLocationConst.KEY_NEXT_AUTO_UPDATE_TIME, 0);
        if (nextUpTime == 0) {
            Log.i(TAG, "Suspensive operation. User need to confirm 'Auto Update' ###");
            return;
        }
        
        long curTime = System.currentTimeMillis();
        if (nextUpTime <= curTime) {
            Log.i(TAG, "auto update intent->###");  
            Intent intent = new Intent(NumberLocationConst.ACTION_AUTO_UPDATE);
            intent.setClass(m_ctx, NumberLocationService.class);
            m_ctx.startService(intent);
        } else {        
            Log.i(TAG, "###start alarm for auto update at " + (new Date(nextUpTime)).toString());  
            Intent intent = new Intent(NumberLocationConst.ACTION_AUTO_UPDATE);
            intent.setClass(m_ctx, NumberLocationService.class);
            AlarmManager alm = (AlarmManager)m_ctx.getSystemService(Context.ALARM_SERVICE);
            alm.set(AlarmManager.RTC, nextUpTime, 
                    PendingIntent.getService(m_ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));            
        }
    }
    
    /**
     * cancel the started timer
     */
    public void cancelTimer() {
        Log.i(TAG, "cancel the auto-update timer###");
        Intent intent = new Intent(NumberLocationConst.ACTION_AUTO_UPDATE);
        intent.setClass(m_ctx, NumberLocationService.class);
        AlarmManager alm = (AlarmManager)m_ctx.getSystemService(Context.ALARM_SERVICE);
        alm.cancel(PendingIntent.getService(m_ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
    }
    
    /**
     * set the next update time
     */
    public void setNextUpdateTime(long interval_time) {
        long interval = interval_time;
        long curTime = System.currentTimeMillis();
        
        // get last auto-update time
        SharedPreferences prefs = m_ctx.getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE, Context.MODE_PRIVATE);        
        long lastTime = prefs.getLong(NumberLocationConst.KEY_NEXT_AUTO_UPDATE_TIME, -1);
        long nextTime;
        if (lastTime == -1) {
            Log.i(TAG, "first set the auto-update time.###");
            nextTime = curTime + interval;
        } else {
            nextTime = lastTime;
            if (nextTime < curTime) {
                while (nextTime < curTime) {
                    nextTime += interval;
                }
            } else if (nextTime - curTime > interval ) {
                // exception caused by the time is changed to the old
                Log.i(TAG, "the time is changed to the old.###");
                nextTime = curTime + interval ;
            }
        }
        
        SharedPreferences.Editor e = prefs.edit();
        e.putLong(NumberLocationConst.KEY_NEXT_AUTO_UPDATE_TIME, nextTime);
        e.commit();
        Log.i(TAG, "###next auto update time is " + (new Date(nextTime)).toString());
    }   
    
}
