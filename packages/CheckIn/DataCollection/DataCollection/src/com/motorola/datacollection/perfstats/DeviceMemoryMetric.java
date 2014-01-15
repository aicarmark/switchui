/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2011/11/01   IKCTXTAW-359    Initial version
 * w04917 (Brian Lee)          2012/02/14   IKCTXTAW-441    Use the new Checkin API
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.Collection;
import java.util.LinkedList;

import android.os.Bundle;

import com.motorola.data.event.api.Segment;
import com.motorola.kpi.perfstats.DeviceMemoryMetricCollector.DeviceMemoryMetricData;
import com.motorola.kpi.perfstats.LogSetting;
import com.motorola.kpi.perfstats.LogSetting.Metrics;


/**
 * @author w04917 Brian Lee
 * Stores information about device memory usage
 */
public class DeviceMemoryMetric extends Metric {
    private DeviceMemoryMetricData mMetric;

    private static final String FIELD_TOTAL = "TOTL";
    private static final String FIELD_FREE = "FREE";
    private static final String FIELD_BUFFERS = "BUFF";
    private static final String FIELD_CACHED = "CACH";

    public DeviceMemoryMetric(LogSetting logSetting) {
        super(logSetting);
    }

    @Override
    public void handleStart(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_MEMORY) &&
                bundle != null) {
            mMetric = DeviceMemoryMetricData.unpack(bundle);
        }
    }

    @Override
    public void handleStop(Bundle bundle) {
        //do nothing
    }

    public Collection<Segment> getCheckinSegments() {
        Collection<Segment> segmentList = null;
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_MEMORY) &&
                mMetric != null) {
            segmentList = new LinkedList<Segment>();
            Segment segment = new Segment(PerformanceStatsHandler.SEGMENT_MEMORY);
            segment.setValue(FIELD_TOTAL, mMetric.mMemTotal);
            segment.setValue(FIELD_FREE, mMetric.mMemFree);
            segment.setValue(FIELD_BUFFERS, mMetric.mMemBuffers);
            segment.setValue(FIELD_CACHED, mMetric.mMemCached);
            segmentList.add(segment);
        }
        return segmentList;
    }
}
