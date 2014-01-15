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

import android.content.Context;

import com.motorola.android.wrapper.mock.MockPowerManager;

public class TestPowerManager extends MockPowerManager {
    private boolean mScreenOn = false;

    public TestPowerManager(Context context) {
        super(context);
    }

    @Override
    public boolean isScreenOn() {
        return mScreenOn;
    }

    public void setScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
    }

}
