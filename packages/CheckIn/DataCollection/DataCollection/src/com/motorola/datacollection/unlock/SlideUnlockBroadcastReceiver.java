package com.motorola.datacollection.unlock;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 *     FORMAT
 *         [ID=DC_UNLOCK;ver=" + APP_VER + ";time=xyz")
 */
public class SlideUnlockBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_SlideUnlockBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }

    private void onReceiveImpl(Context context, Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;

        if ( LOGD ) Log.d( TAG, "onReceive called" );

        if ( intent == null ) return;

        if ( LOGD ) Log.d( TAG , "Received intent " + intent.toString());

        String action = intent.getAction();
        if(action == null) return;

        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        if ( action.equals( Intent.ACTION_USER_PRESENT)) {
            sb.append("[ID=DC_UNLOCK;ver=" + APP_VER + ";time=" + timeMs);
            sb.append(";]");
        } else {
            // Should not come here
            if ( LOGD ) Log.d( TAG , "wrong intent" );
            return;
        }

        Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_5, "DC_UNLOCK",
                Utilities.EVENT_LOG_VERSION, timeMs );

    }
}
