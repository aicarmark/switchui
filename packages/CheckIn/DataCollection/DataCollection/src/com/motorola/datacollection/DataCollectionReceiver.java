package com.motorola.datacollection;

import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.datacollection.accounts.AccountReceiver;
import com.motorola.datacollection.battery.BatteryHandler;
import com.motorola.datacollection.dock.DockReceiver;
import com.motorola.datacollection.telephony.TelephonyListener;
import com.motorola.datacollection.pkg.AppInfoLogger;

public class DataCollectionReceiver extends BroadcastReceiver {

    private static final boolean LOGD = Utilities.LOGD;
    private static final String TAG = "DCE_DataCollectionReceiver";
    private static final String TIMEZONE_EXTRA = "time-zone";
    private static final String ONE_HOUR_TIMER_INTENT =
        "com.motorola.datacollection.onehourtimer";
    private static long sTzOffet = TimeZone.getDefault().getRawOffset();

    @Override
    public void onReceive(Context context, final Intent intent) {
        // Called from main thread
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(intent);
            }
        };
    }

    public void onReceiveImpl(Intent intent) {
        if (Watchdog.isDisabled()) return;

        // Called from background thread
        if ( intent == null ) {
            if ( LOGD ) { Log.d( TAG, "Received null intent" ); }
            return;
        }

        if ( LOGD ) { Log.d( TAG, "onReceive " + intent.toUri(0) ); }

        String action = intent.getAction();
        if ( Intent.ACTION_BOOT_COMPLETED.equals( action ) ) {
            DockReceiver.handleBootComplete();
            AccountReceiver.handleBootComplete();
        } else if ( Intent.ACTION_SHUTDOWN.equals( action ) ) {
            Utilities.setShutdownActive();
            TelephonyListener.handlePeriodicWrite();
            BatteryHandler.handlePeriodicWrite();
            LogLimiter.handlePeriodicWrite();
        } else if ( ONE_HOUR_TIMER_INTENT.equals( action ) ) {
            if ( !Utilities.checkAndHandleTimeChange( Utilities.ONEHOURTIMER_TIME_REASON, "" ) ) {
                LogLimiter.handlePeriodicWrite();
                TelephonyListener.handlePeriodicWrite();
                BatteryHandler.handlePeriodicWrite();
                AppInfoLogger.handleTimerExpiry();
            }
        } else if ( Intent.ACTION_TIMEZONE_CHANGED.equals( action ) ) {
            long newTzOffset = TimeZone.getDefault().getRawOffset();
            if ( sTzOffet == newTzOffset ) {
                if ( LOGD ) Log.d( TAG, "Timezone offset is same, ignoring timezone change" );
                return;
            }
            String debugData = "";
            if ( Utilities.DEBUG_DUPLICATE_LOGS ) {
                debugData = ";atc=" + intent.getStringExtra(TIMEZONE_EXTRA) +
                    ";otz=" + (sTzOffet/60000) + ";ntz=" + (newTzOffset/60000);
            }
            sTzOffet = newTzOffset;

           Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_3, "DC_TIMEZONECHANGE",
                   Utilities.EVENT_LOG_VERSION, System.currentTimeMillis(),
                   "timezone", intent.getStringExtra(TIMEZONE_EXTRA));

            Utilities.checkAndHandleTimeChange( Utilities.TIMEZONE_REASON, debugData );
        } else if ( Intent.ACTION_TIME_CHANGED.equals( action ) ) {
            Utilities.checkAndHandleTimeChange( Utilities.TIME_REASON, "" );
        }
    }

    static final void initialize() {
        // Called from main thread
        Utilities.startAlarmManagerInexactPendingIntentTimer( ONE_HOUR_TIMER_INTENT,
                DataCollectionReceiver.class, Utilities.Periodicity.HOURLY );
    }
}
