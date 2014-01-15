/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number         Brief Description
 * ------------------------- ---------- -------------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272       Initial version
 * w04917 (Brian Lee)        2011/05/11   IKCTXTAW-272       Error handling for non-responsive peripheral
 * w04917 (Brian Lee)        2012/07/19   IKHSS6UPGR-14637   Handle Gson init error
 *
 */

package com.motorola.collectionservice;

import android.os.Bundle;

import com.motorola.meter.Logger;
import com.motorola.meter.Meter;
import com.motorola.meter.reading.MeterReading;
import com.motorola.meter.reading.PowerReading;

/**
 *  This is the CollectionService library that applications wishing to use the
 * MeterReader framework should include in their project.
 * There is no need for applications to include or see the whole MeterReader framework, which also
 * interacts with Meters.
 * Any CollectionServiceAPI specific interface exposed to the applications are defined here
 *
 * @author w04917 (Brian Lee)
 *
 */
public class CollectionServiceAPI {
    private static final String TAG = "CollectionServiceAPI";

    /* intent used by applications to bind to the CollectionService interface of the MeterReader service */
    public static final String INTENT_BIND_FRAMEWORK = "com.motorola.collectionservice.intent.bind_framework";

    /* This class is not meant to be instantiated.
     * By making the constructor private, no one else can instantiate this class.
     * By throwing an AssertionError, we can make sure we don't accidently call it within this class.
     */
    private CollectionServiceAPI() {
        throw new AssertionError();
    }

    /**
     * Applications will use this static function to unpack the bundle they received from CollectionService.
     * This will reconstruct the original MeterReading data as the original subclass object used by the
     * peripheral-defined Meter
     *
     * @param bundle	The bundle received from CollectionService
     * @return a reconstructed MeterReading subclass that was created by the peripheral. returns null
     *         on error (such as timer expiration due to non-responsive peripheral)
     */
    /* The bundle's METER_DATA_TYPE should always have the class name of the subclass of MeterReading */
    public static MeterReading unpack(Bundle bundle) {
        MeterReading meterReading = null;

        if (bundle != null && bundle.containsKey(Meter.METER_DATA_TYPE)
                && bundle.containsKey(Meter.METER_READING_JSON)) {
            Class<? extends MeterReading> meterReadingSubclass = null;
            String className = bundle.getString(Meter.METER_DATA_TYPE);
            String jsonString = bundle.getString(Meter.METER_READING_JSON);
            try {
                meterReadingSubclass = Class.forName(className).asSubclass(MeterReading.class);
            } catch (ClassNotFoundException e1) {
                /* If the caller class does not have the class referred to in the bundle's METER_DATA_TYPE,
                 * ClassNotFoundException will occur.
                 * In other words, if com.motorola.SampleMeter was the meter that was packed,
                 * the application wishing to unpack it and use it should have the com.motorola.SampleMeter class.
                 * This makes sense because other wise, they won't be able to use it anyway.
                 * If they just need totalPower value, apps can use getTotalUwh() call.
                 */
                Logger.e(TAG, "Could not find class " + className);
            } catch (ClassCastException e2) {
                Logger.e(TAG, "Could not cast class " + className);
            } catch (LinkageError e3) {
                /* LinkageError is a parent class of ExceptionInInitializerError as well */
                Logger.e(TAG, "Could not link class " + className);
            }
            if (meterReadingSubclass != null) {
                meterReading = MeterReading.unpack(jsonString, meterReadingSubclass);
            }
        }

        return meterReading;
    }

    /**
     * @param bundle	The bundle received from CollectionService
     * @return The totalUwh for the meter that was packed in the bundle. -1 on error.
     */
    public static int getTotalUwh(Bundle bundle) {
        int totalUwh = -1;

        if (bundle != null && bundle.containsKey(Meter.METER_DATA_TYPE)
                && bundle.containsKey(Meter.METER_READING_JSON)) {
            String jsonString = bundle.getString(Meter.METER_READING_JSON);
            PowerReading powerReading = (PowerReading) MeterReading.unpack(jsonString, PowerReading.class);
            if (powerReading != null) {
                totalUwh = powerReading.totalUwh;
            }
        }
        return totalUwh;
    }

    /**
     * @param bundle	The bundle received from CollectionService
     * @return The timestamp when the meter values were calculated. -1 on error.
     */
    public static long getTimestamp(Bundle bundle) {
        long timestamp = -1;
        if (bundle != null && bundle.containsKey(Meter.METER_TIMESTAMP)) {
            timestamp = bundle.getLong(Meter.METER_TIMESTAMP);
        }
        return timestamp;
    }

    /**
     * @param bundle	The bundle received from CollectionService
     * @return The full canonical name of the MeterReading subclass that was packed into the bundle
     */
    public static String getDataType(Bundle bundle) {
        String dataType = null;
        if (bundle != null && bundle.containsKey(Meter.METER_DATA_TYPE)) {
            dataType = bundle.getString(Meter.METER_DATA_TYPE);
        }
        return dataType;
    }


    /**
     * @param bundle	The bundle received from CollectionService
     * @return The JSON string that represents the MeterReading subclass that's packed into this bundle.
     *         null on error.
     */
    public static String getRawData(Bundle bundle) {
        String rawData = null;
        if (bundle != null && bundle.containsKey(Meter.METER_READING_JSON)) {
            rawData = bundle.getString(Meter.METER_READING_JSON);
        }
        return rawData;
    }
}
