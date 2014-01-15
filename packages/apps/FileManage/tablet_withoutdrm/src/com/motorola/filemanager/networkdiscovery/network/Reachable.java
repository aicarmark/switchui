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

//import android.util.Log;

public class Reachable {
    //private static final String TAG = "Reachable:";
    // final int[] ports = { 445, 22, 80, 111 };
    final int[] ports = {445};

    final int len = ports.length;

    public int isReachable(InetAddress host, int timeout) {
        for (int i = 0; i < len; i++) {
            try {
                // SambaExplorer.log(TAG + "HostAddress: " + host.getHostAddress() + " Port: " + ports[i], false);
                Socket s = new Socket();
                s.bind(null);
                s.connect(new InetSocketAddress(host, ports[i]), timeout);
                s.close();
                return ports[i];
            } catch (IOException e) {
                //SambaExplorer.log("Reachable" + e.getMessage(), true);
            }
        }
        return -1;
    }
}
