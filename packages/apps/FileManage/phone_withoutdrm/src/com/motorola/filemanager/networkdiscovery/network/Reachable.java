/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date          CR                Author       Description
 *  2010-03-23    IKDROIDTWO-152    E12758       initial
 */

package com.motorola.filemanager.networkdiscovery.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

// import android.util.Log;

public class Reachable {
    // private static final String TAG = "Reachable:";
    // final int[] ports = { 445, 22, 80, 111 };
    final int[] ports = { 445 };

    final int len = ports.length;

    public int isReachable(InetAddress host, int timeout) {
	Socket s = null;
	for (int i = 0; i < len; i++) {
	    try {
		// SambaExplorer.log(TAG + "HostAddress: " +
		// host.getHostAddress() +
		// " Port: " + ports[i]);
		s = new Socket();
		s.bind(null);
		s.connect(new InetSocketAddress(host, ports[i]), timeout);
		return ports[i];
	    } catch (IOException e) {
		// Log.e("Reachable", e.getMessage());
	    } finally {
		try {
		    if (s != null) {
			s.close();
		    }
		} catch (IOException e) {
		    // Log.e("Reachable", e.getMessage());
		}
	    }
	}
	return -1;
    }
}
