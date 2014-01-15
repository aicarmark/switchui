package com.motorola.datacollection;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DataCollectionSelfService extends Service {
    private static final String TAG = "DCE_DataCollectionSelfService";
    private static final boolean LOGD = Utilities.LOGD;
    private static final long SELF_START_DELAY_MS = 5 * 60 * 1000;

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        if ( LOGD ) Log.d( TAG, "onStartCommand called" );
        return START_STICKY;
    }

    @Override
    public final IBinder onBind(Intent arg0) {
        return null;
    }

    static final void startMe() {
        Utilities.getHandler().postDelayed(
                new Runnable() {
                    public void run() {
                        if ( LOGD ) {
                            Log.d( TAG, "starting self after " +  SELF_START_DELAY_MS + " ms" );
                        }
                        Utilities.getContext().startService( new Intent( Utilities.getContext(),
                                DataCollectionSelfService.class ) );
                    }
                }, SELF_START_DELAY_MS );
    }
}
