/*
 * Copyright (c) 2010 Motorola, Inc.
   * All Rights Reserved
   *
   * The contents of this file are Motorola Confidential Restricted (MCR).
   * Revision history (newest first):
   *
   *  Date            CR                Author               Description
   *  2010-03-23  IKDROIDTWO-152:    E12758                   initial
*/
package com.motorola.filemanager.networkdiscovery;

import android.content.Context;

public class DiscoveryEnabler {
    private Context mContext;
    private DiscoveryExecuter mDiscoveryTask = null;
    private DiscoveryObserver mDiscoveryListener;
    private DiscoveryHandler mHandler;

    public DiscoveryEnabler(Context context) {
        mContext = context;
        mDiscoveryListener = null;
    }

    public DiscoveryEnabler(Context context, DiscoveryObserver listener) {
        mContext = context;
        mDiscoveryListener = listener;
    }

    public void startDiscovering() {
        mDiscoveryTask = new DiscoveryExecuter(mContext);
        mHandler = mDiscoveryTask.getHandler();
        mHandler.addServerListener(mDiscoveryListener);
        mDiscoveryTask.execute();
    }

    public void stopDiscovering() {
        cancelTasks();
    }

    private void cancelTasks() {
        if (mDiscoveryTask != null) {
            mDiscoveryTask.cancel(true);
            mDiscoveryTask = null;
        }
    }
}