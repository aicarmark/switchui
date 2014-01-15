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
import com.motorola.kpi.perfstats.DeviceCpuMetricCollector.DeviceCpuMetricData;
import com.motorola.kpi.perfstats.LogSetting;
import com.motorola.kpi.perfstats.LogSetting.Metrics;

/**
 * @author w04917 Brian Lee
 * Stores information about device cpu usage
 */
public class DeviceCpuMetric extends Metric {
    private DeviceCpuMetricData mBaseMetric, mCurrentMetric;

    private static final String FIELD_LOAD = "LOAD";
    private static final String FIELD_USER = "USER";
    private static final String FIELD_SYSTEM = "SYS";
    private static final String FIELD_IO_WAIT = "IOW";
    private static final String FIELD_IRQ = "IRQ";
    private static final String FIELD_SOFT_IRQ = "SIRQ";

    public DeviceCpuMetric(LogSetting logSetting) {
        super(logSetting);
    }

    @Override
    public void handleStart(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_CPU) && bundle != null) {
            mBaseMetric = DeviceCpuMetricData.unpack(bundle);
        }
    }

    @Override
    public void handleStop(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_CPU) && bundle != null) {
            mCurrentMetric = DeviceCpuMetricData.unpack(bundle);
        }
    }

    public Collection<Segment> getCheckinSegments() {
        Collection<Segment> segmentList = null;
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_CPU) &&
                mBaseMetric != null && mCurrentMetric != null) {
            /* the times are in jiffies */
            long userTime = mCurrentMetric.mUserTime - mBaseMetric.mUserTime;
            long systemTime = mCurrentMetric.mSystemTime - mBaseMetric.mSystemTime;
            long ioWaitTime = mCurrentMetric.mIoWaitTime - mBaseMetric.mIoWaitTime;
            long irqTime = mCurrentMetric.mIrqTime - mBaseMetric.mIrqTime;
            long softIrqTime =  mCurrentMetric.mSoftIrqTime - mBaseMetric.mSoftIrqTime;
            long idleTime = mCurrentMetric.mIdleTime - mBaseMetric.mIdleTime;
            long totalTime = userTime + systemTime + ioWaitTime + irqTime + softIrqTime + idleTime;

            if (totalTime > 0) {
                int userPercent = (int) (userTime * 100 / totalTime);
                int systemPercent = (int) (systemTime * 100 / totalTime);
                int ioWaitPercent = (int) (ioWaitTime * 100 / totalTime);
                int irqPercent = (int) (irqTime * 100 / totalTime);
                int softIrqPercent = (int) (softIrqTime * 100 / totalTime);
                int loadPercent = userPercent + systemPercent + ioWaitPercent + irqPercent +
                                  softIrqPercent;

                segmentList = new LinkedList<Segment>();
                Segment segment = new Segment(PerformanceStatsHandler.SEGMENT_CPU);
                segment.setValue(FIELD_LOAD, loadPercent);
                segment.setValue(FIELD_USER, userPercent);
                segment.setValue(FIELD_SYSTEM, systemPercent);
                segment.setValue(FIELD_IO_WAIT, ioWaitPercent);
                segment.setValue(FIELD_IRQ, irqPercent);
                segment.setValue(FIELD_SOFT_IRQ, softIrqPercent);
                segmentList.add(segment);
            }
        }
        return segmentList;
    }
}
