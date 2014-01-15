package com.motorola.datacollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.util.Log;

import com.motorola.android.provider.CheckinEvent;
import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.accessory.AccessoryReceiver;
import com.motorola.datacollection.battery.BatteryHandler;
import com.motorola.datacollection.configinfo.ConfigInfoBroadcastReceiver;
import com.motorola.datacollection.pkg.AppInfoLogger;
import com.motorola.datacollection.sdcard.SdCardBroadcastReceiver;
import com.motorola.datacollection.telephony.TelephonyListener;

public class DataCollectionApplication extends Application {
    private static final String TAG = "DCE_App";
    private static final boolean LOGD = Utilities.LOGD;

    private static final String MY_PROCESS_NAME = "com.motorola.process.system";
    private static final String SYSTEM_UID_NAME = "system";
    private static final long SLEEP_AFTER_SIGNAL_MS = 10 * 1000; // 10 secs
    private static final String PREFERENCE_FILE = "errors";
    private static final String MULTIPLE_PROCESS_COUNT = "multipleproc.count";
    private static final String MULTIPLE_PROCESS_LASTLOG = "multipleproc.lastlog";
    private static final long ERROR_HOLD_TIME_MS = 24 * 60 * 60 * 1000; // 1 day

    private static boolean sCreateCompleted;

    @Override
    public void onCreate() {
        // This log is intentionally enabled, and indicates whether the datacolletion
        //   code is active.
        Log.d( TAG, "active");

        super.onCreate();

        if ( sCreateCompleted == true ) return;
        sCreateCompleted = true;

        Utilities.initialize( this );

        new Utilities.BackgroundRunnable() {
            public void run() {
                Watchdog.initialize(DataCollectionApplication.this);
                if (Watchdog.isDisabled()) return;

                LogLimiter.initialize();
                TelephonyListener.startTelephonyListeners( DataCollectionApplication.this );
                BatteryHandler.initialize();
                SdCardBroadcastReceiver.initialize();
                AppInfoLogger.initialize();

                AccessoryReceiver.initialize();
                ConfigInfoBroadcastReceiver.initialize(DataCollectionApplication.this);
                DataCollectionSelfService.startMe();
                DataCollectionReceiver.initialize();
                killMyOldProcesses();
            }
        };
    }

    private final void killMyOldProcesses() {
        // Create a new low priority thread to kill any duplicate processes
        if ( LOGD ) Log.d( TAG, "Creating thread to kill my old processes" );
        Thread oldProcessKiller = new Thread() {
            @Override
            public final void run() {
                killMyOldProcessesImpl();
            }
        };

        oldProcessKiller.setPriority( Thread.MIN_PRIORITY );
        oldProcessKiller.start();
    }

    private final void killMyOldProcessesImpl() {
        // Called from low priority thread
        if ( LOGD ) Log.d( TAG, "Entered killMyOldProcessesImpl" );

        String myPid = String.valueOf(Process.myPid());
        int myUid = Process.myUid();
        if ( myUid != Process.SYSTEM_UID ) {
            Log.e( TAG, "My uid should be  " + Process.SYSTEM_UID + ", but is " + myUid );
            return;
        }

        java.lang.Process process = null;
        BufferedReader reader = null;
        boolean selfFound = false;
        int oldPid = -1;
        Pattern whitespace = Pattern.compile("\\s+");

        try {
            if ( LOGD ) Log.d( TAG, "Searching for old " + MY_PROCESS_NAME );

            process = Runtime.getRuntime().exec( "ps" );
            process.getOutputStream().close();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (true) {
                String line = reader.readLine();
                if ( line == null ) break;

                // Search for the following line
                // system    1370  1219  800    368   c02... afd... S com.motorola.process.system
                if ( line.endsWith(MY_PROCESS_NAME)) {
                    String[] fields = whitespace.split(line,3);
                    if ( fields.length == 3 && SYSTEM_UID_NAME.equals(fields[0]) ) {
                        if ( myPid.equals( fields[1] ) ) {
                            selfFound = true;
                        } else {
                            oldPid = Integer.valueOf(fields[1]);
                        }
                    }
                }
            }

            if ( LOGD ) Log.d( TAG, "sf=" + selfFound + " op=" + oldPid );

            // This is a sanity check. If I cant find myself, then perhaps "ps" has changed its
            // output format. So dont do any processing in that case.
            if ( selfFound == false ) {
                Log.e( TAG, "I am invisible!" );
                return;
            }

            if ( oldPid == -1 ) {
                if ( LOGD ) Log.d( TAG, "No old processes to kill" );
                return;
            }

            if ( LOGD ) Log.d( TAG, "Send SIGNAL_QUIT to " + oldPid );
            Process.sendSignal(oldPid, Process.SIGNAL_QUIT);
            Thread.sleep( SLEEP_AFTER_SIGNAL_MS );

            if ( LOGD ) Log.d( TAG, "Send SIGNAL_KILL to " + oldPid );
            Process.sendSignal(oldPid, Process.SIGNAL_KILL);

            SharedPreferences pref = getSharedPreferences( PREFERENCE_FILE, Context.MODE_PRIVATE );
            if ( pref != null ) {
                int errors = 1 + pref.getInt( MULTIPLE_PROCESS_COUNT, 0 ); // cumulative
                long lastLogTime = pref.getLong( MULTIPLE_PROCESS_LASTLOG, 0 );
                long currentTime = System.currentTimeMillis();

                SharedPreferences.Editor edit = pref.edit();
                if ( edit != null ) {
                    edit.putInt( MULTIPLE_PROCESS_COUNT, errors );

                    // Dont send more than 1 report per day
                    if ( Math.abs( currentTime - lastLogTime ) > ERROR_HOLD_TIME_MS ) {
                        edit.putLong( MULTIPLE_PROCESS_LASTLOG, currentTime );

                        // Create a log like
                        //   MOT_CA_STATS_L1|[ID=DC_ERRORS;ver=0.33;time=1317360031141;][mp;1;]
                        try {
                            CheckinEvent event = new CheckinEvent (Utilities.LOG_TAG_LEVEL_1, "DC_ERRORS", Utilities.EVENT_LOG_VERSION, currentTime);
                            Segment segment1 = new Segment ("mp");
                            segment1.setValue(1, errors);
                            event.addSegment(segment1);
                            ContentResolver cr = this.getContentResolver();
                            event.publish(cr);
                        } catch (IllegalArgumentException e) {
                        }
                    }
                    Utilities.commitNoCrash( edit );
                }
            }
        } catch ( Exception e ) {

            Log.e( TAG, Log.getStackTraceString(e));

        } finally {

            try {
                if ( reader != null ) reader.close();
            } catch ( IOException e ) {
            }

            // Commenting because of weird logcat logs
            // if ( process != null ) process.destroy();
        }
    }
}
