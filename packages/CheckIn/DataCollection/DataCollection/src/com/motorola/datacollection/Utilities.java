package com.motorola.datacollection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import com.motorola.android.provider.CheckinEvent;
import com.motorola.datacollection.battery.BatteryHandler;
import com.motorola.datacollection.telephony.TelephonyListener;

public class Utilities {
    private static final String TAG = "DCE_Utilities";
    private static final String CHECKIN_ERROR_TAG = "DataCollection";
    public static final boolean CHECKIN_TO_LOGCAT = false; // ODO
    public static final boolean LOGD = false; // ODO
    static final boolean DEBUG_DUPLICATE_LOGS = true; //TODO
    public static final String EVENT_LOG_VERSION = "0.33";
    private static Context sContext;
    private static Handler sHandler;
    private static Handler sBackgroundHandler;
    private static Thread sBackgroundThread;
    private static boolean sShutdownActive;
    public static final Timer sTimer = new Timer(true);
    static final String TIMEZONE_REASON = "tz";
    static final String TIME_REASON = "tm";
    static final String ONEHOURTIMER_TIME_REASON = "ohttc";
    public static final String ACCUMULATE_REASON = "ac";
    private static final long MAX_TIMESHIFT_BEFORE_DISCARD_STATISTICS_MS = 12 * 60 * 60 * 1000;
    private static final long THRESHOLD_FOR_TIME_CHANGE_MS = 100;
    private static long sBootWallClockTime =
        System.currentTimeMillis() - SystemClock.elapsedRealtime();
    private static long sBootWallClockTimeTc = sBootWallClockTime; // for logging time change
    public static final long MS_IN_DAY = 24 * 60 * 60 * 1000;
    public static final long MS_IN_YEAR = MS_IN_DAY * 365;
    private static final String BACKGROUND_THREAD_NAME = "dcBgThread";

    public static final String LOG_TAG_LEVEL_1 = "MOT_CA_STATS_L1";
    public static final String LOG_TAG_LEVEL_2 = "MOT_CA_STATS_L2";
    public static final String LOG_TAG_LEVEL_3 = "MOT_CA_STATS_L3";
    public static final String LOG_TAG_LEVEL_4 = "MOT_CA_STATS_L4";
    public static final String LOG_TAG_LEVEL_5 = "MOT_CA_STATS_L5";
    public static final String LOG_TAG_LEVEL_6 = "MOT_CA_STATS_L6";

    public enum Periodicity {
        HOURLY,
        DAILY
    }

    @SuppressWarnings("all")
    public static final void logEvent( final String logTag, final String id, final int logLength,
            final CheckinEvent checkinEvent ) {
        new BackgroundRunnable() {
            public void run() {
                LogLimiter.logEvent(logTag, id, logLength, checkinEvent);
            }
        };
    }

    public static final void logPriorityEvent( final CheckinEvent checkinEvent ) {
        new BackgroundRunnable() {
            public void run() {
                LogLimiter.logPriorityEvent(checkinEvent);
            }
        };
    }

    public static final void initialize( Context aContext ) {
        // MUST be called in the context of the main thread so that sHandler is on main thread
        sContext = aContext;
        sHandler = new Handler();

        HandlerThread bgThread = new HandlerThread(BACKGROUND_THREAD_NAME);
        sBackgroundThread = bgThread;

        bgThread.start();
        Looper looper = bgThread.getLooper();
        if (looper == null) {
            Log.e(TAG, "Background thread looper is null");
            return;
        }

        sBackgroundHandler = new Handler(looper);

        if ( LOGD ) Log.d( TAG, "Main thread found background handler" );
    }

    public static final void startAlarmManagerInexactPendingIntentTimer( String action,
            Class<?> handlerClass, Periodicity period ) {
        Intent wakeUpIntent = new Intent( Utilities.sContext, handlerClass );
        wakeUpIntent.setAction( action );
        PendingIntent pendingIntent = PendingIntent.getBroadcast( Utilities.sContext, 0,
                wakeUpIntent, PendingIntent.FLAG_UPDATE_CURRENT );
        AlarmManager alarmManager = (AlarmManager)Utilities.sContext.getSystemService(
                Context.ALARM_SERVICE );
        if (alarmManager == null) {
            if (LOGD) { Log.d (TAG, "alarmManager is NULL. Returning...."); }
            return;
        }
        // At this point, we dont know whether the app was killed and restarted,
        //  or is running for first time after boot.
        // The documentation doesn't say whether creating a new alarm automatically
        //  cancels the previous pending intent timer.
        // So let us forcibly kill any existing pending intent timer.
        alarmManager.cancel( pendingIntent );

        long interval;
        Calendar calendar = Calendar.getInstance();
        if ( period == Periodicity.DAILY ) {
            calendar.setTimeInMillis( System.currentTimeMillis() +
                    24 * 60 * 60 * 1000 ); // 1 day later
            calendar.set(
                    calendar.get( Calendar.YEAR ),
                    calendar.get( Calendar.MONTH ),
                    calendar.get( Calendar.DAY_OF_MONTH ),
                    0, 0, 0 ); // at 00:00:00 AM tonight
            interval = AlarmManager.INTERVAL_DAY;
        } else if ( period == Periodicity.HOURLY ) {
            calendar.setTimeInMillis( System.currentTimeMillis() +
                    1 * 60 * 60 * 1000 ); // 1 hour later
            calendar.set(
                    calendar.get( Calendar.YEAR ),
                    calendar.get( Calendar.MONTH ),
                    calendar.get( Calendar.DAY_OF_MONTH ),
                    calendar.get( Calendar.HOUR_OF_DAY ),
                    0, 0 ); // at xx:00:00
            interval = AlarmManager.INTERVAL_HOUR;
        } else {
            if ( LOGD ) { Log.d( TAG,
                    "startAlarmManagerInexactPendingIntentTime: invalid Period " + period ); }
            return;
        }

        alarmManager.setInexactRepeating( AlarmManager.RTC, calendar.getTimeInMillis(),
                interval, pendingIntent );
    }

    static public final class FileSystemStats {
        public long mAvailableSize;
        public long mTotalSize;

        public FileSystemStats( String mountPoint ) {
            try {
                StatFs stat = new StatFs(mountPoint);
                int blockSize = stat.getBlockSize();
                int totalBlocks = stat.getBlockCount();
                int availBlocks = stat.getAvailableBlocks();
                mAvailableSize = ((long)blockSize) * availBlocks;
                mTotalSize = ((long)blockSize) *totalBlocks;
            } catch ( IllegalArgumentException e ) {
                if ( LOGD ) Log.d( TAG, Log.getStackTraceString( e ) );
                mAvailableSize = mTotalSize = -1;
            }
        }
    }

    static public final void setShutdownActive() {
        sShutdownActive = true;
    }

    static public final boolean isShutdownActive() {
        return sShutdownActive;
    }

    static public final void commitNoCrash( final SharedPreferences.Editor editor ) {
        // Called from low priority thread OR background thread
        // However, still doing a switch to background thread here,
        // just in case future code accidentally calls this from main thread

        new BackgroundRunnable() {
            @Override
            public void run() {
                try {
                    editor.commit();
                } catch ( java.lang.OutOfMemoryError exception ) {
                    // the commit() function allocates more than 16384 bytes.
                    // When anyone leaks memory, it will usually crash here.
                    // Silently ignore the OutOfMemory here so that it is more likely to crash
                    //  at the actual place that is leaking memory.
                    try {
                        Log.e( CHECKIN_ERROR_TAG, "No memory for commit", exception );
                    } catch ( Exception e ) {
                        // Ignore any exception during logging
                    }
                }
            }
        };
    }

    public static final long getDayStartTimeMs() {
        return getDayStartTimeMs( System.currentTimeMillis() );
    }

    public static final long getDayStartTimeMs( long epochTimeMs ) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis( epochTimeMs );
        calendar.set(
                calendar.get( Calendar.YEAR ),
                calendar.get( Calendar.MONTH ),
                calendar.get( Calendar.DAY_OF_MONTH ),
                0, 0, 0 ); // at 00:00:00
        calendar.set( Calendar.MILLISECOND, 0 );

        return calendar.getTimeInMillis();
    }

    public static final String serializeObject( Object o ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
                oos = new ObjectOutputStream(baos);
                oos.writeObject( o );
                oos.close();
                return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT );
        } catch ( Exception e ) {
            Log.e( TAG, e.toString() );
            return null;
        }
    }

    public static final Object deSerializeObject( String encoded ) {
        if ( encoded == null ) return null;

        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream( Base64.decode( encoded, Base64.DEFAULT ) ) );
            return ois.readObject();
        } catch (Exception e) {
            Log.e( TAG, e.toString() );
            return null;
        }
    }

    public static final Context getContext() {
        return sContext;
    }

    public static final Handler getHandler() {
        return sHandler;
    }

    @SuppressWarnings("all")
    public static final boolean checkAndHandleTimeChange( String timeAction, String debugData ) {
        // Called from background thread
        long newBootWallClockTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        long timeShift = newBootWallClockTime - sBootWallClockTimeTc;
        if(Math.abs(timeShift) > THRESHOLD_FOR_TIME_CHANGE_MS){
            Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_3, "DC_TIMECHANGE",
                    Utilities.EVENT_LOG_VERSION, System.currentTimeMillis(), "timeshift",
                    String.valueOf(timeShift));
            sBootWallClockTimeTc =  newBootWallClockTime;
        }

        if ( LOGD && !TIMEZONE_REASON.equals( timeAction ) &&
                !ACCUMULATE_REASON.equals(timeAction) ) {
            timeShift = newBootWallClockTime - sBootWallClockTime;
            if ( Math.abs( timeShift ) <= MAX_TIMESHIFT_BEFORE_DISCARD_STATISTICS_MS ) {
                Log.d( TAG, "timeshift " + timeShift + " is small, ignoring!" );
            }
        }

        /*
         * If the user or network didn't shift the time much, don't discard the statistics
         *  we have already accumulated for today.
         */
        if ( TIMEZONE_REASON.equals( timeAction ) == true ||
                ( Math.abs( newBootWallClockTime - sBootWallClockTime ) >
                    MAX_TIMESHIFT_BEFORE_DISCARD_STATISTICS_MS ) ) {
            if ( Utilities.DEBUG_DUPLICATE_LOGS ) {
                debugData += ";ct=" + System.currentTimeMillis() +
                    ";ut=" + SystemClock.uptimeMillis() +
                    ";bw=" + sBootWallClockTime +
                    ";nb=" + newBootWallClockTime +
                    ";er=" + SystemClock.elapsedRealtime();
            }
            timeAction += debugData;
            // LogLimiter must be the first to be called here,
            //   so that DC_LOGOVERFLOW does not get reported
            LogLimiter.handleTimeChange( timeAction );
            TelephonyListener.handleTimeChange( timeAction );
            BatteryHandler.handleTimeChange( timeAction );
            sBootWallClockTime = newBootWallClockTime;
            return true;
        }
        return false;
    }

    public static abstract class BackgroundRunnable implements Runnable {
        public BackgroundRunnable() {
            if ( Thread.currentThread() == sBackgroundThread ) {
                run();
            } else {
                if ( sBackgroundHandler != null ) sBackgroundHandler.post( this );
            }
        }
    }

    public static final void reportBasic (String logTagLevel, String ID, String EventLogVersion,
            long time, String... keyValues )
    {
        int logLength = 4 + ID.length() + 5 + EventLogVersion.length() + 6 + 2; // 2 for ;]
        String [] kvpair = keyValues;

        CheckinEvent e1 = new CheckinEvent (logTagLevel, ID, EventLogVersion, time);
        if (kvpair != null) {
            int argLength = kvpair.length;
            for (int i = 0; i+1 < argLength; ) {
                String key = kvpair[i++];
                String value = kvpair[i++];
                if (value == null) {
                    value = "null";
                }
                e1.setValue(key, value);
                logLength += key.length() + 1 + value.length() + 1;  // key=value;
            }
        }

        Utilities.logEvent(logTagLevel, ID, logLength, e1);
    }
}
