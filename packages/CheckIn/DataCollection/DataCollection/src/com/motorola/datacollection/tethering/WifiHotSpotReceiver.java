package com.motorola.datacollection.tethering;

import java.util.ArrayList;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public final class WifiHotSpotReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_WifiHotSpotReceiver";
    private static final boolean LOGD = Utilities.LOGD;

    private static final String HOTSPOT_STATE_CHANGED_INTENT = "com.motorola.mynet.STATE_CHANGED";
    private static final String EXTRA_STATE = "mynet_state";
    private static final int STATE_INVALID = -1;
    private static final int STATE_STARTED = 6;
    private static final int STATE_STOPPED = 8;
    private static final int HOTSPOT_DISABLED = 0;
    private static final int HOTSPOT_ENABLED = 1;
    private static final String ACTION_TETHER_STATE_CHANGED =
        "android.net.conn.TETHER_STATE_CHANGED";
    private static final String EXTRA_ACTIVE_TETHER = "activeArray";
    private static final String MOT_3G_HOTSPOT_INTERFACE = "wifi";
    private static String sLastInterfaceDataCheckedIn = "";
    private static String sLastTetherInterface = "UNKNOWN";

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }

    private final void onReceiveImpl(Context context, Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;

        if ( LOGD ) { Log.d( TAG, "Received intent " + intent ); }

        if ( intent == null ) return;

        String action = intent.getAction();
        int hotSpotState;

        if ( HOTSPOT_STATE_CHANGED_INTENT.equals( action ) == true ) {
            int stateValue = intent.getIntExtra( EXTRA_STATE, STATE_INVALID );
            if ( LOGD ) { Log.d( TAG, "Got state " + stateValue ); }

            sLastTetherInterface = MOT_3G_HOTSPOT_INTERFACE;

            switch ( stateValue ) {
            case STATE_STARTED:
                hotSpotState = HOTSPOT_ENABLED;
                break;
            case STATE_STOPPED:
                hotSpotState = HOTSPOT_DISABLED;
                break;
            default:
                if ( LOGD ) { Log.d( TAG, "Ignoring state " + stateValue ); }
                return;
            }

        } else if ( ACTION_TETHER_STATE_CHANGED.equals( action ) == true ) {
            ArrayList<String> activeTetherList =
                intent.getStringArrayListExtra( EXTRA_ACTIVE_TETHER );

            if ( activeTetherList != null && !activeTetherList.isEmpty() ) {
                hotSpotState = HOTSPOT_ENABLED;
                sLastTetherInterface = TextUtils.join( ",", activeTetherList );
            } else {
                hotSpotState = HOTSPOT_DISABLED;
            }
            if ( LOGD ) { Log.d( TAG, "Got data " + hotSpotState + " " + intent.toUri(0) ); }
        } else {
            if ( LOGD ) { Log.d( TAG, "Ignoring unexpected intent " + intent ); }
            return;
        }

        String interfaceCheckinData = ";on=" + hotSpotState + ";if=" + sLastTetherInterface + ";]";

        if ( !sLastInterfaceDataCheckedIn.equals(interfaceCheckinData) ) {
            sLastInterfaceDataCheckedIn = interfaceCheckinData;

            // Log Format: [ID=DC_HOTSPOT;ver=0.1;time=123;on=1;if=tiwlan0]
            String [] kvpair = new String[4];
            kvpair[0] = "on";
            kvpair[1] = Integer.toString(hotSpotState);
            kvpair[2] = "if";
            kvpair[3] = sLastTetherInterface;
            Utilities.reportBasic(Utilities.LOG_TAG_LEVEL_2, "DC_HOTSPOT",
                    Utilities.EVENT_LOG_VERSION, timeMs, kvpair);
        }
    }
}
