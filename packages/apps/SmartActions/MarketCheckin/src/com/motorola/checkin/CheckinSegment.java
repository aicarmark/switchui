/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/14   IKCTXTAW-481    Initial version
 */

package com.motorola.checkin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.util.Log;

/**
 * This class allows Market Apps to use Checkin APIs without having dependency on the framework.
 * It gracefully exits on devices without MMI SDK.
 *
 */
public class CheckinSegment {
    private static final String TAG = CheckinSegment.class.getSimpleName();

    // Constants for reflection
    private static final String CLASS_SEGMENT = "com.motorola.data.event.api.Segment";
    private static final String METHOD_GET_SEGMENT_NAME = "getSegmentName";
    private static final String METHOD_SET_VALUE = "setValue";

    private static boolean sInitialized = false;

    private static Constructor<?> sConstructorSegment;
    private static Method sMethodGetSegmentName, sMethodSetNameValueBoolean,
            sMethodSetNameValueDouble, sMethodSetNameValueInt, sMethodSetNameValueLong,
            sMethodSetNameValueString;

    static {
        try {
            final Class<?> clsSegment = Class.forName(CLASS_SEGMENT);
            // Segment Constructor
            sConstructorSegment = clsSegment.getDeclaredConstructor(String.class);

            // Segment Methods
            sMethodGetSegmentName = clsSegment.getDeclaredMethod(METHOD_GET_SEGMENT_NAME);
            sMethodSetNameValueBoolean = clsSegment.getDeclaredMethod(METHOD_SET_VALUE,
                                         String.class, Boolean.TYPE);
            sMethodSetNameValueDouble = clsSegment.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                        Double.TYPE);
            sMethodSetNameValueInt = clsSegment.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                     Integer.TYPE);
            sMethodSetNameValueLong = clsSegment.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                      Long.TYPE);
            sMethodSetNameValueString = clsSegment.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                        String.class);

            sInitialized = true;
        } catch (Throwable t) {
            Log.w(TAG, "Unable to get segment class.");
            sInitialized = false;
        }
    }

    // the actual reflected Segment
    private final Object mReflectedSegment;

    /**
     * @param segmentName name of the CheckinSegment
     */
    public CheckinSegment(String segmentName) {
        Object segment = null;
        if (sInitialized && segmentName != null && !segmentName.isEmpty()) {
            try {
                segment = sConstructorSegment.newInstance(segmentName);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to instantiate Segment.");
            }
        }
        mReflectedSegment = segment;
    }

    /**
     * @return true if CheckinSegment classes were initialized successfully, false otherwise
     */
    public static boolean isInitialized() {
        return sInitialized;
    }

    /**
     * @return returns the reflected Segment object to add to the real CheckinEvent
     */
    Object getSegment() {
        return mReflectedSegment;
    }

    /**
     * @return the name of this CheckinSegment
     */
    public String getSegmentName() {
        String segmentName = null;
        if (sInitialized && mReflectedSegment != null) {
            try {
                segmentName = String.valueOf(sMethodGetSegmentName.invoke(mReflectedSegment));
            } catch (Throwable t) {
                Log.w(TAG, "Unable to get Segment name.");
            }
        }
        return segmentName;
    }

    /**
     * Inserts attribute name-value pair into this CheckinSegment.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, boolean value) {
        if (sInitialized && mReflectedSegment != null) {
            try {
                sMethodSetNameValueBoolean.invoke(mReflectedSegment, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinSegment.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, double value) {
        if (sInitialized && mReflectedSegment != null) {
            try {
                sMethodSetNameValueDouble.invoke(mReflectedSegment, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinSegment.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, int value) {
        if (sInitialized && mReflectedSegment != null) {
            try {
                sMethodSetNameValueInt.invoke(mReflectedSegment, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinSegment.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, long value) {
        if (sInitialized && mReflectedSegment != null) {
            try {
                sMethodSetNameValueLong.invoke(mReflectedSegment, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinSegment.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, String value) {
        if (sInitialized && mReflectedSegment != null) {
            if (value == null) {
                value = CheckinEvent.NULL_STR;
            }
            try {
                sMethodSetNameValueString.invoke(mReflectedSegment, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }
}
