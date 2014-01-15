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

package com.motorola.android.wrapper;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * A wrapper for {@link android.os.PowerManager} class.
 * All of its calls are delegated to the PowerManager class.
 * Can be subclassed to modify behavior for tesing or mocking purposes.
 */
public class PowerManagerWrapper {
    protected final PowerManager mPowerManager;

    public PowerManagerWrapper(Context context) {
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    /**
     * {@link PowerManager#goToSleep}
     */
    public void goToSleep(long time) {
        mPowerManager.goToSleep(time);
    }

    /**
     * {@link PowerManager#isScreenOn}
     */
    public boolean isScreenOn() {
        return mPowerManager.isScreenOn();
    }

    /**
     * {@link PowerManager#newWakeLock}
     */
    public WakeLock newWakeLock(int flags, String tag) {
        return mPowerManager.newWakeLock(flags, tag);
    }

    /**
     * {@link PowerManager#reboot}
     */
    public void reboot(String reason) {
        mPowerManager.reboot(reason);
    }

    /**
     * {@link PowerManager#userActivity}
     */
    public void userActivity(long when, boolean noChangeLights) {
        mPowerManager.userActivity(when, noChangeLights);
    }
}
