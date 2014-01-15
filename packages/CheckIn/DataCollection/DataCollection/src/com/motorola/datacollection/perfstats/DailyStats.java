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
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.Collection;
import java.util.LinkedList;

import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.CheckinEventHelper;
import com.motorola.datacollection.CheckinSegmentGenerator;

/**
 * @author w04917 Brian Lee
 * Object containing a single day's stats for a single component/action pair
 */
public class DailyStats implements CheckinSegmentGenerator {
    private static final String CHECK_IN_EVENT_DAILY_STATS = "PERF_DAILY_STATS";

    private static final String SEGMENT_STAT = "stat";
    private static final String FIELD_DATE = "DATE";
    private static final String FIELD_COMPONENT = "COMP";
    private static final String FIELD_ACTION = "ACT";
    private static final String FIELD_THRESHOLD = "THR";
    private static final String FIELD_NUM_GOOD_CASE = "GOOD";
    private static final String FIELD_NUM_BAD_CASE = "BAD";
    private static final String FIELD_MIN_TIME = "MIN";
    private static final String FIELD_MAX_TIME = "MAX";
    private static final String FIELD_AVG_TIME = "AVG";

    private final String mDate, mComponent, mAction;
    private final long mThreshold;
    private final int mNumGoodCase, mNumBadCase;
    private final long mMinTime, mMaxTime, mAvgTime;

    public DailyStats(String date, String component, String action, long threshold,
                      int numGoodCase, int numBadCase,
                      long minTime, long maxTime, long avgTime) {
        mDate = date;
        mComponent = component;
        mAction = action;
        mThreshold = threshold;
        mNumGoodCase = numGoodCase;
        mNumBadCase = numBadCase;
        mMinTime = minTime;
        mMaxTime = maxTime;
        mAvgTime = avgTime;
    }

    /**
     * @return CheckinEventHelper to add DailyStats segments to
     */
    public static CheckinEventHelper getCheckinEventHelper() {
        return new CheckinEventHelper(PerformanceStatsHandler.CHECK_IN_TAG,
                                      CHECK_IN_EVENT_DAILY_STATS,
                                      PerformanceStatsHandler.CHECK_IN_VERSION);
    }

    public Collection<Segment> getCheckinSegments() {
        Collection<Segment> segmentList = null;

        if (mDate != null && !mDate.isEmpty() &&
                mComponent != null && !mComponent.isEmpty() &&
                mAction != null && !mAction.isEmpty()) {
            segmentList = new LinkedList<Segment>();
            Segment segment = new Segment(SEGMENT_STAT);
            segment.setValue(FIELD_DATE, mDate);
            segment.setValue(FIELD_COMPONENT, mComponent);
            segment.setValue(FIELD_ACTION, mAction);
            segment.setValue(FIELD_THRESHOLD, mThreshold);
            segment.setValue(FIELD_NUM_GOOD_CASE, mNumGoodCase);
            segment.setValue(FIELD_NUM_BAD_CASE, mNumBadCase);
            segment.setValue(FIELD_MIN_TIME, mMinTime);
            segment.setValue(FIELD_MAX_TIME, mMaxTime);
            segment.setValue(FIELD_AVG_TIME, mAvgTime);
            segmentList.add(segment);
        }
        return segmentList;
    }

    public Collection<Segment> getCheckinSegments(int type) {
        return null;
    }
}
