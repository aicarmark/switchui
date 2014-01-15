package com.motorola.datacollection.hardkey;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;


/* FORMAT
 *      [ID=DC_HK;ver=xx;time=xx ;key=xx]
 *      key=VUP for volume up
 *      key=VDN for volume down
 *      key=CAM for camera
 */

public class HardkeyBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_HardkeyBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_VALUE =
        "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final String EXTRA_PREV_VOLUME_STREAM_VALUE =
        "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
    private static final int VOLUME_MAX = 15;
    private static final int VOLUME_MIN = 1;
    private static final int KEY_ACTION_DOWN = 1;
    private static final int KEY_ACTION_UP = 0;
    private static int sExpectedKeyAction = KEY_ACTION_DOWN;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }

    public void onReceiveImpl(Context context, Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;

        String key;
        if ( intent == null ) return;
        String action = intent.getAction();
        if ( action == null ) return;

        if ( action.equals(VOLUME_CHANGED_ACTION) ) {
            int newVolLevel =
                intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, 0);
            int oldVolLevel =
                intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, 0);

            if ( LOGD ) Log.d( TAG, " New Volume = " +newVolLevel );
            if ( LOGD ) Log.d( TAG,"  Old Volume = " + oldVolLevel );

            if ( ( newVolLevel > oldVolLevel ) && ( newVolLevel != VOLUME_MAX ) ) {
                if ( LOGD ) Log.d( TAG, " newVolLevel > oldVolLevel " );
                key = "VUP";
                sExpectedKeyAction = KEY_ACTION_UP;
            } else if ( ( newVolLevel < oldVolLevel ) && ( newVolLevel != VOLUME_MIN ) ) {
                if ( LOGD ) Log.d( TAG, " newVolLevel < oldVolLevel " );
                key = "VDN";
                sExpectedKeyAction = KEY_ACTION_UP;
            } else if ( ( newVolLevel == VOLUME_MAX ) || ( newVolLevel == VOLUME_MIN ) ) {
                if ( sExpectedKeyAction == KEY_ACTION_DOWN ) {
                    if( newVolLevel == VOLUME_MAX ) {
                        if ( LOGD ) Log.d( TAG, " newVolLevel == VOLUME_MAX " );
                        key = "VUP";
                    } else {
                        if ( LOGD ) Log.d( TAG, " newVolLevel == VOLUME_MIN " );
                        key = "VDN";
                    }
                    if ( LOGD ) Log.d( TAG, " sExpectedKeyAction changed to KEY_ACTION_UP" );
                    sExpectedKeyAction = KEY_ACTION_UP;
                } else {
                    if ( LOGD ) Log.d(TAG,"Key Action Up, So no log required" + newVolLevel);
                    if ( LOGD ) Log.d( TAG, " sExpectedKeyAction changed to KEY_ACTION_DOWN" );
                    sExpectedKeyAction = KEY_ACTION_DOWN;
                    return;
                }
            } else {
                if ( LOGD ) Log.d(TAG,"Key Action Up, So no log required" + newVolLevel);
                sExpectedKeyAction = KEY_ACTION_DOWN;
                return;
            }
        } else if (action.equals(Intent.ACTION_CAMERA_BUTTON)) {
            if ( LOGD ) {
                KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if ( event == null ) {
                    if ( LOGD ) Log.d( TAG, "event is null" );
                    return;
                }
                Log.d(TAG, "Camera Button pressed" + event.getKeyCode());
            }
            key= "CAM";
        } else {
            if ( LOGD ) Log.d(TAG, "Unknown hardkey pressed");
            return;
        }
        Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_HK", APP_VER, timeMs, "key", key);

    }
}
