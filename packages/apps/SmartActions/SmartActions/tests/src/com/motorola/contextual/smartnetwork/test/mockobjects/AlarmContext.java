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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.test.mock.MockContext;
import android.util.Log;

public class AlarmContext extends MockContext {
    private static final String TAG = AlarmContext.class.getSimpleName();

    private static final long MAX_BROADCAST_WAIT_TIME = 1000;

    /* base context */
    private final Context mBaseContext;

    // broadcast intent list
    private final List<Intent> mIntentList = new LinkedList<Intent>();

    public AlarmContext(Context context) {
        mBaseContext = context;
    }

    @Override
    public Object getSystemService(String name) {
        return mBaseContext.getSystemService(name);
    }

    @Override
    public String getPackageName() {
        return mBaseContext.getPackageName();
    }

    @Override
    public ContentResolver getContentResolver() {
        return mBaseContext.getContentResolver();
    }

    @Override
    public void sendBroadcast(Intent intent) {
        // some components send broadcasts on background thread
        synchronized(mIntentList) {
            if (intent != null) {
                // hijack the broadcast intent
                mIntentList.add(intent);
            }
            mIntentList.notify();
        }
    }

    /**
     * Returns the oldest received intent and removes it off the broadcast list
     * @return the oldest received intent
     */
    public Intent receiveBroadcast() {
        Intent intent = null;
        // some components send broadcasts on background thread so we have to wait
        synchronized(mIntentList) {
            if (mIntentList.isEmpty()) {
                try {
                    mIntentList.wait(MAX_BROADCAST_WAIT_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "mIntentList.wait interrupted.");
                }
            }
        }
        if (!mIntentList.isEmpty()) {
            intent = mIntentList.remove(0);
        }
        return intent;
    }

    /**
     * @return true is there are intents to be broadcasted, false otherwise
     */
    public boolean hasIntents() {
        return !mIntentList.isEmpty();
    }

    /**
     * @return number of intents left to be broadcasted
     */
    public int countIntents() {
        return mIntentList.size();
    }

    public void cleanup() {
        // clear all intents to be broadcasted
        mIntentList.clear();
    }

}
