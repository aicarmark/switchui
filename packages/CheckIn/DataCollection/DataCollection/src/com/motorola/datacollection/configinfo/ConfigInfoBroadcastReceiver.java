package com.motorola.datacollection.configinfo;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.Log;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

/* FORMAT
 *      [ID=DC_CI;conf=xxx;ver=xx;time=xx ;st=xx]
 *      conf = HKB -> hardkeyboard, OR -> Orientation
 *      if conf = HKB, st=1 for open slider, st=2 for close slider
 *      if conf = OR, st = 0 -> Portrait, 1 -> Landscape, 2 -> Square
 */
public class ConfigInfoBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_CIBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;
    private static int sCurHardKeyboardHidden = 0;
    private static int sCurOrientation = 0;
    private static final int ORNT_PORTRAIT = 0;
    private static final int ORNT_LAND = 1;
    private static final int ORNT_SQUARE = 2;

    public synchronized static void initialize(Context context) {
        if ( LOGD ) Log.d ( TAG, "Config info initialize" );

        ConfigInfoBroadcastReceiver ciChangedReceiver = new ConfigInfoBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        Utilities.getContext().registerReceiver(ciChangedReceiver, intentFilter);
        Configuration myConfig = context.getResources().getConfiguration();
        sCurHardKeyboardHidden = myConfig.hardKeyboardHidden;
        sCurOrientation = myConfig.orientation;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }

    private final void onReceiveImpl(Context context, Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;

        final Configuration myConfig;

        if ( intent == null ) return;
        String action = intent.getAction();
        if ( action == null ) return;

        myConfig = context.getResources().getConfiguration();
        if ( LOGD ) Log.d(TAG, "Received config Info is " + myConfig.toString());

        if ( action.equals(Intent.ACTION_CONFIGURATION_CHANGED) ) {
            if ( myConfig.hardKeyboardHidden != sCurHardKeyboardHidden ) {
                sCurHardKeyboardHidden = myConfig.hardKeyboardHidden;
                if ( LOGD ) {
                    Log.d(TAG, "Slider keyboard state = " + sCurHardKeyboardHidden);
                }
                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_4, "DC_HKB",
                        APP_VER, timeMs, "st", Integer.toString(sCurHardKeyboardHidden));
            } else if ( myConfig.orientation != sCurOrientation ) {
                if ( LOGD ) {
                    Log.d(TAG, "Orientation changed from : " + sCurOrientation + " to : "
                            + myConfig.orientation);
                }
                int orient=0; // 0 = Portrait, 1 = Landscape, 2 = Square
                sCurOrientation = myConfig.orientation;
                if( myConfig.orientation == Configuration.ORIENTATION_PORTRAIT ) {
                    orient = ORNT_PORTRAIT;
                } else if( myConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ) {
                    orient = ORNT_LAND;
                } else if ( myConfig.orientation == Configuration.ORIENTATION_SQUARE ) {
                    orient = ORNT_SQUARE;
                } else {
                    if ( LOGD ) {
                        Log.d(TAG, "Unexpected change in orientation");
                        return;
                    }
                }
                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_4, "DC_ORNT",
                        APP_VER, timeMs, "st", Integer.toString(orient));
            } else {
                if ( LOGD ) Log.d(TAG, "Some other config changed");
            }
        }
    }
}
