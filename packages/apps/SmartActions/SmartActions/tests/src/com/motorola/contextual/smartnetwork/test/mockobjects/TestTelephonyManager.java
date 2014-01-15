/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 */

package com.motorola.contextual.smartnetwork.test.mockobjects;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.util.Log;

import com.motorola.android.wrapper.mock.MockTelephonyManager;

public class TestTelephonyManager extends MockTelephonyManager {
    private static final String TAG = TestTelephonyManager.class.getSimpleName();

    private final List<PhoneStateListener> mListenerList = new LinkedList<PhoneStateListener>();
    private static final long MAX_WAIT_TIME = 1000;

    public TestTelephonyManager(Context context) {
        super(context);
    }

    @Override
    public void listen(PhoneStateListener listener, int events) {
        synchronized(mListenerList) {
            if (listener != null) {
                if (events != PhoneStateListener.LISTEN_NONE) {
                    mListenerList.add(listener);
                } else {
                    mListenerList.remove(listener);
                }
            }
            mListenerList.notify();
        }
    }

    public PhoneStateListener getListener() {
        PhoneStateListener listener = null;
        synchronized(mListenerList) {
            if (mListenerList.isEmpty()) {
                try {
                    mListenerList.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "getListener() interrupted.");
                }
            }
        }
        if (!mListenerList.isEmpty()) {
            listener = mListenerList.remove(0);
        }
        return listener;
    }

}
