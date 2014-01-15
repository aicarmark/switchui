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

package com.motorola.meter.reader;

import com.motorola.meter.Callback_Meter;
import android.os.Bundle;

/** This is the MeterReader interface exposed to the peripheral meter
 * Do not make this oneway interface. We have to make sure
 * the meter is actually registered before returning.
 *
 * @author w04917 (Brian Lee)
 */
interface IMeterReader {
    /** for registering meter to the MeterReader service
     * @param meter Callback_Meter defined by meter
     * @param dataType Canonical name of the MeterReading subclass used
     */
    void registerMeter(Callback_Meter meter, String dataType);

    /** for unregistering meter from the MeterReader service
     * @param meter Callback_Meter defined by meter. This has to be the same object
     * used for registering
     */
    void unregisterMeter(Callback_Meter meter);

    /** for reporting the asynchronously requested meterReading data
     * @param data A bundle having the packed meterReading JSON data and information about it
     */
    void reportData(in Bundle data);
}
