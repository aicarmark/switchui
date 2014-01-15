/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * e51141                      06/26/12   IKCTXTAW-485      Init version
 */

package com.motorola.android.wrapper;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.WifiManager.WifiLock;
import java.util.List;

/**
 * A wrapper for {@link android.net.wifi.WifiManager} class.
 * All of its calls are delegated to the WifiManger class.
 * Can be subclassed to modify behavior for tesing or mocking purposes.
 */
public class WifiManagerWrapper {
    private static final String TAG = WifiManagerWrapper.class.getSimpleName();
    protected final WifiManager mWifiManager;

    /**
     * Constructor, get system service singleton
     */
    public WifiManagerWrapper(Context ctx) {
        mWifiManager = (WifiManager)ctx.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * {@link WifiManager#addNetwork}
     * @param config
     * @return
     */
    public int addNetwork(WifiConfiguration config) {
        return mWifiManager.addNetwork(config);
    }

    /**
     * {@link WifiManager#createMulticastLock}
     * @param tag
     * @return
     */
    public MulticastLock createMulticastLock(String tag) {
        return mWifiManager.createMulticastLock(tag);
    }

    /**
     * {@link WifiManager#createWifiLock}
     * @param lockType
     * @param tag
     * @return
     */
    public WifiLock createWifiLock(int lockType, String tag) {
        return mWifiManager.createWifiLock(lockType, tag);
    }

    /**
     * {@link WifiManager#addNetwork}
     * @param tag
     * @return
     */
    public WifiLock createWifiLock(String tag) {
        return mWifiManager.createWifiLock(tag);
    }

    /**
     * {@link WifiManager#disableNetwork}
     * @param netId
     * @return
     */
    public boolean disableNetwork(int netId) {
        return mWifiManager.disableNetwork(netId);
    }

    /**
     * {@link WifiManager#disconnect}
     * @return
     */
    public boolean disconnect() {
        return mWifiManager.disconnect();
    }

    /**
     * {@link WifiManager#enableNetwork}
     * @param netId
     * @param disableOthers
     * @return
     */
    public boolean enableNetwork(int netId, boolean disableOthers) {
        return mWifiManager.enableNetwork(netId, disableOthers);
    }

    /**
     * {@link WifiManager#getConfiguredNetworks}
     * @return
     */
    public List<WifiConfiguration> getConfiguredNetworks() {
        return mWifiManager.getConfiguredNetworks();
    }

    /**
     * {@link WifiManager#getDhcpInfo}
     * @return
     */
    public DhcpInfo getDhcpInfo() {
        return mWifiManager.getDhcpInfo();
    }

    /**
     * {@link WifiManager#getConnectionInfo}
     */
    public WifiInfo getConnectionInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * {@link WifiManager#getScanResults}
     */
    public List<ScanResult> getScanResults() {
        return mWifiManager.getScanResults();
    }

    /**
     * {@link WifiManager#getWifiState}
     */
    public int getWifiState() {
        return mWifiManager.getWifiState();
    }

    /**
     * {@link WifiManager#isWifiEnabled}
     */
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * {@link WifiManager#pingSupplicant}
     * @return
     */
    public boolean pingSupplicant() {
        return mWifiManager.pingSupplicant();
    }

    /**
     * {@link WifiManager#reassociate}
     * @return
     */
    public boolean reassociate() {
        return mWifiManager.reassociate();
    }

    /**
     * {@link WifiManager#reconnect}
     * @return
     */
    public boolean reconnect() {
        return mWifiManager.reconnect();
    }

    /**
     * {@link WifiManager#removeNetwork}
     * @param netId
     * @return
     */
    public boolean removeNetwork(int netId) {
        return mWifiManager.removeNetwork(netId);
    }

    /**
     * {@link WifiManager#saveConfiguration}
     * @return
     */
    public boolean saveConfiguration() {
        return mWifiManager.saveConfiguration();
    }

    /**
     * {@link WifiManager#setWifiEnabled}
     */
    public boolean setWifiEnabled(boolean enabled) {
        return mWifiManager.setWifiEnabled(enabled);
    }

    /**
     * {@link WifiManager#startScan}
     */
    public boolean startScan() {
        return mWifiManager.startScan();
    }

    /**
     * {@link WifiManager#updateNetwork}
     * @param config
     * @return
     */
    public int updateNetwork(WifiConfiguration config) {
        return mWifiManager.updateNetwork(config);
    }
}
