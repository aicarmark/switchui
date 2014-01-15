/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * e51141                       06/26/12    IKCTXTAW-485    init version
 */

package com.motorola.android.wrapper.mock;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.WifiManager.WifiLock;

import com.motorola.android.wrapper.WifiManagerWrapper;

import java.util.List;

/**
 * A mock {@link com.motorola.android.wrapper.WifiManagerWrapper} class.
 * All methods are non-functional and throw {@link java.lang.UnsupportedOperationException}.
 * You can use this to inject other dependencies, mocks, or monitors into the classes you are
 * testing.
 */

public class MockWifiManager extends WifiManagerWrapper {

    public MockWifiManager(Context context) {
        super(context);
    }

    @Override
    public int addNetwork(WifiConfiguration config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MulticastLock createMulticastLock(String tag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WifiLock createWifiLock(int lockType, String tag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WifiLock createWifiLock(String tag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean disableNetwork(int netId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean disconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean enableNetwork(int netId, boolean disableOthers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<WifiConfiguration> getConfiguredNetworks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DhcpInfo getDhcpInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WifiInfo getConnectionInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ScanResult> getScanResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWifiState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWifiEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean pingSupplicant() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean reassociate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean reconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeNetwork(int netId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean saveConfiguration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setWifiEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean startScan() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int updateNetwork(WifiConfiguration config) {
        throw new UnsupportedOperationException();
    }

}
