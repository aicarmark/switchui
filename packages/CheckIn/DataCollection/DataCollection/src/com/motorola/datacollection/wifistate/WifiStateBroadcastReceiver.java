package com.motorola.datacollection.wifistate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

/* FORMAT
 *      [ID=DC_WIFIDRVST;ver=xx;time=xx;st=xx;]
 *       st = current wifi driver state
 *        0 : OFF
 *        1 : ON
 */
public class WifiStateBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_WifiStateBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;
    private static final int INVALID = -1;
    private static final int OFF = 0;
    private static final int ON = 1;
    private static int sPrevWifiDrvState = INVALID;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }
    private void onReceiveImpl(Context context, final Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;
        if ( LOGD ) Log.d( TAG, "onReceive called" );
        if ( intent == null ) return;

        String action = intent.getAction();
        if (action == null) return;

        if ( LOGD ) Log.d( TAG , "Received intent " + intent.toString() + ", action = "+action);

        if ( action.equals( WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            int currentWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (LOGD) {
                int previousWifistate = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                Log.d(TAG, "onReceive WIFI_STATE_CHANGED_ACTION. cst = "+currentWifiState+", pst = "+previousWifistate);
            }

            if ((sPrevWifiDrvState != OFF) && (currentWifiState == WifiManager.WIFI_STATE_DISABLING ||
                    currentWifiState == WifiManager.WIFI_STATE_DISABLED) ) {
                sPrevWifiDrvState = OFF;
                logWifiDrvTransition(sPrevWifiDrvState, timeMs);
            } else {
                if (sPrevWifiDrvState != ON &&
                      (currentWifiState == WifiManager.WIFI_STATE_ENABLING ||
                              currentWifiState == WifiManager.WIFI_STATE_ENABLED)) {
                    sPrevWifiDrvState = ON;
                    logWifiDrvTransition(sPrevWifiDrvState, timeMs);
                }
            }
        }

        // cannot use WIFI_NETWORK_STATE_CHANGED intent with disconnected state,
        // as we get it even when phone scans for networks and doesn't connect to any network.
        // Hence using SUPPLICANT_STATE_CHANGED_ACTION

        if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
            SupplicantState st = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (LOGD) Log.d(TAG, "received SUPPLICANT_STATE_CHANGED_ACTION. new state = " + st);
            if (sPrevWifiDrvState != OFF && SupplicantState.DORMANT.equals(st) ) {
                sPrevWifiDrvState = OFF;
                logWifiDrvTransition(sPrevWifiDrvState, timeMs);
            }

            if (sPrevWifiDrvState != ON && !SupplicantState.DORMANT.equals(st)) {
                sPrevWifiDrvState  = ON;
                logWifiDrvTransition(sPrevWifiDrvState, timeMs);
            }
        }
    }

    private void logWifiDrvTransition(int state, long timeMs) {
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        sb.append("[ID=DC_WIFIDRVST;ver=" + APP_VER + ";time=" + timeMs);
        sb.append(";st="+state);
        sb.append(";]");
        String[] kvpair = new String[2];
        kvpair[0] = "st";
        kvpair[1] = Integer.toString(state);
        Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_3, "DC_WIFIDRVST",
                Utilities.EVENT_LOG_VERSION, timeMs, kvpair);
        if (LOGD) Log.d(TAG, sb.toString());
    }

}
