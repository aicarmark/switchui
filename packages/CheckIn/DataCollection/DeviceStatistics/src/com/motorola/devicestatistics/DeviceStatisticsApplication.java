package com.motorola.devicestatistics;

import android.app.Application;
import android.util.Log;

public class DeviceStatisticsApplication extends Application implements DeviceStatsConstants {
    private static final String TAG = "DeviceStatisticsApplication";
    private static final boolean DUMP = GLOBAL_DUMP;

    @Override
    public void onCreate() {
        if (DUMP) Log.d(TAG, "DeviceStatisticsApplication.onCreate called");

        super.onCreate();

        new Utils.RunInBackgroundThread() {
            public void run() {
                Watchdog.initialize(DeviceStatisticsApplication.this);
                if (Watchdog.isDisabled()) return;

                TagSizeLimiter.init(DeviceStatisticsApplication.this);
            }
        };
    }
}
