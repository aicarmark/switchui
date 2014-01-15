package com.motorola.datacollection.pkg;

import java.util.List;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.motorola.android.provider.CheckinEvent;
import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.Utilities;

public class AppInfoLogger {
    private static final String TAG = "DCE_AppInfoLogger";
    private static final boolean LOGD = Utilities.LOGD;
    private static final long LOGGING_PERIOD_IN_MS = 7*24*60*60*1000; // 7 days
    private static final long MAX_TIME_DIFF_IN_MS = 2*24*60*60*1000; // 2 days
    private static final int NO_OPTIONAL_FLAGS = 0;
    private static final long MS_IN_HOUR = 60*60*1000; // number of ms in an hr
    private static long sNextLoggingTime = 0;
    private static final String APPINFO_EVENT_LOG_SEG0 = "DC_APPINFO";
    private static final String LOGGING_TIME = "sLoggingTime";
    private static final String PREFS_NAME = "AppInfoPrefsFile";
    private static long sPrevTime = 0;

    public static void initialize() {
        // Called from background thread

        if (LOGD) Log.v( TAG, "Initialize " );
        // Read logging_time from persistent memory
        SharedPreferences settings = Utilities.getContext().getSharedPreferences(PREFS_NAME,
            android.content.Context.MODE_PRIVATE);
        sNextLoggingTime = settings.getLong(LOGGING_TIME, 0);
        if ( sNextLoggingTime == 0 ) saveSharedPreferences();
        // Log the data incase the time for logging has already elapsed.
        handleTimerExpiry();
    }

    private static void saveSharedPreferences() {
        // Called from background thread

        // During powerdown store the next time that we should be logging data i.e. sNextLoggingTime.
        if ( LOGD ) Log.v( TAG, "saveSharedPreferences " );
        SharedPreferences settings = Utilities.getContext().getSharedPreferences(PREFS_NAME,
            android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (LOGD) Log.v(TAG,"Writing sNextLoggingTime, sNextLoggingTime ="+sNextLoggingTime);
        editor.putLong(LOGGING_TIME, sNextLoggingTime);
        Utilities.commitNoCrash(editor);
    }

    public static void handleTimerExpiry() {
        // Called from background thread

       // Log the data if we have hit the time for logging data.
       if (LOGD) Log.v( TAG, "**handleTimerExpiry: sNextLoggingTime = "+ sNextLoggingTime );
       long curTime = System.currentTimeMillis();
       if (LOGD) Log.v( TAG, "curTime = "+ curTime + " sPrevTime = " + sPrevTime );

       // Incase the user has changed the time by a large value, update the next logging
       // time so that data does not get collected unnecessarily.

       // sPrevTime != 0 : This check is for cases when phone is powered up first time,
       // to prevent incorrect large values of curTime - sPrevTime even though the user
       // has not changed the time.
       // Math.abs(curTime - sPrevTime) > MAX_TIME_DIFF_IN_MS : This check is to detect when the user has
       // changed the time by more than a couple of days and hence to avoid sending out the data
       // too soon.
       // Math.abs(curTime - sNextLoggingTime) > LOGGING_PERIOD_IN_MS : This check is
       // to detect scenarios where the user had changed the time and before the 1 hour
       // timer could expire, the phone had powered down, hence the next logging
       // time correction could not be applied.
       if( ( (sPrevTime != 0) && ( Math.abs(curTime - sPrevTime) > MAX_TIME_DIFF_IN_MS) )
           || (Math.abs(curTime - sNextLoggingTime) > LOGGING_PERIOD_IN_MS) ) {
           sNextLoggingTime = curTime + LOGGING_PERIOD_IN_MS;
           saveSharedPreferences();
           if (LOGD) Log.v( TAG, "Time change: restarting sNextLoggingTime = "+ sNextLoggingTime );
       }

       // Collect the data if the Logging time has been crossed.
       if(curTime > sNextLoggingTime)
       {
           logAppData();
           sNextLoggingTime = curTime + LOGGING_PERIOD_IN_MS;
           // Store the new logging time
           saveSharedPreferences();
       }

       if (LOGD) Log.v( TAG, "Updated sNextLoggingTime = "+ sNextLoggingTime );
       if (LOGD) Log.v( TAG, "Num hrs to log = "+ ( sNextLoggingTime - curTime ) / MS_IN_HOUR );
       if (LOGD) Log.v( TAG, "Num days to log  = "+ ( sNextLoggingTime - curTime ) / (24 * MS_IN_HOUR) );

       sPrevTime = curTime;

    }

   private static void logAppData() {

        // Log the Application name, version and UID corresponding to each of the packages.
        String title = "";
        CheckinEvent checkinEvent = new CheckinEvent(Utilities.LOG_TAG_LEVEL_2,
                       APPINFO_EVENT_LOG_SEG0, Utilities.EVENT_LOG_VERSION, System.currentTimeMillis());


        if (LOGD) Log.v( TAG, "logAppData " );
        PackageManager pm = Utilities.getContext().getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(NO_OPTIONAL_FLAGS);
        for (PackageInfo pinfo : packageInfos) {
            ApplicationInfo ainfo = pinfo.applicationInfo;
            if(pm.getApplicationLabel(ainfo) != null) {
                // TODO: CLEANUP
                title = pm.getApplicationLabel(ainfo).toString();
            }
            else {
                title = "";
            }
            Segment segment = new Segment("appinfo");
            if (pinfo.packageName == null) {
                segment.setValue(1, "null");
            } else {
                segment.setValue(1, pinfo.packageName);
            }
            if (pinfo.versionName == null) {
                segment.setValue(2, "null");
            } else {
                segment.setValue(2, pinfo.versionName);
            }
            if (title == null) {
                segment.setValue(3, "null");
            } else {
                segment.setValue(3, title);
            }
            segment.setValue(4, ainfo.uid);
            // Log the package, app version, app name and app uid

            if (LOGD) Log.v(TAG,"pkg="+pinfo.packageName+";av="+pinfo.versionName
                +";an="+title+";au="+ainfo.uid);
            checkinEvent.addSegment(segment);
        }
        Utilities.logPriorityEvent( checkinEvent );
    }

   }

