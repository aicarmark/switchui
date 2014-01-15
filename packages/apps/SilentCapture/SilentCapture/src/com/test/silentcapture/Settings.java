package com.test.silentcapture;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Settings {

    public static final String TAG = "SilentCapture";
    public static final boolean LOGD = true;

    /**
     * The extra string for starting DeliverService, is to carry captured file path.
     */
    public static final String EXTRA_ATTACHMENT = "attachment";

    // Shared preference
    public static final String SHARED_PREFS = "SilentCaptureSettings";
    public static final String SHARED_AUTO_LAUNCH = "shared_auto_launch";
    public static final String SHARED_SCREEN_ON = "shared_screen_on";


    public static boolean isAutoLaunchEnabled(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Settings.SHARED_PREFS, Context.MODE_PRIVATE);
        boolean shared_auto_launch = settings.getBoolean(Settings.SHARED_AUTO_LAUNCH, true);
        //Log.d(Settings.TAG, "Settings is auto launch enabled:" + shared_auto_launch);
        return shared_auto_launch;
    }

    public static boolean isScreenOnExitEnabled(Context context) {
        //SharedPreferences settings = context.getSharedPreferences(Settings.SHARED_PREFS, Context.MODE_PRIVATE);
        //boolean shared_screen_on = settings.getBoolean(Settings.SHARED_SCREEN_ON, true);
        //Log.d(Settings.TAG, "Settings is screen on exit enabled:" + shared_screen_on);
        //return shared_screen_on;
        return true;
    }
}
