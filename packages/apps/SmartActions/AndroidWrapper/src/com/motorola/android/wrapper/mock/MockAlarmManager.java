/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/04   IKCTXTAW-479    Initial version
 */

package com.motorola.android.wrapper.mock;

import android.app.PendingIntent;
import android.content.Context;

import com.motorola.android.wrapper.AlarmManagerWrapper;

/**
 * A mock {@link com.motorola.system.MotoAlarmManager} class.
 * All methods are non-functional and throw {@link java.lang.UnsupportedOperationException}.
 * You can use this to inject other dependencies, mocks, or monitors into the classes you are
 * testing.
 */
public class MockAlarmManager extends AlarmManagerWrapper {

    public MockAlarmManager(Context context) {
        super(context);
    }

    @Override
    public void cancel(PendingIntent operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int type, long triggerAtTime, PendingIntent operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInexactRepeating(int type, long triggerAtTime, long interval,
                                    PendingIntent operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRepeating(int type, long triggerAtTime, long interval, PendingIntent operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTime(long millis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeZone(String timeZone) {
        throw new UnsupportedOperationException();
    }
}