package com.motorola.devicestatistics;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Class that keeps track of GPS usage over battery. This is different from that reported in
 * peruidstats, because that has overlap issues when 2 uids use gps at the same time.
 */
public class GpsTracker {
    private static final String TAG = "GpsTracker";
    private static final boolean VERBOSE_DUMP = false;
    private static final boolean DUMP = VERBOSE_DUMP || DevStatUtils.GLOBAL_DUMP;
    private static final int INVALID_VALUE = -1;
    private static final int STATE_INVALID = -1;
    private static final int STATE_OFF = 0;
    private static final int STATE_ON = 1;
    private static final int ON_BATTERY = 0;

    // The singleton instance of GpsTracker
    private static GpsTracker sInstance;

    private int mGpsState;
    private int mBatteryState;

    // The time up till which we have already updated shared pref with gps usage on battery
    private long mGpsTrackStartTime;

    /**
     * Constructor that initializes all states/values to unknown values.
     */
    private GpsTracker() {
        if (VERBOSE_DUMP) Log.d(TAG, "GpsTracker constructed");
        mGpsTrackStartTime = INVALID_VALUE;
        mGpsState = STATE_INVALID;
        mBatteryState = STATE_INVALID;
    }

    /**
     * Internal method used to get the singleton GpsTracker instance
     * @return The singleton instance of GpsTracker
     */
    private synchronized static final GpsTracker getInstance() {
        if (sInstance == null) sInstance = new GpsTracker();
        return sInstance;
    }

    /**
     * Method that updates GPS usage on battery when gps on/off, and battery connect/disconnect
     * events occur
     * @param context Android Context
     * @param intent The gps on/off OR battery connect/disconnect event that has occurred now
     */
    public static final void noteIntent(Context context, Intent intent) {
        getInstance().handleIntentImpl(context.getApplicationContext(), intent);
    }

    /**
     * Method to be called periodically to update the gps usage on battery in shared preferences
     * @param context Android context
     */
    public static final void noteTimeout(Context context) {
        getInstance().accumulate(context.getApplicationContext(), SystemClock.elapsedRealtime());
    }

    /**
     * Get the total gps usage on battery
     * @param context Android context
     * @return The cumulative gps usage on battery since factory reset
     */
    public static final long getTimeMs(Context context) {
        noteTimeout(context);
        DevStatPrefs pref = DevStatPrefs.getInstance(context);
        return pref.getLongSetting(DevStatPrefs.DEVSTATS_GPS_TIME, 0);
    }

    /**
     * Updates current gps usage on battery
     * @param context Android context
     * @param intent The gps on/off OR power intent that was just received
     */
    private void handleIntentImpl(Context context, Intent intent) {
        if (DUMP) Log.d(TAG, "Got GPS intent " + intent);

        String action = intent.getAction();
        if (action == null) return;

        // based on whether GPS was on battery till now, update the gps usage on battery in
        // shared preferences
        long elapsedRealtime = SystemClock.elapsedRealtime();
        accumulate(context, elapsedRealtime);

        if (action.equals(LocationManager.GPS_ENABLED_CHANGE_ACTION)) {
            boolean enabled = intent.getBooleanExtra(LocationManager.EXTRA_GPS_ENABLED, false);
            mGpsState = enabled ? STATE_ON : STATE_OFF;
        } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, INVALID_VALUE);
            if (plugged != INVALID_VALUE) {
                mBatteryState = (plugged == ON_BATTERY) ? STATE_ON : STATE_OFF;
            }
        }

        // If currently GPS is ON, on battery, then note down the time from which we have to
        // start updating the gps usage on battery
        if (isGpsActiveOnBattery()) mGpsTrackStartTime = elapsedRealtime;
    }

    /**
     * Find whether GPS is ON, on battery
     * @return true if GPS is active on battery, false otherwise
     */
    private final boolean isGpsActiveOnBattery() {
        return mGpsState == STATE_ON && mBatteryState == STATE_ON;
    }

    /**
     * If GPS was active on battery earlier, then update the usage in shared preferences
     * @param context Android context
     * @param elapsedRealtime The current value of SystemClock.elapsedRealtime
     */
    private void accumulate(Context context, long elapsedRealtime) {
        if (isGpsActiveOnBattery() == false) {
            if (VERBOSE_DUMP) {
                Log.d(TAG, "Not accumulating since mGTST=" + mGpsTrackStartTime +
                        " active=" + isGpsActiveOnBattery());
            }
            return;
        }

        long diffTime = elapsedRealtime - mGpsTrackStartTime;
        if (diffTime < 0) {
            Log.e(TAG, "Invalid negative time " + diffTime + " " + elapsedRealtime);
            return;
        }

        // Add the current gps usage on battery to that in shared preferences
        DevStatPrefs pref = DevStatPrefs.getInstance(context);
        long newTime = pref.getLongSetting(DevStatPrefs.DEVSTATS_GPS_TIME, 0) + diffTime;

        pref.setLongSetting(DevStatPrefs.DEVSTATS_GPS_TIME, newTime);
        if (VERBOSE_DUMP) Log.d(TAG, "GpsTime=" + newTime + ", delta=" + diffTime );

        // This assignment is for the call from noteTimeout
        mGpsTrackStartTime = elapsedRealtime;
    }
}
