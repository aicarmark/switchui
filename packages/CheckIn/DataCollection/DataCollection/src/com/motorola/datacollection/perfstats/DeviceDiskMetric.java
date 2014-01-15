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
import java.util.HashMap;
import java.util.LinkedList;

import android.os.Bundle;

import com.motorola.data.event.api.Segment;
import com.motorola.kpi.perfstats.DeviceDiskMetricCollector.DeviceDiskMetricData;
import com.motorola.kpi.perfstats.DiskDeviceList;
import com.motorola.kpi.perfstats.LogSetting;
import com.motorola.kpi.perfstats.LogSetting.Metrics;


/**
 * @author w04917 Brian Lee
 * Stores information about device cpu usage
 */
public class DeviceDiskMetric extends Metric {

    private final HashMap<String, DeviceDiskMetricData> mBaseDisks =
        new HashMap<String, DeviceDiskMetricData>();
    private final HashMap<String, DeviceDiskMetricData> mCurrentDisks =
        new HashMap<String, DeviceDiskMetricData>();

    private static final String FIELD_BLOCK = "BLK";
    private static final String FIELD_READ = "READ";
    private static final String FIELD_WRITTEN = "WRI";

    public DeviceDiskMetric(LogSetting logSetting) {
        super(logSetting);
    }

    @Override
    public void handleStart(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_DISK) &&
                bundle != null) {
            mBaseDisks.clear();
            for (String diskName : DiskDeviceList.DISK_DEVICES) {
                if (diskName != null && !diskName.isEmpty()) {
                    DeviceDiskMetricData diskData = DeviceDiskMetricData.unpack(bundle, diskName);
                    if (diskData != null) {
                        mBaseDisks.put(diskName, diskData);
                    }
                }
            }
        }
    }

    @Override
    public void handleStop(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_DISK) &&
                bundle != null) {
            mCurrentDisks.clear();
            for (String diskName : DiskDeviceList.DISK_DEVICES) {
                if (diskName != null && !diskName.isEmpty()) {
                    DeviceDiskMetricData diskData = DeviceDiskMetricData.unpack(bundle, diskName);
                    if (diskData != null) {
                        mCurrentDisks.put(diskName, diskData);
                    }
                }
            }
        }
    }

    public Collection<Segment> getCheckinSegments() {
        Collection<Segment> segmentList = null;
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_DISK)) {
            segmentList = new LinkedList<Segment>();
            for (String diskName : DiskDeviceList.DISK_DEVICES) {
                if (diskName != null && !diskName.isEmpty()) {
                    DeviceDiskMetricData baseData = mBaseDisks.get(diskName);
                    DeviceDiskMetricData currentData = mCurrentDisks.get(diskName);
                    if (baseData != null && currentData != null &&
                            ((currentData.mKbRead > baseData.mKbRead) ||
                             (currentData.mKbWritten > baseData.mKbWritten)) ) {
                        Segment segment = new Segment(PerformanceStatsHandler.SEGMENT_DISK);
                        segment.setValue(FIELD_BLOCK, diskName);
                        segment.setValue(FIELD_READ, (currentData.mKbRead - baseData.mKbRead));
                        segment.setValue(FIELD_WRITTEN,
                                         (currentData.mKbWritten - baseData.mKbWritten));
                        segmentList.add(segment);
                    }
                }
            }
        }
        return segmentList;
    }
}
