/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number         Brief Description
 * ------------------------- ---------- -------------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272       Initial version
 * w04917 (Brian Lee)        2012/07/19   IKHSS6UPGR-14637   Handle Gson init error
 */

package com.motorola.meter.reading;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.motorola.meter.Logger;

/** This is the base class for MeterReading data.
 * This class makes the packing/unpacking of data transparent for all of its children.
 *
 * @author w04917 (Brian Lee)
 */
public abstract class MeterReading {
    private static final String TAG = "MeterReading";

    /* version number of meterReading, so the appropriate methods can be used
     * to parse the data later when retrieved. To be set by the individual subclasses
     */
    protected int version = 0;

    protected MeterReading() {
    }

    /**
     * @return version version for check-in data parsing purposes
     */
    public int getVersion() {
        return version;
    }

    /** used by Meter to pack data before sending to MeterReader
     *
     * @return JSON String representation of this object, null on error
     */
    public final String pack() {
        String jsonString = null;
        try {
            Gson gson = new Gson();
            jsonString = gson.toJson(this);
        } catch (Throwable t) {
            // catch any exceptions/errors like ExceptionInInitializerError
            Logger.e(TAG, "Error during pack(): " + t.getMessage());
        }
        return jsonString;
    }

    /** static function to return a new instance of meterReadingSubclass with data unpacked from the packedObject
     *
     * @param packedObject JSON String of the packed object
     * @param meterReadingSubclass The meterReadingSubclass to reconstruct from the packedObject
     * @return reconstructed meterReadingSubclass
     */
    public static final MeterReading unpack(String packedObject, Class<? extends MeterReading> meterReadingSubclass) {
        MeterReading meterReading = null;
        if (packedObject != null && !packedObject.isEmpty()) {
            try {
                Gson gson = new Gson();
                meterReading = gson.fromJson(packedObject, meterReadingSubclass);
            } catch (JsonSyntaxException e) {
                Logger.e(TAG, "JsonSyntaxException");
            } catch (Throwable t) {
                // catch any exceptions/errors like ExceptionInInitializerError
                Logger.e(TAG, "Error during unpack(): " + t.getMessage());
            }
        }
        return meterReading;
    }
}
