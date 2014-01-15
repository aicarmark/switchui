/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/02/14   IKCTXTAW-441    Initial version
 * w04917 (Brian Lee)        2012/02/21   IKCTXTAW-442    Detailed meter data checkin event change.
 */

package com.motorola.datacollection;

import java.util.Collection;
import java.util.List;

import android.content.ContentResolver;

import com.motorola.android.provider.CheckinEvent;
import com.motorola.data.event.api.Segment;

/**
 * @author w04917 Brian Lee
 *
 * This helper class is a wrapper around CheckinEvent
 * to add commonly used functions that's not in the CheckinEvent API.
 * It would have been ideal to subclass the CheckinEvent class,
 * but the CheckinEvent class is a final class. It would have been easier
 * if CheckinEvent class had final methods, instead of being a final class.
 */
public class CheckinEventHelper {
    private static final String VALUE_NULL = "null";

    private final CheckinEvent mCheckinEvent;
    private boolean mHasSegments = false;

    public CheckinEventHelper(String tag, String eventName, String version)
    throws IllegalArgumentException {
        mCheckinEvent = new CheckinEvent(tag, eventName, version);
    }

    public CheckinEventHelper(String tag, String eventName, String version, long timestamp)
    throws IllegalArgumentException {
        mCheckinEvent = new CheckinEvent(tag, eventName, version, timestamp);
    }

    public final void publish(ContentResolver cr) throws IllegalArgumentException, Exception {
        mCheckinEvent.publish(cr);
    }

    public final void addSegment(Segment segment) {
        if (segment != null) {
            mCheckinEvent.addSegment(segment);
            mHasSegments = true;
        }
    }

    /**
     * Given a Collection of segments, adds the non-null segment members of the collection
     * to the wrapped CheckinEvent.
     * @param segments Collection of segments to add, can be null. If null, no segments are added.
     * @return number of segments added
     */
    public final int addSegments(Collection<Segment> segments) {
        int segmentAdded = 0;
        if (segments != null) {
            for (Segment segment : segments) {
                if (segment != null) {
                    mCheckinEvent.addSegment(segment);
                    segmentAdded++;
                    mHasSegments = true;
                }
            }
        }
        return segmentAdded;
    }

    /**
     * @return number of segments in the wrapped CheckinEvent
     */
    public final int getSegmentCount() {
        int segmentCount = 0;
        List<Segment> segmentList = mCheckinEvent.getSegments();
        if (segmentList != null) {
            segmentCount = segmentList.size();
        }
        return segmentCount;
    }

    /**
     * @return true if the wrapped CheckinEvent has segments, false otherwise
     */
    public final boolean hasSegments() {
        return mHasSegments;
    }

    /**
     * Sets a value for the specified field in the CheckinEvent
     * @param key key
     * @param value value to set
     * @throws IllegalArgumentException
     */
    public final void setValue(String key, String value) throws IllegalArgumentException {
        if (value == null || value.isEmpty()) {
            value = VALUE_NULL;
        }
        mCheckinEvent.setValue(key, value);
    }

    /**
     * Sets a value for the specified field in the CheckinEvent
     * @param key key
     * @param value value to set
     * @throws IllegalArgumentException
     */
    public final void setValue(String key, long value) throws IllegalArgumentException {
        mCheckinEvent.setValue(key, value);
    }

    /**
     * @return length of the check-in String represented by the wrapped CheckinEvent,
     * including its segments
     */
    public final int length() {
        int length = 0;
        String checkInString = this.toString();
        if (checkInString != null) {
            length = checkInString.length();
        }
        return length;
    }

    @Override
    /* generates the check-in String represented by the wrapped CheckinEvent */
    public final String toString() {
        StringBuilder sb = mCheckinEvent.serializeEvent();
        String checkInString = "";
        if (sb != null) {
            checkInString = sb.toString();
        }
        return checkInString;
    }

}
