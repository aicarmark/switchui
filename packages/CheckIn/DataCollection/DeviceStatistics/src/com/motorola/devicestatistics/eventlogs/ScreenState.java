package com.motorola.devicestatistics.eventlogs;

import android.content.Context;
import android.content.SharedPreferences;

import com.motorola.devicestatistics.CheckinHelper;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;
import com.motorola.devicestatistics.DevStatPrefs;
import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.Utils;

public class ScreenState {

    private static final String PREF_NAME= "ScreenState";
    private static final String KEY="sreen_state2";
    private static final String CHECKIN_TAG = Config.TAGS[1]; //MOT_DEVICE_STATS_L1
    private static final long SECOND_MS = 1000;

    /**
     * Stores the current screen state in shared preference for future reporting
     * @param context : context in which this is called
     * @param state : current screen state
     */
    public static void storeScreenState(Context context, boolean state) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        synchronized (ScreenState.class) {

            String storedStates = pref.getString(KEY, "");
            StringBuilder sb = new StringBuilder(storedStates);
            if (sb.length() != 0) sb.append('[');
            sb.append(System.currentTimeMillis()).append(";").append(state ? 1:0);
            editor.putString(KEY, sb.toString());
            Utils.saveSharedPreferences(editor);
        }
    }

    /**
     * Check in the Screens state that are pending to be reported.
     * @param context : context in this is called
     */
    public static void checkinScreenStateStats(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        synchronized (ScreenState.class) {
            String stats = pref.getString(KEY, null);
            if(stats != null) {
                LogBuilder.updateTimeConstants();
                DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent( CHECKIN_TAG, "EventLogs",
                        DevStatPrefs.VERSION, System.currentTimeMillis()/SECOND_MS,
                        "tz", String.valueOf(LogBuilder.sTz));
                String[] screenStates = stats.split("\\[");
                for (int i=0; i<screenStates.length; i++) {
                    String fields[] = screenStates[i].split(";");
                    DsSegment segment = CheckinHelper.createUnnamedSegment(
                            "scrst", fields[0], fields[1] );
                    checkinEvent.addSegment(segment);
                }
                checkinEvent.publish(context.getContentResolver());

                // once the available data is checked in clear the shared prefs
                SharedPreferences.Editor editor = pref.edit();
                editor.remove(KEY);
                Utils.saveSharedPreferences(editor);
            }
        }
    }
}
