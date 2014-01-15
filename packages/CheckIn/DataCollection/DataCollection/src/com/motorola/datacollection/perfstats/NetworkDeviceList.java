/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/02/07   IKCTXTAW-359    Initial version
 *
 */

package com.motorola.datacollection.perfstats;

/**
 * @author w04917 Brian Lee
 * Defines NETWORK_DEVICES to look for.
 * Stand-alone file to enable overlays for different products.
 */
public class NetworkDeviceList {

    public static final String[] NETWORK_DEVICES = {
        "cfhsi0",
        "ppp0",
        "rmnet0",
        "rmnet1",
        "tiwlan0",
        "wlan0"
    };
}
