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

package com.motorola.meter;

/** This is the callback used by MeterReader to collect data from Meter.
 * It is a oneway interface to make sure the service is not blocked by this asynchronous call.
 * This interface is called in the critical section of the MeterReader.
 *
 * @author w04917 (Brian Lee)
 */
oneway interface Callback_Meter {
    /** the callback the MeterReader service uses to read the values from the meter
    */
    void readMeter();
}
