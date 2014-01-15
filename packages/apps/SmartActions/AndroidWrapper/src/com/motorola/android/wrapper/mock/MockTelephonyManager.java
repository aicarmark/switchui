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

import java.util.List;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;

import com.motorola.android.wrapper.TelephonyManagerWrapper;

/**
 * A mock {@link com.motorola.system.MotoTelephonyManager} class.
 * All methods are non-functional and throw {@link java.lang.UnsupportedOperationException}.
 * You can use this to inject other dependencies, mocks, or monitors into the classes you are
 * testing.
 */
public class MockTelephonyManager extends TelephonyManagerWrapper {

    public MockTelephonyManager(Context context) {
        super(context);
    }

    @Override
    public int getCallState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CellLocation getCellLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDataActivity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDataState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDeviceId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDeviceSoftwareVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLine1Number() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNetworkCountryIso() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNetworkOperator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNetworkOperatorName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNetworkType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPhoneType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSimCountryIso() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSimOperator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSimOperatorName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSimSerialNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSimState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSubscriberId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVoiceMailNumber() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasIccCard() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNetworkRoaming() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void listen(PhoneStateListener listener, int events) {
        throw new UnsupportedOperationException();
    }
}
