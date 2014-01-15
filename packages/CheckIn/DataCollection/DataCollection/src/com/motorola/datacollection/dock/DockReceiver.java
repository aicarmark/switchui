package com.motorola.datacollection.dock;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class DockReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_DockReceiver";
    private static final boolean LOGD = Utilities.LOGD;

    private static final String DOCK_PREFERENCE_FILE = "Dock";
    private static final String DOCK_STATE_KEY = "LastState";
    private static final int DOCK_STATE_INVALID = -1;
    private static int sLastDockState = DOCK_STATE_INVALID;
    private static final String CAR_DOCK = "car";
    private static final String DESK_DOCK = "desk";
    private static final int DOCK_NOT_ACTIVE = 0;
    private static final int DOCK_ACTIVE = 1;

    private static final int getLastDockState() {
        // Called from background thread
        if ( sLastDockState == DOCK_STATE_INVALID ) {
            SharedPreferences pref = Utilities.getContext().getSharedPreferences(
                    DOCK_PREFERENCE_FILE, Context.MODE_PRIVATE );
            sLastDockState = pref.getInt( DOCK_STATE_KEY, DOCK_STATE_INVALID);
        }
        return sLastDockState;
    }

    private static final void setLastDockState( int dockState ) {
        // Called from background thread
        sLastDockState = dockState;
        SharedPreferences pref = Utilities.getContext().getSharedPreferences( DOCK_PREFERENCE_FILE,
                Context.MODE_PRIVATE );
        SharedPreferences.Editor edit = pref.edit();
        edit.putInt(DOCK_STATE_KEY,sLastDockState);
        Utilities.commitNoCrash(edit);
    }

    private static final boolean isLastDockStateValid() {
        // Called from background thread
        return sLastDockState == DOCK_STATE_INVALID ? false : true;
    }

    public static final void handleBootComplete() {
        // Called from background thread
        if ( isLastDockStateValid() == false ) {
            setLastDockState(Intent.EXTRA_DOCK_STATE_UNDOCKED);
        }
    }

    @Override
    public final void onReceive(Context context, final Intent intent) {
        // Called from main thread
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl( intent, timeMs );
            }
        };
    }

    private final void onReceiveImpl(Intent intent, long timeMs) {
        // Called from background thread
        if (Watchdog.isDisabled()) return;

        if ( intent == null ) {
            if ( LOGD ) Log.d( TAG, "Null intent received" );
            return;
        }

        if ( LOGD ) Log.d( TAG, intent.toUri(0) );

        String action = intent.getAction();
        if ( Intent.ACTION_DOCK_EVENT.equals( action ) ) {
            int newDockState = intent.getIntExtra( Intent.EXTRA_DOCK_STATE, DOCK_STATE_INVALID );
            if ( newDockState == DOCK_STATE_INVALID ) {
                if ( LOGD ) Log.d( TAG, "ACTION_DOCK_EVENT missing EXTRA_DOCK_STATE");
                return;
            }

            int oldDockState = getLastDockState();

            int state = DOCK_ACTIVE;
            String logDock = null;

            switch ( newDockState ) {
            case Intent.EXTRA_DOCK_STATE_CAR:
                logDock  = CAR_DOCK;
                break;

            case Intent.EXTRA_DOCK_STATE_DESK:
                logDock = DESK_DOCK;
                break;

            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                state = DOCK_NOT_ACTIVE;
                switch (oldDockState) {
                case Intent.EXTRA_DOCK_STATE_CAR:
                    logDock = CAR_DOCK;
                    break;

                case Intent.EXTRA_DOCK_STATE_DESK:
                    logDock = DESK_DOCK;
                    break;

                case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                case DOCK_STATE_INVALID:
                    if ( LOGD ) Log.d( TAG, "Unexpected old dock state " + oldDockState );
                    // Do nothing
                    break;

                default:
                    if ( LOGD ) Log.d( TAG, "Disconnected unknown Dock " + oldDockState );
                    logDock = "Unknown" + oldDockState;
                    break;
                }
                break;

            default:
                if ( LOGD ) Log.d( TAG, "Connected unknown Dock " + newDockState );
                logDock = "Unknown" + newDockState;
                break;
            }

            // Format: [ID=DC_DOCK;ver=0.2;time=123;ty=car;st=1]
            if ( logDock != null ) {
                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_DOCK",
                        Utilities.EVENT_LOG_VERSION, timeMs, "ty", logDock,
                        "st", Integer.toString(state));
            }

            setLastDockState( newDockState );
        }
    }
}
