package com.motorola.datacollection.battery;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.motorola.datacollection.IntervalAccumulator;
import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

public class BatteryHandler extends BroadcastReceiver {

    private static final String TAG = "DCE_BatteryHandler";
    private static final boolean LOGD = Utilities.LOGD;
    private static final boolean TESTING = false; // ODO

    @SuppressWarnings("unused")
    private static final int BATTERY_STATE_UNKNOWN=0;
    private static final int BATTERY_STATE_ON=1;
    private static final int BATTERY_STATE_OFF=2;
    private static final long BATTERY_SAMPLE_INTERVAL_SECONDS =
        2 * 60 * 60; // every 2 hour interval
    private static final long WRITE_PREFERENCE_DELAY_MSEC = 10 * 60 * 1000; // 10 minutes
    private static final String TEST_LOG_REASON = "te";

    private static int sBatteryState;
    private static long sBatteryConnectUptime;
    private static BatteryHandler sBatteryChangedReceiver;
    private static BatteryAccumulator sBatteryAccumulator;

    synchronized public static final void initialize() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "initialize" ); }

        sBatteryAccumulator = new BatteryAccumulator();
        sBatteryChangedReceiver = new BatteryHandler();

        // ACTION_BATTERY_CHANGED will not be received, if it is declared in
        //   AndroidManifest.xml. So we create a dynamic broadcast receiver
        //   with the ACTION_BATTERY_CHANGED intent added to the intent filter.
        Utilities.getContext().registerReceiver( sBatteryChangedReceiver,
                new IntentFilter( Intent.ACTION_BATTERY_CHANGED ) );
    }

    @Override
    public final void onReceive(Context context, final Intent intent) {
        // Called from main thread
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(intent);
            }
        };
    }

    @SuppressWarnings("all")
    private static final void onReceiveImpl(Intent intent) {
        // Called from background thread
        if (Watchdog.isDisabled()) return;

        if ( intent == null ) {
            if ( LOGD ) { Log.d( TAG, "Received null intent" ); }
            return;
        }

        if ( LOGD ) { Log.d( TAG, "onReceive " + intent.toUri(0) ); }

        String action = intent.getAction();
        if ( action == null ) return;

        sBatteryAccumulator.accumulate();

        if ( action.equals(Intent.ACTION_POWER_CONNECTED) ) {
            setBatteryState( BATTERY_STATE_OFF );
        } else if ( action.equals(Intent.ACTION_POWER_DISCONNECTED) ) {
            setBatteryState( BATTERY_STATE_ON );
        } else if ( action.equals( Intent.ACTION_BATTERY_CHANGED ) ) {
            handleBatteryChanged( intent );
        }

        // Unit Test Code - automatically compiled off when testing field is set to false.
        if ( TESTING == true ) {
            if ( action.equals("com.motorola.datacollection.battery.test.1") ) {
                Intent i = new Intent( "com.motorola.datacollection.battery.test.1.result" );
                i.putExtra( "data", sBatteryAccumulator.getCheckinString() );
                Utilities.getContext().sendBroadcast( i );
                sBatteryAccumulator.logToCheckinImpl(TEST_LOG_REASON);
            }
        }

        writePreferences( false );
    }

    private static final void setBatteryState( int newBatteryState ) {
        // Called from background thread
        sBatteryState = newBatteryState;
    }

    private static final int getBatteryState() {
        // Called from background thread
        return sBatteryState;
    }

    synchronized private static final void handleBatteryChanged( Intent intent ) {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "HandleBatteryChanged" ); }

        if ( sBatteryChangedReceiver != null ) {
            Utilities.getContext().unregisterReceiver( sBatteryChangedReceiver );
            sBatteryChangedReceiver = null;
        }
        int plugged = intent.getIntExtra( BatteryManager.EXTRA_PLUGGED, -1);
        if ( plugged == -1 ) {
            if ( LOGD ) { Log.d( TAG, "HandleBatteryChanged: not plugged" ); }
            return;
        }

        setBatteryState( plugged == 0 ? BATTERY_STATE_ON : BATTERY_STATE_OFF );

        if ( LOGD ) { Log.d( TAG, "Battery state initialized to " + getBatteryState()  +
                " batteryConnectUptime to " + sBatteryConnectUptime ); }
    }

    private static final void writePreferences( boolean force ) {
        // Called from background thread
        sBatteryAccumulator.writePreferences( force );
    }

    public static final void handlePeriodicWrite() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "handlePeriodicWrite" ); }
        sBatteryAccumulator.accumulate();
        writePreferences( true );
    }

    public static final void handleTimeChange( String reason ) {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "handleTimeChange" ); }
        sBatteryAccumulator.flushToCheckinAndReset( reason );
        sBatteryAccumulator.accumulate();
        writePreferences( true );
    }

    // All functions in this class are called from background thread
    static class BatteryAccumulator extends IntervalAccumulator {

        private static final long serialVersionUID = 1L;
        private long mUptimeLeft;
        private static final int REALTIME_INDEX = 0;
        private static final int UPTIME_INDEX = 1;

        BatteryAccumulator() {
            super( BATTERY_SAMPLE_INTERVAL_SECONDS, WRITE_PREFERENCE_DELAY_MSEC,
                    "Battery", "Data" );
        }

        public final void accumulate() {
            long currentUptime = SystemClock.uptimeMillis();
            mUptimeLeft = currentUptime - sBatteryConnectUptime;
            super.accumulate( System.currentTimeMillis() );
            sBatteryConnectUptime = currentUptime;
        }

        private final long[] getLongData( HashMapWrapper hashMap ) {
            long[] data = (long[]) hashMap.get( "data" );
            if ( data == null ) {
                data = new long[2];
                hashMap.put( "data", data );
            }
            return data;
        }

        protected final void readFromStreamImpl(ObjectInputStream ois) throws Exception {
            boolean success = false;

            do {
                if ( ois.readLong() != serialVersionUID ) break;

                int serialBatteryState = ois.readInt();
                long serialBatteryConnectUptime = ois.readLong();
                long serialBootWallClockTime = ois.readLong();

                super.readFromStreamImpl(ois);

                long bootWallClockTimeMs =
                    System.currentTimeMillis() - SystemClock.elapsedRealtime();
                // check if boot time is different
                if ( Math.abs( serialBootWallClockTime - bootWallClockTimeMs ) < 5 * 1000 ) {
                    setBatteryState( serialBatteryState );
                    sBatteryConnectUptime = serialBatteryConnectUptime;
                }

                success = true;
            } while ( false );

            if ( success == false ) {
                throw new InvalidObjectException( "Reading BatteryAccumulator from String failed");
            }
        }

        protected final void writeToStreamImpl(ObjectOutputStream oos) {
            try {
                oos.writeLong( serialVersionUID );
                oos.writeInt( getBatteryState() );
                oos.writeLong( sBatteryConnectUptime );
                oos.writeLong( System.currentTimeMillis() - SystemClock.elapsedRealtime() );

                super.writeToStreamImpl(oos);
            } catch (IOException e) {
                Log.e( TAG, e.toString() );
            }
        }

        protected final void accumulateImpl(HashMapWrapper hashMap, long duration ) {
            long[] data = getLongData( hashMap );
            if ( getBatteryState() == BATTERY_STATE_ON ) {
                data[REALTIME_INDEX] += duration;
                long thisUptime = ( mUptimeLeft >= duration ) ? duration : mUptimeLeft;
                data[UPTIME_INDEX] += thisUptime;
                mUptimeLeft -= thisUptime;
            }
        }

        private final String getCheckinString( ) {
            if ( mLastTime == null ) return "";

            StringBuilder sb = new StringBuilder( "" );
            int hourInterval = (int) (mIntervalSeconds / ( 60 * 60 ));
            for ( int i=0; i<mNumBins; i++ ) {
                long[] data = getLongData( mAccumulatedData[i] );

                if ( i != 0 ) sb.append( ",");

                sb.append( i * hourInterval ).
                append( '-' ).
                append( (i+1) * hourInterval ).
                append( ',' ) .
                append( data[0] ) .
                append( ',' ) .
                append( data[1] );
            }
            return sb.toString();
        }

        public final void logToCheckinImpl( String reason ) {
            Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_1, "DC_BATTERY",
                    Utilities.EVENT_LOG_VERSION, mLastTime.dayStartMs, "realtimeuptime",
                    getCheckinString(), "re", reason);
        }
    }
}
