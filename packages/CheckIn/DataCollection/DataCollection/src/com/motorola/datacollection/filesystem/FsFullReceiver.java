package com.motorola.datacollection.filesystem;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;
import com.motorola.datacollection.Utilities.FileSystemStats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

public class FsFullReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_FsFullReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final int FS_STATE_INVALID = -1;
    private static final int FS_STATE_NOT_FULL = 0;
    private static final int FS_STATE_FULL = 1;

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

        if ( intent == null ) {
            if ( LOGD ) { Log.d( TAG, "Received nulll intent" ); }
            return;
        }

        int fullState = FS_STATE_INVALID;

        String action = intent.getAction();
        if ( Intent.ACTION_DEVICE_STORAGE_LOW.equals( action ) == true ) {
            fullState = FS_STATE_FULL;
        } else if ( Intent.ACTION_DEVICE_STORAGE_OK.equals( action ) == true ) {
            fullState = FS_STATE_NOT_FULL;
        } else {
            if ( LOGD ) { Log.d( TAG, "Ignoring unexpected intent " + intent ); }
            return;
        }

        FileSystemStats stats = new FileSystemStats( Environment.getDataDirectory().getPath() );

        // Log Format: [ID=DC_FSFULL;ver=0.2;time=1291323013205;fl=0;tm=112590848;am=22626304;]
        Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_FSFULL", Utilities.EVENT_LOG_VERSION,
                timeMs, "fl", Integer.toString(fullState), "tm", String.valueOf(stats.mTotalSize),
                "am", String.valueOf(stats.mAvailableSize));
    }
}
