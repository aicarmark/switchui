package com.motorola.datacollection.telephony;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.TimerTask;

import com.motorola.datacollection.IntervalAccumulator;
import com.motorola.datacollection.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TelephonyListener extends PhoneStateListener {

    private static final String TAG = "DCE_TelephonyListener";
    private static final boolean LOGD = Utilities.LOGD;
    private static final boolean TESTING = false; // ODO
    private static final long SIGNALSTRENGTH_SAMPLE_INTERVAL_SECONDS =
        2 * 60 * 60; // every 2 hour interval
    private static final long WRITE_PREFERENCE_DELAY_MSEC = 10 * 60 * 1000; // 10 minutes
    private static final int INVALID_VALUE = -1;
    private static final int TOGGLE_RESET_VALUE = -1;
    private static final String TEST_LOG_REASON = "te";

    private static final int SIGNAL_STRENGTH_INVALID = INVALID_VALUE;
    private static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    private static final int SIGNAL_STRENGTH_POOR = 1;
    private static final int SIGNAL_STRENGTH_MODERATE = 2;
    private static final int SIGNAL_STRENGTH_GOOD = 3;
    private static final int SIGNAL_STRENGTH_GREAT = 4;
    private static final int SIGNAL_STRENGTH_RADIO_OFF = 5; // typically airplane mode
    private static final int SIGNAL_STRENGTH_END = 6; // Used as array size, 1 higher than max value

    private static int  sServiceState;
    private static int  sCurrentSignalStrengthBin;
    private static SignalStrengthAccumulator signalStrengthAccumulator;

    private static final int IGNORE_STATE_THRESHOLD_MSEC = 60 * 1000; // 60 seconds
    private static String sFinalState = "UNKNOWN";
    private static String sLastLoggedState = "";
    private static int sDataState = TelephonyManager.DATA_DISCONNECTED;
    private static int sDataNetworkType = INVALID_VALUE;
    private static int sToggleCount = -2;
    private static TimerTask sTimerTask;

    private static final void writePrefs( boolean force ) {
        // Called from background thread
        signalStrengthAccumulator.writePreferences( force );
    }

     @SuppressWarnings("all")
    synchronized public static final void startTelephonyListeners( Context context ) {
         // Called from background thread
        signalStrengthAccumulator = new SignalStrengthAccumulator();
        sServiceState = ServiceState.STATE_OUT_OF_SERVICE;
        sCurrentSignalStrengthBin = SIGNAL_STRENGTH_INVALID;

        // Unit Test Code - automatically compiled off when testing field is set to false.
        if ( TESTING == true ) {
            IntentFilter filter = new IntentFilter();
            filter.addAction( "com.motorola.datacollection.test.1" );
            filter.addAction( "com.motorola.datacollection.test.2" );
            context.registerReceiver(
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Log.d( TAG, "DBG intent " + intent );
                            if ( "com.motorola.datacollection.test.1".equals(
                                    intent.getAction() ) ) {
                                final int strength = intent.getIntExtra("strength", 0);
                                Log.d( TAG, "DBG_HandleSignalStrengthChanged " + strength );
                                onSignalStrengthsChangedImpl(
                                    new SignalStrength() {
                                        public boolean isGsm() { return true; }
                                        public int getGsmSignalStrength() { return strength; }
                                    }
                                );
                            } else if ( "com.motorola.datacollection.test.2".equals(
                                    intent.getAction() ) ) {
                                signalStrengthAccumulator.accumulate();
                                writePrefs(false);
                                signalStrengthAccumulator.logToCheckinImpl(TEST_LOG_REASON);
                            }
                        }
                    },
                    filter );
        }

        TelephonyManager t = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (t == null) {
            if (LOGD) { Log.d (TAG, "Can't listen to telephony events. Returning..."); }
            return;
        }
        t.listen( new TelephonyListener(),
                LISTEN_SERVICE_STATE | LISTEN_SIGNAL_STRENGTHS | LISTEN_DATA_CONNECTION_STATE );
    }

     @Override
     public final void onServiceStateChanged(ServiceState serviceState) {
         // Called from background thread
        if ( LOGD ) { Log.d( TAG, " got " + serviceState ); }
        super.onServiceStateChanged(serviceState);

        int newState = serviceState.getState();
        if ( newState == ServiceState.STATE_POWER_OFF ) {
            handleNewSignalStrengthBin( SIGNAL_STRENGTH_RADIO_OFF );
        }

        if ( newState == sServiceState ) return;

        sServiceState = newState;
        if ( sServiceState != ServiceState.STATE_IN_SERVICE ) {
            setDataState( TelephonyManager.DATA_DISCONNECTED );
        }

        logNetworkState();
    }

    // Function added just to please findbug
    private static final void setDataState( int newDataState ) {
        sDataState = newDataState;
    }

    @Override
    public final void onSignalStrengthsChanged(SignalStrength signalStrength) {
        // Called from background thread
        onSignalStrengthsChangedImpl( signalStrength );
    }

    private static final void onSignalStrengthsChangedImpl(SignalStrength signalStrength) {
        // Called from background thread
        int newStrengthBin;

        // Logic copied from BatteryStatsImpl.notePhoneSignalStrengthLocked
        if (!signalStrength.isGsm()) {
            int dBm = signalStrength.getCdmaDbm();
            if ( LOGD ) { Log.d( TAG, "signalStrength.getCdmaDbm() is " + dBm ); }

            if (dBm >= -75) newStrengthBin = SIGNAL_STRENGTH_GREAT;
            else if (dBm >= -85) newStrengthBin = SIGNAL_STRENGTH_GOOD;
            else if (dBm >= -95)  newStrengthBin = SIGNAL_STRENGTH_MODERATE;
            else if (dBm >= -100)  newStrengthBin = SIGNAL_STRENGTH_POOR;
            else newStrengthBin = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        } else {
            int asu = signalStrength.getGsmSignalStrength();
            if ( LOGD ) { Log.d( TAG, "signalStrength.getGsmSignalStrength() is " + asu ); }

            if (asu < 0 || asu >= 99) newStrengthBin = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            else if (asu >= 16) newStrengthBin = SIGNAL_STRENGTH_GREAT;
            else if (asu >= 8)  newStrengthBin = SIGNAL_STRENGTH_GOOD;
            else if (asu >= 4)  newStrengthBin = SIGNAL_STRENGTH_MODERATE;
            else newStrengthBin = SIGNAL_STRENGTH_POOR;
        }

        if ( LOGD ) { Log.d( TAG, "Got onSignalStrengthsChanged bin of " + newStrengthBin ); }

        if ( sServiceState != ServiceState.STATE_POWER_OFF ) {
            handleNewSignalStrengthBin( newStrengthBin );
        }
    }

    private static final void handleNewSignalStrengthBin( int newStrengthBin ) {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "Received Signal strength bin of " + newStrengthBin ); }

        if ( sCurrentSignalStrengthBin == newStrengthBin ) return;

        Utilities.checkAndHandleTimeChange( Utilities.ACCUMULATE_REASON, "");
        signalStrengthAccumulator.accumulate();
        sCurrentSignalStrengthBin = newStrengthBin;
        writePrefs(false);
    }

    @Override
    public void onDataConnectionStateChanged(int state, int networkType) {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, " got " + state + " " + networkType ); }
        super.onDataConnectionStateChanged(state, networkType);

        switch (state) {
        case TelephonyManager.DATA_CONNECTED:
        case TelephonyManager.DATA_DISCONNECTED:
            break;

        case TelephonyManager.DATA_CONNECTING:
        case TelephonyManager.DATA_SUSPENDED:
            state = TelephonyManager.DATA_DISCONNECTED;
            break;

        default:
            if ( LOGD ) { Log.d( TAG, "Invalid data connection state " + state ); }
            return;
        }

        if ( state == sDataState && networkType == sDataNetworkType ) return;

        setDataState( state );
        sDataNetworkType = networkType;
        logNetworkState();
    }

    void logNetworkState() {
        // Called from background thread
        String state;
        if ( sServiceState == ServiceState.STATE_POWER_OFF ) {
            state = "RADIO_OFF";
        } else if ( sDataState == TelephonyManager.DATA_DISCONNECTED ) {

            if ( sServiceState == ServiceState.STATE_OUT_OF_SERVICE ||
                    sServiceState == ServiceState.STATE_EMERGENCY_ONLY ) {
                state = "NO_NETWORK";
            } else {
                state = "VOICE";
            }

        } else  {
            switch ( sDataNetworkType ) {
            case TelephonyManager.NETWORK_TYPE_GPRS: state = "GPRS";    break;
            case TelephonyManager.NETWORK_TYPE_EDGE: state = "EDGE";    break;
            case TelephonyManager.NETWORK_TYPE_UMTS: state = "UMTS";    break;
            case TelephonyManager.NETWORK_TYPE_CDMA: state = "CDMA";    break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0: state = "EVDO_0";  break;
            case TelephonyManager.NETWORK_TYPE_EVDO_A: state = "EVDO_A";  break;
            case TelephonyManager.NETWORK_TYPE_EVDO_B: state = "EVDO_B";  break;
            case TelephonyManager.NETWORK_TYPE_1xRTT: state = "1xRTT";   break;
            case TelephonyManager.NETWORK_TYPE_HSDPA: state = "HSDPA";   break;
            case TelephonyManager.NETWORK_TYPE_HSUPA: state = "HSUPA";   break;
            case TelephonyManager.NETWORK_TYPE_HSPA: state = "HSPA";    break;
            case TelephonyManager.NETWORK_TYPE_HSPAP: state = "HSPAP";    break;
            case TelephonyManager.NETWORK_TYPE_IDEN: state = "IDEN";    break;
            case TelephonyManager.NETWORK_TYPE_EHRPD: state = "EHRPD";    break;
            case TelephonyManager.NETWORK_TYPE_LTE: state = "LTE";    break;
            default:
                state = "UNKNOWN" + sDataNetworkType;
                for (Field field :TelephonyManager.class.getFields()) {
                    if (field.getType().equals(int.class)) {
                        int modifiers = field.getModifiers();
                        try {
                            if (Modifier.isStatic(modifiers) &&
                                (field.getInt(null) == sDataNetworkType) &&
                                (field.getName().startsWith("NETWORK_TYPE_"))) {
                                state = field.getName();
                                state = state.substring("NETWORK_TYPE_".length());
                                if (LOGD) Log.d (TAG, "Network state read is "+state);
                                break;
                            }
                        } catch (Exception e) {
                            Log.e (TAG, "Network type"+sDataNetworkType, e);
                        }
                    }
                }
                break;
            }
        }
        if ( sFinalState.equals( state )) return;

        synchronized (this) {
            sToggleCount++;

            sFinalState = state;
            final long eventTimeMsec = System.currentTimeMillis();

            if ( sTimerTask != null ) sTimerTask.cancel();

            sTimerTask = new TimerTask() {
                public void run() {
                    synchronized( TelephonyListener.this ) {
                        if ( this == sTimerTask ) {
                            if ( !sLastLoggedState.equals( sFinalState ) ) {
                                String[] kvpair = new String[4];
                                kvpair[0] = "network";
                                kvpair[1] = sFinalState;
                                kvpair[2] = "missed";
                                kvpair[3] = Integer.toString(sToggleCount);
                                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_3, "DC_NETWORK", Utilities.EVENT_LOG_VERSION, eventTimeMsec, kvpair);
                                sLastLoggedState = sFinalState;
                                sToggleCount = TOGGLE_RESET_VALUE;
                            }
                            sTimerTask = null;
                        }
                    }
                }
            };

            Utilities.sTimer.schedule( sTimerTask, IGNORE_STATE_THRESHOLD_MSEC );
        }
    }

    public static final void handlePeriodicWrite() {
        // Called from background thread
        if ( LOGD ) Log.d(TAG, "handlePeriodicWrite" );
        signalStrengthAccumulator.accumulate();
        writePrefs( true );
    }

    public static final void handleTimeChange( String reason ) {
        // Called from background thread
        if ( LOGD ) Log.d(TAG, "handleTimeChange" );
        signalStrengthAccumulator.flushToCheckinAndReset( reason );
        signalStrengthAccumulator.accumulate();
        writePrefs( true );
    }

    // All functions in this class are called from background thread
    private static class SignalStrengthAccumulator extends IntervalAccumulator {
        private static final long serialVersionUID = 1L;

        SignalStrengthAccumulator() {
            super( SIGNALSTRENGTH_SAMPLE_INTERVAL_SECONDS, WRITE_PREFERENCE_DELAY_MSEC,
                    "SignalStrength", "SavedData" );
        }

        protected final void readFromStreamImpl(ObjectInputStream ois) throws Exception {
            if ( ois.readLong() != serialVersionUID ) {
                throw new InvalidObjectException(
                        "Reading SignalStrengthAccumulator from String failed");
            }
            super.readFromStreamImpl(ois);
        }

        protected final void writeToStreamImpl(ObjectOutputStream oos) {
            try {
                oos.writeLong( serialVersionUID );
                super.writeToStreamImpl(oos);
            } catch (IOException e) {
                Log.e( TAG, e.toString() );
            }
        }

        private static final long[] getLongData( HashMapWrapper hashMap ) {
            long[] data = (long[]) hashMap.get( "data" );
            if ( data == null ) {
                data = new long[SIGNAL_STRENGTH_END];
                hashMap.put( "data", data );
            }
            return data;
        }

        protected final void accumulateImpl(HashMapWrapper hashMap, long duration ) {
            if ( sCurrentSignalStrengthBin != SIGNAL_STRENGTH_INVALID ) {
                getLongData( hashMap )[sCurrentSignalStrengthBin] += duration;
            }
        }

        public final void logToCheckinImpl( String reason ) {
            if ( mLastTime == null ) return;

            StringBuilder sb = new StringBuilder ("");
            int hourInterval = 24 / mNumBins;
            for ( int i=0; i<mNumBins; i++ ) {
                long[] data = getLongData( mAccumulatedData[i] );

                if ( i != 0 ) sb.append( ",");

                sb.append( i * hourInterval )
                    .append( '-' )
                    .append( (i+1) * hourInterval );

                for ( int j=0; j<SIGNAL_STRENGTH_END; j++ ) {
                    sb.append( ',' ).append(data[j]);
                }
            }
            String[] kvpair = new String[4];
            kvpair[0] = "strengths";
            kvpair[1] = sb.toString();
            kvpair[2] = "re";
            kvpair[3] = reason;
            Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_1, "DC_SIGNALSTRENGTH", Utilities.EVENT_LOG_VERSION, mLastTime.dayStartMs, kvpair);
        }
    }
}
