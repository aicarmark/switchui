/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/02/07   IKCTXTAW-359    Initial version
 * w04917 (Brian Lee)          2012/02/14   IKCTXTAW-441    Use the new Checkin API
 */

package com.motorola.datacollection;

import java.util.Collection;

import com.motorola.data.event.api.Segment;

/**
 * @author w04917 Brian Lee
 * Interface to generate check-in segment.
 * Useful when check-in event needs to be generated by combining check-in segments
 * from multiple components.
 */
public interface CheckinSegmentGenerator {

    /**
     * Generic method to generate a check-in segment.
     * Used if the implementing class has only one type of check-in segment to generate.
     * @return A collection of proper and complete check-in segment(s).
     * May return null or an empty collection.
     */
    public Collection<Segment> getCheckinSegments();

    /**
     * Used if the implementing class can generate different types of check-in segment.
     * @param type identifies which check-in segment to generate if supported
     * @return A collection of proper and complete check-in segment(s).
     * May return null or an empty collection.
     */
    public Collection<Segment> getCheckinSegments(int type);
}