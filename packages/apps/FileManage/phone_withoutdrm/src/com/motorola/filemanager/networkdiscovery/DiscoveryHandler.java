/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR                Author               Description
 *  2010-03-23  IKDROIDTWO-152    E12758                   initial
 */
package com.motorola.filemanager.networkdiscovery;

import android.content.Context;

import com.motorola.filemanager.samba.SambaExplorer;

/**
 * @author w14354
 * 
 */
public class DiscoveryHandler {
    private static final String TAG = "DiscoveryHandler";

    DiscoveryObserver m_listener = null;

    // Context mContext;

    public DiscoveryHandler(Context context) {
	// TODO Auto-generated constructor stub
	// mContext = context;
    }

    /**
     * Trigger a new host found event
     * 
     * @param host
     *            String
     */
    protected final void fireHostFoundEvent(String host) {
	SambaExplorer.log(TAG + " fireHostFoundEvent()" + "Host: " + host);
	if (m_listener != null) {
	    m_listener.DiscoveryHostFound(host);
	}
    }

    /**
     * Trigger a discovery finished vent
     */
    protected final void fireDiscoveryFinishEvent() {
	SambaExplorer.log(TAG + " fireDiscoveryFinishEvent()" + "\n");
	if (m_listener != null) {
	    m_listener.DiscoveryFinished();
	}
    }

    /**
     * Trigger a discovery finished vent
     */
    protected final void fireDiscoveryStartEvent() {
	SambaExplorer.log(TAG + " fireDiscoveryStartEvent()" + "\n");
	if (m_listener != null) {
	    m_listener.DiscoveryStarted();
	}
    }

    /**
     * Add a server listener to this server
     * 
     * @param l
     *            DiscoveryListener
     */
    public final void addServerListener(DiscoveryObserver l) {
	SambaExplorer.log(TAG + " addServerListener() " + l);
	m_listener = l;
    }

    /**
     * Remove the server listener
     * 
     * @param l
     *            DiscoveryListener
     */
    public final void removeServerListener(DiscoveryObserver l) {
	if (m_listener == l) {
	    m_listener = null;
	}
    }
}
