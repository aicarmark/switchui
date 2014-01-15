/**
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */
package com.motorola.devicestatistics;

import android.os.PowerManager;
import android.content.Context;

final class ScreenStateSaver {
    static final int UNKNOWN = -1;
    static final int ON = 0;
    static final int OFF = 1;

    int mLastScreenState = UNKNOWN;
    PowerManager pm;

    ScreenStateSaver( Context context ) {
        synchronized(ScreenStateSaver.class) {
            if (pm == null) pm = (PowerManager)context.getSystemService( Context. POWER_SERVICE );
        }
    }

    final int getScreenState() {
        return pm.isScreenOn() ? ON : OFF;
    }

    final int getLastScreenState() {
        return mLastScreenState;
    }

    final void setLastScreenState(int lastState) {
        mLastScreenState = lastState;
    }

    final void setLastScreenState() {
        mLastScreenState = getScreenState();
    }

    final int getAndSetScreenState() {
        int oldState = mLastScreenState;
        setLastScreenState();
        return oldState;
    }
}
