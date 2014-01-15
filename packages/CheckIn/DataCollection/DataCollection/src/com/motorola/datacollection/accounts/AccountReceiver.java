package com.motorola.datacollection.accounts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

public class AccountReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_AccountReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String SHARED_PREFERENCE_FILE = "Account";
    private static final String SHARED_PREFERENCE_KEY = "Old";
    private static final String MOTOROLA_ACCOUNT_PREFIX = "com.motorola";
    private static boolean sPreferenceValid;
    private static HashMap<String,Integer> sAccounts;

    @Override
    public void onReceive(Context context, final Intent intent) {
     // Called from main thread
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(intent);
            }
        };
    }

    private final void onReceiveImpl(Intent intent) {
        // Called from background thread
        if (Watchdog.isDisabled()) return;

        if ( intent == null) {
            if ( LOGD ) { Log.d( TAG, "Null intent"); }
            return;
        }

        if ( LOGD ) { Log.d( TAG, "Received intent " + intent.toUri(0) ); }

        String action = intent.getAction();
        if ( action == null ||
                action.equals( AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION ) == false ) {
            if ( LOGD ) { Log.d( TAG, "Unexpected intent " ); }
            return;
        }

        checkAccountsChange();
    }

    public static final void handleBootComplete() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "handleBootComplete"); }
        checkAccountsChange();
    }

    private static final void logAccountDataToCheckin( String type, int currentCount, int delta ) {
        // Called from background thread
        Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_1, "DC_ACCOUNT", Utilities.EVENT_LOG_VERSION,
                System.currentTimeMillis(), "ac", type, "cc", Integer.toString(currentCount), "de",
                Integer.toString(delta));
    }

    private static final void checkAccountsChange() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "checkAccountsChange"); }
        readPreferences();

        HashMap<String,Integer> newAccounts = new HashMap<String,Integer>();
        for ( Account act : AccountManager.get( Utilities.getContext() ).getAccounts() ) {
            String type = act.type;
            if ( type.startsWith( MOTOROLA_ACCOUNT_PREFIX ) == false ) {
                Integer count = newAccounts.get( type );
                if ( count == null ) {
                    count = Integer.valueOf(1);
                } else {
                    count++;
                }
                newAccounts.put( type, count );
            }
        }

        for ( Entry<String, Integer> newEntry : newAccounts.entrySet() ) {
            String type = newEntry.getKey();
            Integer newCount = newEntry.getValue();

            Integer oldCount = sAccounts.remove( type );
            int delta = oldCount == null ?  newCount : newCount - oldCount;

            if ( delta != 0 ) logAccountDataToCheckin( type, newCount, delta );
        }

        for ( Entry<String, Integer> oldEntry: sAccounts.entrySet() ) {
            logAccountDataToCheckin( oldEntry.getKey(), 0, -oldEntry.getValue() );
        }

        sAccounts = newAccounts;
        writePreferences();
    }

    @SuppressWarnings("unchecked")
    private static final void readPreferences() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "readPreferences"); }

        if ( sPreferenceValid == false ) {
            sPreferenceValid = true;

            SharedPreferences pref =
                Utilities.getContext().getSharedPreferences( SHARED_PREFERENCE_FILE,
                        Context.MODE_PRIVATE);
            if ( pref != null ) {

                String encoded = pref.getString( SHARED_PREFERENCE_KEY, null);
                if ( encoded != null ) {

                    try {
                        ObjectInputStream ois = new ObjectInputStream(
                                new ByteArrayInputStream(
                                        Base64.decode( encoded, Base64.DEFAULT ) ) );
                        sAccounts = (HashMap<String,Integer>) ois.readObject();
                    } catch (Exception e) {
                        Log.d( TAG, Log.getStackTraceString(e) );
                    }
                }
            }

            if ( sAccounts == null ) sAccounts = new HashMap<String,Integer>();
        }
    }

    private static final void writePreferences() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "writePreferences"); }

        sPreferenceValid = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject( sAccounts );
            oos.close();
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT );
            SharedPreferences pref =
                Utilities.getContext().getSharedPreferences( SHARED_PREFERENCE_FILE,
                        Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString( SHARED_PREFERENCE_KEY, encoded );
            Utilities.commitNoCrash(edit);
        } catch ( Exception e ) {
            Log.d( TAG, Log.getStackTraceString(e) );
        }
    }
}
