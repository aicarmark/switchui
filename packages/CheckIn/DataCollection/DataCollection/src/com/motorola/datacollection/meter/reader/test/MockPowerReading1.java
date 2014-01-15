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

package com.motorola.datacollection.meter.reader.test;

import com.motorola.meter.reading.PowerReading;

/* This class has to be in a separate file
 * so that the static method CollectionServiceAPI.unpack() can be used
 * to unpack this class
 */
public class MockPowerReading1 extends PowerReading {
    public static final int MOCK_POWER_READING_1_VERSION = 1;
    public int mockInt;
    public String mockString;
    public double mockDouble;

    public MockPowerReading1() {
        version = MOCK_POWER_READING_1_VERSION;
    }
}
