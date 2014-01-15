/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 *
 */

package com.motorola.meter.reading;

/** base class for all power related MeterReadings.
 * anything that is general to all power readings would go here
 *
 * @author w04917 (Brian Lee)
 */
public class PowerReading extends MeterReading {
    /* total micro watt hour, cumulative since last boot */
    public int totalUwh = 0;

    /* required for Gson instantiation to extract totalUwh during check-in */
    protected PowerReading() {
    }
}
