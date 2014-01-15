/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 * Class name: MELogger.java
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
import android.content.Context;
import android.content.ContentResolver;
import android.os.Process;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;


/**
 * ME logger is a thread container to log meaningful event in background.
 *
 * @author a19121
 */
public class MELogger {
    // debuger
    private static final String LOG_TAG = "MELogger";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    // configuration
    private static final String BACKGROUND_THREAD_NAME = "MELogger";
    // internal event ID
    private static final int ME_LOG_EVENT_ID = 1;

    // CHECKIN event TAG
    public static final String CHECKIN_TAG_CALL_L1 = "MOT_CALL_STATS_L1";
    public static final String CHECKIN_TAG_CALL_L2 = "MOT_CALL_STATS_L2";
    public static final String CHECKIN_TAG_CALL_L3 = "MOT_CALL_STATS_L3";
    public static final String CHECKIN_TAG_CALL_L4 = "MOT_CALL_STATS_L4";
    public static final String CHECKIN_TAG_CALL_L5 = "MOT_CALL_STATS_L5";
    public static final String CHECKIN_TAG_CALL_L6 = "MOT_CALL_STATS_L6";

    public static final String TAG_VER_CURRENT = "ver=0.1;";

    public static final String TAG_TIME = "time";
    public static final String TAG_TYPE = "type";

    // working variables
    private MESyncHandler mWorkHandler = null;
    private HandlerThread mWorkThread = null;
    private Context mContext = null;
    private static MELogger mMELogger = null;

    private MELogger(Context context) {
        mContext = context;
        mWorkThread = new HandlerThread(BACKGROUND_THREAD_NAME,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mWorkThread.start();
        mWorkHandler = new MESyncHandler(mWorkThread.getLooper());
    }

    //create singleinstance of MElogger
    // 1. create and return the MELogger instance if it is not created yet
    // 2. return the MElogger instance if it is already existing
    public static MELogger createSingleInstance(Context context) {
        if (mMELogger == null) {
            mMELogger = new MELogger(context);
        }
        return mMELogger;
    }

    // create async event to pass the ME to background thread
    public static void logEvent(final String tag, final String value) {
        if (mMELogger != null) {
            Message msg = Message.obtain(mMELogger.mWorkHandler, ME_LOG_EVENT_ID, new MELogItem(tag, value));
            log("sendMEMsg = "+ msg);
            msg.sendToTarget();
        }
    }

    // To keep a good performence habit, check this instead of motosetting.
    public static boolean isEnabled() {
        return (mMELogger != null);
    }

    // to keep same debug TAG in log file
    private static final void log(final String text) {
        if (DBG) Log.d(LOG_TAG, text);
    }

    // a structure to contain ME in a message
    static class MELogItem {
        /* package */ MELogItem(final String tag, final String value) {
            mTag = tag;
            mValue = value;
        }
        /* package */ String mTag;
        /* package */ String mValue;
    }

    // handler to access checkin database
    /* package */ class MESyncHandler extends Handler{
        /* package */ MESyncHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case ME_LOG_EVENT_ID:
                    final ContentResolver cr = mContext.getContentResolver();
                    /* to-pass-build, Xinyu Liu/dcjf34 */ 
                    //Checkin.logEvent(cr, ((MELogItem)msg.obj).mTag, ((MELogItem)msg.obj).mValue);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    // a helper to easy your code to generate ME value
    public static class MEValueBuilder{
        // This helps to create most efficient StringBuilder for most ME.
        public static final int MOST_ME_VALUE_LENGTH = 128;

        // common signs in ME value
        public static final char ME_TAG_BEGIN = '[';
        public static final char ME_TAG_END = ']';
        public static final char ME_TAG_ASSIGN = '=';
        public static final char ME_TAG_SEPERATOR = ';';

        // MEValueBuilder is utilitied by StringBuiler
        public StringBuilder mStringBuilder;
        // default constructor should be good to most cases
        public MEValueBuilder() {
            mStringBuilder = new StringBuilder(MOST_ME_VALUE_LENGTH);
        }
        // special constructor should be good to rare cases who need long ME
        public MEValueBuilder(int capacity) {
            mStringBuilder = new StringBuilder(capacity);
        }

        // output the result
        public String toString(){
            return mStringBuilder.toString();
        }

        // first thing of ME value
        public MEValueBuilder open() {
            mStringBuilder.append(ME_TAG_BEGIN);
            return this;
        };

        // last thing of ME value
        public MEValueBuilder close() {
            mStringBuilder.append(ME_TAG_END);
            return this;
        };

        // append pure string to ME value. so that caller can decide everything
        public MEValueBuilder appendRaw(final String str) {
            mStringBuilder.append(str);
            return this;
        };

        // append value with pair of tag and value text
        public MEValueBuilder append(final String tag, final String value) {
            mStringBuilder.append(tag).append(ME_TAG_ASSIGN)
                          .append(value).append(ME_TAG_SEPERATOR);
            return this;
        };

        // append value with pair of tag and value number
        public MEValueBuilder append(final String tag, long value) {
            mStringBuilder.append(tag).append(ME_TAG_ASSIGN)
                          .append(value).append(ME_TAG_SEPERATOR);
            return this;
        };
    }
}
