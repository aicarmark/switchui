/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: KpiLogBootReceiver.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Aug 3, 2010	      a24178      Created file
 **********************************************************
 */

package com.motorola.devicestatistics.eventlogs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.Watchdog;

/**
 * @author a24178
 *
 */
public class EventLogBootReceiver extends BroadcastReceiver {
    private static final String TAG = "EventLogBootReceiver";
    private static final String EVENT_EXTRA_FORCE_READ =
        "com.motorola.devicestatistics.forceeventread";

    /* (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(final Context ctx, final Intent intent) {
        new Utils.RunInBackgroundThread() {
            public void run() {
                onReceiveImpl(ctx, intent);
            }
        };
    }

    private void onReceiveImpl(Context ctx, Intent intent) {
        if (Watchdog.isDisabled()) return;

        if(intent == null) return;
        String action = intent.getAction();
        if(action == null) return;

        SharedPreferences sp = ctx.getSharedPreferences(EventConstants.OPTIONS,
                Context.MODE_PRIVATE);
        int level = sp.getInt(EventConstants.CONFIG_LEVEL, EventConstants.LOG_CONFIG);
        if(level == 0) return; // This means event logging is disabled
        // We should ideally be using Config for the above, but the faster here the better

        if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            setNextAlarm(sp, ctx);
            storeBootTime(sp);
        }else if(action.equals(EventConstants.Intents.LOG_ACTION)) {
            boolean forceRead = intent.getBooleanExtra( EVENT_EXTRA_FORCE_READ, false );
            EventLoggerService.getInstance(ctx).wakeupEventLogThread( forceRead ?
                    EventLoggerService.WAKEUP_FORCE_READ : EventLoggerService.WAKEUP_ALARM);
            setNextAlarm(sp, ctx);
        }
    }
    
    private void setNextAlarm(SharedPreferences sp, Context ctx) {
        long interval = sp.getLong(EventConstants.SAMPLE_RATE, EventConstants.LOG_INTERVAL);

        AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pI = PendingIntent.getBroadcast(ctx, 0, 
                new Intent(EventConstants.Intents.LOG_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT);
        if (am != null) {
            am.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + interval,
                pI);
        } else {
            Log.d (TAG, "Alarm Manager returned null");
        }
    }

    private void storeBootTime(SharedPreferences sp) {
        // We are in a pickle here if there are multiple restarts
        // before the service ever comes up since the log buffer
        // never stops running unless its a total shutdown/restart
        // So this is what we do:
        // 1. Store the latest bootuptime
        // 2. Store a seq which goes up everytime we are here
        // 3. If seq > 1 at service read, then we ignore bootuptime
        // 4. If seq = 1 at service read, we use bootuptime
        // 5. In either case we reset seq
        long now = System.currentTimeMillis();
        int seq = sp.getInt(EventConstants.BOOTTIME_SEQNUM, 0);
        seq++;

        SharedPreferences.Editor spe = sp.edit();
        spe.putLong(EventConstants.BOOTTIME_REFERENCE, now);
        spe.putInt(EventConstants.BOOTTIME_SEQNUM, seq);
        Utils.saveSharedPreferences(spe);
    }

}

