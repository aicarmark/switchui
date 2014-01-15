/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR                Author               Description
 *  2010-03-23      IKDROIDTWO-152    E12758               initial
 */
package com.motorola.filemanager.networkdiscovery;

public interface DiscoveryObserver {
    /**
     * Called when a host computer found.
     * 
     * @param sess
     *            Network session details.
     */
    public void DiscoveryHostFound(String host);

    /**
     * Called when a Discovery Started.
     * 
     * @param sess
     *            Network session details.
     */
    public void DiscoveryStarted();

    /**
     * Called when a Discovery Stopped.
     * 
     * @param sess
     *            Network session details.
     */
    public void DiscoveryFinished();
}
