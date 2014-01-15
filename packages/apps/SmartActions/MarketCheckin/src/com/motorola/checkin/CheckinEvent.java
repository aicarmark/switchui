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

import android.content.ContentResolver;
import android.util.Log;

/**
 * This class allows Market Apps to use Checkin APIs without having dependency on the framework.
 * It gracefully exits on devices without MMI SDK.
 *
 */
public class CheckinEvent {
    private static final String TAG = CheckinEvent.class.getSimpleName();
    static final String NULL_STR = "null";

    // Constants for reflection
    private static final String CLASS_EVENT = "com.motorola.data.event.api.Event";
    private static final String CLASS_CHECKIN_EVENT = "com.motorola.android.provider.CheckinEvent";
    private static final String CLASS_SEGMENT = "com.motorola.data.event.api.Segment";
    private static final String METHOD_ADD_SEGMENT = "addSegment";
    private static final String METHOD_GET_EVENT_NAME = "getEventName";
    private static final String METHOD_GET_TAG_NAME = "getTagName";
    private static final String METHOD_GET_TIMESTAMP = "getTimestamp";
    private static final String METHOD_GET_VERSION = "getVersion";
    private static final String METHOD_SERIALIZE_EVENT = "serializeEvent";
    private static final String METHOD_SET_VALUE = "setValue";
    private static final String METHOD_PUBLISH = "publish";

    private static boolean sInitialized = false;

    // Event methods
    private static Method sMethodAddSegment, sMethodGetEventName, sMethodGetTagName,
            sMethodGetTimestamp, sMethodGetVersion, sMethodSerializeEvent, sMethodSetNameValueBoolean,
            sMethodSetNameValueDouble, sMethodSetNameValueInt, sMethodSetNameValueLong,
            sMethodSetNameValueString;

    // CheckinEvent methods
    private static Method sMethodPublish;
    private static Constructor<?> sConstructorCheckinEvent, sConstructorCheckinEventTimestamp;

    static {
        try {
            final Class<?> clsEvent = Class.forName(CLASS_EVENT);
            final Class<?> clsCheckinEvent = Class.forName(CLASS_CHECKIN_EVENT);
            final Class<?> clsSegment = Class.forName(CLASS_SEGMENT);
            // CheckinEvent Constructor
            sConstructorCheckinEvent = clsCheckinEvent.getDeclaredConstructor(
                                           String.class, String.class, String.class);
            sConstructorCheckinEventTimestamp = clsCheckinEvent.getDeclaredConstructor(
                                                    String.class, String.class, String.class, Long.TYPE);

            // Event Methods
            sMethodAddSegment = clsEvent.getDeclaredMethod(METHOD_ADD_SEGMENT, clsSegment);
            sMethodGetEventName = clsEvent.getDeclaredMethod(METHOD_GET_EVENT_NAME);
            sMethodGetTagName = clsEvent.getDeclaredMethod(METHOD_GET_TAG_NAME);
            sMethodGetTimestamp = clsEvent.getDeclaredMethod(METHOD_GET_TIMESTAMP);
            sMethodGetVersion = clsEvent.getDeclaredMethod(METHOD_GET_VERSION);
            sMethodSerializeEvent = clsEvent.getDeclaredMethod(METHOD_SERIALIZE_EVENT);
            sMethodSetNameValueBoolean = clsEvent.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                         Boolean.TYPE);
            sMethodSetNameValueDouble = clsEvent.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                        Double.TYPE);
            sMethodSetNameValueInt = clsEvent.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                     Integer.TYPE);
            sMethodSetNameValueLong = clsEvent.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                      Long.TYPE);
            sMethodSetNameValueString = clsEvent.getDeclaredMethod(METHOD_SET_VALUE, String.class,
                                        String.class);

            // CheckinEvent methods
            sMethodPublish = clsCheckinEvent.getDeclaredMethod(METHOD_PUBLISH, Object.class);

            sInitialized = true;
        } catch (Throwable t) {
            Log.w(TAG, "Unable to get checkin class.");
            sInitialized = false;
        }
    }

    // the actual reflected CheckinEvent
    private final Object mReflectedCheckinEvent;

    /**
     * @param tag tag against which the CheckinEvent is to be logged/captured
     * @param eventName name of the CheckinEvent
     * @param version version of the CheckinEvent
     */
    public CheckinEvent(String tag, String eventName, String version) {
        Object checkinEvent = null;
        if (sInitialized && tag != null && !tag.isEmpty()
                && eventName != null && !eventName.isEmpty()
                && version != null && !version.isEmpty()) {
            try {
                checkinEvent = sConstructorCheckinEvent.newInstance(tag, eventName, version);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to instantiate CheckinEvent.");
            }
        }
        mReflectedCheckinEvent = checkinEvent;
    }

    /**
     * @param tag tag against which the CheckinEvent is to be logged/captured
     * @param eventName name of the CheckinEvent
     * @param version version of the CheckinEvent
     * @param timestamp timestamp when CheckinEvent was captured
     */
    public CheckinEvent(String tag, String eventName, String version, long timestamp) {
        Object checkinEvent = null;
        if (sInitialized && tag != null && !tag.isEmpty()
                && eventName != null && !eventName.isEmpty()
                && version != null && !version.isEmpty()) {
            try {
                checkinEvent = sConstructorCheckinEventTimestamp.newInstance(tag, eventName,
                               version, timestamp);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to instantiate CheckinEvent.");
            }
        }
        mReflectedCheckinEvent = checkinEvent;
    }

    /**
     * @return true if CheckinEvent classes were initialized successfully, false otherwise
     */
    public static boolean isInitialized() {
        return sInitialized;
    }

    /**
     * @param segment CheckinSegment to add to the CheckinEvent
     */
    public void addSegment(CheckinSegment segment) {
        if (sInitialized && mReflectedCheckinEvent != null && segment != null) {
            try {
                sMethodAddSegment.invoke(mReflectedCheckinEvent, segment.getSegment());
            } catch (Throwable t) {
                Log.w(TAG, "Unable to add Segment.");
            }
        }
    }

    /**
     * @return the base segment name of this CheckinEvent
     */
    public String getEventName() {
        String eventName = null;
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                eventName = String.valueOf(sMethodGetEventName.invoke(mReflectedCheckinEvent));
            } catch (Throwable t) {
                Log.w(TAG, "Unable to get event name.");
            }
        }
        return eventName;
    }

    /**
     * @return the tag name for this CheckinEvent
     */
    public String getTagName() {
        String tagName = null;
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                tagName = String.valueOf(sMethodGetTagName.invoke(mReflectedCheckinEvent));
            } catch (Throwable t) {
                Log.w(TAG, "Unable to get tag name.");
            }
        }
        return tagName;
    }

    /**
     * @return the timestamp of this CheckinEvent
     */
    public long getTimestamp() {
        long timestamp = 0;
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                Long val = (Long) sMethodGetTimestamp.invoke(mReflectedCheckinEvent);
                if (val != null) timestamp = val.longValue();
            } catch (Throwable t) {
                Log.w(TAG, "Unable to get timestamp.");
            }
        }
        return timestamp;
    }

    /**
     * @return the version info for this CheckinEvent
     */
    public String getVersion() {
        String version = null;
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                version = String.valueOf(sMethodGetVersion.invoke(mReflectedCheckinEvent));
            } catch (Throwable t) {
                Log.w(TAG, "Unable to get version.");
            }
        }
        return version;
    }

    /**
     * Serializes all the inserted CheckinEvent details and returns the value the CheckinEvent.
     * name is serialized first, followed by version, timestamp, header attributes,
     * CheckinEvent attributes and finally the CheckinSegments of the CheckinEvent
     * @return serialized CheckinEvent information
     */
    public StringBuilder serializeEvent() {
        StringBuilder sb = null;
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                sb = (StringBuilder) sMethodSerializeEvent.invoke(mReflectedCheckinEvent);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to serialize event.");
            }
        }
        return sb;
    }

    /**
     * Inserts attribute name-value pair into this CheckinEvent.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, boolean value) {
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                sMethodSetNameValueBoolean.invoke(mReflectedCheckinEvent, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinEvent.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, double value) {
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                sMethodSetNameValueDouble.invoke(mReflectedCheckinEvent, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinEvent.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, int value) {
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                sMethodSetNameValueInt.invoke(mReflectedCheckinEvent, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinEvent.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, long value) {
        if (sInitialized && mReflectedCheckinEvent != null) {
            try {
                sMethodSetNameValueLong.invoke(mReflectedCheckinEvent, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Inserts attribute name-value pair into this CheckinEvent.
     * Attributes are serialized in the order they are inserted
     * @param name name of the attribute
     * @param value value of the attribute.
     */
    public void setValue(String name, String value) {
        if (sInitialized && mReflectedCheckinEvent != null) {
            if (value == null) {
                value = NULL_STR;
            }
            try {
                sMethodSetNameValueString.invoke(mReflectedCheckinEvent, name, value);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to set value.");
            }
        }
    }

    /**
     * Publishes the event to the framework specific publisher.
     * Prior to publishing:
     * 1. validates the event data first against the data dictionary definition of the event.
     * 2. Serializes the event per the serialization rules of the backend processing infrastructure
     * @param cr ContentResolver, cannot be null
     */
    public void publish(ContentResolver cr) {
        if (sInitialized && mReflectedCheckinEvent != null && cr != null) {
            try {
                sMethodPublish.invoke(mReflectedCheckinEvent, cr);
            } catch (Throwable t) {
                Log.w(TAG, "Unable to publish.");
            }
        }
    }
}
