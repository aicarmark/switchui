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

/**
 * @author w14354
 *
 */
public interface DiscoveryPrefInterface {
    public final static boolean DEFAULT_RESOLVE_NAME = false;
    public final static boolean DEFAULT_VIBRATE_FINISH = false;
    public final static String DEFAULT_PORT_START = "1";
    public final static String DEFAULT_PORT_END = "1024";
    public final static int MAX_PORT_END = 65535;
    public static final String DEFAULT_NTHREADS = "32";
    public final static boolean DEFAULT_TIMEOUT_FORCE = false;
    public final static String DEFAULT_TIMEOUT = "500";
    public static final boolean DEFAULT_RATECTRL_ENABLE = true;
    public final static String DEFAULT_TIMEOUT_DISCOVER = "500";
    public final static long DEFAULT_VIBRATE = 250;
}
