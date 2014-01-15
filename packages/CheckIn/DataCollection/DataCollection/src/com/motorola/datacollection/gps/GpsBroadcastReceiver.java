package com.motorola.datacollection.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;


public class GpsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_GpsBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;

    @Override
    public void onReceive(Context context, final Intent intent) {
        final Boolean enabled = intent.getBooleanExtra( "enabled", false );
        final long timeMs = System.currentTimeMillis();

        new Utilities.BackgroundRunnable() {
            public void run() {
                if (Watchdog.isDisabled()) return;

                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "GPS_ON_OFF",
                        APP_VER, timeMs, "gps_enabled", enabled.toString());

                if ( LOGD ) {
                    Log.d(TAG,"Received the GPS Enable event="+intent.getAction()+
                            "and GPS Enable="+enabled);
                }
            }
        };
    }
}
