/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 * Class name: MEDialer.java
 * Description: Please see the class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 12-Apr-2011    a19121       Init ME log feature
 **********************************************************
 */
package com.motorola.contacts.util; //MOTO Dialer Code IKHSS6-723

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.R;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import android.util.Log;


/**
 * MEDialer will cook original information and pass formated stuff to MELogger.
 *
 * @author a19121
 */
public class MEDialer {
    private static final String LOG_TAG = "MEDialer";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    private static final String TAG_ID_CALL_VM_VIA_DIALER = "ID=CALL_VM_VIA_DIALER;";
    private static final String TAG_ID_CALL_VVM_VIA_DIALER = "ID=CALL_VVM_VIA_DIALER;";

    public  static boolean isSmartDialer = false;
    public  static boolean isQwertyKeypad = false;

    // static for MELogger initialization state
    public static MELogger mMELogger = null;
    public static boolean mLaunched = false;

    public  static final String TAG_CALL_SOURCE = "dialer";
    public  static final String TAG_SMARTDIALER = "smartdialer";

    // Dialer who placed MO call.
    public enum DialFrom {
        UNKNOWN,          // unknown dialer type. used for any applications without supporting call ME
        DIALERKEY,        // dial pad
        RECENT,           // recent call lists
        CONTACTS,         // contacts
        FAVORITES,        // favorite lists
        SPEEDDIAL,        // speed dial
        BTREDIAL,         // bluetooth redial
        BTCMD,            // bluetooth command
        MPC,              // multipart call
        CONTACTSEARCH     // search contacts option in twelvekey dialer
    }

    // keypad type used for smartdialer. now always SOFT. because android limit
    public enum SmartDialFrom {
        SOFT,             // soft keypad
        QWERTY            // qwerty keyboard
    }

    public static boolean inited(Context context) {
        if (!mLaunched ) {
            mLaunched = true;
            if (context.getResources().getBoolean(R.bool.ftr_36344_meaningful_event_log) ) {
                mMELogger = MELogger.createSingleInstance(context);
            }
        }
        return (mMELogger != null);
    }

    // intent: will be modified to include dialer information.
    public static void onDial(final Context context, Intent intent, final DialFrom dialFrom) {
        if (inited(context)) {
            if(DBG) log(" <- onDialer -> dialer:" + dialFrom);
            intent.putExtra(TAG_CALL_SOURCE, dialFrom.name());
            // clear smardialer
            isSmartDialer = false;
            isQwertyKeypad = false;
        }
    }

    // intent: will be modified to include dialer information.
    public static void onDial(final Context context, Intent intent, final SmartDialFrom smartDialFrom) {
        if (inited(context)) {
            if(DBG) log(" <- onDialer -> smartdialer:" + smartDialFrom);
            intent.putExtra(TAG_CALL_SOURCE, DialFrom.DIALERKEY.name());
            intent.putExtra(TAG_SMARTDIALER, smartDialFrom.name());
            // clear smardialer
            isSmartDialer = false;
            isQwertyKeypad = false;
        }
    }

    // intent: will be modified to include dialer information.
    public static void copyDialerInfoExtra(final Context context, final Intent intent, Intent newIntent) {
        if (inited(context)) {
            if (intent.hasExtra(TAG_CALL_SOURCE)) {
                newIntent.putExtra(TAG_CALL_SOURCE,
                    intent.getStringExtra(TAG_CALL_SOURCE));
            }
            if (intent.hasExtra(TAG_SMARTDIALER)) {
                newIntent.putExtra(TAG_SMARTDIALER,
                    intent.getStringExtra(TAG_SMARTDIALER));
            }
        }
    }

    public static void setSmartDialer(final boolean isUsed){
        isSmartDialer = isUsed;
    }

    public static boolean getSmartDialer(){
        return isSmartDialer;
    }

    public static void setQwertyKeypad(final boolean isUsed){
        isQwertyKeypad = isUsed;
    }

    public static boolean getQwertyKeypad(){
        return isQwertyKeypad;
    }

    public static void onVVM(final Context context){
        if (inited(context)) {
            if(DBG) log(" <- onVVM -> ");
            //[ID=CALL_VM_VIA_DIALER; ver=11.11; time=2147483647; ]
            MELogger.MEValueBuilder valueBuilder = new MELogger.MEValueBuilder();
            valueBuilder.open()
                .appendRaw(TAG_ID_CALL_VVM_VIA_DIALER)
                .appendRaw(MELogger.TAG_VER_CURRENT)
                .append(MELogger.TAG_TIME, SystemClock.elapsedRealtime())
                .close();

            MELogger.logEvent(MELogger.CHECKIN_TAG_CALL_L1, valueBuilder.toString());
        }
    }

    public static void onVM(final Context context){
        if (inited(context)) {
            if(DBG) log(" <- onVM -> ");
            //[ID=CALL_VM_VIA_DIALER; ver=11.11; time=2147483647; ]
            MELogger.MEValueBuilder valueBuilder = new MELogger.MEValueBuilder();
            valueBuilder.open()
                .appendRaw(TAG_ID_CALL_VM_VIA_DIALER)
                .appendRaw(MELogger.TAG_VER_CURRENT)
                .append(MELogger.TAG_TIME, SystemClock.elapsedRealtime())
                .close();

            MELogger.logEvent(MELogger.CHECKIN_TAG_CALL_L1, valueBuilder.toString());
        }
    }

    // print log with local tag
    private static final void log(final String text) {
        Log.d(LOG_TAG, text);
    }

}

